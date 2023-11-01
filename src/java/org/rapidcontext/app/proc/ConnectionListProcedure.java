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

import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.Connection;
import org.rapidcontext.core.type.Plugin;
import org.rapidcontext.core.type.Procedure;

/**
 * The built-in connection list procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ConnectionListProcedure extends Procedure {

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public ConnectionListProcedure(String id, String type, Dict dict) {
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

        CallContext.checkSearchAccess(Connection.PATH.toString());
        Array res = new Array();
        Storage storage = cx.getStorage();
        storage.query(Connection.PATH)
            .filterAccess(cx.readPermission(1))
            .metadatas()
            .forEach(meta -> {
                Object o = storage.load(meta.path());
                Dict dict = null;
                if (o instanceof Connection) {
                    dict = (Dict) StorableObject.sterilize(o, true, false, true);
                } else if (o instanceof Dict) {
                    dict = (Dict) o;
                    if (!dict.containsKey("_error")) {
                        String msg = "failed to initialize: plug-in for " +
                                     "connnection type probably not loaded";
                        dict.add("_error", msg);
                    }
                }
                if (dict != null) {
                    dict.add("plugin", Plugin.source(meta));
                    res.add(dict);
                }
            });
        return res;
    }
}
