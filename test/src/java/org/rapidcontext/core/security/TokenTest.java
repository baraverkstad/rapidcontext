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

import static org.junit.Assert.*;
import static org.rapidcontext.core.security.Token.*;
import static org.rapidcontext.util.BinaryUtil.*;

import org.junit.Test;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.JsonSerializer;
import org.rapidcontext.core.security.Random;

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
}
