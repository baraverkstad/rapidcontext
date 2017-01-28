/*
 * RapidContext JDBC plug-in <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2013 Per Cederberg. All rights reserved.
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

import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.type.Connection;

/**
 * The built-in JDBC connection list procedure. This procedure
 * provides a list of all currently available JDBC connections,
 * with some additional status information.
 *
 * @deprecate Use System.Connection.List instead.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class JdbcBuiltInConnectionListProcedure implements Procedure {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "PlugIn.Jdbc.Connection.List";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new JDBC connection list procedure.
     */
    public JdbcBuiltInConnectionListProcedure() {
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
        return "Lists all available JDBC connections and their current status.";
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

        CallContext.checkSearchAccess(Connection.PATH.toString());
        Metadata[] meta = cx.getStorage().lookupAll(Connection.PATH);
        Array res = new Array(meta.length);
        for (int i = 0; i < meta.length; i++) {
            Path path = meta[i].path();
            if (SecurityContext.hasReadAccess(path.toString())) {
                Object obj = cx.getStorage().load(path);
                if (obj instanceof JdbcConnection) {
                    res.add(((JdbcConnection) obj).serialize().copy());
                }
            }
        }
        return res;
    }
}
