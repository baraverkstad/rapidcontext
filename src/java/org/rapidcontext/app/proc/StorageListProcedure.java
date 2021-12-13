/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2019 Per Cederberg. All rights reserved.
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

package org.rapidcontext.app.proc;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;

/**
 * The built-in storage list procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StorageListProcedure implements Procedure {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.Storage.List";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new storage list procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public StorageListProcedure() throws ProcedureException {
        defaults.set("path", Bindings.ARGUMENT, "", "The object path to search");
        defaults.seal();
    }

    /**
     * Returns the procedure name.
     *
     * @return the procedure name
     */
    public String getName() {
        return NAME;
    }

    /**
     * Returns the procedure description.
     *
     * @return the procedure description
     */
    public String getDescription() {
        return "Reads all objects on a storage path.";
    }

    /**
     * Returns the bindings for this procedure. If this procedure
     * requires any special data, adapter connection or input
     * argument binding, those bindings should be set (but possibly
     * to null or blank values).
     *
     * @return the bindings for this procedure
     */
    public Bindings getBindings() {
        return defaults;
    }

    /**
     * Executes a call of this procedure in the specified context
     * and with the specified call bindings. The semantics of what
     * the procedure actually does, is up to each implementation.
     * Note that the call bindings are normally inherited from the
     * procedure bindings with arguments bound to their call values.
     *
     * @param cx             the procedure call context
     * @param bindings       the call bindings to use
     *
     * @return the result of the call, or
     *         null if the call produced no result
     *
     * @throws ProcedureException if the call execution caused an
     *             error
     */
    public Object call(CallContext cx, Bindings bindings)
        throws ProcedureException {

        String path = ((String) bindings.getValue("path", "")).trim();
        if (path.length() <= 0) {
            throw new ProcedureException("path cannot be empty");
        } else if (!path.endsWith("/")) {
            throw new ProcedureException("path must be an index");
        }
        CallContext.checkSearchAccess(path);
        Storage storage = ApplicationContext.getInstance().getStorage();
        Path searchPath = new Path(path);
        Metadata[] meta = storage.lookupAll(searchPath);
        Array res = new Array(meta.length);
        for (Metadata m : meta) {
            Path loadPath = m.path();
            if (SecurityContext.hasReadAccess(loadPath.toString())) {
                Object obj = storage.load(loadPath);
                Dict dict = StorageReadProcedure.serialize(loadPath, obj);
                if (dict != null) {
                    res.add(dict);
                }
            }
        }
        return res;
    }
}
