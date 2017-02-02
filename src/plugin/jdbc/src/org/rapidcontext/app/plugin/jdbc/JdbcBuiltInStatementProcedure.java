/*
 * RapidContext JDBC plug-in <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2017 Per Cederberg. All rights reserved.
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

import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.ConnectionException;

/**
 * The built-in JDBC SQL statement procedure. This procedure supports
 * executing a generic SQL statement.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class JdbcBuiltInStatementProcedure implements Procedure {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "PlugIn.Jdbc.Statement";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new JDBC SQL statement procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public JdbcBuiltInStatementProcedure() throws ProcedureException {
        defaults.set(JdbcProcedure.BINDING_DB, Bindings.ARGUMENT, "",
                     "The JDBC connection identifier.");
        defaults.set(JdbcProcedure.BINDING_SQL, Bindings.ARGUMENT, "",
                     "The SQL statement string.");
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
        return "Executes an SQL statement on a JDBC connection.";
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

        JdbcChannel channel = JdbcProcedure.connectionReserve(cx, bindings);
        String sql = (String) bindings.getValue(JdbcProcedure.BINDING_SQL);
        try {
            return channel.executeStatement(sql);
        } catch (ConnectionException e) {
            throw new ProcedureException(e.getMessage());
        }
    }
}
