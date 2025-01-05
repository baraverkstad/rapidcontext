/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2025 Per Cederberg. All rights reserved.
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

import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.Procedure;

/**
 * A procedure library. The library handles procedure aliases, call
 * interceptors and other functions for all procedures.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Library {

    /**
     * The data storage to use for loading and listing procedures.
     */
    private Storage storage = null;

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
     * Creates a new procedure library.
     *
     * @param storage        the data storage to use
     */
    public Library(Storage storage) {
        this.storage = storage;
    }

    /**
     * Locates a procedure using either its identifier or an alias.
     *
     * @param id             the procedure identifier
     *
     * @return the procedure object
     *
     * @throws ProcedureException if the procedure couldn't be found,
     *             or failed to load correctly
     *
     * @deprecated Use Procedure.find(storage,id) directly instead.
     */
    @Deprecated(forRemoval = true)
    public Procedure load(String id) throws ProcedureException {
        Procedure proc = Procedure.find(storage, id);
        if (proc != null) {
            return proc;
        } else {
            throw new ProcedureException("no procedure '" + id + "' found");
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
     *
     * @deprecated Tracing per-procedure will be removed in a future release. Use per-call tracing instead.
     */
    @Deprecated(forRemoval = true)
    public boolean isTracing(String name) {
        return traces.containsKey(name);
    }

    /**
     * Sets or clears the call tracing for a procedure.
     *
     * @param name           the name of the procedure
     * @param enabled        true to enabled tracing,
     *                       false to disable
     *
     * @deprecated Tracing per-procedure will be removed in a future release. Use per-call tracing instead.
     */
    @Deprecated(forRemoval = true)
    public void setTracing(String name, boolean enabled) {
        if (enabled) {
            traces.put(name, Boolean.TRUE);
        } else {
            traces.remove(name);
        }
    }
}
