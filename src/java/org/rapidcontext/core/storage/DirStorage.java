/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2019 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE for more details.
 */

package org.rapidcontext.core.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.PropertiesSerializer;
import org.rapidcontext.util.FileUtil;

/**
 * A persistent data storage and retrieval handler based on a file
 * system directory. This class will read and write both Java
 * property files and binary files depending on the object type
 * provided. The property files are used for all dictionary data
 * storage and retrieval.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class DirStorage extends Storage {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(DirStorage.class.getName());

    /**
     * The dictionary key for the base directory. The value stored is
     * a file object.
     */
    public static final String KEY_DIR = "dir";

    /**
     * The file suffix used for properties files. These files are
     * used for serializing dictionary objects to files.
     */
    public static final String SUFFIX_PROPS = ".properties";

    /**
     * Creates a new directory storage.
     *
     * @param dir            the base data directory to use
     * @param readWrite      the read write flag
     */
    public DirStorage(File dir, boolean readWrite) {
        super("dir", readWrite);
        dict.set(KEY_DIR, dir);
    }

    /**
     * Returns the base directory for the storage data files.
     *
     * @return the base directory for data files
     */
    public File dir() {
        return (File) dict.get(KEY_DIR);
    }

    /**
     * Searches for an object at the specified location and returns
     * metadata about the object if found. The path may locate either
     * an index or a specific object.
     *
     * @param path           the storage location
     *
     * @return the metadata for the object, or
     *         null if not found
     */
    public Metadata lookup(Path path) {
        File  file;

        if (PATH_STORAGEINFO.equals(path)) {
            return new Metadata(Dict.class, path, path(), mountTime());
        }
        file = locateFile(path);
        if (file == null) {
            return null;
        } else if (file.isDirectory()) {
            return new Metadata(Index.class, path, path(), file.lastModified());
        } else if (file.getName().endsWith(SUFFIX_PROPS)) {
            return new Metadata(Dict.class, path, path(), file.lastModified());
        } else {
            return new Metadata(Binary.class, path, path(), file.lastModified());
        }
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
     */
    public Object load(Path path) {
        File    file;
        String  msg;

        if (PATH_STORAGEINFO.equals(path)) {
            return dict;
        }
        file = locateFile(path);
        if (file == null) {
            return null;
        } else if (path.isIndex()) {
            Index idx = new Index(path);
            if (path.isRoot()) {
                idx.addObject(PATH_STORAGEINFO.name());
            }
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                String name = files[i].getName();
                if (files[i].isDirectory()) {
                    idx.addIndex(name);
                } else {
                    idx.addObject(StringUtils.removeEnd(name, SUFFIX_PROPS));
                }
            }
            idx.updateLastModified(new Date(file.lastModified()));
            return idx;
        } else if (file.getName().endsWith(SUFFIX_PROPS)) {
            try {
                return PropertiesSerializer.read(file);
            } catch (FileNotFoundException e) {
                msg = "failed to find file " + file.toString();
                LOG.log(Level.SEVERE, msg, e);
                return null;
            } catch (IOException e) {
                msg = "failed to read file " + file.toString();
                LOG.log(Level.SEVERE, msg, e);
                return null;
            }
        } else {
            return new Binary.BinaryFile(file);
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
        if (path.isIndex()) {
            String msg = "cannot write to index " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        } else if (data == null) {
            String msg = "cannot store null data, use remove(): " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        } else if (!isReadWrite()) {
            String msg = "cannot store to read-only storage at " + path();
            LOG.warning(msg);
            throw new StorageException(msg);
        } else if (PATH_STORAGEINFO.equals(path)) {
            String msg = "storage info is read-only: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        if (data instanceof StorableObject) {
            data = ((StorableObject) data).serialize();
        }
        File dir = locateDir(path);
        File file = new File(dir, path.name());
        File tmp = null;
        if (data instanceof Dict) {
            try {
                file = new File(dir, path.name() + SUFFIX_PROPS);
                tmp = FileUtil.tempFile(file.getName());
                PropertiesSerializer.write(tmp, (Dict) data);
            } catch (Exception e) {
                String msg = "failed to write temporary file " + tmp + ": " +
                             e.getMessage();
                LOG.warning(msg);
                throw new StorageException(msg);
            }
        } else if (data instanceof Binary) {
            try (InputStream is = ((Binary) data).openStream()) {
                tmp = FileUtil.tempFile(file.getName());
                FileUtil.copy(is, tmp);
            } catch (Exception e) {
                String msg = "failed to write temporary file " + tmp + ": " +
                             e.getMessage();
                LOG.warning(msg);
                throw new StorageException(msg);
            }
        } else if (data instanceof File) {
            tmp = (File) data;
        } else {
            String msg = "cannot store unsupported data type at " + path();
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        dir.mkdirs();
        if (!tmp.renameTo(file)) {
            String msg = "failed to move " + tmp + " to file " + file;
            LOG.warning(msg);
            throw new StorageException(msg);
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

        if (!isReadWrite()) {
            msg = "cannot remove from read-only storage at " + path();
            LOG.warning(msg);
            throw new StorageException(msg);
        } else if (PATH_STORAGEINFO.equals(path)) {
            msg = "storage info is read-only: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        file = locateFile(path);
        if (file != null) {
            try {
                if (path.isRoot()) {
                    FileUtil.deleteFiles(file);
                } else {
                    FileUtil.delete(file);
                    FileUtil.deleteEmptyDirs(dir());
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
        File subDir = dir();
        for (int i = 0; i < path.depth(); i++) {
            subDir = new File(subDir, path.name(i));
        }
        return subDir;
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
        if (file.isDirectory() != path.isIndex()) {
            return null;
        } else {
            return file.canRead() ? file : null;
        }
    }
}
