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

package org.rapidcontext.core.proc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.logging.Logger;

import org.rapidcontext.core.data.Data;
import org.rapidcontext.core.data.DataStore;
import org.rapidcontext.core.data.DataStoreException;

/**
 * A procedure library. This class contains the set of loaded and
 * built-in procedures. The library automatically loads and caches
 * procedures found in the data store. If the procedure content in
 * the data store is modified, the library cache must be cleared or
 * inconsistencies will result.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class Library {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(Library.class.getName());

    /**
     * The procedure data type used in the data store.
     */
    private static final String PROC_TYPE = "procedure";

    /**
     * The map of procedure type names and implementation classes.
     */
    private static HashMap types = new HashMap();

    /**
     * The data store to use for loading and listing procedures.
     */
    private DataStore store = null;

    /**
     * The map of built-in procedures. The map is indexed by the
     * procedure name and is populated manually by the
     * addBuiltIn() and removeBuiltIn() methods.
     *
     * @see #addBuiltIn(Procedure)
     * @see #removeBuiltIn(String)
     */
    private HashMap builtIns = new HashMap();

    /**
     * The map of cached procedures. The map is indexed by the
     * procedure name and is populated automatically from the data
     * store upon procedure requests.
     */
    private HashMap cache = new HashMap();

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
    public static void registerType(String type, Class cls)
        throws ProcedureException {

        String  msg;

        if (types.containsKey(type)) {
            msg = "procedure type " + type + " is already registered";
            LOG.warning(msg);
            throw new ProcedureException(msg);
        }
        types.put(type, cls);
        LOG.info("registered procedure type " + type +
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
        LOG.info("unregistered procedure type " + type);
    }

    /**
     * Returns an array with all the registered procedure type names.
     *
     * @return an array with all procedure type names
     */
    public static String[] getTypes() {
        String[]  res = new String[types.size()];

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
        AddOnProcedure  proc;

        try {
            proc = (AddOnProcedure) ((Class) types.get(type)).newInstance();
            return proc.getBindings();
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * Creates a new procedure library.
     *
     * @param store          the data store to use
     */
    public Library(DataStore store) {
        this.store = store;
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
     */
    public String[] getProcedureNames() {
        LinkedHashSet  set = new LinkedHashSet(builtIns.keySet());
        String[]       res;

        set.addAll(Arrays.asList(store.findDataIds(PROC_TYPE)));
        res = new String[set.size()];
        set.toArray(res);
        return res;
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
        AddOnProcedure  proc;
        long            modified;

        // TODO: remove this legacy conversion before 1.0
        if (name.startsWith("ReTracer.")) {
            name = "System" + name.substring(8);
        }
        if (builtIns.containsKey(name)) {
            return (Procedure) builtIns.get(name);
        }
        proc = (AddOnProcedure) cache.get(name);
        modified = store.findDataTimeStamp(PROC_TYPE, name);
        if (proc != null && modified > 0 && modified <= proc.getLastModified()) {
            return proc;
        } else if (store.hasData(PROC_TYPE, name)) {
            return loadProcedure(name);
        } else {
            throw new ProcedureException("no procedure '" + name +
                                         "' found");
        }
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
    }

    /**
     * Removes a built-in procedure from the library.
     *
     * @param name           the procedure name
     */
    public void removeBuiltIn(String name) {
        builtIns.remove(name);
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
    public Procedure loadProcedure(String name)
        throws ProcedureException {

        AddOnProcedure  proc;
        Data            data;
        String          msg;

        try {
            data = store.readData(PROC_TYPE, name);
            if (data == null) {
                msg = "no procedure '" + name + "' found";
                throw new ProcedureException(msg);
            }
        } catch (DataStoreException e) {
            msg = "failed to read procedure data: " + e.getMessage();
            throw new ProcedureException(msg);
        }
        proc = createProcedure(data);
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
    public Procedure storeProcedure(Data data) throws ProcedureException {
        AddOnProcedure  proc;
        String          msg;

        proc = createProcedure(data);
        try {
            store.writeData(PROC_TYPE, proc.getName(), proc.getData());
        } catch (DataStoreException e) {
            msg = "failed to write procedure data: " + e.getMessage();
            throw new ProcedureException(msg);
        }
        return proc;
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
    private AddOnProcedure createProcedure(Data data) throws ProcedureException {
        AddOnProcedure  proc;
        String          name;
        String          type;
        String          msg;
        Object          obj;

        name = data.getString("name", null);
        if (name == null) {
            msg = "failed to find required procedure property 'name'";
            throw new ProcedureException(msg);
        }
        type = data.getString("type", null);
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
            obj = ((Class) types.get(type)).newInstance();
        } catch (IllegalAccessException e) {
            msg = "failed to create procedure '" + name + "' as type '" +
                  type + "': illegal access to class or constructor";
            throw new ProcedureException(msg);
        } catch (Throwable e) {
            msg = "failed to create procedure '" + name + "' as type '" +
                  type + "': " + e.toString();
            throw new ProcedureException(msg);
        }
        try {
            proc = (AddOnProcedure) obj;
        } catch (ClassCastException e) {
            msg = "failed to create procedure '" + name + "' as type '" +
                  type + "': class doesn't subclass AddOnProcedure";
            throw new ProcedureException(msg);
        }
        proc.setData(data);
        return proc;
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
}
