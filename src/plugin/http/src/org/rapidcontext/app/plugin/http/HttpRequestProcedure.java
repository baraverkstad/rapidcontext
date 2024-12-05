/*
 * RapidContext HTTP plug-in <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2024 Per Cederberg. All rights reserved.
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

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.TextEncoding;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.web.Mime;

/**
 * An HTTP request procedure for any HTTP method. This procedure
 * provides simple access to HTTP request sending and data retrieval.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class HttpRequestProcedure extends HttpProcedure {

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
            Bindings b = getBindings();
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

        return execCall(cx, bindings);
    }

    /**
     * Executes a call of this procedure in the specified context
     * and with the specified call bindings.
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
    static Object execCall(CallContext cx, Bindings bindings)
    throws ProcedureException {

        String method = bindings.getValue(BINDING_METHOD).toString();
        String flags = bindings.getValue(BINDING_FLAGS, "").toString();
        boolean jsonData = hasFlag(flags, "json", false);
        boolean jsonError = hasFlag(flags, "jsonerror", false);
        boolean metadata = hasFlag(flags, "metadata", false);
        HttpChannel channel = getChannel(cx, bindings);
        URL url = getUrl(bindings, channel);
        String headers = getHeaders(bindings);
        String data = bindings.getValue(BINDING_DATA).toString();
        HttpURLConnection con = setup(url, data.length() > 0);
        long startTime = System.currentTimeMillis();
        try {
            if (channel != null) {
                setRequestHeaders(con, channel.getHeaders());
            }
            setRequestHeaders(con, headers);
            setRequestMethod(con, method);
            String contentType = con.getRequestProperty("Content-Type");
            if (contentType == null || Mime.isMatch(contentType, Mime.WWW_FORM)) {
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
            send(cx, con, data);
            Object res = receive(cx, con, metadata, jsonData, jsonError);
            if (channel != null) {
                channel.report(startTime, true, null);
            }
            return res;
        } catch (ProcedureException e) {
            if (channel != null) {
                channel.report(startTime, false, e.getMessage());
            }
            throw e;
        } finally {
            con.disconnect();
        }
    }

    /**
     * Returns the optional HTTP connection from the bindings.
     *
     * @param cx             the procedure call context
     * @param bindings       the call bindings to use
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
        if (obj != null && !(obj instanceof HttpChannel)) {
            throw new ProcedureException("connection not of HTTP type: " +
                                         obj.getClass().getName());
        }
        return (HttpChannel) obj;
    }

    /**
     * Returns the URL from the bindings and/or connection.
     *
     * @param bindings       the call bindings to use
     * @param con            the optional connection
     *
     * @return the resolved URL
     *
     * @throws ProcedureException if the bindings couldn't be read, or
     *             if the URL was malformed
     */
    private static URL getUrl(Bindings bindings, HttpChannel con)
    throws ProcedureException {

        String str = bindings.getValue(BINDING_URL, "").toString().trim();
        str = bindings.processTemplate(str, TextEncoding.URL);
        try {
            if (con != null && !str.isEmpty()) {
                return con.getUrl().toURI().resolve(str).toURL();
            } else if (con != null) {
                return con.getUrl();
            } else {
                return new URI(str).toURL();
            }
        } catch (MalformedURLException | URISyntaxException | IllegalArgumentException e) {
            throw new ProcedureException("invalid URL; " + e.getMessage() + ": "+ str);
        }
    }

    /**
     * Returns the additional HTTP headers from the bindings.
     *
     * @param bindings       the call bindings to use
     *
     * @return the parsed and prepared HTTP headers
     *
     * @throws ProcedureException if the bindings couldn't be read
     */
    private static String getHeaders(Bindings bindings)
    throws ProcedureException {

        String str = "";
        if (bindings.hasName("header")) {
            str = bindings.getValue("header", "").toString();
            LOG.warning("deprecated: legacy 'header' binding used, renamed to 'headers'");
        } else if (bindings.hasName(BINDING_HEADERS)) {
            str = bindings.getValue(BINDING_HEADERS, "").toString();
        }
        return bindings.processTemplate(str, TextEncoding.NONE);
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
}
