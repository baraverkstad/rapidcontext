/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2009 Per Cederberg & Dynabyte AB.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE.txt file for more details.
 */

package org.rapidcontext.app.proc;

import java.io.File;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.plugin.PluginDataStore;
import org.rapidcontext.core.data.Data;
import org.rapidcontext.core.data.DataStore;
import org.rapidcontext.core.data.DataStoreException;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.Restricted;
import org.rapidcontext.core.security.SecurityContext;

/**
 * The built-in plug-in list procedure.
 *
 * @author   Jonas Ekstrand, Dynabyte AB
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class PluginListProcedure implements Procedure, Restricted {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.PlugIn.List";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new plug-in list procedure.
     */
    public PluginListProcedure() {
        defaults.seal();
    }

    /**
     * Checks if the currently authenticated user has access to this object.
     * 
     * @return true if the current user has access, or false otherwise
     */
    public boolean hasAccess() {
        return SecurityContext.hasAdmin();
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
        return "Lists all available plug-ins.";
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

        ApplicationContext  ctx = ApplicationContext.getInstance();
        PluginDataStore     store = ctx.getDataStore();
        String[]            ids = store.listPlugins();
        DataStore           ds;
        File[]              files;
        String              id;
        Data                res;
        Data                data;

        res = new Data(ids.length);
        for (int i = 0; i < ids.length; i++) {
            ds = store.getPlugin(ids[i]);
            data = null;
            try {
                data = ds.readData(null, "plugin");
            } catch (DataStoreException ignore) {
                // Read errors are handled below
            }
            if (data == null) {
                data = new Data();
                data.set("id", ids[i]);
            }
            data.setBoolean("loaded", true);
            res.add(data);
        }
        files = ctx.getPluginDir().listFiles();
        for (int i = 0; i < files.length; i++) {
            id = files[i].getName();
            if (store.getPlugin(id) == null && files[i].isDirectory()) {
                ds = store.createStore(id);
                try {
                    data = ds.readData(null, "plugin");
                    if (data != null) {
                        data.setBoolean("loaded", false);
                        res.add(data);
                    }
                } catch (DataStoreException ignore) {
                    // Skip plug-ins with invalid file
                }
            }
        }
        return res;
    }
}
