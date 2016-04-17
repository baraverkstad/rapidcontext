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

import org.apache.commons.lang.StringUtils;

/**
 * A set of utility methods for handling strings and characters.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StringUtil {

    /**
     * Checks if a character belongs to the printable ASCII set.
     * Any character code between 32 and 126 is considered inside
     * this set.
     *
     * @param c              the character to check
     *
     * @return true if the character is printable ASCII, or
     *         false otherwise
     */
    public static boolean isAscii(char c) {
        return 0x20 <= c && c <= 0x7e;
    }

    /**
     * Checks if all characters in a string belongs to the printable
     * ASCII set. Any character code between 32 and 126 is
     * considered inside this set.
     *
     * @param str            the string to check
     *
     * @return true if the string is printable ASCII, or
     *         false otherwise
     */
    public static boolean isAscii(String str) {
        if (str == null) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!isAscii(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a character belongs to the printable ISO-8859-1
     * set. Any character in the printable ASCII set or with a
     * character code between 160 and 255 in considered inside
     * this set.
     *
     * @param c              the character to check
     *
     * @return true if the character is printable ISO-8859-1, or
     *         false otherwise
     */
    public static boolean isIsoLatin1(char c) {
        return isAscii(c) || (0xA0 <= c && c <= 0xFF);
    }

    /**
     * Checks if all characters in a string belongs to the printable
     * ISO-8859-1 set. Any character in the printable ASCII set or
     * with a character code between 160 and 255 in considered
     * inside this set.
     *
     * @param str            the string to check
     *
     * @return true if the string is printable ISO-8859-1, or
     *         false otherwise
     */
    public static boolean isIsoLatin1(String str) {
        if (str == null) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!isIsoLatin1(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a character is a numeric ASCII character.
     *
     * @param c              the character to check
     *
     * @return true if the character is a number, or
     *         false otherwise
     */
    public static boolean isNumber(char c) {
        return 0x30 <= c && c <= 0x39;
    }

    /**
     * Checks if all characters in a string are numeric ASCII
     * characters. The string must also be at least one character
     * long and no whitespace characters are accepted.
     *
     * @param str            the string to check
     *
     * @return true if the string is numeric, or
     *         false otherwise
     */
    public static boolean isNumber(String str) {
        if (str == null || str.length() <= 0) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!isNumber(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a 4 digit hexadecimal representation of a character.
     *
     * @param chr            the character to convert
     *
     * @return the hexadecimal character code
     */
    public static String toHex(char chr) {
        int high = (chr & 0xFF00) >>> 16;
        int low = chr & 0xFF;
        return StringUtils.leftPad(Integer.toHexString(high), 2, '0') +
               StringUtils.leftPad(Integer.toHexString(low), 2, '0');
    }

    /**
     * Returns an escaped property file value for the specified input
     * character. This method will return character escapes for
     * backslash, tab and newline. It will also return a Unicode
     * escape for any non-ASCII character.
     *
     * @param chr            the character to convert
     *
     * @return the valid property text string
     */
    public static String escapeProperty(char chr) {
        switch (chr) {
        case '\\':
            return "\\\\";
        case '\t':
            return "\\t";
        case '\n':
            return "\\n";
        default:
            if (isAscii(chr)) {
                return "" + chr;
            } else {
                return "\\u" + toHex(chr);
            }
        }
    }

    /**
     * Returns an escaped property file value for the specified input
     * string. This method will return character escapes for
     * backslash, tab and newline. It will also return a Unicode
     * escape for any non-ASCII character.
     *
     * @param str            the string to convert
     *
     * @return the valid property text string
     */
    public static String escapeProperty(String str) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; str != null && i < str.length(); i ++) {
            buffer.append(escapeProperty(str.charAt(i)));
        }
        return buffer.toString();
    }

    /**
     * Returns properly escaped XML for the specified input character.
     * This method will return an XML character entities for all
     * non-ASCII characters as well as the reserved XML characters.
     *
     * @param chr            the character to convert
     *
     * @return the valid XML string
     */
    public static String escapeXml(char chr) {
        switch (chr) {
        case '<':
            return "&lt;";
        case '>':
            return "&gt;";
        case '&':
            return "&amp;";
        case '"':
            return "&quot;";
        default:
            if (isAscii(chr)) {
                return "" + chr;
            } else {
                return "&#" + ((int) chr) + ";";
            }
        }
    }

    /**
     * Returns properly escaped XML for the specified input string.
     * This method will add XML character entities for all non-ASCII
     * characters as well as the reserved XML characters.
     *
     * @param str            the string to convert
     *
     * @return the valid XML string
     */
    public static String escapeXml(String str) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; str != null && i < str.length(); i ++) {
            buffer.append(escapeXml(str.charAt(i)));
        }
        return buffer.toString();
    }

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
