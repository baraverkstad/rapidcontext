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

import java.util.Objects;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.User;

/**
 * The built-in user search procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class UserSearchProcedure extends Procedure {

    // TODO: Replace this procedure with Query API?

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public UserSearchProcedure(String id, String type, Dict dict) {
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

        Storage storage = cx.getStorage();
        String match = bindings.getValue("email", "").toString().trim();
        return storage.query(User.PATH).paths().map(path -> {
            User user = storage.load(path, User.class);
            // TODO: Should really also compare with realm
            if (user != null && user.email().equalsIgnoreCase(match)) {
                if (SecurityContext.hasReadAccess(path.toString())) {
                    return StorableObject.sterilize(user, true, true, true);
                } else {
                    Dict dict = new Dict();
                    dict.set(User.KEY_ID, user.id());
                    dict.set(User.KEY_TYPE, user.type());
                    dict.set(User.KEY_REALM, user.realm());
                    dict.set(User.KEY_EMAIL, user.email());
                    dict.set(User.KEY_ENABLED, user.isEnabled());
                    return dict;
                }
            }
            return null;
        }).filter(Objects::nonNull).findFirst().orElse(null);
    }
}
