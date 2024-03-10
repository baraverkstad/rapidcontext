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

package org.rapidcontext.core.proc;

import java.util.HashMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.commons.lang3.ObjectUtils;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.StorageException;
import org.rapidcontext.core.type.Metrics;
import org.rapidcontext.core.type.Type;

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
    public static final Path PATH_PROC = org.rapidcontext.core.type.Procedure.PATH;

    /**
     * The data storage to use for loading and listing procedures.
     */
    private Storage storage = null;

    /**
     * The map of procedure name aliases. The map is indexed by the
     * old procedure name and points to the new one.
     *
     * @see #refreshAliases()
     */
    private HashMap<String,String> aliases = new HashMap<>();

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
     * The procedure call metrics.
     */
    private Metrics metrics = null;

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
     *
     * @deprecated Built-in procedures are managed as storable
     *     objects, so this method is no longer reliable.
     */
    @Deprecated(forRemoval=true)
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
        storage.query(PATH_PROC).paths().forEach(path -> set.add(path.toIdent(1)));
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
        if (builtIns.containsKey(name)) {
            return builtIns.get(name);
        }
        AddOnProcedure proc = cache.get(name);
        Metadata meta = storage.lookup(Path.resolve(PATH_PROC, name));
        if (meta == null && aliases.containsKey(name)) {
            return getProcedure(aliases.get(name));
        } else if (meta == null) {
            throw new ProcedureException("no procedure '" + name + "' found");
        }
        if (proc == null || ObjectUtils.compare(meta.modified(), proc.getLastModified()) > 0) {
            return loadProcedure(name);
        } else {
            return proc;
        }
    }

    /**
     * Loads built-in procedures via storage types. This method is safe to call
     * repeatedly (after each plug-in load).
     */
    public void refreshAliases() {
        // FIXME: This is very slow when a large number of procedures are available...
        aliases.clear();
        org.rapidcontext.core.type.Procedure.all(storage).forEach(p -> {
            if (p.alias() != null && !p.alias().isEmpty()) {
                aliases.put(p.alias(), p.id());
            }
        });
    }

    /**
     * Adds a new built-in procedure to the library.
     *
     * @param proc           the procedure definition
     *
     * @throws ProcedureException if an identically named procedure
     *             already exists
     *
     * @deprecated Built-in procedures should be initialized via the 'className'
     *     property on a proper stored (serialized) object.
     */
    @Deprecated(forRemoval=true)
    public void addBuiltIn(Procedure proc) throws ProcedureException {
        if (builtIns.containsKey(proc.getName())) {
            String msg = "a procedure '" + proc.getName() + "' already exists";
            throw new ProcedureException(msg);
        }
        LOG.warning("deprecated: procedure '" + proc.getName() + "' not declared via storage");
        builtIns.put(proc.getName(), proc);
    }

    /**
     * Removes a built-in procedure from the library.
     *
     * @param name           the procedure name
     *
     * @deprecated Built-in procedures should be managed as proper stored
     *     (serialized) objects.
     */
    @Deprecated(forRemoval=true)
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
    public Procedure loadProcedure(String name) throws ProcedureException {
        Object obj = storage.load(Path.resolve(PATH_PROC, name));
        if (obj instanceof Procedure p) {
            return p;
        } else {
            String msg = "no (valid) procedure '" + name + "' found";
            throw new ProcedureException(msg);
        }
    }

    /**
     * Stores a procedure to the data store. The procedure is created
     * from the specified data object, stored to the data store and
     * also placed in the library cache.
     *
     * @param id             the procedure name (object id)
     * @param type           the procedure type
     * @param data           the procedure data object
     *
     * @return the procedure stored
     *
     * @throws ProcedureException if the procedure couldn't be
     *             created or written to the data store
     */
    public Procedure storeProcedure(String id, String type, Dict data)
    throws ProcedureException {

        try {
            Dict dict = new Dict();
            dict.set(Type.KEY_ID, id);
            dict.set(Type.KEY_TYPE, type);
            dict.setAll(data);
            storage.store(Path.resolve(PATH_PROC, id + Storage.EXT_YAML), dict);
            return loadProcedure(id);
        } catch (StorageException e) {
            String msg = "failed to write procedure data: " + e.getMessage();
            throw new ProcedureException(msg);
        }
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
            storage.remove(Path.resolve(PATH_PROC, name));
        } catch (StorageException e) {
            String msg = "failed to remove procedure: " + e.getMessage();
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

    /**
     * Returns the procedure usage metrics.
     *
     * @return the procedure usage metrics
     */
    public Metrics getMetrics() {
        if (metrics == null) {
            synchronized (this) {
                try {
                    metrics = Metrics.findOrCreate(storage, "procedure");
                } catch (StorageException e) {
                    LOG.warning("failed to initialize procedure usage metrics: " + e);
                    return null;
                }
            }
        }
        return metrics;
    }

    /**
     * Reports procedure usage metrics for a single call.
     *
     * @param proc           the procedure executed
     * @param start          the start time (in millis)
     * @param success        the success flag
     * @param error          the optional error message
     */
    public void report(Procedure proc, long start, boolean success, String error) {
        getMetrics();
        if (metrics != null) {
            long now = System.currentTimeMillis();
            int duration = (int) (System.currentTimeMillis() - start);
            metrics.report(proc.getName(), now, 1, duration, success, error);
        }
    }
}
