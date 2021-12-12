/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2019 Per Cederberg. All rights reserved.
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

import org.apache.commons.lang3.time.DateUtils;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.User;

/**
 * The built-in user authentication token creation procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class UserAuthenticationTokenProcedure implements Procedure {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.User.AuthenticationToken";

    /**
     * The default authentication token duration.
     */
    public static final long DEFAULT_DURATION = 15 * DateUtils.MILLIS_PER_DAY;

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new user authentication token creation procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public UserAuthenticationTokenProcedure() throws ProcedureException {
        defaults.set("user", Bindings.ARGUMENT, "",
                     "The user id to create a token for");
        defaults.set("duration", Bindings.ARGUMENT, "",
                     "The token duration in milliseconds");
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
        return "Creates a login authentication token for a user.";
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

        String userId = bindings.getValue("user").toString();
        String duration = bindings.getValue("duration", "").toString();
        long   expiryTime = System.currentTimeMillis() + DEFAULT_DURATION;

        CallContext.checkAccess("user/" + userId, cx.readPermission(1));
        User user = User.find(cx.getStorage(), userId);
        if (user == null) {
            throw new ProcedureException("user not found");
        }
        if (duration.trim().length() > 0) {
            expiryTime = System.currentTimeMillis() + Long.parseLong(duration);
        }
        return user.createAuthToken(expiryTime);
    }
}
