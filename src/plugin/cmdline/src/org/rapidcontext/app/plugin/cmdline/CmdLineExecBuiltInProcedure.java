/**
 * RapidContext command-line plug-in <http://www.rapidcontext.com/>
 * Copyright (c) 2008-2009 Per Cederberg & Dynabyte AB.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE.txt file for more details.
 */

package org.rapidcontext.app.plugin.cmdline;

import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.Restricted;
import org.rapidcontext.core.security.SecurityContext;

/**
 * The built-in command-line execution procedure. This procedure
 * provides the functionality of executing local command-line
 * programs and capturing their output. It is restricted to
 * admin-only access for security reasons. Create a more specified
 * add-on procedure of the "cmdline.exec" type to allow other users
 * access to this functionality.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class CmdLineExecBuiltInProcedure implements Procedure, Restricted {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "PlugIn.CmdLine.Exec";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new command-line execution procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public CmdLineExecBuiltInProcedure() throws ProcedureException {
        defaults.set(CmdLineExecProcedure.BINDING_COMMAND, Bindings.ARGUMENT, "",
                     "The command-line to execute.");
        defaults.set(CmdLineExecProcedure.BINDING_DIRECTORY, Bindings.ARGUMENT, "",
                     "The working directory or blank for current.");
        defaults.set(CmdLineExecProcedure.BINDING_ENVIRONMENT, Bindings.ARGUMENT, "",
                     "The environment variable bindings or blank for current.");
        this.defaults.seal();
    }

    /**
     * Checks if the currently authenticated user has access to this
     * object.
     *
     * @return true if the current user has access, or
     *         false otherwise
     */
    public boolean hasAccess() {
        return SecurityContext.hasAdmin();
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
        return "Executes a local command-line program and captures the output.";
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

        return CmdLineExecProcedure.execCall(cx, bindings);
    }
}
