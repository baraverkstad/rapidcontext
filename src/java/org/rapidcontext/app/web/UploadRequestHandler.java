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

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.fileupload.FileItemStream;
import org.rapidcontext.core.type.Session;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.core.web.Request;
import org.rapidcontext.core.web.RequestHandler;
import org.rapidcontext.util.FileUtil;

/**
 * A file upload request handler. This request handler is used when
 * files are POST:ed to the special upload URL.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class UploadRequestHandler extends RequestHandler {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(UploadRequestHandler.class.getName());

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
        Session          session = (Session) Session.activeSession.get();
        FileItemStream   stream;
        String           fileId = request.getPath();
        String           fileName;
        File             file;

        // TODO: ensure that we have a valid session...
        try {
            stream = request.getNextFile();
            if (stream == null) {
                errorBadRequest(request, "Missing file data");
                return;
            }
            fileName = stream.getName();
            if (fileName.lastIndexOf("/") >= 0) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
            if (fileName.lastIndexOf("\\") >= 0) {
                fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
            }
            while (fileId != null && fileId.startsWith("/")) {
                fileId = fileId.substring(1);
            }
            if (fileId == null || fileId.trim().length() == 0) {
                fileId = fileName;
            }
            file = FileUtil.tempFile(fileName);
            FileUtil.copy(stream.openStream(), file);
            session.addFile(fileId, file);
            request.sendText(Mime.TEXT[0], "Session file " + fileId + " uploaded");
        } catch (IOException e) {
            LOG.log(Level.WARNING, "failed to process file upload", e);
            errorBadRequest(request, e.getMessage());
        }
    }
}
