/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2012 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE.txt file for more details.
 */

package org.rapidcontext.app.proc;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.Restricted;
import org.rapidcontext.core.security.SecurityContext;

/**
 * The built-in thread creation procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ThreadCreateProcedure implements Procedure, Restricted {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.Thread.Create";

    /**
     * The thread creation counter. Used only for generating unique
     * thread names.
     */
    private static int counter = 1;

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new thread creation procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public ThreadCreateProcedure() throws ProcedureException {
        defaults.set("procedure", Bindings.ARGUMENT, "",
                     "The name of the procedure to execute.");
        defaults.set("arguments", Bindings.ARGUMENT, "",
                     "The array with procedure arguments.");
        defaults.seal();
    }

    /**
     * Checks if the currently authenticated user has access to this
     * object.
     *
     * @return true if the current user has access, or
     *         false otherwise
     */
    public boolean hasAccess() {
        return SecurityContext.currentUser() != null;
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
        return "Creates a new server thread for executing the specified procedure.";
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

        Thread    thread;
        String    proc;
        Object[]  args;
        String    source;
        String    name;
        Array     data;
        Object    obj;

        proc = bindings.getValue("procedure").toString();
        obj = bindings.getValue("arguments");
        if (obj instanceof Array) {
            data = (Array) obj;
            args = new Object[data.size()];
            for (int i = 0; i < args.length; i++) {
                args[i] = data.get(i);
            }
        } else {
            args = new Object[1];
            args[0] = obj;
        }
        source = (String) cx.getAttribute(CallContext.ATTRIBUTE_SOURCE);
        name = "Procedure Thread " + counter++;
        thread = new Thread(new ProcedureExecutor(proc, args, source), name);
        thread.start();
        return Integer.valueOf(thread.hashCode());
    }

    /**
     * An asynchronous procedure executor.
     *
     * @author   Per Cederberg, Dynabyte AB
     * @version  1.0
     */
    private static class ProcedureExecutor implements Runnable {

        /**
         * The name of the currently authenticated user.
         */
        private String userName;

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
            this.userName = SecurityContext.currentUser().id();
            this.proc = name;
            this.args = args;
            this.source = source;
        }

        /**
         * Executes the procedure in the current application context.
         */
        public void run() {
            ApplicationContext  ctx = ApplicationContext.getInstance();

            SecurityContext.auth(userName);
            try {
                ctx.executeAsync(proc, args, source);
            } finally {
                SecurityContext.authClear();
            }
        }
    }
}
