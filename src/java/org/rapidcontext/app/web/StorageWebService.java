/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2017 Per Cederberg. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.proc.StorageCopyProcedure;
import org.rapidcontext.app.proc.StorageDeleteProcedure;
import org.rapidcontext.app.proc.StorageWriteProcedure;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.HtmlSerializer;
import org.rapidcontext.core.data.PropertiesSerializer;
import org.rapidcontext.core.data.XmlSerializer;
import org.rapidcontext.core.js.JsException;
import org.rapidcontext.core.js.JsSerializer;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.DirStorage;
import org.rapidcontext.core.storage.Index;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.WebService;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.core.web.Request;

/**
 * A storage API web service. This service is used for accessing the
 * raw data storage through HTTP or WebDAV and provides a view of the
 * storage hierarchy similar to a file system. Note that WebDAV makes
 * use of extended HTTP methods.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StorageWebService extends WebService {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(StorageWebService.class.getName());

    /**
     * The HTTP methods supported by this web service.
     */
    public static final String[] METHODS = {
        METHOD.GET,
        METHOD.POST,
        METHOD.PATCH,
        METHOD.PUT,
        METHOD.DELETE,
        METHOD.PROPFIND,
        METHOD.MKCOL,
        METHOD.MOVE,
        METHOD.LOCK,
        METHOD.UNLOCK
    };

    /**
     * The HTML file extension.
     */
    public static final String EXT_HTML = ".html";

    /**
     * The JSON file extension.
     */
    public static final String EXT_JSON = ".json";

    /**
     * The properties file extension.
     */
    public static final String EXT_PROPERTIES = ".properties";

    /**
     * The XML file extension.
     */
    public static final String EXT_XML = ".xml";

    /**
     * Creates a new storage web service from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public StorageWebService(String id, String type, Dict dict) {
        super(id, type, dict);
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
        return METHODS;
    }

    /**
     * Processes a request for this handler. This method assumes
     * local request paths (removal of the mapped URL base).
     *
     * @param request the request to process
     */
    public void process(Request request) {
        request.setResponseHeader(HEADER.DAV, "2");
        request.setResponseHeader("MS-Author-Via", "DAV");
        if (request.hasMethod(METHOD.PROPFIND)) {
            doPropFind(request);
        } else if (request.hasMethod(METHOD.MKCOL)) {
            doMkCol(request);
        } else if (request.hasMethod(METHOD.MOVE)) {
            doMove(request);
        } else if (request.hasMethod(METHOD.LOCK)) {
            doLock(request);
        } else if (request.hasMethod(METHOD.UNLOCK)) {
            doUnlock(request);
        } else {
            super.process(request);
        }
    }

    /**
     * Processes an HTTP OPTIONS request.
     *
     * @param request        the request to process
     */
    protected void doOptions(Request request) {
        request.setResponseHeader(HEADER.ACCEPT_PATCH, Mime.JSON[0]);
        super.doOptions(request);
    }

    /**
     * Processes an HTTP GET request.
     *
     * @param request        the request to process
     */
    protected void doGet(Request request) {
        ApplicationContext  ctx = ApplicationContext.getInstance();
        Path                orig = new Path(request.getPath());
        Metadata            meta = null;
        Object              res = null;
        boolean             isHtml = false;
        boolean             isJson = false;
        boolean             isProps = false;
        boolean             isXml = false;
        boolean             isDefault = true;
        Dict                dict;

        // TODO: Change to read-access here once a solution has been devised
        //       to disable search queries for certain paths and/or users.
        if (!SecurityContext.hasWriteAccess(request.getPath())) {
            errorUnauthorized(request);
            return;
        }
        try {
            // TODO: Extend data lookup via query language
            Path path = normalizePath(orig);
            if (path != null && !path.equals(orig)) {
                isHtml = StringUtils.endsWith(orig.name(), EXT_HTML);
                isJson = StringUtils.endsWith(orig.name(), EXT_JSON);
                isXml = StringUtils.endsWith(orig.name(), EXT_XML);
            }
            isProps = StringUtils.endsWith(orig.name(), EXT_PROPERTIES);
            isDefault = (!isHtml && !isJson && !isProps && !isXml);
            if (path != null) {
                res = ctx.getStorage().load(path);
                meta = ctx.getStorage().lookup(path);
            }
            if (res instanceof Index) {
                res = serializeIndex((Index) res, isDefault || isHtml);
            } else if (res instanceof Binary && !isDefault) {
                res = serializeBinary((Binary) res, request, path);
            }

            // Render result as raw data, Properties, HTML, JSON or XML
            if (res == null) {
                errorNotFound(request);
            } else if (isDefault && res instanceof Binary) {
                request.sendBinary((Binary) res, true);
            } else if (isDefault || isHtml) {
                sendHtml(request, path, meta, res);
            } else if (isJson) {
                dict = new Dict();
                dict.set("metadata", serializeMetadata(meta, request));
                dict.set("data", res);
                request.sendText(Mime.JSON[0], JsSerializer.serialize(dict, true));
            } else if (isProps) {
                request.sendText(Mime.TEXT[0], PropertiesSerializer.serialize(res));
            } else if (isXml) {
                dict = new Dict();
                dict.set("metadata", serializeMetadata(meta, request));
                dict.set("data", res);
                request.sendText(Mime.XML[0], XmlSerializer.serialize("results", dict));
            } else {
                request.sendError(STATUS.NOT_ACCEPTABLE);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed to process storage request", e);
            // TODO: How do users want their error messages?
            dict = new Dict();
            dict.set("error", e.getMessage());
            res = dict;
            if (isJson) {
                request.sendText(Mime.JSON[0], JsSerializer.serialize(res, true));
            } else if (isXml) {
                request.sendText(Mime.XML[0], XmlSerializer.serialize("error", res));
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
    private void sendHtml(Request request, Path path, Metadata meta, Object res) {
        StringBuffer html = new StringBuffer();

        html.append("<html>\n<head>\n<link rel='stylesheet' href='");
        html.append(relativeBackPath(request.getPath()));
        html.append("files/css/style.css' type='text/css' />\n");
        html.append("<title>RapidContext Storage API</title>\n");
        html.append("</head>\n<body>\n<div class='query'>\n");
        html.append("<h1>RapidContext Storage API</h1>\n");
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
        html.append(HtmlSerializer.serialize(serializeMetadata(meta, request)));
        html.append("</div>\n");
        html.append("<h2>Query Results</h2>");
        html.append(HtmlSerializer.serialize(res));
        html.append("<hr/><p><strong>Data Formats:</strong>");
        if (meta.isIndex()) {
            html.append(" &nbsp;<a href='index.json'>JSON</a>");
            html.append(" &nbsp;<a href='index.xml'>XML</a>");
        } else {
            html.append(" &nbsp;<a href='" + path.name() + ".json'>JSON</a>");
            html.append(" &nbsp;<a href='" + path.name() + ".xml'>XML</a>");
            if (meta.isBinary()) {
                html.append(" &nbsp;<a href='" + path.name() + "'>RAW</a>");
            } else {
                html.append(" &nbsp;<a href='" + path.name() + ".properties'>PROPERTIES</a>");
            }
        }
        html.append("</p></div>\n</body>\n</html>\n");
        request.sendText(Mime.HTML[0], html.toString());
    }

    /**
     * Serializes an index to a suitable external representation. If
     * the HTML link flag is set, all the references in the index
     * will be prefixed by "$href$" (being serialized into HTML links).
     *
     * @param idx            the index to serialize
     * @param linkify        the HTML link flag
     *
     * @return the serialized representation of the index
     */
    private Dict serializeIndex(Index idx, boolean linkify) {
        Dict   dict = new Dict();
        Array  arr;

        dict.set("type", "index");
        arr = idx.indices().copy();
        arr.sort();
        for (int i = 0; i < arr.size(); i++) {
            String name = arr.getString(i, null);
            if (name.startsWith(".")) {
                arr.remove(i--);
            } else if (linkify) {
                arr.set(i, "$href$" + name + "/");
            } else {
                arr.set(i, name + "/");
            }
        }
        dict.set("directories", arr);
        arr = idx.objects().copy();
        arr.sort();
        for (int i = 0; i < arr.size(); i++) {
            String name = arr.getString(i, null);
            if (name.startsWith(".")) {
                arr.remove(i--);
            } else if (linkify) {
                arr.set(i, "$href$" + name + ".html$" + name);
            }
        }
        dict.set("objects", arr);
        return dict;
    }

    /**
     * Serializes a binary data object to an external representation.
     *
     * @param data           the binary data object
     * @param request        the web request
     * @param path           the storage path
     *
     * @return serialized representation of the file
     */
    private Dict serializeBinary(Binary data, Request request, Path path) {
        Dict  dict = new Dict();

        dict.set("type", "file");
        dict.set("name", path.name());
        dict.set("mimeType", data.mimeType());
        dict.set("size", new Long(data.size()));
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
        dict.remove(Metadata.KEY_TYPE);
        dict.set("processTime", new Long(request.getProcessTime()));
        return dict;
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
    private String relativeBackPath(String path) {
        int count = StringUtils.countMatches(path, "/");
        return StringUtils.repeat("../", count);
    }

    /**
     * Processes an HTTP PATCH request.
     *
     * @param request        the request to process
     */
    protected void doPatch(Request request) {
        if (!SecurityContext.hasWriteAccess(request.getPath())) {
            errorUnauthorized(request);
            return;
        } else if (request.getPath().endsWith("/")) {
            errorBadRequest(request, "cannot write data to folder");
            return;
        } else if (!Mime.isInputMatch(request, Mime.JSON)) {
            String msg = "application/json content type required";
            request.sendError(STATUS.UNSUPPORTED_MEDIA_TYPE, null, msg);
            return;
        }
        try {
            Object data = JsSerializer.unserialize(request.getInputString());
            Path path = new Path(request.getPath());
            Storage storage = ApplicationContext.getInstance().getStorage();
            Object prev = storage.load(path);
            Dict dict = (prev instanceof Dict) ? (Dict) prev : null;
            if (prev instanceof StorableObject) {
                dict = ((StorableObject) prev).serialize();
            }
            if (prev == null) {
                errorNotFound(request);
            } else if (dict == null){
                String msg = "resource is not object";
                request.sendError(STATUS.UNPROCESSABLE_ENTITY, null, msg);
            } else if (!(data instanceof Dict)) {
                String msg = "patch data should be JSON object";
                request.sendError(STATUS.UNPROCESSABLE_ENTITY, null, msg);
            } else {
                dict.setAll((Dict) data);
                storage.store(path, dict);
                request.sendText(Mime.JSON[0], JsSerializer.serialize(dict, true));
            }
        } catch (JsException e) {
            String msg = "invalid input JSON: " + e.getMessage();
            LOG.warning(msg);
            errorBadRequest(request, msg);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed to write " + request.getPath(), e);
            errorInternal(request, e.getMessage());
        }
    }

    /**
     * Processes an HTTP POST request.
     *
     * @param request        the request to process
     */
    protected void doPost(Request request) {
        if (!SecurityContext.hasWriteAccess(request.getPath())) {
            errorUnauthorized(request);
            return;
        } else if (request.getPath().endsWith("/")) {
            errorBadRequest(request, "cannot write data to folder");
            return;
        } else if (!Mime.isInputMatch(request, Mime.JSON)) {
            String msg = "application/json content type required";
            request.sendError(STATUS.UNSUPPORTED_MEDIA_TYPE, null, msg);
            return;
        }
        try {
            Path path = new Path(request.getPath());
            Path normalizedPath = normalizePath(path);
            Object data = JsSerializer.unserialize(request.getInputString());
            if (StorageWriteProcedure.store(path, data)) {
                if (normalizedPath == null) {
                    request.sendText(STATUS.CREATED, null, null);
                } else {
                    request.sendText(STATUS.OK, null, null);
                }
            } else {
                String msg = "failed to write " + request.getPath();
                LOG.warning(msg);
                errorInternal(request, msg);
            }
        } catch (JsException e) {
            String msg = "invalid input JSON: " + e.getMessage();
            LOG.warning(msg);
            errorBadRequest(request, msg);
        }
    }

    /**
     * Processes an HTTP PUT request.
     *
     * @param request        the request to process
     */
    protected void doPut(Request request) {
        if (!SecurityContext.hasWriteAccess(request.getPath())) {
            errorUnauthorized(request);
            return;
        } else if (request.getHeader(HEADER.CONTENT_RANGE) != null) {
            request.sendError(STATUS.NOT_IMPLEMENTED);
            return;
        }
        if (request.getPath().endsWith("/")) {
            errorBadRequest(request, "cannot store data in a directory");
            return;
        }
        try {
            Path path = new Path(request.getPath());
            Path normalizedPath = normalizePath(path);
            Binary data = new Binary.BinaryStream(request.getInputStream(), -1);
            if (StorageWriteProcedure.store(path, data)) {
                if (normalizedPath == null) {
                    request.sendText(STATUS.CREATED, null, null);
                } else {
                    request.sendText(STATUS.OK, null, null);
                }
            } else {
                String msg = "failed to write " + request.getPath();
                LOG.warning(msg);
                errorInternal(request, msg);
            }
        } catch (IOException e) {
            String msg = "failed to write " + request.getPath() + ": " +
                         e.getMessage();
            LOG.log(Level.WARNING, msg, e);
            errorInternal(request, msg);
        }
    }

    /**
     * Processes an HTTP DELETE request.
     *
     * @param request        the request to process
     */
    protected void doDelete(Request request) {
        if (!SecurityContext.hasWriteAccess(request.getPath())) {
            errorUnauthorized(request);
            return;
        }
        Path path = normalizePath(new Path(request.getPath()));
        if (path == null) {
            errorNotFound(request);
            return;
        }
        if (StorageDeleteProcedure.delete(path)) {
            request.sendText(STATUS.NO_CONTENT, null, null);
        } else {
            String msg = "failed to delete " + request.getPath();
            errorInternal(request, msg);
        }
    }

    /**
     * Processes a WebDAV PROPFIND request.
     *
     * @param request        the request to process
     */
    protected void doPropFind(Request request) {
        ApplicationContext  ctx = ApplicationContext.getInstance();
        Path                path = new Path(request.getPath());
        String              href;
        WebDavRequest       davRequest;
        Metadata            meta = null;
        Object              data = null;
        Index               idx;
        Array               arr;
        String              str;

        if (!SecurityContext.hasWriteAccess(request.getPath())) {
            errorUnauthorized(request);
            return;
        }
        try {
            davRequest = new WebDavRequest(request);
            if (davRequest.depth() < 0 || davRequest.depth() > 1) {
                davRequest.sendErrorFiniteDepth();
                return;
            }
            path = normalizePath(path);
            if (path != null) {
                data = ctx.getStorage().load(path);
                meta = ctx.getStorage().lookup(path);
            }
            if (path == null || data == null || meta == null) {
                errorNotFound(request);
                return;
            }
            href = request.getAbsolutePath();
            if (path.isIndex() && !href.endsWith("/")) {
                href += "/";
            }
            addResource(davRequest, href, meta, data);
            if (davRequest.depth() > 0 && data instanceof Index) {
                idx = (Index) data;
                arr = idx.paths();
                LOG.fine("Paths: " + arr);
                for (int i = 0; i < arr.size(); i++) {
                    path = (Path) arr.get(i);
                    data = ctx.getStorage().load(path);
                    meta = ctx.getStorage().lookup(path);
                    if (data != null && meta != null) {
                        str = href + path.name();
                        if (path.isIndex()) {
                            str += "/";
                        } else if (meta.isObject()) {
                            str += DirStorage.SUFFIX_PROPS;
                        }
                        addResource(davRequest, str, meta, data);
                    }
                }
            }
            davRequest.sendMultiResponse();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed to process WebDAV propfind request", e);
            errorInternal(request, e.getMessage());
        }
    }

    /**
     * Adds a resource to the WebDAV response.
     *
     * @param request        the WebDAV request container
     * @param href           the root-relative resource link
     * @param meta           the resource meta-data
     * @param data           the resource object
     *
     * @throws Exception if the resource couldn't be added
     */
    private void addResource(WebDavRequest request,
                             String href,
                             Metadata meta,
                             Object data)
    throws Exception {

        Date modified = meta.lastModified();
        if (data instanceof Index) {
            request.addResource(href, modified, modified, 0);
        } else if (data instanceof File) {
            // TODO: storage file
            File file = (File) data;
            request.addResource(href, modified, modified, file.length());
        } else {
            String str = PropertiesSerializer.serialize(data);
            byte[] bytes = str.getBytes("ISO-8859-1");
            request.addResource(href, modified, modified, bytes.length);
        }
    }

    /**
     * Processes a WebDAV MKCOL request.
     *
     * @param request        the request to process
     */
    protected void doMkCol(Request request) {
        ApplicationContext  ctx = ApplicationContext.getInstance();
        Path                path;
        Metadata            meta;

        if (!SecurityContext.hasWriteAccess(request.getPath())) {
            errorUnauthorized(request);
            return;
        }
        try {
            path = new Path(request.getPath());
            if (!path.isIndex()) {
                path = path.parent().child(path.name(), true);
            }
            meta = ctx.getStorage().lookup(path);
            if (meta != null) {
                request.sendError(STATUS.FORBIDDEN);
            } else {
                ctx.getStorage().store(path.child("dummy", false), new Dict());
                ctx.getStorage().remove(path.child("dummy", false));
                request.sendText(STATUS.CREATED, null, null);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed to create dir " + request.getPath(), e);
            errorInternal(request, e.getMessage());
        }
    }

    /**
     * Processes a WebDAV MOVE request.
     *
     * @param request        the request to process
     */
    protected void doMove(Request request) {
        String path = request.getPath();
        String prefix = StringUtils.substringBeforeLast(request.getUrl(), path);
        prefix = StringUtils.removeEnd(prefix, "/");
        String href = request.getHeader(HEADER.DESTINATION);
        if (!SecurityContext.hasWriteAccess(path)) {
            errorUnauthorized(request);
            return;
        } else if (href == null) {
            errorBadRequest(request, "missing Destination header");
            return;
        }
        href = Helper.decodeUrl(href);
        if (!href.startsWith(prefix)) {
            request.sendError(STATUS.BAD_GATEWAY);
            return;
        }
        Path dst = new Path(href.substring(prefix.length()));
        Path src = normalizePath(new Path(path));
        if (!SecurityContext.hasWriteAccess(dst.toString())) {
            errorUnauthorized(request);
            return;
        } else if (src == null) {
            errorNotFound(request);
        } else if (src.isIndex()) {
            // TODO: add support for collection moves
            errorForbidden(request);
        } else {
            boolean success = StorageCopyProcedure.copy(src, dst, false) &&
                              StorageDeleteProcedure.delete(src);
            if (success) {
                href = Helper.encodeUrl(prefix + dst.toString());
                request.setResponseHeader(HEADER.LOCATION, href);
                request.sendText(STATUS.CREATED, null, null);
            } else {
                String msg = "failed to move " + request.getPath();
                errorInternal(request, msg);
            }
        }
    }

    /**
     * Processes a WebDAV LOCK request.
     *
     * @param request        the request to process
     */
    protected void doLock(Request request) {
        ApplicationContext  ctx = ApplicationContext.getInstance();
        Path                path = new Path(request.getPath());
        String              href;
        WebDavRequest       davRequest;
        Dict                lockInfo;
        Metadata            meta = null;

        if (!SecurityContext.hasWriteAccess(request.getPath())) {
            errorUnauthorized(request);
            return;
        }
        try {
            davRequest = new WebDavRequest(request);
            if (davRequest.depth() > 0) {
                errorBadRequest(request, "invalid lock depth header");
                return;
            }
            lockInfo = davRequest.lockInfo();
            if (lockInfo == null) {
                // TODO: allow lock refresh with missing body!
                errorBadRequest(request, "missing DAV:lockinfo request body");
                return;
            }
            path = normalizePath(path);
            if (path != null) {
                meta = ctx.getStorage().lookup(path);
            }
            if (path == null || meta == null) {
                errorNotFound(request);
                return;
            }
            href = request.getAbsolutePath();
            if (path.isIndex() && !href.endsWith("/")) {
                href += "/";
            }
            lockInfo.set("href", href);
            lockInfo.set("token", "locktoken:" + System.currentTimeMillis());
            // TODO: on lock fail, return multi-status response
            // TODO: lock refresh...
            davRequest.sendLockResponse(lockInfo, 60);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed to process WebDAV lock request", e);
            errorInternal(request, e.getMessage());
        }
    }

    /**
     * Processes a WebDAV UNLOCK request.
     *
     * @param request        the request to process
     */
    protected void doUnlock(Request request) {
        if (!SecurityContext.hasWriteAccess(request.getPath())) {
            errorUnauthorized(request);
            return;
        }
        // TODO: remove lock
        request.sendText(STATUS.NO_CONTENT, null, null);
    }

    /**
     * Attempts to correct or normalize the specified path if no data
     * can be found at the specified location. This is necessary in
     * order to provide "*.properties" file access to data objects
     * and to adjust for some WebDAV client bugs.
     *
     * @param path           the path to normalize
     *
     * @return the normalized path
     */
    private Path normalizePath(Path path) {
        Storage storage = ApplicationContext.getInstance().getStorage();
        String pathName = path.name();
        Path lookupPath = path;
        Metadata meta = storage.lookup(lookupPath);
        if (meta == null) {
            String str = pathName;
            if (StringUtils.endsWith(str, EXT_HTML)) {
                str = StringUtils.removeEnd(str, EXT_HTML);
            } else if (StringUtils.endsWith(str, EXT_JSON)) {
                str = StringUtils.removeEnd(str, EXT_JSON);
            } else if (StringUtils.endsWith(str, EXT_PROPERTIES)) {
                str = StringUtils.removeEnd(str, EXT_PROPERTIES);
            } else if (StringUtils.endsWith(str, EXT_XML)) {
                str = StringUtils.removeEnd(str, EXT_XML);
            }
            if (!StringUtils.equals(pathName, str)) {
                lookupPath = path.parent().child(str, false);
                meta = storage.lookup(lookupPath);
                if (meta == null && "index".equals(str)) {
                    lookupPath = path.parent();
                    meta = storage.lookup(lookupPath);
                }
            }
        }
        // Fix for Windows WebDAV (omitting trailing /)
        if (meta == null && !path.isIndex()) {
            lookupPath = path.parent().child(pathName, true);
            meta = storage.lookup(lookupPath);
        }
        return (meta == null) ? null : lookupPath;
    }
}
