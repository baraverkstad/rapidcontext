/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2013 Per Cederberg. All rights reserved.
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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;

/**
 * The built-in storage delete procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StorageDeleteProcedure implements Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(StorageDeleteProcedure.class.getName());

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.Storage.Delete";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new storage delete procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public StorageDeleteProcedure() throws ProcedureException {
        defaults.set("path", Bindings.ARGUMENT, "", "The object path to remove");
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
        return "Deletes an object or a whole path from storage.";
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
        }
        CallContext.checkWriteAccess(path);
        return Boolean.valueOf(delete(new Path(path)));
    }

    /**
     * Deletes a storage object or path.
     *
     * @param path           the storage path to remove
     *
     * @return true if the data was successfully removed, or
     *         false otherwise
     */
    public static boolean delete(Path path) {
        Storage storage = ApplicationContext.getInstance().getStorage();
        try {
            storage.remove(path);
            return true;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed to delete " + path, e);
            return false;
        }
    }
}
