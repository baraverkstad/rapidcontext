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

package org.rapidcontext.util;

import static org.junit.Assert.*;
import static org.rapidcontext.util.ValueUtil.*;

import java.util.Date;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class ValueUtilTest {

    @Test
    public void testIsBool() {
        assertFalse(isBool(null));
        assertFalse(isBool(""));
        assertFalse(isBool("0"));
        assertFalse(isBool("1"));
        assertFalse(isBool("maybe"));

        assertTrue(isBool("on"));
        assertTrue(isBool("  On  "));
        assertTrue(isBool("true"));
        assertTrue(isBool("  TruE"));
        assertTrue(isBool("yes"));
        assertTrue(isBool("YES  "));

        assertTrue(isBool("off"));
        assertTrue(isBool("OFF"));
        assertTrue(isBool("  false"));
        assertTrue(isBool("FaLSE "));
        assertTrue(isBool("no"));
        assertTrue(isBool("nO"));
    }

    @Test
    public void testBool() {
        assertFalse(bool(null, false));
        assertTrue(bool("", true));
        assertTrue(bool("maybe", true));

        assertTrue(bool("1", false));
        assertTrue(bool("oN", false));
        assertTrue(bool("t", false));
        assertTrue(bool("tRUe", false));
        assertTrue(bool(" Y ", false));
        assertTrue(bool("yes", false));

        assertFalse(bool("0", true));
        assertFalse(bool("oFF", true));
        assertFalse(bool("f", true));
        assertFalse(bool("falsE", true));
        assertFalse(bool("n", true));
        assertFalse(bool(" NO ", true));
    }

    @Test
    public void testConvert() {
        assertEquals(null, convert(null));
        assertEquals("", convert(""));
        assertEquals(true, convert("on"));
        assertEquals(false, convert("false"));
        assertEquals(true, convert("yes"));
        assertEquals(0, convert("0"));
        assertEquals(123, convert("123"));
        assertEquals(-123, convert("-123"));
        assertEquals(999999999, convert("999999999"));
        assertEquals(1234567890L, convert("1234567890"));
        assertEquals(1234567890L, convert("+1234567890"));
        assertEquals(-1234567890L, convert("-1234567890"));
        assertTrue(convert("@123") instanceof java.util.Date);
    }

    @Test
    public void testConvertWithClass() {
        assertNull(convert(null, String.class));
        assertNull(convert(null, Boolean.class));
        assertNull(convert(null, Integer.class));

        assertEquals("", convert("", String.class));
        assertEquals("123", convert(123, String.class));
        assertEquals("true", convert(true, String.class));
        assertEquals("@1234567890", convert(new Date(1234567890L), String.class));

        assertEquals(false, convert("", Boolean.class));
        assertEquals(true, convert("1", Boolean.class));
        assertEquals(true, convert("on", Boolean.class));
        assertEquals(true, convert("  true  ", Boolean.class));
        assertEquals(false, convert("0", Boolean.class));
        assertEquals(false, convert("OfF", Boolean.class));
        assertEquals(false, convert("false", Boolean.class));
        assertEquals(true, convert("anything-else", Boolean.class));

        assertEquals(Integer.valueOf(123), convert("123", Integer.class));
        assertEquals(Integer.valueOf(0), convert(" 0 ", Integer.class));
        assertEquals(Integer.valueOf(-456), convert("-456", Integer.class));
        assertEquals(Long.valueOf(123L), convert("123", Long.class));
        assertEquals(Long.valueOf(0L), convert("0", Long.class));
        assertEquals(Long.valueOf(-456L), convert(" -456  ", Long.class));

        long tm = 1234567890L;
        assertEquals(new Date(tm), convert(tm, Date.class));
        assertEquals(new Date(tm), convert((int) tm, Date.class));
        assertEquals(new Date(0), convert("0", Date.class));
        assertEquals(new Date(tm), convert("  @" + tm, Date.class));
    }

    @Test
    public void testConvertException() {
        assertThrows(NumberFormatException.class, () -> {
            convert("not-a-number", Integer.class);
        });
        assertThrows(NumberFormatException.class, () -> {
            convert("not-a-number", Long.class);
        });
        assertThrows(NumberFormatException.class, () -> {
            convert("not-a-date", Date.class);
        });
    }
}
