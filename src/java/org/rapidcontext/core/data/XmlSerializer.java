/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2013 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.data;

import java.util.Date;

import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.util.StringUtil;

/**
 * A data serializer for XML. This class currently only attempts to
 * render a machine-readable version of a data object, without any
 * efforts of parsing input data. The following basic requirements
 * must be met in order to serialize an object:
 *
 * <ul>
 *   <li>No circular references are permitted.
 *   <li>String, Integer, Boolean, Array and Dict objects are supported.
 *   <li>Key names must only consist of valid XML tag characters.
 * </ul>
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class XmlSerializer {

    /**
     * Serializes an object into an XML representation. The string
     * returned is a stand-alone XML document marked as being in the
     * UTF-8 charset.
     *
     * @param id             the data identifier
     * @param obj            the object to convert, or null
     *
     * @return an XML representation
     */
    public static String serialize(String id, Object obj) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        serialize(id, obj, 0, buffer);
        buffer.append("\n");
        return buffer.toString();
    }

    /**
     * Serializes an object into an XML representation.
     *
     * @param id             the data identifier
     * @param obj            the object to convert
     * @param indent         the current indentation level
     * @param buffer         the string buffer to append into
     */
    private static void serialize(String id, Object obj, int indent, StringBuilder buffer) {
        if (obj == null) {
            buffer.append("<");
            tagName(id, buffer);
            buffer.append(" type=\"null\"/>");
        } else if (obj instanceof Dict) {
            serialize(id, (Dict) obj, indent, buffer);
        } else if (obj instanceof Array) {
            serialize(id, (Array) obj, indent, buffer);
        } else if (obj instanceof Date) {
            tagStart(id, "date", buffer);
            buffer.append("@" + ((Date) obj).getTime());
            tagEnd(id, buffer);
        } else if (obj instanceof Class) {
            tagStart(id, "class", buffer);
            buffer.append(StringUtil.escapeXml(((Class) obj).getName()));
            tagEnd(id, buffer);
        } else if (obj instanceof StorableObject) {
            serialize(id, ((StorableObject) obj).serialize(), indent, buffer);
        } else {
            tagStart(id, null, buffer);
            buffer.append(StringUtil.escapeXml(obj.toString()));
            tagEnd(id, buffer);
        }
    }

    /**
     * Serializes a dictionary into an XML representation.
     *
     * @param id             the data identifier
     * @param dict           the dictionary to convert
     * @param indent         the current indentation level
     * @param buffer         the string buffer to append into
     */
    private static void serialize(String id, Dict dict, int indent, StringBuilder buffer) {
        String[] keys = dict.keys();
        tagStart(id, "object", buffer);
        for (int i = 0; i < keys.length; i++) {
            buffer.append("\n");
            buffer.append(StringUtils.repeat("  ", indent + 1));
            serialize(keys[i], dict.get(keys[i]), indent + 1, buffer);
        }
        buffer.append("\n");
        buffer.append(StringUtils.repeat("  ", indent));
        tagEnd(id, buffer);
    }

    /**
     * Serializes an array into an XML representation.
     *
     * @param id             the data identifier
     * @param arr            the array to convert
     * @param indent         the current indentation level
     * @param buffer         the string buffer to append into
     */
    private static void serialize(String id, Array arr, int indent, StringBuilder buffer) {
        tagStart(id, "array", buffer);
        for (int i = 0; i < arr.size(); i++) {
            buffer.append("\n");
            buffer.append(StringUtils.repeat("  ", indent + 1));
            serialize("item", arr.get(i), indent + 1, buffer);
        }
        buffer.append("\n");
        buffer.append(StringUtils.repeat("  ", indent));
        tagEnd(id, buffer);
    }

    /**
     * Writes an XML start tag.
     *
     * @param id             the tag name (identifier)
     * @param type           the data type, or null for none
     * @param buffer         the string buffer to append into
     */
    private static void tagStart(String id, String type, StringBuilder buffer) {
        buffer.append("<");
        tagName(id, buffer);
        if (type != null) {
            buffer.append(" type=\"");
            buffer.append(type);
            buffer.append("\"");
        }
        buffer.append(">");
    }

    /**
     * Writes an XML end tag.
     *
     * @param id             the tag name (identifier)
     * @param buffer         the string buffer to append into
     */
    private static void tagEnd(String id, StringBuilder buffer) {
        buffer.append("</");
        tagName(id, buffer);
        buffer.append(">");
    }

    /**
     * Writes an XML tag name.
     *
     * @param id             the tag name (identifier)
     * @param buffer         the string buffer to append into
     */
    private static void tagName(String id, StringBuilder buffer) {
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (i == 0 && !CharUtils.isAsciiAlpha(c)) {
                c = '_';
            } else if (!CharUtils.isAsciiAlphanumeric(c)) {
                c = '_';
            }
            buffer.append(c);
        }
    }
}
