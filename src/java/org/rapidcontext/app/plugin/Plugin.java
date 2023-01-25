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

package org.rapidcontext.app.plugin;

import java.util.logging.Logger;

import org.rapidcontext.core.data.Dict;

/**
 * The base plug-in class. A plug-in may extend this class in order
 * to supervise the loading and unloading process. If a plug-in does
 * not declare an overriding implementation, a default instance of
 * this class is created instead.
 *
 * @author   Per Cederberg
 * @version  1.0
 * @deprecated Use org.rapidcontext.core.type.Plugin instead.
 */
@Deprecated
public class Plugin extends org.rapidcontext.core.type.Plugin {

    /**
     * The class logger.
     */
    private static final Logger LOG = Logger.getLogger(Plugin.class.getName());

    /**
     * Creates a new plug-in from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public Plugin(String id, String type, Dict dict) {
        super(id, type, dict);
        LOG.warning("deprecated: legacy plug-in class referenced for " + this);
    }

    /**
     * Creates a new plug-in instance with the specified plug-in
     * configuration data. All subclasses must provide a constructor
     * matching this signature.
     *
     * @param dict           the plug-in configuration data
     *
     * @deprecated Constructor signature has changed as plug-in objects
     *     are now initialized by the root storage.
     */
    @Deprecated
    public Plugin(Dict dict) {
        super(dict.getString(KEY_ID, ""), "plugin", dict);
        LOG.warning("deprecated: legacy plug-in constructor called for " + this);
    }
}
