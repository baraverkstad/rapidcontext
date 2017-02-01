/**
 * RapidContext HTTP plug-in <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2012 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the BSD license.
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
import java.net.URL;
import java.util.LinkedHashMap;

import org.apache.commons.lang.StringUtils;
import org.rapidcontext.core.data.TextEncoding;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;

/**
 * An HTTP POST procedure. This procedure provides simplified access
 * to HTTP data sending and retrieval.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class HttpPostProcedure extends HttpProcedure {

    /**
     * The binding name for the HTTP connection.
     */
    public static final String BINDING_CONNECTION = "connection";

    /**
     * The binding name for the HTTP URL.
     */
    public static final String BINDING_URL = "url";

    /**
     * The binding name for the additional HTTP headers.
     */
    public static final String BINDING_HEADERS = "headers";

    /**
     * The binding name for the HTTP POST data (name and value pairs).
     */
    public static final String BINDING_DATA = "data";

    /**
     * Creates a new HTTP POST procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public HttpPostProcedure() throws ProcedureException {
        super();
        defaults.set(BINDING_CONNECTION, Bindings.DATA, null,
                     "The HTTP connection pool name, set to blank for none.");
        defaults.set(BINDING_URL, Bindings.DATA, "",
                     "The HTTP URL, optionally containing argument template " +
                     "variables (e.g. ':arg' or '@arg'). May be blank or " +
                     "relative to the connection pool URL.");
        defaults.set(BINDING_HEADERS, Bindings.DATA, "",
                     "Any additional HTTP headers, optionally containing " +
                     "argument template variables (e.g. ':arg' or '@arg'). " +
                     "Headers are listed in 'Name: Value' pairs, separated " +
                     "by line breaks. Leave blank for default headers.");
        defaults.set(BINDING_DATA, Bindings.DATA, "",
                     "The HTTP payload data to send, optionally containing " +
                     "argument template variables (e.g. ':arg' or '@arg'). " +
                     "Data must be URL-encoded, but may be split into " +
                     "lines (automatically joined by '&' characters).");
        defaults.seal();
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
    protected static Object execCall(CallContext cx, Bindings bindings)
        throws ProcedureException {

        HttpChannel channel = getChannel(cx, bindings);
        URL url = getUrl(bindings, channel);
        LinkedHashMap headers = getHeaders(bindings, channel);
        String data = bindings.getValue(BINDING_DATA).toString();
        data = bindings.processTemplate(data, TextEncoding.URL);
        data = data.replace("\n", "&");
        data = data.replace("&&", "&");
        data = StringUtils.removeStart(data, "&");
        data = StringUtils.removeEnd(data, "&");
        HttpURLConnection con = setup(url, headers, data.length() > 0);
        try {
            setRequestMethod(con, "POST");
            send(cx, con, data);
            return receive(cx, con);
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
        }
        if (obj instanceof String) {
            String str = ((String) obj).trim();
            obj = (str.length() > 0) ? cx.connectionReserve(str) : null;
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
                return new URL(con.getUrl(), str);
            } else if (con != null) {
                return con.getUrl();
            } else {
                return new URL(str);
            }
        } catch (MalformedURLException e) {
            throw new ProcedureException("malformed URL: " + str);
        }
    }

    /**
     * Returns the additional HTTP headers from the bindings and/or
     * connection.
     *
     * @param bindings       the call bindings to use
     * @param con            the optional connection
     *
     * @return the parsed and prepared HTTP headers
     *
     * @throws ProcedureException if the bindings couldn't be read
     */
    private static LinkedHashMap getHeaders(Bindings bindings, HttpChannel con)
    throws ProcedureException {

        LinkedHashMap headers = new LinkedHashMap();
        if (con != null) {
            addHeaders(headers, con.getHeaders());
        }
        String str = "";
        if (bindings.hasName("header")) {
            // TODO: Remove this legacy binding name (2017-02-01)
            str = bindings.getValue("header", "").toString();
        } else if (bindings.hasName(BINDING_HEADERS)) {
            str = bindings.getValue(BINDING_HEADERS, "").toString();
        }
        addHeaders(headers, bindings.processTemplate(str, TextEncoding.NONE));
        return headers;
    }
}
