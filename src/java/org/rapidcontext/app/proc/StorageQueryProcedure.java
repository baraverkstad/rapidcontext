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

import java.util.stream.Stream;

import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;

/**
 * The built-in storage query procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StorageQueryProcedure extends StorageProcedure {

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public StorageQueryProcedure(String id, String type, Dict dict) {
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

        Dict opts = options("path", bindings.getValue("path"));
        Path path = Path.from(opts.getString("path", "/"));
        if (path.isIndex()) {
            CallContext.checkSearchAccess(path.toString());
        } else {
            CallContext.checkAccess(path.toString(), cx.readPermission(1));
        }
        boolean computed = opts.getBoolean("computed", false);
        Stream<Metadata> stream = lookup(cx.getStorage(), path, opts);
        if (path.isIndex()) {
            Array res = new Array();
            stream.forEach(m -> res.add(serialize(m, computed)));
            return res;
        } else {
            return serialize(stream.findFirst().orElse(null), computed);
        }
    }
}
