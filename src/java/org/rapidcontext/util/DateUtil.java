/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2019 Per Cederberg. All rights reserved.
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

import org.apache.commons.lang3.time.FastDateFormat;

/**
 * A set of utility methods for handling date and time objects.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class DateUtil {

    /**
     * The ISO date format.
     */
    private static final FastDateFormat ISO_DATE_FORMAT =
        FastDateFormat.getInstance("yyyy-MM-dd");

    /**
     * The ISO date and time format.
     */
    private static final FastDateFormat ISO_DATETIME_FORMAT =
        FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    /**
     * The ISO time format.
     */
    private static final FastDateFormat ISO_TIME_FORMAT =
        FastDateFormat.getInstance("HH:mm:ss");

    /**
     * Formats a date to an ISO date representation. Note that the
     * time component of the date value will be ignored.
     *
     * @param date           the date to convert
     *
     * @return the ISO date string
     */
    public static String formatIsoDate(Date date) {
        return (date == null) ? null : ISO_DATE_FORMAT.format(date);
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
}
