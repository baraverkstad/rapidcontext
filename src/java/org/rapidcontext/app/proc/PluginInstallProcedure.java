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

import java.io.File;
import java.util.logging.Logger;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.plugin.PluginException;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.Session;

/**
 * The built-in plug-in installer procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class PluginInstallProcedure extends Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(PluginInstallProcedure.class.getName());

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public PluginInstallProcedure(String id, String type, Dict dict) {
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

        ApplicationContext  ctx = ApplicationContext.getInstance();
        Session             session = Session.activeSession.get();
        String              fileId;
        File                file;
        String              pluginId;
        String              msg;

        fileId = (String) bindings.getValue("sessionFileId");
        file = (session == null) ? null : session.file(fileId);
        if (session == null || file == null || !file.canRead()) {
            msg = "failed to read session file with id '" + fileId + "'";
            throw new ProcedureException(this, msg);
        }
        CallContext.checkWriteAccess("plugin/" + fileId);
        try {
            LOG.info("installing plugin " + file.getName());
            pluginId = ctx.installPlugin(file);
            session.removeFile(fileId);
            return pluginId;
        } catch (PluginException e) {
            msg = "failed to install plug-in file '" + file.getName() + "': " +
                  e.getMessage();
            throw new ProcedureException(this, msg);
        }
    }
}
