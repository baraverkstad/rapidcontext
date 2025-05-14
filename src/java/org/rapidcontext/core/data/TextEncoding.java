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

package org.rapidcontext.core.data;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Objects;

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
        return switch (encoding) {
            case ASCII -> encodeAscii(str, false);
            case PROPERTIES -> encodeProperty(str, false);
            case JSON -> encodeJson(str);
            case XML -> encodeXml(str, false);
            case URL -> encodeUrl(str);
            default -> str;
        };
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
        Objects.requireNonNullElse(str, "").chars().forEach(c -> {
            buffer.append(switch (c) {
                case '\n', '\r' -> linebreaks ? (char) c : ' ';
                default -> (32 <= c && c < 127) ? (char) c : ' ';
            });
        });
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
        Objects.requireNonNullElse(str, "").chars().forEach(c -> {
            int len = buffer.length();
            boolean initial = len == 0 || buffer.charAt(len - 1) == '\n';
            buffer.append(switch (c) {
                case '\\' -> "\\\\";
                case '\t' -> "\\t";
                case ' ' -> initial ? "\\ " : " ";
                case '\n' -> linebreaks ? "\\n\\\n" : "\\n";
                case '\r' -> linebreaks ? "" : "\\r";
                default -> (32 <= c && c < 127) ? (char) c : String.format("\\u%04x", c);
            });
        });
        return buffer.toString();
    }

    /**
     * Encodes a string into a JSON string literal. The output will
     * only contain printable ASCII, with other characters converted
     * to Unicode escape sequences.
     *
     * @param str            the text to encode, or null
     *
     * @return the encoded JSON string inside quotes, or
     *         the "null" string if the input was null
     */
    public static String encodeJson(String str) {
        if (str == null) {
            return "null";
        } else {
            StringBuilder buffer = new StringBuilder();
            buffer.append('"');
            Objects.requireNonNullElse(str, "").chars().forEach(c -> {
                buffer.append(switch (c) {
                    case '\\' -> "\\\\";
                    case '\"' -> "\\\"";
                    case '\t' -> "\\t";
                    case '\n' -> "\\n";
                    case '\r' -> "\\r";
                    default -> (32 <= c && c < 127) ? (char) c : String.format("\\u%04x", c);
                });
            });
            buffer.append('"');
            return buffer.toString();
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
        Objects.requireNonNullElse(str, "").codePoints().forEach(c -> {
            buffer.append(switch (c) {
                case '<' -> "&lt;";
                case '>' -> "&gt;";
                case '&' -> "&amp;";
                case '"' -> "&quot;";
                case '\n' -> linebreaks ? "\n" : "&#10;";
                case '\r' -> linebreaks ? "\r" : "&#13;";
                default -> (32 <= c && c < 127) ? (char) c : String.format("&#%d;", c);
            });
        });
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
