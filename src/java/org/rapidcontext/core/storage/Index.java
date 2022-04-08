/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2022 Per Cederberg. All rights reserved.
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

import java.util.Date;

import org.rapidcontext.core.data.Array;

/**
 * An index dictionary. An index is an object containing the names
 * of objects and sub-indices.<p>
 *
 * IMPORTANT: The index objects shouldn't be modified directly by
 * outside the owning storage implementation. Use the copy() method
 * to create a copy if changes need to be made elsewhere.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Index extends StorableObject {

    /**
     * The dictionary key for the storage path. The value stored is a
     * Path object.
     */
    public static final String KEY_PATH = "path";

    /**
     * The dictionary key for the last modified date. The value stored
     * is a Date object.
     */
    public static final String KEY_MODIFIED = "lastModified";

    /**
     * The dictionary key for the array of indices.
     */
    public static final String KEY_IDXS = "indices";

    /**
     * The dictionary key for the array of objects.
     */
    public static final String KEY_OBJS = "objects";

    /**
     * Merges two index dictionaries. Note that this may modify one
     * of the two directories.
     *
     * @param one            the first index dictionary
     * @param two            the second index dictionary
     *
     * @return the merged index
     */
    public static Index merge(Index one, Index two) {
        if (one == null) {
            return two;
        } else if (two == null) {
            return one;
        } else {
            Array idxs = one.indices().union(two.indices());
            Array objs = one.objects().union(two.objects());
            return new Index(one.path(), idxs, objs);
        }
    }

    /**
     * Creates a new empty index.
     *
     * @param path           the storage path for the index
     */
    public Index(Path path) {
        this(path, new Array(), new Array());
    }

    /**
     * Creates a new index with the specified entries.
     *
     * @param path           the storage path for the index
     * @param indices        the initial index array
     * @param objects        the initial object array
     */
    public Index(Path path, Array indices, Array objects) {
        super(null, "index");
        dict.remove(KEY_ID);
        dict.set(KEY_PATH, path);
        updateLastModified(null);
        dict.set(KEY_IDXS, indices);
        dict.set(KEY_OBJS, objects);
    }

    /**
     * Returns the storage path for the index.
     *
     * @return the storage path for the index
     */
    public Path path() {
        return (Path) dict.get(KEY_PATH);
    }

    /**
     * Returns the last modified date.
     *
     * @return the last modified date
     */
    public Date lastModified() {
        return (Date) dict.get(KEY_MODIFIED);
    }

    /**
     * Updates the last modified date.
     *
     * @param date           the date to set, or null for now
     */
    public void updateLastModified(Date date) {
        dict.set(KEY_MODIFIED, (date == null) ? new Date() : date);
    }

    /**
     * Returns an array of sub-index names.
     *
     * @return an array of sub-index names
     */
    public Array indices() {
        return dict.getArray(KEY_IDXS);
    }

    /**
     * Returns an array of object names.
     *
     * @return an array of object names
     */
    public Array objects() {
        return dict.getArray(KEY_OBJS);
    }

    /**
     * Returns an array of paths corresponding to all sub-indexes and
     * objects in this index.
     *
     * @return an array of path objects
     */
    public Array paths() {
        Array res = new Array();
        Path path = path();
        for (Object o : indices()) {
            res.add(path.child(o.toString(), true));
        }
        for (Object o : objects()) {
            res.add(path.child(o.toString(), false));
        }
        return res;
    }

    /**
     * Adds a sub-index name. The name will only be added only if not
     * already in the index.
     *
     * @param name           the index name
     *
     * @return true if the name was added to the index, or
     *         false otherwise
     */
    public boolean addIndex(String name) {
        Array arr = indices();
        if (!arr.containsValue(name)) {
            arr.add(name);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Adds an object name. The name will only be added only if not
     * already in the index.
     *
     * @param name           the object name
     *
     * @return true if the name was added to the index, or
     *         false otherwise
     */
    public boolean addObject(String name) {
        Array arr = objects();
        if (!arr.containsValue(name)) {
            arr.add(name);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes a sub-index name.
     *
     * @param name           the index name
     *
     * @return true if the name was removed from the index, or
     *         false otherwise
     */
    public boolean removeIndex(String name) {
        Array arr = indices();
        int pos = arr.indexOf(name);
        if (arr.containsIndex(pos)) {
            arr.remove(pos);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes an object name.
     *
     * @param name           the object name
     *
     * @return true if the name was removed from the index, or
     *         false otherwise
     */
    public boolean removeObject(String name) {
        Array arr = objects();
        int pos = arr.indexOf(name);
        if (arr.containsIndex(pos)) {
            arr.remove(pos);
            return true;
        } else {
            return false;
        }
    }
}
