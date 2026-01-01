/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2026 Per Cederberg. All rights reserved.
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

import org.rapidcontext.app.model.ApiUtil;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.User;

/**
 * The built-in user access control procedure.
 *
 * @author Per Cederberg
 */
public class UserAccessProcedure extends Procedure {

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public UserAccessProcedure(String id, String type, Dict dict) {
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

        String path = bindings.getValue("path").toString();
        Dict opts = ApiUtil.options("permission", bindings.getValue("permission"));
        String userId = bindings.getValue("user", "").toString();
        String perm = opts.get("permission", String.class, "read");
        String via = opts.get("via", String.class, "");
        if (userId.isBlank() && via.isBlank()) {
            return cx.hasAccess(path, perm);
        } else {
            cx.requireReadAccess("user/" + userId);
            User user = switch (userId) {
                case "", "@self" -> cx.user();
                case "anonymous" -> null;
                default -> User.find(cx.storage(), userId);
            };
            via = via.isBlank() ? null : via;
            return SecurityContext.hasAccess(user, path, via, perm);
        }
    }
}
