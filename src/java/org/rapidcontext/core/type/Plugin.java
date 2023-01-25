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

package org.rapidcontext.core.type;

import java.util.logging.Logger;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;

/**
 * A bundle of add-on functionality to the system. The plug-in
 * bundles types, code, JAR files, web files and data objects into a
 * storage bundle that can be loaded and unloaded dynamically. A
 * plug-in is usually distributed as a ZIP file with the '.plugin'
 * file extension.
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
}
