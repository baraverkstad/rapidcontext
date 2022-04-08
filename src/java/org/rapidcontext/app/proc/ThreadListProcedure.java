/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2022 Per Cederberg. All rights reserved.
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
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;

/**
 * The built-in thread list procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ThreadListProcedure implements Procedure {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.Thread.List";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new thread list procedure.
     */
    public ThreadListProcedure() {
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
        return "Returns a list of all currently running threads.";
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

        ApplicationContext ctx = ApplicationContext.getInstance();
        CallContext.checkSearchAccess("thread/");
        Array res = new Array();
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            int id = t.hashCode();
            if (SecurityContext.hasReadAccess("thread/" + id)) {
                Dict dict = new Dict();
                dict.setInt("id", t.hashCode());
                dict.set("name", t.getName());
                dict.setInt("priority", t.getPriority());
                dict.set("group", t.getThreadGroup().getName());
                dict.setBoolean("daemon", t.isDaemon());
                dict.setBoolean("alive", t.isAlive());
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
