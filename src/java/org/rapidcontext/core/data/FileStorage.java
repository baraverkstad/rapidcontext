/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE.txt file for more details.
 */

package org.rapidcontext.core.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.rapidcontext.util.FileUtil;

/**
 * A persistent data storage and retrieval handler based on the file
 * system. This class will read and write both Java property files
 * and binary files depending on the object type provided. The
 * property files are used for all dictionary data storage and
 * retrieval.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class FileStorage implements Storage {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(FileStorage.class.getName());

    /**
     * The file suffix used for properties files. These files are
     * used for serializing dictionary objects to files.
     */
    public static final String SUFFIX_PROPS = ".properties";

    /**
     * The base directory for storing data files.
     */
    private File baseDir;

    /**
     * Creates a new file storage.
     *
     * @param dir            the base data directory to use
     */
    public FileStorage(File dir) {
        this.baseDir = dir;
    }

    /**
     * Searches for an object at the specified location and returns
     * meta-data about the object if found. The path may locate
     * either an index or a specific object. 
     *
     * @param path           the storage location
     *
     * @return the meta-data dictionary for the object, or
     *         null if not found
     */
    public Dict lookup(Path path) {
        File  file = locateFile(path);
        Dict  dict = null;

        if (file != null) {
            dict = new Dict();
            if (file.isDirectory()) {
                dict.set(KEY_TYPE, TYPE_INDEX);
                dict.set(KEY_CLASS, Index.class);
            } else if (file.getName().endsWith(SUFFIX_PROPS)) {
                dict.set(KEY_TYPE, TYPE_OBJECT);
                dict.set(KEY_CLASS, Dict.class);
            } else {
                dict.set(KEY_TYPE, TYPE_FILE);
                dict.set(KEY_CLASS, File.class);
            }
            dict.set(KEY_MODIFIED, new Long(file.lastModified()));
        }
        return dict;
    }

    /**
     * Loads an object from the specified location. The path may
     * locate either an index or a specific object. In case of an
     * index, the data returned is an index dictionary listing of
     * all objects in it.
     *
     * @param path           the storage location
     *
     * @return the data read, or
     *         null if not found
     *
     * @throws StorageException if the data couldn't be read
     */
    public Object load(Path path) throws StorageException {
        File  file = locateFile(path);

        if (file == null) {
            return path.isIndex() ? new Index(path) : null;
        } else if (path.isIndex()) {
            Index idx = new Index(path);
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                String name = files[i].getName();
                if (files[i].isDirectory()) {
                    idx.addIndex(name);
                } else {
                    idx.addObject(StringUtils.removeEnd(name, SUFFIX_PROPS));
                }
            }
            return idx;
        } else if (file.getName().endsWith(SUFFIX_PROPS)) {
            try {
                return PropertiesSerializer.read(file);
            } catch (FileNotFoundException e) {
                String msg = "failed to find file " + file.toString();
                LOG.warning(msg);
                throw new StorageException(msg);
            } catch (IOException e) {
                String msg = "failed to read file " + file.toString() +
                             ": " + e.getMessage();
                LOG.warning(msg);
                throw new StorageException(msg);
            }
        } else {
            return file;
        }
    }

    /**
     * Stores an object at the specified location. The path must
     * locate a particular object or file, since direct manipulation
     * of indices is not supported. Any previous data at the
     * specified path will be overwritten or removed. Note that only
     * dictionaries and files can be stored in a file storage.
     *
     * @param path           the storage location
     * @param data           the data to store
     *
     * @throws StorageException if the data couldn't be written
     */
    public void store(Path path, Object data) throws StorageException {
        String  msg;
        File    file;

        if (path.isIndex()) {
            msg = "cannot write to index " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        } else if (data == null) {
            msg = "cannot store null data, use remove() instead: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        if (data instanceof Dict) {
            file = locateDir(path);
            file.mkdirs();
            file = new File(file, path.name() + SUFFIX_PROPS);
            try {
                PropertiesSerializer.write(file, (Dict) data);
            } catch (IOException e) {
                msg = "failed to write file " + file + ": " + e.getMessage();
                LOG.warning(msg);
                throw new StorageException(msg);
            }
        } else if (data instanceof File) {
            file = locateDir(path);
            file.mkdirs();
            file = new File(file, path.name());
            ((File) data).renameTo(file);
        } else {
            throw new StorageException("cannot store unsupported data type");
        }
    }

    /**
     * Removes an object or an index at the specified location. If
     * the path refers to an index, all contained objects and indices
     * will be removed recursively.
     *
     * @param path           the storage location
     *
     * @throws StorageException if the data couldn't be removed
     */
    public void remove(Path path) throws StorageException {
        String  msg;
        File    file;

        file = locateFile(path);
        if (file != null) {
            try {
                if (path.isRoot()) {
                    FileUtil.deleteContents(file);
                } else {
                    FileUtil.delete(file);
                }
            } catch (IOException e) {
                msg = "failed to remove " + file + ": " + e.getMessage();
                LOG.warning(msg);
                throw new StorageException(msg);
            }
        }
    }

    /**
     * Locates the last directory referenced by the specified path.
     * If the path is an index, the directory returned will be the
     * index directory itself. Otherwise the parent directory will be
     * returned.
     *
     * @param path           the storage location
     *
     * @return the last directory referenced by the path
     */
    private File locateDir(Path path) {
        File dir = baseDir;

        for (int i = 0; i < path.depth(); i++) {
            dir = new File(dir, path.name(i));
        }
        return dir;
    }

    /**
     * Locates the file references by the specified path. If no file
     * is named exactly as the last path name, the properties file
     * extension is appended to the name.
     *
     * @param path           the storage location
     *
     * @return the file referenced by the path, or
     *         null if no existing file was found
     */
    private File locateFile(Path path) {
        File    dir = locateDir(path);
        File    file = dir;

        if (!path.isIndex()) {
            file = new File(dir, path.name());
            if (!file.canRead()) {
                file = new File(dir, path.name() + SUFFIX_PROPS);
            }
        }
        return file.canRead() ? file : null;
    }
}
