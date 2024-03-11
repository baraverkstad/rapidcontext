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

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.plugin.PluginManager;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.Plugin;
import org.rapidcontext.core.type.Procedure;

/**
 * The built-in procedure read procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ProcedureReadProcedure extends Procedure {

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public ProcedureReadProcedure(String id, String type, Dict dict) {
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
    @Override
    public Object call(CallContext cx, Bindings bindings)
        throws ProcedureException {

        String name = (String) bindings.getValue("name");
        CallContext.checkAccess("procedure/" + name, cx.readPermission(1));
        Procedure proc = cx.getLibrary().load(name);
        return getProcedureData(proc);
    }

    /**
     * Converts a procedure object into a data object.
     *
     * @param proc           the procedure
     *
     * @return the data object created
     *
     * @throws ProcedureException if the bindings data access
     *             failed
     */
    static Object getProcedureData(Procedure proc)
    throws ProcedureException {
        Storage storage = ApplicationContext.getInstance().getStorage();
        Path storagePath = Plugin.storagePath(PluginManager.LOCAL_PLUGIN);
        Path path = Path.resolve(Procedure.PATH, proc.id());
        Metadata meta = storage.lookup(Path.resolve(storagePath, path));
        Dict res = new Dict();
        res.setAll(proc.serialize());
        res.remove(KEY_BINDING);
        res.set("name", proc.id());
        res.set("description", proc.description());
        res.set("local", meta != null);
        res.set("bindings", getBindingsData(proc.getBindings()));
        return StorableObject.sterilize(res, true, true, true);
    }

    /**
     * Converts a procedure bindings object into a data object.
     *
     * @param bindings       the procedure bindings
     *
     * @return the bindings data array object
     *
     * @throws ProcedureException if the bindings data access
     *             failed
     */
    static Array getBindingsData(Bindings bindings) throws ProcedureException {
        String[] names = bindings.getNames();
        Array res = new Array(names.length);
        for (String s : names) {
            res.add(
                new Dict()
                .set("name", s)
                .set("type", bindings.getTypeName(s))
                .set("value", bindings.getValue(s, ""))
                .set("description", bindings.getDescription(s))
            );
        }
        return res;
    }
}
