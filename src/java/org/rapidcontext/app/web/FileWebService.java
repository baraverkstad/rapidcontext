/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2026 Per Cederberg. All rights reserved.
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

package org.rapidcontext.app.web;

import java.util.stream.Stream;

import org.rapidcontext.core.ctx.Context;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Index;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.RootStorage;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.WebService;
import org.rapidcontext.core.web.Request;

/**
 * A file web service. This service is used for retrieving binary (or
 * text) files from the storage, usually the standard web site files
 * (HTML, CSS, JavaScript, etc). This web service only support HTTP GET
 * requests.
 *
 * @author Per Cederberg
 */
public class FileWebService extends WebService {

    /**
     * The dictionary key for the base storage path for files.
     */
    public static final String KEY_PATH = "path";

    /**
     * Creates a new file web service from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public FileWebService(String id, String type, Dict dict) {
        super(id, type, dict);
        this.dict.setDefault(KEY_PATH, RootStorage.PATH_FILES);
    }

    /**
     * Returns the base storage path for file lookups.
     *
     * @return the base storage path
     */
    @Override
    public Path path() {
        Object obj = dict.get(KEY_PATH);
        if (obj == null) {
            return RootStorage.PATH_FILES;
        } else if (obj instanceof Path p) {
            return p;
        } else {
            return Path.from(obj.toString());
        }
    }

    /**
     * Returns the HTTP methods implemented for the specified
     * request. The OPTIONS or HEAD methods doesn't have to be added
     * to the result (added automatically later).
     *
     * @param request        the request to check
     *
     * @return the array of HTTP method names supported
     *
     * @see #methods(Request)
     */
    @Override
    protected String[] methodsImpl(Request request) {
        return METHODS_GET;
    }

    /**
     * Processes an HTTP GET request.
     *
     * @param request        the request to process
     */
    @Override
    protected void doGet(Request request) {
        processFile(request, Path.resolve(path(), request.getPath()), false);
    }

    /**
     * Processes a storage file retrieval request (if possible).
     *
     * @param request        the request to process
     * @param filePath       the storage path to the binary file
     * @param exact          the exact path match flag
     */
    protected void processFile(Request request, Path filePath, boolean exact) {
        Storage storage = Context.active().storage();
        Object obj = lookupPaths(filePath, exact)
            .map(storage::load)
            .filter(o -> o instanceof Binary || o instanceof Index)
            .findFirst()
            .orElse(null);
        if (obj instanceof Binary b) {
            if (request.getParameter("download") != null) {
                String str = "attachment; filename=" + filePath.name();
                request.setResponseHeader("Content-Disposition", str);
            }
            request.sendBinary(b);
        } else if (obj instanceof Index) {
            request.sendRedirect(request.getUrl() + "/");
        }
    }

    /**
     * Returns an ordered stream of file lookup paths. For non-exact matches
     * this includes all parent "404.html" and "index.html" paths.
     *
     * @param filePath       the requested file path
     * @param exact          the exact path match flag
     *
     * @return the stream of lookup paths
     */
    protected Stream<Path> lookupPaths(Path filePath, boolean exact) {
        Stream.Builder<Path> builder = Stream.builder();
        if (filePath.isIndex()) {
            builder.add(filePath.child("index.html", false));
        } else {
            builder.add(filePath);
            builder.add(filePath.parent().child(filePath.name(), true));
        }
        Path base = path();
        while (!exact && !filePath.isRoot() && !filePath.equals(base)) {
            filePath = filePath.parent();
            builder.add(filePath.child("404.html", false));
            builder.add(filePath.child("index.html", false));
        }
        return builder.build();
    }
}
