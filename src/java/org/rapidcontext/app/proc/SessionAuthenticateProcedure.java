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

import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.Session;

/**
 * The built-in session authentication procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class SessionAuthenticateProcedure extends Procedure {

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public SessionAuthenticateProcedure(String id, String type, Dict dict) {
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

        Session session = Session.activeSession.get();
        if (session == null) {
            return response(false, "no active session found", null);
        } else if (session.userId().length() > 0) {
            return response(true, "session already authenticated", session.userId());
        }
        String userId = bindings.getValue("user").toString();
        String nonce = bindings.getValue("nonce").toString();
        String hash = bindings.getValue("hash").toString();
        // FIXME: Include session id in hash to avoid replay attacks
        try {
            SecurityContext.verifyNonce(nonce);
            SecurityContext.authHash(userId, ":" + nonce, hash);
            session.setUserId(userId);
            return response(true, "successful authentication", userId);
        } catch (Exception e) {
            return response(false, e.getMessage(), null);
        }
    }

    /**
     * Builds an authentication response object.
     *
     * @param success        the authentication success flag
     * @param message        the success or error message
     * @param userId         the user identifier, or null
     *
     * @return the authentication response object
     */
    public static Dict response(boolean success, String message, String userId) {
        Dict res = new Dict();
        res.set("success", success);
        res.set(success ? "message" : "error", message);
        res.set("user", userId);
        return res;
    }
}
