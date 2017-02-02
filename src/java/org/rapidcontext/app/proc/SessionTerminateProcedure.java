/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2017 Per Cederberg. All rights reserved.
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

import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Session;

/**
 * The built-in session termination procedure. This can be used to
 * logout the currently authenticated session or to force the logout
 * of another user.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class SessionTerminateProcedure implements Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(SessionTerminateProcedure.class.getName());

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

        Session session = null;
        String id = (String) bindings.getValue("sessionId", "");
        if (id == null || id.trim().equals("")) {
            session = Session.activeSession.get();
        } else {
            CallContext.checkWriteAccess("session/" + id);
            session = Session.find(cx.getStorage(), id);
        }
        if (session == null) {
            String msg = "cannot find session with id " + id;
            throw new ProcedureException(msg);
        }
        LOG.info("terminating " + session);
        session.invalidate();
        return "session terminated";
    }
}
