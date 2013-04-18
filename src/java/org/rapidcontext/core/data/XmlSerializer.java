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
 * See the RapidContext LICENSE.txt file for more details.
 */

package org.rapidcontext.core.data;

import java.util.Date;

import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.util.StringUtil;

/**
 * A data serializer for XML. This class currently only attempts to
 * render a machine-readable version of a data object, without any
 * efforts of parsing input data. The following basic requirements
 * must be met in order to serialize an object:<p>
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
     * returned can be used (without escaping) inside an XML document.
     *
     * @param obj            the object to convert, or null
     *
     * @return an XML representation
     */
    public static String serialize(Object obj) {
        StringBuilder  buffer = new StringBuilder();

        serialize("object", obj, buffer);
        return buffer.toString();
    }

    /**
     * Serializes an object into an XML representation.
     *
     * @param id             the data identifier
     * @param obj            the object to convert
     * @param buffer         the string buffer to append into
     */
    private static void serialize(String id, Object obj, StringBuilder buffer) {
        if (obj == null) {
            buffer.append("<");
            buffer.append(id);
            buffer.append(" type=\"null\"/>");
        } else if (obj instanceof Dict) {
            serialize(id, (Dict) obj, buffer);
        } else if (obj instanceof Array) {
            serialize(id, (Array) obj, buffer);
        } else if (obj instanceof Date) {
            tagStart(id, "date", buffer);
            buffer.append("@" + ((Date) obj).getTime());
            tagEnd(id, buffer);
        } else if (obj instanceof Class) {
            tagStart(id, "class", buffer);
            buffer.append(StringUtil.escapeXml(((Class) obj).getName()));
            tagEnd(id, buffer);
        } else if (obj instanceof StorableObject) {
            serialize(id, ((StorableObject) obj).serialize(), buffer);
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
     * @param buffer         the string buffer to append into
     */
    private static void serialize(String id, Dict dict, StringBuilder buffer) {
        String[]  keys = dict.keys();

        tagStart(id, "object", buffer);
        for (int i = 0; i < keys.length; i++) {
            serialize(keys[i], dict.get(keys[i]), buffer);
        }
        tagEnd(id, buffer);
    }

    /**
     * Serializes an array into an XML representation.
     *
     * @param id             the data identifier
     * @param arr            the array to convert
     * @param buffer         the string buffer to append into
     */
    private static void serialize(String id, Array arr, StringBuilder buffer) {
        tagStart(id, "array", buffer);
        for (int i = 0; i < arr.size(); i++) {
            serialize("item", arr.get(i), buffer);
        }
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
        buffer.append(id);
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
        buffer.append(id);
        buffer.append(">");
    }
}
