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

package org.rapidcontext.core.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.JsonSerializer;
import org.rapidcontext.util.BinaryUtil;

/**
 * A set of utility methods for handling authenticated tokens.
 *
 * @author Per Cederberg
 */
public final class Token {

    /**
     * Creates a JWT (JSON Web Token) with the specified payload.
     *
     * @param secret         the secret key to sign with
     * @param expiry         the expiry timestamp (in millis)
     * @param payload        the payload dictionary
     *
     * @return the JWT string
     *
     * @throws SecurityException if the token creation fails
     */
    public static String createJwt(String secret, long expiry, Dict payload) {
        if (secret == null || secret.isBlank()) {
            throw new SecurityException("cannot create JWT: secret cannot be blank");
        } else if (payload == null || payload.size() == 0) {
            throw new SecurityException("cannot create JWT: payload cannot be empty");
        }
        try {
            Dict header = new Dict().set("alg", "HS256").set("typ", "JWT");
            Dict claims = payload.copy();
            claims.set("iat", System.currentTimeMillis() / 1000);
            claims.set("exp", expiry / 1000);
            byte[] h = JsonSerializer.serialize(header, false).getBytes(StandardCharsets.UTF_8);
            byte[] c = JsonSerializer.serialize(claims, false).getBytes(StandardCharsets.UTF_8);
            String data = BinaryUtil.encodeBase64(h) + "." + BinaryUtil.encodeBase64(c);
            return data + "." + BinaryUtil.encodeBase64(BinaryUtil.hmacSHA256(secret, data));
        } catch (Exception e) {
            throw new SecurityException("failed to create JWT: " + e.getMessage());
        }
    }

    /**
     * Decodes a JWT token payload without validation.
     *
     * @param token          the JWT token string
     *
     * @return the token payload, or an empty dictionary on error
     */
    public static Dict decodeJwt(String token) {
        try {
            String[] parts = Objects.requireNonNullElse(token, "").split("\\.");
            byte[] data = BinaryUtil.decodeBase64((parts.length == 3) ? parts[1] : "");
            String json = (data != null) ? new String(data, StandardCharsets.UTF_8) : "";
            if (JsonSerializer.unserialize(json) instanceof Dict dict) {
                return dict;
            }
        } catch (Exception ignore) {
            // Ignore errors
        }
        return new Dict();
    }

    /**
     * Validates a JWT (JSON Web Token) and returns the payload.
     *
     * @param secret         the secret key to verify with
     * @param token          the JWT string
     *
     * @return the payload dictionary
     *
     * @throws SecurityException if the token is invalid or expired
     */
    public static Dict validateJwt(String secret, String token) {
        String[] parts = Objects.requireNonNullElse(token, "").split("\\.");
        if (parts.length != 3) {
            throw new SecurityException("invalid JWT format");
        }
        String data = parts[0] + "." + parts[1];
        String sign = BinaryUtil.encodeBase64(BinaryUtil.hmacSHA256(secret, data));
        if (!isEqualSafe(sign, parts[2])) {
            throw new SecurityException("invalid JWT signature");
        }
        Dict payload;
        try {
            String json = new String(BinaryUtil.decodeBase64(parts[1]), StandardCharsets.UTF_8);
            payload = (Dict) JsonSerializer.unserialize(json);
        } catch (Exception e) {
            throw new SecurityException("invalid JWT payload: " + e.getMessage());
        }
        long exp = payload.get("exp", Long.class, 0L) * 1000L;
        if (exp < System.currentTimeMillis()) {
            String msg = "JWT expired at @" + exp + " (now @" + System.currentTimeMillis() + ")";
            throw new SecurityException(msg);
        }
        return payload;
    }

    /**
     * Compares two strings for equality in a constant time.
     *
     * @param a              the first string
     * @param b              the second string
     *
     * @return true if the strings are equal
     */
    private static boolean isEqualSafe(String a, String b) {
        return MessageDigest.isEqual(
            a.getBytes(StandardCharsets.UTF_8),
            b.getBytes(StandardCharsets.UTF_8)
        );
    }

    // No instances
    private Token() {}
}
