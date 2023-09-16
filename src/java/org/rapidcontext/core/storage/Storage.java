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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.core.data.JsonSerializer;
import org.rapidcontext.core.data.PropertiesSerializer;
import org.rapidcontext.core.data.XmlSerializer;
import org.rapidcontext.core.data.YamlSerializer;
import org.rapidcontext.core.web.Mime;

/**
 * The persistent data storage and retrieval class. This base class
 * is extended by storage services to provide actual data lookup and
 * storage.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public abstract class Storage extends StorableObject implements Comparable<Storage> {

    /**
     * The storage information path. Each storage implementation
     * should provide introspection abilities by returning it's
     * own dictionary when queried for this path.
     */
    public static final Path PATH_STORAGEINFO = Path.from("/.storageinfo");

    /**
     * The dictionary key for the read-write flag.
     */
    public static final String KEY_READWRITE = "readWrite";

    /**
     * The dictionary key for the mount path. The value stored is a
     * path object. If the storage is not mounted, this key is not
     * set or has a null value.
     */
    public static final String KEY_MOUNT_PATH = "mountPath";

    /**
     * The dictionary key for the mount timestamp. The value stored
     * is a Date object, using the current system time as default.
     */
    public static final String KEY_MOUNT_TIME = "mountTime";

    /**
     * The dictionary key for the mount overlay path. The value
     * stored is a path object indicating if the storage is also
     * mounted as a global overlay. If the storage has not been
     * mounted at all, this key is not set.
     */
    public static final String KEY_MOUNT_OVERLAY_PATH = "mountOverlayPath";

    /**
     * The dictionary key for the mount overlay priority. The value
     * stored is an integer indicating the mount overlay priority,
     * higher numbers corresponding to higher priority. If the
     * storage has not been mounted at all, this key is not set.
     */
    public static final String KEY_MOUNT_OVERLAY_PRIO = "mountOverlayPrio";

    /**
     * The file extension for properties data.
     */
    public static final String EXT_PROPERTIES = ".properties";

    /**
     * The file extension for JSON data.
     */
    public static final String EXT_JSON = ".json";

    /**
     * The file extension for XML data.
     */
    public static final String EXT_XML = ".xml";

    /**
     * The file extension for YAML data.
     */
    public static final String EXT_YAML = ".yaml";

    /**
     * The list of file extensions supported for data.
     */
    public static final String[] EXT_ALL = { EXT_PROPERTIES, EXT_JSON, EXT_XML, EXT_YAML };

    /**
     * The system time of the last mount info update. This value is
     * tracked here in order to ensure a unique timestamp for each
     * storage mount, making them deterministically comparable.
     */
    private static long lastMountTime = 0L;

    /**
     * Returns a normalized object name by removing any supported data format
     * file extension (if found).
     *
     * @param name           the filename or file path to check
     *
     * @return the filename without extension, or
     *         the input filename if not recognized
     */
    public static String objectName(String name) {
        for (String ext : EXT_ALL) {
            String str = StringUtils.removeEndIgnoreCase(name, ext);
            if (str != name) {
                return str;
            }
        }
        return name;
    }

    /**
     * Returns a normalized path by removing any supported data format file
     * extension (if found).
     *
     * @param path           the original path
     *
     * @return the path without trailing extension, or
     *         the input path if not recognized
     */
    public static Path objectPath(Path path) {
        String name = objectName(path.name());
        return name.equals(path.name()) ? path : path.sibling(name);
    }

    /**
     * Returns a serialized path by adding a supported data format file
     * extension.
     *
     * @param path           the original path
     * @param mimeType       the optional desired MIME type
     *
     * @return the path with a supported file extension, or
     *         the input path if an extension was already set
     */
    public static Path serializedPath(Path path, String mimeType) {
        if (path.name().contains(".")) {
            return path;
        } else if (mimeType != null && Mime.isMatch(mimeType, Mime.JSON)) {
            return path.sibling(path.name() + EXT_JSON);
        } else if (mimeType != null && Mime.isMatch(mimeType, Mime.XML)) {
            return path.sibling(path.name() + EXT_XML);
        } else if (mimeType != null && Mime.isMatch(mimeType, Mime.YAML)) {
            return path.sibling(path.name() + EXT_YAML);
        } else {
            return path.sibling(path.name() + EXT_PROPERTIES);
        }
    }

    /**
     * Serializes a data object into a data stream.
     *
     * @param filename       the filename (to choose format)
     * @param obj            the object to write
     * @param os             the output data stream
     *
     * @throws IOException if the serialization failed
     */
    protected static void serialize(String filename, Object obj, OutputStream os)
    throws IOException {

        if (StringUtils.endsWithIgnoreCase(filename, EXT_PROPERTIES)) {
            PropertiesSerializer.serialize(obj, os);
        } else if (StringUtils.endsWithIgnoreCase(filename, EXT_JSON)) {
            JsonSerializer.serialize(obj, os);
        } else if (StringUtils.endsWithIgnoreCase(filename, EXT_XML)) {
            XmlSerializer.serialize(obj, os);
        } else if (StringUtils.endsWithIgnoreCase(filename, EXT_YAML)) {
            YamlSerializer.serialize(obj, os);
        } else {
            throw new IOException("unsupported file type: " + filename);
        }
    }

    /**
     * Unserializes a data stream into a data object.
     *
     * @param filename       the filename (to choose format)
     * @param is             the input data stream
     *
     * @return the object read, or null if not supported
     *
     * @throws IOException if the unserialization failed
     */
    protected static Object unserialize(String filename, InputStream is)
    throws IOException {

        if (StringUtils.endsWithIgnoreCase(filename, EXT_PROPERTIES)) {
            return PropertiesSerializer.unserialize(is);
        } else if (StringUtils.endsWithIgnoreCase(filename, EXT_JSON)) {
            return JsonSerializer.unserialize(is);
        } else if (StringUtils.endsWithIgnoreCase(filename, EXT_XML)) {
            return XmlSerializer.unserialize(is);
        } else if (StringUtils.endsWithIgnoreCase(filename, EXT_YAML)) {
            return YamlSerializer.unserialize(is);
        } else {
            throw new IOException("unsupported file type: " + filename);
        }
    }

    /**
     * Creates a new storage.
     *
     * @param id             the storage identifier
     * @param storageType    the storage type name
     * @param readWrite      the read write flag
     */
    protected Storage(String id, String storageType, boolean readWrite) {
        super(id, "storage/" + storageType);
        dict.set(KEY_READWRITE, readWrite);
    }

    /**
     * Compares this storage with another.
     *
     * @param other          the object to compare with
     *
     * @return a negative integer, zero, or a positive integer as
     *         this object is less than, equal to, or greater than
     *         the specified one
     *
     * @throws ClassCastException if the object wasn't comparable
     */
    public int compareTo(Storage other) throws ClassCastException {
        int      cmp1 = mountOverlayPrio() - other.mountOverlayPrio();
        int      cmp2 = mountTime().compareTo(other.mountTime());

        if (cmp1 != 0) {
            return -cmp1;
        } else if (cmp2 != 0) {
            return cmp2;
        } else {
            return (this == other) ? 0 : -1;
        }
    }

    /**
     * Returns the storage mount path.
     *
     * @return the storage mount path, or
     *         the root path if the storage isn't mounted
     */
    public Path path() {
        return dict.get(KEY_MOUNT_PATH, Path.class, Path.ROOT);
    }

    /**
     * Returns a local storage path by removing an optional base
     * storage path. If the specified path does not have the base
     * path prefix, it is returned unmodified.
     *
     * @param path           the path to adjust
     *
     * @return the local storage path
     */
    protected Path localPath(Path path) {
        if (path != null && !path.isRoot() && path.startsWith(this.path())) {
            return path.removePrefix(this.path());
        } else {
            return path;
        }
    }

    /**
     * Returns the read-write flag.
     *
     * @return true if the storage is writable, or
     *         false otherwise
     */
    public boolean isReadWrite() {
        return dict.getBoolean(KEY_READWRITE, false);
    }

    /**
     * Returns the storage mount time.
     *
     * @return the storage mount time, or
     *         the current system time if not mounted
     */
    public Date mountTime() {
        return dict.get(KEY_MOUNT_TIME, Date.class, new Date());
    }

    /**
     * Returns the mount overlay path. This value is set if the
     * storage has been mounted as a global overlay (to the path
     * returned). If the storage has not been mounted at all, null
     * will be returned.
     *
     * @return the storage mount overlay path, or
     *         null if no mount overlay is used
     */
    public Path mountOverlayPath() {
        return dict.get(KEY_MOUNT_OVERLAY_PATH, Path.class);
    }

    /**
     * Returns the mount overlay priority. Higher numbers correspond
     * to higher priority.
     *
     * @return the mount overlay priority, or
     *         -1 if not mounted
     */
    public int mountOverlayPrio() {
        return dict.get(KEY_MOUNT_OVERLAY_PRIO, Integer.class, -1);
    }

    /**
     * Updates the mount information for this storage. This method
     * will update the mount time to the current timestamp.
     *
     * @param path           the mount path (or storage root path)
     * @param readWrite      the storage read-write flag
     * @param overlay        the mount overlay path
     * @param prio           the mount overlay priority
     */
    public void setMountInfo(Path path,
                             boolean readWrite,
                             Path overlay,
                             int prio) {

        lastMountTime = Math.max(System.currentTimeMillis(), lastMountTime + 1);
        dict.set(KEY_MOUNT_PATH, path);
        dict.set(KEY_MOUNT_TIME, new Date(lastMountTime));
        dict.set(KEY_READWRITE, readWrite);
        dict.set(KEY_MOUNT_OVERLAY_PATH, overlay);
        dict.set(KEY_MOUNT_OVERLAY_PRIO, (overlay != null) ? prio : -1);
    }

    /**
     * Returns a new storage query for this storage. Supported data
     * file extensions will automatically be hidden by this query.
     *
     * @param base           the base path (must be an index)
     *
     * @return the storage query
     */
    public Query query(Path base) {
        return new Query(this, base);
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
    public abstract Metadata lookup(Path path);

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
    public abstract Object load(Path path);

    /**
     * Loads an object from the specified location. The result must
     * be an instance of a specific class (or subclass).
     *
     * @param <T>            the type to cast objects to
     * @param path           the storage location
     * @param clazz          the class required
     *
     * @return the data read, or
     *         null if not found or unmatched type
     */
    @SuppressWarnings("unchecked")
    public <T> T load(Path path, Class<T> clazz) {
        Object obj = load(path);
        return clazz.isInstance(obj) ? (T) obj : null;
    }

    /**
     * Stores an object at the specified location. The path must
     * locate a particular object or file, since direct manipulation
     * of indices is not supported. Any previous data at the
     * specified path will be overwritten or removed.
     *
     * @param path           the storage location
     * @param data           the data to store
     *
     * @throws StorageException if the data couldn't be written
     */
    public abstract void store(Path path, Object data) throws StorageException;

    /**
     * Removes an object or an index at the specified location. If
     * the path refers to an index, all contained objects and indices
     * will be removed recursively.
     *
     * @param path           the storage location
     *
     * @throws StorageException if the data couldn't be removed
     */
    public abstract void remove(Path path) throws StorageException;
}
