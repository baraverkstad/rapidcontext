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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.junit.Test;
import org.rapidcontext.util.FileUtil;
import static org.rapidcontext.core.data.XmlSerializer.*;

@SuppressWarnings("javadoc")
public class XmlSerializerTest {

    @Test
    public void testTypeSupport() {
        assertEquals(PROLOG + "<o>test</o>\n", serialize("o", "test"));
        assertEquals(PROLOG + "<o type=\"number\">123</o>\n", serialize("o", 123));
        assertEquals(PROLOG + "<o type=\"boolean\">true</o>\n", serialize("o", true));
        assertEquals(PROLOG + "<o type=\"date\">@0</o>\n", serialize("o", new Date(0)));
        StringBuilder buf = new StringBuilder();
        buf.append(PROLOG);
        buf.append("<a type=\"array\">\n");
        buf.append("  <item type=\"number\">1</item>\n");
        buf.append("  <item type=\"number\">2</item>\n");
        buf.append("</a>\n");
        assertEquals(buf.toString(), serialize("a", Arrays.asList(1, 2)));
        buf.setLength(0);
        buf.append(PROLOG);
        buf.append("<o type=\"object\">\n");
        buf.append("  <a type=\"number\">1</a>\n");
        buf.append("</o>\n");
        assertEquals(buf.toString(), serialize("o", Map.of("a", 1)));
    }

    @Test
    public void testSerialize() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("xmldata.xml")) {
            String text = FileUtil.readText(is);
            assertEquals(text, serialize("root", buildDict()));
        }
    }

    @Test
    public void testUnserialize() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("xmldata.xml")) {
            assertEquals(buildDict(), XmlSerializer.unserialize(FileUtil.readText(is)));
        }
    }

    private Dict buildDict() {
        return new Dict()
            .set("id", "xmldata")
            .set("a", "abc\u00E5\u00E4\u00F6")
            .set("b", 2)
            .set("c", false)
            .set("d", new Date(0))
            .set("object", new Dict().set("key", "value"))
            .set("array", Array.of("item 1", "item 2"))
            .set("empty", new Dict().add("obj", new Dict()).add("arr", new Array()));
    }
}
