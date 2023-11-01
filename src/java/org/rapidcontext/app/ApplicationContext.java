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

package org.rapidcontext.app;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.rapidcontext.app.model.AppStorage;
import org.rapidcontext.app.plugin.PluginException;
import org.rapidcontext.app.plugin.PluginManager;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.js.JsCompileInterceptor;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Interceptor;
import org.rapidcontext.core.proc.Library;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.StorageException;
import org.rapidcontext.core.type.Environment;
import org.rapidcontext.core.type.Session;
import org.rapidcontext.core.type.WebMatcher;
import org.rapidcontext.core.type.WebService;
import org.rapidcontext.util.FileUtil;

/**
 * The application context. This is a singleton object that contains
 * references to global application settings and objects. It also
 * provides simple procedure execution and resource and plug-in
 * initialization and deinitialization.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ApplicationContext {

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
     * The path to the platform information.
     */
    public static final Path PATH_PLATFORM = Path.from("/platform");

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
    private static final int CACHE_CLEAN_WAIT_SECS = 30;

    /**
     * The minutes to wait between runs of the expired session cleaner job.
     */
    private static final int SESSION_CLEAN_WAIT_MINS = 60;

    /**
     * The singleton application context instance.
     */
    private static ApplicationContext instance = null;

    /**
     * The thread factory for background (scheduled) jobs.
     */
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {

        /**
         * The thread group to use.
         */
        private ThreadGroup group = new ThreadGroup("background");

        /**
         * The count of created threads.
         */
        private int count = 0;

        @Override
        public synchronized Thread newThread(Runnable r) {
            return new Thread(group, r, "background-" + count++);
        }
    };

    /**
     * The application root storage.
     */
    private AppStorage storage;

    /**
     * The plug-in manager.
     */
    private PluginManager pluginManager;

    /**
     * The background task scheduler.
     */
    private ScheduledThreadPoolExecutor scheduler = null;

    /**
     * The application configuration.
     */
    private Dict config;

    /**
     * The active environment.
     */
    private Environment env = null;

    /**
     * The cached list of web matchers (from the web services).
     */
    private WebMatcher[] matchers = null;

    /**
     * The procedure library.
     */
    private Library library = null;

    /**
     * The thread call context map.
     */
    private Map<Thread, CallContext> threadContext =
        Collections.synchronizedMap(new HashMap<>());

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
        if (instance == null) {
            instance = new ApplicationContext(baseDir, localDir);
        }
        if (start) {
            instance.initAll();
        }
        return instance;
    }

    /**
     * Destroys the application context and frees all resources used.
     */
    protected static synchronized void destroy() {
        if (instance != null) {
            instance.destroyAll();
            instance = null;
        }
    }

    /**
     * Returns the singleton application context instance.
     *
     * @return the singleton application context instance
     */
    public static ApplicationContext getInstance() {
        return instance;
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
        File builtinDir = FileUtil.canonical(new File(baseDir, "plugin"));
        File pluginDir = FileUtil.canonical(new File(localDir, "plugin"));
        initTmpDir(FileUtil.canonical(new File(localDir, "tmp")));
        this.storage = new AppStorage();
        this.pluginManager = new PluginManager(builtinDir, pluginDir, this.storage);
        this.library = new Library(this.storage);
        this.config = this.storage.load(PATH_CONFIG, Dict.class);
        if (this.config == null) {
            LOG.severe("failed to load application config");
        } else if (!this.config.containsKey("guid")) {
            this.config.set("guid", UUID.randomUUID().toString());
            try {
                this.storage.store(PATH_CONFIG, this.config);
            } catch (Exception e) {
                LOG.severe("failed to update application config with GUID");
            }
        }
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
        initLibrary();
        initPlugins();
        initScheduler();
        // TODO: Move aliases into storage catalog
        scheduler.submit(() -> library.refreshAliases());
        // TODO: Remove singleton environment reference
        env = Environment.all(storage).findFirst().orElse(null);
        try {
            SecurityContext.init(storage);
        } catch (StorageException e) {
            LOG.severe("Failed to load security config: " + e.getMessage());
        }
        START_TIME = new Date();
    }

    /**
     * Initializes the library in this context.
     */
    private void initLibrary() {
        // Add default interceptors
        Interceptor i = library.getInterceptor();
        library.setInterceptor(new JsCompileInterceptor(i));
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
                loadPlugin(pluginId);
            } catch (PluginException e) {
                LOG.warning("failed to load plugin " + pluginId + ": " +
                            e.getMessage());
            }
        }
    }

    /**
     * Initializes and starts the background task scheduler.
     */
    private void initScheduler() {
        scheduler = new ScheduledThreadPoolExecutor(1, THREAD_FACTORY);
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        scheduler.scheduleWithFixedDelay(
                () -> storage.cacheClean(false),
                ThreadLocalRandom.current().nextInt(CACHE_CLEAN_WAIT_SECS),
                CACHE_CLEAN_WAIT_SECS,
                TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(
                () -> Session.removeExpired(this.getStorage()),
                ThreadLocalRandom.current().nextInt(SESSION_CLEAN_WAIT_MINS),
                SESSION_CLEAN_WAIT_MINS,
                TimeUnit.MINUTES);
    }

    /**
     * Destroys this context and frees all resources.
     */
    private void destroyAll() {
        scheduler.shutdownNow();
        scheduler = null;
        pluginManager.unloadAll();
        library = new Library(this.storage);
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
     * Returns the application configuration.
     *
     * @return the application configuration
     */
    public Dict getConfig() {
        return this.config;
    }

    /**
     * Returns the application data storage. This is the global data
     * storage that contains all loaded plug-ins and maps requests to
     * them in order.
     *
     * @return the application data store
     */
    public Storage getStorage() {
        return this.storage;
    }

    /**
     * Returns the environment used.
     *
     * @return the environment used
     */
    public Environment getEnvironment() {
        return this.env;
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
            matchers = WebService.matchers(storage).toArray(WebMatcher[]::new);
        }
        return matchers;
    }

    /**
     * Returns the procedure library used.
     *
     * @return the procedure library used
     */
    public Library getLibrary() {
        return library;
    }

    /**
     * Returns the application base directory.
     *
     * @return the application base directory
     */
    public File getBaseDir() {
        return pluginManager.pluginDir;
    }

    /**
     * Returns the plug-in class loader.
     *
     * @return the plug-in class loader
     */
    public ClassLoader getClassLoader() {
        return pluginManager.classLoader;
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
                storage.store(PATH_CONFIG, config);
            } catch (StorageException e) {
                String msg = "failed to update application config: " +
                             e.getMessage();
                throw new PluginException(msg);
            }
            library.refreshAliases();
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
        library.clearCache();
        library.refreshAliases();
        Array pluginList = config.getArray("plugins");
        pluginList.remove(pluginId);
        try {
            storage.store(PATH_CONFIG, config);
        } catch (StorageException e) {
            String msg = "failed to update application config: " + e.getMessage();
            throw new PluginException(msg);
        }
    }

    /**
     * Executes a procedure within this context.
     *
     * @param name           the procedure name
     * @param args           the procedure arguments
     * @param source         the call source information
     * @param trace          the trace buffer or null for none
     *
     * @return the result of the call, or
     *         null if the call produced no result
     *
     * @throws ProcedureException if the procedure execution failed
     */
    public Object execute(String name,
                          Object[] args,
                          String source,
                          StringBuilder trace)
        throws ProcedureException {

        CallContext cx = new CallContext(storage, env, library);
        threadContext.put(Thread.currentThread(), cx);
        cx.setAttribute(CallContext.ATTRIBUTE_USER,
                        SecurityContext.currentUser());
        cx.setAttribute(CallContext.ATTRIBUTE_SOURCE, source);
        if (trace != null) {
            cx.setAttribute(CallContext.ATTRIBUTE_TRACE, Boolean.TRUE);
            cx.setAttribute(CallContext.ATTRIBUTE_LOG_BUFFER, trace);
        }
        try {
            return cx.execute(name, args);
        } finally {
            threadContext.remove(Thread.currentThread());
        }
    }

    /**
     * Executes a procedure asynchronously within this context. This
     * method will sleep for 10 minutes after terminating the
     * procedure execution, allowing the results to be fetched from
     * the context by another thread.
     *
     * @param name           the procedure name
     * @param args           the procedure arguments
     * @param source         the call source information
     */
    public void executeAsync(String name, Object[] args, String source) {
        CallContext cx = new CallContext(storage, env, library);
        threadContext.put(Thread.currentThread(), cx);
        cx.setAttribute(CallContext.ATTRIBUTE_USER,
                        SecurityContext.currentUser());
        cx.setAttribute(CallContext.ATTRIBUTE_SOURCE, source);
        try {
            Object res = cx.execute(name, args);
            cx.setAttribute(CallContext.ATTRIBUTE_RESULT, res);
        } catch (Exception e) {
            cx.setAttribute(CallContext.ATTRIBUTE_ERROR, e.getMessage());
        } finally {
            // Delay thread context removal for 10 minutes
            try {
                Thread.sleep(600000);
            } catch (InterruptedException ignore) {
                // Allow thread interrupt to remove context
            }
            threadContext.remove(Thread.currentThread());
        }
    }

    /**
     * Finds the currently active call context for a thread.
     *
     * @param thread         the thread to search for
     *
     * @return the call context found, or
     *         null if no context was active
     */
    public CallContext findContext(Thread thread) {
        return threadContext.get(thread);
    }

    /**
     * Finds the currently active call context for a thread id. The
     * thread id is identical to the hash code for the thread.
     *
     * @param threadId       the thread id to search for
     *
     * @return the call context found, or
     *         null if no context was active
     */
    public CallContext findContext(int threadId) {
        synchronized (threadContext) {
            for (Thread thread : threadContext.keySet()) {
                if (thread.hashCode() == threadId) {
                    return threadContext.get(thread);
                }
            }
        }
        return null;
    }
}
