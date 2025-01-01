/*
 * RapidContext JDBC plug-in <https://www.rapidcontext.com/>
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

package org.rapidcontext.app.plugin.jdbc;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.ConnectionException;
import org.rapidcontext.core.type.Procedure;

/**
 * The built-in JDBC SQL query procedure. This procedure supports
 * executing a generic SQL query and returning the results in a
 * structured format.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class JdbcBuiltInQueryProcedure extends Procedure {

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public JdbcBuiltInQueryProcedure(String id, String type, Dict dict) {
        super(id, type, dict);
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
    @Override
    public Object call(CallContext cx, Bindings bindings)
    throws ProcedureException {

        JdbcChannel channel = JdbcProcedure.connectionReserve(cx, bindings);
        String sql = (String) bindings.getValue(JdbcProcedure.BINDING_SQL);
        String flags = (String) bindings.getValue(JdbcProcedure.BINDING_FLAGS);
        try {
            return channel.executeQuery(sql, flags);
        } catch (ConnectionException e) {
            throw new ProcedureException(this, e);
        }
    }
}
