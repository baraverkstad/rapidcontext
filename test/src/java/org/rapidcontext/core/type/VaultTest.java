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

package org.rapidcontext.core.type;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rapidcontext.core.data.Dict;

public class VaultTest {

    @Before
    public void setUp() throws Exception {
        Dict cfg = new Dict()
            .set("type", "config")
            .set("env", "prod")
            .set("region", "eu");
        Dict user = new Dict()
            .set("type", "user")
            .set("name", "Alice")
            .set("age", "30");
        Map<String, Vault> cache = vaultCache();
        cache.clear();
        cache.put("cfg", new TestVault("global", true, cfg));
        cache.put("user", new TestVault("user", false, user));
    }

    @After
    public void tearDown() throws Exception {
        vaultCache().clear();
    }

    @Test
    public void testLookup() throws Exception {
        assertNull(Vault.lookup(null, null));
        assertNull(Vault.lookup(null, ""));
        assertNull(Vault.lookup(null, "missing"));
        assertNull(Vault.lookup("cfg", "missing"));
        assertNull(Vault.lookup("user", null));
        assertNull(Vault.lookup("user", ""));
        assertNull(Vault.lookup("user", "missing"));
        assertEquals("config", Vault.lookup(null, "type"));
        assertEquals("config", Vault.lookup("", "type"));
        assertEquals("config", Vault.lookup("cfg", "type"));
        assertEquals("user", Vault.lookup("user", "type"));
        assertNull(Vault.lookup("missing", "type"));
        assertNull(Vault.lookup(null, "name"));
        assertNull(Vault.lookup("", "name"));
        assertNull(Vault.lookup("cfg", "name"));
        assertEquals("Alice", Vault.lookup("user", "name"));
    }

    @Test
    public void testExpand() throws Exception {
        assertEquals("no vars", Vault.expand("no vars"));
        assertEquals("Hello Alice", Vault.expand("Hello ${{user!name}}"));
        assertEquals("Age: 30 (user)", Vault.expand("Age: ${{user!age}} (${{user!type}})"));
        assertEquals("Env: prod (eu)", Vault.expand("Env: ${{env}} (${{region}})"));
        assertEquals("{config} + user", Vault.expand("{${{type}}} + ${{user!type}}"));
        assertEquals("Missing: ${{missing}}", Vault.expand("Missing: ${{missing}}"));
        assertEquals("Default: N/A", Vault.expand("Default: ${{missing!type:N/A}}"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Vault> vaultCache() throws Exception {
        Field field = Vault.class.getDeclaredField("cache");
        field.setAccessible(true);
        return (Map<String, Vault>) field.get(null);
    }


    private static class TestVault extends Vault {

        private Dict data;

        public TestVault(String id, boolean global, Dict data) {
            super(id, "vault", new Dict().set(KEY_ID, id).set(KEY_TYPE, "vault"));
            dict.set(KEY_GLOBAL, global);
            this.data = data;
        }

        @Override
        public String lookup(String key) {
            return data.get(key, String.class);
        }
    }
}

