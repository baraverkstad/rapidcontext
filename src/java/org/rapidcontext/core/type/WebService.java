/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2013 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE.txt file for more details.
 */

package org.rapidcontext.core.type;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.core.web.Request;
import org.rapidcontext.util.HttpUtil;

/**
 * An HTTP web service (request handler). This is a generic type, providing
 * basic support for matching HTTP requests to the actual handlers (file
 * handler, app handler, WebDAV, etc). A custom web service usually subclasses
 * this class directly.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public abstract class WebService extends StorableObject implements HttpUtil {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(WebService.class.getName());

    /**
     * The dictionary key for the description text.
     */
    public static final String KEY_DESCRIPTION = "description";

    /**
     * The dictionary key for the request match array.
     */
    public static final String KEY_MATCH = "match";

    /**
     * The web service object storage path.
     */
    public static final Path PATH = new Path("/webservice/");

    /**
     * The array of matcher objects.
     */
    protected ArrayList matchers;

    /**
     * Searches for all matchers in all web services in the storage.
     *
     * @param storage        the storage to search in
     *
     * @return an array of all matchers found
     */
    public static WebMatcher[] findAllMatchers(Storage storage) {
        Object[]   objs = storage.loadAll(PATH);
        ArrayList  list = new ArrayList();

        for (int i = 0; i < objs.length; i++) {
            if (objs[i] instanceof WebService) {
                list.addAll(((WebService) objs[i]).matchers);
            }
        }
        return (WebMatcher[]) list.toArray(new WebMatcher[list.size()]);
    }

    /**
     * Creates a new web service from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public WebService(String id, String type, Dict dict) {
        super(id, type, dict);
        dict.set(KEY_DESCRIPTION, description());
        Array arr = dict.getArray(KEY_MATCH);
        int size = (arr == null) ? 0 : arr.size();
        matchers = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            matchers.add(new WebMatcher(this, arr.getDict(i)));
        }
    }

    /**
     * Returns the description text.
     *
     * @return the description text.
     */
    public String description() {
        return dict.getString(KEY_DESCRIPTION, "");
    }

    /**
     * Returns the HTTP methods supported for the specified request.
     * The OPTIONS method is always supported and the HEAD method is
     * automatically added if GET is supported.
     *
     * @param request        the request to check
     *
     * @return the array of HTTP method names supported
     */
    public String[] methods(Request request) {
        LinkedHashSet set = new LinkedHashSet();
        set.add(METHOD.OPTIONS);
        for (int i = 0; i < matchers.size(); i++) {
            WebMatcher m = (WebMatcher) matchers.get(i);
            if (m.match(request) > 0) {
                String method = m.method();
                if (METHOD.GET.equals(method)) {
                    set.add(METHOD.HEAD);
                }
                set.add(method);
            }
        }
        return (String[]) set.toArray(new String[set.size()]);
    }

    /**
     * Returns the current session for the request. Or creates a new
     * one if none existed.
     *
     * @param request        the request to check
     * @param create         the session create flag
     *
     * @return the user session found or created, or
     *         null if not available
     */
    public Session session(Request request, boolean create) {
        Session session = (Session) Session.activeSession.get();
        if (create && session == null) {
            String ip = request.getRemoteAddr();
            String client = request.getHeader("User-Agent");
            User user = SecurityContext.currentUser();
            String userId = (user == null) ? null : user.id();
            session = new Session(userId, ip, client);
            Session.activeSession.set(session);
            long destroy = session.destroyTime().getTime();
            long now = System.currentTimeMillis();
            int expiry = (int) ((destroy - now) / 1000);
            request.setSessionId(session.id(), expiry);
        }
        return session;
    }

    /**
     * Processes a request for this handler. This method assumes
     * local request paths (removal of the mapped URL base).
     *
     * @param request        the request to process
     *
     * @see WebMatcher#process(Request)
     */
    public void process(Request request) {
        if (request.hasMethod(METHOD.GET)) {
            doGet(request);
        } else if (request.hasMethod(METHOD.POST)) {
            doPost(request);
        } else if (request.hasMethod(METHOD.PATCH)) {
            doPatch(request);
        } else if (request.hasMethod(METHOD.PUT)) {
            doPut(request);
        } else if (request.hasMethod(METHOD.DELETE)) {
            doDelete(request);
        } else if (request.hasMethod(METHOD.OPTIONS)) {
            doOptions(request);
        } else if (request.hasMethod(METHOD.HEAD)) {
            doHead(request);
        } else if (request.hasMethod(METHOD.TRACE)) {
            doTrace(request);
        } else {
            errorMethodNotAllowed(request);
        }
    }

    /**
     * Processes an HTTP OPTIONS request.
     *
     * @param request
     */
    protected void doOptions(Request request) {
        headerAllow(request);
        request.sendText(null, null);
    }

    /**
     * Processes an HTTP HEAD request. By default this method will
     * call doGet() and set the response headers only flag if a
     * result was generated. There is normally no need to subclass
     * this method.
     *
     * @param request        the request to process
     */
    protected void doHead(Request request) {
        doGet(request);
        if (request.hasResponse()) {
            request.setResponseHeadersOnly(true);
        }
    }

    /**
     * Processes an HTTP GET request. By default this method will
     * generate an HTTP 405 method not allowed error.
     *
     * @param request        the request to process
     */
    protected void doGet(Request request) {
        errorMethodNotAllowed(request);
    }

    /**
     * Processes an HTTP POST request. By default this method will
     * generate an HTTP 405 method not allowed error.
     *
     * @param request        the request to process
     */
    protected void doPost(Request request) {
        errorMethodNotAllowed(request);
    }

    /**
     * Processes an HTTP PATCH request. By default this method will
     * generate an HTTP 405 method not allowed error.
     *
     * @param request        the request to process
     */
    protected void doPatch(Request request) {
        errorMethodNotAllowed(request);
    }

    /**
     * Processes an HTTP PUT request. By default this method will
     * generate an HTTP 405 method not allowed error.
     *
     * @param request        the request to process
     */
    protected void doPut(Request request) {
        errorMethodNotAllowed(request);
    }

    /**
     * Processes an HTTP DELETE request. By default this method will
     * generate an HTTP 405 method not allowed error.
     *
     * @param request        the request to process
     */
    protected void doDelete(Request request) {
        errorMethodNotAllowed(request);
    }

    /**
     * Processes an HTTP TRACE request. By default this method will
     * generate an HTTP 405 method not allowed error.
     *
     * @param request        the request to process
     */
    protected void doTrace(Request request) {
        errorMethodNotAllowed(request);
    }

    /**
     * Adds the HTTP allow header to the response. The allowed
     * methods listed in the header are retrieved from the methods()
     * method.
     *
     * @param request        the request to modify
     */
    protected void headerAllow(Request request) {
        String str = StringUtils.join(methods(request), " ");
        request.setResponseHeader(HEADER.ALLOW, str);
    }

    /**
     * Sends an HTTP 400 bad request error.
     *
     * @param request        the request to process
     * @param message        the additional error message
     */
    protected void errorBadRequest(Request request, String message) {
        request.sendError(STATUS.BAD_REQUEST,
                          Mime.TEXT[0],
                          "HTTP 400 Bad Request: " + message);
    }

    /**
     * Sends an HTTP 401 unauthorized or 403 forbidden error. The
     * HTTP 401 authorization request will only be sent if the user
     * is not already logged in.
     *
     * @param request        the request to process
     */
    protected void errorUnauthorized(Request request) {
        if (SecurityContext.currentUser() == null) {
            request.sendAuthenticationRequest(User.DEFAULT_REALM,
                                              SecurityContext.nonce());
        } else {
            errorForbidden(request);
        }
    }

    /**
     * Sends an HTTP 403 forbidden error.
     *
     * @param request        the request to process
     */
    protected void errorForbidden(Request request) {
        LOG.info("[" + request.getRemoteAddr() + "] forbidden access to " +
                 request.getUrl());
        request.sendError(STATUS.FORBIDDEN);
    }

    /**
     * Sends an HTTP 404 not found error.
     *
     * @param request        the request to process
     */
    protected void errorNotFound(Request request) {
        request.sendError(STATUS.NOT_FOUND);
    }

    /**
     * Sends an HTTP 405 method not allowed error.
     *
     * @param request        the request to process
     */
    protected void errorMethodNotAllowed(Request request) {
        headerAllow(request);
        request.sendError(STATUS.METHOD_NOT_ALLOWED);
    }

    /**
     * Sends an HTTP 500 internal server error.
     *
     * @param request        the request to process
     * @param message        the additional error message
     */
    protected void errorInternal(Request request, String message) {
        request.sendError(STATUS.INTERNAL_SERVER_ERROR,
                          Mime.TEXT[0],
                          "HTTP 500 Internal Server Error: " + message);
    }
}
