/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2012 Per Cederberg. All rights reserved.
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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorageException;
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
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(FileRequestHandler.class.getName());

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
        Path                path;
        Object              obj = null;
        boolean             isRoot;
        boolean             cache;
        String              str;

        str = request.getPath();
        isRoot = "".equals(str) || StringUtils.startsWithIgnoreCase(str, "index.htm");
        path = new Path(PATH_FILES, str);
        obj = ctx.getStorage().load(path);
        if (isRoot) {
            try {
                AppRequestHandler.processApp(request, null);
            } catch (StorageException e) {
                LOG.log(Level.WARNING, "failed to launch default starter app", e);
                errorInternal(request, e.getMessage());
            } catch (IOException e) {
                LOG.log(Level.WARNING, "failed to launch default starter app", e);
                errorInternal(request, e.getMessage());
            }
        } else if (obj == null) {
            errorNotFound(request);
        } else if (obj instanceof Binary) {
            if (request.getParameter("download") != null) {
                str = "attachment; filename=" + path.name();
                request.setResponseHeader("Content-Disposition", str);
            }
            cache = ctx.getConfig().getBoolean("responseNoCache", false);
            request.sendBinary((Binary) obj, cache);
        } else {
            errorForbidden(request);
        }
    }
}
