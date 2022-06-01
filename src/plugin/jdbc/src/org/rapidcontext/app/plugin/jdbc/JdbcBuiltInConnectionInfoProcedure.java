/*
 * RapidContext JDBC plug-in <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2022 Per Cederberg. All rights reserved.
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
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.ConnectionException;

/**
 * The built-in JDBC connection information procedure. This procedure
 * attempts to retrieve detailed information about a specific
 * connection.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class JdbcBuiltInConnectionInfoProcedure implements Procedure {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "PlugIn.Jdbc.Connection.Info";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new JDBC connection info procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public JdbcBuiltInConnectionInfoProcedure() throws ProcedureException {
        defaults.set(JdbcProcedure.BINDING_DB, Bindings.ARGUMENT, "",
                     "The JDBC connection identifier.");
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
        return "Shows detailed information for a JDBC connection.";
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
            throw new ProcedureException(e.getMessage());
        }
        return res;
    }
}
