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

package org.rapidcontext.app;

import static org.junit.Assert.*;
import static org.rapidcontext.core.security.Token.*;

import java.io.File;
import java.util.Objects;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rapidcontext.app.model.RequestContext;

import org.rapidcontext.core.type.User;

@SuppressWarnings("javadoc")
public class AuthenticationIntegrationTest {

    private RequestContext cx;

    @BeforeClass
    public static void setup() throws Exception {
        String dir = Objects.requireNonNullElse(System.getenv("LOCAL_DIR"), "tmp/integration");
        ApplicationContext.init(new File("."), new File(dir), true);
    }

    @AfterClass
    public static void teardown() throws Exception {
        ApplicationContext.destroy();
    }

    @Before
    public void setupTest() throws Exception {
        cx = RequestContext.initLocal("test-viewer");
    }

    @After
    public void teardownTest() throws Exception {
        if (cx != null) {
            cx.close();
        }
    }

    @Test
    @SuppressWarnings("removal")
    public void testLoginToken() throws Exception {
        User user = cx.user();

        // Test JWT token
        String token = createLoginToken(user, System.currentTimeMillis() + 60000L);
        assertNotNull(token);
        assertTrue(token.contains("."));
        assertEquals(user, validateLoginToken(token));

        // Test legacy token
        String legacy = createAuthToken(user, System.currentTimeMillis() + 60000L);
        assertNotNull(legacy);
        assertFalse(legacy.contains("."));
        assertEquals(user, validateLoginToken(legacy));

        // Test invalid tokens
        assertThrows(SecurityException.class, () -> validateLoginToken("invalid"));
        assertThrows(SecurityException.class, () -> validateLoginToken("invalid.jwt.token"));
    }
}
