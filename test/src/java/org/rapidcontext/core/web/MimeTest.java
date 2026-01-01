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

package org.rapidcontext.core.web;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class MimeTest {

    @Test
    public void testType() {
        assertEquals("application/octet-stream", Mime.type((File) null));
        assertEquals("application/octet-stream", Mime.type((String) null));
        assertEquals("application/octet-stream", Mime.type(""));
        assertEquals("application/octet-stream", Mime.type("unknown.xyz"));
        assertEquals("application/octet-stream", Mime.type("filewithoutextension"));
        assertEquals("text/plain", Mime.type("test.txt"));
        assertEquals("text/plain", Mime.type(new File("test.TXT")));
        assertEquals("text/html", Mime.type("test.htm"));
        assertEquals("text/html", Mime.type("test.HTML"));
        assertEquals("text/html", Mime.type(new File("test.html")));
        assertEquals("text/css", Mime.type("style.css"));
        assertEquals("text/css", Mime.type(new File("test.css")));
        assertEquals("text/javascript", Mime.type("script.js"));
        assertEquals("text/javascript", Mime.type("script.cjs"));
        assertEquals("text/javascript", Mime.type("script.mjs"));
        assertEquals("application/json", Mime.type("data.json"));
        assertEquals("text/x-java-properties", Mime.type("config.properties"));
        assertEquals("text/xml", Mime.type("data.xml"));
        assertEquals("text/yaml", Mime.type("config.yml"));
        assertEquals("text/yaml", Mime.type("config.yaml"));
        assertEquals("text/markdown", Mime.type("README.md"));
        assertEquals("image/gif", Mime.type("image.gif"));
        assertEquals("image/jpeg", Mime.type("image.jpg"));
        assertEquals("image/jpeg", Mime.type("image.jpeg"));
        assertEquals("image/png", Mime.type("image.PNG"));
        assertEquals("image/svg+xml", Mime.type("image.svg"));
        assertEquals("image/vnd.microsoft.icon", Mime.type("favicon.ico"));
    }

    @Test
    public void testIsText() {
        assertTrue(Mime.isText("text/plain"));
        assertTrue(Mime.isText("text/html"));
        assertTrue(Mime.isText("TEXT/CSS"));
        assertTrue(Mime.isText("text/javascript"));
        assertTrue(Mime.isText("text/x-java-properties"));
        assertTrue(Mime.isText("text/x-dummy-format"));
        assertTrue(Mime.isText("application/XHTML"));
        assertTrue(Mime.isText("application/x-javascript"));
        assertTrue(Mime.isText("application/json"));
        assertTrue(Mime.isText("application/xml"));
        assertTrue(Mime.isText("application/yaml"));
        assertTrue(Mime.isText("application/x-yaml"));
        assertTrue(Mime.isText("image/svg+xml"));

        assertFalse(Mime.isText(null));
        assertFalse(Mime.isText("image/png"));
        assertFalse(Mime.isText("application/octet-stream"));
        assertFalse(Mime.isText("application/pdf"));
        assertFalse(Mime.isText("video/mp4"));
        assertFalse(Mime.isText("audio/mp3"));
    }

    @Test
    public void testIsMatch() {
        assertFalse(Mime.isMatch(null, Mime.TEXT));
        assertFalse(Mime.isMatch("text/plain", null));
        assertTrue(Mime.isMatch("TEXT/PLAIN", Mime.TEXT));
        assertFalse(Mime.isMatch("TEXT/PLAIN", Mime.HTML));
        assertTrue(Mime.isMatch("text/html", Mime.HTML));
        assertTrue(Mime.isMatch("text/xhtml; charset=utf-8", Mime.HTML));
        assertTrue(Mime.isMatch("application/xhtml+xml;q=0.9", Mime.HTML));
        assertFalse(Mime.isMatch("application/html", Mime.HTML));
        assertFalse(Mime.isMatch("application/json", Mime.HTML));
    }
}
