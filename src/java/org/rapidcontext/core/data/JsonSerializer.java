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

import java.io.IOException;

import org.rapidcontext.core.js.JsRuntime;
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
            return JsRuntime.call(serializeFunction, args, null).toString();
        } catch (JsException ignore) {
            return "null";
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
