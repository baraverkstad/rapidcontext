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

package org.rapidcontext.app.model;

import java.util.Date;
import java.util.Objects;
import java.util.logging.Logger;

import org.rapidcontext.core.ctx.Context;
import org.rapidcontext.core.ctx.ThreadContext;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.type.Session;
import org.rapidcontext.core.type.User;
import org.rapidcontext.core.web.Request;

/**
 * The request execution context, i.e. the top-level thread-level context.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class RequestContext extends ThreadContext {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(RequestContext.class.getName());

    /**
     * Returns the currently active request context. If no request context
     * is available, null is returned.
     *
     * @return the currently active request context, or null
     */
    public static RequestContext active() {
        return Context.active(RequestContext.class);
    }

    /**
     * Authenticates a verified user id and creates a new execution context.
     * This method will verify that the user id exists and is enabled. But it
     * also assumes that the user authentication can be trusted (via local
     * login or similar).
     *
     * @param userId         the request user id
     *
     * @return a new execution context
     *
     * @throws SecurityException if the user failed authentication
     */
    public static RequestContext initLocal(String userId) throws SecurityException {
        User user = SecurityContext.auth(userId);
        RequestContext cx = new RequestContext("local [" + user + "]");
        cx.set(CX_CREATED, new Date());
        cx.set(CX_USER, user);
        cx.open();
        return cx;
    }

    /**
     * Creates a new request execution context for a web request. If a valid
     * session is found, its user will be authenticated and added to the context.
     *
     * @param request        the request being processed
     *
     * @return a new execution context
     */
    public static RequestContext initWeb(Request request) {
        String source = "web [" + request.getRemoteAddr() + "]";
        RequestContext cx = new RequestContext(source);
        cx.set(CX_CREATED, new Date());
        cx.set(CX_REQUEST, request);
        try {
            cx.authBySession();
        } catch (Exception e) {
            LOG.info(cx + " " + e.getMessage());
        }
        cx.open();
        return cx;
    }

    /**
     * Creates a new request execution context for an asynchronous request.
     *
     * @param session        the request session (or null)
     * @param user           the request user (or null)
     *
     * @return a new execution context
     */
    public static RequestContext initAsync(Session session, User user) {
        String source = "async [" + Objects.toString(user, "anonymous") + "]";
        RequestContext cx = new RequestContext(source);
        cx.set(CX_CREATED, new Date());
        cx.set(CX_SESSION, session);
        try {
            if (user != null) {
                cx.auth(user.id());
            }
        } catch (Exception e) {
            LOG.info(cx + " " + e.getMessage());
        }
        cx.open();
        return cx;
    }

    /**
     * Creates a new request execution context.
     *
     * @param id             the context identifier (name)
     */
    protected RequestContext(String id) {
        super(id);
    }

    /**
     * Closes this context if and only if it is active for the thread.
     * Normally this method is not called directly, but implicitly for
     * contexts implementing AutoClosable.
     *
     * If this method is not called from the same thread that created the
     * context, no changes will be made.
     *
     * The parent context will be set as the new active context. All object
     * references will be cleared in preparation for garbage collection.
     *
     * @see AutoCloseable
     */
    @Override
    @SuppressWarnings("removal")
    public void close() {
        if (this == Context.active()) {
            // FIXME: This override is not needed when SecurityContext is removed
            Session.activeSession.remove();
            SecurityContext.deauth();
            super.close();
        }
    }

    /**
     * Authenticates the specified user. This method will verify that
     * the user exists and is enabled. It should only be called if a
     * previous user authentication can be trusted, either via a
     * cookie, command-line login or similar. After a successful
     * authentication the context user will be set.
     *
     * @param id             the unique user id
     *
     * @return the authenticated user
     *
     * @throws SecurityException if the user failed authentication
     */
    public User auth(String id) throws SecurityException {
        User user = SecurityContext.auth(id);
        set(CX_USER, user);
        return user;
    }

    /**
     * Authenticates a user via the current request session. If no
     * session is found (or if it is anonymous), null is returned.
     * If the session is expired, user is invalid, etc. an exception
     * is thrown.
     *
     * @return the authenticated user, or null
     *
     * @throws SecurityException if the session was expired or the
     *     user failed authentication
     */
    @SuppressWarnings("removal")
    protected User authBySession() throws SecurityException {
        Session.activeSession.remove();
        SecurityContext.deauth();
        Request request = request();
        String sessionId = request().getSessionId();
        Session session = null;
        User user = null;
        if (sessionId != null && !sessionId.isBlank()) {
            LOG.fine(this + " processing session authentication info");
            session = Session.find(root.storage(), sessionId);
            if (session != null) {
                session.updateAccessTime();
                session.setIp(request.getRemoteAddr());
                session.setClient(request.getHeader("User-Agent"));
                user = session.authenticate();
                set(CX_SESSION, session);
                set(CX_USER, user);
                Session.activeSession.set(session);
                return user;
            }
        }
        return null;
    }
}
