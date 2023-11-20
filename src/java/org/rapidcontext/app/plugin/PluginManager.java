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
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.app.model.AppStorage;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.DirStorage;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.StorageException;
import org.rapidcontext.core.storage.ZipStorage;
import org.rapidcontext.core.type.Plugin;
import org.rapidcontext.core.type.Type;
import org.rapidcontext.util.FileUtil;

/**
 * A plug-in manager. This singleton class contains the utility
 * functions for managing the plug-in loading and unloading.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public final class PluginManager {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(PluginManager.class.getName());

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
    public AppStorage storage;

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
     * Creates a new plug-in storage.
     *
     * @param builtinDir     the built-in plug-in directory
     * @param pluginDir      the base plug-in directory
     * @param storage        the storage to use for plug-ins
     */
    public PluginManager(File builtinDir, File pluginDir, AppStorage storage) {
        this.builtinDir = builtinDir;
        this.pluginDir = pluginDir;
        this.storage = storage;
        if (!builtinDir.equals(pluginDir)) {
            File builtinLocal = new File(builtinDir, LOCAL_PLUGIN);
            File pluginLocal = new File(pluginDir, LOCAL_PLUGIN);
            try {
                FileUtil.copy(builtinLocal, pluginLocal);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "failed to copy " + builtinLocal + " files", e);
            }
        }
        try {
            createStorage(SYSTEM_PLUGIN);
            loadOverlay(SYSTEM_PLUGIN);
        } catch (PluginException ignore) {
            // Error already logged, ignored here
        }
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
        return storage.lookup(Plugin.storagePath(pluginId)) != null;
    }

    /**
     * Checks if the specified plug-in is built-in (or installed).
     * Note any plug-ins located in the built-in plug-in directory
     * will be considered built-in.
     *
     * @param pluginId       the unique plug-in id
     *
     * @return true if the plug-in exists and is built-in, or
     *         false otherwise
     */
    public boolean isBuiltIn(String pluginId) {
        File f = storageFile(pluginId);
        return f != null && f.getParentFile().equals(builtinDir);
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
                ps = new PluginZipStorage(pluginId, file);
            }
            storage.mount(ps, Plugin.storagePath(pluginId));
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
            storage.unmount(Plugin.storagePath(pluginId));
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
            // TODO: Support /plugin/<id>.yaml location too
            storage = new ZipStorage(file);
            dict = storage.load(Path.from("/plugin"), Dict.class);
            if (dict == null) {
                throw new PluginException("missing plugin.properties");
            }
            pluginId = dict.get(Plugin.KEY_ID, String.class);
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
        if (SYSTEM_PLUGIN.equals(pluginId) || LOCAL_PLUGIN.equals(pluginId)) {
            String msg = "cannot force loading of system or local plug-ins";
            throw new PluginException(msg);
        }
        loadOverlay(pluginId);
        loadJarFiles(pluginId);
        Type.all(storage).forEach(o -> { /* Force refresh cached types */ });
        Object obj = storage.load(Path.resolve(Plugin.PATH, pluginId));
        if (!(obj instanceof Plugin)) {
            String msg = pluginId + " plug-in failed to initialize properly";
            LOG.warning(msg);
            throw new PluginException(msg);
        }
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
        try {
            Path path = Plugin.storagePath(pluginId);
            Path cache = Plugin.cachePath(pluginId);
            boolean readWrite = LOCAL_PLUGIN.equals(pluginId);
            int prio = SYSTEM_PLUGIN.equals(pluginId) ? 0 : 100;
            storage.remount(path, readWrite, cache, Path.ROOT, prio);
        } catch (StorageException e) {
            String msg = "failed to overlay " + pluginId + " plug-in storage";
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
        if (SYSTEM_PLUGIN.equals(pluginId) || LOCAL_PLUGIN.equals(pluginId)) {
            String msg = "cannot unload system or local plug-ins";
            throw new PluginException(msg);
        }
        try {
            storage.remount(Plugin.storagePath(pluginId), false, null, null, 0);
        } catch (StorageException e) {
            String msg = "plugin " + pluginId + " storage remount failed";
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
        Path[] paths = storage.query(Plugin.PATH).paths().toArray(Path[]::new);
        for (Path p : paths) {
            String pluginId = p.toIdent(1);
            if (!SYSTEM_PLUGIN.equals(pluginId) && !LOCAL_PLUGIN.equals(pluginId)) {
                try {
                    unload(pluginId);
                } catch (PluginException e) {
                    LOG.warning("failed to unload " + pluginId + " plugin");
                }
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
        Path path = Path.resolve(Plugin.storagePath(pluginId), AppStorage.PATH_LIB);
        storage.query(path)
            .filterFileExtension(".jar")
            .metadatas()
            .filter(meta -> meta.isBinary())
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
            Binary data = storage.load(path, Binary.class);
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
