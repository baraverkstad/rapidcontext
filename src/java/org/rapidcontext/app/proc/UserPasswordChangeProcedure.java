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
 * See the RapidContext LICENSE for more details.
 */

package org.rapidcontext.app.proc;

import java.util.logging.Logger;

import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.type.User;

/**
 * The built-in user password modification procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class UserPasswordChangeProcedure implements Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(UserPasswordChangeProcedure.class.getName());

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.User.ChangePassword";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new user password modification procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public UserPasswordChangeProcedure() throws ProcedureException {
        defaults.set("oldHash", Bindings.ARGUMENT, "",
                     "The hexadecimal MD5 hash of the current password. The " +
                     "MD5 hash is calculated from a string on the form " +
                     "'<userId>:<realm>:<password>'. A login token may be used.");
        defaults.set("newHash", Bindings.ARGUMENT, "",
                     "The hexadecimal MD5 hash of the new password. The MD5 " +
                     "hash is calculated from a string on the form " +
                     "'<userId>:<realm>:<password>'.");
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
        return "Changes the password for the current user.";
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
