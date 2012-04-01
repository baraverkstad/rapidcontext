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

import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.WrappedStorage;
import org.rapidcontext.core.type.Role;
import org.rapidcontext.core.type.User;

/**
 * A storage wrapper for plug-ins. This class performs a number of
 * optional and dynamic transformations to the data loaded from a
 * plug-in storage.  This enables newer version of the platform to
 * load data from old plug-ins (with some caveats).
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class PluginUpgradeStorage extends WrappedStorage {

    /**
     * Creates a new version 0 plug-in storage.
     *
     * @param backend        the storage to wrap
     */
    public PluginUpgradeStorage(Storage backend) {
        super("pluginUpgrade/" + backend.storageType(), backend);
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
            transform(path, (Dict) obj);
        }
        return obj;
    }

    /**
     * Transforms a loaded dictionary object, if needed. This method
     * will only modify dictionary data from some paths and in some
     * cases.
     *
     * @param path           the storage location
     * @param dict           the dictionary data loaded
     */
    private void transform(Path path, Dict dict) {
        boolean hasType = dict.containsKey(KEY_TYPE);
        if (!hasType && path.startsWith(Role.PATH)) {
            dict.set(KEY_TYPE, path.name(0));
            dict.set(KEY_ID, path.name());
        } else if (!hasType && path.startsWith(User.PATH)) {
            dict.set(KEY_TYPE, path.name(0));
            dict.set(KEY_ID, path.name());
            dict.set(User.KEY_NAME, dict.getString(User.KEY_DESCRIPTION, ""));
            dict.set(User.KEY_DESCRIPTION, "");
            Array list = dict.getArray(User.KEY_ROLE);
            for (int i = 0; i < list.size(); i++) {
                list.set(i, list.getString(i, "").toLowerCase());
            }
        }
    }
}
