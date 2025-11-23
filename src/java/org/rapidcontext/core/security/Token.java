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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.JsonSerializer;
import org.rapidcontext.core.type.User;
import org.rapidcontext.util.BinaryUtil;

/**
 * A set of utility methods for handling authentication tokens.
 *
 * @author Per Cederberg
 */
public final class Token {

    /**
     * The default random number generator.
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Creates a random secret string.
     *
     * @return a random secret string
     */
    public static String createSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return BinaryUtil.encodeHexString(bytes);
    }

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
            return data + "." + BinaryUtil.encodeBase64(hmacSha256(secret, data));
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
        String sign = BinaryUtil.encodeBase64(hmacSha256(secret, data));
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
     * Calculates the HMAC-SHA256 hash of the specified data using the
     * provided secret key.
     *
     * @param secret         the secret key
     * @param data           the data to hash
     *
     * @return the HMAC-SHA256 hash
     */
    private static byte[] hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new SecurityException("failed to calculate HMAC-SHA256: " + e.getMessage());
        }
    }

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
        String raw = (data == null) ? "" : new String(data, StandardCharsets.UTF_8);
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
            String msg = "auth token expired at @" + expiry + " (now @" + System.currentTimeMillis() + ")";
            throw new SecurityException(msg);
        } else if (user == null || !user.isEnabled()) {
            throw new SecurityException("auth token user disabled: " + user);
        } else if (!isEqualSafe(createAuthToken(user, expiry), token)) {
            throw new SecurityException("invalid auth token");
        }
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
