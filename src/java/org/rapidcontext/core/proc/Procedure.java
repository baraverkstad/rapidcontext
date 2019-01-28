/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2019 Per Cederberg. All rights reserved.
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

/**
 * A procedure that can be called for execution from the public API.
 * Normally a procedure wraps a parameterized call to an external
 * system (such as an SQL query), but more complex procedures are
 * also possible using JavaScript.<p>
 *
 * There are two types of procedures &mdash; add-on and built-in.
 * The add-on procedures are created and modified by the user with a
 * set of configurable parameters (e.g. SQL text, connection pool
 * names, etc). The built-in procedures are basically singleton
 * procedures that cannot be modified by the user. The latter type
 * provides services needed by the platform, such as introspection,
 * administration and control.<p>
 *
 * Each procedure has a number of bindings, containing any resources
 * required during execution. The bindings have different types to
 * clarify their content, such as input argument, static data,
 * adapter pool name or referenced procedure name.<p>
 *
 * Most classes implementing this interface should be subclasses of
 * the AddOnProcedure class and follow the requirements for
 * serialization and configurability defined by that class.
 * Otherwise the procedure is considered a built-in procedure and is
 * responsible for its own initialization and security checks. Great
 * care must be taken when implementing this interface so that any
 * sensitive operation is properly checked for security.
 *
 * @see AddOnProcedure
 * @see Bindings
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public interface Procedure {

    /**
     * Returns the procedure name.
     *
     * @return the procedure name
     */
    public String getName();

    /**
     * Returns the procedure description.
     *
     * @return the procedure description
     */
    public String getDescription();

    /**
     * Returns the bindings for this procedure. If this procedure
     * requires any special data, adapter connection or input
     * argument binding, those bindings should be set (but possibly
     * to null or blank values).
     *
     * @return the bindings for this procedure
     */
    public Bindings getBindings();

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
        throws ProcedureException;
}
