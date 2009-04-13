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
 * See the RapidContext LICENSE.txt file for more details.
 */

package org.rapidcontext.util;

/**
 * A set of utility methods for handling strings and characters.
 *
 * @author   Per Cederberg, Dynabyte AB
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
     * Creates a Unicode escape sequence for a character.
     *
     * @param c              the character to convert
     *
     * @return the Unicode escape sequence
     */
    public static String escapeUnicode(char c) {
        StringBuffer  res = new StringBuffer();
        String        str;

        res.append("\\u");
        str = Integer.toHexString((c & 0xFF00) >>> 16);
        if (str.length() < 2) {
            res.append("0");
        }
        res.append(str);
        str = Integer.toHexString(c & 0xFF);
        if (str.length() < 2) {
            res.append("0");
        }
        res.append(str);
        return res.toString();
    }

    /**
     * Creates an XML-encoded string.
     *
     * @param str            the string to encode
     *
     * @return the XML encoded string
     */
    public static String escapeXml(String str) {
        StringBuffer  buffer = new StringBuffer();

        for (int i = 0; str != null && i < str.length(); i ++) {
            switch (str.charAt(i)) {
            case '<':
                buffer.append("&lt;");
                break;
            case '>':
                buffer.append("&gt;");
                break;
            case '&':
                buffer.append("&amp;");
                break;
            default:
                buffer.append(str.charAt(i));
                break;
            }
        }
        return buffer.toString();
    }
}
