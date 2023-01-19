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
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;

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
     * The class logger.
     */
    private static final Logger LOG = Logger.getLogger(Plugin.class.getName());

    /**
     * The dictionary key for the plug-in build version.
     */
    public static final String KEY_VERSION = "version";

    /**
     * The dictionary key for the minimum platform version needed.
     */
    public static final String KEY_PLATFORM = "platform";

    /**
     * The dictionary key for the plug-in build date.
     */
    public static final String KEY_DATE = "date";

    /**
     * The dictionary key for the plug-in copyright notice.
     */
    public static final String KEY_COPYRIGHT = "copyright";

    /**
     * The dictionary key for the plug-in description text.
     */
    public static final String KEY_DESCRIPTION = "description";

    /**
     * The plug-in object storage path.
     */
    public static final Path PATH = Path.from("/plugin/");

    /**
     * Normalizes a plug-in data object if needed. This method will
     * modify legacy data into the proper keys and values.
     *
     * @param id             the object identifier
     * @param dict           the storage data
     *
     * @return the storage data (possibly modified)
     */
    public static Dict normalize(String id, Dict dict) {
        if (!dict.containsKey(KEY_TYPE)) {
            LOG.warning("deprecated: plugin " + id + " data: missing object type");
            dict.set(KEY_TYPE, "plugin");
        }
        return dict;
    }

    /**
     * Creates a new plug-in from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public Plugin(String id, String type, Dict dict) {
        super(id, type, normalize(id, dict));
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
        this(dict.getString(KEY_ID, ""), "plugin", dict);
    }
}
