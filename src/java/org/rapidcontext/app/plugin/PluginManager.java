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
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.FileStorage;
import org.rapidcontext.core.data.Path;
import org.rapidcontext.core.data.Storage;
import org.rapidcontext.core.data.StorageException;
import org.rapidcontext.core.data.VirtualStorage;
import org.rapidcontext.util.FileUtil;

/**
 * A plug-in manager. This singleton class contains the utility
 * functions for managing the plug-in loading and unloading.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class PluginManager {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(PluginManager.class.getName());

    /**
     * The storage path to the mounted plug-in storages.
     */
    public static final Path PATH_STORAGE = new Path("/storage/plugin/");

    /**
     * The identifier of the default plug-in.
     */
    public static final String DEFAULT_PLUGIN = "default";

    /**
     * The identifier of the local plug-in.
     */
    public static final String LOCAL_PLUGIN = "local";

    /**
     * The plug-in directory. This is the base directory from which
     * plug-ins are loaded.
     */
    public File pluginDir = null;

    /**
     * The storage to use when loading and unloading plug-ins.
     */
    public VirtualStorage storage;

    /**
     * The map of plug-in instances. The map is indexed by the
     * plug-in identifier.
     */
    // TODO: replace with memory storage
    private HashMap plugins = new HashMap();

    /**
     * The plug-in class loader.
     */
    public PluginClassLoader classLoader = new PluginClassLoader();

    /**
     * Returns the plug-in storage path for a specified plug-in id.
     *
     * @param pluginId       the unique plug-in id
     *
     * @return the plug-in storage path
     */
    public static Path storagePath(String pluginId) {
        return PATH_STORAGE.child(pluginId, true);
    }

    /**
     * Returns the plug-in configuration object path for a specified
     * plug-in id.
     *
     * @param pluginId       the unique plug-in id
     *
     * @return the plug-in configuration storage path
     */
    public static Path configPath(String pluginId) {
        return storagePath(pluginId).child("plugin", false);
    }

    /**
     * Creates a new plug-in storage.
     *
     * @param pluginDir      the base plug-in directory
     * @param storage        the storage to use for plug-ins
     */
    public PluginManager(File pluginDir, VirtualStorage storage) {
        this.pluginDir = pluginDir;
        this.storage = storage;
        File[] files = pluginDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                try {
                    createStorage(files[i].getName());
                } catch (PluginException ignore) {
                    // Error already logged, ignored here
                }
            }
        }
        try {
            loadOverlay(DEFAULT_PLUGIN);
        } catch (PluginException ignore) {
            // Error already logged, ignored here
        }
        try {
            loadOverlay(LOCAL_PLUGIN);
        } catch (PluginException ignore) {
            // Error already logged, ignored here
        }
    }

    /**
     * Checks if the specified plug-in is currently loaded.
     *
     * @param pluginId       the unique plug-in id
     *
     * @return true if the plug-in was loaded, or
     *         false otherwise
     */
    public boolean isLoaded(String pluginId) {
        return plugins.containsKey(pluginId) ||
               DEFAULT_PLUGIN.equals(pluginId) ||
               LOCAL_PLUGIN.equals(pluginId);
    }

    /**
     * Creates and mounts a plug-in file storage. This is the first
     * step when installing a plug-in, allowing access to the plug-in
     * files without overlaying then on the root index.
     *
     * @param pluginId       the unique plug-in id
     *
     * @return the plug-in file storage created
     *
     * @throws PluginException if the plug-in had already been mounted
     */
    private Storage createStorage(String pluginId) throws PluginException {
        Path     path = storagePath(pluginId);
        File     dir = new File(this.pluginDir, pluginId);
        Storage  fs = new FileStorage(dir, path, false);

        try {
            storage.mount(fs, false, false, 0);
        } catch (StorageException e) {
            String msg = "failed to create " + pluginId + " plug-in storage: " +
                         e.getMessage();
            LOG.severe(msg);
            throw new PluginException(msg);
        }
        return fs;
    }

    /**
     * Destroys a plug-in file storage. This is only needed when a
     * new plug-in will be installed over a previous one, otherwise
     * the unload() method is sufficient.
     *
     * @param pluginId       the unique plug-in id
     *
     * @throws PluginException if the plug-in hadn't been mounted
     */
    private void destroyStorage(String pluginId) throws PluginException {
        try {
            storage.unmount(storagePath(pluginId));
        } catch (StorageException e) {
            String msg = "failed to remove " + pluginId + " plug-in storage: " +
                         e.getMessage();
            LOG.severe(msg);
            throw new PluginException(msg);
        }
    }

    /**
     * Installs a plug-in from the specified file. If an existing
     * plug-in with the same id already exists, it will be replaced
     * without warning. Note that the new plug-in will NOT be loaded.
     *
     * @param file           the plug-in ZIP file
     *
     * @return the unique plug-in id
     *
     * @throws PluginException if the plug-in couldn't be installed
     *             correctly
     */
    public String install(File file) throws PluginException {
        ZipFile      zip = null;
        ZipEntry     entry;
        InputStream  is;
        Properties   props;
        String       pluginId;
        File         dir;
        String       msg;

        try {
            zip = new ZipFile(file);
            entry = zip.getEntry("plugin.properties");
            if (entry == null) {
                msg = "missing plugin.properties inside zip file " + file.getName();
                LOG.warning(msg);
                throw new PluginException(msg);
            }
            is = zip.getInputStream(entry);
            props = new Properties();
            try {
                props.load(is);
            } finally {
                try {
                    is.close();
                } catch (Exception ignore) {
                    // Ignore exception on closing file
                }
            }
            pluginId = props.getProperty("id");
            if (pluginId == null || pluginId.trim().length() < 0) {
                msg = "missing plug-in identifier in plugin.properties";
                throw new PluginException(msg);
            }
            dir = new File(pluginDir, pluginId);
            if (dir.exists()) {
                unload(pluginId);
                destroyStorage(pluginId);
                // TODO: perhaps backup the old directory instead?
                FileUtil.delete(dir);
            }
            FileUtil.unpackZip(zip, dir);
        } catch (IOException e) {
            msg = "IO error while reading zip file " + file.getName() + ": " +
                  e.getMessage();
            LOG.warning(msg);
            throw new PluginException(msg);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ignore) {
                    // Do nothing
                }
            }
        }
        createStorage(pluginId);
        return pluginId;
    }

    /**
     * Loads a plug-in. The plug-in file storage will be added to the
     * root overlay and the plug-in configuration file will be used
     * to initialize the plug-in Java class.
     *
     * @param pluginId       the unique plug-in id
     *
     * @throws PluginException if the plug-in loading failed
     */
    public void load(String pluginId) throws PluginException {
        Plugin  plugin;
        Dict    dict;
        String  className;
        Class   cls;
        Object  obj;
        String  msg;

        // Load plug-in configuration
        if (DEFAULT_PLUGIN.equals(pluginId) || LOCAL_PLUGIN.equals(pluginId)) {
            msg = "cannot force loading of default or local plug-ins";
            throw new PluginException(msg);
        }
        try {
            dict = (Dict) storage.load(configPath(pluginId));
            if (dict == null) {
                throw new StorageException("file not found");
            }
        } catch (StorageException e) {
            msg = "couldn't load " + pluginId + " plugin config file: " +
                  e.getMessage();
            LOG.warning(msg);
            throw new PluginException(msg);
        }

        // Add to root overlay
        loadOverlay(pluginId);

        // Create plug-in instance
        classLoader.addPluginJars(new File(this.pluginDir, pluginId));
        className = dict.getString("className", null);
        if (className == null || className.trim().length() <= 0) {
            plugin = new Plugin();
        } else {
            try {
                cls = classLoader.loadClass(className);
            } catch (Throwable e) {
                msg = "couldn't load " + pluginId + " plugin class " +
                      className + ": " + e.getMessage();
                LOG.warning(msg);
                throw new PluginException(msg);
            }
            try {
                obj = cls.newInstance();
            } catch (Throwable e) {
                msg = "couldn't create " + pluginId + " plugin instance for " +
                      className + ": " + e.getMessage();
                LOG.warning(msg);
                throw new PluginException(msg);
            }
            if (obj instanceof Plugin) {
                plugin = (Plugin) obj;
            } else {
                msg = pluginId + " plugin class " + className +
                      " doesn't implement the Plugin interface";
                LOG.warning(msg);
                throw new PluginException(msg);
            }
        }

        // Initialize plug-in instance
        plugin.setData(dict);
        try {
            plugin.init();
        } catch (Throwable e) {
            msg = "plugin class " + className + " threw exception on init: " +
                  e.getMessage();
            LOG.warning(msg);
            throw new PluginException(msg);
        }
        plugins.put(pluginId, plugin);
    }

    /**
     * Loads a plug-in storage to the root overlay. The plug-in
     * storage must already have been mounted.
     *
     * @param pluginId       the unique plug-in id
     *
     * @throws PluginException if the plug-in storage couldn't be
     *             overlaid on the root
     */
    private void loadOverlay(String pluginId) throws PluginException {
        boolean  readWrite = LOCAL_PLUGIN.equals(pluginId);
        int      prio = DEFAULT_PLUGIN.equals(pluginId) ? 0 : 100;
        String   msg;

        try {
            storage.remount(storagePath(pluginId), readWrite, true, prio);
        } catch (StorageException e) {
            msg = "failed to overlay " + pluginId + " plug-in storage: " +
                  e.getMessage();
            LOG.severe(msg);
            throw new PluginException(msg);
        }
    }

    /**
     * Unloads a plug-in. All plug-in classes will be destroyed and
     * the plug-in file storage will be hidden from the root overlay.
     *
     * @param pluginId       the unique plug-in id
     *
     * @throws PluginException if the plug-in unloading failed
     */
    public void unload(String pluginId) throws PluginException {
        Plugin  plugin = (Plugin) plugins.get(pluginId);
        String  msg;

        if (DEFAULT_PLUGIN.equals(pluginId) || LOCAL_PLUGIN.equals(pluginId)) {
            msg = "cannot unload default or local plug-ins";
            throw new PluginException(msg);
        }
        if (plugin != null) {
            plugin.destroy();
        } else {
            LOG.severe("skipping destroy on plugin: " + pluginId);
        }
        try {
            storage.remount(storagePath(pluginId), false, false, 0);
        } catch (StorageException e) {
            msg = "plugin " + pluginId + " storage remount failed:" +
                  e.getMessage();
            LOG.warning(msg);
            throw new PluginException(msg);
        }
        plugins.remove(pluginId);
    }

    /**
     * Unloads all plug-ins. All plug-in classes will be destroyed
     * and the plug-in file storages will be hidden from the root
     * overlay. Note that the built-in plug-ins will be unaffected by
     * this.
     */
    public void unloadAll() {
        String[]  ids = new String[plugins.size()];

        plugins.keySet().toArray(ids);
        for (int i = 0; i < ids.length; i++) {
            try {
                unload(ids[i]);
            } catch (PluginException e) {
                LOG.warning("failed to unload " + ids[i] + " plugin: " +
                            e.getMessage());
            }
        }
        classLoader = new PluginClassLoader();
    }
}
