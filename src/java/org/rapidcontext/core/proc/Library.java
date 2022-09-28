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

package org.rapidcontext.core.proc;

import java.util.HashMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.rapidcontext.app.plugin.PluginManager;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.StorageException;

/**
 * A procedure library. This class contains the set of loaded and
 * built-in procedures. The library automatically loads and caches
 * procedures found in the data store. If the procedure content in
 * the data store is modified, the library cache must be cleared or
 * inconsistencies will result.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Library {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(Library.class.getName());

    /**
     * The procedure object storage path.
     */
    public static final Path PATH_PROC = Path.from("/procedure/");

    /**
     * The map of procedure type names and implementation classes.
     */
    private static HashMap<String,Class<?>> types = new HashMap<>();

    /**
     * The identifier of the currently loading plug-in.
     */
    public static String builtInPlugin = null;

    /**
     * The data storage to use for loading and listing procedures.
     */
    private Storage storage = null;

    /**
     * The map of built-in procedures. The map is indexed by the
     * procedure name and is populated manually by the
     * addBuiltIn() and removeBuiltIn() methods.
     *
     * @see #addBuiltIn(Procedure)
     * @see #removeBuiltIn(String)
     */
    private HashMap<String,Procedure> builtIns = new HashMap<>();

    /**
     * The map of built-in procedure plugin identifiers. The map is
     * indexed by the procedure name and contains the plug-in
     * identifier for the procedure.
     */
    private HashMap<String,String> builtInPlugins = new HashMap<>();

    /**
     * The map of cached procedures. The map is indexed by the
     * procedure name and is populated automatically from the data
     * store upon procedure requests.
     */
    private HashMap<String,AddOnProcedure> cache = new HashMap<>();

    /**
     * The map of active procedure traces. The map is indexed by the
     * procedure name and an entry is only added if all calls to the
     * procedure should be traced (which affects performance
     * slightly).
     */
    private HashMap<String,Boolean> traces = new HashMap<>();

    /**
     * The procedure call interceptor.
     */
    private Interceptor interceptor = new DefaultInterceptor();

    /**
     * Registers a procedure type name. All add-on procedures should
     * register their unique type names with this method. Normally
     * each procedure class has one type name, but alias names are
     * permitted.
     *
     * @param type           the unique procedure type name
     * @param cls            the procedure implementation class
     *
     * @throws ProcedureException if the procedure type was already
     *             registered
     *
     * @see #unregisterType(String)
     */
    public static void registerType(String type, Class<?> cls)
    throws ProcedureException {

        if (types.containsKey(type)) {
            String msg = "procedure type " + type + " is already registered";
            LOG.warning(msg);
            throw new ProcedureException(msg);
        }
        types.put(type, cls);
        LOG.fine("registered procedure type " + type +
                 " with class " + cls.getName());
    }

    /**
     * Unregisters a procedure type name. All add-on procedures that
     * are created via plug-ins should call this method when the
     * plug-in unloads.
     *
     * @param type           the unique procedure type name
     *
     * @see #registerType(String, Class)
     */
    public static void unregisterType(String type) {
        types.remove(type);
        LOG.fine("unregistered procedure type " + type);
    }

    /**
     * Returns an array with all the registered procedure type names.
     *
     * @return an array with all procedure type names
     */
    public static String[] getTypes() {
        String[] res = new String[types.size()];
        types.keySet().toArray(res);
        return res;
    }

    /**
     * Returns the default bindings for a registered procedure type.
     * This function will instantiate a new empty procedure of the
     * specified type and return the bindings thus created.
     *
     * @param type           the unique procedure type name
     *
     * @return the default bindings for the procedure type, or
     *         null if the procedure creation failed
     */
    public static Bindings getDefaultBindings(String type) {
        try {
            AddOnProcedure proc = (AddOnProcedure) types.get(type).getDeclaredConstructor().newInstance();
            return proc.getBindings();
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * Creates a new procedure library.
     *
     * @param storage        the data storage to use
     */
    public Library(Storage storage) {
        this.storage = storage;
    }

    /**
     * Checks if the specified procedure name is a registered
     * built-in procedure.
     *
     * @param name           the procedure name
     *
     * @return true if the procedure is a built-in procedure, or
     *         false otherwise
     */
    public boolean hasBuiltIn(String name) {
        return builtIns.containsKey(name);
    }

    /**
     * Returns an array with the names of all loaded procedures.
     *
     * @return an array with the names of all loaded procedures
     *
     * @throws ProcedureException if the procedures couldn't be listed
     */
    public String[] getProcedureNames() throws ProcedureException {
        TreeSet<String> set = new TreeSet<>(builtIns.keySet());
        storage.query(PATH_PROC).paths().forEach(path -> set.add(path.name()));
        return set.toArray(new String[set.size()]);
    }

    /**
     * Returns a loaded procedure.
     *
     * @param name           the procedure name
     *
     * @return the procedure object
     *
     * @throws ProcedureException if the procedure couldn't be found,
     *             or failed to load correctly
     */
    public Procedure getProcedure(String name) throws ProcedureException {
        // TODO: remove this legacy conversion before 1.0
        if (name.startsWith("ReTracer.")) {
            name = "System" + name.substring(8);
        }
        if (builtIns.containsKey(name)) {
            return builtIns.get(name);
        }
        AddOnProcedure proc = cache.get(name);
        Metadata meta = storage.lookup(PATH_PROC.child(name, false));
        if (meta == null) {
            throw new ProcedureException("no procedure '" + name + "' found");
        }
        if (proc == null || meta.lastModified().after(proc.getLastModified())) {
            return loadProcedure(name);
        } else {
            return proc;
        }
    }

    /**
     * Returns the plug-in identifier of the specified procedure.
     *
     * @param name           the procedure name
     *
     * @return the plug-in identifier, or
     *         null if not found
     */
    public String getProcedurePluginId(String name) {
        Metadata meta = storage.lookup(PATH_PROC.child(name, false));
        if (builtInPlugins.containsKey(name)) {
            return builtInPlugins.get(name);
        } else if (meta != null) {
            return PluginManager.pluginId(meta);
        }
        return null;
    }

    /**
     * Adds a new built-in procedure to the library.
     *
     * @param proc           the procedure definition
     *
     * @throws ProcedureException if an identically named procedure
     *             already exists
     */
    public void addBuiltIn(Procedure proc) throws ProcedureException {
        if (builtIns.containsKey(proc.getName())) {
            throw new ProcedureException("a procedure with name '" +
                                         proc.getName() +
                                         "' already exists");
        }
        builtIns.put(proc.getName(), proc);
        builtInPlugins.put(proc.getName(), builtInPlugin);
    }

    /**
     * Removes a built-in procedure from the library.
     *
     * @param name           the procedure name
     */
    public void removeBuiltIn(String name) {
        builtIns.remove(name);
        builtInPlugins.remove(name);
    }

    /**
     * Clears the cache of loaded procedures from the library. The
     * procedure cache is continuously built up each time a procedure
     * is accessed.
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Loads (or reloads) a procedure from the data store to the cache.
     *
     * @param name           the procedure name
     *
     * @return the procedure loaded
     *
     * @throws ProcedureException if the procedure couldn't be loaded
     */
    public Procedure loadProcedure(String name) throws ProcedureException {
        Dict data = (Dict) storage.load(PATH_PROC.child(name, false));
        if (data == null) {
            String msg = "no procedure '" + name + "' found";
            throw new ProcedureException(msg);
        }
        AddOnProcedure proc = createProcedure(data);
        cache.put(proc.getName(), proc);
        return proc;
    }

    /**
     * Stores a procedure to the data store. The procedure is created
     * from the specified data object, stored to the data store and
     * also placed in the library cache.
     *
     * @param data           the procedure data object
     *
     * @return the procedure stored
     *
     * @throws ProcedureException if the procedure couldn't be
     *             created or written to the data store
     */
    public Procedure storeProcedure(Dict data) throws ProcedureException {
        AddOnProcedure proc = createProcedure(data);
        try {
            cache.remove(proc.getName());
            storage.store(PATH_PROC.child(proc.getName(), false), proc.getData());
        } catch (StorageException e) {
            String msg = "failed to write procedure data: " + e.getMessage();
            throw new ProcedureException(msg);
        }
        return proc;
    }

    /**
     * Removes a procedure from the storage (if possible). Normally,
     * only procedures in the local plug-in can be removed this way.
     * Built-in procedures will remain unaffected by this.
     *
     * @param name           the name of the procedure
     *
     * @throws ProcedureException if an error occurred while removing
     *             the procedure from storage
     */
    public void deleteProcedure(String name) throws ProcedureException {
        try {
            cache.remove(name);
            storage.remove(PATH_PROC.child(name, false));
        } catch (StorageException e) {
            String msg = "failed to remove procedure: " + e.getMessage();
            throw new ProcedureException(msg);
        }
    }

    /**
     * Creates a new add-on procedure from the specified data object.
     *
     * @param data           the procedure data object
     *
     * @return the add-on procedure created
     *
     * @throws ProcedureException if the procedure couldn't be
     *             created due to errors in the data object
     */
    private AddOnProcedure createProcedure(Dict data) throws ProcedureException {
        String msg;
        String name = data.getString("name", null);
        if (name == null) {
            msg = "failed to find required procedure property 'name'";
            throw new ProcedureException(msg);
        }
        String type = data.getString("type", null);
        if (type == null) {
            msg = "failed to create procedure '" + name + "': " +
                  "missing required procedure property 'type'";
            throw new ProcedureException(msg);
        } else if (types.get(type) == null) {
            msg = "failed to create procedure '" + name + "': " +
                  "procedure type '" + type + "' is undefined";
            throw new ProcedureException(msg);
        }
        try {
            Object obj = types.get(type).getDeclaredConstructor().newInstance();
            AddOnProcedure proc = (AddOnProcedure) obj;
            proc.setData(data);
            return proc;
        } catch (IllegalAccessException e) {
            msg = "failed to create procedure '" + name + "' as type '" +
                  type + "': illegal access to class or constructor";
            throw new ProcedureException(msg);
        } catch (ClassCastException e) {
            msg = "failed to create procedure '" + name + "' as type '" +
                  type + "': class doesn't subclass AddOnProcedure";
            throw new ProcedureException(msg);
        } catch (Throwable e) {
            msg = "failed to create procedure '" + name + "' as type '" +
                  type + "': " + e.toString();
            throw new ProcedureException(msg);
        }
    }

    /**
     * Returns the procedure call interceptor.
     *
     * @return the procedure call interceptor
     */
    public Interceptor getInterceptor() {
        return interceptor;
    }

    /**
     * Sets the procedure call interceptor, overriding the default.
     *
     * @param i              the procedure call interceptor to use
     */
    public void setInterceptor(Interceptor i) {
        if (i == null) {
            this.interceptor = new DefaultInterceptor();
        } else {
            this.interceptor = i;
        }
    }

    /**
     * Checks if all calls to a procedure should be traced.
     *
     * @param name           the name of the procedure
     *
     * @return true if all calls should be traced, or
     *         false otherwise
     */
    public boolean isTracing(String name) {
        return traces.containsKey(name);
    }

    /**
     * Sets or clears the call tracing for a procedure.
     *
     * @param name           the name of the procedure
     * @param enabled        true to enabled tracing,
     *                       false to disable
     */
    public void setTracing(String name, boolean enabled) {
        if (enabled) {
            traces.put(name, Boolean.TRUE);
        } else {
            traces.remove(name);
        }
    }
}
