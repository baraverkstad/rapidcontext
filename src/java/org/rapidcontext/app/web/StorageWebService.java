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

package org.rapidcontext.app.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.proc.StorageDeleteProcedure;
import org.rapidcontext.app.proc.StorageWriteProcedure;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.HtmlSerializer;
import org.rapidcontext.core.data.JsonSerializer;
import org.rapidcontext.core.data.PropertiesSerializer;
import org.rapidcontext.core.data.XmlSerializer;
import org.rapidcontext.core.data.YamlSerializer;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.Index;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.WebService;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.core.web.Request;
import org.rapidcontext.util.FileUtil;

/**
 * A storage API web service. This service is used for accessing the
 * raw data storage through HTTP and provides a view of the
 * storage hierarchy similar to a file system.
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
        METHOD.DELETE
    };

    /**
     * The HTML file extension.
     */
    public static final String EXT_HTML = ".html";

    /**
     * The XML file extension.
     */
    public static final String EXT_XML = ".xml";

    /**
     * The list of supported file extensions.
     */
    public static final String[] EXT_ALL = {
        EXT_HTML, Storage.EXT_JSON, Storage.EXT_PROPERTIES, EXT_XML, Storage.EXT_YAML
    };

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
        // TODO: Change to read-access here once a solution has been devised
        //       to disable search queries for certain paths and/or users.
        if (!SecurityContext.hasWriteAccess(request.getPath())) {
            errorUnauthorized(request);
            return;
        }
        Path path = new Path(request.getPath());
        Metadata meta = lookup(path);
        boolean isExact = meta != null && path.equals(meta.path());
        boolean isHtml = !isExact && StringUtils.endsWithIgnoreCase(path.name(), EXT_HTML);
        boolean isJson = !isExact && StringUtils.endsWithIgnoreCase(path.name(), Storage.EXT_JSON);
        boolean isProps = !isExact && StringUtils.endsWithIgnoreCase(path.name(), Storage.EXT_PROPERTIES);
        boolean isXml = !isExact && StringUtils.endsWithIgnoreCase(path.name(), EXT_XML);
        boolean isYaml = !isExact && StringUtils.endsWithIgnoreCase(path.name(), Storage.EXT_YAML);
        try {
            Storage storage = ApplicationContext.getInstance().getStorage();
            Object res = (meta == null) ? null : storage.load(meta.path());
            if (res instanceof Index) {
                res = serializeIndex((Index) res, isExact || isHtml);
            } else if (res instanceof Binary && !isExact) {
                res = serializeBinary((Binary) res, request, path);
            }
            // Render result as raw data, Properties, HTML, JSON or XML
            if (meta == null || res == null) {
                errorNotFound(request);
            } else if (res instanceof Binary) {
                request.sendBinary((Binary) res);
            } else if (isExact || isHtml) {
                sendHtml(request, meta.path(), meta, res);
            } else if (isJson) {
                request.sendText(Mime.JSON[0], JsonSerializer.serialize(res, true));
            } else if (isProps) {
                request.sendText(Mime.TEXT[0], PropertiesSerializer.serialize(res));
            } else if (isXml) {
                request.sendText(Mime.XML[0], XmlSerializer.serialize("results", res));
            } else if (isYaml) {
                request.sendText(Mime.YAML[0], YamlSerializer.serialize(res));
            } else {
                request.sendError(STATUS.NOT_ACCEPTABLE);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed to process storage request", e);
            // TODO: How do users want their error messages?
            Dict dict = new Dict();
            dict.set("error", e.toString());
            if (isJson) {
                request.sendText(Mime.JSON[0], JsonSerializer.serialize(dict, true));
            } else if (isXml) {
                request.sendText(Mime.XML[0], XmlSerializer.serialize("error", dict));
            } else if (isYaml) {
                request.sendText(Mime.YAML[0], YamlSerializer.serialize(dict));
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
        String link = meta.isIndex() ? "index" : path.name();
        html.append(" &nbsp;<a href='" + link + Storage.EXT_JSON + "'>JSON</a>");
        html.append(" &nbsp;<a href='" + link + Storage.EXT_PROPERTIES + "'>PROPERTIES</a>");
        html.append(" &nbsp;<a href='" + link + EXT_XML + "'>XML</a>");
        html.append(" &nbsp;<a href='" + link + Storage.EXT_YAML + "'>YAML</a>");
        if (meta.isBinary()) {
            html.append(" &nbsp;<a href='" + link + "'>RAW</a>");
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
        boolean isDataPath = (
            !idx.path().startsWith(FileWebService.PATH_FILES) &&
            !idx.path().startsWith(Storage.PATH_STORAGE)
        );
        Array indices = new Array();
        idx.indices().filter((item) -> !item.startsWith(".")).forEach((item) -> {
            indices.add((linkify ? "$href$" : "") + item + "/");
        });
        Array objects = new Array();
        idx.objects().filter((item) -> !item.startsWith(".")).forEach((item) -> {
            if (isDataPath) {
                for (String ext : Storage.EXT_ALL) {
                    if (StringUtils.endsWithIgnoreCase(item, ext)) {
                        item = StringUtils.removeEndIgnoreCase(item, ext);
                        if (idx.hasObject(item)) {
                            item = null;
                        }
                        break;
                    }
                }
            }
            if (linkify && item != null) {
                objects.add("$href$" + item + ".html$" + item);
            } else if (item != null) {
                objects.add(item);
            }
        });
        Dict dict = new Dict();
        dict.set("type", "index");
        dict.set("directories", indices);
        dict.set("objects", objects);
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
        Dict dict = new Dict();
        dict.set("type", "file");
        dict.set("name", path.name());
        dict.set("mimeType", data.mimeType());
        dict.set("size", Long.valueOf(data.size()));
        if (Mime.isText(data.mimeType())) {
            try (InputStream is = data.openStream()) {
                dict.set("text", FileUtil.readText(is, "UTF-8"));
            } catch (Exception e) {
                String msg = "invalid data read: " + e.getMessage();
                LOG.log(Level.WARNING, msg, e);
            }
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

        dict = meta.serialize();
        dict.remove(Metadata.KEY_ID);
        dict.remove(Metadata.KEY_TYPE);
        dict.set("processTime", Long.valueOf(request.getProcessTime()));
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
            Object data = JsonSerializer.unserialize(request.getInputString());
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
                request.sendError(STATUS.BAD_REQUEST, null, msg);
            } else if (!(data instanceof Dict)) {
                String msg = "patch data should be JSON object";
                request.sendError(STATUS.NOT_ACCEPTABLE, null, msg);
            } else {
                dict.setAll((Dict) data);
                storage.store(path, dict);
                request.sendText(Mime.JSON[0], JsonSerializer.serialize(dict, true));
            }
        } catch (Exception e) {
            String msg = "failed to write " + request.getPath() + ": " + e.getMessage();
            LOG.log(Level.WARNING, msg, e);
            errorBadRequest(request, msg);
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
            Metadata meta = lookup(path);
            Object data = JsonSerializer.unserialize(request.getInputString());
            if (StorageWriteProcedure.store(path, data)) {
                if (meta == null) {
                    request.sendText(STATUS.CREATED, null, null);
                } else {
                    request.sendText(STATUS.OK, null, null);
                }
            } else {
                String msg = "failed to write " + request.getPath();
                LOG.warning(msg);
                errorInternal(request, msg);
            }
        } catch (Exception e) {
            String msg = "failed to write " + request.getPath() + ": " + e.getMessage();
            LOG.log(Level.WARNING, msg, e);
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
        Path path = new Path(request.getPath());
        Metadata meta = lookup(path);
        try (InputStream is = request.getInputStream()) {
            Binary data = new Binary.BinaryStream(is, -1);
            if (StorageWriteProcedure.store(path, data)) {
                if (meta == null) {
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
        Metadata meta = lookup(new Path(request.getPath()));
        if (meta == null) {
            errorNotFound(request);
            return;
        }
        if (StorageDeleteProcedure.delete(meta.path())) {
            request.sendText(STATUS.NO_CONTENT, null, null);
        } else {
            String msg = "failed to delete " + request.getPath();
            errorInternal(request, msg);
        }
    }

    /**
     * Searches for metadata about a specified path. If no object is
     * found, a second attempt is made without any supported data
     * format extension.
     *
     * @param path           the storage location
     *
     * @return the metadata for the object, or null if not found
     */
    private Metadata lookup(Path path) {
        Storage storage = ApplicationContext.getInstance().getStorage();
        Metadata meta = storage.lookup(path);
        if (meta == null) {
            for (String ext : EXT_ALL) {
                if (StringUtils.endsWithIgnoreCase(path.name(), ext)) {
                    String name = StringUtils.removeEndIgnoreCase(path.name(), ext);
                    boolean isIndex = StringUtils.equalsIgnoreCase(name, "index");
                    path = isIndex ? path.parent() : path.parent().child(name, false);
                    meta = storage.lookup(path);
                    break;
                }
            }
        }
        return meta;
    }
}
