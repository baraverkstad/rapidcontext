/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.web;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.rapidcontext.core.security.SecurityContext;

/**
 * The base request handler class. A request handler is mapped to a
 * set of URL:s, normally based on host and path prefix.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public abstract class RequestHandler {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(RequestHandler.class.getName());

    /**
     * The HTTP GET method constant.
     */
    public static final String METHOD_GET = "GET";

    /**
     * The HTTP POST method constant.
     */
    public static final String METHOD_POST = "POST";

    /**
     * The HTTP PUT method constant.
     */
    public static final String METHOD_PUT = "PUT";

    /**
     * The HTTP DELETE method constant.
     */
    public static final String METHOD_DELETE = "DELETE";

    /**
     * The HTTP OPTIONS method constant.
     */
    public static final String METHOD_OPTIONS = "OPTIONS";

    /**
     * The HTTP HEAD method constant.
     */
    public static final String METHOD_HEAD = "HEAD";

    /**
     * The HTTP TRACE method constant.
     */
    public static final String METHOD_TRACE = "TRACE";

    /**
     * The array of HTTP methods for a GET-only handler. This array
     * contains the OPTIONS, HEAD and GET methods.
     */
    protected static final String[] GET_METHODS_ONLY =
        new String[] { METHOD_OPTIONS, METHOD_HEAD, METHOD_GET };

    /**
     * The array of HTTP methods for a POST-only handler. This array
     * contains the OPTIONS, POST methods.
     */
    protected static final String[] POST_METHODS_ONLY =
        new String[] { METHOD_OPTIONS, METHOD_POST };

    /**
     * Returns the HTTP methods supported for the specified request
     * (path). This method assumes local request paths (removal of
     * the mapped URL base).
     *
     * @param request        the request to check
     *
     * @return the array of HTTP method names supported
     */
    public abstract String[] methods(Request request);

    /**
     * Processes a request for this handler. This method assumes
     * local request paths (removal of the mapped URL base).
     *
     * @param request the request to process
     */
    public void process(Request request) {
        if (request.hasMethod(METHOD_GET)) {
            doGet(request);
        } else if (request.hasMethod(METHOD_POST)) {
            doPost(request);
        } else if (request.hasMethod(METHOD_PUT)) {
            doPut(request);
        } else if (request.hasMethod(METHOD_DELETE)) {
            doDelete(request);
        } else if (request.hasMethod(METHOD_OPTIONS)) {
            doOptions(request);
        } else if (request.hasMethod(METHOD_HEAD)) {
            doHead(request);
        } else if (request.hasMethod(METHOD_TRACE)) {
            doTrace(request);
        } else {
            errorMethodNotAllowed(request);
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
     * Processes an HTTP OPTIONS request.
     *
     * @param request
     */
    protected void doOptions(Request request) {
        headerAllow(request);
        request.sendData(null, null);
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
        request.setResponseHeader("Allow", str);
    }

    /**
     * Sends an HTTP 400 bad request error.
     *
     * @param request        the request to process
     * @param message        the additional error message
     */
    protected void errorBadRequest(Request request, String message) {
        request.sendError(HttpServletResponse.SC_BAD_REQUEST,
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
            request.sendAuthenticationRequest(SecurityContext.REALM,
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
        request.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Sends an HTTP 404 not found error.
     *
     * @param request        the request to process
     */
    protected void errorNotFound(Request request) {
        request.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Sends an HTTP 405 method not allowed error.
     *
     * @param request        the request to process
     */
    protected void errorMethodNotAllowed(Request request) {
        headerAllow(request);
        request.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Sends an HTTP 500 internal server error.
     *
     * @param request        the request to process
     * @param message        the additional error message
     */
    protected void errorInternal(Request request, String message) {
        request.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                          Mime.TEXT[0],
                          "HTTP 500 Internal Server Error: " + message);
    }
}
