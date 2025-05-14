package org.rapidcontext.core.data;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for the TextEncoding helper methods.
 */
@SuppressWarnings("javadoc")
public class TextEncodingTest {

    @Test
    public void testEncodeProperty() {
        assertEquals("", TextEncoding.encodeProperty(null, true));
        assertEquals("\\ \\t\\n\\\n\\ ", TextEncoding.encodeProperty(" \t\r\n ", true));
        assertEquals("\\ \\t\\r\\n ", TextEncoding.encodeProperty(" \t\r\n ", false));
        assertEquals("\\\\", TextEncoding.encodeProperty("\\", true));
        assertEquals("abc123", TextEncoding.encodeProperty("abc123", true));
        assertEquals("\\u00e5\\u00e4\\u00f6", TextEncoding.encodeProperty("\u00E5\u00E4\u00F6", true));
        assertEquals("emoji", "\\ud83e\\udd16", TextEncoding.encodeProperty("\uD83E\uDD16", true));
    }

    @Test
    public void testEncodeJson() {
        assertEquals("null", TextEncoding.encodeJson(null));
        assertEquals("\" \\t\\r\\n\"", TextEncoding.encodeJson(" \t\r\n"));
        assertEquals("\"\\\\\\\"'\"", TextEncoding.encodeJson("\\\"'"));
        assertEquals("\"abc123\"", TextEncoding.encodeJson("abc123"));
        assertEquals("\"\\u00e5\\u00e4\\u00f6\"", TextEncoding.encodeJson("\u00E5\u00E4\u00F6"));
        assertEquals("emoji", "\"\\ud83e\\udd16\"", TextEncoding.encodeJson("\uD83E\uDD16"));
    }

    @Test
    public void testEncodeXml() {
        assertEquals("", TextEncoding.encodeXml(null, true));
        assertEquals("\r\n", TextEncoding.encodeXml("\r\n", true));
        assertEquals("&#13;&#10;", TextEncoding.encodeXml("\r\n", false));
        assertEquals("&lt;&gt;&amp;&quot;'", TextEncoding.encodeXml("<>&\"'", true));
        assertEquals("abc123", TextEncoding.encodeXml("abc123", true));
        assertEquals("&#229;&#228;&#246;", TextEncoding.encodeXml("\u00E5\u00E4\u00F6", true));
        assertEquals("emoji", "&#129302;", TextEncoding.encodeXml("\uD83E\uDD16", true));
    }
}
