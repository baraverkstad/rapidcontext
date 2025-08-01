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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Function;
import org.rapidcontext.core.js.JsException;
import org.rapidcontext.core.js.JsRuntime;
import org.rapidcontext.util.DateUtil;

/**
 * A data serializer and unserializer for the JSON format. The object
 * mapping to JSON is not exact, and may omit serialization of data
 * in some cases. The following basic requirements must be met in
 * order to serialize an object:
 *
 * <ul>
 *   <li>No circular references are permitted.
 *   <li>String, Number, Boolean, Date, Array and Dict values are
 *       supported.
 * </ul>
 *
 * @author Per Cederberg
 */
public final class JsonSerializer {

    // The compiled unserialize function
    private static Function unserializeFunction = null;

    /**
     * Serializes an object into a JSON representation.
     *
     * @param obj            the object to convert, or null
     * @param os             the output stream to write to
     *
     * @throws IOException if the data couldn't be serialized
     */
    public static void serialize(Object obj, OutputStream os) throws IOException {
        try (OutputStreamWriter ow = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
            ow.write(serialize(obj, true));
            ow.write("\n");
            ow.flush();
        }
    }

    /**
     * Serializes an object into a JSON representation. If the
     * indentation flag is set, the JSON data will be indented and
     * formatted. Otherwise a minimal string will be returned.
     *
     * @param obj            the object to convert, or null
     * @param indent         the indentation flag
     *
     * @return a JSON data representation
     */
    public static String serialize(Object obj, boolean indent) {
        StringBuilder buffer = new StringBuilder();
        serialize(obj, indent ? 0 : -1, buffer);
        return buffer.toString();
    }

    /**
     * Serializes an object into a JavaScript literal (JSON). The
     * string returned can be used as a constant inside JavaScript
     * code or returned via a JSON API. If the indentation flag is
     * set, the JSON data will be indented and formatted. Otherwise
     * a minimal string will be created. The serialized result will
     * be written into the specified string buffer.
     *
     * @param obj            the object to convert, or null
     * @param indent         the indentation level, or -1 for none
     * @param buffer         the string buffer to append into
     */
    private static void serialize(Object obj, int indent, StringBuilder buffer) {
        if (obj == null) {
            buffer.append("null");
        } else if (obj instanceof Dict d) {
            serialize(d, indent, buffer);
        } else if (obj instanceof Array a) {
            serialize(a, indent, buffer);
        } else if (obj instanceof Map<?,?> m) {
            serialize(Dict.from(m), indent, buffer);
        } else if (obj instanceof Iterable<?> i) {
            serialize(Array.from(i), indent, buffer);
        } else if (obj instanceof Boolean) {
            buffer.append(obj.toString());
        } else if (obj instanceof Number n) {
            buffer.append(StringUtils.removeEnd(n.toString(), ".0"));
        } else if (obj instanceof Date dt) {
            String str = DateUtil.asEpochMillis(dt);
            buffer.append(TextEncoding.encodeJson(str));
        } else if (obj instanceof Class<?> c) {
            buffer.append(TextEncoding.encodeJson(c.getName()));
        } else {
            buffer.append(TextEncoding.encodeJson(obj.toString()));
        }
    }

    /**
     * Serializes a dictionary into a JavaScript literal (JSON). The
     * string returned can be used as a constant inside JavaScript
     * code or returned via a JSON API. If the indentation flag is
     * set, the JSON data will be indented and formatted. Otherwise
     * a minimal string will be created. The serialized result will
     * be written into the specified string buffer.
     *
     * @param dict           the dictionary to convert
     * @param indent         the indentation level, or -1 for none
     * @param buffer         the string buffer to append into
     */
    private static void serialize(Dict dict, int indent, StringBuilder buffer) {
        int next = indent;
        String prefix = "";
        String infix = ":";
        int col = Math.max(0, buffer.length() - buffer.lastIndexOf("\n"));
        if (indent > 0 && serializedWidth(dict, col, 0) <= 120) {
            next = -1;
            prefix = " ";
            infix += " ";
        } else if (indent >= 0) {
            next = indent + 1;
            prefix = "\n" + StringUtils.repeat("  ", indent + 1);
            infix += " ";
        }
        String[] keys = dict.keys();
        buffer.append("{");
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                buffer.append(",");
            }
            buffer.append(prefix);
            buffer.append(TextEncoding.encodeJson(keys[i]));
            buffer.append(infix);
            serialize(dict.get(keys[i]), next, buffer);
        }
        if (keys.length > 0 && next >= 0) {
            buffer.append("\n");
            buffer.append(StringUtils.repeat("  ", indent));
        } else if (keys.length > 0 && indent >= 0) {
            buffer.append(" ");
        }
        buffer.append("}");
    }

    /**
     * Serializes an array into a JavaScript literal (JSON). The
     * string returned can be used as a constant inside JavaScript
     * code or returned via a JSON API. If the indentation flag is
     * set, the JSON data will be indented and formatted. Otherwise
     * a minimal string will be created. The serialized result will
     * be written into the specified string buffer.
     *
     * @param arr            the array to convert
     * @param indent         the indentation level, or -1 for none
     * @param buffer         the string buffer to append into
     */
    private static void serialize(Array arr, int indent, StringBuilder buffer) {
        int next = indent;
        String prefix = "", suffix = "", infix = "";
        int col = Math.max(0, buffer.length() - buffer.lastIndexOf("\n"));
        if (indent > 0 && serializedWidth(arr, col, 0) <= 120) {
            next = -1;
            prefix = suffix = "";
            infix = " ";
        } else if (indent >= 0) {
            next = indent + 1;
            prefix = infix = "\n" + StringUtils.repeat("  ", indent + 1);
            suffix = "\n" + StringUtils.repeat("  ", indent);
        }
        buffer.append("[");
        buffer.append(prefix);
        for (int i = 0; i < arr.size(); i++) {
            if (i > 0) {
                buffer.append(",");
                buffer.append(infix);
            }
            serialize(arr.get(i), next, buffer);
        }
        buffer.append(suffix);
        buffer.append("]");
    }

    /**
     * Returns the approximate column after serializing an object.
     * If the nesting is too deep or the column size becomes larger
     * than 120, the estimation is aborted.
     *
     * @param obj            the object to serialize, or null
     * @param pos            the current line position (column)
     * @param depth          the current item depth
     *
     * @return the approximate maximum line position (column) reached
     */
    private static int serializedWidth(Object obj, int pos, int depth) {
        if (obj == null || obj instanceof Boolean || obj instanceof Number) {
            return pos + 4;
        } else if (obj instanceof Date) {
            return pos + 16;
        } else if (obj instanceof Dict && depth > 1) {
            return Integer.MAX_VALUE;
        } else if (obj instanceof Dict d) {
            pos += 4;
            for (String key : d.keys()) {
                pos += 5 + key.length();
                pos = serializedWidth(d.get(key), pos, depth + 1);
                if (pos > 120) {
                    break;
                }
            }
            return pos;
        } else if (obj instanceof Array && depth > 1) {
            return Integer.MAX_VALUE;
        } else if (obj instanceof Array a) {
            pos += 2;
            for (Object item : a) {
                pos += 2;
                pos = serializedWidth(item, pos, depth + 1);
                if (pos > 120) {
                    break;
                }
            }
            return pos;
        } else {
            return pos + 2 + obj.toString().length();
        }
    }

    /**
     * Unserializes JSON data into a Java object. Returns the
     * corresponding String, Number, Boolean, Dict or Array value.
     *
     * @param json           the JSON data to convert
     *
     * @return the Java data representation
     *
     * @throws IOException if the unserialization failed
     */
    public static Object unserialize(String json) throws IOException {
        try {
            if (unserializeFunction == null) {
                String[] args = new String[] { "val" };
                String body = "return JSON.parse(val);";
                unserializeFunction = JsRuntime.compile("unserialize", args, body);
            }
            Object[] args = new Object[] { json };
            return JsRuntime.unwrap(JsRuntime.call(unserializeFunction, args, null));
        } catch (JsException e) {
            throw new IOException("Failed to unserialize JSON", e);
        }
    }

    // No instances
    private JsonSerializer() {}
}
