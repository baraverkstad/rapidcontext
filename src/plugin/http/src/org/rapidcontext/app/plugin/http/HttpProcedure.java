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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.JsonSerializer;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.util.FileUtil;

/**
 * The base class for HTTP request procedures. This class contains
 * helper methods to simplify working the HTTP connections.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public abstract class HttpProcedure extends Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(HttpProcedure.class.getName());

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public HttpProcedure(String id, String type, Dict dict) {
        super(id, type, dict);
    }

    /**
     * Creates a new HTTP connection for the specified URL.
     *
     * @param url            the URL to use
     * @param data           the send data (payload) flag
     *
     * @return the HTTP connection created
     *
     * @throws ProcedureException if the connection couldn't be created
     */
    protected static HttpURLConnection setup(URL url, boolean data)
    throws ProcedureException {

        HttpURLConnection con;
        try {
            con = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            String msg = "failed to open URL " + url + ":" + e.getMessage();
            throw new ProcedureException(msg);
        }
        con.setDoInput(true);
        con.setDoOutput(data);
        con.setAllowUserInteraction(false);
        con.setUseCaches(false);
        con.setInstanceFollowRedirects(false);
        con.setConnectTimeout(10000);
        con.setReadTimeout(45000);
        con.setRequestProperty("Connection", "close");
        con.setRequestProperty("Cache-Control", "no-cache");
        con.setRequestProperty("Accept", "text/*, application/*");
        con.setRequestProperty("Accept-Charset", "UTF-8");
        con.setRequestProperty("Accept-Encoding", "identity");
        // TODO: Extract correct version number from JAR file
        con.setRequestProperty("User-Agent", "RapidContext/1.0");
        if (data) {
            con.setRequestProperty("Content-Type", Mime.WWW_FORM[0]);
        }
        return con;
    }

    /**
     * Sets HTTP connection headers from a string of unparsed headers.
     *
     * @param con            the HTTP connection
     * @param headers        the headers to set
     */
    protected static void setRequestHeaders(HttpURLConnection con, String headers) {
        for (String line : headers.split("[\\n\\r]+")) {
            String[] parts = line.split("\\s*:\\s*", 2);
            if (parts.length == 2) {
                con.setRequestProperty(parts[0].trim(), parts[1].trim());
            }
        }
    }

    /**
     * Sets the HTTP method of the HTTP connection. This method uses
     * reflection if an "unsupported" method is specified (e.g.
     * "PATCH").
     *
     * @param con            the HTTP connection
     * @param method         the HTTP method
     *
     * @throws ProcedureException if the request method couldn't be set
     */
    protected static void setRequestMethod(HttpURLConnection con, String method)
    throws ProcedureException {
        try {
            con.setRequestMethod(method);
        } catch (ProtocolException ignore) {
            // Do nothing here
        } catch (SecurityException ignore) {
            // Do nothing here
        }
        if (!con.getRequestMethod().equals(method)) {
            try {
                FieldUtils.writeField(con, "method", method, true);
                String className = "sun.net.www.protocol.https.HttpsURLConnectionImpl";
                if (Class.forName(className).isInstance(con)) {
                    Object delegate = FieldUtils.readField(con, "delegate", true);
                    FieldUtils.writeField(delegate, "method", method, true);
                }
            } catch (Exception e) {
                String msg = "failed to use HTTP " + method + ": " + e.getMessage();
                LOG.log(Level.WARNING, msg, e);
                throw new ProcedureException(msg);
            }
        }
    }

    /**
     * Sends the HTTP request and uploads any data provided.
     *
     * @param cx             the procedure call context
     * @param con            the HTTP connection
     * @param data           the data payload, or null
     *
     * @throws ProcedureException if the connection couldn't be established
     */
    protected static void send(CallContext cx, HttpURLConnection con, String data)
    throws ProcedureException {

        try {
            byte[] bytes = null;
            if (data != null && data.length() > 0) {
                bytes = data.getBytes(StandardCharsets.UTF_8);
                con.setRequestProperty("Content-Length", "" + bytes.length);
            }
            logRequest(cx, con, data);
            con.connect();
            if (bytes != null) {
                try (
                    OutputStream os = con.getOutputStream();
                ) {
                    os.write(bytes);
                }
            }
        } catch (IOException e) {
            String msg = "http connection failed: " + e.getMessage();
            LOG.log(Level.INFO, msg, e);
            throw new ProcedureException(msg);
        }
    }

    /**
     * Receives the HTTP response and processes its text content.
     *
     * @param cx             the procedure call context
     * @param con            the HTTP connection
     * @param metadata       the response wrapper flag
     * @param jsonData       the JSON response data flag
     * @param jsonError      the JSON response error flag
     *
     * @return the HTTP response data
     *
     * @throws ProcedureException if the response couldn't be read or
     *             contained an HTTP error code
     */
    protected static Object receive(CallContext cx,
                                    HttpURLConnection con,
                                    boolean metadata,
                                    boolean jsonData,
                                    boolean jsonError)
    throws ProcedureException {

        try {
            int httpCode = con.getResponseCode();
            boolean success = (httpCode / 100 == 2);
            String text = responseText(con);
            logResponse(cx, con, text);
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
                con.getHeaderFields().forEach((k, v) -> {
                    if (k != null && !k.isBlank()) {
                        headers.add(k, (v.size() == 1) ? v.get(0) : v);
                    }
                });
                return new Dict()
                    .set("success", success)
                    .set("response", con.getHeaderField(0))
                    .set("responseCode", httpCode)
                    .set("responseMessage", con.getResponseMessage())
                    .set("headers", headers)
                    .set("data", success ? data : null)
                    .set("error", success ? null : data);
            } else if (success || jsonError) {
                return data;
            } else {
                String msg = con.getHeaderField(0);
                if (!text.isBlank()) {
                    msg += ": " + text;
                }
                throw new ProcedureException(msg);
            }
        } catch (IOException e) {
            String msg = "error on " + logHttpId(con) + ": " + e.getMessage();
            LOG.log(Level.INFO, msg, e);
            throw new ProcedureException(msg);
        }
    }

    /**
     * Returns the HTTP response character set from the content type
     * header. Defaults to UTF-8 if no proper character set was
     * specified.
     *
     * @param con            the HTTP connection
     *
     * @return the HTTP response character set, or
     *         UTF-8 if not specified
     */
    private static String responseCharset(HttpURLConnection con) {
        String contentType = Objects.requireNonNullElse(con.getContentType(), "");
        for (String param : contentType.replace(" ", "").split(";")) {
            if (param.startsWith("charset=")) {
                return param.split("=", 2)[1];
            }
        }
        return "UTF-8";
    }

    /**
     * Returns the HTTP text response data. The data is expected to
     * be in text format and the encoding is read from the content
     * type header.
     *
     * @param con            the HTTP connection
     *
     * @return the HTTP text response
     *
     * @throws IOException if the response couldn't be read properly
     */
    protected static String responseText(HttpURLConnection con) throws IOException {
        boolean success = (con.getResponseCode() / 100 == 2);
        try (InputStream is = success ? con.getInputStream() : con.getErrorStream();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            if (is != null) {
                FileUtil.copy(is, os);
            }
            return os.toString(responseCharset(con));
        }
    }

    /**
     * Returns an HTTP connection debug identifier with relevant
     * parameters from the request.
     *
     * @param con            the HTTP connection
     *
     * @return an HTTP connection debug identifier
     */
    private static String logHttpId(HttpURLConnection con) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("HTTP ");
        buffer.append(con.getRequestMethod());
        buffer.append(" ");
        buffer.append(con.getURL());
        return buffer.toString();
    }

    /**
     * Logs the HTTP request if trace or debug logging is enabled.
     *
     * @param cx             the procedure call context, or null
     * @param con            the HTTP connection
     * @param data           the HTTP request data, or null
     */
    protected static void logRequest(CallContext cx,
                                     HttpURLConnection con,
                                     String data) {

        if (LOG.isLoggable(Level.FINE) || (cx != null && cx.isTracing())) {
            StringBuilder buffer = new StringBuilder();
            buffer.append("[");
            buffer.append(Integer.toHexString(con.hashCode()));
            buffer.append("] HTTP ");
            buffer.append(con.getRequestMethod());
            buffer.append(" ");
            buffer.append(con.getURL());
            buffer.append("\n");
            TreeSet<String> keys = new TreeSet<>(con.getRequestProperties().keySet());
            for (String key : keys) {
                buffer.append(key);
                buffer.append(": ");
                buffer.append(con.getRequestProperty(key));
                buffer.append("\n");
            }
            if (data != null && data.length() > 0) {
                buffer.append("\n");
                buffer.append(data);
                buffer.append("\n");
            }
            LOG.fine(buffer.toString());
            if (cx != null && cx.isTracing()) {
                cx.log(buffer.toString());
            }
        }
    }

    /**
     * Logs the HTTP response if trace or debug logging is enabled.
     * Also logs an INFO message on non-200 response codes.
     *
     * @param cx             the procedure call context, or null
     * @param con            the HTTP connection
     * @param data           the HTTP response data
     */
    protected static void logResponse(CallContext cx,
                                      HttpURLConnection con,
                                      String data) {

        if (LOG.isLoggable(Level.FINE) || (cx != null && cx.isTracing())) {
            StringBuilder buffer = new StringBuilder();
            buffer.append("[");
            buffer.append(Integer.toHexString(con.hashCode()));
            buffer.append("] ");
            buffer.append(con.getHeaderField(0));
            buffer.append("\n");
            for (int i = 1; true; i++) {
                String key = con.getHeaderFieldKey(i);
                String val = con.getHeaderField(i);
                if (key == null || val == null) {
                    break;
                }
                buffer.append(key);
                buffer.append(": ");
                buffer.append(con.getHeaderField(i));
                buffer.append("\n");
            }
            if (data != null && data.length() > 0) {
                buffer.append("\n");
                buffer.append(data);
                buffer.append("\n");
            }
            LOG.fine(buffer.toString());
            if (cx != null && cx.isTracing()) {
                cx.log(buffer.toString());
            }
        }
        try {
            if (con.getResponseCode() / 100 != 2) {
                String msg = "error on " + logHttpId(con) + ": " +
                             con.getHeaderField(0);
                if (data != null && data.length() > 0) {
                    LOG.info(msg + "\n" + data);
                } else {
                    LOG.info(msg);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.INFO, "error on " + logHttpId(con), e);
        }
    }
}
