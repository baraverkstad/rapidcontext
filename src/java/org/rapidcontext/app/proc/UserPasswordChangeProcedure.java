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
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.User;

/**
 * The built-in user password modification procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class UserPasswordChangeProcedure extends Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(UserPasswordChangeProcedure.class.getName());

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public UserPasswordChangeProcedure(String id, String type, Dict dict) {
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

        User user = SecurityContext.currentUser();
        if (user == null) {
            throw new ProcedureException("user must be logged in");
        }
        String oldHash = bindings.getValue("oldHash").toString();
        if (!user.verifyPasswordHash(oldHash) && !user.verifyAuthToken(oldHash)) {
            throw new ProcedureException("invalid current password");
        }
        String newHash = bindings.getValue("newHash").toString();
        if (newHash.length() != 32) {
            throw new ProcedureException("new password hash is malformed");
        }
        try {
            LOG.info("updating " + user + " password");
            user.setPasswordHash(newHash);
            User.store(cx.getStorage(), user);
        } catch (Exception e) {
            throw new ProcedureException(e.getMessage());
        }
        return user.id() + " password changed";
    }
}
