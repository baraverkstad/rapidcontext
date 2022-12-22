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

import java.io.File;
import java.io.IOException;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.ZipStorage;
import org.rapidcontext.core.type.Role;
import org.rapidcontext.core.type.User;

/**
 * A ZIP file storage for plug-ins. This class normalizes some legacy
 * data objects loaded from the source ZIP file.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class PluginZipStorage extends ZipStorage {

    /**
     * Creates a new read-only ZIP file storage for a plug-in.
     *
     * @param pluginId       the plug-in identifier
     * @param zipFile        the ZIP file to use
     *
     * @throws IOException if the ZIP couldn't be opened properly
     */
    public PluginZipStorage(String pluginId, File zipFile) throws IOException {
        super(zipFile);
        dict.set(KEY_TYPE, "storage/zip/plugin");
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
        if (path.startsWith(Role.PATH) && obj instanceof Dict) {
            return Role.normalize(path, (Dict) obj);
        } else if (path.startsWith(User.PATH) && obj instanceof Dict) {
            return User.normalize(path, (Dict) obj);
        } else {
            return obj;
        }
    }
}
