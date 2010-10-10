/*
 * RapidContext JDBC plug-in <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.rapidcontext.app.plugin.jdbc;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.plugin.Plugin;
import org.rapidcontext.app.plugin.PluginException;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.env.AdapterException;
import org.rapidcontext.core.env.AdapterRegistry;
import org.rapidcontext.core.proc.Library;
import org.rapidcontext.core.proc.ProcedureException;

/**
 * The JDBC plug-in. This class handles the initialization and removal
 * of JDBC functionality to the application.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class JdbcPlugin extends Plugin {

    /**
     * Creates a new plug-in instance with the specified plug-in
     * configuration data.
     *
     * @param dict           the plug-in configuration data
     */
    public JdbcPlugin(Dict dict) {
        super(dict);
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
            AdapterRegistry.register("jdbc", new JdbcAdapter());
        } catch (AdapterException e) {
            throw new PluginException(e.getMessage());
        }
        try {
            Library.registerType("jdbc.query",
                                 JdbcQueryProcedure.class);
            Library.registerType("jdbc.statement",
                                 JdbcStatementProcedure.class);
            lib.addBuiltIn(new JdbcQueryBuiltInProcedure());
            lib.addBuiltIn(new JdbcStatementBuiltInProcedure());
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

        lib.removeBuiltIn(JdbcQueryBuiltInProcedure.NAME);
        lib.removeBuiltIn(JdbcStatementBuiltInProcedure.NAME);
        Library.unregisterType("jdbc.query");
        Library.unregisterType("jdbc.statement");
        try {
            AdapterRegistry.unregister("jdbc");
        } catch (AdapterException e) {
            throw new PluginException(e.getMessage());
        }
    }
}
