/*
 * RapidContext <https://www.rapidcontext.com/>
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

import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.type.Session;

/**
 * The built-in session authentication token procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class SessionAuthenticateTokenProcedure implements Procedure {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.Session.AuthenticateToken";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new session authentication token procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public SessionAuthenticateTokenProcedure() throws ProcedureException {
        defaults.set("token", Bindings.ARGUMENT, "",
                     "The authentication token");
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
        return "Authenticates a user (via a token) with the current session.";
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

        Session session = Session.activeSession.get();
        if (session == null) {
            throw new ProcedureException("no current session found");
        } else if (session.userId().length() > 0) {
            throw new ProcedureException("session already authenticated");
        }
        String token = bindings.getValue("token").toString();
        try {
            SecurityContext.authToken(token);
            String userId = SecurityContext.currentUser().id();
            session.setUserId(userId);
            return userId + " logged in, please change password";
        } catch (Exception e) {
            throw new ProcedureException("invalid authentication token");
        }
    }
}
