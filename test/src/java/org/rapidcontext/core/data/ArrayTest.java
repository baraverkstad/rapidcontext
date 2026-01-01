/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2026 Per Cederberg. All rights reserved.
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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class ArrayTest {

    @Test
    public void testConstructor() {
        assertEquals(0, new Array().size());
        assertEquals(0, new Array(10).size());
    }

    @Test
    public void testFrom() {
        List<Object> list = Arrays.asList(
            null,
            false,
            42,
            "one",
            Arrays.asList("nested", "array"),
            Collections.singletonMap("key", "value")
        );
        Array arr = Array.from(list);
        assertEquals(6, arr.size());
        assertEquals(list.get(0), arr.get(0));
        assertEquals(list.get(1), arr.get(1));
        assertEquals(list.get(2), arr.get(2));
        assertEquals(list.get(3), arr.get(3));
        assertTrue(arr.get(4) instanceof Array);
        assertTrue(arr.get(5) instanceof Dict);
    }

    @Test
    public void testOf() {
        Array arr = Array.of(13, null, false, new Array());
        assertEquals(4, arr.size());
        assertEquals(13, arr.get(0));
        assertEquals(null, arr.get(1));
        assertEquals(false, arr.get(2));
        assertTrue(arr.get(3) instanceof Array);
    }

    @Test
    public void testEqualsAndHashCode() {
        Array arr = Array.of("a", "b", "c");
        assertFalse(arr.equals(null));
        assertFalse(arr.equals("not an array"));
        assertFalse(arr.equals(Array.of("a", "b")));
        Array copy = Array.of("a", "b", "c");
        assertEquals(copy, arr);
        assertEquals(copy.hashCode(), arr.hashCode());
    }

    @Test
    public void testToString() {
        Array arr = new Array();
        assertEquals("[]", arr.toString());
        arr = Array.of("a");
        assertEquals("[ a ]", arr.toString());
        arr = Array.of("a", "b", "c", "d", "e", "f", "g");
        assertTrue(arr.toString().contains("a"));
        assertTrue(arr.toString().contains("..."));
    }

    @Test
    public void testIterator() {
        Object[] src = { "a", "b", "c" };
        List<Object> collected = new ArrayList<>();
        for (Object item : Array.of(src)) {
            collected.add(item);
        }
        assertArrayEquals(src, collected.toArray());
    }

    @Test
    public void testStream() {
        Object[] src = { "a", "b", "c" };
        assertArrayEquals(src, Array.of(src).stream().toArray());
    }

    @Test
    public void testStreamWithClass() {
        Object[] src = { "123", "456", "789" };
        Integer[] dst = { 123, 456, 789 };
        Array arr = Array.of(src);
        assertArrayEquals(dst, arr.stream(Integer.class).toArray());
        // FIXME: assertThrows(ClassCastException.class, () -> arr.stream(Class.class).toArray());
        arr.add("str");
        assertThrows(NumberFormatException.class, () -> arr.stream(Integer.class).toArray());
    }

    @Test
    public void testCopy() {
        Dict dict = new Dict().set("key", "value");
        Array nested = Array.of("nested");
        Array original = Array.of("a", "b", "c", dict, nested);
        Array copy = original.copy();
        assertFalse(original == copy);
        assertEquals(original, copy);
        original.add("d");
        assertEquals(6, original.size());
        assertEquals(5, copy.size());
        dict.set("key", "modified");
        assertEquals("value", copy.getDict(3).get("key"));
        nested.add("modified");
        assertEquals(1, copy.getArray(4).size());
    }

    @Test
    public void testSeal() {
        Dict dict = new Dict().set("key", "value");
        Array nested = Array.of("nested");
        Array arr = Array.of("a", "b", "c", dict, nested);
        assertFalse(arr.isSealed());
        arr.seal(false);
        assertTrue(arr.isSealed());
        assertFalse(dict.isSealed());
        assertFalse(nested.isSealed());
        arr.seal(true);
        assertTrue(dict.isSealed());
        assertTrue(nested.isSealed());
        assertThrows(UnsupportedOperationException.class, () -> arr.set(0, "x"));
        assertThrows(UnsupportedOperationException.class, () -> arr.add("d"));
        assertThrows(UnsupportedOperationException.class, () -> arr.addAll(Array.of("c", "d")));
        assertThrows(UnsupportedOperationException.class, () -> arr.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> arr.remove("a"));
    }

    @Test
    public void testSize() {
        Array arr = new Array();
        assertEquals(0, arr.size());
        arr.add("a");
        assertEquals(1, arr.size());
        arr.add("b");
        assertEquals(2, arr.size());
    }

    @Test
    public void testContainsIndex() {
        Array arr = Array.of("a", "b", "c");
        assertTrue(arr.containsIndex(0));
        assertTrue(arr.containsIndex(1));
        assertTrue(arr.containsIndex(2));
        assertFalse(arr.containsIndex(3));
        assertFalse(arr.containsIndex(-1));
    }

    @Test
    public void testContainsValue() {
        Array arr = Array.of("a", "b", "c");
        assertFalse(arr.containsValue(null));
        assertTrue(arr.containsValue("a"));
        assertTrue(arr.containsValue("c"));
        assertFalse(arr.containsValue("d"));
    }

    @Test
    public void testContainsAll() {
        Array arr = Array.of("a", "b", "c");
        assertTrue(arr.containsAll(null));
        assertTrue(arr.containsAll(new Array()));
        assertTrue(arr.containsAll(Array.of("a", "b")));
        assertFalse(arr.containsAll(Array.of("a", "b", "c", "d")));
    }

    @Test
    public void testContainsAny() {
        Array arr = Array.of("a", "b", "c");
        assertFalse(arr.containsAny(null));
        assertFalse(arr.containsAny(new Array()));
        assertTrue(arr.containsAny(Array.of("b", "d")));
        assertFalse(arr.containsAny(Array.of("d", "e")));
    }

    @Test
    public void testIndexOf() {
        Array arr = Array.of("a", "b", "c", "b");
        assertEquals(-1, arr.indexOf(null));
        assertEquals(0, arr.indexOf("a"));
        assertEquals(1, arr.indexOf("b"));
        assertEquals(2, arr.indexOf("c"));
        assertEquals(-1, arr.indexOf("d"));
    }

    @Test
    public void testValues() {
        Object[] src = { "a", "b", "c" };
        Array arr = Array.of(src);
        assertArrayEquals(src, arr.values());
        assertArrayEquals(src, arr.values(new String[0]));
    }

    @Test
    public void testGet() {
        Object[] src = { "a", "b", "c" };
        Array arr = Array.of(src);
        assertEquals(src[0], arr.get(0));
        assertEquals(src[1], arr.get(1));
        assertEquals(src[2], arr.get(2));
        assertNull(arr.get(3));
        assertEquals(src[2], arr.get(-1));
        assertEquals(src[1], arr.get(-2));
        assertEquals(src[0], arr.get(-3));
        assertNull(arr.get(-4));
    }

    @Test
    public void testGetWithClass() {
        Array arr = Array.of("123", "456", "789");
        assertEquals(Integer.valueOf(123), arr.get(0, Integer.class));
    }

    @Test
    public void testGetWithDefault() {
        Array arr = Array.of("123", "456");
        assertEquals(Integer.valueOf(123), arr.get(0, Integer.class, 999));
        assertEquals(Integer.valueOf(456), arr.get(1, Integer.class, 999));
        assertEquals(Integer.valueOf(999), arr.get(2, Integer.class, 999));
    }

    @Test
    public void testGetElse() {
        Array arr = Array.of("123", "456");
        assertEquals(Integer.valueOf(123), arr.getElse(0, Integer.class, () -> 999));
        assertEquals(Integer.valueOf(999), arr.getElse(2, Integer.class, () -> 999));
    }

    @Test
    public void testGetDict() {
        Dict dict = new Dict().set("key", "value");
        Array arr = Array.of(dict, "string");
        assertEquals(dict, arr.getDict(0));
        assertThrows(ClassCastException.class, () -> arr.getDict(1));
    }

    @Test
    public void testGetArray() {
        Array arr = Array.of(Array.of("nested"), "string");
        Array nested = arr.getArray(0);
        assertEquals(1, nested.size());
        assertEquals("nested", nested.get(0));
        assertThrows(ClassCastException.class, () -> arr.getArray(1));
    }

    @Test
    public void testSet() {
        Array arr = new Array();
        arr.set(0, "a");
        assertEquals(1, arr.size());
        assertEquals("a", arr.get(0));
        arr.set(5, "f");
        assertEquals(6, arr.size());
        assertNull(arr.get(1));
        assertNull(arr.get(2));
        assertNull(arr.get(3));
        assertNull(arr.get(4));
        assertEquals("f", arr.get(5));
        assertThrows(IndexOutOfBoundsException.class, () -> arr.set(-1, "x"));
    }

    @Test
    public void testAdd() {
        Array arr = new Array();
        arr.add("a");
        assertEquals(1, arr.size());
        assertEquals("a", arr.get(0));
        arr.add("b");
        assertEquals(2, arr.size());
        assertEquals("b", arr.get(1));
    }

    @Test
    public void testAddAll() {
        Array arr = Array.of("a", "b");
        arr.addAll(Array.of("c", "d"));
        assertEquals(4, arr.size());
        assertEquals("a", arr.get(0));
        assertEquals("b", arr.get(1));
        assertEquals("c", arr.get(2));
        assertEquals("d", arr.get(3));
    }

    @Test
    public void testRemoveByIndex() {
        Array arr = Array.of("a", "b", "c");
        arr.remove(1);
        assertEquals(2, arr.size());
        assertEquals("a", arr.get(0));
        assertEquals("c", arr.get(1));
        arr.remove(-1); // Remove last
        assertEquals(1, arr.size());
        assertEquals("a", arr.get(0));
    }

    @Test
    public void testRemoveByValue() {
        Array arr = Array.of("a", "b", "c", "b");
        arr.remove("b");
        assertEquals(3, arr.size());
        assertEquals("a", arr.get(0));
        assertEquals("c", arr.get(1));
        assertEquals("b", arr.get(2)); // Second "b" remains
    }

    @Test
    public void testComplement() {
        Array arr = Array.of("b", "c", "d", "e");
        Array complement = Array.of("a", "b", "c").complement(arr);
        assertEquals(2, complement.size());
        assertEquals("d", complement.get(0));
        assertEquals("e", complement.get(1));
        arr = Array.of("d", "e", "f");
        assertEquals(arr, Array.of("a", "b", "c").complement(arr));
    }

    @Test
    public void testIntersection() {
        Array arr1 = Array.of("a", "b", "c");
        Array arr2 = Array.of("b", "c", "d", null);
        Array intersection = arr1.intersection(arr2);
        Array none = arr1.intersection(Array.of("d", "e", "f"));
        assertEquals(2, intersection.size());
        assertEquals("b", intersection.get(0));
        assertEquals("c", intersection.get(1));
        assertEquals(0, none.size());
    }

    @Test
    public void testUnion() {
        Array arr1 = Array.of("a", "b", "c");
        Array arr2 = Array.of("b", "c", "d");
        Array union = arr1.union(arr2);
        assertEquals(4, union.size());
        assertTrue(union.containsAll(arr1));
        assertTrue(union.containsAll(arr2));
    }

    @Test
    public void testSort() {
        Array arr = Array.of("c", "a", "b");
        arr.sort();
        assertEquals("a", arr.get(0));
        assertEquals("b", arr.get(1));
        assertEquals("c", arr.get(2));
        arr.seal(true);
        assertThrows(UnsupportedOperationException.class, () -> arr.sort());
    }

    @Test
    public void testSortWithKey() {
        Dict a = new Dict().set("name", "alice");
        Dict b = new Dict().set("name", "bob");
        Dict c = new Dict().set("name", "charlie");
        Array arr = Array.of(b, c, a);
        arr.sort("name");
        assertEquals(a, arr.getDict(0));
        assertEquals(b, arr.getDict(1));
        assertEquals(c, arr.getDict(2));
    }
}
