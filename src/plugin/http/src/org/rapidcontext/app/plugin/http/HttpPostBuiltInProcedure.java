/**
 * RapidContext HTTP plug-in <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2013 Per Cederberg. All rights reserved.
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
 * simplified access to HTTP data sending and retrieval. It is
 * restricted to admin-only access for security reasons. Create a
 * more specified add-on procedure with the "http.post" type to
 * allow other users access to this functionality.
 *
 * @author   Per Cederberg
 * @version  1.0
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
        defaults.set(HttpPostProcedure.BINDING_URL, Bindings.ARGUMENT, "",
                     "The HTTP URL to send the data to.");
        defaults.set(HttpPostProcedure.BINDING_HEADER, Bindings.ARGUMENT, "",
                     "The additional HTTP headers or blank for none.");
        defaults.set(HttpPostProcedure.BINDING_DATA, Bindings.ARGUMENT, "",
                     "The HTTP POST data to send or blank for none.");
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
        return "Sends an HTTP POST request and returns the result.";
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

        return HttpPostProcedure.execCall(cx, null, bindings);
    }
}
