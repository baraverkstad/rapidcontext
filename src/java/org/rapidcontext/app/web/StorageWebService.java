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
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.proc.StorageDeleteProcedure;
import org.rapidcontext.app.proc.StorageWriteProcedure;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.JsonSerializer;
import org.rapidcontext.core.data.PropertiesSerializer;
import org.rapidcontext.core.data.XmlSerializer;
import org.rapidcontext.core.data.YamlSerializer;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.Index;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.RootStorage;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.WebService;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.core.web.Request;
import org.rapidcontext.util.DateUtil;

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
     * The list of supported file extensions.
     */
    public static final String[] EXT_ALL = {
        EXT_HTML, Storage.EXT_JSON, Storage.EXT_PROPERTIES, Storage.EXT_XML, Storage.EXT_YAML
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
        Storage storage = ApplicationContext.getInstance().getStorage();
        Path path = Path.from(request.getPath());
        Metadata meta = lookup(path);
        Object data = (meta == null) ? null : storage.load(meta.path());
        if (meta == null || data == null) {
            errorNotFound(request);
        } else if (data instanceof Binary && path.equals(meta.path())) {
            request.sendBinary((Binary) data);
        } else {
            boolean isHtml = path.isIndex() ||
                    StringUtils.endsWithIgnoreCase(path.name(), EXT_HTML);
            if (data instanceof Index) {
                data = prepareIndex(meta.path(), (Index) data, isHtml);
            } else if (data instanceof Binary) {
                data = prepareBinary(meta.path(), (Binary) data, isHtml);
            }
            sendResult(request, meta.path(), prepareMetadata(meta, isHtml), data);
        }
    }

    private void sendResult(Request request, Path path, Dict meta, Object data) {
        boolean includeMeta = BooleanUtils.toBoolean(request.getParameter("metadata", "0"));
        if (includeMeta) {
            Dict res = new Dict();
            res.add("data", data);
            res.add("metadata", meta);
            data = res;
        }
        meta.set("processTime", Long.valueOf(request.getProcessTime()));
        if (StringUtils.endsWithIgnoreCase(request.getPath(), Storage.EXT_JSON)) {
            request.sendText(Mime.JSON[0], JsonSerializer.serialize(data, true));
        } else if (StringUtils.endsWithIgnoreCase(request.getPath(), Storage.EXT_PROPERTIES)) {
            try {
                request.sendText(Mime.TEXT[0], PropertiesSerializer.serialize(data));
            } catch (IOException e) {
                LOG.log(Level.WARNING, "error serializing properties", e);
                request.sendText(Mime.TEXT[0], e.toString());
            }
        } else if (StringUtils.endsWithIgnoreCase(request.getPath(), Storage.EXT_XML)) {
            String root = includeMeta ? "result" : "data";
            request.sendText(Mime.XML[0], XmlSerializer.serialize(root, data));
        } else if (StringUtils.endsWithIgnoreCase(request.getPath(), Storage.EXT_YAML)) {
            request.sendText(Mime.YAML[0], YamlSerializer.serialize(data));
        } else {
            request.sendText(Mime.HTML[0], HtmlRenderer.render(path, meta, data));
        }
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
            Path path = Path.from(request.getPath());
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
            Path path = Path.from(request.getPath());
            Metadata meta = lookup(path);
            // FIXME: write to existing path if metadata exists?
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
        Path path = Path.from(request.getPath());
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
        Metadata meta = lookup(Path.from(request.getPath()));
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
                    path = isIndex ? path.parent() : path.sibling(name);
                    meta = storage.lookup(path);
                    break;
                }
            }
        }
        return meta;
    }

    /**
     * Creates an external representation of a metadata object.
     *
     * @param meta           the metadata object
     *
     * @return the external representation of the metadata
     */
    private Dict prepareMetadata(Metadata meta, boolean linkify) {
        Dict dict = meta.serialize();
        dict.remove(Metadata.KEY_ID);
        dict.remove(Metadata.KEY_TYPE);
        if (linkify) {
            String root = StringUtils.repeat("../", meta.path().depth());
            Array arr = new Array();
            for (Object path : meta.storagePaths()) {
                String rel = StringUtils.removeStart(path.toString(), "/");
                arr.add("$href$" + root + rel + "$" + path);
            }
            dict.set("storagePaths", arr);
        }
        return dict;
    }

    /**
     * Creates an external representation of an index. If the HTML
     * link flag is set, all the references in the index will be
     * prefixed by "$href$" (being serialized into HTML links).
     *
     * @param path           the index base path
     * @param idx            the index to serialize
     * @param linkify        the HTML link flag
     *
     * @return the external representation of the index
     */
    private Dict prepareIndex(Path path, Index idx, boolean linkify) {
        boolean binary = RootStorage.isBinaryPath(path);
        Array indices = new Array();
        idx.indices().filter((item) -> !item.startsWith(".")).forEach((item) -> {
            indices.add((linkify ? "$href$" : "") + item + "/");
        });
        Array objects = new Array();
        idx.objects().filter((item) -> !item.startsWith(".")).forEach((item) -> {
            item = binary ? item : Storage.objectName(item);
            item = !linkify ? item : "$href$" + item + ".html$" + item;
            if (!objects.containsValue(item)) {
                objects.add(item);
            }
        });
        Dict dict = new Dict();
        dict.set("type", "index");
        dict.set("lastModified", idx.lastModified());
        dict.set("directories", indices);
        dict.set("objects", objects);
        return dict;
    }

    /**
     * Creates an external representation of a binary data object.
     *
     * @param path           the storage path
     * @param data           the binary data object
     * @param linkify        the HTML link flag
     *
     * @return the external representation of the binary data object
     */
    private Dict prepareBinary(Path path, Binary data, boolean linkify) {
        Dict dict = new Dict();
        dict.set("type", "file");
        dict.set("name", (linkify ? "$href$" : "") + path.name());
        dict.set("mimeType", data.mimeType());
        dict.set("lastModified", new Date(data.lastModified()));
        dict.set("size", Long.valueOf(data.size()));
        return dict;
    }

    /**
     * An HTML renderer for storage lookup results.
     */
    private static class HtmlRenderer {

        /**
         * Renders an HTML page for a storage lookup result.
         *
         * @param path           the storage path
         * @param meta           the meta-data to render
         * @param res            the data to render
         *
         * @return the rendered HTML text
         */
        public static String render(Path path, Object meta, Object res) {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang='en'>\n");
            html.append("  <head>\n");
            html.append("    <meta charset='utf-8'>");
            html.append("      <link rel='stylesheet' href='");
            html.append(StringUtils.repeat("../", path.depth()));
            html.append("files/css/style.css'>\n");
            html.append("      <link rel='stylesheet' href='");
            html.append(StringUtils.repeat("../", path.depth()));
            html.append("files/app/storage/app.css'>\n");
            html.append("      <title>RapidContext Storage API</title>\n");
            html.append("</head>\n<body class='storage-app'>\n");
            html.append("<h1>RapidContext Storage API</h1>\n");
            html.append("<nav>\n<ol>\n");
            if (path.isRoot()) {
                html.append("<li class='active'>Start</li>\n");
            } else {
                html.append("<li><a href='");
                html.append(StringUtils.repeat("../", path.depth()));
                html.append("index.html'>Start</a></li>\n");
            }
            for (int i = 0; i < path.length(); i++) {
                if (i + 1 < path.length()) {
                    html.append("<li><a href='");
                    html.append(StringUtils.repeat("../", path.depth() - i - 1));
                    html.append("index.html'>");
                    html.append(path.name(i));
                    html.append("</a></li>\n");
                } else {
                    html.append("<li class='active'>");
                    html.append(path.name(i));
                    html.append("</li>\n");
                }
            }
            html.append("</ol>\n</nav>\n<hr>\n");
            html.append("<div class='metadata'>\n");
            html.append("<h2>Query Metadata</h2>\n");
            renderObject(meta, html);
            html.append("</div>\n");
            html.append("<div class='data'>\n");
            html.append("<h2>Query Results</h2>");
            renderObject(res, html);
            html.append("</div>\n<hr>\n");
            html.append("<p><strong>Data Formats:</strong>");
            String link = path.isIndex() ? "index" : path.name();
            html.append(" &nbsp;<a href='" + link + Storage.EXT_JSON + "'>JSON</a>");
            html.append(" &nbsp;<a href='" + link + Storage.EXT_PROPERTIES + "'>PROPERTIES</a>");
            html.append(" &nbsp;<a href='" + link + Storage.EXT_XML + "'>XML</a>");
            html.append(" &nbsp;<a href='" + link + Storage.EXT_YAML + "'>YAML</a>");
            html.append("</p>\n</body>\n</html>\n");
            return html.toString();
        }

        /**
         * Renders an object into an HTML representation.
         *
         * @param obj            the object to convert
         * @param buffer         the string buffer to append into
         */
        private static void renderObject(Object obj, StringBuilder buffer) {
            if (obj == null) {
                buffer.append("<code>N/A</code>");
            } else if (obj instanceof Dict) {
                renderDict((Dict) obj, buffer);
            } else if (obj instanceof Array) {
                renderArray((Array) obj, buffer);
            } else if (obj instanceof Date) {
                buffer.append("<time datetime='");
                buffer.append(DateUtil.asDateTimeUTC((Date) obj));
                buffer.append("'>");
                buffer.append(DateUtil.asDateTimeUTC((Date) obj));
                buffer.append(" <code>");
                buffer.append(DateUtil.asEpochMillis((Date) obj));
                buffer.append("</code></time>");
            } else if (obj instanceof Class) {
                renderText(((Class<?>) obj).getName(), buffer);
            } else if (obj instanceof StorableObject) {
                renderDict(((StorableObject) obj).serialize(), buffer);
            } else {
                renderText(obj.toString(), buffer);
            }
        }

        /**
         * Renders a dictionary into an HTML representation.
         *
         * @param dict           the dictionary to convert
         * @param buffer         the string buffer to append into
         */
        private static void renderDict(Dict dict, StringBuilder buffer) {
            buffer.append("<table>\n<tbody>\n");
            for (String key : dict.keys()) {
                buffer.append("<tr>\n<th>");
                renderText(key, buffer);
                buffer.append("</th>\n<td>");
                renderObject(dict.get(key), buffer);
                buffer.append("</td>\n</tr>\n");
            }
            buffer.append("</tbody>\n</table>\n");
        }

        /**
         * Renders an array into an HTML representation.
         *
         * @param arr            the array to convert
         * @param buffer         the string buffer to append into
         */
        private static void renderArray(Array arr, StringBuilder buffer) {
            buffer.append("<ol>\n");
            for (Object o : arr) {
                buffer.append("<li>");
                renderObject(o, buffer);
                buffer.append("</li>\n");
            }
            buffer.append("</ol>\n");
        }

        /**
         * Renders a text string into an HTML representation. If the
         * string contains a newline character, it will be wrapped in a
         * pre-tag. Otherwise it will only be properly HTML escaped. This
         * method also makes some rudimentary efforts to detect HTTP
         * links.
         *
         * @param str            the text string to convert
         * @param buffer         the string buffer to append into
         */
        private static void renderText(String str, StringBuilder buffer) {
            if (str == null) {
                buffer.append("<code>N/A</code>");
            } else if (str.startsWith("$href$")) {
                str = StringUtils.substringAfter(str, "$href$");
                String url = StringUtils.substringBefore(str, "$");
                String text = StringUtils.substringAfter(str, "$");
                text = StringUtils.defaultIfEmpty(text, url);
                buffer.append("<a href='");
                buffer.append(StringEscapeUtils.escapeHtml4(url));
                buffer.append("'>");
                buffer.append(StringEscapeUtils.escapeHtml4(text));
                buffer.append("</a>");
            } else if (str.indexOf("\n") >= 0) {
                buffer.append("<pre>");
                buffer.append(StringEscapeUtils.escapeHtml4(str));
                buffer.append("</pre>");
            } else {
                buffer.append(StringEscapeUtils.escapeHtml4(str));
            }
        }

        // No instances
        private HtmlRenderer() {}
    }
}
