/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg.
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

package org.rapidcontext.core.data;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * A data object serializer for XML. This class currently only
 * attempts to render a machine-readable version of a data object,
 * without any efforts of parsing input data. The following basic
 * requirements must be met in order to serialize a data object:<p>
 *
 * <ul>
 *   <li>No circular references are permitted.
 *   <li>String, Integer, Boolean and Data objects are supported.
 *   <li>Any Data object should be either an array or a map.
 *   <li>Key names must only consist of valid XML tag characters.
 * </ul>
 *
 * @author   Per Cederberg, Dynabyte AB
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

        serialize(obj, buffer);
        return buffer.toString();
    }

    /**
     * Serializes an object into an XML representation.
     *
     * @param obj            the object to convert
     * @param buffer         the string buffer to append into
     */
    private static void serialize(Object obj, StringBuilder buffer) {
        if (obj == null) {
            buffer.append("<null/>");
        } else if (obj instanceof Data) {
            serialize((Data) obj, buffer);
        } else {
            serialize(obj.toString(), buffer);
        }
    }

    /**
     * Serializes a data object into an XML representation. If the
     * data contains array data, only the array values will be used.
     * Otherwise the key-value pairs will be used.
     *
     * @param data           the data object to convert
     * @param buffer         the string buffer to append into
     */
    private static void serialize(Data data, StringBuilder buffer) {
        String[]  keys;

        if (data == null) {
            buffer.append("<null/>");
        } else if (data.arraySize() >= 0) {
            for (int i = 0; i < data.arraySize(); i++) {
                buffer.append("<arrayitem>");
                serialize(data.get(i), buffer);
                buffer.append("</arrayitem>");
            }
        } else {
            keys = data.keys();
            buffer.append("<object>");
            for (int i = 0; i < keys.length; i++) {
                buffer.append("<");
                buffer.append(keys[i]);
                buffer.append(">");
                serialize(data.get(keys[i]), buffer);
                buffer.append("</");
                buffer.append(keys[i]);
                buffer.append(">");
            }
            buffer.append("</object>");
        }
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
            buffer.append("<null/>");
        } else if (str.contains("\n")) {
            buffer.append("<![CDATA[");
            buffer.append(str);
            buffer.append("]]>");
        } else {
            buffer.append(StringEscapeUtils.escapeXml(str));
        }
    }
}
