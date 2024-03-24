/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2024 Per Cederberg. All rights reserved.
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.util.DateUtil;

/**
 * A data serializer for the Java properties file format. The mapping
 * to the properties format is not exact, and may omit serialization
 * of data in some cases. The following basic requirements must be
 * met in order to serialize an object:
 *
 * <ul>
 *   <li>No circular references are permitted.
 *   <li>String, Integer, Boolean, Date, Array and Dict values are
 *       supported.
 *   <li>Other value types are converted to strings.
 *   <li>Property key names should consist of printable ISO-8859-1
 *       characters, without spaces or dots.
 *   <li>Property key names should not be numeric, as that will
 *       convert to an array index.
 *   <li>Array null values will be omitted, renumbering the
 *       remaining indices.
 * </ul>
 *
 * The structure and order of dictionary objects will be kept, using
 * the file order in the properties file and dot-notation for
 * property names.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public final class PropertiesSerializer {

    /**
     * Serializes an object into a properties representation.
     *
     * @param obj            the object to serialize, or null
     * @param os             the output stream to write to
     *
     * @throws IOException if the data couldn't be serialized
     */
    public static void serialize(Object obj, OutputStream os) throws IOException {
        try (
            OutputStreamWriter ow = new OutputStreamWriter(os, "ISO-8859-1");
            PrintWriter pw = new PrintWriter(ow);
        ) {
            write(pw, "", obj);
        }
    }

    /**
     * Serializes an object into a properties representation.
     *
     * @param obj            the object to serialize, or null
     *
     * @return a properties file representation
     *
     * @throws IOException if the data couldn't be serialized
     */
    public static String serialize(Object obj) throws IOException {
        try (
            StringWriter buffer = new StringWriter();
            PrintWriter pw = new PrintWriter(buffer);
        ) {
            write(pw, "", obj);
            pw.flush();
            return buffer.toString();
        }
    }

    /**
     * Unserializes an object from a properties representation,
     * typically a properties file. The property names are preserved
     * as dictionary keys, except for structural interpretation. Dot
     * ('.') characters in names are interpreted as sub-object
     * separators, and numbers are interpreted as an array indices.
     * Values are stored as booleans, integers or strings.
     *
     * @param is             the input stream to load
     *
     * @return the object read
     *
     * @throws IOException if an error occurred while reading
     */
    public static Object unserialize(InputStream is) throws IOException {
        byte[] buffer = is.readAllBytes();
        try (
            ByteArrayInputStream bs = new ByteArrayInputStream(buffer);
            ByteArrayInputStream rs = new ByteArrayInputStream(buffer);
            BufferedReader br = new BufferedReader(new InputStreamReader(rs));
        ) {
            return unserialize(bs, br);
        }
    }

    /**
     * Unserializes an object from a properties representation,
     * typically a properties file. The property names are preserved
     * as dictionary keys, except for structural interpretation. Dot
     * ('.') characters in names are interpreted as sub-object
     * separators, and numbers are interpreted as an array indices.
     * Values are stored as booleans, integers or strings.
     *
     * Note that the input stream is read twice in order to preserve
     * the original ordering.
     *
     * @param is             the input stream
     * @param r              the input stream reader (used for order)
     *
     * @return the dictionary read from the file
     *
     * @throws IOException if an error occurred while reading
     */
    private static Dict unserialize(InputStream is, BufferedReader r)
    throws IOException {

        // Read properties file (but doesn't preserve ordering)
        Properties props = new Properties();
        props.load(is);

        // Add properties in file order (using simplified parsing)
        Dict res = new Dict();
        r.lines().forEach((str) -> {
            str = StringUtils.substringBefore(str, '=').trim();
            str = StringUtils.substringBefore(str, ':').trim();
            if (props.containsKey(str)) {
                add(res, str, props.getProperty(str));
                props.remove(str);
            }
        });

        // Add any additional properties
        for (String str : props.stringPropertyNames()) {
            add(res, str, props.getProperty(str));
        }

        // Remove array null values
        removeArrayNulls(res);

        return res;
    }

    /**
     * Adds a property to a dictionary. The property name is normally
     * preserved, except for structural interpretation. Dot ('.')
     * characters in the name are interpreted as object separators,
     * and numbers are interpreted as array indices. The property
     * value is converted to boolean, integer or string depending on
     * heuristics.
     *
     * @param dict           the dictionary to modify
     * @param name           the property name
     * @param value          the property value
     */
    private static void add(Dict dict, String name, String value) {
        String[] path = name.split("\\b\\.\\b");
        Object parent = dict;
        for (int i = 0; i < path.length - 1; i++) {
            Object child = getKey(parent, path[i]);
            if (!(child instanceof Dict || child instanceof Array)) {
                if (toIndex(path[i + 1]) >= 0) {
                    child = new Array();
                } else {
                    child = new Dict();
                }
                setKey(parent, path[i], child);
            }
            parent = child;
        }
        setKey(parent, path[path.length - 1], toValue(value));
    }

    /**
     * Gets a value from a parent container. The parent container may
     * be either an Array or a Dict object.
     *
     * @param parent         the parent container
     * @param key            the dictionary key or array index
     *
     * @return the value found, or
     *         null if not found
     */
    private static Object getKey(Object parent, String key) {
        if (parent instanceof Array a) {
            return a.get(toIndex(key));
        } else {
            return ((Dict) parent).get(key);
        }
    }

    /**
     * Sets a value in a parent container. The parent container may
     * be either an Array or a Dict object.
     *
     * @param parent         the parent container
     * @param key            the dictionary key or array index
     * @param value          the value to set
     */
    private static void setKey(Object parent, String key, Object value) {
        if (parent instanceof Array a) {
            int index = toIndex(key);
            if (index < 0) {
                index = a.size();
            }
            a.set(index, value);
        } else {
            ((Dict) parent).set(key, value);
        }
    }

    /**
     * Converts a string to an array index. The string must only
     * consist of ASCII numbers for the conversion to succeed.
     *
     * @param str            the string to convert
     *
     * @return the array index, or
     *         -1 if the conversion failed
     */
    private static int toIndex(String str) {
        int index = -1;
        if (str.length() > 0 && StringUtils.isNumeric(str)) {
            try {
                index = Integer.parseInt(str);
            } catch (NumberFormatException ignore) {
                // Do nothing here
            }
        }
        return str.equals(String.valueOf(index)) ? index : -1;
    }

    /**
     * Converts a string into an approximate value type. The strings
     * "true" and "false" will be converted into boolean values. Any
     * numeric strings will be converted into an integer. All
     * remaining string values will be returned as-is.
     *
     * @param value          the string to convert
     *
     * @return the converted value
     */
    private static Object toValue(String value) {
        if (value.equals("true") || value.equals("false")) {
            return Boolean.valueOf(value);
        } else if (value.length() > 0 && value.length() <= 9 && StringUtils.isNumeric(value)) {
            return Integer.valueOf(value);
        } else if (DateUtil.isEpochFormat(value)) {
            return new Date(Long.parseLong(value.substring(1)));
        } else {
            return value;
        }
    }

    /**
     * Removes null values from all contained arrays. This method
     * will be applied recursively, ensuring that there will be no
     * null values in array. This handling is required in order to
     * provide support for easily commenting out elements from the
     * properties file.
     *
     * @param dict           the dictionary to modify
     */
    private static void removeArrayNulls(Dict dict) {
        for (String key : dict.keys()) {
            Object obj = dict.get(key);
            if (obj instanceof Dict d) {
                removeArrayNulls(d);
            } else if (obj instanceof Array a) {
                removeArrayNulls(a);
            }
        }
    }

    /**
     * Removes null values from an array recursively, i.e. this
     * method will ensure that all null values are removed also in
     * contained arrays. This handling is required in order to
     * provide support for easily commenting out elements from the
     * properties file.
     *
     * @param arr            the array to modify
     */
    private static void removeArrayNulls(Array arr) {
        for (int i = 0; i < arr.size(); i++) {
            Object obj = arr.get(i);
            if (obj == null) {
                arr.remove(i--);
            } else if (obj instanceof Dict d) {
                removeArrayNulls(d);
            } else if (obj instanceof Array a) {
                removeArrayNulls(a);
            }
        }
    }

    /**
     * Serializes an object into a properties file.
     *
     * @param file           the file to write
     * @param obj            the object value, or null
     *
     * @throws IOException if the data couldn't be serialized
     */
    public static void write(File file, Object obj) throws IOException {
        try (
            FileOutputStream os = new FileOutputStream(file);
            OutputStreamWriter ow = new OutputStreamWriter(os, "ISO-8859-1");
            PrintWriter pw = new PrintWriter(ow);
        ) {
            write(pw, "", obj);
        }
    }

    /**
     * Serializes a value to an output stream.
     *
     * @param os             the output stream
     * @param prefix         the property name prefix (or name)
     * @param obj            the object value, or null
     */
    private static void write(PrintWriter os, String prefix, Object obj) {
        if (obj == null) {
            // Nothing to write
        } else if (obj instanceof Dict d) {
            write(os, prefix, d);
        } else if (obj instanceof Array a) {
            write(os, prefix, a);
        } else if (obj instanceof Map<?,?> m) {
            write(os, prefix, Dict.from(m));
        } else if (obj instanceof Iterable<?> i) {
            write(os, prefix, Array.from(i));
        } else if (obj instanceof Date dt) {
            write(os, prefix, DateUtil.asEpochMillis(dt));
        } else {
            write(os, prefix, obj.toString());
        }
    }

    /**
     * Serializes a dictionary to an output stream. The key names
     * from the dictionary are preserved. Contained dictionary or
     * array values will be written recursively by appending dot
     * ('.') characters between each element.
     *
     * @param os             the output stream
     * @param prefix         the property name prefix
     * @param dict           the dictionary object
     */
    private static void write(PrintWriter os, String prefix, Dict dict) {
        if (prefix.length() == 0) {
            os.println("# General properties");
        }
        String[] keys = dict.keys();
        ArrayList<String> delayed = new ArrayList<>();
        for (String k : keys) {
            Object obj = dict.get(k);
            if (obj instanceof Dict || obj instanceof Array || obj instanceof StorableObject) {
                delayed.add(k);
            } else {
                write(os, prefix + k, obj);
            }
        }
        for (String k : delayed) {
            Object obj = dict.get(k);
            boolean isEmpty = (
                (obj instanceof Dict d && d.size() == 0) ||
                (obj instanceof Array a && a.size() == 0)
            );
            if (!isEmpty && prefix.length() == 0) {
                os.println();
                os.print("# ");
                os.print(k.substring(0, 1).toUpperCase());
                os.print(k.substring(1));
                if (obj instanceof Array) {
                    os.println(" array");
                } else {
                    os.println(" object");
                }
            }
            write(os, prefix + k + ".", obj);
        }
    }

    /**
     * Serializes an array to an output stream. The array order is
     * preserved, but indices will be renumbered from zero and null
     * values are omitted. Contained dictionary or array values will
     * be written recursively by appending dot ('.') characters
     * characters between each element.
     *
     * @param os             the output stream
     * @param prefix         the property name prefix
     * @param arr            the array object
     */
    private static void write(PrintWriter os, String prefix, Array arr) {
        int pos = 0;
        for (Object obj : arr) {
            if (obj instanceof Dict || obj instanceof Array || obj instanceof StorableObject) {
                write(os, prefix + pos + ".", obj);
                pos++;
            } else if (obj != null) {
                write(os, prefix + pos, obj);
                pos++;
            }
        }
    }

    /**
     * Serializes a property name and value to an output stream.
     *
     * @param os               the output stream to use
     * @param name             the property name
     * @param value            the property value
     */
    private static void write(PrintWriter os, String name, String value) {
        if (name == null || name.trim().length() == 0) {
            name = "value";
        }
        for (int i = 0; i < name.length(); i ++) {
            char c = name.charAt(i);
            if (c == ' ' || !CharUtils.isAsciiPrintable(c)) {
                name = name.replace(c, '_');
            }
        }
        os.print(name);
        os.print(" = ");
        os.print(TextEncoding.encodeProperty(value, true));
        os.println();
    }

    // No instances
    private PropertiesSerializer() {}
}
