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

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.junit.Test;
import org.rapidcontext.util.FileUtil;

/**
 * Unit tests for the JSON serializer.
 */
@SuppressWarnings("javadoc")
public class JsonSerializerTest {

    @Test
    public void testTypeSupport() {
        assertEquals("\"test\"", JsonSerializer.serialize("test", false));
        assertEquals("123", JsonSerializer.serialize(Integer.valueOf(123), false));
        assertEquals("true", JsonSerializer.serialize(Boolean.TRUE, false));
        assertEquals("\"@0\"", JsonSerializer.serialize(new Date(0), false));
    }

    @Test
    public void testSerialize() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("testdata.json")) {
            String text = FileUtil.readText(is, "UTF-8").trim();
            assertEquals(text, JsonSerializer.serialize(buildDict(), true));
        }
    }

    @Test
    public void testUnserialize() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("testdata.json")) {
            assertEquals(buildDict(), JsonSerializer.unserialize(is));
        }
    }

    private Dict buildDict() {
        Dict root = new Dict();
        root.add("a", "one");
        root.addInt("b", 2);
        root.addBoolean("c", false);
        root.add("d", new Date(0));
        Dict dict = new Dict();
        dict.add("key", "value");
        root.add("object", dict);
        Array arr = new Array();
        arr.add("item 1");
        arr.add("item 2");
        root.add("array", arr);
        return root;
    }
}
