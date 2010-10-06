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

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.FileStorage;
import org.rapidcontext.core.data.VirtualStorage;
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
     * The root mount storage path.
     */
    public static final Path PATH_STORAGE = new Path("/storage/");

    /**
     * The plug-in mount storage path.
     */
    public static final Path PATH_PLUGIN = PATH_STORAGE.child("plugin", true);

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
     * The storage used for plug-ins.
     */
    private VirtualStorage storage;

    /**
     * Creates a new plug-in storage.
     *
     * @param pluginDir      the base plug-in directory
     */
    public PluginStorage(File pluginDir) {
        this.pluginDir = pluginDir;
        this.storage = new VirtualStorage();
        File[] files = pluginDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                try {
                    createPlugin(files[i].getName());
                } catch (PluginException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        try {
            loadPlugin(DEFAULT_PLUGIN);
        } catch (PluginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            loadPlugin(LOCAL_PLUGIN);
        } catch (PluginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Creates and mounts a plug-in file storage. This is the first
     * step when installing a plug-in, allowing access to the plug-in
     * files without overlaying then on the root index.
     *
     * @param id             the plug-in identifier
     *
     * @return the plug-in file storage created
     *
     * @throws PluginException if the plug-in hadn't been mounted
     */
    public Storage createPlugin(String id) throws PluginException {
        FileStorage  fs = new FileStorage(new File(this.pluginDir, id));

        try {
            storage.mount(fs, PATH_PLUGIN.child(id, true), false, false, 0);
        } catch (StorageException e) {
            throw new PluginException(e.getMessage());
        }
        return fs;
    }

    /**
     * Destroys and unmounts a plug-in file storage. This is only
     * needed when a new plug-in will be installed over a previous
     * one, otherwise the unloadPlugin() method is sufficient.
     *
     * @param id             the plug-in identifier
     *
     * @throws PluginException if the plug-in hadn't been mounted
     */
    public void destroyPlugin(String id) throws PluginException {
        try {
            storage.unmount(PATH_PLUGIN.child(id, true));
        } catch (StorageException e) {
            throw new PluginException(e.getMessage());
        }
    }

    /**
     * Adds a plug-in overlay to the root index. This means that the
     * plug-in configuration files are now generally accessible.
     *
     * @param id             the plug-in identifier
     *
     * @throws PluginException if the plug-in hadn't been mounted
     */
    public void loadPlugin(String id) throws PluginException {
        boolean  readWrite = LOCAL_PLUGIN.equals(id);
        int      prio = DEFAULT_PLUGIN.equals(id) ? 0 : 100;

        try {
            storage.remount(PATH_PLUGIN.child(id, true), readWrite, true, prio);
        } catch (StorageException e) {
            throw new PluginException(e.getMessage());
        }
    }

    /**
     * Removes a plug-in overlay to the root index.
     *
     * @param id             the plug-in identifier
     *
     * @throws PluginException if the plug-in hadn't been mounted
     */
    public void unloadPlugin(String id) throws PluginException {
        try {
            storage.remount(PATH_PLUGIN.child(id, true), false, true, 0);
        } catch (StorageException e) {
            throw new PluginException(e.getMessage());
        }
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
        return storage.lookup(path);
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
        return storage.load(path);
    }

    /**
     * Stores an object at the specified location. The path must
     * locate a particular object or file, since direct manipulation
     * of indices is not supported. Any previous data at the
     * specified path will be overwritten or removed.
     *
     * @param path           the storage location
     * @param data           the data to store
     *
     * @throws StorageException if the data couldn't be written
     */
    public void store(Path path, Object data) throws StorageException {
        storage.store(path, data);
    }

    /**
     * Removes an object or an index at the specified location. If
     * the path refers to an index, all contained objects and indices
     * will be removed recursively.
     *
     * @param path           the storage location
     *
     * @throws StorageException if the data couldn't be removed
     */
    public void remove(Path path) throws StorageException {
        storage.remove(path);
    }
}
