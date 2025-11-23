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

import java.util.logging.Logger;

import org.rapidcontext.app.model.AuthHelper;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.User;

/**
 * The built-in user password modification procedure.
 *
 * @author Per Cederberg
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
    @Override
    public Object call(CallContext cx, Bindings bindings)
        throws ProcedureException {

        User user = cx.user();
        if (user == null) {
            throw new ProcedureException(this, "user must be logged in");
        }
        String oldHash = bindings.getValue("oldHash").toString();
        if (!user.verifyPasswordHash(oldHash) && !isValidAuthToken(user, oldHash)) {
            throw new ProcedureException(this, "invalid current password");
        }
        String newHash = bindings.getValue("newHash").toString();
        if (newHash.length() != 32) {
            throw new ProcedureException(this, "new password hash is malformed");
        }
        try {
            LOG.info("updating " + user + " password");
            user.setPasswordHash(newHash);
            User.store(cx.storage(), user);
        } catch (Exception e) {
            throw new ProcedureException(this, e);
        }
        return user.id() + " password changed";
    }

    /**
     * Checks if the specified authentication token is valid.
     *
     * @param user           the user to validate for
     * @param token          the authentication token
     *
     * @return true if the token is valid, or false otherwise
     */
    private boolean isValidAuthToken(User user, String token) {
        try {
            AuthHelper.validateLoginToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
