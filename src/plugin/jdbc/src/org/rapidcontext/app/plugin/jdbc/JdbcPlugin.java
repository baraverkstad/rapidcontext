/*
 * RapidContext JDBC plug-in <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg. All rights reserved.
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

package org.rapidcontext.app.plugin.jdbc;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.plugin.Plugin;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.env.AdapterException;
import org.rapidcontext.core.env.AdapterRegistry;
import org.rapidcontext.core.proc.Library;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.StorageException;

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
     * Initializes the plug-in. This method should load or initialize
     * any resources required by the plug-in, such as adding
     * additional handlers to the provided in-memory storage.
     *
     * @param storage        the storage the object is added to
     *
     * @throws StorageException if the initialization failed
     */
    public void init(Storage storage) throws StorageException {
        Library  lib = ApplicationContext.getInstance().getLibrary();

        try {
            AdapterRegistry.register("jdbc", new JdbcAdapter());
        } catch (AdapterException e) {
            throw new StorageException(e.getMessage());
        }
        try {
            Library.registerType("jdbc.query",
                                 JdbcQueryProcedure.class);
            Library.registerType("jdbc.statement",
                                 JdbcStatementProcedure.class);
            lib.addBuiltIn(new JdbcConnectionListProcedure());
            lib.addBuiltIn(new JdbcQueryBuiltInProcedure());
            lib.addBuiltIn(new JdbcStatementBuiltInProcedure());
        } catch (ProcedureException e) {
            throw new StorageException(e.getMessage());
        }
    }

    /**
     * Uninitializes the plug-in. This method should free any
     * resources previously loaded or stored by the plug-in.
     *
     * @param storage        the storage the object is removed from
     *
     * @throws StorageException if the destruction failed
     */
    public void destroy(Storage storage) throws StorageException {
        Library  lib = ApplicationContext.getInstance().getLibrary();

        lib.removeBuiltIn(JdbcConnectionListProcedure.NAME);
        lib.removeBuiltIn(JdbcQueryBuiltInProcedure.NAME);
        lib.removeBuiltIn(JdbcStatementBuiltInProcedure.NAME);
        Library.unregisterType("jdbc.query");
        Library.unregisterType("jdbc.statement");
        try {
            AdapterRegistry.unregister("jdbc");
        } catch (AdapterException e) {
            throw new StorageException(e.getMessage());
        }
    }
}
