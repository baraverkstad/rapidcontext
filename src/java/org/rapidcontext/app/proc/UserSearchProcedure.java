/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2022 Per Cederberg. All rights reserved.
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

import java.util.Objects;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.User;

/**
 * The built-in user search procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class UserSearchProcedure implements Procedure {

    // TODO: Replace this procedure with Query API?

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.User.Search";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new user search procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public UserSearchProcedure() throws ProcedureException {
        defaults.set("email", Bindings.ARGUMENT, "",
                     "The user email address to search for");
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
        return "Searches for a user by email address.";
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

        Storage storage = cx.getStorage();
        String match = bindings.getValue("email", "").toString().trim();
        return storage.query(User.PATH).paths().map(path -> {
            Object o = storage.load(path);
            if (o instanceof User) {
                User user = (User) o;
                // TODO: Should really also compare with realm
                if (user.email().equalsIgnoreCase(match)) {
                    if (SecurityContext.hasReadAccess(path.toString())) {
                        return UserListProcedure.serialize(user);
                    } else {
                        Dict dict = new Dict();
                        dict.set(User.KEY_ID, user.id());
                        dict.set(User.KEY_TYPE, user.type());
                        dict.set(User.KEY_REALM, user.realm());
                        dict.set(User.KEY_EMAIL, user.email());
                        dict.set(User.KEY_ENABLED, Boolean.valueOf(user.isEnabled()));
                        return dict;
                    }
                }
            }
            return null;
        }).filter(Objects::nonNull).findFirst().orElse(null);
    }
}
