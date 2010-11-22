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

package org.rapidcontext.core.storage;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;

/**
 * The persistent data storage and retrieval class. This base class
 * is extended by storage services to provide actual data lookup and
 * storage.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public abstract class Storage extends StorableObject implements Comparable {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(Storage.class.getName());

    /**
     * The dictionary key for the storage type.
     */
    public static final String KEY_STORAGE_TYPE = "storageType";

    /**
     * The dictionary key for the read-write flag.
     */
    public static final String KEY_READWRITE = "readWrite";

    /**
     * The dictionary key for the mount path. The value stored is a
     * path object, using the root path by default.
     */
    public static final String KEY_MOUNT_PATH = "mountPath";

    /**
     * The dictionary key for the mount timestamp. The value stored
     * is a Date object, using the current system time as default.
     */
    public static final String KEY_MOUNT_TIME = "mountTime";

    /**
     * The dictionary key for the mount overlay flag. The value
     * stored is a boolean indicating if the storage has been mounted
     * as a global overlay or not. If the storage has not been
     * mounted at all, this key is not set.
     */
    public static final String KEY_MOUNT_OVERLAY = "mountOverlay";

    /**
     * The dictionary key for the mount overlay priority. The value
     * stored is an integer indicating the mount overlay priority,
     * higher numbers corresponding to higher priority. If the
     * storage has not been mounted at all, this key is not set.
     */
    public static final String KEY_MOUNT_OVERLAY_PRIO = "mountOverlayPrio";

    /**
     * The storage information path. Each storage implementation
     * should provide introspection abilities by returning it's
     * own dictionary when queried for this path.
     */
    public static final Path PATH_STORAGEINFO = new Path("/storageinfo");

    /**
     * The storable object initializer constructor arguments.
     */
    private static final Class[] CONSTRUCTOR_ARGS = new Class[] {
        String.class, String.class, Dict.class
    };

    /**
     * The map of object initializer classes. This maps object types
     * to their corresponding Java classes. The map is indexed by the
     * type identifier.
     */
    private static HashMap initializers = new HashMap();

    /**
     * Returns the initializer class for a specified object type.
     *
     * @param typeId         the object type identifier
     *
     * @return the initializer class found, or
     *         null if not found
     */
    public static Class initializer(String typeId) {
        return (Class) initializers.get(typeId);
    }

    /**
     * Returns the initializer class for a specified dictionary.
     *
     * @param dict           the dictionary data
     *
     * @return the initializer class found, or
     *         null if not found
     */
    public static Class initializer(Dict dict) {
        if (dict == null) {
            return null;
        }
        return initializer(dict.getString(KEY_TYPE, null));
    }

    /**
     * Initializes an object if a proper initializer is found. If no
     * initializer was found or if an error occurred, the input
     * dictionary data is returned.
     *
     * @param id             the object identifier
     * @param dict           the dictionary data
     *
     * @return the initialized StorableObject instance, or
     *         the specified dictionary
     */
    public static Object initialize(String id, Dict dict) {
        String          typeId = dict.getString(KEY_TYPE, null);
        Class           cls = initializer(dict);
        Constructor     constr;
        Object[]        args;
        StorableObject  obj;
        String          msg;

        if (cls == null) {
            return dict;
        }
        try {
            constr = cls.getConstructor(CONSTRUCTOR_ARGS);
            args = new Object[] { id, typeId, dict };
            obj = (StorableObject) constr.newInstance(args);
            obj.init();
            return obj;
        } catch (Exception e) {
            msg = "failed to create instance of " + cls.getName() +
                  " for object " + id + " of type " + typeId;
            LOG.log(Level.WARNING, msg, e);
            return dict;
        }
    }

    /**
     * Registers an initializer class for the specified object type.
     * The initializer class must be a StorableObject subclass and
     * provided the proper constructor (both checked by this method).
     *
     * @param typeId         the object type identifier
     * @param cls            the initializer class
     *
     * @throws StorageException if the initializer class wasn't
     *             valid
     */
    public static void registerInitializer(String typeId, Class cls)
    throws StorageException {

        String  msg;

        if (!StorableObject.class.isAssignableFrom(cls)) {
            msg = "invalid initializer class for type " + typeId + ": " +
                  "class " + cls.getName() + " is not a subclass of " +
                  "StorableObject";
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        try {
            cls.getConstructor(CONSTRUCTOR_ARGS);
        } catch (Exception e) {
            msg = "invalid initializer class for type " + typeId + ": " +
                  "no constructor " + cls.getName() +
                  "(String, String, Dict) found";
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        initializers.put(typeId, cls);
    }

    /**
     * Removes a previously registered initializer class.
     *
     * @param typeId         the object type identifier
     */
    public static void unregisterInitializer(String typeId) {
        initializers.remove(typeId);
    }

    /**
     * Creates a new storage.
     *
     * @param storageType    the storage type name
     * @param readWrite      the read write flag
     */
    protected Storage(String storageType, boolean readWrite) {
        super(null, "storage");
        dict.remove(KEY_ID);
        dict.set(KEY_STORAGE_TYPE, storageType);
        dict.setBoolean(KEY_READWRITE, readWrite);
        dict.set(KEY_MOUNT_PATH, Path.ROOT);
        dict.set(KEY_MOUNT_TIME, new Date());
    }

    /**
     * Compares this storage with another.
     *
     * @param obj            the object to compare with
     *
     * @return a negative integer, zero, or a positive integer as
     *         this object is less than, equal to, or greater than
     *         the specified one
     *
     * @throws ClassCastException if the object wasn't comparable
     */
    public int compareTo(Object obj) throws ClassCastException {
        Storage  other = (Storage) obj;
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
     * Returns the storage type name.
     *
     * @return the storage type name
     */
    public String storageType() {
        return dict.getString(KEY_STORAGE_TYPE, null);
    }

    /**
     * Returns the storage mount path.
     *
     * @return the storage mount path
     */
    public Path path() {
        return (Path) dict.get(KEY_MOUNT_PATH);
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
        if (path != null && !path.isRoot() && path.startsWith(path())) {
            return path.subPath(path().length());
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
     * Returns the storage mount or creation time.
     *
     * @return the storage mount or creation time
     */
    public Date mountTime() {
        return (Date) dict.get(KEY_MOUNT_TIME);
    }

    /**
     * Returns the mount overlay flag. This flag is set if the
     * storage has been mounted as a global overlay. If the storage
     * has not been mounted at all, false will be returned.
     *
     * @return true if the storage is mounted as a global overlay, or
     *         false otherwise
     */
    public boolean mountOverlay() {
        return dict.getBoolean(KEY_MOUNT_OVERLAY, false);
    }

    /**
     * Returns the mount overlay priority. Higher numbers correspond
     * to higher priority.
     *
     * @return the mount overlay priority, or
     *         -1 if not mounted
     */
    public int mountOverlayPrio() {
        return dict.getInt(KEY_MOUNT_OVERLAY_PRIO, -1);
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
     * Searches for all objects at the specified location and returns
     * metadata about the ones found. The path may locate either an
     * index or a specific object. Any indices found by this method
     * will be traversed recursively instead of being returned.
     *
     * @param path           the storage location
     *
     * @return the array of object metadata, or
     *         an empty array if no objects were found
     */
    public Metadata[] lookupAll(Path path) {
        ArrayList  list = new ArrayList();

        lookupAll(path, list);
        return (Metadata[]) list.toArray(new Metadata[list.size()]);
    }

    /**
     * Searches for all objects at the specified location and returns
     * metadata about the ones found. The path may locate either an
     * index or a specific object. Any indices found by this method
     * will be traversed recursively instead of being returned.
     *
     * @param path           the storage location
     * @param list           the list where to add results
     */
    private void lookupAll(Path path, ArrayList list) {
        Metadata  meta;
        Index     idx;
        Array     arr;
        Path      child;

        meta = lookup(path);
        if (meta != null && meta.isIndex()) {
            idx = (Index) load(path);
            arr = idx.indices();
            for (int i = 0; arr != null && i < arr.size(); i++) {
                child = path.child(arr.getString(i, null), true);
                lookupAll(child, list);
            }
            arr = idx.objects();
            for (int i = 0; arr != null && i < arr.size(); i++) {
                child = path.child(arr.getString(i, null), false);
                lookupAll(child, list);
            }
        } else if (meta != null) {
            list.add(meta);
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
    public abstract Object load(Path path);

    /**
     * Loads all object from the specified location. The path may
     * locate either an index or a specific object. Any indices
     * found by this method will be traversed recursively instead
     * of being returned.
     *
     * @param path           the storage location
     *
     * @return the array of data read, or
     *         an empty array if no objects were found
     */
    public Object[] loadAll(Path path) {
        ArrayList  list = new ArrayList();

        loadAll(path, list);
        return list.toArray();
    }

    /**
     * Loads all object from the specified location. The path may
     * locate either an index or a specific object. Any indices
     * found by this method will be traversed recursively instead
     * of being returned.
     *
     * @param path           the storage location
     * @param list           the list where to add results
     */
    private void loadAll(Path path, ArrayList list) {
        Object  obj;
        Index   idx;
        Array   arr;
        Path    child;

        obj = load(path);
        if (obj != null && obj instanceof Index) {
            idx = (Index) obj;
            arr = idx.indices();
            for (int i = 0; arr != null && i < arr.size(); i++) {
                child = path.child(arr.getString(i, null), true);
                loadAll(child, list);
            }
            arr = idx.objects();
            for (int i = 0; arr != null && i < arr.size(); i++) {
                child = path.child(arr.getString(i, null), false);
                loadAll(child, list);
            }
        } else if (obj != null) {
            list.add(obj);
        }
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
