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

import java.util.logging.Logger;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.Session;

/**
 * The built-in session termination procedure. This can be used to
 * logout the currently authenticated session or to force the logout
 * of another user.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class SessionTerminateProcedure extends Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(SessionTerminateProcedure.class.getName());

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public SessionTerminateProcedure(String id, String type, Dict dict) {
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
