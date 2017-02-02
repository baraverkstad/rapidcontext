/*
 * RapidContext <http://www.rapidcontext.com/>
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

package org.rapidcontext.app.proc;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.plugin.Plugin;
import org.rapidcontext.app.plugin.PluginManager;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;

/**
 * The built-in plug-in list procedure.
 *
 * @author   Jonas Ekstrand
 * @author   Per Cederberg
 * @version  1.0
 */
public class PluginListProcedure implements Procedure {

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

        CallContext.checkSearchAccess("plugin/");
        ApplicationContext ctx = ApplicationContext.getInstance();
        Dict dict = (Dict) ctx.getStorage().load(Storage.PATH_STORAGEINFO);
        Array arr = dict.getArray("storages");
        Array res = new Array(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            Path path = ((Storage) arr.get(i)).path();
            if (SecurityContext.hasReadAccess(path.toString())) {
                if (path.startsWith(PluginManager.PATH_STORAGE_PLUGIN)) {
                    String pluginId = path.name();
                    dict = ctx.pluginConfig(pluginId);
                    if (dict == null) {
                        dict = new Dict();
                        dict.set(Plugin.KEY_ID, pluginId);
                    } else {
                        dict = dict.copy();
                    }
                    dict.setBoolean("loaded", ctx.isPluginLoaded(pluginId));
                    res.add(dict);
                }
            }
        }
        return res;
    }
}
