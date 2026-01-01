/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2026 Per Cederberg. All rights reserved.
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
 * A procedure reserve interceptor. Allows overriding, monitoring or
 * extending resource reservation prior to executing procedure calls.
 * This is used e.g. to reserve pooled database connections, check
 * execution permissions or similar.
 *
 * This class implements a default action if it is the last interceptor in
 * the chain. Otherwise it forwards the call to the next one. Custom reserve
 * interceptors can be created by subclassing this class.
 *
 * @author Per Cederberg
 */
public class ReserveInterceptor extends Interceptor {

    /**
     * Returns the top-level reserve interceptor.
     *
     * @return the call interceptor
     */
    public static ReserveInterceptor get() {
        return Interceptor.get(ReserveInterceptor.class);
    }

    /**
     * Creates a new reserve interceptor from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public ReserveInterceptor(String id, String type, Dict dict) {
        super(id, type, dict);
    }

    /**
     * Reserves all resources needed for executing a procedure. All
     * resources needed by sub-procedures will also be reserved.
     *
     * @param cx             the procedure context
     * @param proc           the procedure definition
     *
     * @throws ProcedureException if some resource couldn't be reserved
     */
    public void reserve(CallContext cx, Procedure proc)
        throws ProcedureException {

        if (next() instanceof ReserveInterceptor i) {
            i.reserve(cx, proc);
        } else {
            cx.reserveImpl();
        }
    }

    /**
     * Releases all reserved adapter connections. The connections
     * will either be committed or rolled back, depending on the
     * commit flag.
     *
     * @param cx             the procedure context
     * @param commit         the commit (or rollback) flag
     */
    public void releaseAll(CallContext cx, boolean commit) {
        if (next() instanceof ReserveInterceptor i) {
            i.releaseAll(cx, commit);
        } else {
            cx.connectionReleaseAll(commit);
        }
    }
}
