/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2022 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.data;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.lang3.CharUtils;

/**
 * A text encoding/escaping helper. Always encodes to printable ASCII
 * (7-bit) characters, making it possible to use the output directly.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public enum TextEncoding {

    /**
     * No encoding (i.e. raw pass-through)
     */
    NONE,

    /**
     * ASCII printable encoding. Replaces any non-printable ASCII
     * (i.e not 7-bit ASCII) or control characters with spaces.
     */
    ASCII,

    /**
     * Java properties encoding. Replaces any non-printable ASCII
     * (i.e not 7-bit ASCII) characters with Unicode escape sequences.
     */
    PROPERTIES,

    /**
     * JSON string encoding. Escapes newlines, tabs, backslashes and
     * double quotes. Also, any character outside the range 0x20-0x7f
     * will be replaced with a Unicode escape sequence.
     */
    JSON,

    /**
     * XML string encoding. This encoding escapes all non-printable
     * ASCII (i.e not 7-bit ASCII) characters with numeric XML entities.
     */
    XML,

    /**
     * The "application/x-www-form-urlencoded" encoding, i.e. for
     * URLs and form data. Replaces any non-printable ASCII (i.e not
     * 7-bit ASCII) characters with numeric UTF-8 %-sequences.
     */
    URL;

    /**
     * Encodes a string with the specified encoding.
     *
     * @param encoding       the encoding to use
     * @param str            the text to encode
     *
     * @return the encoded text string, or
     *         an empty string if input was null
     */
    public static String encode(TextEncoding encoding, String str) {
        switch (encoding) {
        case ASCII:
            return encodeAscii(str, false);
        case PROPERTIES:
            return encodeProperty(str, false);
        case JSON:
            return encodeJson(str);
        case XML:
            return encodeXml(str, false);
        case URL:
            return encodeUrl(str);
        default:
            return str;
        }
    }

    /**
     * Encodes a string to ASCII text. The output will only contain
     * printable ASCII with an option to keep newlines. All other
     * characters will be converted to spaces.
     *
     * @param str            the text to encode
     * @param linebreaks     the flag for preserving line breaks
     *
     * @return the encoded ASCII text string, or
     *         an empty string if the input was null
     */
    public static String encodeAscii(String str, boolean linebreaks) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; str != null && i < str.length(); i ++) {
            char c = str.charAt(i);
            switch (c) {
            case '\n':
            case '\r':
                buffer.append(linebreaks ? c : ' ');
                break;
            default:
                buffer.append(CharUtils.isAsciiPrintable(c) ? c : ' ');
            }
        }
        return buffer.toString();
    }

    /**
     * Encodes a string to a escaped property value. The output will
     * only contain printable ASCII with an option to keep newlines.
     * All other characters will be converted to Unicode escape
     * sequences.
     *
     * @param str            the text to encode
     * @param linebreaks     the flag for preserving line breaks
     *
     * @return the encoded property text string, or
     *         an empty string if the input was null
     */
    public static String encodeProperty(String str, boolean linebreaks) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; str != null && i < str.length(); i ++) {
            char c = str.charAt(i);
            switch (c) {
            case '\\':
                buffer.append("\\\\");
                break;
            case '\t':
                buffer.append("\\t");
                break;
            case ' ':
                if (buffer.lastIndexOf("\n") == buffer.length() - 1) {
                    buffer.append("\\");
                }
                buffer.append(" ");
                break;
            case '\n':
                buffer.append("\\n");
                if (linebreaks) {
                    buffer.append("\\\n");
                }
                break;
            case '\r':
                if (!linebreaks) {
                    buffer.append("\\r");
                }
                break;
            default:
                if (CharUtils.isAsciiPrintable(c)) {
                    buffer.append(c);
                } else {
                    buffer.append(String.format("\\u%04x", Integer.valueOf(c)));
                }
            }
        }
        return buffer.toString();
    }

    /**
     * Encodes a string into a JSON string literal. The output will
     * only contain printable ASCII, with other characters converted
     * to Unicode escape sequences.
     *
     * @param str            the text to encode
     *
     * @return the encoded JSON string
     */
    public static String encodeJson(String str) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; str != null && i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
            case '\\':
                buffer.append("\\\\");
                break;
            case '\"':
                buffer.append("\\\"");
                break;
            case '\t':
                buffer.append("\\t");
                break;
            case '\n':
                buffer.append("\\n");
                break;
            case '\r':
                buffer.append("\\r");
                break;
            default:
                if (32 <= c && c < 127) {
                    buffer.append(c);
                } else {
                    buffer.append(String.format("\\u%04x", Integer.valueOf(c)));
                }
            }
        }
        return buffer.toString();
    }

    /**
     * Encodes a string into a JSON string literal. The output will
     * only contain printable ASCII, with other characters converted
     * to Unicode escape sequences.
     *
     * @param str            the text to encode, or null
     *
     * @return the encoded JSON string, or
     *         "null" if null
     */
    public static String encodeJsonString(String str) {
        if (str == null) {
            return "null";
        } else {
            return '"' + encodeJson(str) + '"';
        }
    }

    /**
     * Encodes a string to properly escaped XML. The output will only
     * contain printable ASCII with an option to keep newlines. All
     * other characters will be converted to numeric XML entities.
     *
     * @param str            the text to encode
     * @param linebreaks     the flag for preserving line breaks
     *
     * @return the encoded XML string, or
     *         an empty string if the input was null
     */
    public static String encodeXml(String str, boolean linebreaks) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; str != null && i < str.length(); i ++) {
            char c = str.charAt(i);
            switch (c) {
            case '<':
                buffer.append("&lt;");
                break;
            case '>':
                buffer.append("&gt;");
                break;
            case '&':
                buffer.append("&amp;");
                break;
            case '"':
                buffer.append("&quot;");
                break;
            case '\n':
                buffer.append(linebreaks ? "\n" : "&#10;");
                break;
            case '\r':
                buffer.append(linebreaks ? "\r" : "&#13;");
                break;
            default:
                if (CharUtils.isAsciiPrintable(c)) {
                    buffer.append(c);
                } else {
                    buffer.append("&#");
                    buffer.append(String.valueOf((int) c));
                    buffer.append(";");
                }
            }
        }
        return buffer.toString();
    }

    /**
     * Encodes a string for URLs or form data, i.e. the MIME type
     * "application/x-www-form-urlencoded". The output will only
     * contain printable ASCII. All other characters will be
     * converted to numeric UTF-8 %-sequences.
     *
     * @param str            the text to encode
     *
     * @return the encoded string, or
     *         an empty string if the input was null
     */
    public static String encodeUrl(String str) {
        if (str != null) {
            try {
                return URLEncoder.encode(str, "utf8");
            } catch (UnsupportedEncodingException ignore) {
                // Nothing here, UTF-8 is supported
            }
        }
        return "";
    }
}
