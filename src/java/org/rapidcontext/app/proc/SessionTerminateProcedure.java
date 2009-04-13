/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2009 Per Cederberg & Dynabyte AB.
 * All rights reserved.
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

import javax.servlet.http.HttpSession;

import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.Restricted;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.web.SessionManager;

/**
 * The built-in session termination procedure. This can be used to
 * logout the currently authenticated session or to force the logout
 * of another user.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class SessionTerminateProcedure implements Procedure, Restricted {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.Session.Terminate";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new session termination procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public SessionTerminateProcedure() throws ProcedureException {
        defaults.set("sessionId", Bindings.ARGUMENT, "",
                     "The unique session id, use null or blank for current session");
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
        return true;
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
        return "Terminates the user session specified.";
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

        HttpSession  session;
        String       id;
        String       msg;

        id = (String) bindings.getValue("sessionId", "");
        if (id == null || id.trim().equals("")) {
            session = SessionManager.getCurrentSession();
        } else if (SecurityContext.hasAdmin()){
            session = SessionManager.getSession(id);
        } else {
            msg = "only admin users can terminate other sessions";
            throw new ProcedureException(msg);
        }
        if (session == null) {
            msg = "cannot find session with id " + id;
            throw new ProcedureException(msg);
        }
        session.invalidate();
        return "session terminated";
    }
}
