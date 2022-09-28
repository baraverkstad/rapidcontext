/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2022 Per Cederberg. All rights reserved.
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

package org.rapidcontext.app;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.RootStorage;
import org.rapidcontext.core.storage.StorageException;
import org.rapidcontext.core.storage.ZipStorage;
import org.rapidcontext.core.type.Session;
import org.rapidcontext.core.type.User;
import org.rapidcontext.core.type.WebMatcher;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.core.web.Request;
import org.rapidcontext.util.BinaryUtil;

/**
 * The main application servlet. This servlet handles all incoming
 * web requests.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ServletApplication extends HttpServlet {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(ServletApplication.class.getName());

    /**
     * The documentation storage path.
     */
    public static final Path DOC_PATH = Path.from("/files/doc/");

    /**
     * The context to use for process execution.
     */
    private ApplicationContext ctx = null;

    /**
     * Creates a new application servlet instance.
     */
    public ServletApplication() {}

    /**
     * Initializes this servlet.
     *
     * @throws ServletException if the initialization failed
     */
    public void init() throws ServletException {
        super.init();
        Mime.context = getServletContext();
        File baseDir = new File(getServletContext().getRealPath("/"));
        ctx = ApplicationContext.init(baseDir, baseDir, true);
        // TODO: move the doc directory into the system plug-in storage
        try {
            File docZip = new File(baseDir, "doc.zip");
            ZipStorage docStore = new ZipStorage(docZip);
            RootStorage root = (RootStorage) ctx.getStorage();
            Path storagePath = RootStorage.PATH_STORAGE.child("doc", true);
            root.mount(docStore, storagePath, false, DOC_PATH, 0);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "failed to mount doc storage", e);
        }
    }

    /**
     * Uninitializes this servlet.
     */
    public void destroy() {
        ApplicationContext.destroy();
        super.destroy();
    }

    /**
     * Processes a servlet request.
     *
     * @param req            the servlet request
     * @param resp           the servlet response
     *
     * @throws ServletException if an internal error occurred when processing
     *             the request
     * @throws IOException if an IO error occurred when processing the request
     */
    protected void service(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

        Request       request = new Request(req, resp);
        WebMatcher    bestMatcher = null;
        int           bestScore = 0;

        try {
            LOG.fine(ip(request) + "Processing authentication info");
            processAuthCheck(request);
            LOG.fine(ip(request) + "Finding best matching web service");
            for (WebMatcher matcher : ctx.getWebMatchers()) {
                int score = matcher.match(request);
                if (score > bestScore) {
                    bestScore = score;
                    bestMatcher = matcher;
                }
            }
            if (bestMatcher != null) {
                if (!request.hasResponse()) {
                    LOG.fine(ip(request) + "Processing with " + bestMatcher.parent());
                    bestMatcher.process(request);
                } else {
                    LOG.fine(ip(request) + "Processed during web service matching");
                }
            }
            Session session = Session.activeSession.get();
            if (session != null && session.isValid()) {
                request.setSessionId(session.id());
            } else if (request.getSessionId() != null) {
                request.setSessionId(null);
            }
            LOG.fine(ip(request) + "Sending response data");
            if (!request.hasResponse()) {
                request.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            request.commit();
        } catch (IOException e) {
            LOG.log(Level.INFO, "IO error when processing request: " + request, e);
        }
        LOG.fine(ip(request) + "Request to " + request.getUrl() +
                 " processed in " + request.getProcessTime() +
                 " millisecs");
        processAuthReset();
        request.dispose();
    }

    /**
     * Clears any previous user authentication. This will remove the
     * security context and session info from this thread. It may
     * also delete the session if it has been invalidated, or store
     * it if newly created.
     */
    private void processAuthReset() {
        Session session = Session.activeSession.get();
        Session.activeSession.set(null);
        if (session != null && !session.isValid()) {
            LOG.fine("session " + session.id() + " invalid (expired on " +
                     session.destroyTime() + "), removing from storage");
            Session.remove(ctx.getStorage(), session.id());
        } else if (session != null && session.isNew()) {
            try {
                Session.store(ctx.getStorage(), session);
            } catch (StorageException e) {
                LOG.log(Level.WARNING, "failed to store session " + session.id(), e);
            }
        }
        SecurityContext.authClear();
    }

    /**
     * Re-establishes user authentication for a servlet request. This
     * will clear any previous user authentication and check for
     * a valid session or authentication response. No request
     * response will be generated from this method.
     *
     * @param request        the request to process
     */
    private void processAuthCheck(Request request) {

        // Clear previous authentication
        processAuthReset();

        // Check for valid session
        String sessionId = StringUtils.defaultString(request.getSessionId());
        Session session = null;
        try {
            if (sessionId.length() > 0) {
                session = Session.find(ctx.getStorage(), sessionId);
            }
            if (session != null) {
                session.validate();
                Session.activeSession.set(session);
                session.updateAccessTime();
                session.setIp(request.getRemoteAddr());
                session.setClient(request.getHeader("User-Agent"));
                if (!StringUtils.isEmpty(session.userId())) {
                    SecurityContext.auth(session.userId());
                }
            }
        } catch (Exception e) {
            LOG.info(ip(request) + e.getMessage());
            if (session != null) {
                LOG.info("request session " + session.id() +
                         " invalid, removing from storage");
                Session.remove(ctx.getStorage(), session.id());
            }
        }

        // Check for authentication response
        try {
            if (SecurityContext.currentUser() == null) {
                Dict authData = request.getAuthentication();
                if (authData != null) {
                    processAuthResponse(request, authData);
                }
            }
        } catch (Exception e) {
            LOG.info(ip(request) + e.getMessage());
        }
    }

    /**
     * Processes a digest authentication response.
     *
     * @param request        the request to process
     * @param auth           the authentication data
     *
     * @throws Exception if the user authentication failed
     */
    private void processAuthResponse(Request request, Dict auth)
    throws Exception {
        String  uri = auth.getString("uri", request.getAbsolutePath());
        String  user = auth.getString("username", "");
        String  realm = auth.getString("realm", "");
        String  nonce = auth.getString("nonce", "");
        String  nc = auth.getString("nc", "");
        String  cnonce = auth.getString("cnonce", "");
        String  response = auth.getString("response", "");
        String  suffix;

        // Verify authentication response
        if (!User.DEFAULT_REALM.equals(realm)) {
            LOG.info(ip(request) + "Invalid authentication realm: " + realm);
            throw new SecurityException("Invalid authentication realm");
        }
        SecurityContext.verifyNonce(nonce);
        suffix = ":" + nonce + ":" + nc + ":" + cnonce + ":auth:" +
                 BinaryUtil.hashMD5(request.getMethod() + ":" + uri);
        SecurityContext.authHash(user, suffix, response);
        LOG.fine(ip(request) + "Valid authentication for " + user);
    }

    /**
     * Returns an IP address tag suitable for logging.
     *
     * @param request        the request to use
     *
     * @return the IP address tag for logging
     */
    private String ip(Request request) {
        return "[" + request.getRemoteAddr() + "] ";
    }
}
