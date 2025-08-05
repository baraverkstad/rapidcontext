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
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

/**
 * A set of utility methods for simple value conversions.
 *
 * @author Per Cederberg
 */
public final class ValueUtil {

    /**
     * Checks if a string value looks like a boolean. The following
     * strings (case-insensitive) are considered boolean values:
     * "on", "true", "yes", "off", "false", and "no".
     *
     * @param str            the value to check
     *
     * @return true if the string looks like a boolean, or
     *         false otherwise
     */
    public static boolean isBool(String str) {
        str = Objects.requireNonNullElse(str, "").trim();
        return Strings.CI.equalsAny(str, "on", "true", "yes", "off", "false", "no");
    }

    /**
     * Converts a string to a boolean by checking for "on" and "off"
     * values. Comparison is case-insensitive and also ignores any
     * leading or trailing whitespace characters. Typical "on" values
     * are "1", "on", "true", etc. Typical "off" values are "0", "off",
     * "false", etc.
     *
     * @param str            the value to check
     * @param defaultValue   the default value if not matched
     *
     * @return true if the string matches an "on" value,
     *         false if the string matches an "off" value, or
     *         the default value otherwise
     */
    public static boolean bool(String str, boolean defaultValue) {
        str = Objects.requireNonNullElse(str, "").trim();
        if (Strings.CI.equalsAny(str, "1", "on", "t", "true", "y", "yes")) {
            return true;
        } else if (Strings.CI.equalsAny(str.trim(), "0", "off", "f", "false", "n", "no")) {
            return false;
        } else {
            return defaultValue;
        }
    }

    /**
     * Converts a string into a value type based on its content.
     * Should properly detect boolean, numeric and epoch datetime
     * values. All remaining values will be returned as-is.
     *
     * @param value          the string to convert
     *
     * @return the converted value
     *
     * @see #isBool
     * @see DateUtil#isEpochFormat
     */
    public static Object convert(String value) {
        if (ValueUtil.isBool(value)) {
            return ValueUtil.bool(value, !value.isBlank());
        } else if (value.length() > 0 && value.length() <= 9 && StringUtils.isNumeric(value)) {
            return Integer.valueOf(value);
        } else if (DateUtil.isEpochFormat(value)) {
            return new Date(Long.parseLong(value.substring(1)));
        } else {
            return value;
        }
    }

    /**
     * Converts a value to a specified object class. The value is
     * either converted (as below) or casted.
     *
     * <p>If the object class is String (and the value isn't), the
     * string representation will be returned. Any Date object will
     * instead be converted to "\@millis".</p>
     *
     * <p>If the object class is Boolean (and the value isn't), the
     * string representation that does not equal "", "0", "f",
     * "false", "no" or "off" is considered true.</p>
     *
     * <p>If the object class is Integer or Long (and the value
     * isn't), a numeric conversion of the string representation will
     * be attempted.</p>
     *
     * <p>If the object class is Date (and the value isn't), a number
     * conversion (to milliseconds) of the string representation
     * (excluding any '@' prefix) will be attempted.</p>
     *
     * @param <T>            the object type to return
     * @param value          the object value
     * @param clazz          the object class
     *
     * @return the converted or casted value
     *
     * @throws ClassCastException if the wasn't possible to cast to
     *             the specified object class
     * @throws NumberFormatException if the value wasn't possible to
     *             parse as a number
     */
    @SuppressWarnings("unchecked")
    public static <T> T convert(Object value, Class<T> clazz) {
        if (value == null || clazz.isInstance(value)) {
            return (T) value;
        } else if (clazz.equals(String.class) && value instanceof Date dt) {
            return (T) DateUtil.asEpochMillis(dt);
        } else if (clazz.equals(String.class)) {
            return (T) value.toString();
        } else if (clazz.equals(Boolean.class)) {
            String str = value.toString();
            return (T) Boolean.valueOf(ValueUtil.bool(str, !str.isBlank()));
        } else if (clazz.equals(Integer.class)) {
            return (T) Integer.valueOf(value.toString());
        } else if (clazz.equals(Long.class)) {
            return (T) Long.valueOf(value.toString());
        } else if (clazz.equals(Date.class) && value instanceof Number n) {
            long millis = n.longValue();
            return (T) new Date(millis);
        } else if (clazz.equals(Date.class)) {
            String str = value.toString();
            if (str.startsWith("@")) {
                str = str.substring(1);
            }
            return (T) new Date(Long.parseLong(str));
        } else {
            return (T) value; // throws ClassCastException
        }
    }

    // No instances
    private ValueUtil() {}
}
