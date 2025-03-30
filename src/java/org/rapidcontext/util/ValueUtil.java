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

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * A set of utility methods for simple value conversions.
 *
 * @author   Per Cederberg
 * @version  1.0
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
        return StringUtils.equalsAnyIgnoreCase(str, "on", "true", "yes", "off", "false", "no");
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
        if (StringUtils.equalsAnyIgnoreCase(str, "1", "on", "t", "true", "y", "yes")) {
            return true;
        } else if (StringUtils.equalsAnyIgnoreCase(str.trim(), "0", "off", "f", "false", "n", "no")) {
            return false;
        } else {
            return defaultValue;
        }
    }

    // No instances
    private ValueUtil() {}
}
