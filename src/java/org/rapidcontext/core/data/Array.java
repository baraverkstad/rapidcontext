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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;
import org.rapidcontext.util.DateUtil;

/**
 * A general data array. Compared to the standard ArrayList, this
 * class provides a number of improvements;
 *
 * <ul>
 *   <li><strong>Access Methods</strong> -- Methods to provide easy
 *       access to integer, boolean and string values without casting.
 *   <li><strong>Negative indices</strong> -- Using a negative index
 *       will access elements from the end of the array.
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
 * The values in the array may have any type, but recommended types
 * are String, Integer, Boolean, Dict and Array. Circular or
 * self-referencing structures should not be used, since most data
 * serialization cannot handle them.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Array implements Iterable<Object> {

    /**
     * A list of indexable array values.
     */
    private ArrayList<Object> list = null;

    /**
     * The sealed flag. When this flag is set to true, no further
     * changes are permitted to this array. Any calls to the
     * modifier methods will result in a run-time exception.
     */
    private boolean sealed = false;

    /**
     * Creates a new empty array.
     */
    public Array() {
        // Nothing to do here
    }

    /**
     * Creates a new empty array. By default an array is created
     * with a null value list, but if this constructor is used the
     * list will be initialized with the specified capacity.
     *
     * @param initialCapacity the initial array capacity
     */
    public Array(int initialCapacity) {
        if (initialCapacity > 0) {
            list = new ArrayList<>(initialCapacity);
        }
    }

    /**
     * Checks if this array is identical to another one. The two
     * arrays will be considered equal if they have the same length
     * and all elements are equal.
     *
     * @param obj            the object to compare with
     *
     * @return true if the two arrays are equal, or
     *         false otherwise
     */
    public boolean equals(final Object obj) {
        return (obj instanceof Array) && Objects.equals(this.list, ((Array) obj).list);
    }

    /**
     * Returns a hash code for this object.
     *
     * @return a hash code for this object
     */
    public int hashCode() {
        return Objects.hashCode(this.list);
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("[");
        int len = size();
        for (int i = 0; i < 5 && i < len; i++) {
            if (i > 0) {
                buffer.append(",");
            }
            buffer.append(" ");
            buffer.append(list.get(i));
        }
        if (len > 5) {
            buffer.append(", ...");
        }
        if (len > 0) {
            buffer.append(" ");
        }
        buffer.append("]");
        return buffer.toString();
    }

    /**
     * Returns an iterator for all elements in the array.
     *
     * @return the object iterator
     */
    @Override
    public Iterator<Object> iterator() {
        return (list == null) ? Collections.emptyIterator() : list.iterator();
    }

    /**
     * Creates a copy of this array. The copy is a "deep copy", as
     * all dictionary and array values will be recursively copied.
     *
     * @return a deep copy of this array
     */
    public Array copy() {
        Array res = new Array(size());
        if (list != null) {
            for (Object value : list) {
                if (value instanceof Dict) {
                    value = ((Dict) value).copy();
                } else if (value instanceof Array) {
                    value = ((Array) value).copy();
                }
                res.list.add(value);
            }
        }
        return res;
    }

    /**
     * Checks if this array is sealed.
     *
     * @return true if this array has been sealed, or
     *         false otherwise
     */
    public boolean isSealed() {
        return sealed;
    }

    /**
     * Seals this array and prohibits any further modifications.
     * If the seal is applied recursively, any dictionary or array
     * values in this object will also be sealed. Once sealed, this
     * instance is an immutable read-only object.
     *
     * @param recursive      the recursive flag
     */
    public void seal(boolean recursive) {
        sealed = true;
        if (recursive && list != null) {
            for (Object value : list) {
                if (value instanceof Dict) {
                    ((Dict) value).seal(recursive);
                } else if (value instanceof Array) {
                    ((Array) value).seal(recursive);
                }
            }
        }
    }

    /**
     * Returns the size of the array, i.e. the number of elements in
     * it.
     *
     * @return the length of the array, or
     *         zero (0) if empty
     */
    public int size() {
        return (list == null) ? 0 : list.size();
    }

    /**
     * Checks if the specified index is defined in this array, i.e.
     * if the index is in a valid range. Note that an index may be
     * defined but still contain a null value.
     *
     * @param index          the array index
     *
     * @return true if the array index is defined, or
     *         false otherwise
     */
    public boolean containsIndex(int index) {
        return list != null && index >= 0 && index < list.size();
    }

    /**
     * Checks if the specified value is contained in this array. Note
     * that equals() comparison is used, so only simple values may be
     * checked.
     *
     * @param value          the value to check for
     *
     * @return true if the value exists, or
     *         false otherwise
     */
    public boolean containsValue(Object value) {
        return indexOf(value) >= 0;
    }

    /**
     * Checks if all of the values in the specified array is
     * contained in this array. Note that equals() comparison is
     * used, so only simple values may be checked. If the specified
     * array is empty or null, true will be returned.
     *
     * @param arr            the array with values to check
     *
     * @return true if all values exist, or
     *         false otherwise
     */
    public boolean containsAll(Array arr) {
        if (arr != null) {
            for (Object o : arr) {
                if (!containsValue(o)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if one or more of the values in the specified array is
     * contained in this array. Note that equals() comparison is
     * used, so only simple values may be checked. If the specified
     * array is empty or null, false will be returned.
     *
     * @param arr            the array with values to check
     *
     * @return true if at least one value exists, or
     *         false otherwise
     */
    public boolean containsAny(Array arr) {
        if (arr != null) {
            for (Object o : arr) {
                if (containsValue(o)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the first array index having the specified value. Note
     * that equals() comparison is used, so only simple values may be
     * checked.
     *
     * @param value          the value to check for
     *
     * @return the array index, or
     *         -1 if the value wasn't found
     */
    public int indexOf(Object value) {
        return (list == null) ? -1 : list.indexOf(value);
    }

    /**
     * Returns an array with all the values in this array.
     *
     * @return an array with all values
     */
    public Object[] values() {
        return (list == null) ? ArrayUtils.EMPTY_OBJECT_ARRAY : list.toArray();
    }

    /**
     * Returns an array with all the values in this array. If the provided
     * array is too small, a new one of the same type is allocated.
     *
     * @param arr            the array to store the values
     * @param <T>            the base type for all values
     *
     * @return an array with all values
     */
    public <T> T[] values(T[] arr) {
        return ((list == null) ? new ArrayList<>(0) : list).toArray(arr);
    }

    /**
     * Returns the array value at the specified index.
     *
     * @param index          the array index
     *
     * @return the array element value, or
     *         null if the index or value is not defined
     */
    public Object get(int index) {
        index = (list != null && index < 0) ? list.size() + index : index;
        return containsIndex(index) ? list.get(index) : null;
    }

    /**
     * Returns the array value at the specified index. If the index
     * is not defined or if the value is set to null, a default
     * value will be returned instead.
     *
     * @param index          the array index
     * @param defaultValue   the default value
     *
     * @return the array element value, or
     *         the default value if the index or value is not defined
     */
    public Object get(int index, Object defaultValue) {
        Object value = get(index);
        return (value == null) ? defaultValue : value;
    }

    /**
     * Returns the array string value for the specified index. If
     * the index is not defined or if the value is set to null, a
     * default value will be returned instead. If the value object
     * is not a string, the toString() method will be called.
     *
     * @param index          the array index
     * @param defaultValue   the default value
     *
     * @return the array element value, or
     *         the default value if the index or value is not defined
     */
    public String getString(int index, String defaultValue) {
        Object value = get(index);
        if (value == null) {
            return defaultValue;
        } else if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Date) {
            return DateUtil.asEpochMillis((Date) value);
        } else {
            return value.toString();
        }
    }

    /**
     * Returns the array boolean value for the specified index. If
     * the index is not defined or if the value is set to null, a
     * default value will be returned instead. If the value object
     * is not a boolean, any object that does not equal "", "0", "f",
     * "false", "no" or "off" is considered true.
     *
     * @param index          the array index
     * @param defaultValue   the default value
     *
     * @return the array element value, or
     *         the default value if the index or value is not defined
     */
    public boolean getBoolean(int index, boolean defaultValue) {
        Object value = get(index);
        if (value == null) {
            return defaultValue;
        } else if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        } else {
            String str = value.toString().toLowerCase().trim();
            return ArrayUtils.contains(Dict.OFF, str);
        }
    }

    /**
     * Returns the array integer value for the specified index. If
     * the index is not defined or if the value is set to null, a
     * default value will be returned instead. If the value object
     * is not a number, a conversion of the toString() value of the
     * object will be attempted.
     *
     * @param index          the array index
     * @param defaultValue   the default value
     *
     * @return the array element value, or
     *         the default value if the index or value is not defined
     *
     * @throws NumberFormatException if the value didn't contain a
     *             valid integer
     */
    public int getInt(int index, int defaultValue)
        throws NumberFormatException {

        Object value = get(index);
        if (value == null) {
            return defaultValue;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else {
            return Integer.parseInt(value.toString());
        }
    }

    /**
     * Returns the array date value for the specified key. If the key
     * is not defined or if the value is set to null, a default value
     * will be returned instead. If the value object is not a date, a
     * numeric conversion of the string value (excluding any '@'
     * prefix) will be attempted.
     *
     * @param index          the array index
     * @param defaultValue   the default value
     *
     * @return the array element value, or
     *         the default value if the index or value is not defined
     *
     * @throws NumberFormatException if the value didn't contain a
     *             valid date, number or numeric string
     */
    public Date getDate(int index, Date defaultValue)
        throws NumberFormatException {

        Object value = get(index);
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
     * Returns the array dictionary value for the specified index. If
     * the value is not a dictionary, an exception will be thrown.
     *
     * @param index          the array index
     *
     * @return the array element value, or
     *         an empty dictionary if the index or value is not defined
     *
     * @throws ClassCastException if the value is not a dictionary
     */
    public Dict getDict(int index) throws ClassCastException {
        Dict dict = (Dict) get(index);
        return (dict == null) ? new Dict() : dict;
    }

    /**
     * Returns the array array value for the specified index. If
     * the value is not a dictionary, an exception will be thrown.
     *
     * @param index          the array index
     *
     * @return the array element value, or
     *         an empty array if the index or value is not defined
     *
     * @throws ClassCastException if the value is not an array
     */
    public Array getArray(int index) throws ClassCastException {
        Array arr = (Array) get(index);
        return (arr == null) ? new Array() : arr;
    }

    /**
     * Modifies or defines the array value for the specified index.
     * The array will automatically be padded with null values to
     * accommodate an index beyond the current valid range.
     *
     * @param index          the array index
     * @param value          the array value
     *
     * @throws IndexOutOfBoundsException if index is negative
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public void set(int index, Object value)
        throws IndexOutOfBoundsException, UnsupportedOperationException {

        if (sealed) {
            String msg = "cannot modify sealed array";
            throw new UnsupportedOperationException(msg);
        }
        if (list == null) {
            list = new ArrayList<>(index + 1);
        }
        while (index >= list.size()) {
            list.add(null);
        }
        list.set(index, value);
    }

    /**
     * Modifies or defines the boolean array value for the specified
     * index. The array will automatically be padded with null values
     * to accommodate an index beyond the current valid range.
     *
     * @param index          the array index
     * @param value          the array value
     *
     * @throws IndexOutOfBoundsException if index is negative
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public void setBoolean(int index, boolean value)
        throws IndexOutOfBoundsException, UnsupportedOperationException {

        set(index, Boolean.valueOf(value));
    }

    /**
     * Modifies or defines the integer array value for the specified
     * index. The array will automatically be padded with null values
     * to accommodate an index beyond the current valid range.
     *
     * @param index          the array index
     * @param value          the array value
     *
     * @throws IndexOutOfBoundsException if index is negative
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public void setInt(int index, int value)
        throws IndexOutOfBoundsException, UnsupportedOperationException {

        set(index, Integer.valueOf(value));
    }

    /**
     * Adds an array value to the end of the list. This method will
     * increase the array size by one.
     *
     * @param value          the array value
     *
     * @return the array index used (== the previous array size)
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public int add(Object value) throws UnsupportedOperationException {
        int index = size();
        if (sealed) {
            String msg = "cannot modify sealed array";
            throw new UnsupportedOperationException(msg);
        }
        set(index, value);
        return index;
    }

    /**
     * Adds a boolean array value to the end of the list. This method
     * will increase the array size by one.
     *
     * @param value          the array value
     *
     * @return the array index used (== the previous array size)
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public int addBoolean(boolean value)
        throws UnsupportedOperationException {

        return add(Boolean.valueOf(value));
    }

    /**
     * Adds an integer array value to the end of the list. This
     * method will increase the array size by one.
     *
     * @param value          the array value
     *
     * @return the array index used (== the previous array size)
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public int addInt(int value) throws UnsupportedOperationException {
        return add(Integer.valueOf(value));
    }

    /**
     * Adds all entries from another array into this one.
     *
     * @param arr            the array to add elements from
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public void addAll(Array arr) {
        if (sealed) {
            String msg = "cannot modify sealed array";
            throw new UnsupportedOperationException(msg);
        }
        if (arr != null && arr.size() > 0) {
            if (list == null) {
                list = new ArrayList<>(arr.size());
            } else {
                list.ensureCapacity(list.size() + arr.size());
            }
            for (Object o : arr) {
                add(o);
            }
        }
    }

    /**
     * Deletes the specified array index and its value. All
     * subsequent array values will be shifted forward by one step.
     *
     * @param index          the array index
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public void remove(int index) throws UnsupportedOperationException {
        if (sealed) {
            String msg = "cannot modify sealed array";
            throw new UnsupportedOperationException(msg);
        }
        index = (list != null && index < 0) ? list.size() + index : index;
        if (containsIndex(index)) {
            list.remove(index);
        }
    }

    /**
     * Returns the relative complement of this array and another
     * array. The resulting array will contain all elements from the
     * other array that weren't found in this one. None of the two
     * arrays will be modified, but a new array will only be created
     * if some elements exist in both arrays.
     *
     * @param arr            the array to filter
     *
     * @return the complement of this array, or
     *         null if the specified array was null
     */
    public Array complement(Array arr) {
        if (size() <= 0 || !containsAny(arr)) {
            return arr;
        } else {
            Array res = new Array(arr.size());
            for (Object value : arr) {
                if (!containsValue(value)) {
                    res.add(value);
                }
            }
            return res;
        }
    }

    /**
     * Returns the intersection of this array and another array. The
     * resulting array will only contain those elements that were
     * found in both arrays. None of the two arrays will be modified,
     * but a new array will not be created if either is empty.
     *
     * @param arr            the array to intersect with
     *
     * @return the intersection of the two arrays, or
     *         null if the specified array was null
     */
    public Array intersection(Array arr) {
        if (arr == null || arr.size() <= 0) {
            return arr;
        } else if (size() <= 0) {
            return this;
        } else {
            Array res = new Array(Math.min(size(), arr.size()));
            for (Object value : arr) {
                if (containsValue(value)) {
                    res.add(value);
                }
            }
            return res;
        }
    }

    /**
     * Returns the union of this array and another array. The
     * resulting array will contain all elements from this array and
     * all elements from the other array that weren't in this one.
     * None of the two arrays will be modified, but a new array will
     * not be created if either is empty or the overlap is 100%.
     *
     * @param arr            the array to combine with
     *
     * @return the union of the two arrays
     */
    public Array union(Array arr) {
        Array comp = complement(arr);
        if (comp == null || comp.size() <= 0) {
            return this;
        } else if (size() <= 0) {
            return comp;
        } else {
            Array res = new Array(size() + comp.size());
            res.addAll(this);
            res.addAll(comp);
            return res;
        }
    }

    /**
     * Sorts all values in this array according to their natural
     * ordering. Note that the array MUST NOT contain dictionaries,
     * arrays or other objects that are not comparable (will result
     * in a ClassCastException). Also, all entries must be comparable
     * with each other, as the natural order of different data types
     * is undefined.
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     * @throws ClassCastException if the array values are not
     *             comparable with each other
     *
     * @see #sort(String)
     */
    public void sort()
        throws UnsupportedOperationException, ClassCastException {

        sort((Comparator<Object>) null);
    }

    /**
     * Sorts all values in this array according to the natural
     * ordering of the specified dictionary key. Note that the
     * array MUST contain dictionaries with comparable key values if
     * this method is used, or a ClassCastException will be thrown.
     *
     * @param key            the dictionary key name
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     * @throws ClassCastException if the array values are not
     *             dictionaries
     *
     * @see #sort()
     */
    public void sort(String key)
        throws UnsupportedOperationException, ClassCastException {

        sort(new DictComparator(key));
    }

    /**
     * Sorts all values in this array according to the comparator
     * specified.
     *
     * @param c              the object comparator to use
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     * @throws ClassCastException if the array values were not
     *             comparable
     */
    public void sort(Comparator<Object> c)
        throws UnsupportedOperationException, ClassCastException {

        if (sealed) {
            String msg = "cannot modify sealed array";
            throw new UnsupportedOperationException(msg);
        }
        if (list != null) {
            if (c == null) {
                Collections.sort(list, null);
            } else {
                Collections.sort(list, c);
            }
        }
    }
}
