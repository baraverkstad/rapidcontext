/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2012 Per Cederberg. All rights reserved.
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
 * A set of utility methods for handling strings and characters.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StringUtil {

    /**
     * Returns the first non-empty value of the argument strings.
     *
     * @param str1           the first string to check
     * @param str2           the second string to check
     * @param str3           the third string to check
     *
     * @return the first non-empty value of the argument strings, or
     *         the last string value
     */
    public static String first(String str1, String str2, String str3) {
        if (str1 != null && str1.trim().length() > 0) {
            return str1;
        } else if (str2 != null && str2.trim().length() > 0) {
            return str2;
        } else {
            return str3;
        }
    }

    /**
     * Returns the first match of a regular expression from a string.
     *
     * @param str           the string to match
     * @param re            the compiled regular expression
     *
     * @return the matched substring, or
     *         null if no match was found
     */
    public static String match(String str, Pattern re) {
        if (str == null) {
            return null;
        }
        Matcher m = re.matcher(str);
        return (m != null && m.find()) ? m.group() : null;
    }
}
