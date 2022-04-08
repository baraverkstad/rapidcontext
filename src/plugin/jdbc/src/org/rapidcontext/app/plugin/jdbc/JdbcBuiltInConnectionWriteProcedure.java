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
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.StorageException;
import org.rapidcontext.core.type.ConnectionException;
import org.rapidcontext.core.type.Type;

/**
 * The built-in JDBC connection write procedure. This procedure
 * saves or removes a JDBC connection from the data storage. Note
 * that only connections in the local plug-in can be removed.
 *
 * @deprecate Use System.Storage.Write or HTTP storage API instead.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class JdbcBuiltInConnectionWriteProcedure implements Procedure {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "PlugIn.Jdbc.Connection.Write";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new JDBC connection write procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public JdbcBuiltInConnectionWriteProcedure() throws ProcedureException {
        defaults.set("id", Bindings.ARGUMENT, "", "The connection storage identifier.");
        defaults.set("data", Bindings.ARGUMENT, "",
                     "An object with at least the required keys from the " +
                     "connection/jdbc type set.");
        defaults.seal();
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
        return "Creates or overwrites a connection by creating a new one in " +
               "the local plug-in. Other versions of the connection may " +
               "still exist in other plug-ins, but will be hidden. Only " +
               "connections in the local plug-in can be removed.";
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

        Storage storage = cx.getStorage();
        Dict res = null;
        String id = (String) bindings.getValue("id");
        Dict data = (Dict) bindings.getValue("data", null);
        Path path = JdbcConnection.PATH.descendant(new Path(id));
        CallContext.checkWriteAccess(path.toString());
        if (data == null) {
            try {
                storage.remove(path);
            } catch (StorageException e) {
                throw new ProcedureException(e.getMessage());
            }
        } else {
            Type type = Type.find(storage, "connection/jdbc");
            res = new Dict();
            res.set(JdbcConnection.KEY_ID, id);
            data.remove(JdbcConnection.KEY_ID);
            res.set(JdbcConnection.KEY_TYPE, type.id());
            data.remove(JdbcConnection.KEY_TYPE);
            for (Object o : type.properties()) {
                Dict dict = (Dict) o;
                String name = dict.getString("name", null);
                if (data.containsKey(name)) {
                    res.set(name, data.get(name));
                } else if (dict.getBoolean("required", false)) {
                    throw new ProcedureException("missing required '" + name +
                                                 "' property");
                }
                data.remove(name);
            }
            res.addAll(data);
            try {
                storage.store(path, createConnection(id, type.id(), res));
            } catch (StorageException e) {
                throw new ProcedureException(e.getMessage());
            }
        }
        return res;
    }

    /**
     * Creates, initializes and tests a new JDBC connection.
     *
     * @param id             the connection id
     * @param type           the type id
     * @param dict           the data dictionary
     *
     * @return the JDBC connection created
     *
     * @throws StorageException if the connection creation or testing
     *             failed
     */
    private JdbcConnection createConnection(String id, String type, Dict dict)
    throws StorageException {

        JdbcConnection con = new JdbcConnection(id, type, dict);
        con.init();
        try {
            JdbcChannel channel = (JdbcChannel) con.reserve();
            con.release(channel);
        } catch (ConnectionException e) {
            con.destroy();
            throw new StorageException(e.getMessage());
        }
        return con;
    }
}
