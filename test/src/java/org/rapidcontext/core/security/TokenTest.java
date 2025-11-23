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

import org.junit.Test;
import org.rapidcontext.core.type.User;
import org.rapidcontext.util.BinaryUtil;

@SuppressWarnings("javadoc")
public class TokenTest {

    @Test
    public void testCreateAuthToken() throws Exception {
        // User validation
        User user = new User("id");
        assertThrows(SecurityException.class, () -> Token.createAuthToken(null, 1L));
        assertThrows(SecurityException.class, () -> Token.createAuthToken(user, 1L));
        user.setPasswordHash("invalid");
        assertThrows(SecurityException.class, () -> Token.createAuthToken(user, 1L));
        user.setPassword("some-password");
        user.setEnabled(false);
        assertThrows(SecurityException.class, () -> Token.createAuthToken(user, 1L));
        user.setEnabled(true);
        assertAuthToken(Token.createAuthToken(user, 0L), "id", "0", BinaryUtil.hashSHA256("id:0:" + user.passwordHash()));

        // Basic validation
        assertThrows(SecurityException.class, () -> Token.createAuthToken(null, 1L, "id"));
        assertThrows(SecurityException.class, () -> Token.createAuthToken("", 1L, "id"));
        assertThrows(SecurityException.class, () -> Token.createAuthToken("secret", 1L, null));
        assertThrows(SecurityException.class, () -> Token.createAuthToken("secret", 1L, ""));

        // Basic creation
        assertAuthToken(Token.createAuthToken("secret", 12345L, "id"), "id", "12345", BinaryUtil.hashSHA256("id:12345:secret"));
        assertAuthToken(Token.createAuthToken("secret", -1L, "id"), "id", "0", BinaryUtil.hashSHA256("id:-1:secret"));
        assertAuthToken(Token.createAuthToken("secret", 0L, "id"), "id", "0", BinaryUtil.hashSHA256("id:0:secret"));
    }

    @Test
    public void testDecodeAuthToken() {
        // Invalid Base64
        assertAuthToken(null, "", "0", "");
        assertAuthToken("", "", "0", "");
        assertAuthToken("\u00E5\u00E4\u00F6", "", "0", "");

        // Invalid formats
        assertAuthToken(BinaryUtil.encodeBase64("test".getBytes()), "test", "0", "");
        assertAuthToken(BinaryUtil.encodeBase64(":".getBytes()), "", "0", "");
        assertAuthToken(BinaryUtil.encodeBase64("::".getBytes()), "", "0", "");
        assertAuthToken(BinaryUtil.encodeBase64(":::".getBytes()), "", "0", ":");
        assertAuthToken(BinaryUtil.encodeBase64(":-1:".getBytes()), "", "0", "");
        assertAuthToken(BinaryUtil.encodeBase64(":+1:".getBytes()), "", "0", "");
        assertAuthToken(BinaryUtil.encodeBase64(":123456789012345:".getBytes()), "", "0", "");
        assertAuthToken(BinaryUtil.encodeBase64(":\u0967\u0968\u0969:".getBytes()), "", "0", "");

        // Valid formats
        assertAuthToken(BinaryUtil.encodeBase64("1:2:3".getBytes()), "1", "2", "3");
        assertAuthToken(BinaryUtil.encodeBase64("u:12345678901234:h".getBytes()), "u", "12345678901234", "h");
    }

    private void assertAuthToken(String token, String user, String expiry, String hash) {
        String[] parts = decodeAuthToken(token);
        assertEquals("token user", user, parts[0]);
        assertEquals("token expiry", expiry, parts[1]);
        assertEquals("token hash", hash, parts[2]);
    }

    @Test
    public void testValidateAuthToken() {
        User user = new User("id");
        user.setPassword("some-password");
        User other = new User("id");
        other.setPassword("other-password");
        long now = System.currentTimeMillis();

        // Basic validation
        Token.validateAuthToken(user, Token.createAuthToken(user, now + 10000L));
        assertThrows(SecurityException.class, () ->
            // Token has expired
            Token.validateAuthToken(user, Token.createAuthToken(user, now - 1000L)));
        assertThrows(SecurityException.class, () ->
            // Malformed token (always expired)
            Token.validateAuthToken(user, BinaryUtil.encodeBase64("id:invalid:hash".getBytes())));

        // User validation
        assertThrows(SecurityException.class, () ->
            // Invalid user
            Token.validateAuthToken(null, Token.createAuthToken(user, now + 10000L)));
        user.setEnabled(false);
        assertThrows(SecurityException.class, () ->
            // User disabled
            Token.validateAuthToken(user, Token.createAuthToken(user, now + 10000L)));
        user.setEnabled(true);
        String token = Token.createAuthToken(user, now + 10000L);
        user.setPassword("new-password");
        assertThrows(SecurityException.class, () ->
            // User password change invalidates token
            Token.validateAuthToken(user, token));

        // Signature validation
        assertThrows(SecurityException.class, () ->
            // Signed by other user
            Token.validateAuthToken(user, Token.createAuthToken(other, now + 10000L)));
        assertThrows(SecurityException.class, () -> {
            // Invalid signature
            String invalid = user.id() + ":" + (now + 10000L) + ":invalid-hash";
            Token.validateAuthToken(user, BinaryUtil.encodeBase64(invalid.getBytes()));
        });
    }
}
