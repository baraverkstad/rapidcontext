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

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.util.HttpUtil;

/**
 * A set HTTP request and response logging helpers.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public abstract class HttpLog {

    /**
     * The class logger.
     */
    private static final Logger LOG = Logger.getLogger(HttpLog.class.getName());

    /**
     * Logs the HTTP request if trace or debug logging is enabled.
     *
     * @param cx             the procedure call context, or null
     * @param req            the HTTP request
     * @param data           the HTTP request data, or null
     */
    protected static void logRequest(CallContext cx, HttpRequest req, String data) {
        if (LOG.isLoggable(Level.FINE) || (cx != null && cx.isTracing())) {
            StringBuilder log = new StringBuilder();
            logIdent(log, req);
            logURI(log, req);
            log.append("\n");
            logContent(log, req.headers(), data);
            LOG.fine(log.toString());
            if (cx != null && cx.isTracing()) {
                cx.log(log.toString());
            }
        }
    }

    /**
     * Logs the HTTP response if trace or debug logging is enabled.
     * Also logs an INFO message on non-200 response codes.
     *
     * @param cx             the procedure call context, or null
     * @param resp           the HTTP response
     */
    protected static void logResponse(CallContext cx, HttpResponse<String> resp) {
        if (LOG.isLoggable(Level.FINE) || (cx != null && cx.isTracing())) {
            if (resp.previousResponse().isPresent()) {
                logResponse(cx, resp.previousResponse().get());
            }
            StringBuilder log = new StringBuilder();
            logIdent(log, resp.request());
            log.append(switch (resp.version()) {
                case HttpClient.Version.HTTP_1_1 -> "HTTP/1.1";
                case HttpClient.Version.HTTP_2 -> "HTTP/2";
            });
            log.append(" ");
            log.append(resp.statusCode());
            log.append("\n");
            logContent(log, resp.headers(), resp.body());
            LOG.fine(log.toString());
            if (cx != null && cx.isTracing()) {
                cx.log(log.toString());
            }
        }
        if (resp.statusCode() / 100 > 3) {
            StringBuilder msg = new StringBuilder();
            msg.append("error on ");
            logURI(msg, resp.request());
            msg.append(": HTTP ");
            msg.append(resp.statusCode());
            if (resp.body() instanceof String s && !s.isBlank()) {
                msg.append("\n");
                msg.append(s);
            }
            LOG.info(msg.toString());
        }
    }

    /**
     * Appends an HTTP request identifier to a string buffer.
     *
     * @param log            the log string buffer
     * @param req            the HTTP request
     */
    private static void logIdent(StringBuilder log, HttpRequest req) {
        log.append("[");
        log.append(Integer.toHexString(req.hashCode()));
        log.append("] ");
    }

    /**
     * Appends HTTP method and URI to a string buffer.
     *
     * @param log            the log string buffer
     * @param req            the HTTP request
     */
    private static void logURI(StringBuilder log, HttpRequest req) {
        log.append("HTTP ");
        log.append(req.method());
        log.append(" ");
        log.append(req.uri());
    }

    /**
     * Appends HTTP headers and content to a string buffer.
     *
     * @param log            the log string buffer
     * @param headers        the HTTP request/response headers
     * @param data           the HTTP request/response content data
     */
    private static void logContent(StringBuilder log, HttpHeaders headers, String data) {
        headers.map().forEach((key, values) -> {
            for (String val : values) {
                if (key != null && !key.isBlank() && !key.startsWith(":")) {
                    log.append(key);
                    log.append(": ");
                    if (HttpUtil.HEADER.hasCredentials(key) && val != null && !val.isBlank()) {
                        int idx = val.indexOf(' ');
                        val = ((idx > 0) ? val.substring(0, idx + 1) : "") + "***";
                    }
                    log.append(val);
                    log.append("\n");
                }
            }
        });
        if (data != null && data.length() > 0) {
            log.append("\n");
            log.append(data);
            log.append("\n");
        }
    }

    // No instances
    private HttpLog() {}
}
