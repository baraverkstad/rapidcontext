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

import org.junit.Test;

@SuppressWarnings("javadoc")
public class RandomTest {

    @Test
    public void testBytes() {
        assertThrows(IllegalArgumentException.class, () -> Random.bytes(0));
        assertThrows(IllegalArgumentException.class, () -> Random.bytes(-1));
        byte[] data = Random.bytes(32);
        assertNotNull(data);
        assertEquals(32, data.length);
        assertNotEquals(data, Random.bytes(32));
        assertEquals(64, Random.bytes(64).length);
    }

    @Test
    public void testBase64() {
        assertThrows(IllegalArgumentException.class, () -> Random.base64(0));
        assertThrows(IllegalArgumentException.class, () -> Random.base64(-1));
        String data = Random.base64(32);
        assertNotNull(data);
        assertEquals(43, data.length());
        assertNotEquals(data, Random.base64(32));
    }
}
