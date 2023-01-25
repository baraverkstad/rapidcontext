/*
 * RapidContext <https://www.rapidcontext.com/>
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

package org.rapidcontext.app.proc;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.plugin.PluginManager;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.Plugin;
import org.rapidcontext.core.type.Procedure;

/**
 * The built-in plug-in list procedure.
 *
 * @author   Jonas Ekstrand
 * @author   Per Cederberg
 * @version  1.0
 */
public class PluginListProcedure extends Procedure {

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public PluginListProcedure(String id, String type, Dict dict) {
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
    public Object call(CallContext cx, Bindings bindings)
        throws ProcedureException {

        CallContext.checkSearchAccess("plugin/");
        ApplicationContext ctx = ApplicationContext.getInstance();
        Dict info = (Dict) ctx.getStorage().load(Storage.PATH_STORAGEINFO);
        Array storages = info.getArray("storages");
        Array res = new Array(storages.size());
        for (Object o : storages) {
            String path = ((Dict) o).getString(Storage.KEY_MOUNT_PATH, "/");
            if (SecurityContext.hasReadAccess(path)) {
                if (path.startsWith(PluginManager.PATH_STORAGE_PLUGIN.toString())) {
                    String pluginId = Path.from(path).name();
                    Dict conf = ctx.pluginConfig(pluginId);
                    if (conf == null) {
                        conf = new Dict();
                        conf.set(Plugin.KEY_ID, pluginId);
                    } else {
                        conf = conf.copy();
                    }
                    conf.setBoolean("loaded", ctx.isPluginLoaded(pluginId));
                    res.add(conf);
                }
            }
        }
        return res;
    }
}
