/*
 * RapidContext HTTP plug-in <https://www.rapidcontext.com/>
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

package org.rapidcontext.app.plugin.http;

import java.util.logging.Logger;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;

/**
 * An HTTP POST procedure. This procedure provides simplified access
 * to HTTP data sending and retrieval.
 *
 * @author   Per Cederberg
 * @version  1.0
 *
 * @deprecated Use HttpRequestProcedure instead (2017-02-01)
 */
@Deprecated
public class HttpPostProcedure extends HttpRequestProcedure {

    /**
     * The class logger.
     */
    private static final Logger LOG = Logger.getLogger(HttpPostProcedure.class.getName());

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public HttpPostProcedure(String id, String type, Dict dict) {
        super(id, type, dict);
        this.dict.set(KEY_TYPE, "procedure/http/post");
        LOG.warning("deprecated: procedure " + id + " type " + type +
                    " is replaced by procedure/http/request");
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

        bindings.set(BINDING_METHOD, Bindings.ARGUMENT, "POST", null);
        bindings.set(BINDING_FLAGS, Bindings.ARGUMENT, "", null);
        return execCall(cx, bindings);
    }
}
