/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2025 Per Cederberg. All rights reserved.
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.util.FileUtil;

/**
 * A persistent data storage and retrieval handler based on a file
 * system directory. This class will read and write both Java
 * property files and binary files depending on the object type
 * provided. The property files are used for all dictionary data
 * storage and retrieval.
 *
 * @author Per Cederberg
 */
public class DirStorage extends Storage {

    /**
     * The class logger.
     */
    private static final Logger LOG = Logger.getLogger(DirStorage.class.getName());

    /**
     * The dictionary key for the base directory. The value stored is
     * a file object.
     */
    public static final String KEY_DIR = "dir";

    /**
     * Creates a new directory storage.
     *
     * @param dir            the base data directory to use
     * @param readWrite      the read write flag
     */
    public DirStorage(File dir, boolean readWrite) {
        super(dir.getName(), "dir", readWrite);
        dict.set(KEY_DIR, dir);
    }

    /**
     * Returns the base directory for the storage data files.
     *
     * @return the base directory for data files
     */
    public File dir() {
        return dict.get(KEY_DIR, File.class);
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
    @Override
    public Metadata lookup(Path path) {
        if (PATH_STORAGEINFO.equals(path)) {
            return new Metadata(Dict.class, PATH_STORAGEINFO, path(), mountTime());
        }
        File file = locateFile(path);
        if (file == null) {
            return null;
        }
        String mime = Mime.type(file.getName());
        Date modified = new Date(file.lastModified());
        long size = file.length();
        if (file.isDirectory()) {
            return new Metadata(Index.class, toPath(file, true), path(), modified);
        } else if (!path.name().equalsIgnoreCase(file.getName())) {
            File f = new File(file.getParentFile(), objectName(file.getName()));
            Metadata m = new Metadata(Dict.class, toPath(f, false), path(), modified);
            return m.mimeType(mime).size(size);
        } else {
            Metadata m = new Metadata(Binary.class, toPath(file, false), path(), modified);
            return m.mimeType(mime).size(size);
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
    @Override
    public Object load(Path path) {
        if (PATH_STORAGEINFO.equals(path)) {
            return dict;
        }
        File file = locateFile(path);
        if (file == null) {
            return null;
        } else if (path.isIndex()) {
            Index idx = new Index(new Date(file.lastModified()));
            if (path.isRoot()) {
                idx.addObject(PATH_STORAGEINFO.name());
            }
            for (File f : file.listFiles()) {
                String name = f.getName();
                if (f.isDirectory()) {
                    idx.addIndex(name);
                } else {
                    idx.addObject(name);
                }
            }
            return idx;
        } else if (!path.name().equalsIgnoreCase(file.getName())) {
            try (InputStream is = new FileInputStream(file)) {
                return unserialize(path, file.getName(), is);
            } catch (IOException e) {
                String msg = "failed to read file " + file.toString();
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
    @Override
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
        File dir = locateDir(path);
        File file = FileUtil.resolve(dir, path.name());
        File tmp = null;
        if (data instanceof Binary b) {
            try (InputStream is = b.openStream()) {
                tmp = FileUtil.tempFile(file.getName());
                FileUtil.copy(is, tmp);
            } catch (Exception e) {
                String msg = "failed to write temporary file " + tmp + ": " +
                             e.getMessage();
                LOG.warning(msg);
                throw new StorageException(msg);
            }
        } else if (data instanceof File f) {
            tmp = f;
        } else {
            String name = objectName(path.name());
            try {
                tmp = FileUtil.tempFile(file.getName());
                if (data instanceof StorableObject) {
                    data = StorableObject.sterilize(data, false, true, false);
                }
                try (FileOutputStream os = new FileOutputStream(tmp)) {
                    serialize(file.getName(), data, os);
                }
            } catch (Exception e) {
                String msg = "failed to write temporary file " + tmp + ": " +
                             e.getMessage();
                LOG.warning(msg);
                throw new StorageException(msg);
            }
            remove(path.sibling(name));
        }
        dir.mkdirs();
        try {
            FileUtil.move(tmp, file);
        } catch (IOException e) {
            String msg = "failed to move " + tmp + " to file " + file;
            LOG.log(Level.WARNING, msg, e);
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
    @Override
    public void remove(Path path) throws StorageException {
        if (!isReadWrite()) {
            String msg = "cannot remove from read-only storage at " + path();
            LOG.warning(msg);
            throw new StorageException(msg);
        } else if (PATH_STORAGEINFO.equals(path)) {
            String msg = "storage info is read-only: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        File file = locateFile(path);
        if (file != null) {
            try {
                if (path.isRoot()) {
                    FileUtil.deleteFiles(file);
                } else {
                    FileUtil.delete(file);
                    FileUtil.deleteEmptyDirs(dir());
                }
            } catch (IOException e) {
                String msg = "failed to remove " + file + ": " + e.getMessage();
                LOG.warning(msg);
                throw new StorageException(msg);
            }
        }
    }

    /**
     * Converts a file path to the corresponding storage path.
     *
     * @param file           the file to convert
     * @param index          the index path flag
     *
     * @return the corresponding storage path
     */
    private Path toPath(File file, boolean index) {
        if (file == null || file.equals(dir())) {
            return Path.ROOT;
        } else {
            Path parent = toPath(file.getParentFile(), true);
            return parent.child(file.getName(), index);
        }
    }

    /**
     * Locates the directory referenced by a path. If the path is an
     * index, the directory returned will be the index directory.
     * Otherwise the parent directory will be returned.
     *
     * @param path           the storage location
     *
     * @return the directory referenced by the path
     */
    private File locateDir(Path path) {
        File file = dir();
        path = path.isIndex() ? path : path.parent();
        if (!path.isRoot()) {
            for (String part : path.toIdent(0).split("/")) {
                file = FileUtil.resolve(file, part);
            }
        }
        return file;
    }

    /**
     * Locates an existing file referenced by a path. If no file
     * is named exactly as the path, the built-in storage data file
     * extensions are checked for a match.
     *
     * @param path           the storage location
     *
     * @return the file referenced by the path, or
     *         null if no existing file was found
     */
    private File locateFile(Path path) {
        File dir = locateDir(path);
        if (path.isIndex()) {
            return dir.isDirectory() && dir.canRead() ? dir : null;
        } else {
            Stream<String> alternatives = Stream.of(EXT_ALL).map(ext -> path.name() + ext);
            Stream<String> names = Stream.concat(Stream.of(path.name()), alternatives);
            Stream<File> mapped = names.map(s -> FileUtil.resolve(dir, s));
            return mapped.filter(f -> !f.isDirectory() && f.canRead()).findFirst().orElse(null);
        }
    }
}
