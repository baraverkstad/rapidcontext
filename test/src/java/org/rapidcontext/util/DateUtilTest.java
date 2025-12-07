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
import static org.rapidcontext.util.DateUtil.*;

import java.util.Date;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class DateUtilTest {

    @Test
    public void testIsEpochFormat() {
        assertFalse(isEpochFormat(null));
        assertFalse(isEpochFormat(""));
        assertTrue(isEpochFormat("@1234567890"));
        assertTrue(isEpochFormat("@0"));
        assertTrue(isEpochFormat("@999999999999"));
        assertTrue(isEpochFormat("@-9999999999999"));
        assertFalse(isEpochFormat("1234567890")); // Missing @
        assertFalse(isEpochFormat("@")); // No number
        assertFalse(isEpochFormat("@abc")); // Non-numeric
        assertFalse(isEpochFormat("@123abc")); // Mixed
        assertFalse(isEpochFormat("@123456789012345")); // Too long
    }

    @Test
    public void testAsEpochMillis() {
        assertNull(asEpochMillis(null));
        assertEquals("@0", asEpochMillis(new Date(0L)));
        Date now = new Date();
        assertEquals("@" + now.getTime(), asEpochMillis(now));
        long tm = 12345678901234L;
        assertEquals("@" + tm, asEpochMillis(new Date(tm)));
        assertEquals("@-" + tm, asEpochMillis(new Date(-tm)));
    }

    @Test
    public void testAsDateTimeUTC() {
        assertNull(asDateTimeUTC(null));
        assertEquals("2009-02-13T23:31:30.123Z", asDateTimeUTC(new Date(1234567890123L)));
    }

    @Test
    public void testFormatIsoDateTime() {
        assertNull(formatIsoDateTime(null));
        String str = formatIsoDateTime(new Date(1234567890123L));
        assertTrue(str.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$"));
        assertTrue(str.contains("2009"));
        assertFalse(str.contains("Z"));
    }

    @Test
    public void testFormatIsoTime() {
        assertNull(formatIsoTime(null));
        String str = formatIsoTime(new Date(1234567890123L));
        assertTrue(str.matches("^\\d{2}:\\d{2}:\\d{2}$"));
        assertFalse(str.contains("2009"));
        assertFalse(str.contains("Z"));
    }
}
