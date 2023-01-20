/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2023 Per Cederberg. All rights reserved.
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
import static org.rapidcontext.core.data.JsonSerializer.*;

/**
 * Unit tests for the JSON serializer.
 */
@SuppressWarnings("javadoc")
public class JsonSerializerTest {

    @Test
    public void testTypeSupport() {
        assertEquals("\"test\"", serialize("test", false));
        assertEquals("123", serialize(Integer.valueOf(123), false));
        assertEquals("true", serialize(Boolean.TRUE, false));
        assertEquals("\"@0\"", serialize(new Date(0), false));
    }

    @Test
    public void testSerialize() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("jsondata.json")) {
            String text = FileUtil.readText(is, "UTF-8").trim();
            assertEquals(text, serialize(buildDict(), true));
        }
    }

    @Test
    public void testUnserialize() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("jsondata.json")) {
            assertEquals(buildDict(), unserialize(is));
        }
    }

    private Dict buildDict() {
        Dict root = new Dict();
        root.set("id", "jsondata");
        root.set("a", "abc\u00E5\u00E4\u00F6");
        root.setInt("b", 2);
        root.setBoolean("c", false);
        root.set("d", new Date(0));
        root.set("object", new Dict().set("key", "value"));
        Array arr = new Array();
        arr.add("item 1");
        arr.add("item 2");
        root.set("array", arr);
        return root;
    }
}
