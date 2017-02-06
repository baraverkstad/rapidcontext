/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2017 Per Cederberg. All rights reserved.
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

import org.apache.commons.lang.StringUtils;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.js.JsSerializer;
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
 * @author   Per Cederberg
 * @version  1.0
 */
public class LogWebService extends WebService{

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
    protected String[] methodsImpl(Request request) {
        return METHODS_GET_POST;
    }

    /**
     * Processes an HTTP GET request.
     *
     * @param request        the request to process
     */
    protected void doGet(Request request) {
        doLog(request);
    }

    /**
     * Processes an HTTP POST request.
     *
     * @param request        the request to process
     */
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
                Object obj = JsSerializer.unserialize(input);
                if (obj instanceof Dict) {
                    logMessage(request, (Dict) obj);
                } else if (obj instanceof Array) {
                    logMessages(request, (Array) obj);
                }
            } else {
                String level = request.getParameter("level", "log");
                String msg = request.getParameter("message", "");
                String data = request.getParameter("data");
                logMessage(request, logLevel(level), msg, data);
            }
            request.sendText(Mime.TEXT[0], "OK");
        } catch (Exception e) {
            request.sendError(STATUS.BAD_REQUEST, Mime.TEXT[0], "ERROR: " + e.toString());
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
        switch (level.toLowerCase()) {
        case "err":
        case "error":
            return Level.SEVERE;
        case "warn":
        case "warning":
            return Level.WARNING;
        case "info":
            return Level.INFO;
        default:
            return Level.FINE;
        }
    }

    /**
     * Logs a sequence of messages from the specified request.
     *
     * @param request        the request source
     * @param arr            the array containing message dicts
     */
    private static void logMessages(Request request, Array arr) {
        for (int i = 0; i < arr.size(); i++) {
            Object obj = arr.get(i);
            if (obj instanceof Dict) {
                logMessage(request, (Dict) obj);
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
        String level = dict.getString("level", "log");
        String msg = dict.getString("message", "");
        Object obj = dict.get("data");
        String data = null;
        if (obj instanceof Array) {
            data = ((Array) obj).toString();
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
            buffer.append("\n# userAgent = ");
            buffer.append(request.getHeader("User-Agent"));
            LOG.log(level, buffer.toString());
        }
    }
}
