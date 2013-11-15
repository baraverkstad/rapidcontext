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
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.type.User;

/**
 * The built-in user access control procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class UserAccessProcedure implements Procedure {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.User.Access";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new user access control procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public UserAccessProcedure() throws ProcedureException {
        defaults.set("path", Bindings.ARGUMENT, "",
                     "The storage path to check");
        defaults.set("permission", Bindings.ARGUMENT, "",
                     "The permission type to check, or null for 'read'");
        defaults.set("user", Bindings.ARGUMENT, "",
                     "The user id to check, or null for current user");
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
        return "Checks access to a storage object for a user.";
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

        String path = bindings.getValue("path").toString();
        String perm = bindings.getValue("permission", "read").toString();
        String userId = bindings.getValue("user", "").toString();
        if (userId.trim().length() <= 0) {
            return Boolean.valueOf(SecurityContext.hasAccess(path, perm));
        } else {
            CallContext.checkAccess("user/" + userId, cx.readPermission(1));
            User user = null;
            if (!userId.equalsIgnoreCase("anonymous")) {
                user = User.find(cx.getStorage(), userId);
            }
            return Boolean.valueOf(SecurityContext.hasAccess(user, path, perm));
        }
    }
}
