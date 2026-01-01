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
 * A procedure call interceptor. This is an abstract class that
 * allows subclasses to override, monitor or extend any resource
 * reservation or procedure call in the library. Interceptors are
 * chained together, and by default each interceptor delegates calls
 * to its parent interceptor. A default interceptor is available
 * that provides a standard implementation for all methods
 * (necessary since it is the last link in the interceptor chain).
 *
 * @deprecated Use ReserveInterceptor or CallInterceptor instead.
 * @see CallInterceptor
 * @see ReserveInterceptor
 *
 * @author Per Cederberg
 */
@Deprecated(forRemoval = true)
public abstract class Interceptor {

    /**
     * The parent interceptor. This is supposed to be a non-null
     * value for all but the default interceptor.
     */
    private Interceptor parent = null;

    /**
     * Creates a new interceptor and links it to the specified
     * parent interceptor.
     *
     * @param parent         the parent interceptor
     */
    protected Interceptor(Interceptor parent) {
        this.parent = parent;
    }

    /**
     * Returns the parent interceptor in the interceptor chain.
     *
     * @return the parent interceptor, or
     *         null if no parent is available
     */
    public final Interceptor getParent() {
        return parent;
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
     *
     * @deprecated Inherit from ReserveInterceptor instead.
     * @see org.rapidcontext.core.proc.ReserveInterceptor
     */
    @Deprecated(forRemoval = true)
    public void reserve(CallContext cx, Procedure proc)
        throws ProcedureException {

        parent.reserve(cx, proc);
    }

    /**
     * Releases all reserved adapter connections. The connections
     * will either be committed or rolled back, depending on the
     * commit flag.
     *
     * @param cx             the procedure context
     * @param commit         the commit (or rollback) flag
     *
     * @deprecated Inherit from ReserveInterceptor instead.
     * @see org.rapidcontext.core.proc.ReserveInterceptor
     */
    @Deprecated(forRemoval = true)
    public void releaseAll(CallContext cx, boolean commit) {
        parent.releaseAll(cx, commit);
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
     *
     * @deprecated Inherit from CallInterceptor instead.
     * @see CallInterceptor
     */
    @Deprecated(forRemoval = true)
    public Object call(CallContext cx, Procedure proc, Bindings bindings)
        throws ProcedureException {

        return parent.call(cx, proc, bindings);
    }
}
