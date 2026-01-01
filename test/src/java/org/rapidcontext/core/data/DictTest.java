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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class DictTest {

    @Test
    public void testConstructor() {
        assertEquals(0, new Dict().size());
        assertEquals(0, new Dict(10).size());
    }

    @Test
    public void testFrom() {
        Map<Object, Object> map = Map.of(
            "bool", false,
            "int", 42,
            "str", "one",
            "array", Arrays.asList("nested", "array"),
            "dict", Map.of("key", "value"),
            123, "numeric",
            true, "boolean"
        );
        Dict dict = Dict.from(map);
        assertEquals(7, dict.size());
        assertEquals(false, dict.get("bool"));
        assertEquals(42, dict.get("int"));
        assertEquals("one", dict.get("str"));
        assertTrue(dict.get("array") instanceof Array);
        assertTrue(dict.get("dict") instanceof Dict);
        assertEquals("numeric", dict.get("123"));
        assertEquals("boolean", dict.get("true"));
        map = new HashMap<>();
        map.put(null, null);
        assertEquals(null, dict.get("null"));
    }

    @Test
    public void testEqualsAndHashCode() {
        Dict dict = new Dict().set("a", 1).set("b", 2);
        assertFalse(dict.equals(null));
        assertFalse(dict.equals("not a dict"));
        assertFalse(dict.equals(new Dict().set("a", 1)));
        Dict same = new Dict().set("a", 1).set("b", 2);
        assertEquals(same, dict);
        assertEquals(same.hashCode(), dict.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("{}", new Dict().toString());
        assertEquals("{ key: value }", new Dict().set("key", "value").toString());
        Dict dict = new Dict().set("a", 1).set("b", 2).set("c", 3).set("d", 4).set("e", 5);
        assertTrue(dict.toString().contains("a"));
        assertTrue(dict.toString().contains("..."));
    }

    @Test
    public void testStream() {
        Dict dict = new Dict().set("a", 1).set("b", 2).set("c", 3);
        Map<String, Object> res = dict.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertEquals(1, res.get("a"));
        assertEquals(2, res.get("b"));
        assertEquals(3, res.get("c"));
        Map<String, Integer> res2 = dict.stream(Integer.class).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertEquals(Integer.valueOf(1), res2.get("a"));
        assertEquals(Integer.valueOf(2), res2.get("b"));
        assertEquals(Integer.valueOf(3), res2.get("c"));
    }

    @Test
    public void testCopy() {
        Dict dict = new Dict().set("key", "value");
        Dict copy = dict.copy();
        assertEquals(dict, copy);
        assertFalse(dict == copy);
        dict.set("key", "modified");
        assertEquals("modified", dict.get("key"));
        assertEquals("value", copy.get("key"));
        Dict nested = new Dict().set("nested", "value");
        dict = new Dict().set("dict", nested).set("array", Array.of("item"));
        copy = dict.copy();
        nested.set("nested", "modified");
        ((Array) dict.get("array")).add("modified");
        assertEquals("value", ((Dict) copy.get("dict")).get("nested"));
        assertEquals(1, ((Array) copy.get("array")).size());
    }

    @Test
    public void testSeal() {
        Array arr = Array.of("item");
        Dict nested = new Dict().set("nested", "value");
        Dict dict = new Dict().set("key", "value").set("dict", nested).set("array", arr);
        assertFalse(dict.isSealed());

        dict.seal(false);
        assertTrue(dict.isSealed());
        assertFalse(arr.isSealed());
        assertFalse(nested.isSealed());
        assertThrows(UnsupportedOperationException.class, () -> dict.set("new", "value"));
        assertThrows(UnsupportedOperationException.class, () -> dict.setDefault("other", "value"));
        assertThrows(UnsupportedOperationException.class, () -> dict.setAll(new Dict().set("d", 4)));
        assertThrows(UnsupportedOperationException.class, () -> dict.remove("key"));
        assertThrows(UnsupportedOperationException.class, () -> dict.add("key", "val4"));
        assertThrows(UnsupportedOperationException.class, () -> dict.merge(new Dict().set("e", 6)));

        dict.seal(true);
        assertTrue(dict.isSealed());
        assertTrue(arr.isSealed());
        assertTrue(nested.isSealed());
        assertThrows(UnsupportedOperationException.class, () -> nested.set("nested", "modified"));
        assertThrows(UnsupportedOperationException.class, () -> arr.add("modified"));
    }

    @Test
    public void testSize() {
        Dict dict = new Dict();
        assertEquals(0, dict.size());
        dict.set("a", 1);
        assertEquals(1, dict.size());
        dict.set("b", 2);
        assertEquals(2, dict.size());
    }

    @Test
    public void testContainsKey() {
        Dict dict = new Dict().set("key", "value");
        assertTrue(dict.containsKey("key"));
        assertFalse(dict.containsKey("nonexistent"));
        assertFalse(dict.containsKey(null));
    }

    @Test
    public void testContainsValue() {
        Dict dict = new Dict().set("key", "value");
        assertTrue(dict.containsValue("value"));
        assertFalse(dict.containsValue("nonexistent"));
        assertFalse(dict.containsValue(null));
    }

    @Test
    public void testKeyOf() {
        Dict dict = new Dict().set("a", "val").set("b", "other").set("c", "val");
        assertEquals("a", dict.keyOf("val"));
        assertEquals("b", dict.keyOf("other"));
        assertNull(dict.keyOf("nonexistent"));
        assertNull(dict.keyOf(null));
    }

    @Test
    public void testKeys() {
        Dict dict = new Dict().set("a", 1).set("b", 2).set("c", 3);
        String[] keys = dict.keys();
        assertEquals(3, keys.length);
        assertEquals("a", keys[0]);
        assertEquals("b", keys[1]);
        assertEquals("c", keys[2]);
    }

    @Test
    public void testGet() {
        Dict dict = new Dict().set("key", "value");
        assertEquals("value", dict.get("key"));
        assertNull(dict.get("nonexistent"));
        assertNull(dict.get(null));
        assertEquals(Integer.valueOf(123), dict.set("key", "123").get("key", Integer.class));
        assertNull(dict.get("nonexistent", Integer.class));
        assertEquals(Integer.valueOf(123), dict.get("key", Integer.class, 999));
        assertEquals(Integer.valueOf(999), dict.get("nonexistent", Integer.class, 999));
        assertEquals(Integer.valueOf(123), dict.getElse("key", Integer.class, () -> 999));
        assertEquals(Integer.valueOf(999), dict.getElse("nonexistent", Integer.class, () -> 999));
    }

    @Test
    public void testGetDict() {
        Dict nested = new Dict().set("nested", "value");
        Dict dict = new Dict().set("dict", nested).set("str", "value");
        assertEquals("value", dict.getDict("dict").get("nested"));
        assertThrows(ClassCastException.class, () -> dict.getDict("str"));
    }

    @Test
    public void testGetArray() {
        Dict dict = new Dict().set("array", Array.of("item")).set("str", "value");
        assertEquals(1, dict.getArray("array").size());
        assertEquals("item", dict.getArray("array").get(0));
        assertThrows(ClassCastException.class, () -> dict.getArray("str"));
    }

    @Test
    public void testSet() {
        Dict dict = new Dict();
        dict.set("key", "value");
        assertEquals(1, dict.size());
        assertEquals("value", dict.get("key"));
        dict.set("key", "new");
        assertEquals(1, dict.size());
        assertEquals("new", dict.get("key"));
        assertThrows(NullPointerException.class, () -> dict.set(null, "value"));
        assertThrows(NullPointerException.class, () -> dict.set("", "value"));
        assertThrows(NullPointerException.class, () -> dict.set("   ", "value"));
    }

    @Test
    public void testSetDefault() {
        Dict dict = new Dict();
        dict.setDefault("key", "default");
        assertEquals("default", dict.get("key"));
        dict.setDefault("key", "new");
        assertEquals("default", dict.get("key"));
        dict.set("key", null);
        dict.setDefault("key", "default");
        assertEquals("default", dict.get("key"));
    }

    @Test
    public void testSetIfNull() {
        Dict dict = new Dict();
        dict.setIfNull("key", () -> "default");
        assertEquals("default", dict.get("key"));
        dict.setIfNull("key", () -> "new");
        assertEquals("default", dict.get("key"));
        dict.set("key", null);
        dict.setIfNull("key", () -> "default");
        assertEquals("default", dict.get("key"));
    }

    @Test
    public void testSetAll() {
        Dict dict = new Dict().set("a", 1);
        dict.setAll(new Dict().set("b", 2).set("c", 3));
        assertEquals(3, dict.size());
        assertEquals(1, dict.get("a"));
        assertEquals(2, dict.get("b"));
        assertEquals(3, dict.get("c"));
    }

    @Test
    public void testAdd() {
        Dict dict = new Dict();
        dict.add("key", "val1");
        assertEquals("val1", dict.get("key"));
        dict.add("key", "val2");
        assertEquals("val1", dict.get("key"));
        assertEquals("val2", dict.get("key_1"));
        dict.add("key", "val3");
        assertEquals("val3", dict.get("key_2"));
    }

    @Test
    public void testMerge() {
        Dict dict = new Dict().set("a", 1).set("b", 2);
        dict.merge(new Dict().set("b", 3).set("c", 4));
        assertEquals(3, dict.size());
        assertEquals(1, dict.get("a"));
        assertEquals(3, dict.get("b"));
        assertEquals(4, dict.get("c"));
        dict.merge(new Dict().set("b", null).set("d", 5));
        assertEquals(3, dict.size());
        assertEquals(1, dict.get("a"));
        assertFalse(dict.containsKey("b"));
        assertEquals(4, dict.get("c"));
        assertEquals(5, dict.get("d"));
    }

    @Test
    public void testRemove() {
        Dict dict = new Dict().set("a", 1).set("b", 2);
        dict.remove("a");
        assertEquals(1, dict.size());
        assertFalse(dict.containsKey("a"));
        assertTrue(dict.containsKey("b"));
        dict.remove("nonexistent");
        assertEquals(1, dict.size());
    }
}
