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

import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
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
     * The hidden file extensions.
     */
    private String[] hiddenExts;

    /**
     * The path predicate filters to apply.
     */
    private ArrayList<Predicate<Path>> filters = new ArrayList<>();

    /**
     * Creates a new query for the specified storage.
     *
     * @param storage        the storage to query
     * @param base           the base path
     * @param hiddenExts     the hidden file extensions
     */
    public Query(Storage storage, Path base, String[] hiddenExts) {
        this.storage = storage;
        this.base = base;
        this.hiddenExts = hiddenExts;
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
     * Adds a file name extension filter to all results.
     *
     * @param ext            the file extension to match
     *
     * @return this query instance
     */
    public Query filterExtension(String ext) {
        return filter(path -> path.name().toLowerCase().endsWith(ext));
    }

    /**
     * Adds a user read access filter to all results.
     *
     * @return this query instance
     */
    public Query filterReadAccess() {
        return filter(path -> SecurityContext.hasReadAccess(path.toString()));
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
            return Stream.empty();
        }
        Index idx = (Index) this.storage.load(parent);
        if (idx == null) {
            return Stream.empty();
        } else {
            boolean isBinaryPath = RootStorage.isBinaryPath(parent);
            return idx.paths(parent).flatMap(path -> {
                if (path.isIndex()) {
                    return allPaths(path);
                } else if (isBinaryPath) {
                    return Stream.of(path);
                } else {
                    String name = path.name();
                    for (String ext : this.hiddenExts) {
                        if (StringUtils.endsWithIgnoreCase(name, ext)) {
                            name = StringUtils.removeEndIgnoreCase(name, ext);
                            path = idx.hasObject(name) ? null : path.parent().child(name, false);
                            break;
                        }
                    }
                    return Stream.ofNullable(path);
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
        return paths().map(path -> this.storage.lookup(path));
    }

    /**
     * Returns the stream of objects found.
     *
     * @return the stream of objects found
     */
    public Stream<Object> objects() {
        return paths().map(path -> this.storage.load(path));
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
