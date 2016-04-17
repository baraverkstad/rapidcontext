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
 * See the RapidContext LICENSE for more details.
 */

package org.rapidcontext.core.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.util.StringUtil;

/**
 * A dictionary serializer and unserializer for the standard Java
 * properties file format. The dictionary mapping to the properties
 * format is not exact, and may omit serialization of data in some
 * cases. The following basic requirements must be met in order to
 * serialize a dictionary into a properties file:
 *
 * <ul>
 *   <li>No circular references are permitted.
 *   <li>String, Integer, Boolean, Array and Dict objects are
 *       supported.
 *   <li>Other object types are converted to strings with toString().
 *   <li>Property key names must consist only of printable
 *       ISO-8859-1 character, without any spaces or dots.
 *   <li>Property key names should never consist of only numeric
 *       characters, since they will then be confused with array
 *       indices.
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
public class PropertiesSerializer {

    /**
     * Serializes an object into an properties representation. The
     * string returned can be used as a properties.
     *
     * @param obj            the object to convert, or null
     *
     * @return an properties file representation
     *
     * @throws IOException if the data couldn't be serialized
     */
    public static String serialize(Object obj) throws IOException {
        StringWriter  buffer = new StringWriter();
        PrintWriter   os = new PrintWriter(buffer);
        String        msg;

        if (obj == null) {
            // Nothing to write
        } else if (obj instanceof Dict) {
            write(os, "", (Dict) obj);
        } else if (obj instanceof Array) {
            write(os, "", (Array) obj);
        } else if (obj instanceof StorableObject) {
            write(os, "", ((StorableObject) obj).serialize());
        } else {
            msg = "Cannot serialize " + obj.getClass();
            throw new InvalidPropertiesFormatException(msg);
        }
        os.close();
        return buffer.toString();
    }

    /**
     * Reads a file containing properties and returns the contents
     * in a dictionary. The property names from the file are
     * normally preserved, except for structural interpretation. Dot
     * ('.') characters in names are interpreted as sub-object
     * separators, and numbers are interpreted as an array indices.
     * The dictionary values are stored as booleans, integers or
     * strings.
     *
     * @param file           the file to load
     *
     * @return the dictionary read from the file
     *
     * @throws FileNotFoundException if the file couldn't be found
     * @throws IOException if an error occurred while reading the
     *             file
     */
    @SuppressWarnings("resource")
    public static Dict read(File file)
    throws FileNotFoundException, IOException {

        FileReader r = new FileReader(file);
        return read(new FileInputStream(file), new BufferedReader(r));
    }

    /**
     * Reads a ZIP file entry containing properties and returns the
     * contents in a dictionary. The property names from the file are
     * normally preserved, except for structural interpretation. Dot
     * ('.') characters in names are interpreted as sub-object
     * separators, and numbers are interpreted as an array indices.
     * The dictionary values are stored as booleans, integers or
     * strings.
     *
     * @param zip            the ZIP file
     * @param entry          the ZIP file entry to load
     *
     * @return the dictionary read from the file
     *
     * @throws IOException if an error occurred while reading the
     *             file
     */
    public static Dict read(ZipFile zip, ZipEntry entry) throws IOException {
        InputStreamReader r = new InputStreamReader(zip.getInputStream(entry));
        return read(zip.getInputStream(entry), new BufferedReader(r));
    }

    /**
     * Reads an input stream and returns the contents in a
     * dictionary. The property names from the file are normally
     * preserved, except for structural interpretation. Dot ('.')
     * characters in names are interpreted as sub-object separators,
     * and numbers are interpreted as an array indices. The
     * dictionary values are stored as booleans, integers or strings.
     * Note that the input stream is read twice in order to preserve
     * the same ordering as in the file.
     *
     * @param is             the input stream
     * @param r              the input stream reader (used for order)
     *
     * @return the dictionary read from the file
     *
     * @throws IOException if an error occurred while reading
     */
    private static Dict read(InputStream is, BufferedReader r)
    throws IOException {

        Dict         res = new Dict();
        Properties   props;
        Enumeration  e;
        String       str;

        // Read properties file (but doesn't preserve ordering)
        props = new Properties();
        try {
            props.load(is);
        } finally {
            try {
                is.close();
            } catch (Exception ignore) {
                // Ignore exception on closing file
            }
        }

        // Add properties in file order (uses simplified parsing)
        try {
            while ((str = r.readLine()) != null) {
                if (str.indexOf("=") > 0) {
                    str = str.substring(0, str.indexOf("=")).trim();
                } else if (str.indexOf(":") > 0) {
                    str = str.substring(0, str.indexOf(":")).trim();
                } else {
                    str = null;
                }
                if (str != null && props.containsKey(str)) {
                    add(res, str, props.getProperty(str));
                    props.remove(str);
                }
            }
        } finally {
            try {
                r.close();
            } catch (Exception ignore) {
                // Ignore exception on closing file
            }
        }

        // Add any additional properties
        e = props.propertyNames();
        while (e.hasMoreElements()) {
            str = (String) e.nextElement();
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
        String[]  path = name.split("\\.");
        Object    parent = dict;

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
        if (parent instanceof Array) {
            return ((Array) parent).get(toIndex(key));
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
        if (parent instanceof Array) {
            int index = toIndex(key);
            if (index < 0) {
                index = ((Array) parent).size();
            }
            ((Array) parent).set(index, value);
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
        if (StringUtil.isNumber(str)) {
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
        } else if (StringUtil.isNumber(value)) {
            return Integer.valueOf(value);
        } else if (value.startsWith("@") && StringUtil.isNumber(value.substring(1))) {
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
        String[]  keys = dict.keys();
        Object    obj;

        for (int i = 0; i < keys.length; i++) {
            obj = dict.get(keys[i]);
            if (obj instanceof Dict) {
                removeArrayNulls((Dict) obj);
            } else if (obj instanceof Array) {
                removeArrayNulls((Array) obj);
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
        Object    obj;

        for (int i = 0; i < arr.size(); i++) {
            obj = arr.get(i);
            if (obj == null) {
                arr.remove(i--);
            } else if (obj instanceof Dict) {
                removeArrayNulls((Dict) obj);
            } else if (obj instanceof Array) {
                removeArrayNulls((Array) obj);
            }
        }
    }

    /**
     * Writes the contents of a dictionary into a properties file.
     * The key names from the dictionary are preserved, but contained
     * array indices will be renumbered from zero while omitting null
     * values. Contained dictionary or array values will be written
     * recursively by appending dot ('.') characters between each
     * element. All other values are stored as strings.
     *
     * @param file           the file to save
     * @param dict           the dictionary object
     *
     * @throws IOException if an error occurred while writing the
     *             file
     */
    public static void write(File file, Dict dict) throws IOException {
        OutputStreamWriter  os;
        PrintWriter         w;

        os = new OutputStreamWriter(new FileOutputStream(file), "ISO-8859-1");
        w = new PrintWriter(os);
        try {
            write(w, "", dict);
        } finally {
            w.close();
        }
    }

    /**
     * Writes the contents of a dictionary to an output stream. The
     * key names from the dictionary are preserved. Contained
     * dictionary or array values will be written recursively by
     * appending dot ('.') characters between each element. All
     * other values are stored as strings.
     *
     * @param os             the output stream
     * @param prefix         the property name prefix
     * @param dict           the dictionary object
     *
     * @throws IOException if one of the dictionary keys wasn't valid
     */
    private static void write(PrintWriter os, String prefix, Dict dict)
        throws IOException {

        String[]  keys = dict.keys();
        Object    obj;

        if (prefix.length() == 0) {
            os.println("# General properties");
        }
        for (int i = 0; i < keys.length; i++) {
            obj = dict.get(keys[i]);
            if (keys[i].startsWith("_")) {
                // Skip saving transient data
            } else if (obj instanceof Dict || obj instanceof Array) {
                // Skip to last
            } else {
                write(os, prefix + keys[i], dict.getString(keys[i], ""));
            }
        }
        for (int i = 0; i < keys.length; i++) {
            obj = dict.get(keys[i]);
            if (keys[i].startsWith("_")) {
                // Skip saving transient data
            } else if (obj instanceof Dict || obj instanceof Array) {
                if (prefix.length() == 0) {
                    os.println();
                    os.print("# ");
                    os.print(keys[i].substring(0, 1).toUpperCase());
                    os.print(keys[i].substring(1));
                    if (obj instanceof Dict) {
                        os.println(" object");
                    } else {
                        os.println(" array");
                    }
                }
                if (obj instanceof Dict) {
                    write(os, prefix + keys[i] + ".", (Dict) obj);
                } else {
                    write(os, prefix + keys[i] + ".", (Array) obj);
                }
            } else {
                // Already handled
            }
        }
    }

    /**
     * Writes the contents of an array to an output stream. The array
     * order is preserved, but indices will be renumbered from zero
     * while omitting null values. Contained dictionary or array
     * values will be written recursively by appending dot ('.')
     * characters characters between each element. All other values
     * are stored as strings.
     *
     * @param os             the output stream
     * @param prefix         the property name prefix
     * @param arr            the array object
     *
     * @throws IOException if one of the dictionary keys wasn't valid
     */
    private static void write(PrintWriter os, String prefix, Array arr)
        throws IOException {

        Object  obj;
        int     pos = 0;

        for (int i = 0; i < arr.size(); i++) {
            obj = arr.get(i);
            if (obj instanceof Dict) {
                write(os, prefix + pos + ".", (Dict) obj);
                pos++;
            } else if (obj instanceof Array) {
                write(os, prefix + pos + ".", (Array) obj);
                pos++;
            } else if (obj != null) {
                write(os, prefix + pos, obj.toString());
                pos++;
            }
        }
    }

    /**
     * Writes a property name and value definition to an output
     * stream.
     *
     * @param os               the output stream to use
     * @param name             the property name
     * @param value            the property value
     *
     * @throws IOException if the property name wasn't valid
     */
    private static void write(PrintWriter os, String name, String value)
        throws IOException {

        boolean  newline = true;
        char     c;

        if (name == null || name.length() <= 0) {
            throw new IOException("property names cannot be blank");
        } else if (name.indexOf(' ') >= 0) {
            throw new IOException("property name '" + name +
                                  "' contains a space character");
        } else if (!StringUtil.isIsoLatin1(name)) {
            throw new IOException("property name '" + name +
                                  "' is not printable ISO-8859-1");
        }
        os.print(name);
        os.print(" = ");
        for (int i = 0; i < value.length(); i++) {
            c = value.charAt(i);
            switch (c) {
            case ' ':
                if (newline) {
                    os.print("\\");
                }
                os.print(" ");
                newline = false;
                break;
            case '\n':
                os.println("\\n\\");
                newline = true;
                break;
            case '\r':
                break;
            default:
                os.print(StringUtil.escapeProperty(c));
                newline = false;
            }
        }
        os.println();
    }

    /**
     * Converts a dictionary into a properties object. The key names
     * from the dictionary are preserved, but contained array indices
     * will be renumbered from zero while omitting null values.
     * Contained dictionary or array values will be written
     * recursively by appending dot ('.') characters between each
     * element. All other values are stored as strings.
     *
     * @param dict           the dictionary object
     *
     * @return the properties object with all the data
     */
    public static Properties toProperties(Dict dict) {
        return toProperties(new Properties(), "", dict);
    }

    /**
     * Converts a dictionary into a properties object. The key names
     * from the dictionary are preserved, but contained array indices
     * will be renumbered from zero while omitting null values.
     * Contained dictionary or array values will be written
     * recursively by appending dot ('.') characters between each
     * element. All other values are stored as strings.
     *
     * @param props          the current properties object
     * @param prefix         the property name prefix
     * @param dict           the dictionary object
     *
     * @return the modified properties object
     */
    private static Properties toProperties(Properties props,
                                           String prefix,
                                           Dict dict) {

        String[]  keys = dict.keys();
        Object    obj;

        for (int i = 0; i < keys.length; i++) {
            obj = dict.get(keys[i]);
            if (keys[i].startsWith("_")) {
                // Skip converting transient values
            } else if (obj instanceof Dict) {
                toProperties(props, prefix + keys[i] + ".", (Dict) obj);
            } else if (obj instanceof Array) {
                toProperties(props, prefix + keys[i] + ".", (Array) obj);
            } else {
                props.setProperty(prefix + keys[i], dict.getString(keys[i], ""));
            }
        }
        return props;
    }

    /**
     * Converts an array into a properties object. The array
     * order is preserved, but indices will be renumbered from zero
     * while omitting null values. Contained dictionary or array
     * values will be written recursively by appending dot ('.')
     * characters characters between each element in the name. All
     * other values are stored as strings.
     *
     * @param props          the current properties object
     * @param prefix         the property name prefix
     * @param arr            the array object
     *
     * @return the modified properties object
     */
    private static Properties toProperties(Properties props,
                                           String prefix,
                                           Array arr) {

        Object  obj;
        int     pos = 0;

        for (int i = 0; i < arr.size(); i++) {
            obj = arr.get(i);
            if (obj instanceof Dict) {
                toProperties(props, prefix + pos + ".", (Dict) obj);
                pos++;
            } else if (obj instanceof Array) {
                toProperties(props, prefix + pos + ".", (Array) obj);
                pos++;
            } else if (obj != null) {
                props.setProperty(prefix + pos, obj.toString());
                pos++;
            }
        }
        return props;
    }
}
