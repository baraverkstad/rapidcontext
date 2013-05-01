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
 * See the RapidContext LICENSE.txt file for more details.
 */

package org.rapidcontext.app.proc;

import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.Restricted;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.type.Session;

public class SessionAuthenticateProcedure implements Procedure, Restricted {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.Session.Authenticate";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new session authentication procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public SessionAuthenticateProcedure() throws ProcedureException {
        defaults.set("user", Bindings.ARGUMENT, "",
                     "The unique user id");
        defaults.set("nonce", Bindings.ARGUMENT, "",
                     "The challenge nonce used for the hash.");
        defaults.set("hash", Bindings.ARGUMENT, "",
                     "The hexadecimal MD5 hash that validates the password. " +
                     "First calculate an MD5 hash from a string on the form " +
                     "'<userId>:<realm>:<password>'. The final hash is then " +
                     "calculated as an MD5 hash of '<first hash>:nonce'.");
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
        return "Authenticates a user with the current session.";
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

        Session session = (Session) Session.activeSession.get();
        if (session == null) {
            throw new ProcedureException("no current session found");
        } else if (session.userId().length() > 0) {
            throw new ProcedureException("session already authenticated");
        }
        String userId = bindings.getValue("user").toString();
        String nonce = bindings.getValue("nonce").toString();
        String hash = bindings.getValue("hash").toString();
        try {
            SecurityContext.verifyNonce(nonce);
            SecurityContext.authHash(userId, ":" + nonce, hash);
            session.setUserId(userId);
            return userId + " logged in";
        } catch (Exception e) {
            throw new ProcedureException("invalid user or password");
        }
    }
}
