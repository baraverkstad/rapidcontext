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

import java.sql.PreparedStatement;
import java.util.logging.Logger;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.ConnectionException;

/**
 * A JDBC SQL statement procedure. This procedure encapsulates the
 * code for executing a parameterized SQL statement.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class JdbcStatementProcedure extends JdbcProcedure {

    /**
     * The class logger.
     */
    private static final Logger LOG = Logger.getLogger(JdbcStatementProcedure.class.getName());

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public JdbcStatementProcedure(String id, String type, Dict dict) {
        super(id, type, dict);
        if (!type.equals("procedure/jdbc/statement")) {
            this.dict.set(KEY_TYPE, "procedure/jdbc/statement");
            LOG.warning("deprecated: procedure " + id + " references legacy type: " + type);
        }
    }

    /**
     * Executes an SQL statement on the specified connection.
     *
     * @param con            the JDBC connection to use
     * @param stmt           the SQL prepared statement
     * @param flags          the processing and mapping flags
     *
     * @return this method returns a list with generated keys
     *
     * @throws ProcedureException if the SQL couldn't be executed
     *             correctly
     */
    @Override
    protected Object execute(JdbcChannel con,
                             PreparedStatement stmt,
                             String flags)
        throws ProcedureException {

        try {
            return con.executeStatement(stmt);
        } catch (ConnectionException e) {
            throw new ProcedureException(this, e);
        }
    }
}
