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

package org.rapidcontext.app.model;

import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.rapidcontext.core.ctx.Context;
import org.rapidcontext.core.ctx.ThreadContext;
import org.rapidcontext.core.type.Session;
import org.rapidcontext.core.type.User;
import org.rapidcontext.core.web.Request;
import org.rapidcontext.util.BinaryUtil;

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
        RequestContext cx = new RequestContext("local [" + userId + "]");
        cx.set(CX_CREATED, new Date());
        if (userId != null && !userId.isBlank()) {
            cx.auth(userId);
        } else {
            cx.set(CX_USER, null);
        }
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
        User user = User.find(storage(), id);
        if (user == null) {
            String msg = "user " + id + " does not exist";
            LOG.info("failed authentication: " + msg);
            throw new SecurityException(msg);
        } else if (!user.isEnabled()) {
            String msg = "user " + id + " is disabled";
            LOG.info("failed authentication: " + msg);
            throw new SecurityException(msg);
        }
        set(CX_USER, user);
        Optional.ofNullable(session()).ifPresent(s -> s.setUserId(user.id()));
        return user;
    }

    /**
     * Authenticates a user via the current request session. If no
     * session is found (or if it is anonymous), null is returned.
     * If the session is expired, the user no longer exists, or the
     * user has changed their password since the session last accessed,
     * an exception is thrown.
     *
     * @return the authenticated user, or null
     *
     * @throws SecurityException if the session was expired, the user
     *     failed authentication, or the user authorization time
     *     mismatches the session
     */
    protected User authBySession() throws SecurityException {
        Request request = request();
        String sessionId = request().getSessionId();
        if (sessionId != null && !sessionId.isBlank()) {
            LOG.fine(this + " processing session authentication info");
            Session session = Session.find(storage(), sessionId);
            if (session != null && session.isExpired()) {
                throw new SecurityException("session has expired");
            } else if (session != null) {
                User user = null;
                if (session.isAuthenticated()) {
                    user = auth(session.userId());
                    session.validateAuth(user.authorizedTime());
                }
                session.updateAccessTime();
                session.setIp(request.getRemoteAddr());
                session.setClient(request.getHeader("User-Agent"));
                set(CX_SESSION, session);
                set(CX_USER, user);
                return user;
            }
        }
        return null;
    }

    /**
     * Authenticates a user via a two-step MD5 hash. If the user is
     * disabled or the hashes don't match an exception is thrown.
     *
     * @param id             the unique user id
     * @param suffix         the user password hash suffix to append
     * @param hash           the expected hashed result
     *
     * @return the authenticated user
     *
     * @throws SecurityException if the authentication failed
     */
    public User authByMd5Hash(String id, String suffix, String hash)
    throws SecurityException {
        User user = User.find(storage(), id);
        if (user == null) {
            String msg = "user " + id + " not found";
            LOG.info("failed authentication: " + msg);
            throw new SecurityException(msg);
        } else if (!user.isEnabled()) {
            String msg = user + " is disabled";
            LOG.info("failed authentication: " + msg);
            throw new SecurityException(msg);
        }
        try {
            String test = BinaryUtil.hashMD5(user.passwordHash() + suffix);
            if (!user.passwordHash().isBlank() && !test.equals(hash)) {
                String msg = "invalid password for " + user;
                LOG.info("failed authentication: " + msg +
                         ", expected: " + test + ", received: " + hash);
                throw new SecurityException(msg);
            }
        } catch (NoSuchAlgorithmException e) {
            String msg = "invalid environment, MD5 not supported";
            LOG.log(Level.SEVERE, msg, e);
            throw new SecurityException(msg, e);
        }
        set(CX_USER, user);
        Optional.ofNullable(session()).ifPresent(s -> s.setUserId(user.id()));
        return user;
    }

    /**
     * Authenticates a user via an authentication token. If the token
     * is expired, invalid or linked to a disabled user an exception
     * is thrown. Note that tokens automatically invalidates when a
     * user password is changed.
     *
     * @param token          the authentication token
     *
     * @return the authenticated user
     *
     * @throws SecurityException if the token was invalid or user
     *     authentication failed
     */
    public User authByToken(String token) throws SecurityException {
        try {
            User user = AuthHelper.validateLoginToken(token);
            set(CX_USER, user);
            Optional.ofNullable(session()).ifPresent(s -> s.setUserId(user.id()));
            return user;
        } catch (Exception e) {
            throw new SecurityException("invalid token: " + e.getMessage());
        }
    }

    /**
     * Identifies the caller of a procedure. This method will validate the
     * procedure call token and set the context identifier to the app path.
     *
     * @param procedure      the procedure identifier
     * @param token          the procedure call token
     */
    public void identifyCaller(String procedure, String token) {
        if (token != null && !token.isBlank()) {
            try {
                id = AuthHelper.validateProcToken(token, procedure);
            } catch (Exception e) {
                LOG.info(session() + " provided invalid call token: " + e.getMessage());
            }
        }
    }
}
