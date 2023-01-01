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
import static org.rapidcontext.core.data.XmlSerializer.*;

/**
 * Unit tests for the XML serializer.
 */
@SuppressWarnings("javadoc")
public class XmlSerializerTest {

    @Test
    public void testTypeSupport() {
        assertEquals(PROLOG + "<o>test</o>\n", serialize("o", "test"));
        assertEquals(PROLOG + "<o type=\"number\">123</o>\n", serialize("o", Integer.valueOf(123)));
        assertEquals(PROLOG + "<o type=\"boolean\">true</o>\n", serialize("o", Boolean.TRUE));
        assertEquals(PROLOG + "<o type=\"date\">@0</o>\n", serialize("o", new Date(0)));
    }

    @Test
    public void testSerialize() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("xmldata.xml")) {
            String text = FileUtil.readText(is, "UTF-8");
            assertEquals(text, serialize("root", buildDict()));
        }
    }

    @Test
    public void testUnserialize() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("xmldata.xml")) {
            assertEquals(buildDict(), XmlSerializer.unserialize(is));
        }
    }

    private Dict buildDict() {
        Dict root = new Dict();
        root.add("id", "xmldata");
        root.add("a", "abc\u00E5\u00E4\u00F6");
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
