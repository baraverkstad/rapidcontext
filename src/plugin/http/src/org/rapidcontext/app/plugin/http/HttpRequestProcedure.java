/*
 * RapidContext HTTP plug-in <https://www.rapidcontext.com/>
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

package org.rapidcontext.app.plugin.http;

import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static org.rapidcontext.util.HttpUtil.Helper.hasContent;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.JsonSerializer;
import org.rapidcontext.core.data.TextEncoding;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.web.Mime;

/**
 * An HTTP request procedure for any HTTP method. This procedure
 * provides simple access to HTTP request sending and data retrieval.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class HttpRequestProcedure extends Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG = Logger.getLogger(HttpRequestProcedure.class.getName());

    /**
     * The binding name for the HTTP connection.
     */
    public static final String BINDING_CONNECTION = "connection";

    /**
     * The binding name for the HTTP URL.
     */
    public static final String BINDING_URL = "url";

    /**
     * The binding name for the HTTP method.
     */
    public static final String BINDING_METHOD = "method";

    /**
     * The binding name for the additional HTTP headers.
     */
    public static final String BINDING_HEADERS = "headers";

    /**
     * The binding name for the (optional) payload data.
     */
    public static final String BINDING_DATA = "data";

    /**
     * The binding name for the processing and mapping flags.
     */
    public static final String BINDING_FLAGS = "flags";

    /**
     * The shared default HTTP client used for all requests.
     */
    private static HttpClient defaultClient = null;

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public HttpRequestProcedure(String id, String type, Dict dict) {
        super(id, type, dict);
        if (!type.equals("procedure/http/request")) {
            this.dict.set(KEY_TYPE, "procedure/http/request");
            LOG.warning("deprecated: procedure " + id + " references legacy type: " + type);
        }
        try {
            Bindings b = new Bindings(null, dict.getArray(KEY_BINDING));
            if (b.hasName(BINDING_CONNECTION) && b.getType(BINDING_CONNECTION) != Bindings.CONNECTION) {
                LOG.warning("deprecated: procedure " + id + " connection binding has improper type: " + b.getTypeName(BINDING_CONNECTION));
            }
        } catch (ProcedureException e) {
            LOG.warning("deprecated: procedure " + id + " invalid connection binding: " + e);
        }
    }

    /**
     * Executes a call of this procedure in the specified context
     * and with the specified call bindings. The semantics of what
     * the procedure actually does, is up to each implementation.
     * Note that the call bindings are normally inherited from the
     * procedure bindings with arguments bound to their call values.
     *
     * @param cx             the procedure call context
     * @param bindings       the call bindings to use
     *
     * @return the result of the call, or
     *         null if the call produced no result
     *
     * @throws ProcedureException if the call execution caused an
     *             error
     */
    @Override
    public Object call(CallContext cx, Bindings bindings)
    throws ProcedureException {
        try {
            return execCall(cx, bindings);
        } catch (ProcedureException e) {
            throw new ProcedureException(this, e);
        }
    }

    /**
     * Executes a call of this procedure in the specified context
     * and with the specified call bindings.
     *
     * @param cx             the procedure call context
     * @param bindings       the call bindings in use
     *
     * @return the result of the call, or
     *         null if the call produced no result
     *
     * @throws ProcedureException if the call execution caused an
     *             error
     */
    @SuppressWarnings("resource")
    protected static Object execCall(CallContext cx, Bindings bindings)
    throws ProcedureException {
        HttpChannel channel = getChannel(cx, bindings);
        URI uri = getURI(bindings);
        String method = bindings.getValue(BINDING_METHOD).toString();
        TreeMap<String,String> headers = getHeaders(bindings);
        String flags = bindings.getValue(BINDING_FLAGS, "").toString();
        boolean jsonData = hasFlag(flags, "json", false);
        boolean jsonError = hasFlag(flags, "jsonerror", false);
        boolean metadata = hasFlag(flags, "metadata", false);
        if (channel != null) {
            URI baseUri = channel.uri();
            uri = (uri == null) ? baseUri : baseUri.resolve(uri);
            TreeMap<String,String> baseHeaders = channel.headers();
            baseHeaders.putAll(headers);
            headers = baseHeaders;
        }
        String data = getRequestContent(method, headers, bindings);
        long startTime = System.currentTimeMillis();
        try {
            HttpClient client = defaultClient();
            HttpRequest req = buildRequest(uri, method, headers, data);
            HttpLog.logRequest(cx, req, data);
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            HttpLog.logResponse(cx, resp);
            Object res = buildResponse(cx, resp, metadata, jsonData, jsonError);
            if (channel != null) {
                channel.report(startTime, true, null);
            }
            return res;
        } catch (ProcedureException e) {
            if (channel != null) {
                channel.report(startTime, false, e.getMessage());
            }
            throw e;
        } catch (IllegalArgumentException | IOException | InterruptedException e) {
            if (channel != null) {
                channel.report(startTime, false, e.getMessage());
            }
            throw new ProcedureException(e.getMessage());
        }
    }

    /**
     * Returns the optional HTTP connection from the bindings.
     *
     * @param cx             the procedure call context
     * @param bindings       the call bindings in use
     *
     * @return the HTTP connection to use, or null for none
     *
     * @throws ProcedureException if the bindings couldn't be read, or
     *             if the connection was invalid
     */
    private static HttpChannel getChannel(CallContext cx, Bindings bindings)
    throws ProcedureException {
        Object obj = null;
        if (bindings.hasName(BINDING_CONNECTION)) {
            obj = bindings.getValue(BINDING_CONNECTION, null);
            boolean isArg = bindings.getType(BINDING_CONNECTION) == Bindings.ARGUMENT;
            if (obj instanceof String s) {
                String perm = cx.readPermission(isArg ? 1 : 0);
                obj = s.isBlank() ? null : cx.connectionReserve(s.trim(), perm);
            }
        }
        if (obj instanceof HttpChannel channel) {
            return channel;
        } else if (obj != null) {
            throw new ProcedureException("connection not of HTTP type: " +
                                         obj.getClass().getName());
        } else {
            return null;
        }
    }

    /**
     * Returns the URI from the bindings and processes template variables.
     *
     * @param bindings       the call bindings in use
     *
     * @return the URI if set, or null otherwise
     *
     * @throws ProcedureException if the bindings couldn't be read, or
     *             if the URL was malformed
     */
    private static URI getURI(Bindings bindings) throws ProcedureException {
        String str = bindings.getValue(BINDING_URL, "").toString().trim();
        str = bindings.processTemplate(str, TextEncoding.URL);
        try {
            return str.isEmpty() ? null : new URI(str);
        } catch (Exception e) {
            throw new ProcedureException("invalid URL; " + e.getMessage() + ": "+ str);
        }
    }

    /**
     * Returns HTTP headers from the bindings and processes template variables.
     *
     * @param bindings       the call bindings in use
     *
     * @return the parsed and prepared HTTP headers
     *
     * @throws ProcedureException if the bindings couldn't be read
     */
    private static TreeMap<String,String> getHeaders(Bindings bindings)
    throws ProcedureException {
        String str = "";
        if (bindings.hasName("header")) {
            str = bindings.getValue("header", "").toString();
            LOG.warning("deprecated: legacy 'header' binding used, renamed to 'headers'");
        } else if (bindings.hasName(BINDING_HEADERS)) {
            str = bindings.getValue(BINDING_HEADERS, "").toString();
        }
        str = bindings.processTemplate(str, TextEncoding.NONE);
        return parseHeaders(str);
    }

    /**
     * Parses HTTP headers from a multi-line string. Empty lines are ignored.
     *
     * @param headers        the header text to parse
     *
     * @return the parsed map of HTTP headers
     *
     * @throws ProcedureException if an HTTP header line was invalid
     */
    protected static TreeMap<String,String> parseHeaders(String headers)
    throws ProcedureException {
        TreeMap<String,String> res = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String line : headers.split("[\\n\\r]+")) {
            String[] parts = line.split("\\s*:\\s*", 2);
            if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                res.put(parts[0].trim(), parts[1].trim());
            } else if (!line.isBlank()) {
                throw new ProcedureException("invalid HTTP header: " + line);
            }
        }
        return res;
    }

    /**
     * Returns HTTP encoded request content if applicable.
     *
     * @param method         the HTTP request method
     * @param headers        the HTTP request headers
     * @param bindings       the call bindings in use
     *
     * @return the encoded request content, or
     *         null if no content should be sent
     *
     * @throws ProcedureException if the bindings couldn't be read
     */
    private static String getRequestContent(
        String method,
        TreeMap<String,String> headers,
        Bindings bindings
    ) throws ProcedureException {
        String data = bindings.getValue(BINDING_DATA).toString();
        boolean hasData = hasContent(method) && data.length() > 0;
        if (hasData) {
            String contentType = headers.get("Content-Type");
            if (contentType == null) {
                contentType = Mime.WWW_FORM[0];
                headers.put("Content-Type", contentType);
            }
            if (!contentType.contains("charset=")) {
                headers.put("Content-Type", contentType + "; charset=utf-8");
            }
            if (Mime.isMatch(contentType, Mime.WWW_FORM)) {
                data = bindings.processTemplate(data, TextEncoding.URL);
                data = data.replace("\n", "&");
                data = data.replace("&&", "&");
                data = StringUtils.removeStart(data, "&");
                data = StringUtils.removeEnd(data, "&");
            } else if (Mime.isMatch(contentType, Mime.JSON)) {
                data = bindings.processTemplate(data, TextEncoding.JSON);
            } else if (Mime.isMatch(contentType, Mime.XML)) {
                data = bindings.processTemplate(data, TextEncoding.XML);
            } else {
                data = bindings.processTemplate(data, TextEncoding.NONE);
            }
        }
        return hasData ? data : null;
    }

    /**
     * Checks if a specified flag is set (or unset). I.e. this method
     * both checks for "no-whatever" and "whatever" in the flags
     * string. If none of the two variants is found, the default
     * value is returned.
     *
     * @param flags          the flags string to check
     * @param key            the flag identifier
     * @param defval         the default flag value
     *
     * @return true if the flag was set, or
     *         false otherwise
     */
    private static boolean hasFlag(String flags, String key, boolean defval) {
        return !StringUtils.contains(flags, "no-" + key) &&
               (StringUtils.contains(flags, key) || defval);
    }

    /**
     * Returns a default HTTP client.
     *
     * @return the HTTP client
     */
    protected static HttpClient defaultClient() {
        if (defaultClient == null) {
            synchronized (HttpRequestProcedure.class) {
                defaultClient = HttpClient.newBuilder()
                        .version(Version.HTTP_2)
                        .followRedirects(Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
            }
        }
        return defaultClient;
    }

    /**
     * Returns an HTTP request for the specified URI, method and data.
     *
     * @param uri            the URI to use
     * @param method         the HTTP method
     * @param headers        the HTTP headers to send
     * @param data           the request content, or null for none
     *
     * @return the HTTP request
     *
     * @throws IllegalArgumentException if the URI scheme was invalid
     */
    protected static HttpRequest buildRequest(
        URI uri,
        String method,
        TreeMap<String,String> headers,
        String data
    ) throws IllegalArgumentException {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri);
        builder.method(method, (data == null) ? noBody() : ofString(data));
        builder.timeout(Duration.ofSeconds(45));
        builder.setHeader("Cache-Control", "no-cache");
        builder.setHeader("Accept", "text/*, application/*");
        builder.setHeader("Accept-Charset", "UTF-8");
        ApplicationContext ctx = ApplicationContext.getInstance();
        String ver = ctx.version().get("version", String.class, "1.0");
        builder.setHeader("User-Agent", "RapidContext/" + ver);
        headers.forEach((name, value) -> builder.setHeader(name, value));
        return builder.build();
    }

    /**
     * Returns the processed HTTP response data.
     *
     * @param cx             the procedure call context
     * @param resp           the HTTP response
     * @param metadata       the response wrapper flag
     * @param jsonData       the JSON response data flag
     * @param jsonError      the JSON response error flag
     *
     * @return the HTTP response data
     *
     * @throws ProcedureException if the response couldn't be read or
     *             contained an HTTP error code
     */
    private static Object buildResponse(
        CallContext cx,
        HttpResponse<String> resp,
        boolean metadata,
        boolean jsonData,
        boolean jsonError
    ) throws ProcedureException {
        int httpCode = resp.statusCode();
        boolean success = (httpCode / 100 == 2);
        String text = resp.body();
        Object data = text;
        if ((jsonData && success) || jsonError) {
            try {
                data = JsonSerializer.unserialize(text);
            } catch (Exception e) {
                String msg = "invalid json: " + e.getMessage();
                LOG.log(Level.INFO, msg, e);
                throw new ProcedureException(msg);
            }
        }
        if (metadata) {
            Dict headers = new Dict();
            resp.headers().map().forEach((key, values) -> {
                if (key != null && !key.isBlank() && !key.startsWith(":")) {
                    headers.add(key, (values.size() == 1) ? values.get(0) : Array.of(values));
                }
            });
            return new Dict()
                .set("success", success)
                .set("responseCode", httpCode)
                .set("headers", headers)
                .set("data", success ? data : null)
                .set("error", success ? null : data);
        } else if (success || jsonError) {
            return data;
        } else {
            String msg = "HTTP " + httpCode;
            if (!text.isBlank()) {
                msg += ": " + text;
            }
            throw new ProcedureException(msg);
        }
    }
}
