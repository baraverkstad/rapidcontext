/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2026 Per Cederberg. All rights reserved.
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
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.core.data.JsonSerializer;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.type.Role;
import org.rapidcontext.core.type.Session;
import org.rapidcontext.core.type.User;
import org.rapidcontext.core.web.Request;
import org.rapidcontext.util.DateUtil;

/**
 * The base thread-level execution context. A new thread context is created
 * for each procedure, web request or similar call, forming a call chain (or
 * stack) of contexts. Each thread-level context is bound to a single execution
 * thread. And each thread has (at most) a single active context that holds
 * data related to a request, procedure call, etc.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public abstract class ThreadContext extends Context {

    /**
     * The maximum number of characters to store in the log buffer.
     */
    public static final int MAX_LOG_LENGTH = 500000;

    /**
     * The creation time context attribute.
     */
    public static final String CX_CREATED = "created";

    /**
     * The log context attribute.
     */
    public static final String CX_LOG = "log";

    /**
     * The request context attribute.
     */
    public static final String CX_REQUEST = "request";

    /**
     * The session context attribute.
     */
    public static final String CX_SESSION = "session";

    /**
     * The user context attribute.
     */
    public static final String CX_USER = "user";

    /**
     * Returns the currently active thread context. If no thread-local context
     * is available, null is returned.
     *
     * @return the currently active thread context, or null
     */
    public static ThreadContext active() {
        return Context.active(ThreadContext.class);
    }

    /**
     * Returns a log representation of an object.
     *
     * @param obj            the value to log
     * @param indent         the indentation enabled flag
     * @param maxLen         the maximum output length
     *
     * @return the log representation
     */
    protected static String logRepr(Object obj, boolean indent, int maxLen) {
        String str = JsonSerializer.serialize(obj, indent);
        if (str.length() > maxLen) {
            return str.substring(0, maxLen) + "...";
        } else {
            return str;
        }
    }

    /**
     * Creates a new thread-level execution context.
     *
     * @param id             the context identifier (name)
     */
    protected ThreadContext(String id) {
        super(id);
    }

    /**
     * Returns the context creation time. Normally only set on the
     * request context (or the top-most call context).
     *
     * @return the context creation time
     */
    public Date created() {
        return get(CX_CREATED, Date.class);
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
     * Returns the context session if set, or creates a new one.
     *
     * @return the context session (possibly a new one)
     */
    @SuppressWarnings("removal")
    public Session sessionRequired() {
        return Objects.requireNonNullElseGet(session(), () -> {
            String ip = request().getRemoteAddr();
            String client = request().getHeader("User-Agent");
            User user = user();
            String userId = (user == null) ? null : user.id();
            Session session = new Session(userId, ip, client);
            set(CX_SESSION, session);
            Session.activeSession.set(session);
            return session;
        });
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

    /**
     * Checks if trace logging is enabled.
     *
     * @return true if trace logging is enabled, or
     *         false otherwise
     */
    public boolean isLogging() {
        return has(CX_LOG);
    }

    /**
     * Checks if the context user has a specified permission for a
     * storage path.
     *
     * @param path           the object storage path
     * @param permission     the permission to check
     *
     * @return true if access is granted, or
     *         false otherwise
     *
     * @see Role#hasAccess(String, String)
     */
    public boolean hasAccess(String path, String permission) {
        return SecurityContext.hasAccess(user(), path, null, permission);
    }

    /**
     * Checks if the context user has a specified permission for a
     * storage path, ignoring any indirect matches.
     *
     * @param path           the object storage path
     * @param permission     the permission to check
     *
     * @return true if access is granted, or
     *         false otherwise
     *
     * @see Role#hasAccess(String, String, String)
     */
    public boolean hasDirectAccess(String path, String permission) {
        return SecurityContext.hasAccess(user(), path, "-", permission);
    }

    /**
     * Checks if the context user has search access for a storage path.
     *
     * @param path           the object storage path
     *
     * @return true if access is granted, or
     *         false otherwise
     */
    public boolean hasSearchAccess(String path) {
        return hasAccess(path, Role.PERM_SEARCH);
    }

    /**
     * Checks if the context user has read access for a storage path.
     *
     * @param path           the object storage path
     *
     * @return true if access is granted, or
     *         false otherwise
     */
    public boolean hasReadAccess(String path) {
        return hasAccess(path, Role.PERM_READ);
    }

    /**
     * Checks if the context user has write access for a storage path.
     *
     * @param path           the object storage path
     *
     * @return true if access is granted, or
     *         false otherwise
     */
    public boolean hasWriteAccess(String path) {
        return hasAccess(path, Role.PERM_WRITE);
    }

    /**
     * Verifies that the context user has a specified permission for a
     * storage path.
     *
     * @param path           the object storage path
     * @param permission     the required permission
     *
     * @throws SecurityException if access was denied
     */
    public void requireAccess(String path, String permission) throws SecurityException {
        if (!hasAccess(path, permission)) {
            String userId = Objects.toString(user(), "anonymous user");
            String msg = path + ": " +permission + " access denied for " + userId;
            throw new SecurityException(msg);
        }
    }

    /**
     * Verifies that the context user has search access for a storage path.
     *
     * @param path           the object storage path
     *
     * @throws SecurityException if access was denied
     */
    public void requireSearchAccess(String path) throws SecurityException {
        requireAccess(path, Role.PERM_SEARCH);
    }

    /**
     * Verifies that the context user has read access for a storage path.
     *
     * @param path           the object storage path
     *
     * @throws SecurityException if access was denied
     */
    public void requireReadAccess(String path) throws SecurityException {
        requireAccess(path, Role.PERM_READ);
    }

    /**
     * Verifies that the context user has write access for a storage path.
     *
     * @param path           the object storage path
     *
     * @throws SecurityException if access was denied
     */
    public void requireWriteAccess(String path) throws SecurityException {
        requireAccess(path, Role.PERM_WRITE);
    }

    /**
     * Returns the current trace log text.
     *
     * @return the current trace log text, or
     *         null if not enabled
     */
    public String log() {
        return get(CX_LOG, String.class);
    }

    /**
     * Logs a message to the trace log. This also enables trace logging
     * for this context (and sub-contexts). Call this method with a
     * null message to just enable trace logging.
     *
     * @param message        the message to log, or null
     */
    public void log(String message) {
        StringBuilder buf = getOrSet(CX_LOG, StringBuilder.class, () -> new StringBuilder());
        if (message != null && !message.isEmpty()) {
            int pos = buf.length();
            buf.append(DateUtil.formatIsoTime(new Date()));
            buf.append(": ");
            buf.append(StringUtils.repeat("  ", depthOf(ThreadContext.class) - 2));
            if (message.contains("\n")) {
                String indent = StringUtils.repeat(" ", buf.length() - pos + 4);
                boolean isFirst = true;
                for (String line : message.split("\n")) {
                    boolean isEmpty = line.isBlank();
                    buf.append(isFirst ? "" : "\n");
                    buf.append((isFirst || isEmpty) ? "" : indent);
                    buf.append(isEmpty ? "" : line);
                    isFirst = false;
                }
            } else {
                buf.append(message);
            }
            if (!message.endsWith("\n")) {
                buf.append("\n");
            }
            if (buf.length() > MAX_LOG_LENGTH) {
                buf.delete(0, buf.length() - MAX_LOG_LENGTH);
            }
        }
    }

    /**
     * Logs a message if trace logging is enabled.
     *
     * @param obj            the message text or value
     */
    public void logTrace(Object obj) {
        if (isLogging()) {
            if (obj instanceof String s) {
                log(s);
            } else {
                log(logRepr(obj, true, 1000));
            }
        }
    }

    /**
     * Logs a call request if trace logging is enabled.
     *
     * @param name           the request name or similar
     * @param args           the call arguments
     */
    public void logRequest(String name, Object[] args) {
        if (isLogging()) {
            StringBuilder buffer = new StringBuilder();
            buffer.append(name);
            if (args != null) {
                buffer.append("(");
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) {
                        buffer.append(", ");
                    }
                    buffer.append(logRepr(args[i], false, 250));
                }
                buffer.append(")");
            }
            log("--> " + buffer.toString());
        }
    }

    /**
     * Logs a call response if trace logging is enabled.
     *
     * @param obj            the response message or value
     */
    public void logResponse(Object obj) {
        if (isLogging()) {
            log("<-- " + logRepr(obj, true, 1000));
        }
    }

    /**
     * Logs an error if trace logging is enabled.
     *
     * @param obj            the error message or exception
     */
    public void logError(Object obj) {
        if (isLogging()) {
            if (obj instanceof String s) {
                log("<-- ERROR: " + s);
            } else if (obj instanceof Throwable t) {
                log("<-- ERROR: " + t.getMessage());
            } else {
                log("<-- ERROR: " + logRepr(obj, true, 1000));
            }
        }
    }
}
