/*
 * RapidContext JDBC plug-in <https://www.rapidcontext.com/>
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

package org.rapidcontext.app.plugin.jdbc;

import java.sql.PreparedStatement;

import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.ConnectionException;

/**
 * A JDBC SQL query procedure. This procedure encapsulates the code
 * for executing a parameterized SQL query and returning the results
 * in a structured format.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class JdbcQueryProcedure extends JdbcProcedure {

    /**
     * Creates a new JDBC query procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public JdbcQueryProcedure() throws ProcedureException {
        super();
    }

    /**
     * Executes an SQL query on the specified connection.
     *
     * @param con            the JDBC connection to use
     * @param stmt           the SQL prepared statement
     * @param flags          the processing and mapping flags
     *
     * @return the query results, or
     *         null for statements
     *
     * @throws ProcedureException if the SQL couldn't be executed
     *             correctly
     */
    protected Object execute(JdbcChannel con,
                             PreparedStatement stmt,
                             String flags)
        throws ProcedureException {

        try {
            return con.executeQuery(stmt, flags);
        } catch (ConnectionException e) {
            throw new ProcedureException(e.getMessage());
        }
    }
}
