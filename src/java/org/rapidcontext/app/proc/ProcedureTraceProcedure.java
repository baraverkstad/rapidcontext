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

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.util.ValueUtil;

/**
 * The built-in procedure trace procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 *
 * @deprecated Tracing per-procedure will be removed in a future release. Use per-call tracing instead.
 */
@Deprecated(forRemoval = true)
public class ProcedureTraceProcedure extends Procedure {

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public ProcedureTraceProcedure(String id, String type, Dict dict) {
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
    @SuppressWarnings("removal")
    public Object call(CallContext cx, Bindings bindings)
        throws ProcedureException {

        String name = ((String) bindings.getValue("name")).trim();
        if (name.isBlank()) {
            throw new ProcedureException(this, "invalid procedure name");
        }
        String str = bindings.getValue("tracing").toString();
        boolean tracing = ValueUtil.bool(str, false);
        CallContext.checkWriteAccess("procedure/" + name);
        cx.getLibrary().setTracing(name, tracing);
        return "" + tracing;
    }
}
