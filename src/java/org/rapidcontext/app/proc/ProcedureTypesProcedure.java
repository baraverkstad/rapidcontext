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
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Library;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.Type;

/**
 * The built-in procedure type list procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ProcedureTypesProcedure extends Procedure {
    // TODO: Remove this procedure when procedures are proper types

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public ProcedureTypesProcedure(String id, String type, Dict dict) {
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

        Storage storage = ApplicationContext.getInstance().getStorage();
        Dict res = new Dict();
        Type.all(storage).forEach(t -> {
            if (t.id().startsWith("procedure/")) {
                Dict dict = new Dict();
                dict.set("type", t.id());
                dict.set("bindings", t.serialize().getArray("binding"));
                res.set(t.id(), dict);
            }
        });
        for (String name : Library.getTypes()) {
            Bindings defs = Library.getDefaultBindings(name);
            if (defs != null) {
                Dict dict = new Dict();
                dict.set("type", name);
                dict.set("bindings", ProcedureReadProcedure.getBindingsData(defs));
                res.set(name, dict);
            }
        }
        return res;
    }
}
