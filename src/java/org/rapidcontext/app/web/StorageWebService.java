/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2025 Per Cederberg. All rights reserved.
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
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.text.StringEscapeUtils;
import org.rapidcontext.app.model.ApiUtil;
import org.rapidcontext.core.ctx.Context;
import org.rapidcontext.core.ctx.ThreadContext;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.JsonSerializer;
import org.rapidcontext.core.data.PropertiesSerializer;
import org.rapidcontext.core.data.XmlSerializer;
import org.rapidcontext.core.data.YamlSerializer;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.RootStorage;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.WebService;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.core.web.Request;
import org.rapidcontext.util.DateUtil;
import org.rapidcontext.util.ValueUtil;

/**
 * A storage API web service. This service is used for accessing the
 * raw data storage through HTTP and provides a view of the
 * storage hierarchy similar to a file system.
 *
 * @author Per Cederberg
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
        Method.GET,
        Method.POST,
        Method.PATCH,
        Method.PUT,
        Method.DELETE
    };

    /**
     * The HTML file extension.
     */
    public static final String EXT_HTML = ".html";

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
    @Override
    protected String[] methodsImpl(Request request) {
        return METHODS;
    }

    /**
     * Processes an HTTP OPTIONS request.
     *
     * @param request        the request to process
     */
    @Override
    protected void doOptions(Request request) {
        request.setResponseHeader(Header.ACCEPT_PATCH, Mime.JSON[0]);
        super.doOptions(request);
    }

    /**
     * Processes an HTTP GET request.
     *
     * @param request        the request to process
     */
    @Override
    protected void doGet(Request request) {
        // TODO: Change to read-access here once a solution has been devised
        //       to disable search queries for certain paths and/or users.
        if (!ThreadContext.active().hasWriteAccess(request.getPath())) {
            errorUnauthorized(request);
            return;
        }
        Storage storage = Context.active().storage();
        Path path = Path.from(request.getPath());
        Metadata meta = lookup(path);
        Object data = (meta == null) ? null : storage.load(meta.path());
        Dict opts = new Dict();
        opts.set("computed", Boolean.TRUE);
        opts.set("hidden", ValueUtil.bool(request.getParameter("hidden"), false));
        opts.set("metadata", ValueUtil.bool(request.getParameter("metadata"), false));
        if (meta == null || data == null) {
            errorNotFound(request);
        } else if (data instanceof Binary b && path.equals(meta.path())) {
            request.sendBinary(b);
        } else if (opts.get("metadata", Boolean.class, false)) {
            Object o = ApiUtil.serialize(meta, data, opts, false);
            sendResult(request, meta.path(), null, o);
        } else {
            Object m = ApiUtil.serialize(meta.path(), meta, opts, false);
            Object o = ApiUtil.serialize(meta.path(), data, opts, false);
            sendResult(request, meta.path(), m, o);
        }
    }

    private void sendResult(Request request, Path path, Object meta, Object data) {
        if (Strings.CI.endsWith(request.getPath(), Storage.EXT_JSON)) {
            request.sendText(Mime.JSON[0], JsonSerializer.serialize(data, true));
        } else if (Strings.CI.endsWith(request.getPath(), Storage.EXT_PROPERTIES)) {
            try {
                request.sendText(Mime.TEXT[0], PropertiesSerializer.serialize(data));
            } catch (IOException e) {
                LOG.log(Level.WARNING, "error serializing properties", e);
                request.sendText(Mime.TEXT[0], e.toString());
            }
        } else if (Strings.CI.endsWith(request.getPath(), Storage.EXT_XML)) {
            String root = (meta == null) ? "result" : "data";
            request.sendText(Mime.XML[0], XmlSerializer.serialize(root, data));
        } else if (Strings.CI.endsWith(request.getPath(), Storage.EXT_YAML)) {
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
    @Override
    protected void doPatch(Request request) {
        Path path = Path.from(request.getPath());
        String str = request.getHeader("X-Move-To");
        Path dst = (str == null) ? path : Path.from(str);
        if (!ThreadContext.active().hasReadAccess(path.toString())) {
            errorUnauthorized(request);
        } else if (!ThreadContext.active().hasWriteAccess(dst.toString())) {
            errorUnauthorized(request);
        } else if (path.isIndex()) {
            errorBadRequest(request, "cannot write data to a directory");
        } else if (!Mime.isInputMatch(request, Mime.JSON)) {
            String msg = "application/json content type required";
            request.sendError(Status.UNSUPPORTED_MEDIA_TYPE, null, msg);
        } else {
            try {
                Storage storage = Context.active().storage();
                Object patch = JsonSerializer.unserialize(request.getInputString());
                if (!(patch instanceof Dict)) {
                    String msg = "patch data should be JSON object";
                    request.sendError(Status.NOT_ACCEPTABLE, null, msg);
                } else if (ApiUtil.update(storage, path, dst, (Dict) patch)) {
                    path = Storage.objectPath(dst);
                    Dict opts = new Dict();
                    opts.set("computed", Boolean.TRUE);
                    Object o = ApiUtil.serialize(dst, storage.load(path), opts, false);
                    request.sendText(Mime.JSON[0], JsonSerializer.serialize(o, true));
                } else {
                    errorBadRequest(request, "failed to patch " + path);
                }
            } catch (Exception e) {
                String msg = "failed to patch " + path + ": " + e.getMessage();
                LOG.log(Level.WARNING, msg, e);
                errorBadRequest(request, msg);
            }
        }
    }

    /**
     * Processes an HTTP POST request.
     *
     * @param request        the request to process
     */
    @Override
    protected void doPost(Request request) {
        Path path = Path.from(request.getPath());
        if (!ThreadContext.active().hasWriteAccess(path.toString())) {
            errorUnauthorized(request);
        } else if (path.isIndex()) {
            errorBadRequest(request, "cannot write data to directory");
        } else if (!Mime.isInputMatch(request, Mime.JSON)) {
            String msg = "application/json content type required";
            request.sendError(Status.UNSUPPORTED_MEDIA_TYPE, null, msg);
        } else {
            try {
                Storage storage = Context.active().storage();
                Object data = JsonSerializer.unserialize(request.getInputString());
                if (ApiUtil.store(storage, path, data)) {
                    request.sendText(Status.NO_CONTENT, null, null);
                } else {
                    errorBadRequest(request, "failed to write " + path);
                }
            } catch (Exception e) {
                String msg = "failed to write " + path + ": " + e.getMessage();
                LOG.log(Level.WARNING, msg, e);
                errorBadRequest(request, msg);
            }
        }
    }

    /**
     * Processes an HTTP PUT request.
     *
     * @param request        the request to process
     */
    @Override
    protected void doPut(Request request) {
        Path path = Path.from(request.getPath());
        if (!ThreadContext.active().hasWriteAccess(path.toString())) {
            errorUnauthorized(request);
        } else if (request.getHeader(Header.CONTENT_RANGE) != null) {
            request.sendError(Status.NOT_IMPLEMENTED);
        } else if (path.isIndex()) {
            errorBadRequest(request, "cannot write data to a directory");
        } else {
            try (InputStream is = request.getInputStream()) {
                Storage storage = Context.active().storage();
                Binary data = new Binary.BinaryStream(is, -1);
                if (ApiUtil.store(storage, path, data)) {
                    request.sendText(Status.NO_CONTENT, null, null);
                } else {
                    errorBadRequest(request, "failed to write " + path);
                }
            } catch (Exception e) {
                String msg = "failed to write " + path + ": " + e.getMessage();
                LOG.log(Level.WARNING, msg, e);
                errorBadRequest(request, msg);
            }
        }
    }

    /**
     * Processes an HTTP DELETE request.
     *
     * @param request        the request to process
     */
    @Override
    protected void doDelete(Request request) {
        Storage storage = Context.active().storage();
        Path path = Path.from(request.getPath());
        if (!ThreadContext.active().hasWriteAccess(path.toString())) {
            errorUnauthorized(request);
        } else if (ApiUtil.delete(storage, path)) {
            request.sendText(Status.NO_CONTENT, null, null);
        } else {
            errorBadRequest(request, "failed to delete " + path);
        }
    }

    /**
     * Searches for metadata for a specified path. The path will be
     * normalized to locate files and objects without any specified
     * format extension.
     *
     * @param path           the storage location
     *
     * @return the metadata for the object, or null if not found
     */
    private Metadata lookup(Path path) {
        Storage storage = Context.active().storage();
        String name = Storage.objectName(path.name());
        name = name.equals(path.name()) ? Strings.CI.removeEnd(name, EXT_HTML) : name;
        boolean idx = Strings.CI.equalsAny(name, ".", "index");
        Stream<Path> stream;
        if (path.isIndex()) {
            stream = Stream.of(path);
        } else if (RootStorage.isBinaryPath(path)) {
            stream = Stream.of(path, idx ? path.parent() : path.sibling(name));
        } else {
            stream = Stream.of(idx ? path.parent() : path.sibling(name));
        }
        Stream<Metadata> metas = stream.map(p -> storage.lookup(p));
        return metas.filter(Objects::nonNull).findFirst().orElse(null);
    }

    /**
     * An HTML renderer for storage lookup results.
     */
    private static class HtmlRenderer {

        /**
         * Renders an HTML page for a storage lookup result.
         *
         * @param path           the object storage path
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
                html.append(".'>Start</a></li>\n");
            }
            for (int i = 0; i < path.length(); i++) {
                if (i + 1 < path.length()) {
                    html.append("<li><a href='");
                    html.append(StringUtils.repeat("../", path.depth() - i - 1));
                    html.append(".'>");
                    html.append(path.name(i));
                    html.append("</a></li>\n");
                } else {
                    html.append("<li class='active'>");
                    html.append(path.name(i));
                    html.append("</li>\n");
                }
            }
            html.append("</ol>\n</nav>\n<hr>\n");
            if (meta != null) {
                html.append("<div class='metadata'>\n");
                html.append("<h2>Query Metadata</h2>\n");
                renderObject(path, meta, html);
                html.append("</div>\n");
            }
            html.append("<div class='data'>\n");
            html.append("<h2>Query Results</h2>");
            renderObject(path, res, html);
            html.append("</div>\n<hr>\n");
            html.append("<p><strong>Data Formats:</strong>");
            String link = " &nbsp;<a href='" + (path.isIndex() ? "index" : path.name());
            html.append(link + Storage.EXT_JSON + "'>JSON</a>");
            html.append(link + Storage.EXT_PROPERTIES + "'>PROPERTIES</a>");
            html.append(link + Storage.EXT_XML + "'>XML</a>");
            html.append(link + Storage.EXT_YAML + "'>YAML</a>");
            html.append("</p>\n</body>\n</html>\n");
            return html.toString();
        }

        /**
         * Renders an object into an HTML representation.
         *
         * @param path           the object storage path
         * @param obj            the object to convert
         * @param buffer         the string buffer to append into
         */
        private static void renderObject(Path path, Object obj, StringBuilder buffer) {
            if (obj == null) {
                buffer.append("<code>N/A</code>");
            } else if (obj instanceof Dict d) {
                renderDict(path, d, buffer);
            } else if (obj instanceof Array a) {
                renderArray(path, a, buffer);
            } else if (obj instanceof Date dt) {
                buffer.append("<time datetime='");
                buffer.append(DateUtil.asDateTimeUTC(dt));
                buffer.append("'>");
                buffer.append(DateUtil.asDateTimeUTC(dt));
                buffer.append(" <code>");
                buffer.append(DateUtil.asEpochMillis(dt));
                buffer.append("</code></time>");
            } else if (obj instanceof Class<?> c) {
                renderText(c.getName(), buffer);
            } else if (obj instanceof Path p) {
                String suffix = p.isIndex() ? "" : EXT_HTML;
                if (p.equals(path)) {
                    renderLink(p.isIndex() ? "." : p.name(), p.toString(), buffer);
                } else if (p.startsWith(path)) {
                    String rel = p.toIdent(path.depth());
                    renderLink(rel + suffix, rel, buffer);
                } else {
                    String rel = StringUtils.repeat("../", path.depth()) + p.toIdent(0);
                    renderLink(rel + suffix, p.toString(), buffer);
                }
            } else {
                renderText(obj.toString(), buffer);
            }
        }

        /**
         * Renders a dictionary into an HTML representation.
         *
         * @param path           the object storage path
         * @param dict           the dictionary to convert
         * @param buffer         the string buffer to append into
         */
        private static void renderDict(Path path, Dict dict, StringBuilder buffer) {
            buffer.append("<table>\n<tbody>\n");
            for (String key : dict.keys()) {
                buffer.append("<tr>\n<th>");
                renderText(key, buffer);
                buffer.append("</th>\n<td>");
                renderObject(path, dict.get(key), buffer);
                buffer.append("</td>\n</tr>\n");
            }
            buffer.append("</tbody>\n</table>\n");
        }

        /**
         * Renders an array into an HTML representation.
         *
         * @param path           the object storage path
         * @param arr            the array to convert
         * @param buffer         the string buffer to append into
         */
        private static void renderArray(Path path, Array arr, StringBuilder buffer) {
            buffer.append("<ol>\n");
            for (Object o : arr) {
                buffer.append("<li>");
                renderObject(path, o, buffer);
                buffer.append("</li>\n");
            }
            buffer.append("</ol>\n");
        }

        /**
         * Renders a text string into an HTML representation. If the
         * string contains a newline character, it will be wrapped in a
         * pre-tag. Otherwise it will only be properly HTML escaped.
         *
         * @param str            the text string to convert
         * @param buffer         the string buffer to append into
         */
        private static void renderText(String str, StringBuilder buffer) {
            if (str == null) {
                buffer.append("<code>N/A</code>");
            } else if (str.indexOf("\n") >= 0) {
                buffer.append("<pre>");
                buffer.append(StringEscapeUtils.escapeHtml4(str));
                buffer.append("</pre>");
            } else {
                buffer.append(StringEscapeUtils.escapeHtml4(str));
            }
        }

        /**
         * Renders a link into an HTML representation.
         *
         * @param url            the URL to render
         * @param text           the link text
         * @param buffer         the string buffer to append into
         */
        private static void renderLink(String url, String text, StringBuilder buffer) {
            text = StringUtils.defaultIfEmpty(text, url);
            buffer.append("<a href='");
            buffer.append(StringEscapeUtils.escapeHtml4(url));
            buffer.append("'>");
            buffer.append(StringEscapeUtils.escapeHtml4(text));
            buffer.append("</a>");
        }

        // No instances
        private HtmlRenderer() {}
    }
}
