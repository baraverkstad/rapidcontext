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

import java.time.Duration;
import java.time.Instant;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.User;
import org.rapidcontext.core.security.Token;

/**
 * The built-in user authentication token creation procedure.
 *
 * @author Per Cederberg
 */
public class UserAuthenticationTokenProcedure extends Procedure {

    /**
     * The default authentication token duration.
     */
    public static final long DEFAULT_DURATION = 15 * DateUtils.MILLIS_PER_DAY;

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public UserAuthenticationTokenProcedure(String id, String type, Dict dict) {
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

        String userId = bindings.getValue("user").toString();
        String duration = bindings.getValue("duration", "").toString().trim();
        cx.requireReadAccess("user/" + userId);
        User user = User.find(cx.storage(), userId);
        if (user == null) {
            throw new ProcedureException(this, "user not found");
        }
        long expires = System.currentTimeMillis() + DEFAULT_DURATION;
        if (StringUtils.isNumeric(duration)) {
            expires = System.currentTimeMillis() + Long.parseLong(duration);
        } else if (!duration.isBlank()) {
            expires = Instant.now().plus(Duration.parse(duration)).toEpochMilli();
        }
        return Token.createLoginToken(user, expires);
    }
}
