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

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.core.storage.Index;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.ZipStorage;

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
        Path legacyPath = locatePath(Path.from("/plugin"));
        if (legacyPath != null) {
            // Relocate to /plugin/<id>.properties location
            String ext = StringUtils.removeStart(legacyPath.name(), "plugin");
            Path fixedPath = Path.resolve(Plugin.PATH, pluginId + ext);
            Index root = (Index) entries.get(Path.ROOT);
            Object data = entries.get(legacyPath);
            root.removeObject(legacyPath.name());
            paths.remove(legacyPath);
            entries.remove(legacyPath);
            Index idx = new Index();
            root.addIndex(Plugin.PATH.name());
            paths.put(Plugin.PATH, Plugin.PATH);
            entries.put(Plugin.PATH, idx);
            idx.addObject(fixedPath.name());
            paths.put(fixedPath, fixedPath);
            entries.put(fixedPath, data);
        }
    }
}
