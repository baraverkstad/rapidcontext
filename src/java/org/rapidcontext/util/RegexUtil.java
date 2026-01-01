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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A set of utility methods for handling regular expressions.
 *
 * @author Per Cederberg
 */
public final class RegexUtil {

    /**
     * Returns the first match for a regular expression.
     *
     * @param re            the compiled regex pattern
     * @param str           the string to match
     *
     * @return the matched substring, or
     *         null if no match was found
     */
    public static String firstMatch(Pattern re, String str) {
        if (str == null) {
            return null;
        }
        Matcher m = re.matcher(str);
        return (m != null && m.find()) ? m.group() : null;
    }

    /**
     * Returns the first match from an array of regular expressions.
     *
     * @param arr           the array of compiled regex patterns
     * @param str           the string to match
     *
     * @return the matched substring, or
     *         null if no match was found
     */
    public static String firstMatch(Pattern[] arr, String str) {
        if (str == null) {
            return null;
        }
        for (Pattern re : arr) {
            Matcher m = re.matcher(str);
            if (m != null && m.find()) {
                return m.group();
            }
        }
        return null;
    }

    /**
     * Converts a glob expression to a regular expression. Handles
     * the special characters '?', '*' and '**'.
     *
     * @param glob           the glob to convert
     *
     * @return the corresponding regular expression
     */
    public static String fromGlob(String glob) {
        String re = "\\Q" + glob.replace("\\E", "\\E\\\\E\\Q") + "\\E";
        re = re.replace("?", "\\E[^/]\\Q");
        re = re.replace("*", "\\E[^/]*\\Q");
        re = re.replace("\\E[^/]*\\Q\\E[^/]*\\Q", "\\E.*\\Q"); // "**"
        return re;
    }

    // No instances
    private RegexUtil() {}
}
