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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;

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
    public static final String BINDING_HEADER = "header";

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
                     "The HTTP URL to send the data to. May be relative to " +
                     "the connection pool URL.");
        defaults.set(BINDING_HEADER, Bindings.DATA, "",
                     "The additional HTTP headers or blank for none.");
        defaults.set(BINDING_DATA, Bindings.DATA, "",
                     "The HTTP POST data to send or blank for none.");
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

        Object obj = bindings.getValue(BINDING_CONNECTION, null);
        if (obj instanceof String) {
            String str = (String) obj;
            obj = (str.length() > 0) ? cx.connectionReserve(str) : null;
        }
        if (obj != null && !(obj instanceof HttpChannel)) {
            throw new ProcedureException("connection not of HTTP type: " +
                                         obj.getClass().getName());
        }
        return execCall(cx, (HttpChannel) obj, bindings);
    }

    /**
     * Executes a call of this procedure in the specified context
     * and with the specified call bindings.
     *
     * @param cx             the procedure call context
     * @param con            the HTTP connection or null for none
     * @param bindings       the call bindings to use
     *
     * @return the result of the call, or
     *         null if the call produced no result
     *
     * @throws ProcedureException if the call execution caused an
     *             error
     */
    static Object execCall(CallContext cx, HttpChannel con, Bindings bindings)
        throws ProcedureException {

        URL             url;
        LinkedHashMap   headers;
        String          str;

        str = bindings.getValue(BINDING_URL, "").toString();
        str = bindings.processTemplate(str, TextEncoding.URL);
        try {
            if (con != null && !str.isEmpty()) {
                url = new URL(con.getUrl(), str);
            } else if (con != null) {
                url = con.getUrl();
            } else {
                url = new URL(str);
            }
        } catch (MalformedURLException e) {
            throw new ProcedureException("malformed URL: " + str);
        }
        headers = new LinkedHashMap();
        if (con != null) {
            addHeaders(headers, con.getHeaders());
        }
        str = (String) bindings.getValue(BINDING_HEADER, "");
        addHeaders(headers, bindings.processTemplate(str, TextEncoding.NONE));
        str = (String) bindings.getValue(BINDING_DATA);
        str = bindings.processTemplate(str, TextEncoding.URL);
        str = str.replace("\n", "&");
        HttpURLConnection urlCon = setup(url, headers, str.length() > 0);
        return sendPostRequest(cx, urlCon, str);
    }

    /**
     * Sends an HTTP POST request and returns the response as a string.
     *
     * @param cx             the procedure call context
     * @param con            the HTTP connection to use
     * @param data           the HTTP POST payload (form URL-encoded)
     *
     * @return the response text data
     *
     * @throws ProcedureException if the request sending caused an
     *             exception
     */
    private static String sendPostRequest(CallContext cx,
                                          HttpURLConnection con,
                                          String data)
    throws ProcedureException {

        try {
            con.setRequestMethod("POST");
            send(cx, con, data);
            return receive(cx, con);
        } catch (IOException e) {
            logResponse(cx, con, null);
            throw new ProcedureException(e.getMessage());
        } finally {
            con.disconnect();
        }
    }
}
