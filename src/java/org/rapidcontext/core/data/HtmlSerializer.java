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

package org.rapidcontext.core.data;

import java.util.Date;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.util.DateUtil;

/**
 * A data serializer for HTML. This class only attempts to render a
 * human-readable version of a data object, without any efforts of
 * making the result machine readable. It is only useful for
 * debugging or similar. The following basic requirements must be met
 * in order to serialize an object:
 *
 * <ul>
 *   <li>No circular references are permitted.
 *   <li>String, Integer, Boolean, Array and Dict objects are supported.
 * </ul>
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class HtmlSerializer {

    /**
     * Serializes an object into an HTML representation. The string
     * returned can be used (without escaping) inside an HTML page.
     *
     * @param obj            the object to convert, or null
     *
     * @return an HTML representation
     */
    public static String serialize(Object obj) {
        StringBuilder  buffer = new StringBuilder();

        serialize(obj, buffer);
        return buffer.toString();
    }

    /**
     * Serializes an object into an HTML representation.
     *
     * @param obj            the object to convert
     * @param buffer         the string buffer to append into
     */
    private static void serialize(Object obj, StringBuilder buffer) {
        if (obj == null) {
            buffer.append("<code>N/A</code>");
        } else if (obj instanceof Dict) {
            serialize((Dict) obj, buffer);
        } else if (obj instanceof Array) {
            serialize((Array) obj, buffer);
        } else if (obj instanceof Date) {
            buffer.append(DateUtil.formatIsoDateTime((Date) obj));
        } else if (obj instanceof Class) {
            serialize(((Class) obj).getName(), buffer);
        } else if (obj instanceof StorableObject) {
            serialize(((StorableObject) obj).serialize(), buffer);
        } else {
            serialize(obj.toString(), buffer);
        }
    }

    /**
     * Serializes a dictionary into an HTML representation.
     *
     * @param dict           the dictionary to convert
     * @param buffer         the string buffer to append into
     */
    private static void serialize(Dict dict, StringBuilder buffer) {
        String[]  keys = dict.keys();

        buffer.append("<table>\n<tbody>\n");
        for (int i = 0; i < keys.length; i++) {
            buffer.append("<tr>\n<th>");
            serialize(keys[i], buffer);
            buffer.append("</th>\n<td>");
            serialize(dict.get(keys[i]), buffer);
            buffer.append("</td>\n</tr>\n");
        }
        buffer.append("</tbody>\n</table>\n");
    }

    /**
     * Serializes an array into an HTML representation.
     *
     * @param arr            the array to convert
     * @param buffer         the string buffer to append into
     */
    private static void serialize(Array arr, StringBuilder buffer) {
        buffer.append("<ol>\n");
        for (int i = 0; i < arr.size(); i++) {
            buffer.append("<li>");
            serialize(arr.get(i), buffer);
            buffer.append("</li>\n");
        }
        buffer.append("</ol>\n");
    }

    /**
     * Serializes a text string into an HTML representation. If the
     * string contains a newline character, it will be wrapped in a
     * pre-tag. Otherwise it will only be properly HTML escaped. This
     * method also makes some rudimentary efforts to detect HTTP
     * links.
     *
     * @param str            the text string to convert
     * @param buffer         the string buffer to append into
     */
    private static void serialize(String str, StringBuilder buffer) {
        if (str == null) {
            buffer.append("<code>N/A</code>");
        } else if (str.startsWith("$href$")) {
            str = StringUtils.substringAfter(str, "$href$");
            String url = StringUtils.substringBefore(str, "$");
            String text = StringUtils.substringAfter(str, "$");
            text = StringUtils.defaultIfEmpty(text, url);
            buffer.append("<a href='");
            buffer.append(StringEscapeUtils.escapeHtml(url));
            buffer.append("'>");
            buffer.append(StringEscapeUtils.escapeHtml(text));
            buffer.append("</a>");
        } else if (str.indexOf("\n") >= 0) {
            buffer.append("<pre>");
            buffer.append(StringEscapeUtils.escapeHtml(str));
            buffer.append("</pre>");
        } else {
            buffer.append(StringEscapeUtils.escapeHtml(str));
        }
    }
}
