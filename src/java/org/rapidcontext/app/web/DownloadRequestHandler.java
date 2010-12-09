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

package org.rapidcontext.app.web;

import org.apache.commons.lang.StringUtils;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.core.web.Request;
import org.rapidcontext.core.web.RequestHandler;

/**
 * A file download request handler. This request handler is used when
 * file data is POST:ed to the special download URL, which makes the
 * handler send the data back with the specified MIME type.
 * Optionally, the "Content-Disposition" header is also set to force
 * the browser "save as" dialog.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class DownloadRequestHandler extends RequestHandler {

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
        return POST_METHODS_ONLY;
    }

    /**
     * Processes an HTTP POST request.
     *
     * @param request        the request to process
     */
    protected void doPost(Request request) {
        String  name = request.getParameter("fileName", request.getPath());
        String  data = request.getParameter("fileData");
        String  mimeType = request.getParameter("mimeType", Mime.type(name));
        String  disp = "attachment";

        if (data == null) {
            errorBadRequest(request, "Missing 'fileData' parameter");
        } else {
            if (request.getParameter("download") != null) {
                if (name.indexOf("/") >= 0) {
                    name = StringUtils.substringAfterLast(name, "/");
                }
                if (name.length() > 0) {
                    disp += "; filename=" + name;
                }
                request.setResponseHeader("Content-Disposition", disp);
            }
            request.sendData(mimeType, data);
        }
    }
}
