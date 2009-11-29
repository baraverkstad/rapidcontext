/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2009 Per Cederberg & Dynabyte AB.
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

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.rapidcontext.core.data.Data;
import org.rapidcontext.core.data.DataStore;
import org.rapidcontext.core.data.DataStoreException;
import org.rapidcontext.core.data.PropertiesStore;

/**
 * A plug-in data storage and retrieval handler that links a set of
 * plug-ins into a single data store. This class will contain a
 * properties data store for each plug-in, retaining the plug-in load
 * order when doing data and file lookups. Support is also available
 * for the "local" and "default" plug-ins that will always be first
 * and last in the plug-in data store list.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class PluginDataStore implements DataStore {

    /**
     * The plug-in id of the default plug-in.
     */
    public static final String DEFAULT_PLUGIN = "default";

    /**
     * The plug-in id of the local plug-in.
     */
    public static final String LOCAL_PLUGIN = "local";

    /**
     * The base plug-in directory.
     */
    private File pluginDir;

    /**
     * The data store for the default plug-in.
     */
    private DataStore defaultStore;

    /**
     * The data store for the local plug-in.
     */
    private DataStore localStore;

    /**
     * The list of plug-in data stores.
     */
    private LinkedHashMap stores = new LinkedHashMap();

    /**
     * Creates a new plug-in data store.
     *
     * @param pluginDir      the base plug-in directory
     */
    public PluginDataStore(File pluginDir) {
        this.pluginDir = pluginDir;
        this.defaultStore = createStore(DEFAULT_PLUGIN);
        this.localStore = createStore(LOCAL_PLUGIN);
        this.stores.put(LOCAL_PLUGIN, this.localStore);
    }

    /**
     * Creates a plug-in data store. This is similar to addPlugin(),
     * but doesn't link the created data store to the list of data
     * stores (hence not loading the plug-in).
     *
     * @param id             the plug-in identifier
     *
     * @return the plug-in data store
     */
    public DataStore createStore(String id) {
        File  dir = new File(this.pluginDir, id);

        if (!dir.exists()) {
            dir.mkdir();
        }
        return new PropertiesStore(dir, new File(dir, "files"));
    }

    /**
     * Returns an array of all plug-in identifiers in the data store.
     *
     * @return an array of all plug-in identifiers
     */
    public String[] listPlugins() {
        String[]  ids = new String[this.stores.size() + 1];

        this.stores.keySet().toArray(ids);
        ids[ids.length - 1] = DEFAULT_PLUGIN;
        return ids;
    }

    /**
     * Returns the plug-in data store for a loaded plug-in.
     *
     * @param id             the plug-in identifier
     *
     * @return the data store for the specified plug-in
     */
    public DataStore getPlugin(String id) {
        if (DEFAULT_PLUGIN.equals(id)) {
            return this.defaultStore;
        } else {
            return (DataStore) this.stores.get(id);
        }
    }

    /**
     * Adds a plug-in to the data store. Also creates a data store
     * for the plug-in and adds it to the list of data stores.
     *
     * @param id             the plug-in identifier
     *
     * @return the data store for the specified plug-in
     */
    public DataStore addPlugin(String id) {
        DataStore  pluginStore = createStore(id);

        this.stores.put(id, pluginStore);
        return pluginStore;
    }

    /**
     * Removes a plugin from the data store.
     *
     * @param id             the plug-in identifier
     */
    public void removePlugin(String id) {
        this.stores.remove(id);
    }

    /**
     * Checks if a file exists in this data store. Any existing file
     * will be checked for readability.
     *
     * @param path           the file path
     *
     * @return true if a readable file was found, or
     *         false otherwise
     */
    public boolean hasFile(String path) {
        Iterator   iter = this.stores.values().iterator();
        DataStore  store;

        while (iter.hasNext()) {
            store = (DataStore) iter.next();
            if (store.hasFile(path)) {
                return true;
            }
        }
        return this.defaultStore.hasFile(path);
    }

    /**
     * Checks if a data object exists in this data store.
     *
     * @param type           the type name, or null for generic
     * @param id             the unique object id
     *
     * @return true if the object was found, or
     *         false otherwise
     */
    public boolean hasData(String type, String id) {
        Iterator   iter = this.stores.values().iterator();
        DataStore  store;

        while (iter.hasNext()) {
            store = (DataStore) iter.next();
            if (store.hasData(type, id)) {
                return true;
            }
        }
        return this.defaultStore.hasData(type, id);
    }

    /**
     * Finds a file in this data store. If the exist flag is
     * specified, the file returned will be checked for both
     * existence and readability. If no file is found or if it
     * didn't pass the existence test, null will be returned
     *
     * @param path           the file path
     * @param exist          the existence check flag
     *
     * @return the file object found, or
     *         null if not found
     */
    public File findFile(String path, boolean exist) {
        Iterator   iter = this.stores.values().iterator();
        DataStore  store;
        File       file;

        while (iter.hasNext()) {
            store = (DataStore) iter.next();
            file = store.findFile(path, exist);
            if (file != null) {
                return file;
            }
        }
        return this.defaultStore.findFile(path, exist);
    }

    /**
     * Finds all data object identifiers of a certain type.
     *
     * @param type           the type name, or null for generic
     *
     * @return an array or data object identifiers
     */
    public String[] findDataIds(String type) {
        Iterator       iter = this.stores.values().iterator();
        DataStore      store;
        LinkedHashSet  set = new LinkedHashSet();
        String[]       ids;

        while (iter.hasNext()) {
            store = (DataStore) iter.next();
            set.addAll(Arrays.asList(store.findDataIds(type)));
        }
        set.addAll(Arrays.asList(this.defaultStore.findDataIds(type)));
        ids = new String[set.size()];
        set.toArray(ids);
        return ids;
    }

    /**
     * Returns the last modified timestamp for a data object. This
     * operation should be implemented as a fast path, without need
     * for complete parsing of the data. It is intended to be used
     * for automatically invalidating objects in data object caches.
     *
     * @param type           the type name, or null for generic
     * @param id             the unique object id
     *
     * @return the last modified timestamp, or
     *         zero (0) if unknown
     */
    public long findDataTimeStamp(String type, String id) {
        Iterator   iter = this.stores.values().iterator();
        DataStore  store;

        while (iter.hasNext()) {
            store = (DataStore) iter.next();
            if (store.hasData(type, id)) {
                return store.findDataTimeStamp(type, id);
            }
        }
        return this.defaultStore.findDataTimeStamp(type, id);
    }

    /**
     * Reads an identified data object of a certain type.
     *
     * @param type           the type name, or null for generic
     * @param id             the unique object id
     *
     * @return the data object read, or
     *         null if not found
     *
     * @throws DataStoreException if the data couldn't be read
     */
    public Data readData(String type, String id) throws DataStoreException {
        Iterator   iter = this.stores.values().iterator();
        DataStore  store;
        Data       data;

        while (iter.hasNext()) {
            store = (DataStore) iter.next();
            data = store.readData(type, id);
            if (data != null) {
                return data;
            }
        }
        return this.defaultStore.readData(type, id);
    }

    /**
     * Writes a data object of a certain type.
     *
     * @param type           the type name, or null for generic
     * @param id             the unique object id
     * @param data           the data to write
     *
     * @throws DataStoreException if the data couldn't be written
     */
    public void writeData(String type, String id, Data data)
        throws DataStoreException {

        this.localStore.writeData(type, id, data);
    }
}
