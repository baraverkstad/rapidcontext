/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg & Dynabyte AB.
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

package org.rapidcontext.app;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.rapidcontext.app.plugin.PluginException;
import org.rapidcontext.app.plugin.PluginManager;
import org.rapidcontext.app.proc.AppListProcedure;
import org.rapidcontext.app.proc.PluginInstallProcedure;
import org.rapidcontext.app.proc.PluginListProcedure;
import org.rapidcontext.app.proc.PluginLoadProcedure;
import org.rapidcontext.app.proc.PluginUnloadProcedure;
import org.rapidcontext.app.proc.ProcedureListProcedure;
import org.rapidcontext.app.proc.ProcedureReadProcedure;
import org.rapidcontext.app.proc.ProcedureTypesProcedure;
import org.rapidcontext.app.proc.ProcedureWriteProcedure;
import org.rapidcontext.app.proc.ResetProcedure;
import org.rapidcontext.app.proc.RoleChangeProcedure;
import org.rapidcontext.app.proc.RoleListProcedure;
import org.rapidcontext.app.proc.SessionCurrentProcedure;
import org.rapidcontext.app.proc.SessionListProcedure;
import org.rapidcontext.app.proc.SessionTerminateProcedure;
import org.rapidcontext.app.proc.StatusProcedure;
import org.rapidcontext.app.proc.ThreadContextProcedure;
import org.rapidcontext.app.proc.ThreadCreateProcedure;
import org.rapidcontext.app.proc.ThreadInterruptProcedure;
import org.rapidcontext.app.proc.ThreadListProcedure;
import org.rapidcontext.app.proc.UserChangeProcedure;
import org.rapidcontext.app.proc.UserCheckAccessProcedure;
import org.rapidcontext.app.proc.UserListProcedure;
import org.rapidcontext.app.proc.UserPasswordChangeProcedure;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.js.JsCompileInterceptor;
import org.rapidcontext.core.js.JsProcedure;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Interceptor;
import org.rapidcontext.core.proc.Library;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.security.SecurityInterceptor;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.StorageException;
import org.rapidcontext.core.storage.RootStorage;
import org.rapidcontext.core.type.Environment;
import org.rapidcontext.core.type.Type;

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
    public static final Path PATH_CONFIG = new Path("/config");

    /**
     * The singleton application context instance.
     */
    private static ApplicationContext instance = null;

    /**
     * The application root storage.
     */
    private RootStorage storage;

    /**
     * The plug-in manager.
     */
    private PluginManager pluginManager;

    /**
     * The application configuration.
     */
    private Dict config;

    /**
     * The active environment.
     */
    private Environment env = null;

    /**
     * The procedure library.
     */
    private Library library = null;

    /**
     * The thread call context map.
     */
    private Map threadContext = Collections.synchronizedMap(new HashMap());

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
        File builtinDir = new File(baseDir, "plugin");
        File pluginDir = new File(localDir, "plugin");
        this.storage = new RootStorage(true);
        this.pluginManager = new PluginManager(builtinDir, pluginDir, storage);
        this.library = new Library(this.storage);
        this.config = (Dict) storage.load(PATH_CONFIG);
        if (this.config == null) {
            LOG.severe("failed to load application config");
        }
    }

    /**
     * Initializes this context by loading the plug-ins, procedures
     * and the environment configuration.
     */
    private void initAll() {
        Type.initAll(storage);
        initLibrary();
        initPlugins();
        // TODO: Remove singleton environment reference
        Environment[] envs = Environment.findAll(storage);
        env = (envs.length > 0) ? envs[0] : null;
        try {
            SecurityContext.init(storage);
        } catch (StorageException e) {
            LOG.severe("Failed to load security config: " + e.getMessage());
        }
    }

    /**
     * Initializes the library in this context.
     */
    private void initLibrary() {
        Interceptor  i;

        // Register default procedure types
        try {
            Library.registerType("javascript", JsProcedure.class);
        } catch (ProcedureException e) {
            LOG.severe("failed to register javascript procedure type: " +
                       e.getMessage());
        }

        // Add default interceptors
        i = library.getInterceptor();
        i = new JsCompileInterceptor(i);
        i = new SecurityInterceptor(i);
        library.setInterceptor(i);

        // Add default built-in procedures
        try {
            library.addBuiltIn(new AppListProcedure());
            library.addBuiltIn(new PluginInstallProcedure());
            library.addBuiltIn(new PluginListProcedure());
            library.addBuiltIn(new PluginLoadProcedure());
            library.addBuiltIn(new PluginUnloadProcedure());
            library.addBuiltIn(new ProcedureListProcedure());
            library.addBuiltIn(new ProcedureReadProcedure());
            library.addBuiltIn(new ProcedureTypesProcedure());
            library.addBuiltIn(new ProcedureWriteProcedure());
            library.addBuiltIn(new ResetProcedure());
            library.addBuiltIn(new RoleChangeProcedure());
            library.addBuiltIn(new RoleListProcedure());
            library.addBuiltIn(new SessionCurrentProcedure());
            library.addBuiltIn(new SessionListProcedure());
            library.addBuiltIn(new SessionTerminateProcedure());
            library.addBuiltIn(new StatusProcedure());
            library.addBuiltIn(new ThreadContextProcedure());
            library.addBuiltIn(new ThreadCreateProcedure());
            library.addBuiltIn(new ThreadInterruptProcedure());
            library.addBuiltIn(new ThreadListProcedure());
            library.addBuiltIn(new UserChangeProcedure());
            library.addBuiltIn(new UserCheckAccessProcedure());
            library.addBuiltIn(new UserListProcedure());
            library.addBuiltIn(new UserPasswordChangeProcedure());
        } catch (ProcedureException e) {
            LOG.severe("failed to create built-in procedures: " +
                       e.getMessage());
        }
    }

    /**
     * Loads all plug-ins listed in an application specific plug-in
     * configuration file. Also loads any jar libraries found in the
     * plug-in "lib" directories.
     */
    private void initPlugins() {
        Array   list;
        String  pluginId;

        list = config.getArray("plugins");
        for (int i = 0; i < list.size(); i++) {
            try {
                pluginId = list.getString(i, null);
                loadPlugin(pluginId);
            } catch (PluginException e) {
                LOG.warning("failed to load plugin " +
                            list.getString(i, null) + ": " +
                            e.getMessage());
            }
        }
    }

    /**
     * Destroys this context and frees all resources.
     */
    private void destroyAll() {
        pluginManager.unloadAll();
        Type.destroyAll(storage);
        Library.unregisterType("javascript");
        library = new Library(this.storage);
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
     * Checks if the specified plug-in is currently loaded.
     *
     * @param pluginId       the unique plug-in id
     *
     * @return true if the plug-in was loaded, or
     *         false otherwise
     */
    public boolean isPluginLoaded(String pluginId) {
        return pluginManager.isLoaded(pluginId);
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
        Array   pluginList;
        String  msg;

        pluginManager.load(pluginId);
        pluginList = config.getArray("plugins");
        if (!pluginList.containsValue(pluginId)) {
            pluginList.add(pluginId);
            try {
                storage.store(PATH_CONFIG, config);
            } catch (StorageException e) {
                msg = "failed to update application config: " +
                      e.getMessage();
                throw new PluginException(msg);
            }
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
        Array   pluginList;
        String  msg;

        pluginManager.unload(pluginId);
        library.clearCache();
        pluginList = config.getArray("plugins");
        pluginList.remove(pluginList.indexOf(pluginId));
        try {
            storage.store(PATH_CONFIG, config);
        } catch (StorageException e) {
            msg = "failed to update application config: " + e.getMessage();
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
                          StringBuffer trace)
        throws ProcedureException {

        CallContext  cx = new CallContext(storage, env, library);

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
        CallContext  cx = new CallContext(storage, env, library);
        Object       res;

        threadContext.put(Thread.currentThread(), cx);
        cx.setAttribute(CallContext.ATTRIBUTE_USER,
                        SecurityContext.currentUser());
        cx.setAttribute(CallContext.ATTRIBUTE_SOURCE, source);
        try {
            res = cx.execute(name, args);
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
        return (CallContext) threadContext.get(thread);
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
        Iterator   iter;
        Object     obj;

        synchronized (threadContext) {
            iter = threadContext.keySet().iterator();
            while (iter.hasNext()) {
                obj = iter.next();
                if (obj.hashCode() == threadId) {
                    return (CallContext) threadContext.get(obj);
                }
            }
        }
        return null;
    }
}
