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

import org.rapidcontext.core.type.Procedure;

/**
 * The default procedure call interceptor. This interceptor provides
 * the standard implementation for all methods (necessary since it
 * is the last link in the interceptor chain). All actual procedure
 * calls will be delegated to the actual procedure implementation,
 * and resource reservation will be delegated to the call context.
 *
 * @deprecated Implemented in ReserveInterceptor or CallInterceptor
 *     instead.
 * @see CallInterceptor
 * @see ReserveInterceptor
 *
 * @author Per Cederberg
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("removal")
public class DefaultInterceptor extends Interceptor {

    /**
     * Creates a new default interceptor.
     */
    public DefaultInterceptor() {
        super(null);
    }

    /**
     * Reserves all adapter connections needed for executing the
     * specified procedure. All connections needed by imported
     * procedures will also be reserved recursively.
     *
     * @param cx             the procedure context
     * @param proc           the procedure definition
     *
     * @throws ProcedureException if the connections couldn't be
     *             reserved
     */
    @Override
    @SuppressWarnings("removal")
    public void reserve(CallContext cx, Procedure proc)
    throws ProcedureException {

        ReserveInterceptor.get().reserve(cx, proc);
    }

    /**
     * Releases all reserved adapter connections. The connections
     * will either be committed or rolled back, depending on the
     * commit flag.
     *
     * @param cx             the procedure context
     * @param commit         the commit (or rollback) flag
     */
    @Override
    @SuppressWarnings("removal")
    public void releaseAll(CallContext cx, boolean commit) {
        ReserveInterceptor.get().releaseAll(cx, commit);
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
     * @throws ProcedureException if the call execution caused an
     *             error
     */
    @Override
    @SuppressWarnings("removal")
    public Object call(CallContext cx, Procedure proc, Bindings bindings)
        throws ProcedureException {

        return CallInterceptor.get().call(cx, proc, bindings);
    }
}
