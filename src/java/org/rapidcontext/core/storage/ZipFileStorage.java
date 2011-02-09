/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2011 Per Cederberg. All rights reserved.
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
 * FileStorage for read-write access.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ZipFileStorage extends Storage {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(ZipFileStorage.class.getName());

    /**
     * The ZIP file being read.
     */
    protected File file;

    /**
     * The ZIP file used for locating resources.
     */
    protected ZipFile zip;

    /**
     * Creates a new read-only ZIP file storage.
     *
     * @param zipFile        the ZIP file to use
     *
     * @throws IOException if the ZIP couldn't be opened properly
     */
    public ZipFileStorage(File zipFile) throws IOException {
        super("zipFile", false);
        this.file = zipFile;
        this.zip = new ZipFile(zipFile);
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
        ZipEntry  entry;

        if (PATH_STORAGEINFO.equals(path)) {
            return new Metadata(Dict.class, path, path(), mountTime());
        }
        entry = locateEntry(path);
        if (path.isRoot()) {
            return new Metadata(Index.class, path, path(), file.lastModified());
        } else if (entry == null) {
            return null;
        } else if (entry.isDirectory()) {
            return new Metadata(Index.class, path, path(), entry.getTime());
        } else if (entry.getName().endsWith(FileStorage.SUFFIX_PROPS)) {
            return new Metadata(Dict.class, path, path(), entry.getTime());
        } else {
            return new Metadata(Binary.class, path, path(), entry.getTime());
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
        ZipEntry  entry;
        String    msg;

        if (PATH_STORAGEINFO.equals(path)) {
            return dict;
        }
        entry = locateEntry(path);
        if (path.isRoot()) {
            Index idx = new Index(path);
            idx.addObject(PATH_STORAGEINFO.name());
            locateEntries(null, idx);
            idx.updateLastModified(new Date(file.lastModified()));
            return idx;            
        } else if (entry == null) {
            return null;
        } else if (path.isIndex()) {
            Index idx = new Index(path);
            locateEntries(entry, idx);
            idx.updateLastModified(new Date(entry.getTime()));
            return idx;
        } else if (entry.getName().endsWith(FileStorage.SUFFIX_PROPS)) {
            try {
                return PropertiesSerializer.read(zip, entry);
            } catch (IOException e) {
                msg = "failed to read ZIP file " + zip + ":" + entry;
                LOG.log(Level.SEVERE, msg, e);
                return null;
            }
        } else {
            return new ZipBinary(entry);
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
     * Locates the ZIP file entry by the specified path. If no entry
     * is named exactly as the path, the properties file extension is
     * appended to the name.
     *
     * @param path           the storage location
     *
     * @return the file referenced by the path, or
     *         null if no existing file was found
     */
    private ZipEntry locateEntry(Path path) {
        String    name = StringUtils.removeStart(path.toString(), "/");
        ZipEntry  entry = zip.getEntry(name);

        if (!path.isIndex() && entry == null) {
            entry = zip.getEntry(name + FileStorage.SUFFIX_PROPS);
        }
        if (entry == null || entry.isDirectory() != path.isIndex()) {
            return null;
        } else {
            return entry;
        }
    }

    /**
     * Locates the immediate child ZIP entries and adds them to the
     * specified index.
     *
     * @param parent         the parent ZIP entry
     * @param idx            the index to add entries to
     */
    private void locateEntries(ZipEntry parent, Index idx) {
        Enumeration  e = zip.entries();
        String       dirName = (parent == null) ? "" : parent.getName();

        while (e.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) e.nextElement();
            String name = entry.getName();
            if (name.startsWith(dirName) && name.length() > dirName.length()) {
                name = name.substring(dirName.length());
                int seps = StringUtils.countMatches(name, "/");
                if (seps == 0) {
                    name = StringUtils.removeEnd(name, FileStorage.SUFFIX_PROPS);
                    idx.addObject(name);
                } else if (seps == 1 && name.endsWith("/")) {
                    idx.addIndex(StringUtils.removeEnd(name, "/"));
                }
            }
        }
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
