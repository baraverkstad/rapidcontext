/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2023 Per Cederberg. All rights reserved.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.rapidcontext.core.js.JsRuntime;
import org.rapidcontext.util.FileUtil;
import org.mozilla.javascript.Function;
import org.rapidcontext.core.js.JsException;

/**
 * A data serializer and unserializer for the JSON format. The object
 * mapping to JSON is not exact, and may omit serialization of data
 * in some cases. The following basic requirements must be met in
 * order to serialize an object:
 *
 * <ul>
 *   <li>No circular references are permitted.
 *   <li>String, Integer, Boolean, Array, Dict and StorableObject
 *       values are supported.
 * </ul>
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public final class JsonSerializer {

    // The compiled serialize function
    private static Function serializeFunction = null;

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
        try (OutputStreamWriter ow = new OutputStreamWriter(os, "UTF-8")) {
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
        try {
            if (serializeFunction == null) {
                String[] args = new String[] { "val", "indent" };
                String body = "return JSON.stringify(val, null, indent);";
                serializeFunction = JsRuntime.compile("serialize", args, body);
            }
            Object[] args = new Object[] { obj, Integer.valueOf(indent ? 2 : 0) };
            String str = JsRuntime.call(serializeFunction, args, null).toString();
            return toPrintableAscii(str);
        } catch (JsException ignore) {
            return "null";
        }
    }


    /**
     * Encodes all non-printable ASCII characters in a JSON string.
     *
     * @param str            the text to encode
     *
     * @return the encoded JSON string
     */
    private static String toPrintableAscii(String str) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; str != null && i < str.length(); i++) {
            char c = str.charAt(i);
            buffer.append((c < 127) ? c : String.format("\\u%04x", Integer.valueOf(c)));
        }
        return buffer.toString();
    }

    /**
     * Unserializes JSON data into a Java object. Returns the
     * corresponding String, Number, Boolean, Dict or Array value.
     *
     * @param is             the input stream to load
     *
     * @return the Java data representation
     *
     * @throws IOException if the unserialization failed
     */
    public static Object unserialize(InputStream is) throws IOException {
        return unserialize(FileUtil.readText(is, "UTF-8"));
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
