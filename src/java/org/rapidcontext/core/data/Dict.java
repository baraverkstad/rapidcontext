/*
 * RapidContext <https://www.rapidcontext.com/>
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
import java.util.LinkedHashMap;

import org.apache.commons.lang.ArrayUtils;

/**
 * A general data dictionary. Compared to the standard Map interface,
 * this class provides a number of improvements;
 *
 * <ul>
 *   <li><strong>Access Methods</strong> -- Methods to provide easy
 *       access to integer, boolean and string values without casting.
 *   <li><strong>Sealing</strong> -- Simple creation of read-only
 *       objects.
 *   <li><strong>Deep Copies</strong> -- Provides a meaningful way to
 *       clone or copy data.
 *   <li><strong>Serialization</strong> -- Utility classes are
 *       available for serializing to JSON, XML, etc.
 *   <li><strong>Debug Info</strong> -- The toString() method provides
 *       a more usable default implementation.
 * </ul>
 *
 * A dictionary is more or less a hash table with name and value
 * pairs. The names are non-empty strings and values may have any
 * type, but recommended types are String, Integer, Boolean, Dict and
 * Array. Circular or self-referencing structures should not be used,
 * since most data serialization cannot handle them.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Dict {

    /**
     * A hash map with names and values.
     */
    private LinkedHashMap<String,Object> map = null;

    /**
     * The sealed flag. When this flag is set to true, no further
     * changes are permitted to this dictionary. Any calls to the
     * modifier methods will result in a run-time exception.
     */
    private boolean sealed = false;

    /**
     * Creates a new empty dictionary.
     */
    public Dict() {
        // Nothing to do here
    }

    /**
     * Creates a new empty dictionary. By default a dictionary is
     * created with a null key map, but if this constructor is used
     * the map will be initialized with the specified capacity.
     *
     * @param initialCapacity the initial dictionary capacity
     */
    public Dict(int initialCapacity) {
        if (initialCapacity > 0) {
            map = new LinkedHashMap<>(initialCapacity);
        }
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("{");
        String[] keys = keys();
        for (int i = 0; i < 3 && i < keys.length; i++) {
            if (i > 0) {
                buffer.append(",");
            }
            buffer.append(" ");
            buffer.append(keys[i]);
            buffer.append(": ");
            buffer.append(map.get(keys[i]));
        }
        if (keys.length > 3) {
            buffer.append(", ...");
        }
        if (keys.length > 0) {
            buffer.append(" ");
        }
        buffer.append("}");
        return buffer.toString();
    }

    /**
     * Creates a copy of this dictionary. The copy is a "deep copy",
     * as all dictionary and array values will be recursively copied.
     *
     * @return a deep copy of this object
     */
    public Dict copy() {
        Dict res = new Dict(size());
        if (map != null) {
            for (String key : map.keySet()) {
                Object value = map.get(key);
                if (value instanceof Dict) {
                    value = ((Dict) value).copy();
                } else if (value instanceof Array) {
                    value = ((Array) value).copy();
                }
                res.map.put(key, value);
            }
        }
        return res;
    }

    /**
     * Checks if this dictionary is sealed.
     *
     * @return true if this dictionary has been sealed, or
     *         false otherwise
     */
    public boolean isSealed() {
        return sealed;
    }

    /**
     * Seals this dictionary and prohibits further modifications. If
     * the seal is applied recursively, any dictionary or array
     * values in this object will also be sealed. Once sealed, this
     * instance is an immutable read-only object.
     *
     * @param recursive      the recursive flag
     */
    public void seal(boolean recursive) {
        sealed = true;
        if (recursive && map != null) {
            for (String key : map.keySet()) {
                Object value = map.get(key);
                if (value instanceof Dict) {
                    ((Dict) value).seal(recursive);
                } else if (value instanceof Array) {
                    ((Array) value).seal(recursive);
                }
            }
        }
    }

    /**
     * Returns the size of the dictionary, i.e. the number of keys
     * in it.
     *
     * @return the size of the dictionary, or
     *         zero (0) if empty
     */
    public int size() {
        return (map == null) ? 0 : map.size();
    }

    /**
     * Checks if the specified key is defined in this dictionary.
     * Note that a key name may be defined but still contain a null
     * value.
     *
     * @param key            the key name
     *
     * @return true if the key is defined, or
     *         false otherwise
     */
    public boolean containsKey(String key) {
        return map != null && map.containsKey(key);
    }

    /**
     * Checks if the specified value is contained in this dictionary.
     * Note that equals() comparison is used, so only simple values
     * may be checked.
     *
     * @param value          the value to check for
     *
     * @return true if the value exists, or
     *         false otherwise
     */
    public boolean containsValue(Object value) {
        return keyOf(value) != null;
    }

    /**
     * Returns the first dictionary key having the specified value.
     * Note that equals() comparison is used, so only simple values
     * may be checked.
     *
     * @param value          the value to check for
     *
     * @return the dictionary key name, or
     *         null if the value wasn't found
     */
    public String keyOf(Object value) {
        if (map != null) {
            for (String key : map.keySet()) {
                Object obj = map.get(key);
                if (obj == null && value == null) {
                    return key;
                } else if (obj != null && obj.equals(value)) {
                    return key;
                }
            }
        }
        return null;
    }

    /**
     * Returns an array with all the defined dictionary key names.
     * The keys are ordered as originally added to this object.
     *
     * @return an array with all dictionary key names
     */
    public String[] keys() {
        if (size() <= 0) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        } else {
            return map.keySet().toArray(new String[map.size()]);
        }
    }

    /**
     * Returns the dictionary value for the specified key.
     *
     * @param key            the dictionary key name
     *
     * @return the dictionary key value, or
     *         null if the key is not defined
     */
    public Object get(String key) {
        return (map == null) ? null : map.get(key);
    }

    /**
     * Returns the dictionary value for the specified key. If the key
     * is not defined or if the value is set to null, a default
     * value will be returned instead.
     *
     * @param key            the dictionary key name
     * @param defaultValue   the default value
     *
     * @return the dictionary key value value, or
     *         the default value if the key is not defined
     */
    public Object get(String key, Object defaultValue) {
        Object value = get(key);
        return (value == null) ? defaultValue : value;
    }

    /**
     * Returns the dictionary string value for the specified key. If
     * the key is not defined or if the value is set to null, a
     * default value will be returned instead. If the value object
     * is not a string, the toString() method will be called.
     *
     * @param key            the dictionary key name
     * @param defaultValue   the default value
     *
     * @return the dictionary key value, or
     *         the default value if the key is not defined
     */
    public String getString(String key, String defaultValue) {
        Object value = get(key);
        if (value == null) {
            return defaultValue;
        } else if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Date) {
            return "@" + ((Date) value).getTime();
        } else {
            return value.toString();
        }
    }

    /**
     * Returns the dictionary boolean value for the specified key. If
     * the key is not defined or if the value is set to null, a
     * default value will be returned instead. If the value object
     * is not a boolean, any object that does not equal FALSE, "",
     * "false" or 0 will be converted to true.
     *
     * @param key            the dictionary key name
     * @param defaultValue   the default value
     *
     * @return the dictionary key value, or
     *         the default value if the key is not defined
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = get(key);
        if (value == null) {
            return defaultValue;
        } else if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        } else {
            return !value.equals(Boolean.FALSE) &&
                   !value.equals("") &&
                   !value.equals("false") &&
                   !value.equals(Integer.valueOf(0));
        }
    }

    /**
     * Returns the dictionary integer value for the specified key. If
     * the key is not defined or if the value is set to null, a
     * default value will be returned instead. If the value object
     * is not a number, a conversion of the toString() value of the
     * object will be attempted.
     *
     * @param key            the dictionary key name
     * @param defaultValue   the default value
     *
     * @return the dictionary key value, or
     *         the default value if the key is not defined
     *
     * @throws NumberFormatException if the value didn't contain a
     *             valid integer
     */
    public int getInt(String key, int defaultValue)
        throws NumberFormatException {

        Object value = get(key);
        if (value == null) {
            return defaultValue;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else {
            return Integer.parseInt(value.toString());
        }
    }

    /**
     * Returns the dictionary date value for the specified key. If
     * the key is not defined or if the value is set to null, a
     * default value will be returned instead. If the value object
     * is not a date, a numeric conversion of the string value
     * (excluding any '@' prefix) will be attempted.
     *
     * @param key            the dictionary key name
     * @param defaultValue   the default value
     *
     * @return the dictionary key value, or
     *         the default value if the key is not defined
     *
     * @throws NumberFormatException if the value didn't contain a
     *             valid date, number or numeric string
     */
    public Date getDate(String key, Date defaultValue)
        throws NumberFormatException {

        Object value = get(key);
        if (value == null) {
            return defaultValue;
        } else if (value instanceof Date) {
            return (Date) value;
        } else if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        } else {
            String str = value.toString();
            if (str.startsWith("@")) {
                str = str.substring(1);
            }
            return new Date(Long.parseLong(str));
        }
    }

    /**
     * Returns the dictionary dictionary value for the specified key.
     * If the value is not a dictionary, an exception will be thrown.
     *
     * @param key            the dictionary key name
     *
     * @return the dictionary key value, or
     *         null if the key is not defined
     *
     * @throws ClassCastException if the value is not a dictionary
     */
    public Dict getDict(String key) throws ClassCastException {
        return (Dict) get(key);
    }

    /**
     * Returns the dictionary array value for the specified key. If
     * the value is not an array, an exception will be thrown.
     *
     * @param key            the dictionary key name
     *
     * @return the dictionary key value, or
     *         null if the key is not defined
     *
     * @throws ClassCastException if the value is not an array
     */
    public Array getArray(String key) throws ClassCastException {
        return (Array) get(key);
    }

    /**
     * Modifies or defines the dictionary value for the specified key.
     *
     * @param key            the dictionary key name
     * @param value          the value to set
     *
     * @throws NullPointerException if the key is null or an empty
     *             string
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public void set(String key, Object value)
        throws NullPointerException, UnsupportedOperationException {

        if (sealed) {
            String msg = "cannot modify sealed dictionary";
            throw new UnsupportedOperationException(msg);
        }
        if (key == null || key.length() == 0) {
            String msg = "property key cannot be null or empty";
            throw new NullPointerException(msg);
        }
        if (map == null) {
            map = new LinkedHashMap<>();
        }
        map.put(key, value);
    }

    /**
     * Modifies or defines the boolean dictionary value for the
     * specified key.
     *
     * @param key            the dictionary key name
     * @param value          the value to set
     *
     * @throws NullPointerException if the key is null or an empty
     *             string
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public void setBoolean(String key, boolean value)
        throws NullPointerException, UnsupportedOperationException {

        set(key, Boolean.valueOf(value));
    }

    /**
     * Modifies or defines the integer dictionary value for the
     * specified key.
     *
     * @param key            the dictionary key name
     * @param value          the value to set
     *
     * @throws NullPointerException if the key is null or an empty
     *             string
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public void setInt(String key, int value)
        throws NullPointerException, UnsupportedOperationException {

        set(key, Integer.valueOf(value));
    }

    /**
     * Modifies or defines all keys from another dictionary. If one
     * of the keys already exists, it will be overwritten with the
     * value from the specified dictionary.
     *
     * @param dict           the dictionary to copy from
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public void setAll(Dict dict) {
        if (dict != null && dict.size() > 0) {
            for (String key : dict.map.keySet()) {
                set(key, dict.map.get(key));
            }
        }
    }

    /**
     * Adds a dictionary value using the specified key if possible.
     * If the key is already in use, a new unique key will be
     * generated instead. This will ensure that an existing value
     * will not be overwritten.
     *
     * @param key            the suggested dictionary key name
     * @param value          the value to set
     *
     * @return the dictionary key name used
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public String add(String key, Object value)
        throws UnsupportedOperationException {

        String keyName = key;
        int attempt = 0;
        while (containsKey(keyName)) {
            attempt++;
            keyName = key + "_" + attempt;
        }
        set(keyName, value);
        return keyName;
    }

    /**
     * Adds a boolean property value using the specified key if
     * possible. If the key is already in use, a new unique key will
     * be generated instead. This will ensure that an existing value
     * will not be overwritten.
     *
     * @param key            the suggested dictionary key name
     * @param value          the value to set
     *
     * @return the dictionary key name used
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public String addBoolean(String key, boolean value)
        throws UnsupportedOperationException {

        return add(key, Boolean.valueOf(value));
    }

    /**
     * Adds an integer property value using the specified key if
     * possible. If the key is already in use, a new unique key will
     * be generated instead. This will ensure that an existing value
     * will not be overwritten.
     *
     * @param key            the suggested dictionary key name
     * @param value          the value to set
     *
     * @return the dictionary key name used
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public String addInt(String key, int value)
        throws UnsupportedOperationException {

        return add(key, Integer.valueOf(value));
    }

    /**
     * Adds all key-value pairs from another dictionary to this one.
     * If one of the keys are already in use, a new unique key will
     * be generated instead. This will ensure that existing values
     * will not be overwritten.
     *
     * @param dict           the dictionary to add from
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public void addAll(Dict dict) {
        if (dict != null && dict.size() > 0) {
            for (String key : dict.map.keySet()) {
                add(key, dict.map.get(key));
            }
        }
    }

    /**
     * Deletes the specified dictionary key and its value.
     *
     * @param key            the dictionary key name
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public void remove(String key) throws UnsupportedOperationException {
        if (sealed) {
            String msg = "cannot modify sealed dictionary";
            throw new UnsupportedOperationException(msg);
        }
        if (map != null) {
            map.remove(key);
        }
    }
}
