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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.web.Mime;
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
        ArrayList           pathList = new ArrayList();
        Path                path;
        Object              obj = null;
        String              str;

        cache = ctx.getConfig().getBoolean("responseNoCache", false);
        path = new Path(PATH_FILES, request.getPath());
        if (path.isIndex()) {
            pathList.add(path.child("index.tmpl", false));
            pathList.add(path.child("index.html", false));
        } else if (StringUtils.startsWithIgnoreCase(path.name(), "index.htm")) {
            pathList.add(path.parent().child("index.tmpl", false));
            pathList.add(path.parent().child("index.html", false));
        }
        pathList.add(path);
        for (int i = 0; obj == null && i < pathList.size(); i++) {
            path = (Path) pathList.get(i);
            obj = ctx.getStorage().load(path);
        }
        if (obj == null) {
            errorNotFound(request);
        } else if (path.name().endsWith(".tmpl") && obj instanceof Binary) {
            try {
                processTemplate(request, ctx.getStorage(), (Binary) obj);
            } catch (IOException e) {
                errorNotFound(request);
            }
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

    /**
     * Processes an HTML template file. The template variables will be
     * replaced with their corresponding search results and values.
     *
     * @param request        the request to process
     * @param storage        the storage to use
     * @param bin            the binary template file
     *
     * @throws IOException if the template file couldn't be read properly
     */
    protected void processTemplate(Request request, Storage storage, Binary bin)
    throws IOException {
        StringBuilder   res = new StringBuilder();
        BufferedReader  reader;
        String          line;
        Metadata[]      files;
        String          name;

        reader = new BufferedReader(new InputStreamReader(bin.openStream(), "UTF-8"));
        while ((line = reader.readLine()) != null) {
            if (line.contains("%JS_FILES%")) {
                files = storage.lookupAll(PATH_FILES.child("js", true));
                for (int i = 0; i < files.length; i++) {
                    name = files[i].path().name();
                    if (files[i].isBinary() && name.endsWith(".js")) {
                        res.append(line.replace("%JS_FILES%", "js/" + name));
                        res.append("\n");
                    }
                }
            } else if (line.contains("%CSS_FILES%")) {
                files = storage.lookupAll(PATH_FILES.child("css", true));
                for (int i = 0; i < files.length; i++) {
                    name = files[i].path().name();
                    if (files[i].isBinary() && name.endsWith(".css")) {
                        res.append(line.replace("%CSS_FILES%", "css/" + name));
                        res.append("\n");
                    }
                }
            } else {
                res.append(line);
                res.append("\n");
            }
        }
        reader.close();
        request.sendText(Mime.HTML[0], res.toString());
    }
}
