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

package org.rapidcontext.app.web;

import java.util.logging.Logger;

import org.apache.commons.lang3.ArrayUtils;
import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.proc.StatusProcedure;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.JsonSerializer;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.WebService;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.core.web.Request;

/**
 * A status API web service. This service provides a server-side status API
 * for simple version information.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StatusWebService extends WebService {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(StatusWebService.class.getName());

    /**
     * Creates a new status web service from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public StatusWebService(String id, String type, Dict dict) {
        super(id, type, dict);
    }

    /**
     * Returns the HTTP methods implemented for the specified
     * request. The OPTIONS or HEAD methods doesn't have to be added
     * to the result (added automatically later).
     *
     * @param request        the request to check
     *
     * @return the array of HTTP method names supported
     *
     * @see #methods(Request)
     */
    protected String[] methodsImpl(Request request) {
        return METHODS_GET;
    }

    /**
     * Processes an HTTP GET request.
     *
     * @param request        the request to process
     */
    protected void doGet(Request request) {
        try {
            ApplicationContext ctx = ApplicationContext.getInstance();
            Object[] args = ArrayUtils.EMPTY_OBJECT_ARRAY;
            String source = "web [" + request.getRemoteAddr() + "]";
            Object obj = ctx.execute(StatusProcedure.NAME, args, source, null);
            request.sendText(Mime.JSON[0], JsonSerializer.serialize(obj, true));
        } catch (ProcedureException e) {
            LOG.warning("error in system status check: " + e.getMessage());
            Dict res = new Dict();
            res.set("error", e.getMessage());
            request.sendText(Mime.JSON[0], JsonSerializer.serialize(res, true));
        }
    }
}
