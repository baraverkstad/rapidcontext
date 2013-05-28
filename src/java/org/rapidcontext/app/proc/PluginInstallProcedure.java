/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2013 Per Cederberg. All rights reserved.
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
import org.rapidcontext.app.plugin.PluginException;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Session;

/**
 * The built-in plug-in installer procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class PluginInstallProcedure implements Procedure {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.PlugIn.Install";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new plug-in installer procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public PluginInstallProcedure() throws ProcedureException {
        defaults.set("sessionFileId", Bindings.ARGUMENT, "",
                     "The session file identifier (containing the plug-in zip file).");
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
        return "Installs and loads a plug-in located in an uploaded zip file.";
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
        Session             session = (Session) Session.activeSession.get();
        String              fileId;
        File                file;
        String              pluginId;
        String              msg;

        fileId = (String) bindings.getValue("sessionFileId");
        file = session.file(fileId);
        if (file == null || !file.canRead()) {
            msg = "failed to read session file with id '" + fileId + "'";
            throw new ProcedureException(msg);
        }
        CallContext.checkWriteAccess("plugin/" + fileId);
        try {
            pluginId = ctx.installPlugin(file);
            session.removeFile(fileId);
            return pluginId;
        } catch (PluginException e) {
            msg = "failed to install plug-in file '" + file.getName() + "': " +
                  e.getMessage();
            throw new ProcedureException(msg);
        }
    }
}
