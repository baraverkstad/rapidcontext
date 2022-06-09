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
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * An index dictionary. An index is an object containing the names
 * of objects and sub-indices.<p>
 *
 * IMPORTANT: The index objects shouldn't be modified directly by
 * outside the owning storage implementation.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Index {

    /**
     * The storage path for this index.
     */
    private Path path;

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
            Index res = new Index(one.path());
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
     * @param path           the storage path for the index
     */
    public Index(Path path) {
        this.path = path;
        this.modified = new Date();
        this.indices = new TreeSet<>();
        this.objects = new TreeSet<>();
    }

    /**
     * Creates an index copy with a different path. Note that the
     * source index content will be referenced, not copied. So any
     * changes will propagate to both indices.
     *
     * @param path           the storage path for the index
     * @param source         the source index to copy
     */
    public Index(Path path, Index source) {
        this.path = path;
        this.modified = new Date();
        this.indices = source.indices;
        this.objects = source.objects;
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
     * Returns the storage path for the index.
     *
     * @return the storage path for the index
     */
    public Path path() {
        return this.path;
    }

    /**
     * Returns the last modified date.
     *
     * @return the last modified date
     */
    public Date lastModified() {
        return this.modified;
    }

    /**
     * Updates the last modified date.
     *
     * @param date           the date to set, or null for now
     */
    public void updateLastModified(Date date) {
        this.modified = (date == null) ? new Date() : date;
    }

    /**
     * Returns an array of sub-index names.
     *
     * @return an array of sub-index names
     */
    public Stream<String> indices() {
        return this.indices.stream();
    }

    /**
     * Returns an array of object names.
     *
     * @return an array of object names
     */
    public Stream<String> objects() {
        return this.objects.stream();
    }

    /**
     * Returns a stream of paths corresponding to all indices and
     * objects in this index.
     *
     * @return a stream path objects
     */
    public Stream<Path> paths() {
        return Stream.concat(
            indices().map((item) -> this.path.child(item, true)),
            objects().map((item) -> this.path.child(item, false))
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
