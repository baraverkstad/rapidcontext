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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.rapidcontext.util.ValueUtil;

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
 * @author Per Cederberg
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
     * Creates a new array containing all elements in an iterable.
     * All contained iterable or map elements will be converted to
     * Array or Dict recursively.
     *
     * @param iter           the iterable to read
     *
     * @return a new array with all elements
     */
    public static Array from(Iterable<?> iter) {
        return from(iter.iterator());
    }

    /**
     * Creates a new array containing all elements in an iterator.
     * All contained iterable or map elements will be converted to
     * Array or Dict recursively.
     *
     * @param iter           the iterator to read
     *
     * @return a new array with all elements
     */
    public static Array from(Iterator<?> iter) {
        Array arr = new Array();
        while (iter.hasNext()) {
            var val = iter.next();
            if (val instanceof Iterable<?> i) {
                arr.add(Array.from(i));
            } else if (val instanceof Map<?,?> m) {
                arr.add(Dict.from(m));
            } else {
                arr.add(val);
            }
        }
        return arr;
    }

    /**
     * Creates a new array containing all elements in a stream. All
     * contained iterable or map elements will be converted to Array
     * or Dict recursively.
     *
     * @param stream         the stream to read
     *
     * @return a new array with all elements
     */
    public static Array from(Stream<?> stream) {
        return from(stream.iterator());
    }

    /**
     * Creates a new array from an array of elements.
     *
     * @param values the values to add to the array
     *
     * @return a new array with the specified elements
     */
    public static Array of(Object... values) {
        Array arr = new Array(values.length);
        for (Object o : values) {
            arr.add(o);
        }
        return arr;
    }

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
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Array a && Objects.equals(this.list, a.list);
    }

    /**
     * Returns a hash code for this object.
     *
     * @return a hash code for this object
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(this.list);
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("[");
        int len = size();
        for (int i = 0; i < 6 && i < len; i++) {
            if (i > 0) {
                buffer.append(",");
            }
            buffer.append(" ");
            buffer.append(list.get(i));
        }
        if (len > 6) {
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
     * Returns a stream of all elements in the array.
     *
     * @return the object stream
     */
    public Stream<Object> stream() {
        return (list == null) ? Stream.empty() : list.stream();
    }

    /**
     * Returns a stream of all elements in the array.
     *
     * @param <T>            the object type to return
     * @param clazz          the object class
     *
     * @return the object stream
     *
     * @throws ClassCastException if the wasn't possible to cast to
     *             the specified object class
     * @throws NumberFormatException if the value wasn't possible to
     *             parse as a number
     *
     * @see ValueUtil#convert(Object, Class)
     */
    public <T> Stream<T> stream(Class<T> clazz) {
        return stream().map(o -> ValueUtil.convert(o, clazz));
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
                if (value instanceof Dict d) {
                    value = d.copy();
                } else if (value instanceof Array a) {
                    value = a.copy();
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
                if (value instanceof Dict d) {
                    d.seal(recursive);
                } else if (value instanceof Array a) {
                    a.seal(recursive);
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
     * Returns the array value at the specified index. The value is
     * either converted or casted to a specified object class.
     *
     * @param <T>            the object type to return
     * @param index          the array index
     * @param clazz          the object class
     *
     * @return the array element value, or
     *         null if the index or value is not defined
     *
     * @throws ClassCastException if the wasn't possible to cast to
     *             the specified object class
     * @throws NumberFormatException if the value wasn't possible to
     *             parse as a number
     *
     * @see ValueUtil#convert(Object, Class)
     */
    public <T> T get(int index, Class<T> clazz) {
        return ValueUtil.convert(get(index), clazz);
    }

    /**
     * Returns the array value at the specified index. The value is
     * either converted or casted to a specified object class. If the
     * index is not defined or if the value is set to null, a default
     * value will be returned instead.
     *
     * @param <T>            the object type to return
     * @param index          the array index
     * @param clazz          the object class
     * @param defaultValue   the default value
     *
     * @return the array element value, or
     *         null if the index or value is not defined
     *
     * @throws ClassCastException if the wasn't possible to cast to
     *             the specified object class
     * @throws NumberFormatException if the value wasn't possible to
     *             parse as a number
     *
     * @see ValueUtil#convert(Object, Class)
     */
    public <T> T get(int index, Class<T> clazz, T defaultValue) {
        return Objects.requireNonNullElse(get(index, clazz), defaultValue);
    }

    /**
     * Returns the array value at the specified index. The value is
     * either converted or casted to a specified object class. If the
     * index is not defined or if the value is set to null, a default
     * value will be returned instead.
     *
     * @param <T>            the object type to return
     * @param index          the array index
     * @param clazz          the object class
     * @param supplier       the default value supplier
     *
     * @return the array element value, or
     *         null if the index or value is not defined
     *
     * @throws ClassCastException if the wasn't possible to cast to
     *             the specified object class
     * @throws NumberFormatException if the value wasn't possible to
     *             parse as a number
     *
     * @see ValueUtil#convert(Object, Class)
     */
    public <T> T getElse(int index, Class<T> clazz, Supplier<? extends T> supplier) {
        return Objects.requireNonNullElseGet(get(index, clazz), supplier);
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
        return getElse(index, Dict.class, () -> new Dict());
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
        return getElse(index, Array.class, () -> new Array());
    }

    /**
     * Modifies or defines the array value for the specified index.
     * The array will automatically be padded with null values to
     * accommodate an index beyond the current valid range.
     *
     * @param index          the array index
     * @param value          the array value
     *
     * @return this array for chained operations
     *
     * @throws IndexOutOfBoundsException if index is negative
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public Array set(int index, Object value)
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
        return this;
    }

    /**
     * Adds an array value to the end of the list. This method will
     * increase the array size by one.
     *
     * @param value          the array value
     *
     * @return this array for chained operations
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public Array add(Object value) throws UnsupportedOperationException {
        int index = size();
        if (sealed) {
            String msg = "cannot modify sealed array";
            throw new UnsupportedOperationException(msg);
        }
        set(index, value);
        return this;
    }

    /**
     * Adds all entries from another array into this one.
     *
     * @param arr            the array to add elements from
     *
     * @return this array for chained operations
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public Array addAll(Array arr) {
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
        return this;
    }

    /**
     * Deletes the specified array index and its value. All
     * subsequent array values will be shifted forward by one step.
     *
     * @param index          the array index
     *
     * @return this array for chained operations
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public Array remove(int index) throws UnsupportedOperationException {
        if (sealed) {
            String msg = "cannot modify sealed array";
            throw new UnsupportedOperationException(msg);
        }
        index = (list != null && index < 0) ? list.size() + index : index;
        if (containsIndex(index)) {
            list.remove(index);
        }
        return this;
    }

    /**
     * Deletes the first array index having the specified value. All
     * subsequent array values will be shifted forward by one step.
     *
     * @param value          the array value
     *
     * @return this array for chained operations
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     */
    public Array remove(Object value) {
        int index = indexOf(value);
        if (index >= 0) {
            remove(index);
        }
        return this;
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
            return Array.from(
                arr.stream().filter(o -> !containsValue(o)).iterator()
            );
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
            return Array.from(
                arr.stream().filter(o -> containsValue(o)).iterator()
            );
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
     * @return this array for chained operations
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     * @throws ClassCastException if the array values are not
     *             comparable with each other
     *
     * @see #sort(String)
     */
    public Array sort()
        throws UnsupportedOperationException, ClassCastException {

        return sort((Comparator<Object>) null);
    }

    /**
     * Sorts all values in this array according to the natural
     * ordering of the specified dictionary key. Note that the
     * array MUST contain dictionaries with comparable key values if
     * this method is used, or a ClassCastException will be thrown.
     *
     * @param key            the dictionary key name
     *
     * @return this array for chained operations
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     * @throws ClassCastException if the array values are not
     *             dictionaries
     *
     * @see #sort()
     */
    public Array sort(String key)
        throws UnsupportedOperationException, ClassCastException {

        return sort(new DictComparator(key));
    }

    /**
     * Sorts all values in this array according to the comparator
     * specified.
     *
     * @param c              the object comparator to use
     *
     * @return this array for chained operations
     *
     * @throws UnsupportedOperationException if this object has been
     *             sealed
     * @throws ClassCastException if the array values were not
     *             comparable
     */
    public Array sort(Comparator<Object> c)
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
        return this;
    }
}
