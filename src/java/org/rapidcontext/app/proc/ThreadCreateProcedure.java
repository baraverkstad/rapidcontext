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

import java.util.logging.Logger;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.model.RequestContext;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.Session;
import org.rapidcontext.core.type.User;

/**
 * The built-in thread creation procedure.
 *
 * @author Per Cederberg
 *
 * @deprecated Background thread execution will be removed in a future release.
 */
@Deprecated(forRemoval = true)
public class ThreadCreateProcedure extends Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(ThreadCreateProcedure.class.getName());

    /**
     * The thread creation counter. Used only for generating unique
     * thread names.
     */
    private static int counter = 1;

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public ThreadCreateProcedure(String id, String type, Dict dict) {
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

        if (SecurityContext.currentUser() == null) {
            throw new ProcedureException(this, "permission denied");
        }
        String proc = bindings.getValue("procedure").toString();
        CallContext.checkAccess("procedure/" + proc, cx.readPermission(1));
        Object[] args = null;
        Object obj = bindings.getValue("arguments");
        if (obj instanceof Array a) {
            args = a.values();
        } else {
            args = new Object[1];
            args[0] = obj;
        }
        String source = (String) cx.getAttribute(CallContext.ATTRIBUTE_SOURCE);
        String name = "Procedure Thread " + counter++;
        Thread thread = new Thread(new ProcedureExecutor(proc, args, source), name);
        LOG.info("created " + name.toLowerCase() + " for " + proc + " by " +
                 SecurityContext.currentUser());
        thread.start();
        return thread.hashCode();
    }

    /**
     * An asynchronous procedure executor.
     *
     * @author Per Cederberg
     */
    private static class ProcedureExecutor implements Runnable {

        /**
         * The current session.
         */
        private Session session;

        /**
         * The current user.
         */
        private User user;

        /**
         * The procedure name.
         */
        private String proc;

        /**
         * The procedure arguments.
         */
        private Object[] args;

        /**
         * The procedure call source information.
         */
        private String source;

        /**
         * Creates a new asynchronous procedure executor.
         *
         * @param name           the procedure name
         * @param args           the procedure arguments
         * @param source         the call source information
         */
        public ProcedureExecutor(String name, Object[] args, String source) {
            RequestContext cx = RequestContext.active();
            this.session = cx.session();
            this.user = cx.user();
            this.proc = name;
            this.args = args;
            this.source = source;
        }

        /**
         * Executes the procedure in the current application context.
         */
        @Override
        @SuppressWarnings("removal")
        public void run() {
            RequestContext cx = RequestContext.initAsync(session, user);
            try {
                ApplicationContext.active().executeAsync(proc, args, source);
            } finally {
                cx.close();
            }
        }
    }
}
