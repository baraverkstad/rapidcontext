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

import java.util.Date;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.commons.lang3.ObjectUtils;

/**
 * An index dictionary. An index is an object containing the names
 * of objects and sub-indices.<p>
 *
 * IMPORTANT: The index objects shouldn't be modified directly by
 * outside the owning storage implementation.
 *
 * @author Per Cederberg
 */
public class Index {

    /**
     * The last modified date for changes to the index.
     */
    private Date modified;

    /**
     * The set of sub-indices contained.
     */
    private TreeSet<String> indices;

    /**
     * The set of objects contained.
     */
    private TreeSet<String> objects;

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
            Index res = new Index(ObjectUtils.max(one.modified, two.modified));
            res.indices.addAll(one.indices);
            res.indices.addAll(two.indices);
            res.objects.addAll(one.objects);
            res.objects.addAll(two.objects);
            return res;
        }
    }

    /**
     * Creates a new empty index.
     *
     * @param modified       the last modified date, or null for unknown
     */
    public Index(Date modified) {
        this.modified = modified;
        this.indices = new TreeSet<>();
        this.objects = new TreeSet<>();
    }

    /**
     * Creates a new index that is a copy of another one. Optionally,
     * all object names can be normalized with Storage.objectName().
     *
     * @param src            the source index to copy
     * @param normalize      the normalize object names flag
     */
    public Index(Index src, boolean normalize) {
        this.modified = src.modified;
        this.indices = new TreeSet<>(src.indices);
        if (normalize) {
            this.objects = new TreeSet<>();
            for (String str : src.objects) {
                this.objects.add(Storage.objectName(str));
            }
        } else {
            this.objects = new TreeSet<>(src.objects);
        }
    }

    /**
     * Checks if this index is empty.
     *
     * @return true if the index is empty, or false otherwise
     */
    public boolean isEmpty() {
        return this.indices.size() + this.objects.size() == 0;
    }

    /**
     * Returns the last modified date.
     *
     * @return the last modified date, or null if unknown
     */
    public Date modified() {
        return this.modified;
    }

    /**
     * Updates the last modified date.
     *
     * @param date           the date to set, or null for now
     */
    public void setModified(Date date) {
        this.modified = (date == null) ? new Date() : date;
    }

    /**
     * Checks if a specified sub-index name is present.
     *
     * @param name           the sub-index name to check
     *
     * @return true if the name is found, or false otherwise
     */
    public boolean hasIndex(String name) {
        return this.indices.contains(name);
    }

    /**
     * Checks if a specified object name is present.
     *
     * @param name           the object name to check
     *
     * @return true if the name is found, or false otherwise
     */
    public boolean hasObject(String name) {
        return this.objects.contains(name);
    }

    /**
     * Returns an array of sub-index names.
     *
     * @param hidden         include hidden paths
     *
     * @return an array of sub-index names
     */
    public Stream<String> indices(boolean hidden) {
        return this.indices.stream()
            .filter((s) -> hidden || !s.startsWith(Path.PREFIX_HIDDEN));
    }

    /**
     * Returns an array of object names.
     *
     * @param hidden         include hidden paths
     *
     * @return an array of object names
     */
    public Stream<String> objects(boolean hidden) {
        return this.objects.stream()
            .filter((s) -> hidden || !s.startsWith(Path.PREFIX_HIDDEN));
    }

    /**
     * Returns a stream of paths corresponding to all indices and
     * objects in this index.
     *
     * @param basePath       the path of this index
     * @param hidden         include hidden paths
     *
     * @return a stream path objects
     */
    public Stream<Path> paths(Path basePath, boolean hidden) {
        return Stream.concat(
            indices(hidden).map((item) -> basePath.child(item, true)),
            objects(hidden).map((item) -> basePath.child(item, false))
        );
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
        return this.indices.add(name);
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
        return this.objects.add(name);
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
        return this.indices.remove(name);
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
        return this.objects.remove(name);
    }
}
