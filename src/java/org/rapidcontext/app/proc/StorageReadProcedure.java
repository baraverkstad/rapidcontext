/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2026 Per Cederberg. All rights reserved.
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

import java.util.stream.Stream;

import org.rapidcontext.app.model.ApiUtil;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Query;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.Role;

/**
 * The built-in storage read procedure.
 *
 * @author Per Cederberg
 */
public class StorageReadProcedure extends Procedure {

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public StorageReadProcedure(String id, String type, Dict dict) {
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

        Dict opts = ApiUtil.options("path", bindings.getValue("path"));
        Path path = Path.from(opts.get("path", String.class, "/"));
        if (path.isIndex()) {
            cx.requireSearchAccess(path.toString());
        } else {
            cx.requireReadAccess(path.toString());
        }
        Query query = cx.storage().query(path).filterAccess(Role.PERM_READ);
        Stream<Object> stream = ApiUtil.load(query, opts);
        if (path.isIndex()) {
            return Array.from(stream);
        } else {
            return stream.findFirst().orElse(null);
        }
    }
}
