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

package org.rapidcontext.util;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class ValueUtilTest {

    @Test
    public void testIsBool() {
        assertFalse(ValueUtil.isBool(null));
        assertFalse(ValueUtil.isBool(""));
        assertFalse(ValueUtil.isBool("0"));
        assertFalse(ValueUtil.isBool("1"));
        assertFalse(ValueUtil.isBool("maybe"));

        assertTrue(ValueUtil.isBool("on"));
        assertTrue(ValueUtil.isBool("  On  "));
        assertTrue(ValueUtil.isBool("true"));
        assertTrue(ValueUtil.isBool("  TruE"));
        assertTrue(ValueUtil.isBool("yes"));
        assertTrue(ValueUtil.isBool("YES  "));

        assertTrue(ValueUtil.isBool("off"));
        assertTrue(ValueUtil.isBool("OFF"));
        assertTrue(ValueUtil.isBool("  false"));
        assertTrue(ValueUtil.isBool("FaLSE "));
        assertTrue(ValueUtil.isBool("no"));
        assertTrue(ValueUtil.isBool("nO"));
    }

    @Test
    public void testBool() {
        assertFalse(ValueUtil.bool(null, false));
        assertTrue(ValueUtil.bool("", true));
        assertTrue(ValueUtil.bool("maybe", true));

        assertTrue(ValueUtil.bool("1", false));
        assertTrue(ValueUtil.bool("oN", false));
        assertTrue(ValueUtil.bool("t", false));
        assertTrue(ValueUtil.bool("tRUe", false));
        assertTrue(ValueUtil.bool(" Y ", false));
        assertTrue(ValueUtil.bool("yes", false));

        assertFalse(ValueUtil.bool("0", true));
        assertFalse(ValueUtil.bool("oFF", true));
        assertFalse(ValueUtil.bool("f", true));
        assertFalse(ValueUtil.bool("falsE", true));
        assertFalse(ValueUtil.bool("n", true));
        assertFalse(ValueUtil.bool(" NO ", true));
    }

    @Test
    public void testConvert() {
        assertEquals(null, ValueUtil.convert(null));
        assertEquals("", ValueUtil.convert(""));
        assertEquals(true, ValueUtil.convert("on"));
        assertEquals(false, ValueUtil.convert("false"));
        assertEquals(true, ValueUtil.convert("yes"));
        assertEquals(0, ValueUtil.convert("0"));
        assertEquals(123, ValueUtil.convert("123"));
        assertEquals(-123, ValueUtil.convert("-123"));
        assertEquals(999999999, ValueUtil.convert("999999999"));
        assertEquals(1234567890L, ValueUtil.convert("1234567890"));
        assertEquals(1234567890L, ValueUtil.convert("+1234567890"));
        assertEquals(-1234567890L, ValueUtil.convert("-1234567890"));
        assertTrue(ValueUtil.convert("@123") instanceof java.util.Date);
    }

    @Test
    public void testConvertWithClass() {
        assertNull(ValueUtil.convert(null, String.class));
        assertNull(ValueUtil.convert(null, Boolean.class));
        assertNull(ValueUtil.convert(null, Integer.class));

        assertEquals("", ValueUtil.convert("", String.class));
        assertEquals("123", ValueUtil.convert(123, String.class));
        assertEquals("true", ValueUtil.convert(true, String.class));
        assertEquals("@1234567890", ValueUtil.convert(new Date(1234567890L), String.class));

        assertEquals(false, ValueUtil.convert("", Boolean.class));
        assertEquals(true, ValueUtil.convert("1", Boolean.class));
        assertEquals(true, ValueUtil.convert("on", Boolean.class));
        assertEquals(true, ValueUtil.convert("  true  ", Boolean.class));
        assertEquals(false, ValueUtil.convert("0", Boolean.class));
        assertEquals(false, ValueUtil.convert("OfF", Boolean.class));
        assertEquals(false, ValueUtil.convert("false", Boolean.class));
        assertEquals(true, ValueUtil.convert("anything-else", Boolean.class));

        assertEquals(Integer.valueOf(123), ValueUtil.convert("123", Integer.class));
        assertEquals(Integer.valueOf(0), ValueUtil.convert(" 0 ", Integer.class));
        assertEquals(Integer.valueOf(-456), ValueUtil.convert("-456", Integer.class));
        assertEquals(Long.valueOf(123L), ValueUtil.convert("123", Long.class));
        assertEquals(Long.valueOf(0L), ValueUtil.convert("0", Long.class));
        assertEquals(Long.valueOf(-456L), ValueUtil.convert(" -456  ", Long.class));

        long tm = 1234567890L;
        assertEquals(new Date(tm), ValueUtil.convert(tm, Date.class));
        assertEquals(new Date(tm), ValueUtil.convert((int) tm, Date.class));
        assertEquals(new Date(0), ValueUtil.convert("0", Date.class));
        assertEquals(new Date(tm), ValueUtil.convert("  @" + tm, Date.class));
    }

    @Test
    public void testConvertException() {
        assertThrows(NumberFormatException.class, () -> {
            ValueUtil.convert("not-a-number", Integer.class);
        });
        assertThrows(NumberFormatException.class, () -> {
            ValueUtil.convert("not-a-number", Long.class);
        });
        assertThrows(NumberFormatException.class, () -> {
            ValueUtil.convert("not-a-date", Date.class);
        });
    }
}
