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

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.rapidcontext.core.security.SecurityContext;

/**
 * A storage query for streaming metadata or data results. Searches
 * are performed depth-first and will only returns objects (or their
 * metadata), omitting indices.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Query {

    /**
     * The storage to query.
     */
    private Storage storage;

    /**
     * The base storage path.
     */
    private Path base;

    /**
     * The include hidden paths flag.
     */
    private boolean hidden = false;

    /**
     * The maximum path depth to return;
     */
    private int maxDepth = Integer.MAX_VALUE;

    /**
     * The path predicate filters to apply.
     */
    private ArrayList<Predicate<Path>> filters = new ArrayList<>();

    /**
     * Creates a new query for the specified storage.
     *
     * @param storage        the storage to query
     * @param base           the base path
     */
    public Query(Storage storage, Path base) {
        this.storage = storage;
        this.base = base;
    }

    /**
     * Adds a path predicate filter to all results.
     *
     * @param predicate      the predicate to filter with
     *
     * @return this query instance
     */
    public Query filter(Predicate<Path> predicate) {
        filters.add(predicate);
        return this;
    }

    /**
     * Toggle hidden path inclusion in results (defaults to false).
     *
     * @param hidden         include hidden paths
     *
     * @return this query instance
     */
    public Query filterShowHidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    /**
     * Adds a path depth filter to all results. A depth of zero (0)
     * will only match child paths directly on the base path.
     *
     * @param depth          the additional depth, or -1 for any
     *
     * @return this query instance
     */
    public Query filterDepth(int depth) {
        this.maxDepth = (depth < 0) ? Integer.MAX_VALUE : this.base.depth() + depth;
        return this;
    }

    /**
     * Adds a file name extension filter to all results.
     *
     * @param ext            the file extension to match
     *
     * @return this query instance
     */
    public Query filterFileExtension(String ext) {
        return filter(path -> path.name().toLowerCase().endsWith(ext));
    }

    /**
     * Adds an access filter (for current user) to all results.
     *
     * @param permission     the permission to require
     *
     * @return this query instance
     */
    public Query filterAccess(String permission) {
        return filter(path -> SecurityContext.hasAccess(path.toString(), permission));
    }

    /**
     * Returns an unfiltered stream of object paths.
     *
     * @param parent         the parent path (must be an index)
     *
     * @return the stream of object paths found
     */
    private Stream<Path> allPaths(Path parent) {
        if (parent == null || !parent.isIndex()) {
            return Stream.ofNullable(parent);
        }
        Index idx = (Index) this.storage.load(parent);
        if (idx == null) {
            return Stream.empty();
        } else {
            boolean isMaxDepth = parent.depth() >= this.maxDepth;
            return idx.paths(parent, this.hidden).flatMap(path -> {
                if (path.isIndex()) {
                    return isMaxDepth ? Stream.empty() : allPaths(path);
                } else {
                    return Stream.of(path);
                }
            });
        }
    }

    /**
     * Returns the stream of matching object paths.
     *
     * @return the stream of matching object paths
     */
    public Stream<Path> paths() {
        Stream<Path> stream = allPaths(this.base);
        for (Predicate<Path> pred : filters) {
            stream = stream.filter(pred);
        }
        return stream;
    }

    /**
     * Returns the stream of metadata found.
     *
     * @return the stream of metadata found
     */
    public Stream<Metadata> metadatas() {
        return paths().map(path -> this.storage.lookup(path)).filter(Objects::nonNull);
    }

    /**
     * Returns the stream of metadata found for objects of a
     * specified type.
     *
     * @param clazz          the class required for objects
     *
     * @return the stream of metadata found
     */
    public Stream<Metadata> metadatas(Class<?> clazz) {
        return metadatas().filter(meta -> meta.isObject(clazz));
    }

    /**
     * Returns the stream of objects found.
     *
     * @return the stream of objects found
     */
    public Stream<Object> objects() {
        return paths().map(path -> this.storage.load(path)).filter(Objects::nonNull);
    }

    /**
     * Returns the stream of objects found of a specified type.
     *
     * @param <T>            the type to cast objects to
     * @param clazz          the class required for objects
     *
     * @return the stream of objects found
     */
    public <T> Stream<T> objects(Class<T> clazz) {
        return objects().filter(clazz::isInstance).map(clazz::cast);
    }
}
