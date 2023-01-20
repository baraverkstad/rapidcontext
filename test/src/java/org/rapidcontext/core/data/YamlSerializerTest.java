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
import static org.rapidcontext.core.data.YamlSerializer.*;

/**
 * Unit tests for the YAML serializer.
 */
@SuppressWarnings("javadoc")
public class YamlSerializerTest {

    @Test
    public void testTypeSupport() {
        assertEquals("test\n", serialize("test"));
        assertEquals("123\n", serialize(Integer.valueOf(123)));
        assertEquals("true\n", serialize(Boolean.TRUE));
        assertEquals("'@0'\n", serialize(new Date(0)));
    }

    @Test
    public void testSerialize() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("yamldata.yaml")) {
            String text = FileUtil.readText(is, "UTF-8");
            assertEquals(text, serialize(buildDict()));
        }
    }

    @Test
    public void testUnserialize() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("yamldata.yaml")) {
            assertEquals(buildDict(), unserialize(is));
        }
    }

    private Dict buildDict() {
        Dict root = new Dict();
        root.set("id", "yamldata");
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
