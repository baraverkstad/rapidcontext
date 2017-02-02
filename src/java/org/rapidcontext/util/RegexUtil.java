/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2017 Per Cederberg. All rights reserved.
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
 * @author   Per Cederberg
 * @version  1.0
 */
public class RegexUtil {

    /**
     * Returns the first match of a regular expression from a string.
     *
     * @param re            the compiled regex
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
}
