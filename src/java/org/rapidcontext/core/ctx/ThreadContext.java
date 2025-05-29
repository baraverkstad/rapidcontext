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

package org.rapidcontext.core.ctx;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.core.type.Session;
import org.rapidcontext.core.type.User;
import org.rapidcontext.core.web.Request;
import org.rapidcontext.util.DateUtil;

/**
 * The top-level execution context.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ThreadContext extends Context implements AutoCloseable {

    /**
     * The maximum number of characters to store in the log buffer.
     */
    public static final int MAX_LOG_LENGTH = 500000;

    /**
     * The log context attribute.
     */
    public static String CX_LOG = "log";

    /**
     * The request context attribute.
     */
    public static String CX_REQUEST = "request";

    /**
     * The session context attribute.
     */
    public static String CX_SESSION = "session";

    /**
     * The user context attribute.
     */
    public static String CX_USER = "user";

    /**
     * Creates a new top-level execution context for a web request.
     *
     * @param request        the request being processed
     * @param session        the request session (or null)
     * @param user           the request user (or null)
     *
     * @return a new execution context
     */
    public static ThreadContext enter(Request request, Session session, User user) {
        String source = "web [" + request.getRemoteAddr() + "]";
        ThreadContext cx = new ThreadContext(source);
        cx.set(CX_REQUEST, request);
        cx.set(CX_SESSION, session);
        cx.set(CX_USER, user);
        return cx;
    }

    /**
     * Creates a new top-level execution context for a command-line call.
     *
     * @param user           the request user (or null)
     *
     * @return a new execution context
     */
    public static ThreadContext enter(User user) {
        String source = "command-line [" + user + "]";
        ThreadContext cx = new ThreadContext(source);
        cx.set(CX_USER, user);
        return cx;
    }

    /**
     * Creates a new web request execution context.
     *
     * @param id             the context identifier (name)
     */
    protected ThreadContext(String id) {
        super(id);
    }

    /**
     * Checks if trace logging is enabled.
     *
     * @return true if trace logging is enabled, or
     *         false otherwise
     */
    public boolean isLogging() {
        return has(CX_LOG);
    }

    // FIXME: init logging?
    
    /**
     * Returns the current trace log.
     *
     * @return the current trace log text, or
     *         null if not enabled
     */
    public String log() {
        return get(CX_LOG, String.class);
    }

    protected void log(int indent, String message) {
        StringBuilder buf = getOrSet(CX_LOG, StringBuilder.class, () -> new StringBuilder());
        if (!message.isEmpty()) {
            String prefix = DateUtil.formatIsoTime(new Date()) + ": ";
            buf.append(prefix);
            buf.append(StringUtils.repeat(" ", indent));
            if (message.contains("\n")) {
                boolean first = true;
                for (String line : message.split("\n")) {
                    boolean isEmpty = message.isBlank();
                    boolean isNext = buffer.length() > 0;
                    buffer.append(isNext ? "\n" : "");
                    buffer.append(isNext && !isEmpty ? indentStr : "");
                    buffer.append(!isEmpty ? line : "");
                }

            } else {
                buf.append(message);
            }
            buf.append(logIndent(prefix.length() + indent, message));
            if (!message.endsWith("\n")) {
                buf.append("\n");
            }
            if (buf.length() > MAX_LOG_LENGTH) {
                buf.delete(0, buf.length() - MAX_LOG_LENGTH);
            }
        }
    }

    /**
     * Returns the context request.
     *
     * @return the context request
     */
    public Request request() {
        return get(CX_REQUEST, Request.class);
    }

    /**
     * Returns the context session if set.
     *
     * @return the context session, or
     *         null if not set
     */
    public Session session() {
        return get(CX_SESSION, Session.class);
    }

    /**
     * Returns the context user if set.
     *
     * @return the context user, or
     *         null if not set (anonymous)
     */
    public User user() {
        return get(CX_USER, User.class);
    }    
}
