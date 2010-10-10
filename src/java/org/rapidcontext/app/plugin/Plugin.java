/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg & Dynabyte AB.
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

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.DynamicObject;
import org.rapidcontext.core.data.Storable;
import org.rapidcontext.core.data.Storage;
import org.rapidcontext.core.data.StorageException;

/**
 * The base plug-in class. A plug-in may extend this class in order
 * to supervise the loading and unloading process. If a plug-in does
 * not declare an overriding implementation, a default instance of
 * this class is created instead.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Plugin extends DynamicObject implements Storable {

    /**
     * The dictionary key for the unique plug-in identifier.
     */
    public static final String KEY_ID = "id";

    /**
     * The dictionary key for the optional plug-in class name.
     */
    public static final String KEY_CLASSNAME = "className";

    /**
     * Creates a new plug-in instance with the specified plug-in
     * configuration data. All subclasses must provide a constructor
     * matching this signature.
     *
     * @param dict           the plug-in configuration data
     */
    public Plugin(Dict dict) {
        super("plugin", dict);
    }

    /**
     * Returns the unique plug-in identifier.
     *
     * @return the unique plug-in identifier
     */
    public String id() {
        return dict.getString(KEY_ID, null);
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
        // Nothing done here by default
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
        // Nothing done here by default
    }
}
