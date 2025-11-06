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
public class DateUtilTest {

    @Test
    public void testIsEpochFormat() {
        assertFalse(DateUtil.isEpochFormat(null));
        assertFalse(DateUtil.isEpochFormat(""));
        assertTrue(DateUtil.isEpochFormat("@1234567890"));
        assertTrue(DateUtil.isEpochFormat("@0"));
        assertTrue(DateUtil.isEpochFormat("@999999999999"));
        assertTrue(DateUtil.isEpochFormat("@-9999999999999"));
        assertFalse(DateUtil.isEpochFormat("1234567890")); // Missing @
        assertFalse(DateUtil.isEpochFormat("@")); // No number
        assertFalse(DateUtil.isEpochFormat("@abc")); // Non-numeric
        assertFalse(DateUtil.isEpochFormat("@123abc")); // Mixed
        assertFalse(DateUtil.isEpochFormat("@123456789012345")); // Too long
    }

    @Test
    public void testAsEpochMillis() {
        assertNull(DateUtil.asEpochMillis(null));
        assertEquals("@0", DateUtil.asEpochMillis(new Date(0L)));
        Date now = new Date();
        assertEquals("@" + now.getTime(), DateUtil.asEpochMillis(now));
        long tm = 12345678901234L;
        assertEquals("@" + tm, DateUtil.asEpochMillis(new Date(tm)));
        assertEquals("@-" + tm, DateUtil.asEpochMillis(new Date(-tm)));
    }

    @Test
    public void testAsDateTimeUTC() {
        assertNull(DateUtil.asDateTimeUTC(null));
        assertEquals("2009-02-13T23:31:30.123Z", DateUtil.asDateTimeUTC(new Date(1234567890123L)));
    }

    @Test
    public void testFormatIsoDateTime() {
        assertNull(DateUtil.formatIsoDateTime(null));
        String str = DateUtil.formatIsoDateTime(new Date(1234567890123L));
        assertTrue(str.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$"));
        assertTrue(str.contains("2009"));
        assertFalse(str.contains("Z"));
    }

    @Test
    public void testFormatIsoTime() {
        assertNull(DateUtil.formatIsoTime(null));
        String str = DateUtil.formatIsoTime(new Date(1234567890123L));
        assertTrue(str.matches("^\\d{2}:\\d{2}:\\d{2}$"));
        assertFalse(str.contains("2009"));
        assertFalse(str.contains("Z"));
    }
}
