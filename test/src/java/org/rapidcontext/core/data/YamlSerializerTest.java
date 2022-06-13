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
 * Unit tests for the YAML serializer.
 */
@SuppressWarnings("javadoc")
public class YamlSerializerTest {

    @Test
    public void testSerializeValues() {
        assertEquals("test\n", YamlSerializer.serialize("test"));
        assertEquals("123\n", YamlSerializer.serialize(Integer.valueOf(123)));
        assertEquals("true\n", YamlSerializer.serialize(Boolean.TRUE));
        assertEquals("'@0'\n", YamlSerializer.serialize(new Date(0)));
    }

    @Test
    public void testSerializeFull() throws IOException {
        Dict root = new Dict();
        root.add("a", "one");
        root.addInt("b", 2);
        root.add("c", new Date(0));
        Dict dict = new Dict();
        dict.add("key", "value");
        root.add("object", dict);
        Array arr = new Array();
        arr.add("item 1");
        arr.add("item 2");
        root.add("array", arr);
        try (InputStream is = getClass().getResourceAsStream("testdata.yaml")) {
            assertEquals(FileUtil.readText(is, "UTF-8"), YamlSerializer.serialize(root));
        }
    }
}
