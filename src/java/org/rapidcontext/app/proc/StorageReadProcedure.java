/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2017 Per Cederberg. All rights reserved.
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
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;

/**
 * The built-in storage read procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StorageReadProcedure implements Procedure {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.Storage.Read";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new storage read procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public StorageReadProcedure() throws ProcedureException {
        defaults.set("path", Bindings.ARGUMENT, "", "The object path to read");
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
        return "Reads an object from storage.";
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
        } else if (path.endsWith("/")) {
            throw new ProcedureException("path cannot be an index");
        }
        CallContext.checkAccess(path, cx.readPermission(1));
        Storage storage = ApplicationContext.getInstance().getStorage();
        Path loadPath = new Path(path);
        return serialize(loadPath, storage.load(loadPath));
    }

    /**
     * Returns a serialized representation of a storage object.
     *
     * @param path           the storage object path
     * @param obj            the storage object
     *
     * @return the serialized representation of the object, or
     *         null if no suitable serialization existed
     */
    public static Dict serialize(Path path, Object obj) {
        if (obj instanceof Binary) {
            Binary data = (Binary) obj;
            Dict dict = new Dict();
            dict.set("type", "file");
            dict.set("name", path.name());
            dict.set("mimeType", data.mimeType());
            dict.set("size", new Long(data.size()));
            return dict;
        } else if (obj instanceof StorableObject) {
            // TODO: remove or obfuscate passwords?
            return ((StorableObject) obj).serialize();
        } else if (obj instanceof Dict) {
            return (Dict) obj;
        } else {
            return null;
        }
    }
}
