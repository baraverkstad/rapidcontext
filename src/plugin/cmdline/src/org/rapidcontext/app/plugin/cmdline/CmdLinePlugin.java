/**
 * RapidContext command-line plug-in <http://www.rapidcontext.com/>
 * Copyright (c) 2008-2009 Per Cederberg & Dynabyte AB.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE.txt file for more details.
 */

package org.rapidcontext.app.plugin.cmdline;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.plugin.Plugin;
import org.rapidcontext.app.plugin.PluginException;
import org.rapidcontext.core.proc.Library;
import org.rapidcontext.core.proc.ProcedureException;

/**
 * The command-line plug-in. This class handles the initialization and
 * removal of command-line execution functionality to the application.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class CmdLinePlugin extends Plugin {

    /**
     * Creates a new plug-in instance.
     */
    public CmdLinePlugin() {
        super();
    }

    /**
     * Initializes the plug-in. This will load any resources required
     * by the plug-in and register classes and interfaces to expose
     * the plug-in functionality to the application.
     *
     * @throws PluginException if the plug-in failed to initialize
     *             properly
     */
    public void init() throws PluginException {
        Library  lib = ApplicationContext.getInstance().getLibrary();

        try {
            Library.registerType("cmdline.exec", CmdLineExecProcedure.class);
            lib.addBuiltIn(new CmdLineExecBuiltInProcedure());
        } catch (ProcedureException e) {
            throw new PluginException(e.getMessage());
        }
    }

    /**
     * Uninitializes the plug-in. This will free any resources
     * previously loaded by the plug-in.
     *
     * @throws PluginException if the plug-in failed to uninitialize
     *             properly
     */
    public void destroy() throws PluginException {
        Library  lib = ApplicationContext.getInstance().getLibrary();

        lib.removeBuiltIn(CmdLineExecBuiltInProcedure.NAME);
        Library.unregisterType("cmdline.exec");
    }
}
