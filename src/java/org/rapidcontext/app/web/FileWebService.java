/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2014 Per Cederberg. All rights reserved.
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

package org.rapidcontext.app.web;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Index;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.type.WebService;
import org.rapidcontext.core.web.Request;

/**
 * A file web service. This service is used for retrieving binary (or
 * text) files from the storage, usually the standard web site files
 * (HTML, CSS, JavaScript, etc). This web service only support HTTP GET
 * requests.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class FileWebService extends WebService {

    /**
     * The web files storage path.
     */
    public static final Path PATH_FILES = new Path("/files/");

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
        this.dict.set(KEY_PATH, path());
    }

    /**
     * Returns the base storage path for file lookups.
     *
     * @return the base storage path
     */
    public Path path() {
        Object obj = dict.get(KEY_PATH);
        if (obj == null) {
            return PATH_FILES;
        } else if (obj instanceof Path) {
            return (Path) obj;
        } else {
            return new Path(obj.toString());
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
    protected String[] methodsImpl(Request request) {
        return METHODS_GET;
    }

    /**
     * Processes an HTTP GET request.
     *
     * @param request        the request to process
     */
    protected void doGet(Request request) {
        processFile(request, new Path(path(), request.getPath()));
    }

    /**
     * Processes a storage file retrieval request (if possible).
     *
     * @param request        the request to process
     * @param path           the storage path to the binary file
     */
    protected void processFile(Request request, Path path) {
        ApplicationContext  ctx = ApplicationContext.getInstance();
        Object              obj = null;
        boolean             cache;

        if (path.isIndex()) {
            path = path.child("index.html", false);
        }
        obj = ctx.getStorage().load(path);
        if (obj instanceof Binary) {
            if (request.getParameter("download") != null) {
                String str = "attachment; filename=" + path.name();
                request.setResponseHeader("Content-Disposition", str);
            }
            cache = ctx.getConfig().getBoolean("responseNoCache", false);
            request.sendBinary((Binary) obj, cache);
        } else if (obj instanceof Index) {
            request.sendRedirect(request.getUrl() + "/");
        } else if (obj != null) {
            errorForbidden(request);
        }
    }
}
