/**
 * RapidContext HTTP plug-in <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2017 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE for more details.
 */

package org.rapidcontext.app.plugin.http;

import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;

/**
 * The built-in HTTP POST procedure. This procedure provides
 * simplified access to HTTP data sending and retrieval.
 *
 * @author   Per Cederberg
 * @version  1.0
 *
 * @deprecated Use HttpRequestBuiltInProcedure instead (2017-02-01)
 */
public class HttpPostBuiltInProcedure implements Procedure {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "PlugIn.Http.Post";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new HTTP POST procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public HttpPostBuiltInProcedure() throws ProcedureException {
        defaults.set(HttpRequestProcedure.BINDING_URL, Bindings.ARGUMENT, "",
                     "The HTTP URL to send the data to.");
        defaults.set(HttpRequestProcedure.BINDING_HEADERS, Bindings.ARGUMENT, "",
                     "Any additional HTTP headers. Headers are listed in " +
                     "'Name: Value' pairs, separated by line breaks. Leave " +
                     "blank for default headers.");
        defaults.set(HttpRequestProcedure.BINDING_DATA, Bindings.ARGUMENT, "",
                     "The HTTP payload data to send. Data should be " +
                     "URL-encoded, unless a 'Content-Type' header is " +
                     "specified. URL-encoded data may be split into lines, " +
                     "which are automatically joined by '&' characters).");
        this.defaults.seal();
    }

    /**
     * Returns the procedure name.
     *
     * @return the procedure name
     */
    public String getName() {
        return NAME;
    }

    /**
     * Returns the procedure description.
     *
     * @return the procedure description
     */
    public String getDescription() {
        return "Sends an HTTP POST request and returns the result. " +
               "DEPRECATED: Use PlugIn.Http.Request instead.";
    }

    /**
     * Returns the bindings for this procedure. If this procedure
     * requires any special data, adapter connection or input
     * argument binding, those bindings should be set (but possibly
     * to null or blank values).
     *
     * @return the bindings for this procedure
     */
    public Bindings getBindings() {
        return defaults;
    }

    /**
     * Executes a call of this procedure in the specified context
     * and with the specified call bindings. The semantics of what
     * the procedure actually does, is up to each implementation.
     * Note that the call bindings are normally inherited from the
     * procedure bindings with arguments bound to their call values.
     *
     * @param cx             the procedure call context
     * @param bindings       the call bindings to use
     *
     * @return the result of the call, or
     *         null if the call produced no result
     *
     * @throws ProcedureException if the call execution caused an
     *             error
     */
    public Object call(CallContext cx, Bindings bindings)
    throws ProcedureException {

        bindings.set(HttpRequestProcedure.BINDING_METHOD, Bindings.ARGUMENT, "POST", null);
        bindings.set(HttpRequestProcedure.BINDING_FLAGS, Bindings.ARGUMENT, "", null);
        return HttpRequestProcedure.execCall(cx, bindings);
    }
}
