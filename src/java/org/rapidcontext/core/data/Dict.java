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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.rapidcontext.util.ValueUtil;

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
     * Creates a new dictionary containing all entries in a map. All
     * map keys will be converted to String via Objects.toString().
     * Any iterable or map values will be converted to Array or Dict
     * recursively.
     *
     * @param map            the map to copy
     *
     * @return a new array with all provided entries
     */
    public static Dict from(Map<?, ?> map) {
        Dict dict = new Dict(map.size());
        for (var entry : map.entrySet()) {
            String key = Objects.toString(entry.getKey());
            Object val = entry.getValue();
            if (val instanceof Map<?,?> m) {
                dict.set(key, Dict.from(m));
            } else if (val instanceof Iterable<?> i) {
                dict.set(key, Array.from(i));
            } else {
                dict.set(key, val);
            }
        }
        return dict;
    }

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
     * Checks if this dictionary is identical to another one. The two
     * dictionaries will be considered equal if they have the same
     * size and all keys and values are equal.
     *
     * @param obj            the object to compare with
     *
     * @return true if the two dictionaries are equal, or
     *         false otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Dict d && Objects.equals(this.map, d.map);
    }

    /**
     * Returns a hash code for this object.
     *
     * @return a hash code for this object
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(this.map);
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("{");
        String[] keys = keys();
        for (int i = 0; i < 4 && i < keys.length; i++) {
            if (i > 0) {
                buffer.append(",");
            }
            buffer.append(" ");
            buffer.append(keys[i]);
            buffer.append(": ");
            buffer.append(map.get(keys[i]));
        }
        if (keys.length > 4) {
            buffer.append(", ...");
        }
        if (keys.length > 0) {
            buffer.append(" ");
        }
        buffer.append("}");
        return buffer.toString();
    }

    /**
     * Returns a stream of all entries in the dictionary.
     *
     * @return the entry object stream
     */
    public Stream<Entry<String, Object>> stream() {
        return (map == null) ? Stream.empty() : map.entrySet().stream();
    }

    /**
     * Returns a stream of all entries in the dictionary.
     *
     * @param <T>            the value object type to return
     * @param clazz          the value object class
     *
     * @return the entry object stream
     *
     * @throws ClassCastException if the wasn't possible to cast to
     *             the specified object class
     * @throws NumberFormatException if the value wasn't possible to
     *             parse as a number
     *
     * @see ValueUtil#convert(Object, Class)
     */
    public <T> Stream<Entry<String, T>> stream(Class<T> clazz) {
        return stream().map(e -> {
            T val = ValueUtil.convert(e.getValue(), clazz);
            return new SimpleImmutableEntry<>(e.getKey(), val);
        });
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
            for (Entry<String, Object> e : map.entrySet()) {
                Object value = e.getValue();
                if (value instanceof Dict d) {
                    value = d.copy();
                } else if (value instanceof Array a) {
                    value = a.copy();
                }
                res.map.put(e.getKey(), value);
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
            for (Object value : map.values()) {
                if (value instanceof Dict d) {
                    d.seal(recursive);
                } else if (value instanceof Array a) {
                    a.seal(recursive);
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
            for (Entry<String, Object> e : map.entrySet()) {
                if (Objects.equals(value, e.getValue())) {
                    return e.getKey();
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
        String[] empty = ArrayUtils.EMPTY_STRING_ARRAY;
        return (size() <= 0) ? empty : map.keySet().toArray(empty);
    }

    /**
     * Returns the dictionary value for the specified key.
     *
     * @param key            the dictionary key name
     *
     * @return the dictionary key value, or
     *         null if the key or value is not defined
     */
    public Object get(String key) {
        return (map == null) ? null : map.get(key);
    }

    /**
     * Returns the dictionary value for the specified key. The value
     * is either converted or casted to a specified object class.
     *
     * @param <T>            the object type to return
     * @param key            the dictionary key name
     * @param clazz          the object class
     *
     * @return the dictionary key value value, or
     *         null if the key or value is not defined
     *
     * @throws ClassCastException if the wasn't possible to cast to
     *             the specified object class
     * @throws NumberFormatException if the value wasn't possible to
     *             parse as a number
     *
     * @see ValueUtil#convert(Object, Class)
     */
    public <T> T get(String key, Class<T> clazz) {
        return ValueUtil.convert(get(key), clazz);
    }

    /**
     * Returns the dictionary value for the specified key. The value
     * is either converted or casted to a specified object class. If
     * the key is not defined or if the value is set to null, a
     * default value will be returned instead.
     *
     * @param <T>            the object type to return
     * @param key            the dictionary key name
     * @param clazz          the object class
     * @param defaultValue   the default value
     *
     * @return the dictionary key value value, or
     *         the default value if the key or value is not defined
     *
     * @throws ClassCastException if the wasn't possible to cast to
     *             the specified object class
     * @throws NumberFormatException if the value wasn't possible to
     *             parse as a number
     *
     * @see ValueUtil#convert(Object, Class)
     */
    public <T> T get(String key, Class<T> clazz, T defaultValue) {
        T value = get(key, clazz);
        return (value == null) ? defaultValue : value;
    }

    /**
     * Returns the dictionary dictionary value for the specified key.
     * If the value is not a dictionary, an exception will be thrown.
     *
     * @param key            the dictionary key name
     *
     * @return the dictionary key value, or
     *         an empty dictionary if the key or value is not defined
     *
     * @throws ClassCastException if the value is not a dictionary
     */
    public Dict getDict(String key) throws ClassCastException {
        Dict dict = get(key, Dict.class);
        return (dict == null) ? new Dict() : dict;
    }

    /**
     * Returns the dictionary array value for the specified key. If
     * the value is not an array, an exception will be thrown.
     *
     * @param key            the dictionary key name
     *
     * @return the dictionary key value, or
     *         an empty array if the key or value is not defined
     *
     * @throws ClassCastException if the value is not an array
     */
    public Array getArray(String key) throws ClassCastException {
        Array arr = get(key, Array.class);
        return (arr == null) ? new Array() : arr;
    }

    /**
     * Modifies or defines the dictionary value for the specified key.
     *
     * @param key            the dictionary key name
     * @param value          the value to set
     *
     * @return this dictionary for chained operations
     *
     * @throws NullPointerException if the key is null or an empty
     *             string
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public Dict set(String key, Object value)
        throws NullPointerException, UnsupportedOperationException {

        if (sealed) {
            String msg = "cannot modify sealed dictionary";
            throw new UnsupportedOperationException(msg);
        }
        if (key == null || key.isBlank()) {
            String msg = "property key cannot be null or empty";
            throw new NullPointerException(msg);
        }
        if (map == null) {
            map = new LinkedHashMap<>();
        }
        map.put(key, value);
        return this;
    }

    /**
     * Sets a dictionary value if not already defined.
     *
     * @param key            the dictionary key name
     * @param value          the value to set
     *
     * @return this dictionary for chained operations
     *
     * @throws NullPointerException if the key is null or an empty
     *             string
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public Dict setDefault(String key, Object value) {
        if (!containsKey(key) || get(key) == null) {
            set(key, value);
        }
        return this;
    }

    /**
     * Sets a dictionary value if not already defined.
     *
     * @param key            the dictionary key name
     * @param supplier       the supplier of the value
     *
     * @return this dictionary for chained operations
     *
     * @throws NullPointerException if the key is null or an empty
     *             string
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public Dict setIfNull(String key, Supplier<Object> supplier) {
        if (!containsKey(key) || get(key) == null) {
            set(key, supplier.get());
        }
        return this;
    }

    /**
     * Modifies or defines all keys from another dictionary. If one
     * of the keys already exists, it will be overwritten with the
     * value from the specified dictionary.
     *
     * @param dict           the dictionary to copy from
     *
     * @return this dictionary for chained operations
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public Dict setAll(Dict dict) {
        if (dict != null && dict.size() > 0) {
            for (Entry<String, Object> e : dict.map.entrySet()) {
                set(e.getKey(), e.getValue());
            }
        }
        return this;
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
     * @return this dictionary for chained operations
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public Dict add(String key, Object value)
        throws UnsupportedOperationException {

        String keyName = key;
        int attempt = 0;
        while (containsKey(keyName)) {
            attempt++;
            keyName = key + "_" + attempt;
        }
        set(keyName, value);
        return this;
    }

    /**
     * Modifies all keys provided in another dictionary. If the value
     * for a key is null, the key will be removed. Otherwise the key
     * will be added or overwritten.
     *
     * @param dict           the dictionary to copy from
     *
     * @return this dictionary for chained operations
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public Dict merge(Dict dict) {
        if (dict != null && dict.size() > 0) {
            for (Entry<String, Object> e : dict.map.entrySet()) {
                if (e.getValue() == null) {
                    remove(e.getKey());
                } else {
                    set(e.getKey(), e.getValue());
                }
            }
        }
        return this;
    }

    /**
     * Deletes the specified dictionary key and its value.
     *
     * @param key            the dictionary key name
     *
     * @return this dictionary for chained operations
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public Dict remove(String key) throws UnsupportedOperationException {
        if (sealed) {
            String msg = "cannot modify sealed dictionary";
            throw new UnsupportedOperationException(msg);
        }
        if (map != null) {
            map.remove(key);
        }
        return this;
    }
}
