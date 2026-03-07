/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2026 Per Cederberg. All rights reserved.
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

package org.rapidcontext.app;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.rapidcontext.app.model.AppStorage;
import org.rapidcontext.app.plugin.PluginException;
import org.rapidcontext.app.plugin.PluginManager;
import org.rapidcontext.core.ctx.Context;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.StorageException;
import org.rapidcontext.core.type.Connection;
import org.rapidcontext.core.type.Environment;
import org.rapidcontext.core.type.Interceptor;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.Session;
import org.rapidcontext.core.type.Type;
import org.rapidcontext.core.type.User;
import org.rapidcontext.core.type.Vault;
import org.rapidcontext.core.type.WebMatcher;
import org.rapidcontext.core.type.WebService;
import org.rapidcontext.util.ClasspathUtil;
import org.rapidcontext.util.FileUtil;

/**
 * The application context. This is a singleton object that contains
 * references to global application settings and objects. It also
 * provides simple procedure execution and resource and plug-in
 * initialization and deinitialization.
 *
 * @author Per Cederberg
 */
public class ApplicationContext extends Context {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(ApplicationContext.class.getName());

    /**
     * The path to the global configuration.
     */
    public static final Path PATH_CONFIG = Path.from("/config");

    /**
     * The class load time (system initialization time).
     */
    public static final Date INIT_TIME = new Date();

    /**
     * The context start (or reset) time.
     */
    public static Date START_TIME = new Date();

    /**
     * The seconds to wait between runs of the storage cache cleaner job.
     */
    private static final int CACHE_CLEAN_WAIT_SECS = 10;

    /**
     * The minutes to wait between runs of the expired session cleaner job.
     */
    private static final int SESSION_CLEAN_WAIT_MINS = 10;

    /**
     * The plug-in manager.
     */
    private PluginManager pluginManager;


    /**
     * The application configuration.
     */
    private Dict config;

    /**
     * The platform version.
     */
    private Dict version;

    /**
     * The cached list of web matchers (from the web services).
     */
    private WebMatcher[] matchers = null;

    /**
     * Returns the currently active application context.
     *
     * @return the currently active application context, or null
     */
    public static ApplicationContext active() {
        return Context.active(ApplicationContext.class);
    }

    /**
     * Creates and initializes the application context. If the start
     * flag is set, all plug-ins will be loaded along with procedures
     * and the environment configuration. Otherwise only the storages
     * will be initialized. Note that if the context has already been
     * created, it will not be recreated.
     *
     * @param baseDir        the base application directory
     * @param localDir       the local add-on directory
     * @param start          the initialize plug-ins flag
     *
     * @return the application context created or found
     */
    protected static synchronized ApplicationContext init(File baseDir,
                                                          File localDir,
                                                          boolean start) {
        ApplicationContext ctx = (ApplicationContext) root;
        if (ctx == null) {
            ctx = new ApplicationContext(baseDir, localDir);
            ctx.open();
        }
        if (start) {
            ctx.initAll();
        }
        return ctx;
    }

    /**
     * Destroys the application context and frees all resources used.
     */
    protected static synchronized void destroy() {
        if (root instanceof ApplicationContext ctx) {
            ctx.destroyAll();
            ctx.close();
            root = null;
        }
    }

    /**
     * Returns the singleton application context instance.
     *
     * @return the singleton application context instance
     *
     * @deprecated Use active() instead.
     * @see #active()
     */
    @Deprecated(forRemoval = true)
    public static ApplicationContext getInstance() {
        return (ApplicationContext) root;
    }

    /**
     * Creates a new application context. This constructor should
     * only be called once in the application and it will store away
     * the instance created.
     *
     * @param baseDir        the base application directory
     * @param localDir       the local add-on directory
     */
    private ApplicationContext(File baseDir, File localDir) {
        super("global");
        File builtinDir = FileUtil.canonical(new File(baseDir, "plugin"));
        File pluginDir = FileUtil.canonical(new File(localDir, "plugin"));
        initTmpDir(FileUtil.canonical(new File(localDir, "tmp")));
        set(CX_DIRECTORY, pluginDir);
        AppStorage storage = set(CX_STORAGE, new AppStorage());
        this.pluginManager = new PluginManager(builtinDir, pluginDir, storage);
        this.config = storage.load(PATH_CONFIG, Dict.class);
        if (this.config == null) {
            LOG.severe("failed to load application config");
        }
        this.version = new Dict();
        try {
            Manifest mf = ClasspathUtil.manifest(this.getClass());
            this.version.add("version", mf.getMainAttributes().getValue("Package-Version"));
            this.version.add("date", mf.getMainAttributes().getValue("Package-Date"));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "failed to load platform version info", e);
        }
        this.version.seal(true);
    }

    /**
     * Initializes a temporary directory.
     *
     * @param tmpDir         the directory to create and use
     */
    private void initTmpDir(File tmpDir) {
        LOG.fine("creating temporary directory: " + tmpDir);
        tmpDir.mkdir();
        tmpDir.deleteOnExit();
        try {
            FileUtil.deleteFiles(tmpDir);
        } catch (IOException e) {
            LOG.warning("failed to cleanup temporary directory: " + tmpDir +
                        ": " + e.getMessage());
        }
        FileUtil.setTempDir(tmpDir);
    }

    /**
     * Initializes this context by loading the plug-ins, procedures
     * and the environment configuration.
     */
    private void initAll() {
        Type.loader = this.pluginManager.classLoader;
        initPlugins();
        initScheduler();
        initCaches();
        START_TIME = new Date();
    }

    /**
     * Loads all plug-ins listed in an application specific plug-in
     * configuration file. Also loads any jar libraries found in the
     * plug-in "lib" directories.
     */
    private void initPlugins() {
        for (Object o : config.getArray("plugins")) {
            String pluginId = o.toString();
            try {
                pluginManager.load(pluginId);
            } catch (PluginException e) {
                LOG.warning("failed to load plugin " + pluginId + ": " +
                            e.getMessage());
            }
        }
    }

    /**
     * Initializes and starts the background task scheduler.
     */
    @SuppressWarnings("resource")
    private void initScheduler() {
        set(CX_SCHEDULER, Executors.newScheduledThreadPool(0, Thread.ofVirtual().factory()));
        scheduler().scheduleWithFixedDelay(
            () -> appStorage().cacheClean(false),
            ThreadLocalRandom.current().nextInt(CACHE_CLEAN_WAIT_SECS),
            CACHE_CLEAN_WAIT_SECS,
            TimeUnit.SECONDS
        );
        scheduler().scheduleWithFixedDelay(
            () -> Session.checkExpired(storage()),
            ThreadLocalRandom.current().nextInt(SESSION_CLEAN_WAIT_MINS),
            SESSION_CLEAN_WAIT_MINS,
            TimeUnit.MINUTES
        );
    }

    /**
     * Initializes cached objects.
     */
    @SuppressWarnings("resource")
    private void initCaches() {
        Vault.loadAll(storage());
        // FIXME: Why is pre-loading of all types necessary?
        Type.all(storage()).forEach(o -> { /* Force refresh cached types */ });
        Interceptor.init(storage());
        // FIXME: Remove singleton environment reference
        set(CX_ENVIRONMENT, Environment.all(storage()).findFirst().orElse(null));
        // FIXME: Remove role cache from SecurityContext
        try {
            SecurityContext.init(storage());
        } catch (StorageException e) {
            LOG.severe("Failed to load security config: " + e.getMessage());
        }
        Connection.metrics(storage()); // Load or create connection metrics
        Procedure.metrics(storage()); // Load or create procedure metrics
        User.metrics(storage()); // Load or create user metrics
        scheduler().submit(() -> Procedure.refreshAliases(storage())); // FIXME: Move aliases into storage catalog
    }

    /**
     * Destroys this context and frees all resources.
     */
    @SuppressWarnings("resource")
    private void destroyAll() {
        scheduler().shutdownNow();
        try {
            scheduler().awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.warning("timeout waiting for scheduled/background tasks to terminate");
        }
        remove(Context.CX_SCHEDULER);
        pluginManager.unloadAll();
        matchers = null;
    }

    /**
     * Resets this context and reloads all resources.
     */
    public void reset() {
        destroyAll();
        initAll();
    }

    /**
     * Returns the current web cache path. This is renewed each time the
     * application context is initialized or reset.
     *
     * @return the current web cache path
     */
    public String cachePath() {
        return "@" + Integer.toHexString(START_TIME.hashCode() & 0xffffffff);
    }

    /**
     * Returns the application configuration.
     *
     * @return the application configuration
     */
    public Dict getConfig() {
        return this.config;
    }

    /**
     * Returns the platform version information (in a sealed dictionary).
     *
     * @return the platform version information dictionary
     */
    public Dict version() {
        return this.version;
    }

    /**
     * Returns the application data storage. This is the global data
     * storage that contains all loaded plug-ins and maps requests to
     * them in order.
     *
     * @return the application data store
     *
     * @deprecated Use inherited Context.storage() or appStorage() instead.
     */
    @Deprecated(forRemoval = true)
    public Storage getStorage() {
        return storage();
    }

    /**
     * Returns the app (root) data store.
     *
     * @return the context data store
     */
    public AppStorage appStorage() {
        return get(CX_STORAGE, AppStorage.class);
    }

    /**
     * Returns the environment used.
     *
     * @return the environment used
     *
     * @deprecated Use inherited Context.environment() instead.
     */
    @Deprecated(forRemoval = true)
    public Environment getEnvironment() {
        return environment();
    }

    /**
     * Returns the array of cached web matchers (from the web services).
     * This list is only re-read when the context is reset.
     *
     * @return the array of cached web matchers
     *
     * @see #reset()
     */
    public WebMatcher[] getWebMatchers() {
        if (matchers == null) {
            matchers = WebService.matchers(storage()).toArray(WebMatcher[]::new);
        }
        return matchers;
    }

    /**
     * Returns the application base directory.
     *
     * @return the application base directory
     *
     * @deprecated Use inherited Context.baseDir() instead.
     */
    @Deprecated(forRemoval = true)
    public File getBaseDir() {
        return pluginManager.pluginDir;
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
    public boolean isPluginBuiltIn(String pluginId) {
        return pluginManager.isBuiltIn(pluginId);
    }

    /**
     * Installs a plug-in from the specified file. If an existing
     * plug-in with the same id already exists, it will be
     * replaced without warning. After installation, the new plug-in
     * will also be loaded and added to the default configuration
     * for automatic launch on the next restart.
     *
     * @param file           the plug-in ZIP file
     *
     * @return the unique plug-in id
     *
     * @throws PluginException if the plug-in couldn't be installed
     *             correctly
     */
    public String installPlugin(File file) throws PluginException {
        String pluginId = pluginManager.install(file);
        loadPlugin(pluginId);
        return pluginId;
    }

    /**
     * Uninstalls and removes a plug-in file. If the plug-in is
     * loaded or mounted, it will first be unloaded and the
     * associated storage will be destroyed.
     *
     * @param pluginId       the unique plug-in id
     *
     * @throws PluginException if the plug-in removal failed
     */
    public void uninstallPlugin(String pluginId) throws PluginException {
        unloadPlugin(pluginId);
        pluginManager.uninstall(pluginId);
    }

    /**
     * Loads a plug-in. If the plug-in was loaded successfully, it will
     * also be added to the default configuration for automatic
     * launch on the next restart.
     *
     * @param pluginId       the unique plug-in id
     *
     * @throws PluginException if no plug-in instance could be created
     *             or if the plug-in initialization failed
     */
    public void loadPlugin(String pluginId) throws PluginException {
        pluginManager.load(pluginId);
        Array pluginList = config.getArray("plugins");
        if (!pluginList.containsValue(pluginId)) {
            pluginList.add(pluginId);
            try {
                storage().store(PATH_CONFIG, config);
            } catch (StorageException e) {
                String msg = "failed to update application config: " +
                             e.getMessage();
                throw new PluginException(msg);
            }
            initCaches();
        }
    }

    /**
     * Unloads a plug-in. The plug-in will also be removed from the
     * default configuration for automatic launches.
     *
     * @param pluginId       the unique plug-in id
     *
     * @throws PluginException if the plug-in deinitialization failed
     */
    public void unloadPlugin(String pluginId) throws PluginException {
        pluginManager.unload(pluginId);
        initCaches();
        Array pluginList = config.getArray("plugins");
        pluginList.remove(pluginId);
        try {
            storage().store(PATH_CONFIG, config);
        } catch (StorageException e) {
            String msg = "failed to update application config: " + e.getMessage();
            throw new PluginException(msg);
        }
    }

}
