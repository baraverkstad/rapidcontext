/**
 * RapidContext HTTP plug-in <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2012 Per Cederberg. All rights reserved.
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

package org.rapidcontext.app.plugin.http;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.plugin.Plugin;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Library;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.StorageException;

/**
 * The HTTP plug-in. This class handles the initialization and removal
 * of HTTP functionality to the application.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class HttpPlugin extends Plugin {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(HttpPlugin.class.getName());

    /**
     * Creates a new plug-in instance with the specified plug-in
     * configuration data.
     *
     * @param dict           the plug-in configuration data
     */
    public HttpPlugin(Dict dict) {
        super(dict);
    }

    /**
     * Initializes the plug-in. This method should load or initialize
     * any resources required by the plug-in, such as adding
     * additional handlers to the provided in-memory storage.
     *
     * @throws StorageException if the initialization failed
     */
    public void init() throws StorageException {
        Library  lib = ApplicationContext.getInstance().getLibrary();

        try {
            System.setProperty("http.keepAlive", "false");
        } catch (Throwable t) {
            String msg = this + ": failed to set http.keepAlive to false";
            LOG.log(Level.WARNING, msg, t);
        }
        try {
            Library.registerType("http.post", HttpPostProcedure.class);
            lib.addBuiltIn(new HttpPostBuiltInProcedure());
        } catch (ProcedureException e) {
            throw new StorageException(e.getMessage());
        }
    }

    /**
     * Uninitializes the plug-in. This method should free any
     * resources previously loaded or stored by the plug-in.
     *
     * @throws StorageException if the destruction failed
     */
    public void destroy() throws StorageException {
        Library  lib = ApplicationContext.getInstance().getLibrary();

        lib.removeBuiltIn(HttpPostBuiltInProcedure.NAME);
        Library.unregisterType("http.post");
    }
}
