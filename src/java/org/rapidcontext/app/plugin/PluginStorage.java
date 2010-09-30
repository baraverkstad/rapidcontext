/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg. All rights reserved.
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

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.FileStorage;
import org.rapidcontext.core.data.Path;
import org.rapidcontext.core.data.Storage;
import org.rapidcontext.core.data.StorageException;

/**
 * A persistent data storage and retrieval handler that links a set
 * of  plug-in directories as a single coherent storage. This class
 * contain a file storage for each plug-in, retaining the plug-in
 * load order when doing lookups. Support is also available for the
 * "local" and "default" plug-ins that will always be the first and
 * last plug-in the lookup list.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class PluginStorage implements Storage {

    /**
     * The identifier of the default plug-in.
     */
    public static final String DEFAULT_PLUGIN = "default";

    /**
     * The identifier of the local plug-in.
     */
    public static final String LOCAL_PLUGIN = "local";

    /**
     * The base plug-in directory.
     */
    private File pluginDir;

    /**
     * The file storage for the default plug-in.
     */
    private Storage defaultPlugin;

    /**
     * The file storage for the local plug-in.
     */
    private Storage localPlugin;

    /**
     * The list of plug-in file storages.
     */
    private LinkedHashMap plugins = new LinkedHashMap();

    /**
     * Creates a new plug-in storage.
     *
     * @param pluginDir      the base plug-in directory
     */
    public PluginStorage(File pluginDir) {
        this.pluginDir = pluginDir;
        this.defaultPlugin = createStorage(DEFAULT_PLUGIN);
        this.localPlugin = createStorage(LOCAL_PLUGIN);
        this.plugins.put(LOCAL_PLUGIN, this.localPlugin);
    }

    /**
     * Creates a plug-in data storage. This is similar to
     * addPlugin(), but doesn't link the created data storage to the
     * lookup list (hence not loading the plug-in).
     *
     * @param id             the plug-in identifier
     *
     * @return the plug-in data storage
     */
    public Storage createStorage(String id) {
        File  dir = new File(this.pluginDir, id);

        if (!dir.exists()) {
            dir.mkdir();
        }
        return new FileStorage(dir);
    }

    /**
     * Returns an array of all loaded plug-in identifiers.
     *
     * @return an array of all plug-in identifiers
     */
    public String[] listPlugins() {
        String[]  ids = new String[plugins.size() + 1];

        plugins.keySet().toArray(ids);
        ids[ids.length - 1] = DEFAULT_PLUGIN;
        return ids;
    }

    /**
     * Returns the plug-in storage for a loaded plug-in.
     *
     * @param id             the plug-in identifier
     *
     * @return the data storage for the specified plug-in
     */
    public Storage getPlugin(String id) {
        if (DEFAULT_PLUGIN.equals(id)) {
            return this.defaultPlugin;
        } else {
            return (Storage) plugins.get(id);
        }
    }

    /**
     * Adds a plug-in to this storage. This will create the file
     * storage for the plug-in itself and add it to the list of
     * loaded plug-in file storages.
     *
     * @param id             the plug-in identifier
     *
     * @return the data store for the specified plug-in
     */
    public Storage addPlugin(String id) {
        Storage  storage = createStorage(id);

        plugins.put(id, storage);
        return storage;
    }

    /**
     * Removes a plug-in from the data store.
     *
     * @param id             the plug-in identifier
     */
    public void removePlugin(String id) {
        plugins.remove(id);
    }

    /**
     * Searches for an object at the specified location and returns
     * meta-data about the object if found. The path may locate
     * either an index or a specific object. 
     *
     * @param path           the storage location
     *
     * @return the meta-data dictionary for the object, or
     *         null if not found
     *
     * @throws StorageException if the storage couldn't be accessed
     */
    public Dict lookup(Path path) throws StorageException {
        Iterator   iter = plugins.keySet().iterator();
        String     plugin;
        Storage    storage;
        Dict       meta = null;

        while (iter.hasNext()) {
            plugin = (String) iter.next();
            storage = (Storage) plugins.get(plugin);
            meta = storage.lookup(path);
            if (meta != null) {
                // TODO: Should merge meta-data for indices...
                meta.set("plugin", plugin);
                return meta;
            }
        }
        meta = defaultPlugin.lookup(path);
        if (meta != null) {
            meta.set("plugin", DEFAULT_PLUGIN);
        }
        return meta;
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
     *
     * @throws StorageException if the data couldn't be read
     */
    public Object load(Path path) throws StorageException {
        Iterator   iter = plugins.values().iterator();
        Storage    storage;
        Object     res;
        Dict       index = null;

        while (iter.hasNext()) {
            storage = (Storage) iter.next();
            res = storage.load(path);
            if (path.isIndex()) {
                index = mergeIndex(index, (Dict) res);
            } else if (res != null) {
                return res;
            }
        }
        res = defaultPlugin.load(path);
        return path.isIndex() ? mergeIndex(index, (Dict) res) : res;
    }

    /**
     * Merges two index dictionaries.
     *
     * @param base           the base index dictionary
     * @param tmp            the index dictionary to add
     *
     * @return the merged index
     */
    private Dict mergeIndex(Dict base, Dict tmp) {
        if (base == null) {
            return tmp;
        } else if (tmp == null) {
            return base;
        } else {
            merge(base.getArray("directories"), tmp.getArray("directories"));
            merge(base.getArray("objects"), tmp.getArray("objects"));
            return base;
        }
    }

    /**
     * Merges two arrays by adding all missing values from the second
     * array to the first one.
     *
     * @param base           the base array
     * @param tmp            the array to add elements from
     */
    private void merge(Array base, Array tmp) {
        for (int i = 0; i < tmp.size(); i++) {
            String name = tmp.getString(i, null);
            if (!base.containsValue(name)) {
                base.add(name);
            }
        }
    }

    /**
     * Stores or removes an object at the specified location. The
     * path must locate a particular object or file, since direct
     * manipulation of indices is not supported. Any previous data
     * at the specified path will be overwritten or removed without
     * any notice. The data types supported for storage depends on
     * implementation, but normally files and dictionaries are
     * accepted.
     *
     * @param path           the storage location
     * @param data           the data to store, or null to delete
     *
     * @throws StorageException if the data couldn't be written
     */
    public void store(Path path, Object data) throws StorageException {
        localPlugin.store(path, data);
    }
}
