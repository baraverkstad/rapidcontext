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
public class Index extends Dict {

    /**
     * The dictionary key for the path.
     */
    public static final String KEY_PATH = "path";

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
     * Creates a new index dictionary.
     *
     * @param path           the index path
     */
    public Index(Path path) {
        this(path, new Array(), new Array());
    }

    /**
     * Creates a new index dictionary with the specified entries.
     *
     * @param path           the index path
     * @param indices        the initial index array
     * @param objects        the initial object array
     */
    public Index(Path path, Array indices, Array objects) {
        super(4);
        add("type", "index");
        add(KEY_PATH, path);
        add(KEY_IDXS, indices);
        add(KEY_OBJS, objects);
    }

    /**
     * Returns the index path.
     *
     * @return the index path
     */
    public Path path() {
        return (Path) get(KEY_PATH);
    }

    /**
     * Returns an array of sub-index names.
     *
     * @return an array of sub-index names
     */
    public Array indices() {
        return (Array) get(KEY_IDXS);
    }

    /**
     * Returns an array of object names.
     *
     * @return an array of object names
     */
    public Array objects() {
        return (Array) get(KEY_OBJS);
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
