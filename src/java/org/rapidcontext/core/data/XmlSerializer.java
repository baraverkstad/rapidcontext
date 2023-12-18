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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.util.DateUtil;
import org.rapidcontext.util.ValueUtil;

/**
 * A data serializer for XML. It will read and write stand-alone XML
 * documents in the UTF-8 character set. The following basic
 * requirements must be met in order to serialize an object:
 *
 * <ul>
 *   <li>No circular references are permitted.
 *   <li>String, Integer, Boolean, Date, Array and Dict values are
 *       supported.
 *   <li>Other value types are converted to strings.
 *   <li>Key names should consist of valid XML tag characters (or
 *       will be transformed).
 * </ul>
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public final class XmlSerializer {

    /**
     * The XML file prolog (XML declaration).
     */
    protected static final String PROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

    /**
     * Serializes an object into an XML representation.
     *
     * @param obj            the object to convert, or null
     * @param os             the output stream to write to
     *
     * @throws IOException if the data couldn't be serialized
     */
    public static void serialize(Object obj, OutputStream os) throws IOException {
        try (OutputStreamWriter ow = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
            ow.write(serialize("data", obj));
            ow.flush();
        }
    }

    /**
     * Serializes an object into an XML representation. The string
     * returned is a stand-alone XML document marked as being in the
     * UTF-8 charset.
     *
     * @param id             the data identifier
     * @param obj            the object to convert, or null
     *
     * @return an XML representation
     */
    public static String serialize(String id, Object obj) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(PROLOG);
        toXml(id, obj, 0, buffer);
        buffer.append("\n");
        return buffer.toString();
    }

    /**
     * Unserializes an object from an XML representation.
     *
     * @param is             the input stream to load
     *
     * @return the object read
     *
     * @throws IOException if an error occurred while reading
     */
    public static Object unserialize(InputStream is) throws IOException {
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            // prevent xxe: https://rules.sonarsource.com/java/RSPEC-2755
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            XMLEventReader reader = factory.createXMLEventReader(is);
            while (reader.hasNext() && !reader.peek().isStartElement()) {
                reader.nextEvent();
            }
            Object res = fromXml(reader);
            reader.close();
            return res;
        } catch (Exception e) {
            throw new IOException("Failed to unserialize XML", e);
        }
    }

    /**
     * Serializes an object into an XML representation.
     *
     * @param id             the data identifier
     * @param obj            the object to convert
     * @param indent         the current indentation level
     * @param buffer         the string buffer to append into
     */
    private static void toXml(String id, Object obj, int indent, StringBuilder buffer) {
        if (obj == null) {
            buffer.append("<");
            tagName(id, buffer);
            buffer.append(" type=\"null\"/>");
        } else if (obj instanceof Dict) {
            toXml(id, (Dict) obj, indent, buffer);
        } else if (obj instanceof Array) {
            toXml(id, (Array) obj, indent, buffer);
        } else if (obj instanceof Map) {
            toXml(id, Dict.from((Map<?, ?>) obj), indent, buffer);
        } else if (obj instanceof Iterable) {
            toXml(id, Array.from((Iterable<?>) obj), indent, buffer);
        } else if (obj instanceof Boolean) {
            tagStart(id, "boolean", buffer);
            buffer.append(TextEncoding.encodeXml(obj.toString(), false));
            tagEnd(id, buffer);
        } else if (obj instanceof Date) {
            tagStart(id, "date", buffer);
            buffer.append(DateUtil.asEpochMillis((Date) obj));
            tagEnd(id, buffer);
        } else if (obj instanceof Number) {
            tagStart(id, "number", buffer);
            buffer.append(TextEncoding.encodeXml(obj.toString(), false));
            tagEnd(id, buffer);
        } else if (obj instanceof Class) {
            tagStart(id, "class", buffer);
            buffer.append(TextEncoding.encodeXml(((Class<?>) obj).getName(), false));
            tagEnd(id, buffer);
        } else {
            tagStart(id, null, buffer);
            buffer.append(TextEncoding.encodeXml(obj.toString(), false));
            tagEnd(id, buffer);
        }
    }

    /**
     * Serializes a dictionary into an XML representation.
     *
     * @param id             the data identifier
     * @param dict           the dictionary to convert
     * @param indent         the current indentation level
     * @param buffer         the string buffer to append into
     */
    private static void toXml(String id, Dict dict, int indent, StringBuilder buffer) {
        tagStart(id, "object", buffer);
        for (String key : dict.keys()) {
            buffer.append("\n");
            buffer.append(StringUtils.repeat("  ", indent + 1));
            toXml(key, dict.get(key), indent + 1, buffer);
        }
        buffer.append("\n");
        buffer.append(StringUtils.repeat("  ", indent));
        tagEnd(id, buffer);
    }

    /**
     * Serializes an array into an XML representation.
     *
     * @param id             the data identifier
     * @param arr            the array to convert
     * @param indent         the current indentation level
     * @param buffer         the string buffer to append into
     */
    private static void toXml(String id, Array arr, int indent, StringBuilder buffer) {
        tagStart(id, "array", buffer);
        for (Object o : arr) {
            buffer.append("\n");
            buffer.append(StringUtils.repeat("  ", indent + 1));
            toXml("item", o, indent + 1, buffer);
        }
        buffer.append("\n");
        buffer.append(StringUtils.repeat("  ", indent));
        tagEnd(id, buffer);
    }

    /**
     * Writes an XML start tag.
     *
     * @param id             the tag name (identifier)
     * @param type           the data type, or null for none
     * @param buffer         the string buffer to append into
     */
    private static void tagStart(String id, String type, StringBuilder buffer) {
        buffer.append("<");
        tagName(id, buffer);
        if (type != null) {
            buffer.append(" type=\"");
            buffer.append(type);
            buffer.append("\"");
        }
        buffer.append(">");
    }

    /**
     * Writes an XML end tag.
     *
     * @param id             the tag name (identifier)
     * @param buffer         the string buffer to append into
     */
    private static void tagEnd(String id, StringBuilder buffer) {
        buffer.append("</");
        tagName(id, buffer);
        buffer.append(">");
    }

    /**
     * Writes an XML tag name.
     *
     * @param id             the tag name (identifier)
     * @param buffer         the string buffer to append into
     */
    private static void tagName(String id, StringBuilder buffer) {
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (i == 0 && !CharUtils.isAsciiAlpha(c)) {
                c = '_';
            } else if (!CharUtils.isAsciiAlphanumeric(c)) {
                c = '_';
            }
            buffer.append(c);
        }
    }

    /**
     * Unserializes the next XML events into a Java object.
     *
     * @param reader         the XML event iterator
     * @return the next Java object read from the stream
     * @throws Exception if the XML couldn't be parsed correctly
     */
    private static Object fromXml(XMLEventReader reader) throws Exception {
        XMLEvent evt = reader.nextEvent();
        if (evt.isStartElement()) {
            Attribute attr = evt.asStartElement().getAttributeByName(new QName("type"));
            String type = (attr == null) ? "string" : attr.getValue();
            Object val;
            if (type.equalsIgnoreCase("null")) {
                val = null;
            } else if (type.equalsIgnoreCase("object")) {
                val = fromXmlObject(reader);
            } else if (type.equalsIgnoreCase("array")) {
                val = fromXmlArray(reader);
            } else {
                val = fromXml(reader);
            }
            String str = (val == null) ? "" : val.toString();
            if (type.equalsIgnoreCase("boolean")) {
                val = ValueUtil.isOn(str);
            } else if (type.equalsIgnoreCase("date")) {
                if (DateUtil.isEpochFormat(str)) {
                    val = new Date(Long.parseLong(str.substring(1)));
                } else {
                    throw new Exception("unsupported XML date format: " + str);
                }
            } else if (type.equalsIgnoreCase("number")) {
                if (str.length() > 0 && str.length() <= 9 && StringUtils.isNumeric(str)) {
                    val = Integer.valueOf(str);
                } else {
                    throw new Exception("unsupported XML number format: " + str);
                }
            }
            evt = reader.nextEvent();
            if (!evt.isEndElement()) {
                throw new Exception("unexpected XML data: " + evt);
            }
            return val;
        } else if (evt.isCharacters() || evt.isEntityReference()) {
            String str = evt.asCharacters().toString();
            XMLEvent next = reader.peek();
            while (next.isCharacters() || next.isEntityReference()) {
                reader.nextEvent();
                str += next.asCharacters().toString();
                next = reader.peek();
            }
            return str;
        } else {
            throw new Exception("unexpected XML value: " + evt);
        }
    }

    /**
     * Unserializes an object from a stream of XML events. The opening
     * and closing tags are not handled by this method.
     *
     * @param reader         the XML event iterator
     * @return the dictionary read from the stream
     * @throws Exception if the XML couldn't be parsed correctly
     */
    private static Dict fromXmlObject(XMLEventReader reader) throws Exception {
        Dict dict = new Dict();
        while (reader.hasNext()) {
            XMLEvent next = reader.peek();
            if (next.isStartElement()) {
                String name = next.asStartElement().getName().getLocalPart();
                dict.add(name, fromXml(reader));
            } else if (next.isEndElement()) {
                break;
            } else if (next.isCharacters() && next.asCharacters().isWhiteSpace()) {
                reader.nextEvent();
            } else {
                throw new Exception("unexpected XML in object: " + next);
            }
        }
        return dict;
    }

    /**
     * Unserializes an array from a stream of XML events. The opening
     * and closing tags are not handled by this method.
     *
     * @param reader         the XML event iterator
     * @return the array read from the stream
     * @throws Exception if the XML couldn't be parsed correctly
     */
    private static Array fromXmlArray(XMLEventReader reader) throws Exception {
        Array arr = new Array();
        while (reader.hasNext()) {
            XMLEvent next = reader.peek();
            if (next.isStartElement()) {
                arr.add(fromXml(reader));
            } else if (next.isEndElement()) {
                break;
            } else if (next.isCharacters() && next.asCharacters().isWhiteSpace()) {
                reader.nextEvent();
            } else {
                throw new Exception("unexpected XML in array: " + next);
            }
        }
        return arr;
    }

    // No instances
    private XmlSerializer() {}
}
