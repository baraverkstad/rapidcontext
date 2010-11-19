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

import java.util.ArrayList;

import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.DynamicObject;

/**
 * The persistent data storage and retrieval class. This base class
 * is extended by storage services to provide actual data lookup and
 * storage.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public abstract class Storage extends DynamicObject {

    /**
     * The dictionary key for the storage type.
     */
    public static final String KEY_STORAGE_TYPE = "storageType";

    /**
     * The dictionary key for the base storage path.
     */
    public static final String KEY_PATH = "path";

    /**
     * The dictionary key for the read-write flag.
     */
    public static final String KEY_READWRITE = "readWrite";

    /**
     * Creates a new storage.
     *
     * @param storageType    the storage type name
     * @param path           the base storage path
     * @param readWrite      the read write flag
     */
    protected Storage(String storageType, Path path, boolean readWrite) {
        super("storage");
        dict.set(KEY_STORAGE_TYPE, storageType);
        dict.set(KEY_PATH, path);
        dict.setBoolean(KEY_READWRITE, readWrite);
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
     * Returns the base storage path.
     *
     * @return the base storage path
     */
    public Path path() {
        return (Path) dict.get(KEY_PATH);
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
