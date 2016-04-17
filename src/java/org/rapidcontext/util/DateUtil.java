/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2009 Per Cederberg & Dynabyte AB.
 * All rights reserved.
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A set of utility methods for handling date and time objects.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class DateUtil {

    /**
     * The ISO date format.
     */
    private static final SimpleDateFormat ISO_DATE_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd");

    /**
     * The ISO date and time format.
     */
    private static final SimpleDateFormat ISO_DATETIME_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * The ISO time format.
     */
    private static final SimpleDateFormat ISO_TIME_FORMAT =
        new SimpleDateFormat("HH:mm:ss");

    /**
     * Converts an ISO date or datetime string to a date object.
     * Note that this method will omit the time portion of the
     * string if not available.
     *
     * @param str            the string to convert
     *
     * @return the date object, or
     *         null if the format wasn't recognized
     */
    public static Date parseIsoDate(String str) {
        Date  date;

        try {
            date = ISO_DATETIME_FORMAT.parse(str);
            if (formatIsoDateTime(date).equals(str)) {
                return date;
            }
        } catch (ParseException e) {
            // Do nothing, try with another date format
        }
        try {
            date = ISO_DATE_FORMAT.parse(str);
            if (formatIsoDate(date).equals(str)) {
                return date;
            }
        } catch (ParseException e) {
            // Do nothing, try with another date format
        }
        return null;
    }

    /**
     * Formats a date to an ISO date representation. Note that the
     * time component of the date value will be ignored.
     *
     * @param date           the date to convert
     *
     * @return the ISO date string
     */
    public static String formatIsoDate(Date date) {
        if (date == null) {
            return null;
        } else {
            return ISO_DATE_FORMAT.format(date);
        }
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
        if (date == null) {
            return null;
        } else {
            return ISO_DATETIME_FORMAT.format(date);
        }
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
        if (date == null) {
            return null;
        } else {
            return ISO_TIME_FORMAT.format(date);
        }
    }
}
