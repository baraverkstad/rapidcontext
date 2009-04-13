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

package org.rapidcontext.app.plugin;

import org.rapidcontext.core.data.Data;

/**
 * The base plug-in class. A plug-in may extend this class in order
 * to supervise the loading and unloading process. If a plug-in does
 * not declare an overriding implementation, a default instance of
 * this class is created instead.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class Plugin {

    /**
     * The plug-in configuration data.
     */
    private Data data = null;

    /**
     * Creates a new plug-in instance.
     */
    public Plugin() {
        // Nothing to do here
    }

    /**
     * Returns the plug-in data object. This object contains all the
     * required plug-in definition data.
     *
     * @return the plug-in definition data
     */
    public final Data getData() {
        return this.data;
    }

    /**
     * Sets the plug-in data object. This object contains all the
     * required plug-in definition data.
     *
     * @param data           the plug-in definition data
     */
    public final void setData(Data data) {
        this.data = data;
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
        // Nothing to do here
    }

    /**
     * Uninitializes the plug-in. This will free any resources
     * previously loaded by the plug-in.
     *
     * @throws PluginException if the plug-in failed to uninitialize
     *             properly
     */
    public void destroy() throws PluginException {
        // Nothing to do here
    }
}
