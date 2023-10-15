/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2023 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.type;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.web.Request;

/**
 * An HTTP web request matcher. The web matcher is connected to a web
 * service and detect if a request can be processed.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class WebMatcher {

    /**
     * The parent web service for this matcher.
     */
    protected WebService service;

    /**
     * The serialized matcher representation.
     */
    protected Dict dict;

    /**
     * Creates a new web matcher.
     *
     * @param service        the web service to use
     * @param dict           the serialized representation
     */
    protected WebMatcher(WebService service, Dict dict) {
        this.service = service;
        this.dict = dict;
    }

    /**
     * Returns the parent web service this matcher is connected to.
     *
     * @return the parent web service
     */
    public WebService parent() {
        return this.service;
    }

    /**
     * The HTTP request method to match. Defaults to null, meaning
     * that any request method will match.
     *
     * @return the HTTP request method to match
     */
    public String method() {
        return dict.get("method", String.class);
    }

    /**
     * The request protocol to match (i.e. "http" or "https").
     * Defaults to null, meaning that any protocol will match.
     *
     * @return the request protocol to match, or
     *         null to match any protocol
     */
    public String protocol() {
        return dict.get("protocol", String.class);
    }

    /**
     * The web server host name to match. Defaults to null, meaning
     * that any host name will match.
     *
     * @return the web server host name to match, or
     *         null to match any host
     */
    public String host() {
        return dict.get("host", String.class);
    }

    /**
     * The web server port number to match. Defaults to zero (0),
     * meaning that any port number will match.
     *
     * @return the web server port number, or
     *         zero (0) match any port
     */
    public int port() {
        return dict.get("port", Integer.class, 0);
    }

    /**
     * The base request path to match. Defaults to an empty string,
     * meaning that any path will match.
     *
     * @return the base request path to match, or
     *         an empty string to match any request
     */
    public String path() {
        return StringUtils.removeStart(dict.get("path", String.class, ""), "/");
    }

    /**
     * The user authentication required flag. Defaults to false.
     *
     * @return true if user authentication is required, or
     *         false if it is optional
     */
    public boolean auth() {
        return dict.get("auth", Boolean.class, false);
    }

    /**
     * Returns the matcher priority. Defaults to zero (0).
     *
     * @return the matcher priority, or
     *         zero (0) if not set
     */
    public int prio() {
        return dict.get("prio", Integer.class, 0);
    }

    /**
     * Returns the web matcher score.
     *
     * @return the web matcher score
     */
    public int score() {
        int score = dict.get("_score", Integer.class, 0);
        if (score == 0) {
            if (method() != null) {
                score += 400;
            }
            if (protocol() != null) {
                score += 300;
            }
            if (host() != null) {
                score += 200;
            }
            if (port() > 0) {
                score += 100;
            }
            score += 1 + path().length() + prio();
            dict.set("_score", score);
        }
        return score;
    }

    /**
     * Matches the specified request and returns the matching score.
     *
     * @param request        the request to match
     *
     * @return the match score, or
     *         zero (0) if the request doesn't match
     */
    public int match(Request request) {
        if (!match(method(), request.getMethod()) ||
            !match(protocol(), request.getProtocol()) ||
            !match(host(), request.getHost()) ||
            !match(port(), request.getPort())) {
            return 0;
        }
        String matchPath = path();
        String submatchPath = StringUtils.removeEnd(matchPath, "/");
        if (!request.getPath().startsWith(submatchPath)) {
            return 0;
        } else if (!request.getPath().startsWith(matchPath)) {
            return score() - 1;
        } else {
            return score();
        }
    }

    /**
     * Checks if a string matches a pattern.
     *
     * @param pattern        the pattern to match, or null
     * @param value          the value to match
     *
     * @return true if the string matches or the pattern is null, or
     *         false otherwise
     */
    private boolean match(String pattern, String value) {
        return pattern == null || pattern.equalsIgnoreCase(value);
    }

    /**
     * Checks if an integer matches a pattern.
     *
     * @param pattern        the pattern to match, or zero
     * @param value          the value to match
     *
     * @return true if the values are identical or the pattern is zero, or
     *         false otherwise
     */
    private boolean match(int pattern, int value) {
        return pattern == 0 || pattern == value;
    }

    /**
     * Processes a matching request. If authentication is required, an
     * authentication request will be sent if a user isn't available.
     *
     * @param request        the request to process
     */
    public void process(Request request) {
        if (auth() && SecurityContext.currentUser() == null) {
            service.errorUnauthorized(request);
        } else if (request.matchPath(path())) {
            service.process(request);
        }
    }
}
