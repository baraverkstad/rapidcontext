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

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.HtmlSerializer;
import org.rapidcontext.core.data.XmlSerializer;
import org.rapidcontext.core.js.JsSerializer;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.Index;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.core.web.Request;
import org.rapidcontext.core.web.RequestHandler;

/**
 * A storage API request handler. This request handler supports both
 * WebDAV and normal browser requests and provides a file-system-like
 * view of the storage hierarchy.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StorageRequestHandler extends RequestHandler {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(StorageRequestHandler.class.getName());

    /**
     * The supported HTTP methods.
     */
    protected static final String[] METHODS = {
        "OPTIONS", "HEAD", "GET"
        //TODO:, "POST", "PUT", "DELETE", "PROPFIND", "PROPPATCH", "MKCOL", "COPY", "MOVE"
    };

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
     * Processes a request for this handler. This method assumes
     * local request paths (removal of the mapped URL base).
     *
     * @param request the request to process
     */
    public void process(Request request) {
        //request.setResponseHeader("DAV", "1 3");
        if (!SecurityContext.hasAdmin()) {
            errorUnauthorized(request);
        } else {
            super.process(request);
        }
    }

    /**
     * Processes an HTTP GET request.
     *
     * @param request        the request to process
     */
    protected void doGet(Request request) {
        ApplicationContext  ctx = ApplicationContext.getInstance();
        String              mimeType = request.getParameter("mimeType");
        boolean             isHtml = isMimeMatch(request, Mime.HTML);
        boolean             isJson = isMimeMatch(request, Mime.JSON);
        boolean             isXml = isMimeMatch(request, Mime.XML);
        boolean             isDefault = (!isJson && !isXml && !isHtml);
        Path                path = new Path(request.getPath());
        Object              meta = null;
        Object              res = null;
        Dict                dict;

        request.setPath(null);
        try {
            // TODO: Extend data lookup via standardized query language
            res = ctx.getStorage().load(path);
            meta = ctx.getStorage().lookup(path);
            if (res instanceof Index) {
                res = serializeIndex((Index) res, isHtml || isDefault);
            } else if (res instanceof File && !isDefault) {
                res = serializeFile((File) res, request, path);
            }
            if (meta instanceof Metadata) {
                meta = serializeMetadata((Metadata) meta, request);
            }

            // Render result as raw data, HTML, JSON or XML
            if (isDefault && res instanceof File) {
                request.sendFile((File) res, true); 
            } else if (isDefault || isHtml) {
                sendHtml(request, path, meta, res);
            } else if (isJson) {
                dict = new Dict();
                dict.set("metadata", meta);
                dict.set("data", res);
                mimeType = StringUtils.defaultIfEmpty(mimeType, Mime.JSON[0]);
                request.sendData(mimeType, JsSerializer.serialize(dict));
            } else if (isXml && !isHtml) {
                dict = new Dict();
                dict.set("metadata", meta);
                dict.set("data", res);
                mimeType = StringUtils.defaultIfEmpty(mimeType, Mime.XML[0]);
                request.sendData(mimeType, XmlSerializer.serialize(dict));
            } else {
                request.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed to process storage request", e);
            // TODO: How do users want their error messages?
            dict = new Dict();
            dict.set("error", e.getMessage());
            res = dict;
            if (isJson) {
                request.sendData(Mime.JSON[0], JsSerializer.serialize(res));
            } else if (isXml) {
                request.sendData(Mime.XML[0], XmlSerializer.serialize(res));
            } else {
                errorInternal(request, e.getMessage());
            }
        }
    }

    /**
     * Creates and sends the HTML response for a storage request.
     *
     * @param request        the request to modify
     * @param path           the storage path requested
     * @param meta           the meta-data to render
     * @param res            the actual data to render
     */
    private void sendHtml(Request request, Path path, Object meta, Object res) {
        StringBuffer html = new StringBuffer();

        html.append("<html>\n<head>\n<link rel='stylesheet' href='");
        html.append(relativeBackPath(request.getPath()));
        html.append("css/style.css' type='text/css' />\n");
        html.append("<title>RapidContext Query Response</title>\n");
        html.append("</head>\n<body>\n<div class='query'>\n");
        html.append("<h1>RapidContext Query API</h1>\n");
        html.append("<table class='navigation'>\n<tr>\n");
        if (path.isRoot()) {
            html.append("<td class='active'>Start</td>\n");
        } else {
            html.append("<td class='prev'><a href='");
            html.append(StringUtils.repeat("../", path.depth()));
            html.append(".'>Start</a></td>\n");
        }
        for (int i = 0; i < path.length(); i++) {
            if (i + 1 < path.length()) {
                html.append("<td class='prev-prev'>&nbsp;</td>\n");
                html.append("<td class='prev'><a href='");
                html.append(StringUtils.repeat("../", path.depth() - i - 1));
                html.append(".'>");
                html.append(path.name(i));
                html.append("</a>");
            } else {
                html.append("<td class='prev-active'>&nbsp;</td>\n");
                html.append("<td class='active'>");
                html.append(path.name(i));
            }
            html.append("</td>\n");
        }
        html.append("<td class='active-end'>&nbsp;</td>\n");
        html.append("</tr>\n</table>\n<hr/>\n");
        html.append("<div class='metadata'>\n");
        html.append("<h2>Query Metadata</h2>\n");
        html.append(HtmlSerializer.serialize(meta));
        html.append("</div>\n");
        html.append("<h2>Query Results</h2>");
        html.append(HtmlSerializer.serialize(res));
        html.append("<hr/><p><strong>Data Formats:</strong>");
        html.append(" &nbsp;<a href='?mimeType=text/javascript'>JSON</a>");
        html.append(" &nbsp;<a href='?mimeType=text/xml'>XML</a></p>");
        html.append("</div>\n</body>\n</html>\n");
        request.sendData(Mime.HTML[0], html.toString());
    }

    /**
     * Serializes an index to a suitable external representation. If
     * the HTTP link flag is set, all the references in the index
     * will be prefixed by "http:" (being serialized into HTML links).
     *
     * @param idx            the index to serialize
     * @param linkify        the HTTP link flag
     *
     * @return the serialized representation of the index
     */
    private Dict serializeIndex(Index idx, boolean linkify) {
        Dict   dict = new Dict();
        Array  arr;

        dict.set("type", "index");
        arr = idx.indices();
        arr = arr.copy();
        if (linkify) {
            arr.sort();
        }
        for (int i = 0; i < arr.size(); i++) {
            if (linkify) {
                arr.set(i, "http:" + arr.getString(i, null) + "/");
            } else {
                arr.set(i, arr.getString(i, null) + "/");
            }
        }
        dict.set("directories", arr);
        arr = idx.objects();
        if (linkify) {
            arr = arr.copy();
            arr.sort();
            for (int i = 0; i < arr.size(); i++) {
                arr.set(i, "http:" + arr.getString(i, null));
            }
        }
        dict.set("objects", arr);
        return dict;
    }

    /**
     * Serializes a file to an external representation.
     *
     * @param file           the file object
     * @param request        the web request
     * @param path           the storage path
     *
     * @return serialized representation of the file
     */
    private Dict serializeFile(File file, Request request, Path path) {
        Dict  dict = new Dict();

        dict.set("type", "file");
        dict.set("name", file.getName());
        dict.set("mimeType", Mime.type(file));
        dict.set("size", new Long(file.length()));
        String url = StringUtils.removeEnd(request.getUrl(), request.getPath()) +
                     StringUtils.removeStart(path.toString(), "/files");
        if (path.name(0).equals("files")) {
            dict.set("url", url);
        }
        return dict;
    }

    /**
     * Serializes a metadata object to an external representation.
     *
     * @param meta           the metadata object
     * @param request        the web request
     *
     * @return serialized representation of the metadata
     */
    private Dict serializeMetadata(Metadata meta, Request request) {
        Dict  dict = new Dict();

        dict = meta.serialize().copy();
        dict.set("processTime", new Long(request.getProcessTime()));
        return dict;
    }

    /**
     * Checks if the request accepts one of the listed MIME types as response.
     * 
     * @param request
     *            the request to analyze
     * @param mimes
     *            the MIME types to check for
     * 
     * @return true if one of the MIME types is accepted, or false otherwise
     */
    private boolean isMimeMatch(Request request, String[] mimes) {
        String param = request.getParameter("mimeType");

        if (param != null) {
            return ArrayUtils.contains(mimes, param);
        } else {
            return Mime.isMatch(request, mimes);
        }
    }

    /**
     * Returns the relative path to reverse the specified path. This
     * method will add an "../" part for each directory in the
     * current path so that site-relative links can be created
     * easily.
     *
     * @param path           the path to reverse
     *
     * @return the relative reversed path
     */
    protected String relativeBackPath(String path) {
        int count = StringUtils.countMatches(path, "/");
        return StringUtils.repeat("../", count - 1);
    }
}
