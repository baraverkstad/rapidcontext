/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2009 Per Cederberg & Dynabyte AB.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;

import org.rapidcontext.util.StringUtil;

/**
 * A data object serializer and unserializer for the standard Java
 * properties file format. The data object mapping to the properties
 * format is not exact, and may omit serialization of data in some
 * cases. The following basic requirements must be met in order to
 * serialize a data object into a properties file:<p>
 *
 * <ul>
 *   <li>No circular references are permitted.
 *   <li>String, Integer, Boolean and Data objects are supported.
 *   <li>Other object types are converted to strings with toString().
 *   <li>Any Data object should be either an array or a map.
 *   <li>Property key names must consist only of printable
 *       ISO-8859-1 character, without any spaces.
 *   <li>Property key names should never consist of only numeric
 *       characters, since they will then be confused with array
 *       indices.
 *   <li>Array null values will be omitted, renumbering the
 *       remaining indices.
 * </ul>
 *
 * The structure and order of Data objects will be kept, using the
 * file order in the properties file and dot-notation for property
 * names.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class PropertiesSerializer {

    /**
     * Reads a file containing properties and returns the contents
     * in a data object. The property names from the file are
     * normally preserved, except for structural interpretation. Dot
     * ('.') characters in names are interpreted as object
     * separators, and numbers are interpreted as an array indices.
     * The property values are stored as booleans, integers or
     * strings.
     *
     * @param file           the file to load
     *
     * @return the data object read from the file
     *
     * @throws FileNotFoundException if the file couldn't be found
     * @throws IOException if an error occurred while reading the
     *             file
     */
    public static Data read(File file)
        throws FileNotFoundException, IOException {

        Data             res = new Data();
        FileInputStream  is;
        BufferedReader   r;
        Properties       props;
        Enumeration      e;
        String           str;

        // Read properties file
        is = new FileInputStream(file);
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

        // Add properties in file order
        r = new BufferedReader(new FileReader(file));
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
     * Adds a property to a data object. The property name is
     * normally preserved, except for structural interpretation. Dot
     * ('.') characters in the name are interpreted as object
     * separators, and numbers are interpreted as array indices. The
     * property value is stored as a string.
     *
     * @param data           the data object to modify
     * @param name           the property name
     * @param value          the property value
     */
    private static void add(Data data, String name, String value) {
        String  key;
        int     pos;

        // Check for object notation
        pos = name.indexOf(".");
        if (pos > 0) {
            key = name.substring(0, pos);
            name = name.substring(pos + 1);
        } else {
            key = name;
            name = null;
        }

        // Check for array index
        pos = -1;
        if (StringUtil.isNumber(key)) {
            try {
                pos = Integer.parseInt(key);
            } catch (NumberFormatException ignore) {
                // Do nothing here
            }
        }

        // Set property value
        if (String.valueOf(pos).equals(key)) {
            if (name == null) {
                data.set(pos, toValue(value));
            } else {
                if (!data.containsIndex(pos)) {
                    data.set(pos, new Data());
                }
                add(data.getData(pos), name, value);
            }
        } else {
            if (name == null) {
                data.set(key, toValue(value));
            } else {
                if (!data.containsKey(key)) {
                    data.set(key, new Data());
                }
                add(data.getData(key), name, value);
            }
        }
    }

    /**
     * Removes null values from all data arrays. This method will be
     * applied recursively, ensuring that there will be no null
     * values in array. This is required in order to simplify
     * handling when array entries have been commented out from the
     * properties file.
     *
     * @param data           the data object to modify
     */
    private static void removeArrayNulls(Data data) {
        String[]  keys = data.keys();
        Object    obj;

        for (int i = 0; i < keys.length; i++) {
            obj = data.get(keys[i]);
            if (obj instanceof Data) {
                removeArrayNulls((Data) obj);
            }
        }
        for (int i = 0; i < data.arraySize(); i++) {
            obj = data.get(i);
            if (obj == null) {
                data.remove(i--);
            } else if (obj instanceof Data) {
                removeArrayNulls((Data) obj);
            }
        }
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
        } else {
            return value;
        }
    }

    /**
     * Writes the contents of a data object into a properties file.
     * The property names from the data object are preserved, but
     * augmented with dot ('.') characters and numbers for structure.
     * The data object values are all stored as strings.
     *
     * @param file           the file to save
     * @param data           the data object
     *
     * @throws IOException if an error occurred while writing the
     *             file
     */
    public static void write(File file, Data data) throws IOException {
        OutputStreamWriter  os;
        PrintWriter         w;

        os = new OutputStreamWriter(new FileOutputStream(file), "ISO-8859-1");
        w = new PrintWriter(os);
        try {
            write(w, "", data);
        } finally {
            w.close();
        }
    }

    /**
     * Writes the contents of a data object to an output stream.
     * The property names from the data object are preserved, but
     * augmented with dot ('.') characters and numbers for structure.
     * The data object values are all stored as strings.
     *
     * @param os             the output stream
     * @param prefix         the property name prefix
     * @param data           the data object
     *
     * @throws IOException if one of the property names wasn't valid
     */
    private static void write(PrintWriter os, String prefix, Data data)
        throws IOException {

        String[]  keys = data.keys();
        Object    obj;
        int       pos = 0;

        if (prefix.length() == 0) {
            os.println("# General properties");
        }
        for (int i = 0; i < keys.length; i++) {
            obj = data.get(keys[i]);
            if (obj instanceof Data) {
                // Skip to last
            } else {
                write(os, prefix + keys[i], data.getString(keys[i], ""));
            }
        }
        for (int i = 0; i < keys.length; i++) {
            obj = data.get(keys[i]);
            if (obj instanceof Data) {
                if (prefix.length() == 0) {
                    os.println();
                    os.print("# ");
                    os.print(keys[i].substring(0, 1).toUpperCase());
                    os.print(keys[i].substring(1));
                    if (((Data) obj).mapSize() >= 0) {
                        os.println(" object");
                    } else {
                        os.println(" array");
                    }
                }
                write(os, prefix + keys[i] + ".", (Data) obj);
            } else {
                // Already handled
            }
        }
        for (int i = 0; i < data.arraySize(); i++) {
            obj = data.get(i);
            if (obj instanceof Data) {
                write(os, prefix + pos + ".", (Data) obj);
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
            case '\\':
                os.print("\\\\");
                newline = false;
                break;
            case '\n':
                os.println("\\n\\");
                newline = true;
                break;
            case '\r':
                break;
            case '\t':
                os.print("\\t");
                newline = false;
                break;
            default:
                if (StringUtil.isIsoLatin1(c)) {
                    os.print(c);
                } else {
                    os.print(StringUtil.escapeUnicode(c));
                }
                newline = false;
            }
        }
        os.println();
    }

    /**
     * Converts a data object into a properties object. The property
     * names from the data object are preserved, but augmented with
     * dot ('.') characters and numbers for structure. The data
     * object values are all stored as strings.
     *
     * @param data           the data object
     *
     * @return the properties object with all the data
     */
    public static Properties toProperties(Data data) {
        return toProperties(new Properties(), "", data);
    }

    /**
     * Converts a data object into a properties object. The property
     * names from the data object are preserved, but augmented with
     * dot ('.') characters and numbers for structure. The data
     * object values are all stored as strings.
     *
     * @param props          the current properties object
     * @param prefix         the property name prefix
     * @param data           the data object
     *
     * @return the modified properties object
     */
    private static Properties toProperties(Properties props, String prefix, Data data) {
        String[]  keys = data.keys();
        Object    obj;
        int       pos = 0;

        for (int i = 0; i < keys.length; i++) {
            obj = data.get(keys[i]);
            if (obj instanceof Data) {
                toProperties(props, prefix + keys[i] + ".", (Data) obj);
            } else {
                props.setProperty(prefix + keys[i], data.getString(keys[i], ""));
            }
        }
        for (int i = 0; i < data.arraySize(); i++) {
            obj = data.get(i);
            if (obj instanceof Data) {
                toProperties(props, prefix + pos + ".", (Data) obj);
                pos++;
            } else if (obj != null) {
                props.setProperty(prefix + pos, obj.toString());
                pos++;
            }
        }
        return props;
    }
}
