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

package org.rapidcontext.app;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidcontext.app.model.AppStorage;
import org.rapidcontext.app.model.RequestContext;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorageException;
import org.rapidcontext.core.storage.ZipStorage;
import org.rapidcontext.core.type.Plugin;
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
 * @author Per Cederberg
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
     * Creates a new application servlet instance.
     */
    public ServletApplication() {}

    /**
     * Initializes this servlet.
     *
     * @throws ServletException if the initialization failed
     */
    @Override
    public void init() throws ServletException {
        super.init();
        Mime.context = getServletContext();
        File baseDir = new File(getServletContext().getRealPath("/"));
        ApplicationContext.init(baseDir, baseDir, true);
    }

    /**
     * Uninitializes this servlet.
     */
    @Override
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
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

        Request request = new Request(req, resp);
        RequestContext cx = RequestContext.initWeb(request);
        try {
            processAuthData(cx, request.getAuth());
            LOG.fine(cx + " finding best matching web service");
            WebMatcher bestMatcher = null;
            int bestScore = 0;
            for (WebMatcher matcher : cx.parent(ApplicationContext.class).getWebMatchers()) {
                int score = matcher.match(request);
                LOG.fine(cx + " " + matcher + ", score " + score);
                if (score > bestScore) {
                    bestScore = score;
                    bestMatcher = matcher;
                }
            }
            if (bestMatcher != null) {
                if (!request.hasResponse()) {
                    LOG.fine(cx + " processing with " + bestMatcher.parent());
                    bestMatcher.process(request);
                } else {
                    LOG.fine(cx + " processed during web service matching");
                }
            }
            Session session = cx.session();
            if (session != null) {
                session.updateAccessTime();
                if (session.isNew()) {
                    try {
                        // Add session to storage cache, persists on eviction
                        Path path = Path.resolve(Plugin.cachePath("local"), session.path());
                        cx.storage().store(path, session);
                    } catch (StorageException e) {
                        LOG.log(Level.WARNING, "failed to store session " + session.id(), e);
                    }
                }
            }
            String cookieSession = request.getSessionId();
            // FIXME: Add support for configurable cookie name, domain, path, expiry, etc.
            // String cookiePath = (bestMatcher == null) ? null : bestMatcher.path();
            String cookiePath = null;
            if (session != null && !session.id().equals(cookieSession)) {
                request.setSessionId(session.id(), cookiePath);
            } else if (session == null && cookieSession != null) {
                request.setSessionId(null, cookiePath);
            }
            LOG.fine(cx + " sending response data");
            if (!request.hasResponse()) {
                request.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            request.commit();
        } catch (Exception e) {
            LOG.log(Level.INFO, cx + " error processing " + request, e);
        } finally {
            LOG.fine(cx + " processed " + request.getUrl() + " in " + request.getProcessTime() + " ms");
            cx.close();
            request.dispose();
        }
    }

    /**
     * Processes authentication header data for an unauthenticated request.
     * If valid auth data is provided, the request context will be updated
     * with the proper user.
     *
     * @param cx             the request context
     * @param auth           the authentication header data
     */
    private void processAuthData(RequestContext cx, Dict auth) {
        if (cx.user() == null && auth != null) {
            String scheme = auth.get("scheme", String.class, "");
            LOG.fine(cx + " processing '" + scheme + "' authentication");
            try {
                User user;
                if (scheme.equalsIgnoreCase("Digest")) {
                    user = processAuthDigest(cx, auth);
                } else if (scheme.equalsIgnoreCase("Token")) {
                    user = cx.authByToken(auth.get("data", String.class, ""));
                } else {
                    throw new SecurityException("Unsupported authentication scheme: " + scheme);
                }
                LOG.fine(cx + " valid '" + scheme + "' auth for " + user);
            } catch (SecurityException e) {
                LOG.info(cx + " " + e.getMessage());
            }
        }
    }

    private User processAuthDigest(RequestContext cx, Dict auth) throws SecurityException {
        if (!User.DEFAULT_REALM.equals(auth.get("realm"))) {
            String msg = "Unsupported authentication realm: " + auth.get("realm");
            throw new SecurityException(msg);
        } else if (!"MD5".equalsIgnoreCase(auth.get("algorithm", String.class, "MD5"))) {
            String msg = "Unsupported authentication algorithm: " + auth.get("algorithm");
            throw new SecurityException(msg);
        }
        String user = auth.get("username", String.class, "");
        String uri = auth.get("uri", String.class, cx.request().getAbsolutePath());
        String nonce = auth.get("nonce", String.class, "");
        String nc = auth.get("nc", String.class, "");
        String cnonce = auth.get("cnonce", String.class, "");
        String response = auth.get("response", String.class, "");
        SecurityContext.verifyNonce(nonce);
        try {
            String suffix = ":" + nonce + ":" + nc + ":" + cnonce + ":auth:" +
                BinaryUtil.hashMD5(cx.request().getMethod() + ":" + uri);
            return cx.authByMd5Hash(user, suffix, response);
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException("failed to process MD5 hash: " + e);
        }
    }
}
