/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2012 Per Cederberg. All rights reserved.
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
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.WrappedStorage;
import org.rapidcontext.core.type.Role;

/**
 * A legacy plug-in storage wrapper. This class performs a number of
 * dynamic transformations to the storage of an old legacy plug-in.
 * This enabled newer version of the platform to load old plug-ins
 * without requiring changes.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class LegacyPluginStorage extends WrappedStorage {

    /**
     * Checks if the specified storage belongs to a legacy plug-in.
     *
     * @param storage        the storage to check
     *
     * @return true if the storage is considered legacy, or
     *         false otherwise
     */
    public static boolean isLegacyPlugin(Storage storage) {
        Dict dict = (Dict) storage.load(new Path("/plugin"));
        // TODO: Perform proper platform version check
        return dict.getString(Plugin.KEY_PLATFORM, "").equals("");
    }

    /**
     * Creates a new legacy plug-in storage.
     *
     * @param backend        the storage to wrap
     */
    public LegacyPluginStorage(Storage backend) {
        super("legacyPlugin/" + backend.storageType(), backend);
    }

    /**
     * Loads an object from the specified location. The path may
     * locate either an index or a specific object. In case of an
     * index, the data returned is an index dictionary listing of
     * all objects in it.
     *
     * @param path           the storage location
     *
     * @return the data read, or
     *         null if not found
     */
    public Object load(Path path) {
        Object obj = super.load(path);
        if (obj instanceof Dict) {
            fixMissingObjectType(path, (Dict) obj);
        }
        return obj;
    }

    /**
     * Adds missing object type fields to some results. This method
     * will modify the specified dictionary data.
     *
     * @param path           the storage location
     * @param dict           the dictionary data found
     */
    private void fixMissingObjectType(Path path, Dict dict) {
        if (!dict.containsKey(KEY_TYPE) && path.startsWith(Role.PATH)) {
            dict.set(KEY_TYPE, path.name(0));
        }
    }
}
