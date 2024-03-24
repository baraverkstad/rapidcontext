/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2024 Per Cederberg. All rights reserved.
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

import org.apache.commons.lang3.ArrayUtils;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.StorageException;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.User;

/**
 * The built-in user modification procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class UserChangeProcedure extends Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(UserChangeProcedure.class.getName());

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public UserChangeProcedure(String id, String type, Dict dict) {
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

        // Validate arguments
        String id = bindings.getValue("id", "").toString().trim();
        if (id.length() <= 0) {
            throw new ProcedureException(this, "user id cannot be blank");
        } else if (!id.matches("^[a-zA-Z0-9_/]*$")) {
            throw new ProcedureException(this, "user id contains invalid character");
        }
        CallContext.checkWriteAccess("user/" + id);
        User user = User.find(cx.getStorage(), id);
        String name = bindings.getValue("name", "").toString();
        String email = bindings.getValue("email", "").toString();
        String descr = bindings.getValue("description", "").toString();
        String str = bindings.getValue("enabled", "").toString();
        boolean enabled = (!str.equals("") && !str.equals("false") && !str.equals("0"));
        String pwd = bindings.getValue("password").toString();
        if ((user == null || pwd.length() > 0) && pwd.length() < 5) {
            throw new ProcedureException(this, "password must be at least 5 characters");
        }
        String[] roles = null;
        Object obj = bindings.getValue("roles");
        if (obj instanceof Array a) {
            roles = a.values(ArrayUtils.EMPTY_STRING_ARRAY);
        } else {
            roles = obj.toString().split("[ ,]+");
        }

        // Create or modify user
        String res = null;
        if (user == null) {
            user = new User(id);
            res = user.id() + " created";
        } else {
            res = user.id() + " modified";
        }
        user.setName(name);
        user.setEmail(email);
        user.setDescription(descr);
        user.setEnabled(enabled);
        if (pwd.length() > 0) {
            user.setPassword(pwd);
        }
        user.setRoles(roles);
        try {
            LOG.info("updating " + user);
            User.store(cx.getStorage(), user);
        } catch (StorageException e) {
            throw new ProcedureException(this, e);
        }
        return res;
    }
}
