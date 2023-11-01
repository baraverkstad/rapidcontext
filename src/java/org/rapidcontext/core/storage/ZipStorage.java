/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2023 Per Cederberg. All rights reserved.
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
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.util.BinaryUtil;

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
     * The dictionary key for the ZIP file location. The value stored
     * is a file object.
     */
    public static final String KEY_FILE = "file";

    /**
     * The ZIP file used for locating resources.
     */
    protected ZipFile zip;

    /**
     * The ZIP storage paths (identity) map. This is used to normalize paths
     * for case-insensitive matching. HashMap objects don't allow fast access
     * to hash keys or entries (only values), so a separate map is required.
     */
    protected HashMap<Path,Path> paths = new HashMap<>();

    /**
     * The ZIP entries and index map. Indexed by the storage path and
     * linked to either the Index or the ZipEntry objects.
     */
    protected HashMap<Path,Object> entries = new HashMap<>();

    /**
     * Creates a new read-only ZIP file storage.
     *
     * @param zipFile        the ZIP file to use
     *
     * @throws IOException if the ZIP couldn't be opened properly
     */
    public ZipStorage(File zipFile) throws IOException {
        super(StringUtils.removeEnd(zipFile.getName(), ".zip"), "zip", false);
        dict.set(KEY_FILE, zipFile);
        this.zip = new ZipFile(zipFile);
        init();
    }

    /**
     * Initializes this object. This method locates all the ZIP file
     * entries and creates all the index objects.
     */
    @Override
    public final void init() {
        Index root = new Index(new Date(file().lastModified()));
        root.addObject(PATH_STORAGEINFO.name());
        paths.put(Path.ROOT, Path.ROOT);
        entries.put(Path.ROOT, root);
        Enumeration<? extends ZipEntry> e = zip.entries();
        while (e.hasMoreElements()) {
            ZipEntry entry = e.nextElement();
            String name = entry.getName();
            if (entry.isDirectory() && !name.endsWith("/")) {
                name += "/";
            }
            Path path = Path.from(name);
            Index idx = (Index) entries.get(path.parent());
            if (path.isIndex()) {
                idx.addIndex(path.name());
                idx = new Index(new Date(entry.getTime()));
                paths.put(path, path);
                entries.put(path, idx);
            } else {
                idx.addObject(path.name());
                paths.put(path, path);
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
    @Override
    public void destroy() throws StorageException {
        try {
            paths.clear();
            entries.clear();
            zip.close();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed to close zip file: " + file(), e);
        }
    }

    /**
     * Returns the ZIP file being read by this storage.
     *
     * @return the ZIP file being read
     */
    public final File file() {
        return dict.get(KEY_FILE, File.class);
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
            Metadata m = new Metadata(Dict.class, PATH_STORAGEINFO, path());
            return m.modified(mountTime());
        }
        Path match = locatePath(path);
        Object obj = (match == null) ? null : entries.get(match);
        if (obj instanceof Index) {
            Metadata m = new Metadata(Index.class, match, path());
            return m.modified(((Index) obj).modified());
        } else if (obj instanceof ZipEntry) {
            ZipEntry entry = (ZipEntry) obj;
            Metadata m = path.equals(match) ?
                new Metadata(Binary.class, match, path()) :
                new Metadata(Dict.class, objectPath(match), path());
            m.mimeType(Mime.type(entry.getName()));
            m.modified(new Date(entry.getTime()));
            m.size(entry.getSize());
            return m;
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
    @Override
    public Object load(Path path) {
        if (PATH_STORAGEINFO.equals(path)) {
            return dict;
        }
        Path match = locatePath(path);
        Object obj = (match == null) ? null : entries.get(match);
        if (obj instanceof Index) {
            return obj;
        } else if (obj instanceof ZipEntry) {
            ZipEntry entry = (ZipEntry) obj;
            if (!path.equals(match)) {
                try (InputStream is = zip.getInputStream(entry)) {
                    return unserialize(entry.getName(), is);
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
    @Override
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
    @Override
    public void remove(Path path) throws StorageException {
        String msg = "cannot remove from read-only storage at " + path();
        LOG.warning(msg);
        throw new StorageException(msg);
    }

    /**
     * Searches for an existing path in the ZIP file. If no exact match is
     * found, the storage data file extensions are tried for a match.
     *
     * @param path           the storage location
     *
     * @return an existing path in the ZIP file, or
     *         null if no match was found
     */
    protected Path locatePath(Path path) {
        return Stream.concat(
            Stream.of(paths.get(path)),
            Stream.of(EXT_ALL).map(ext -> paths.get(path.sibling(path.name() + ext)))
        ).filter(Objects::nonNull).findFirst().orElse(null);
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
        @Override
        public long size() {
            return entry.getSize();
        }

        /**
         * The last modified timestamp for the object, if known.
         *
         * @return the last modified timestamp, or
         *         negative or the current system if unknown
         */
        @Override
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
        @Override
        public String mimeType() {
            return Mime.type(entry.getName());
        }

        /**
         * The SHA-256 of the binary data, if known.
         *
         * @return the hexadecimal string with the SHA-256 hash, or
         *         null if not available
         */
        @Override
        public String sha256() {
            try (InputStream input = openStream()) {
                return BinaryUtil.hashSHA256(input);
            } catch (NoSuchAlgorithmException | IOException e) {
                return null;
            }
        }

        /**
         * Opens a new input stream for reading the data. Note that this
         * method SHOULD be possible to call several times.
         *
         * @return a new input stream for reading the binary data
         *
         * @throws IOException if the data couldn't be opened for reading
         */
        @Override
        public InputStream openStream() throws IOException {
            return zip.getInputStream(entry);
        }
    }
}
