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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.JsonSerializer;
import org.rapidcontext.core.type.Session;
import org.rapidcontext.core.type.WebService;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.core.web.Request;

/**
 * A logging API web service. This service is used for server-side logging of
 * client-side events. The request parameters "level", "message" and "data"
 * are used to create the log entry. One or more log events can also be sent
 * as JSON (using "appliction/json" MIME type for HTTP POST). Either as single
 * object or an array or objects are supported. The JSON objects use the same
 * "level", "message" and "data" properties.
 *
 * @author Per Cederberg
 */
public class LogWebService extends WebService {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(LogWebService.class.getName());

    /**
     * Creates a new file web service from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public LogWebService(String id, String type, Dict dict) {
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
        return METHODS_GET_POST;
    }

    /**
     * Processes an HTTP GET request.
     *
     * @param request        the request to process
     */
    @Override
    protected void doGet(Request request) {
        doLog(request);
    }

    /**
     * Processes an HTTP POST request.
     *
     * @param request        the request to process
     */
    @Override
    protected void doPost(Request request) {
        doLog(request);
    }

    /**
     * Processes a logging request.
     *
     * @param request        the request to process
     */
    private void doLog(Request request) {
        try {
            if (Mime.isInputMatch(request, Mime.JSON)) {
                String input = request.getInputString();
                Object obj = JsonSerializer.unserialize(input);
                if (obj instanceof Dict d) {
                    logMessage(request, d);
                } else if (obj instanceof Array a) {
                    logMessages(request, a);
                }
            } else {
                String level = request.getParameter("level", "log");
                String msg = request.getParameter("message", "");
                String data = request.getParameter("data");
                logMessage(request, logLevel(level), msg, data);
            }
            request.sendText(Mime.TEXT[0], "OK");
        } catch (Exception e) {
            request.sendError(Status.BAD_REQUEST, Mime.TEXT[0], "ERROR: " + e.toString());
        }
    }

    /**
     * Converts a string log level to a java.util.logging level.
     *
     * @param level          the string level name
     *
     * @return the java.util.logging log level
     */
    private static Level logLevel(String level) {
        return switch (level.toLowerCase()) {
            case "err", "error" -> Level.SEVERE;
            case "warn", "warning" -> Level.WARNING;
            case "info" -> Level.INFO;
            default -> Level.FINE;
        };
    }

    /**
     * Logs a sequence of messages from the specified request.
     *
     * @param request        the request source
     * @param arr            the array containing message dicts
     */
    private static void logMessages(Request request, Array arr) {
        for (Object o : arr) {
            if (o instanceof Dict d) {
                logMessage(request, d);
            }
        }
    }

    /**
     * Logs a message from the specified request.
     *
     * @param request        the request source
     * @param dict           the dict containing message, level, etc
     */
    private static void logMessage(Request request, Dict dict) {
        String level = dict.get("level", String.class, "log");
        String msg = dict.get("message", String.class, "");
        Object obj = dict.get("data");
        String data = null;
        if (obj instanceof Array a) {
            data = a.toString();
        } else if (obj != null) {
            data = obj.toString();
        }
        logMessage(request, logLevel(level), msg, data);

    }

    /**
     * Logs a message from the specified request.
     *
     * @param request        the request source
     * @param level          the log level
     * @param msg            the log message
     * @param data           the optional log data
     */
    private static void logMessage(Request request,
                                   Level level,
                                   String msg,
                                   String data) {

        String userAgent = request.getHeader("User-Agent");
        boolean unsupported = Helper.browserUnsupported(userAgent);
        if (unsupported && level.intValue() > Level.INFO.intValue()) {
            level = Level.INFO; // Lower max level for bots
        }
        if (LOG.isLoggable(level)) {
            StringBuilder buffer = new StringBuilder();
            buffer.append(request.getHost());
            buffer.append(" [");
            buffer.append(request.getRemoteAddr());
            buffer.append("]: ");
            buffer.append(msg);
            if (data != null) {
                buffer.append("\n");
                buffer.append(data);
            }
            Session s = Session.activeSession.get();
            if (s != null) {
                buffer.append("\n# session = ");
                buffer.append(s.id());
                if (StringUtils.isNotEmpty(s.userId())) {
                    buffer.append("\n# user = ");
                    buffer.append(s.userId());
                }
            }
            String browser = Helper.browserInfo(userAgent);
            if (browser != null) {
                buffer.append("\n# browser = ");
                buffer.append(browser);
            }
            buffer.append("\n# userAgent = ");
            buffer.append(userAgent);
            LOG.log(level, buffer.toString());
        }
    }
}
