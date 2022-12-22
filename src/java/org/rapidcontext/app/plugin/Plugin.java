/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2022 Per Cederberg. All rights reserved.
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

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.StorageException;

/**
 * The base plug-in class. A plug-in may extend this class in order
 * to supervise the loading and unloading process. If a plug-in does
 * not declare an overriding implementation, a default instance of
 * this class is created instead.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Plugin extends StorableObject {

    /**
     * The dictionary key for the minimum platform version.
     */
    public static final String KEY_PLATFORM = "platform";

    /**
     * The plug-in object storage path.
     */
    public static final Path PATH = Path.from("/plugin/");

    /**
     * Creates a new plug-in instance with the specified plug-in
     * configuration data. All subclasses must provide a constructor
     * matching this signature.
     *
     * @param dict           the plug-in configuration data
     */
    public Plugin(Dict dict) {
        // TODO: change constructor signature once automatic init works
        super(null, "plugin", dict);
    }

    /**
     * Initializes the plug-in. This method should load or initialize
     * any resources required by the plug-in, such as adding
     * additional handlers to the provided in-memory storage.
     *
     * @throws StorageException if the initialization failed
     */
    public void init() throws StorageException {
        // TODO: Remove these public methods once init is handled inside
        //       storage package.
    }

    /**
     * Uninitializes the plug-in. This method should free any
     * resources previously loaded or stored by the plug-in.
     *
     * @throws StorageException if the destruction failed
     */
    public void destroy() throws StorageException {
        // TODO: Remove these public methods once destroy is handled inside
        //       storage package.
    }
}
