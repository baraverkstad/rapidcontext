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

package org.rapidcontext.app.proc;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.Procedure;

/**
 * The built-in storage delete procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StorageDeleteProcedure extends Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(StorageDeleteProcedure.class.getName());

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public StorageDeleteProcedure(String id, String type, Dict dict) {
        super(id, type, dict);
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
        return Boolean.valueOf(delete(Path.from(path)));
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
            LOG.fine("deleting storage path " + path);
            storage.remove(path);
            return true;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed to delete " + path, e);
            return false;
        }
    }
}
