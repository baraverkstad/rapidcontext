/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2013 Per Cederberg. All rights reserved.
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

import java.util.logging.Logger;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.type.User;

/**
 * The built-in thread interrupt procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ThreadInterruptProcedure implements Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(ThreadInterruptProcedure.class.getName());

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.Thread.Interrupt";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new thread interrupt procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public ThreadInterruptProcedure() throws ProcedureException {
        defaults.set("threadId", Bindings.ARGUMENT, "", "The thread id");
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
        return "Interrupts the specified (running) thread.";
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

        String str = bindings.getValue("threadId").toString();
        int threadId = 0;
        try {
            threadId = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new ProcedureException("invalid thread id: " + str);
        }
        cx = ApplicationContext.getInstance().findContext(threadId);
        if (cx == null) {
            throw new ProcedureException("cannot interrupt thread without context");
        }
        User user = (User) cx.getAttribute(CallContext.ATTRIBUTE_USER);
        boolean isOwner = user != null && user == SecurityContext.currentUser();
        if (!isOwner) {
            CallContext.checkWriteAccess("thread/" + threadId);
        }
        LOG.info("interrupting thread " + threadId);
        cx.interrupt();
        return null;
    }
}
