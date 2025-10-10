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

package org.rapidcontext.app.proc;

import org.rapidcontext.app.plugin.PluginManager;
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
 * @author Per Cederberg
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
        cx.requireAccess("procedure/" + name, cx.readPermission(1));
        Storage storage = cx.storage();
        Path path = Path.resolve(Procedure.PATH, name);
        Metadata meta = storage.lookup(path);
        Object obj = storage.load(path);
        if (obj instanceof Procedure proc) {
            return getProcedureData(meta, proc.serialize(), null);
        } else if (obj instanceof Dict dict) {
            return getProcedureData(meta, dict, "procedure type not available");
        } else {
            return null;
        }
    }

    /**
     * Converts a procedure object into a data object.
     *
     * @param meta           the storage metadata
     * @param data           the storage data
     * @param error          the storage error, or null
     *
     * @return the data object created
     */
    static Object getProcedureData(Metadata meta, Dict data, String error) {
        Path localPath = Plugin.storagePath(PluginManager.LOCAL_PLUGIN);
        Dict res =
            new Dict()
            .setAll(data)
            .set("name", data.get("id"))
            .remove(KEY_BINDING)
            .set("bindings", data.getArray(KEY_BINDING))
            .set("local", meta.storages().containsValue(localPath));
        if (error != null) {
            res.set("_error", error);
        }
        return StorableObject.sterilize(res, true, false, true);
    }
}
