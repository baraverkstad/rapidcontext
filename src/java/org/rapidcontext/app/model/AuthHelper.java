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

package org.rapidcontext.app.model;

import java.util.logging.Logger;

import org.rapidcontext.core.ctx.Context;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.security.Token;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.User;

/**
 * Helper methods for handling authentication tokens.
 *
 * @author Per Cederberg
 */
public class AuthHelper {

    /**
     * The class logger.
     */
    private static final Logger LOG = Logger.getLogger(AuthHelper.class.getName());

    /**
     * Creates a login token for a user. The token contains the user id,
     * an expiry timestamp and a validation signature.
     *
     * @param user           the user to create the token for
     * @param expiry         the expiry timestamp (in millis)
     *
     * @return the login token
     */
    public static String createLoginToken(User user, long expiry) {
        Dict payload = new Dict().set("u", user.id());
        return Token.createJwt(user.passwordHash(), expiry, payload);
    }

    /**
     * Validates a login token. This method supports both the new JWT
     * format and the legacy auth token format.
     *
     * @param token          the login token
     *
     * @return the authenticated user
     *
     * @throws SecurityException if the token is invalid or expired
     */
    @SuppressWarnings("removal")
    public static User validateLoginToken(String token) {
        Storage storage = Context.active().storage();
        if (token.contains(".")) {
            Dict payload = Token.decodeJwt(token);
            String userId = payload.get("u", String.class);
            User user = User.find(storage, userId);
            if (user == null || !user.isEnabled()) {
                throw new SecurityException("login token user disabled: " + user);
            }
            Token.validateJwt(user.passwordHash(), token);
            return user;
        } else {
            LOG.warning("deprecated: legacy auth token used");
            String[] parts = Token.decodeAuthToken(token);
            User user = User.find(storage, parts[0]);
            Token.validateAuthToken(user, token);
            return user;
        }
    }

    // No instances
    private AuthHelper() {}
}
