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
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Library;
import org.rapidcontext.core.storage.DirStorage;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.StorageException;
import org.rapidcontext.core.storage.RootStorage;
import org.rapidcontext.core.storage.ZipStorage;
import org.rapidcontext.core.type.Type;
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
     * The storage path to the mounted plug-in file storages.
     */
    public static final Path PATH_STORAGE_PLUGIN =
        RootStorage.PATH_STORAGE.child("plugin", true);

    /**
     * The storage path to the loaded plug-in objects.
     */
    public static final Path PATH_PLUGIN = new Path("/plugin/");

    /**
     * The platform information path.
     */
    public static final Path PATH_INFO = new Path("/platform");

    /**
     * The identifier of the system plug-in.
     */
    public static final String SYSTEM_PLUGIN = "system";

    /**
     * The identifier of the local plug-in.
     */
    public static final String LOCAL_PLUGIN = "local";

    /**
     * The built-in plug-in directory. This is the base directory from
     * which built-in plug-ins are loaded.
     */
    public File builtinDir = null;

    /**
     * The plug-in directory. This is the base directory from which
     * plug-ins are loaded.
     */
    public File pluginDir = null;

    /**
     * The storage to use when loading and unloading plug-ins.
     */
    public RootStorage storage;

    /**
     * The platform information dictionary.
     */
    public Dict platformInfo;

    /**
     * The plug-in class loader.
     */
    public PluginClassLoader classLoader = new PluginClassLoader();

    /**
     * A list of all temporary files created. When destroying all
     * plug-ins, all these files are deleted.
     */
    private ArrayList<File> tempFiles = new ArrayList<>();

    /**
     * Returns the plug-in storage path for a specified plug-in id.
     *
     * @param pluginId       the unique plug-in id
     *
     * @return the plug-in storage path
     */
    public static Path storagePath(String pluginId) {
        return PATH_STORAGE_PLUGIN.child(pluginId, true);
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
     * Returns the plug-in instance path for a specified plug-in id.
     *
     * @param pluginId       the unique plug-in id
     *
     * @return the plug-in instance path
     */
    public static Path pluginPath(String pluginId) {
        Path ident = storagePath(pluginId).relativeTo(RootStorage.PATH_STORAGE);
        Path cachePath = RootStorage.PATH_STORAGE_CACHE.descendant(ident);
        return cachePath.descendant(PATH_PLUGIN.child(pluginId, false));
    }

    /**
     * Returns the plug-in identifier for a storage object. The
     * object storage paths will be used to return the first matching
     * plug-in.
     *
     * @param meta           the metadata object
     *
     * @return the plug-in identifier, or
     *         null if not found
     */
    public static String pluginId(Metadata meta) {
        for (Object o : meta.storagePaths()) {
            Path path = (Path) o;
            if (path.startsWith(PATH_STORAGE_PLUGIN)) {
                return path.name();
            }
        }
        return null;
    }

    /**
     * Creates a new plug-in storage.
     *
     * @param builtinDir     the built-in plug-in directory
     * @param pluginDir      the base plug-in directory
     * @param storage        the storage to use for plug-ins
     */
    public PluginManager(File builtinDir, File pluginDir, RootStorage storage) {
        this.builtinDir = builtinDir;
        this.pluginDir = pluginDir;
        this.storage = storage;
        try {
            createStorage(SYSTEM_PLUGIN);
            loadOverlay(SYSTEM_PLUGIN);
        } catch (PluginException ignore) {
            // Error already logged, ignored here
        }
        this.platformInfo = (Dict) storage.load(PATH_INFO);
        initStorages(pluginDir);
        initStorages(builtinDir);
        try {
            loadOverlay(LOCAL_PLUGIN);
        } catch (PluginException ignore) {
            // Error already logged, ignored here
        }
    }

    /**
     * Initializes the plug-in storages found in a base plug-in
     * directory. Any errors will be logged and ignored. If a storage
     * has already been mounted (from another base directory), it
     * will be omitted.
     *
     * @param baseDir        the base plug-in directory
     */
    private void initStorages(File baseDir) {
        for (File f : baseDir.listFiles()) {
            String id = pluginId(f);
            if (id != null && !isAvailable(id)) {
                try {
                    createStorage(id);
                } catch (PluginException ignore) {
                    // Error already logged, ignored here
                }
            }
        }
    }

    /**
     * Checks if the specified plug-in is currently available, i.e.
     * if it has been mounted to the plug-in storage.
     *
     * @param pluginId       the unique plug-in id
     *
     * @return true if the plug-in was available, or
     *         false otherwise
     */
    public boolean isAvailable(String pluginId) {
        return storage.lookup(storagePath(pluginId)) != null;
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
        return storage.lookup(pluginPath(pluginId)) != null ||
               SYSTEM_PLUGIN.equals(pluginId) ||
               LOCAL_PLUGIN.equals(pluginId);
    }

    /**
     * Checks if the specified plug-in storage may contain legacy
     * data.
     *
     * @param pluginId       the unique plug-in id
     * @param storage        the storage to check
     *
     * @return true if the storage is considered legacy, or
     *         false otherwise
     */
    private boolean isLegacyPlugin(String pluginId, Storage storage) {
        Dict dict = (Dict) storage.load(new Path("/plugin"));
        String version = "";
        if (dict != null) {
            version = dict.getString(Plugin.KEY_PLATFORM, "");
        }
        return !SYSTEM_PLUGIN.equals(pluginId) &&
               !version.equals(this.platformInfo.getString("version", ""));
    }

    /**
     * Returns the specified plug-in configuration dictionary.
     *
     * @param pluginId       the unique plug-in id
     *
     * @return the plug-in configuration dictionary, or
     *         null if not found
     */
    public Dict config(String pluginId) {
        return (Dict) storage.load(configPath(pluginId));
    }

    /**
     * Returns the plug-in identifier corresponding to a storage file
     * or directory.
     *
     * @param file           the file to check
     *
     * @return the plug-in identifier, or
     *         null if not a supported storage file
     */
    private String pluginId(File file) {
        String name = file.getName();
        if (file.isDirectory()) {
            return name;
        } else if (name.endsWith(".zip")) {
            return StringUtils.removeEnd(name, ".zip");
        } else {
            return null;
        }
    }

    /**
     * Finds the best matching plug-in storage file or directory for
     * a plug-in identifier.
     *
     * @param pluginId       the unique plug-in id
     *
     * @return the readable plug-in file or directory, or
     *         null if not found
     */
    private File storageFile(String pluginId) {
        File[] files = {
            new File(pluginDir, pluginId + ".zip"),
            new File(pluginDir, pluginId),
            new File(builtinDir, pluginId + ".zip"),
            new File(builtinDir, pluginId)
        };
        for (File f : files) {
            if (f.canRead()) {
                return f;
            }
        }
        return null;
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
    private Storage createStorage(String pluginId)
    throws PluginException {
        File     file = storageFile(pluginId);
        Storage  ps;
        String   msg;

        if (file == null) {
            msg = "couldn't find " + pluginId + " plug-in storage";
            LOG.log(Level.SEVERE, msg);
            throw new PluginException(msg);
        }
        try {
            if (file.isDirectory()) {
                ps = new DirStorage(file, false);
            } else {
                ps = new ZipStorage(file);
            }
            if (isLegacyPlugin(pluginId, ps)) {
                ps = new PluginUpgradeStorage(ps);
            }
            storage.mount(ps, storagePath(pluginId), false, null, 0);
        } catch (Exception e) {
            msg = "failed to create " + pluginId + " plug-in storage";
            LOG.log(Level.SEVERE, msg, e);
            throw new PluginException(msg + ": " + e.getMessage());
        }
        return ps;
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
            String msg = "failed to remove " + pluginId + " plug-in storage";
            LOG.log(Level.SEVERE, msg, e);
            throw new PluginException(msg + ": " + e.getMessage());
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
        ZipStorage  storage = null;
        Dict        dict;
        String      pluginId;
        File        dst;
        String      msg;

        try {
            storage = new ZipStorage(file);
            dict = (Dict) storage.load(new Path("/plugin"));
            if (dict == null) {
                throw new PluginException("missing plugin.properties");
            }
            pluginId = dict.getString(Plugin.KEY_ID, null);
            if (pluginId == null || pluginId.trim().length() < 0) {
                msg = "missing plug-in identifier in plugin.properties";
                throw new PluginException(msg);
            }
            if (isAvailable(pluginId)) {
                unload(pluginId);
                destroyStorage(pluginId);
            }
            dst = new File(pluginDir, pluginId);
            if (dst.exists()) {
                FileUtil.delete(dst);
            }
            dst = new File(pluginDir, pluginId + ".zip");
            if (dst.exists()) {
                FileUtil.delete(dst);
            }
            FileUtil.copy(file, dst);
        } catch (Exception e) {
            msg = "invalid plug-in file " + file.getName();
            LOG.log(Level.WARNING, msg, e);
            throw new PluginException(msg + ": " + e.getMessage());
        } finally {
            if (storage != null) {
                try {
                    storage.destroy();
                } catch (Exception ignore) {
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
        String  msg;

        // Load plug-in configuration
        if (SYSTEM_PLUGIN.equals(pluginId) || LOCAL_PLUGIN.equals(pluginId)) {
            msg = "cannot force loading of system or local plug-ins";
            throw new PluginException(msg);
        }
        Dict dict = config(pluginId);
        if (dict == null) {
            msg = "couldn't load " + pluginId + " plugin config file";
            LOG.warning(msg);
            throw new PluginException(msg);
        }

        // Create overlay & load JAR files
        loadOverlay(pluginId);
        loadJarFiles(pluginId);

        // Create plug-in instance
        Plugin plugin;
        Library.builtInPlugin = pluginId;
        String className = dict.getString(Plugin.KEY_CLASSNAME, null);
        if (className == null || className.trim().length() <= 0) {
            plugin = new Plugin(dict);
        } else {
            Class<?> cls;
            try {
                cls = classLoader.loadClass(className);
            } catch (Throwable e) {
                msg = "couldn't load " + pluginId + " plugin class " +
                      className;
                LOG.log(Level.WARNING, msg, e);
                throw new PluginException(msg + ": " + e.getMessage());
            }
            if (!ClassUtils.getAllSuperclasses(cls).contains(Plugin.class)) {
                msg = pluginId + " plugin class " + className +
                      " isn't a subclass of the Plugin class";
                LOG.warning(msg);
                throw new PluginException(msg);
            }
            Constructor<?> ctor;
            try {
                ctor = cls.getConstructor(new Class[] { Dict.class});
            } catch (Throwable e) {
                msg = pluginId + " plugin class " + className +
                      " missing constructor with valid signature";
                LOG.log(Level.WARNING, msg, e);
                throw new PluginException(msg + ": " + e.getMessage());
            }
            try {
                plugin = (Plugin) ctor.newInstance(new Object[] { dict });
            } catch (Throwable e) {
                msg = "couldn't create " + pluginId + " plugin instance for " +
                      className;
                LOG.log(Level.WARNING, msg, e);
                throw new PluginException(msg + ": " + e.getMessage());
            }
        }

        // Initialize plug-in instance
        try {
            Type.all(storage).forEach(o -> { /* Force refresh cached types */ });
            // TODO: plug-in initialization should be handled by storage
            plugin.init();
            storage.store(pluginPath(pluginId), plugin);
        } catch (Throwable e) {
            msg = "plugin class " + className + " threw exception on init";
            LOG.log(Level.WARNING, msg, e);
            throw new PluginException(msg + ": " + e.getMessage());
        }
        Library.builtInPlugin = SYSTEM_PLUGIN;
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
        int      prio = SYSTEM_PLUGIN.equals(pluginId) ? 0 : 100;
        String   msg;

        try {
            storage.remount(storagePath(pluginId), readWrite, Path.ROOT, prio);
        } catch (StorageException e) {
            msg = "failed to overlay " + pluginId + " plug-in storage";
            LOG.log(Level.SEVERE, msg, e);
            throw new PluginException(msg + ": " + e.getMessage());
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
        Path    path = pluginPath(pluginId);
        String  msg;

        if (SYSTEM_PLUGIN.equals(pluginId) || LOCAL_PLUGIN.equals(pluginId)) {
            msg = "cannot unload system or local plug-ins";
            throw new PluginException(msg);
        }
        try {
            storage.remove(path);
        } catch (StorageException e) {
            msg = "failed destroy call on " + pluginId + " plugin";
            LOG.log(Level.SEVERE, msg, e);
        }
        try {
            storage.remount(storagePath(pluginId), false, null, 0);
        } catch (StorageException e) {
            msg = "plugin " + pluginId + " storage remount failed";
            LOG.log(Level.WARNING, msg, e);
            throw new PluginException(msg + ": " + e.getMessage());
        }
    }

    /**
     * Unloads all plug-ins. All plug-in classes will be destroyed
     * and the plug-in file storages will be hidden from the root
     * overlay. Note that the built-in plug-ins will be unaffected by
     * this.
     */
    public void unloadAll() {
        String[] ids = storage.query(PATH_PLUGIN)
            .objects(Plugin.class)
            .map(p -> p.id())
            .toArray(String[]::new);
        for (String pluginId : ids) {
            try {
                unload(pluginId);
            } catch (PluginException e) {
                LOG.warning("failed to unload " + pluginId + " plugin");
            }
        }
        storage.cacheClean(true);
        classLoader = new PluginClassLoader();
        while (tempFiles.size() > 0) {
            File file = tempFiles.remove(tempFiles.size() - 1);
            try {
                file.delete();
            } catch (Exception ignore) {
                // File will be deleted on exit instead
            }
        }
    }

    /**
     * Loads all JAR files found in the specified plug-in library
     * path. All the files found will be copied to a temporary
     * directory before loading in order to avoid file locking and
     * other issues.
     *
     * @param pluginId       the unique plug-in id
     */
    private void loadJarFiles(String pluginId) {
        Path path = storagePath(pluginId).descendant(RootStorage.PATH_LIB);
        storage.query(path)
            .filterExtension(".jar")
            .metadatas()
            .filter(meta -> meta != null && meta.isBinary())
            .forEach(meta -> loadJarFile(meta.path()));
    }

    /**
     * Adds a JAR file to the plug-in class loader. The JAR file will
     * be copied from storage to a temporary directory to avoid file
     * locking and other issues. This method will only log errors on
     * failure and no error will be thrown.
     *
     * @param path           the storage path to the JAR file
     */
    private void loadJarFile(Path path) {
        try {
            LOG.fine("adding JAR to class loader: " + path);
            Binary data = (Binary) storage.load(path);
            File tmpFile = FileUtil.tempFile(path.name());
            tempFiles.add(tmpFile);
            try (InputStream is = data.openStream()) {
                FileUtil.copy(is, tmpFile);
            }
            classLoader.addJar(tmpFile);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "failed to load JAR file: " + path, e);
        }
    }

    /**
     * Simple extension of URLClassLoader to be able to add new URL:s
     * after creation.
     */
    public static class PluginClassLoader extends URLClassLoader {

        /**
         * Creates a new empty class loader.
         */
        public PluginClassLoader() {
            super(new URL[0], PluginClassLoader.class.getClassLoader());
        }

        /**
         * Adds the specified JAR file to the class loader.
         *
         * @param file       the JAR file to add
         *
         * @throws IOException if the file couldn't be located
         */
        public void addJar(File file) throws IOException {
            addURL(file.toURI().toURL());
        }
    }
}
