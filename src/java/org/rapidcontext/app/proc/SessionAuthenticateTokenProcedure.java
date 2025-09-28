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

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.Session;
import static org.rapidcontext.app.proc.SessionAuthenticateProcedure.response;

import org.rapidcontext.app.model.RequestContext;

/**
 * The built-in session authentication token procedure.
 *
 * @author Per Cederberg
 */
public class SessionAuthenticateTokenProcedure extends Procedure {

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public SessionAuthenticateTokenProcedure(String id, String type, Dict dict) {
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

        Session session = RequestContext.active().session();
        if (session == null) {
            return response(false, "no active session found", null);
        } else if (!session.userId().isBlank()) {
            return response(true, "session already authenticated", session.userId());
        }
        String token = bindings.getValue("token").toString();
        try {
            SecurityContext.authToken(token);
            String userId = SecurityContext.currentUser().id();
            session.setUserId(userId);
            return response(true, "token valid, please change password", userId);
        } catch (Exception e) {
            return response(false, e.getMessage(), null);
        }
    }
}
