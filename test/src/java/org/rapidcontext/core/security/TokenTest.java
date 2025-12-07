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

import static org.junit.Assert.*;
import static org.rapidcontext.core.security.Token.*;
import static org.rapidcontext.util.BinaryUtil.*;

import org.junit.Test;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.JsonSerializer;
import org.rapidcontext.core.security.Random;
import org.rapidcontext.core.type.User;

@SuppressWarnings("javadoc")
public class TokenTest {

    @Test
    public void testCreateJwt() throws Exception {
        String secret = Random.base64(32);
        long start = System.currentTimeMillis();
        long expiry = start + 60000L;
        Dict payload = new Dict().set("u", "user");

        // Verify token structure
        String token = createJwt(secret, expiry, payload);
        assertNotNull(token);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
        Dict header = (Dict) JsonSerializer.unserialize(new String(decodeBase64(parts[0])));
        Dict claims = (Dict) JsonSerializer.unserialize(new String(decodeBase64(parts[1])));
        assertEquals(43, parts[2].length()); // 64 bytes base64 encoded
        assertEquals("HS256", header.get("alg"));
        assertEquals("JWT", header.get("typ"));
        assertTrue(claims.get("iat", Long.class) >= start / 1000L);
        assertTrue(claims.get("iat", Long.class) <= System.currentTimeMillis() / 1000L);
        assertEquals(expiry / 1000L, (long) claims.get("exp", Long.class));
        assertEquals("user", claims.get("u"));
        assertEquals(token, createJwt(secret, expiry, payload));

        // Validation
        assertThrows(SecurityException.class, () -> createJwt(null, 0, payload));
        assertThrows(SecurityException.class, () -> createJwt("", 0, payload));
        assertThrows(SecurityException.class, () -> createJwt(secret, 0, null));
        assertThrows(SecurityException.class, () -> createJwt(secret, 0, new Dict()));
    }

    @Test
    public void testDecodeJwt() {
        // Invalid token format
        assertEquals(0, decodeJwt(null).size());
        assertEquals(0, decodeJwt("").size());
        assertEquals(0, decodeJwt("invalid").size());
        assertEquals(0, decodeJwt("a.b").size());
        assertEquals(0, decodeJwt("a.b.c.d").size());
        assertEquals(0, decodeJwt("header.invalid_base64.signature").size());

        // Valid payload in invalid token
        Dict data = new Dict().set("u", "user").set("test", "123");
        String json = JsonSerializer.serialize(data, false);
        Dict decoded = decodeJwt("-." + encodeBase64(json.getBytes()) + ".-");
        assertEquals(data, decoded);
    }

    @Test
    public void testValidateJwt() {
        String secret = Random.base64(32);
        long expiry = System.currentTimeMillis() + 60000;
        Dict payload = new Dict().set("u", "user");
        String token = createJwt(secret, expiry, payload);
        assertThrows(NullPointerException.class, () -> validateJwt(null, token));
        assertThrows(SecurityException.class, () -> validateJwt("", token));
        assertThrows(SecurityException.class, () -> validateJwt(secret, null));
        assertThrows(SecurityException.class, () -> validateJwt(secret, ""));
        assertThrows(SecurityException.class, () -> validateJwt(secret, "a.b"));
        assertThrows(SecurityException.class, () -> validateJwt("wrongsecret", token));
        assertThrows(SecurityException.class, () -> validateJwt(secret, "  " + token));
        String expired = createJwt(secret, System.currentTimeMillis() - 1000, payload);
        assertThrows(SecurityException.class, () -> validateJwt(secret, expired));
        Dict decoded = validateJwt(secret, token);
        assertEquals("user", decoded.get("u"));
    }

    @Test
    @SuppressWarnings("removal")
    public void testCreateAuthToken() throws Exception {
        // User validation
        User user = new User("id");
        assertThrows(SecurityException.class, () -> createAuthToken(null, 1L));
        assertThrows(SecurityException.class, () -> createAuthToken(user, 1L));
        user.setPasswordHash("invalid");
        assertThrows(SecurityException.class, () -> createAuthToken(user, 1L));
        user.setPassword("some-password");
        user.setEnabled(false);
        assertThrows(SecurityException.class, () -> createAuthToken(user, 1L));
        user.setEnabled(true);
        assertAuthToken(createAuthToken(user, 0L), "id", "0", hashSHA256("id:0:" + user.passwordHash()));

        // Basic validation
        assertThrows(SecurityException.class, () -> createAuthToken(null, 1L, "id"));
        assertThrows(SecurityException.class, () -> createAuthToken("", 1L, "id"));
        assertThrows(SecurityException.class, () -> createAuthToken("secret", 1L, null));
        assertThrows(SecurityException.class, () -> createAuthToken("secret", 1L, ""));

        // Basic creation
        assertAuthToken(createAuthToken("secret", 12345L, "id"), "id", "12345", hashSHA256("id:12345:secret"));
        assertAuthToken(createAuthToken("secret", -1L, "id"), "id", "0", hashSHA256("id:-1:secret"));
        assertAuthToken(createAuthToken("secret", 0L, "id"), "id", "0", hashSHA256("id:0:secret"));
    }

    @Test
    public void testDecodeAuthToken() {
        // Invalid Base64
        assertAuthToken(null, "", "0", "");
        assertAuthToken("", "", "0", "");
        assertAuthToken("\u00E5\u00E4\u00F6", "", "0", "");

        // Invalid formats
        assertAuthToken(encodeBase64("test".getBytes()), "test", "0", "");
        assertAuthToken(encodeBase64(":".getBytes()), "", "0", "");
        assertAuthToken(encodeBase64("::".getBytes()), "", "0", "");
        assertAuthToken(encodeBase64(":::".getBytes()), "", "0", ":");
        assertAuthToken(encodeBase64(":-1:".getBytes()), "", "0", "");
        assertAuthToken(encodeBase64(":+1:".getBytes()), "", "0", "");
        assertAuthToken(encodeBase64(":123456789012345:".getBytes()), "", "0", "");
        assertAuthToken(encodeBase64(":\u0967\u0968\u0969:".getBytes()), "", "0", "");

        // Valid formats
        assertAuthToken(encodeBase64("1:2:3".getBytes()), "1", "2", "3");
        assertAuthToken(encodeBase64("u:12345678901234:h".getBytes()), "u", "12345678901234", "h");
    }

    @SuppressWarnings("removal")
    private void assertAuthToken(String token, String user, String expiry, String hash) {
        String[] parts = decodeAuthToken(token);
        assertEquals("token user", user, parts[0]);
        assertEquals("token expiry", expiry, parts[1]);
        assertEquals("token hash", hash, parts[2]);
    }

    @Test
    @SuppressWarnings("removal")
    public void testValidateAuthToken() {
        User user = new User("id");
        user.setPassword("some-password");
        User other = new User("id");
        other.setPassword("other-password");
        long now = System.currentTimeMillis();

        // Basic validation
        validateAuthToken(user, createAuthToken(user, now + 10000L));
        assertThrows(SecurityException.class, () ->
            // Token has expired
            validateAuthToken(user, createAuthToken(user, now - 1000L)));
        assertThrows(SecurityException.class, () ->
            // Malformed token (always expired)
            validateAuthToken(user, encodeBase64("id:invalid:hash".getBytes())));

        // User validation
        assertThrows(SecurityException.class, () ->
            // Invalid user
            validateAuthToken(null, createAuthToken(user, now + 10000L)));
        user.setEnabled(false);
        assertThrows(SecurityException.class, () ->
            // User disabled
            validateAuthToken(user, createAuthToken(user, now + 10000L)));
        user.setEnabled(true);
        String token = createAuthToken(user, now + 10000L);
        user.setPassword("new-password");
        assertThrows(SecurityException.class, () ->
            // User password change invalidates token
            validateAuthToken(user, token));

        // Signature validation
        assertThrows(SecurityException.class, () ->
            // Signed by other user
            validateAuthToken(user, createAuthToken(other, now + 10000L)));
        assertThrows(SecurityException.class, () -> {
            // Invalid signature
            String invalid = user.id() + ":" + (now + 10000L) + ":invalid-hash";
            validateAuthToken(user, encodeBase64(invalid.getBytes()));
        });
    }
}
