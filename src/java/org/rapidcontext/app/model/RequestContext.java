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

import org.rapidcontext.core.ctx.ThreadContext;
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
     * Creates a new request execution context for a web request.
     *
     * @param request        the request being processed
     * @param session        the request session (or null)
     * @param user           the request user (or null)
     *
     * @return a new execution context
     */
    public static RequestContext enter(Request request, Session session, User user) {
        String source = "web [" + request.getRemoteAddr() + "]";
        RequestContext cx = new RequestContext(source);
        cx.set(CX_REQUEST, request);
        cx.set(CX_SESSION, session);
        cx.set(CX_USER, user);
        cx.open();
        return cx;
    }

    /**
     * Creates a new request execution context for a command-line call.
     *
     * @param user           the request user (or null)
     *
     * @return a new execution context
     */
    public static RequestContext enter(User user) {
        String source = "command-line [" + user + "]";
        RequestContext cx = new RequestContext(source);
        cx.set(CX_USER, user);
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
}
