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

package org.rapidcontext.app.web;

import java.util.ArrayList;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.js.JsSerializer;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.core.web.Request;
import org.rapidcontext.core.web.RequestHandler;

/**
 * A procedure execution request handler. This request handler is
 * used to trigger server-side procedure execution with the POST:ed
 * data.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ProcedureRequestHandler extends RequestHandler {

    /**
     * Returns the HTTP methods supported for the specified request
     * (path). This method assumes local request paths (removal of
     * the mapped URL base).
     *
     * @param request        the request to check
     *
     * @return the array of HTTP method names supported
     */
    public String[] methods(Request request) {
        return POST_METHODS_ONLY;
    }

    /**
     * Processes an HTTP POST request.
     *
     * @param request        the request to process
     */
    protected void doPost(Request request) {
        ApplicationContext  ctx = ApplicationContext.getInstance();
        Dict                res = new Dict();
        ArrayList           argList = new ArrayList();
        Object[]            args;
        StringBuffer        trace = null;
        String              str = "";
        Object              obj;

        res.set("data", null);
        res.set("trace", null);
        res.set("error", null);
        try {
            for (int i = 0; str != null; i++) {
                str = request.getParameter("arg" + i, null);
                if (str != null) {
                    argList.add(JsSerializer.unserialize(str));
                }
            }
            args = argList.toArray();
            if (request.getParameter("trace", null) != null) {
                trace = new StringBuffer();
            }
            str = "web [" + request.getRemoteAddr() + "]";
            obj = ctx.execute(request.getPath(), args, str, trace);
            res.set("data", obj);
        } catch (Exception e) {
            res.set("error", e.getMessage());
        }
        if (trace != null) {
            res.set("trace", trace.toString());
        }
        request.sendData(Mime.JSON[0], JsSerializer.serialize(res));
    }
}
