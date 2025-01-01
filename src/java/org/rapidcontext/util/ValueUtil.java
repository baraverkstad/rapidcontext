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

import org.apache.commons.lang3.StringUtils;

/**
 * A set of utility methods for simple value conversions.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public final class ValueUtil {

    /**
     * Checks if a string value represents an "on" value. Typical
     * values are "1", "on", "true", etc (case-insensitive).
     *
     * @param str            the value to check
     *
     * @return true if the string matches an "on" value, or
     *         false otherwise
     */
    public static boolean isOn(String str) {
        return StringUtils.equalsAnyIgnoreCase(str, "1", "on", "t", "true", "y", "yes");
    }

    /**
     * Checks if a string value represents an "off" value. Typical
     * values are "0", "off", "false", etc (case-insensitive).
     *
     * @param str            the value to check
     *
     * @return true if the string matches an "off" value, or
     *         false otherwise
     */
    public static boolean isOff(String str) {
        return StringUtils.equalsAnyIgnoreCase(str, "0", "off", "f", "false", "n", "no");
    }

    // No instances
    private ValueUtil() {}
}
