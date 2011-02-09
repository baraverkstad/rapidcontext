/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2011 Per Cederberg. All rights reserved.
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

import org.apache.commons.lang.StringUtils;
import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.web.Request;
import org.rapidcontext.core.web.RequestHandler;

/**
 * A normal file request handler. This request handler is used for
 * retrieving files from the storage, usually the standard web site
 * files (HTML, CSS, JavaScript, etc). The files are retrieved from
 * storage.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class FileRequestHandler extends RequestHandler {

    /**
     * The web files storage path.
     */
    public static final Path PATH_FILES = new Path("/files/");

    /**
     * Returns the HTTP methods supported for the specified request
     * (path). This method assumes local request paths (removal of
     * the mapped URL base).
     *
     * @param request        the request to check
     *
     * @return the array of HTTP method names supported
     */
    public String[] methods(Request request) {
        return GET_METHODS_ONLY;
    }

    /**
     * Processes an HTTP GET request.
     *
     * @param request        the request to process
     */
    protected void doGet(Request request) {
        ApplicationContext  ctx = ApplicationContext.getInstance();
        boolean             cache;
        Path                path;
        Object              obj = null;
        String              str;

        cache = ctx.getConfig().getBoolean("responseNoCache", false);
        path = new Path(PATH_FILES, request.getPath());
        if (path.isIndex()) {
            obj = ctx.getStorage().load(path.child("index.html", false));
        } else if (StringUtils.startsWithIgnoreCase(path.name(), "index.htm")) {
            obj = ctx.getStorage().load(path.parent().child("index.html", false));
        }
        if (obj == null) {
            obj = ctx.getStorage().load(path);
        }
        if (obj == null) {
            errorNotFound(request);
        } else if (obj instanceof Binary) {
            if (request.getParameter("download") != null) {
                str = "attachment; filename=" + path.name();
                request.setResponseHeader("Content-Disposition", str);
            }
            request.sendBinary((Binary) obj, cache);
        } else {
            errorForbidden(request);
        }
    }
}
