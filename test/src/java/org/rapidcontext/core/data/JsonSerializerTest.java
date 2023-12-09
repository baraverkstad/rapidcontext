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
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

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
        assertEquals("123", serialize(123, false));
        assertEquals("true", serialize(true, false));
        assertEquals("\"@0\"", serialize(new Date(0), false));
        assertEquals("[1,2]", serialize(Arrays.asList(1, 2), false));
        assertEquals("{\"a\":1}", serialize(Map.of("a", 1), false));
    }

    @Test
    public void testSerialize() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("jsondata.json")) {
            String text = FileUtil.readText(is).trim();
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
        return new Dict()
            .set("id", "jsondata")
            .set("a", "abc\u00E5\u00E4\u00F6")
            .set("b", 2)
            .set("c", false)
            .set("d", new Date(0))
            .set("object", new Dict().set("key", "value"))
            .set("array", Array.of("item 1", "item 2"))
            .set("empty", new Dict().add("obj", new Dict()).add("arr", new Array()));
    }
}
