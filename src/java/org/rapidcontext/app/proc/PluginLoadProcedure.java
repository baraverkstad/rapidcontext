/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2025 Per Cederberg. All rights reserved.
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

import java.util.logging.Logger;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.plugin.PluginException;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.type.Plugin;
import org.rapidcontext.core.type.Procedure;

/**
 * The built-in plug-in loading procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class PluginLoadProcedure extends Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(PluginLoadProcedure.class.getName());

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public PluginLoadProcedure(String id, String type, Dict dict) {
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
    @Override
    public Object call(CallContext cx, Bindings bindings)
        throws ProcedureException {

        ApplicationContext ctx = ApplicationContext.getInstance();
        String pluginId = (String) bindings.getValue("pluginId");
        Path path = Plugin.instancePath(pluginId);
        CallContext.checkReadAccess(path.toString());
        CallContext.checkWriteAccess(path.toString());
        if (cx.getStorage().lookup(path) != null) {
            String msg = "plug-in '" + pluginId + "' is already loaded";
            throw new ProcedureException(this, msg);
        }
        try {
            LOG.info("loading plugin " + pluginId);
            ctx.loadPlugin(pluginId);
        } catch (PluginException e) {
            String msg = "failed to load plug-in '" + pluginId + "': " +
                         e.getMessage();
            throw new ProcedureException(this, msg);
        }
        return null;
    }
}
