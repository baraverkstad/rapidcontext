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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.rapidcontext.core.proc.AddOnProcedure;
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
public class HttpPostProcedure extends AddOnProcedure {

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
        defaults.set(BINDING_CONNECTION, Bindings.CONNECTION, null,
                     "The HTTP connection pool name, set to null for none.");
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

        String  str;
        Object  obj;

        obj = bindings.getValue(BINDING_CONNECTION, null);
        if (obj != null && !HttpChannel.class.isInstance(obj)) {
            str = "connection not of HTTP type: " + obj.getClass().getName();
            throw new ProcedureException(str);
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
        str = replaceArguments(str, bindings, true);
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
            parseHeaders(headers, con.getHeaders());
        }
        str = (String) bindings.getValue(BINDING_HEADER, "");
        parseHeaders(headers, replaceArguments(str, bindings, false));
        str = (String) bindings.getValue(BINDING_DATA);
        str = replaceArguments(str, bindings, true);
        return sendPostRequest(cx, createConnection(url, headers), str);
    }

    /**
     * Parses a string with HTTP headers into a result value map.
     *
     * @param map            the result name and value map
     * @param data           the unparsed header string
     */
    private static void parseHeaders(LinkedHashMap map, String data) {
        for (String line : data.split("[\\n\\r]+")) {
            String[] parts = line.split("\\s*:\\s*", 2);
            if (parts.length == 2) {
                map.put(parts[0].trim(), parts[1].trim());
            }
        }
    }

    /**
     * Replaces any parameters with the corresponding argument value
     * from the bindings. Optionally, this method also percent-encodes
     * (URL encodes) the argument values.
     *
     * @param data           the data string to process
     * @param bindings       the bindings to use
     * @param encode         the encode values flag
     *
     * @return the processed data string
     *
     * @throws ProcedureException if some parameter couldn't be found
     */
    private static String replaceArguments(String data,
                                           Bindings bindings,
                                           boolean encode)
        throws ProcedureException {

        String[]  names = bindings.getNames();
        String    value;

        for (int i = 0; i < names.length; i++) {
            if (bindings.getType(names[i]) == Bindings.ARGUMENT) {
                value = bindings.getValue(names[i], "").toString();
                if (encode) {
                    try {
                        value = URLEncoder.encode(value, "utf8");
                    } catch (UnsupportedEncodingException e) {
                        throw new ProcedureException("unsupported encoding", e);
                    }
                }
                data = StringUtils.replace(data, ":" + names[i], value.toString());
            }
        }
        return data;
    }

    /**
     * Creates an HTTP connection for the specified URL and headers.
     *
     * @param url            the URL to use
     * @param headers        the additional HTTP headers
     *
     * @return the HTTP connection created
     *
     * @throws ProcedureException if the connection couldn't be created
     */
    private static HttpURLConnection createConnection(URL url, Map headers)
    throws ProcedureException {

        HttpURLConnection  con;
        String             msg;

        try {
            con = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            msg = "failed to open URL " + url + ":" + e.getMessage();
            throw new ProcedureException(msg);
        }
        con.setDoInput(true);
        con.setAllowUserInteraction(false);
        con.setUseCaches(false);
        con.setInstanceFollowRedirects(false);
        con.setConnectTimeout(10000);
        con.setReadTimeout(45000);
        con.setRequestProperty("Cache-Control", "no-cache");
        con.setRequestProperty("Accept", "text/*, application/*");
        con.setRequestProperty("Accept-Charset", "UTF-8");
        con.setRequestProperty("Accept-Encoding", "identity");
        // TODO: Extract correct version number from JAR file
        con.setRequestProperty("User-Agent", "RapidContext/1.0");
        Iterator iter = headers.keySet().iterator();
        while (iter.hasNext()) {
            String str = (String) iter.next();
            con.setRequestProperty(str, (String) headers.get(str));
        }
        return con;
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
            // Setup request
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            String mime = "application/x-www-form-urlencoded;charset=UTF-8";
            con.setRequestProperty("Content-Type", mime);
            byte[] dataBytes = data.getBytes("UTF-8");
            con.setRequestProperty("Content-Length", "" + dataBytes.length);
            if (cx.isTracing()) {
                logRequest(cx, con, data);
            }
            OutputStream os = con.getOutputStream();
            try {
                os.write(dataBytes);
            } finally {
                os.close();
            }

            // Send request & handle response
            int httpCode = con.getResponseCode();
            String httpMsg = con.getResponseMessage();
            String charset = guessResponseCharset(con);
            if (httpCode / 100 == 2) {
                data = readStream(con.getInputStream(), charset);
            } else {
                data = readStream(con.getErrorStream(), charset);
            }
            if (cx.isTracing()) {
                logResponse(cx, con, data);
            }
            if (httpCode / 100 == 2) {
                return data;
            } else {
                throw new ProcedureException("HTTP " + httpCode + " " + httpMsg);
            }
        } catch (IOException e) {
            throw new ProcedureException(e.getMessage());
        } finally {
            con.disconnect();
        }
    }

    /**
     * Attempts to guess the HTTP response character set based on the content
     * type header. Defaults to UTF-8 if no proper character set was specified.
     *
     * @param con            the HTTP connection
     *
     * @return the HTTP response character set
     */
    private static String guessResponseCharset(HttpURLConnection con) {
        String contentType = con.getContentType().replace(" ", "");
        for (String param : contentType.split(";")) {
            if (param.startsWith("charset=")) {
                return  param.split("=", 2)[1];
            }
        }
        return "UTF-8";
    }

    /**
     * Reads data from an input stream until it ends. The data is expected to
     * be in text format, using the specified encoding.
     *
     * @param is             the stream to read
     * @param charset        the character set to use
     *
     * @return the text read from the stream
     *
     * @throws IOException if the data couldn't be read properly
     */
    private static String readStream(InputStream is, String charset)
    throws IOException {

        StringBuilder   buffer = new StringBuilder();
        BufferedReader  reader;
        String          str;

        reader = new BufferedReader(new InputStreamReader(is, charset));
        while ((str = reader.readLine()) != null) {
            buffer.append(str);
        }
        is.close();
        return buffer.toString();
    }

    /**
     * Logs the HTTP request to the procedure call context.
     *
     * @param cx             the procedure call context
     * @param con            the HTTP connection
     * @param data           the HTTP request data
     */
    private static void logRequest(CallContext cx,
                                   HttpURLConnection con,
                                   String data) {

        cx.log("HTTP " + con.getRequestMethod() + " " + con.getURL());
        Iterator iter = con.getRequestProperties().keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            cx.log("  " + key + ": " + con.getRequestProperty(key));
        }
        if (data != null) {
            cx.log(data);
        }
    }

    /**
     * Logs the HTTP response to the procedure call context.
     *
     * @param cx             the procedure call context
     * @param con            the HTTP connection
     * @param data           the HTTP response data
     *
     * @throws IOException if the HTTP response couldn't be extracted
     */
    private static void logResponse(CallContext cx,
                                    HttpURLConnection con,
                                    String data)
    throws IOException {

        cx.log(con.getHeaderField(0));
        for (int i = 1; true; i++) {
            String key = con.getHeaderFieldKey(i);
            if (key == null) {
                break;
            }
            cx.log("  " + key + ": " + con.getHeaderField(i));
        }
        if (data != null) {
            cx.log(data);
        }
    }
}
