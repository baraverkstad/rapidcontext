/*
 * RapidContext JDBC plug-in <https://www.rapidcontext.com/>
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

package org.rapidcontext.app.plugin.jdbc;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.ConnectionException;
import org.rapidcontext.core.type.Procedure;

/**
 * The built-in JDBC connection information procedure. This procedure
 * attempts to retrieve detailed information about a specific
 * connection.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class JdbcBuiltInConnectionInfoProcedure extends Procedure {

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public JdbcBuiltInConnectionInfoProcedure(String id, String type, Dict dict) {
        super(id, type, dict);
    }

    /**
     * Executes a call of this procedure in the specified context
     * and with the specified call bindings.
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

        JdbcChannel channel = JdbcProcedure.connectionReserve(cx, bindings);
        Dict res = channel.getConnection().serialize();
        try {
            res.set("_metadata", channel.metadata());
        } catch (ConnectionException e) {
            throw new ProcedureException(this, e);
        }
        return res;
    }
}
