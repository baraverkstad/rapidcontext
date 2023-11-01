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
 */
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
    public Object call(CallContext cx, Bindings bindings)
        throws ProcedureException {

        ApplicationContext ctx = ApplicationContext.getInstance();
        CallContext.checkSearchAccess("thread/");
        Array res = new Array();
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            int id = t.hashCode();
            if (SecurityContext.hasReadAccess("thread/" + id)) {
                Dict dict = new Dict();
                dict.set("id", t.hashCode());
                dict.set("name", t.getName());
                dict.set("priority", t.getPriority());
                dict.set("group", t.getThreadGroup().getName());
                dict.set("daemon", t.isDaemon());
                dict.set("alive", t.isAlive());
                CallContext threadCx = ctx.findContext(t);
                if (threadCx == null) {
                    dict.set("context", null);
                } else {
                    dict.set("context", ThreadContextProcedure.getContextData(threadCx));
                }
                res.add(dict);
            }
        }
        return res;
    }
}
