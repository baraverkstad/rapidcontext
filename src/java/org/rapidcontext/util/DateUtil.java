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

import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang3.time.FastDateFormat;

/**
 * A set of utility methods for handling date and time objects.
 *
 * @author Per Cederberg
 */
public final class DateUtil {

    // The UTC time zone
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    // The UTC date and time format
    private static final FastDateFormat UTC_DATETIME =
        FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", UTC);

    // The ISO date and time format.
    private static final FastDateFormat ISO_DATETIME_FORMAT =
        FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    // The ISO time format.
    private static final FastDateFormat ISO_TIME_FORMAT =
        FastDateFormat.getInstance("HH:mm:ss");

    /**
     * Checks if a string is in Unix epoch format. Note that there is
     * no safe way to distinguish between epoch values in second vs.
     * millisecond resolution for all cases.
     *
     * @param str            the string to test
     *
     * @return true if the string is in epoch format, or
     *         false otherwise
     */
    public static boolean isEpochFormat(String str) {
        return str != null && str.matches("^@-?\\d{1,14}$");
    }

    /**
     * Formats a date and time in Unix (millisecond) epoch format
     * (i.e. "@1653037430316").
     *
     * @param date           the date and time to convert
     *
     * @return the epoch millisecond string
     */
    public static String asEpochMillis(Date date) {
        return (date == null) ? null : "@" + date.getTime();
    }

    /**
     * Formats a date and time in an ISO 8601 datetime representation
     * for the UTC timezone.
     *
     * @param date           the date and time to convert
     *
     * @return the ISO 8601 datetime string in UTC
     */
    public static String asDateTimeUTC(Date date) {
        return (date == null) ? null : UTC_DATETIME.format(date);
    }

    /**
     * Formats a date and time to an ISO datetime representation
     * (without timezone).
     *
     * @param date           the date and time to convert
     *
     * @return the ISO datetime string
     */
    public static String formatIsoDateTime(Date date) {
        return (date == null) ? null : ISO_DATETIME_FORMAT.format(date);
    }

    /**
     * Formats a time to an ISO time representation (without
     * timezone).
     *
     * @param date           the date and time to convert
     *
     * @return the ISO time string
     */
    public static String formatIsoTime(Date date) {
        return (date == null) ? null : ISO_TIME_FORMAT.format(date);
    }

    // No instances
    private DateUtil() {}
}
