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

package org.rapidcontext.core.proc;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.type.Interceptor;
import org.rapidcontext.core.type.Procedure;

/**
 * A procedure call interceptor. Allows overriding, auditing or transforming
 * results either pre- or post-call.
 *
 * This class implements a default action if it is the last interceptor in
 * the chain. Otherwise it forwards the call to the next one. Custom call
 * interceptors can be created by subclassing this class.
 *
 * @author Per Cederberg
 */
public class CallInterceptor extends Interceptor {

    /**
     * Returns the top-level call interceptor.
     *
     * @return the call interceptor
     */
    public static CallInterceptor get() {
        return Interceptor.get(CallInterceptor.class);
    }

    /**
     * Creates a new call interceptor from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public CallInterceptor(String id, String type, Dict dict) {
        super(id, type, dict);
    }

    /**
     * Calls a procedure with the specified bindings.
     *
     * @param cx             the procedure context
     * @param proc           the procedure definition
     * @param bindings       the procedure bindings
     *
     * @return the result of the call, or
     *         null if the call produced no result
     *
     * @throws ProcedureException if the call execution caused an error
     */
    public Object call(CallContext cx, Procedure proc, Bindings bindings)
        throws ProcedureException {

        if (next() instanceof CallInterceptor nxt) {
            return nxt.call(cx, proc, bindings);
        } else {
            long start = System.currentTimeMillis();
            try {
                cx.logCall(proc.id(), bindings);
                Object obj = proc.call(cx, bindings);
                cx.logResponse(obj);
                proc.report(start, true, null);
                return obj;
            } catch (ProcedureException e) {
                cx.logError(e);
                proc.report(start, false, e.toString());
                throw e;
            }
        }
    }
}
