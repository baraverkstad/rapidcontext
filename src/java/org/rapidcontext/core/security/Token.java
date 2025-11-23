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

package org.rapidcontext.core.security;

import java.security.MessageDigest;
import java.util.Objects;

import org.rapidcontext.core.type.User;
import org.rapidcontext.util.BinaryUtil;

/**
 * A set of utility methods for handling authentication tokens.
 *
 * @author Per Cederberg
 */
public final class Token {

    /**
     * Creates an authentication token for a user. The token contains the
     * user id, an expiry timestamp and a validation hash based on both of
     * these and the user password hash.
     *
     * @param user           the user to create the token for
     * @param expiry         the expiry timestamp (in millis)
     *
     * @return the authentication token
     *
     * @throws SecurityException if user isn't enabled or password isn't set
     */
    public static String createAuthToken(User user, long expiry) {
        if (user == null || !user.isEnabled()) {
            throw new SecurityException("cannot create auth token: user isn't enabled");
        } else if (Objects.requireNonNullElse(user.passwordHash(), "").trim().length() < 32) {
            throw new SecurityException("cannot create auth token: user password not set");
        }
        return createAuthToken(user.passwordHash(), expiry, user.id());
    }

    /**
     * Creates a user authentication token. The token contains the user id,
     * an expiry timestamp and a validation hash based on both of these and
     * the secret.
     *
     * @param secret         the secret key (password hash)
     * @param expiry         the expiry timestamp (in millis)
     * @param id             the user id
     *
     * @return the authentication token
     *
     * @throws SecurityException if the token secret or user id aren't valid
     */
    public static String createAuthToken(String secret, long expiry, String id) {
        if (secret == null || secret.isBlank()) {
            throw new SecurityException("cannot create auth token: secret cannot be blank");
        } else if (id == null || id.isBlank()) {
            throw new SecurityException("cannot create auth token: user id cannot be blank");
        }
        try {
            String hash = BinaryUtil.hashSHA256(id + ":" + expiry + ":" + secret);
            String str = id + ':' + expiry + ':' + hash;
            return BinaryUtil.encodeBase64(str.getBytes());
        } catch (Exception e) {
            throw new SecurityException("cannot create auth token: " + e.getMessage());
        }
    }

    /**
     * Decodes a user authentication token. This method always returns
     * an array of length 3, even for syntactically incorrect tokens.
     * It also guarantees that the expiry time is a valid long value.
     *
     * @param token          the token string
     *
     * @return an array of user id, expiry time and validation hash
     */
    public static String[] decodeAuthToken(String token) {
        byte[] data = BinaryUtil.decodeBase64(token);
        String raw = (data == null) ? "" : new String(data);
        String[] parts = raw.split(":", 3);
        if (parts.length != 3) {
            String[] copy = new String[3];
            copy[0] = (parts.length > 0) ? parts[0] : "";
            copy[1] = (parts.length > 1) ? parts[1] : "";
            copy[2] = (parts.length > 2) ? parts[2] : "";
            parts = copy;
        }
        if (!parts[1].matches("^\\d{1,14}$")) {
            parts[1] = "0";
        }
        return parts;
    }

    /**
     * Validates a user authentication token.
     *
     * @param user           the user to validate the token for
     * @param token          the authentication token
     *
     * @throws SecurityException if the token is invalid or expired
     */
    public static void validateAuthToken(User user, String token) {
        String[] parts = decodeAuthToken(token);
        long expiry = Long.parseLong(parts[1]);
        if (expiry < System.currentTimeMillis()) {
            throw new SecurityException("auth token expired at @" + expiry);
        } else if (user == null || !user.isEnabled()) {
            throw new SecurityException("auth token user disabled: " + user);
        } else if (!MessageDigest.isEqual(createAuthToken(user, expiry).getBytes(), token.getBytes())) {
            throw new SecurityException("invalid auth token");
        }
    }

    // No instances
    private Token() {}
}
