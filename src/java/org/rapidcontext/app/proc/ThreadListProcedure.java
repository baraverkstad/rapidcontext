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

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.type.Procedure;

/**
 * The built-in thread list procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 *
 * @deprecated Thread introspection will be removed in a future release.
 */
@Deprecated(forRemoval = true)
public class ThreadListProcedure extends Procedure {

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public ThreadListProcedure(String id, String type, Dict dict) {
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

        ApplicationContext ctx = ApplicationContext.getInstance();
        CallContext.checkSearchAccess("thread/");
        Array res = new Array();
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            int id = t.hashCode();
            if (SecurityContext.hasReadAccess("thread/" + id)) {
                CallContext tcx = ctx.findContext(t);
                Dict data = (tcx == null) ? null : ThreadContextProcedure.getContextData(tcx);
                res.add(
                    new Dict()
                    .set("id", t.hashCode())
                    .set("name", t.getName())
                    .set("priority", t.getPriority())
                    .set("group", t.getThreadGroup().getName())
                    .set("daemon", t.isDaemon())
                    .set("alive", t.isAlive())
                    .set("context", data)
                );
            }
        }
        return res;
    }
}
