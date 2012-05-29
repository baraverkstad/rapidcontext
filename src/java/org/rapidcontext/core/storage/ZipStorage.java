/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2012 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang.StringUtils;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.PropertiesSerializer;
import org.rapidcontext.core.web.Mime;

/**
 * A persistent data storage and retrieval handler based on a ZIP
 * file. This class will read both Java property files and binary
 * files depending on the file extension. The property files are
 * converted to dictionary object on retrieval.<p>
 *
 * Note: This storage is read-only. Unpack the ZIP file and use a
 * DirStorage for read-write access.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ZipStorage extends Storage {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(ZipStorage.class.getName());

    /**
     * The ZIP file being read.
     */
    protected File file;

    /**
     * The ZIP file used for locating resources.
     */
    protected ZipFile zip;

    /**
     * The ZIP entries and index map. Indexed by the storage path and
     * linked to either the Index or the ZipEntry objects.
     */
    protected HashMap entries = new HashMap();

    /**
     * Creates a new read-only ZIP file storage.
     *
     * @param zipFile        the ZIP file to use
     *
     * @throws IOException if the ZIP couldn't be opened properly
     */
    public ZipStorage(File zipFile) throws IOException {
        super("zip", false);
        this.file = zipFile;
        this.zip = new ZipFile(zipFile);
        init();
    }

    /**
     * Initializes this object. This method locates all the ZIP file
     * entries and creates all the indexes.
     */
    public void init() {
        Index idx = new Index(Path.ROOT);
        idx.addObject(PATH_STORAGEINFO.name());
        idx.updateLastModified(new Date(file.lastModified()));
        entries.put(Path.ROOT, idx);
        Enumeration e = zip.entries();
        while (e.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) e.nextElement();
            String name = entry.getName();
            if (entry.isDirectory() && !name.endsWith("/")) {
                name += "/";
            } else {
                name = StringUtils.removeEnd(name, DirStorage.SUFFIX_PROPS);
            }
            Path path = new Path(name);
            Path parent = path.parent();
            idx = (Index) entries.get(parent);
            if (path.isIndex()) {
                idx.addIndex(path.name());
                idx = new Index(path);
                idx.updateLastModified(new Date(entry.getTime()));
                entries.put(path, idx);
            } else {
                idx.addObject(path.name());
                entries.put(path, entry);
            }
        }
    }

    /**
     * Destroys this object. This method is used to free any
     * resources used when this object is no longer used.
     *
     * @throws StorageException if the destruction failed
     */
    public void destroy() throws StorageException {
        try {
            entries.clear();
            zip.close();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed to closed zip file: " + file, e);
        }
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
        if (PATH_STORAGEINFO.equals(path)) {
            return new Metadata(Dict.class, path, path(), mountTime());
        }
        Object obj = entries.get(path);
        if (obj instanceof Index) {
            Index idx = (Index) obj;
            return new Metadata(Index.class, path, path(), idx.lastModified());
        } else if (obj instanceof ZipEntry) {
            ZipEntry entry = (ZipEntry) obj;
            if (entry.getName().endsWith(DirStorage.SUFFIX_PROPS)) {
                return new Metadata(Dict.class, path, path(), entry.getTime());
            } else {
                return new Metadata(Binary.class, path, path(), entry.getTime());
            }
        } else {
            return null;
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
        if (PATH_STORAGEINFO.equals(path)) {
            return dict;
        }
        Object obj = entries.get(path);
        if (obj instanceof Index) {
            return obj;
        } else if (obj instanceof ZipEntry) {
            ZipEntry entry = (ZipEntry) obj;
            if (entry.getName().endsWith(DirStorage.SUFFIX_PROPS)) {
                try {
                    return PropertiesSerializer.read(zip, entry);
                } catch (IOException e) {
                    String msg = "failed to read ZIP file " + zip + ":" + entry;
                    LOG.log(Level.SEVERE, msg, e);
                    return null;
                }
            } else {
                return new ZipBinary(entry);
            }
        } else {
            return null;
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
        String msg = "cannot store to read-only storage at " + path();
        LOG.warning(msg);
        throw new StorageException(msg);
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
        String msg = "cannot remove from read-only storage at " + path();
        LOG.warning(msg);
        throw new StorageException(msg);
    }

    /**
     * An encapsulated ZIP file entry for binary data.
     */
    private class ZipBinary implements Binary {

        /**
         * The encapsulated ZIP file entry.
         */
        private ZipEntry entry;

        /**
         * Creates a new ZIP binary data object.
         *
         * @param entry          the zip file entry
         */
        public ZipBinary(ZipEntry entry) {
            this.entry = entry;
        }

        /**
         * Returns the size (in bytes) of the binary object, if known.
         *
         * @return the object size (in bytes), or
         *         -1 if unknown
         */
        public long size() {
            return entry.getSize();
        }

        /**
         * The last modified timestamp for the object, if known.
         *
         * @return the last modified timestamp, or
         *         negative or the current system if unknown
         */
        public long lastModified() {
            return entry.getTime();
        }

        /**
         * The MIME type of the binary data. Use a standard opaque
         * binary data MIME type or one based on requested file name
         * if unknown.
         *
         * @return the MIME type of the binary data
         */
        public String mimeType() {
            return Mime.type(entry.getName());
        }

        /**
         * Opens a new input stream for reading the data. Note that this
         * method SHOULD be possible to call several times.
         *
         * @return a new input stream for reading the binary data
         *
         * @throws IOException if the data couldn't be opened for reading
         */
        public InputStream openStream() throws IOException {
            return zip.getInputStream(entry);
        }
    }
}
