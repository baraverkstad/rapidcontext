/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2019 Per Cederberg. All rights reserved.
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

package org.rapidcontext.app.web;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.core.web.Request;
import org.rapidcontext.util.HttpUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * A WebDAV request handler. This class is used to help analyzing a
 * PROPFIND request and generate the proper response.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class WebDavRequest implements HttpUtil {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(WebDavRequest.class.getName());

    /**
     * The WebDAV display name property constant.
     */
    public static final String PROP_DISPLAY_NAME = "displayname";

    /**
     * The WebDAV creation datetime property constant.
     */
    public static final String PROP_CREATION_DATE = "creationdate";

    /**
     * The WebDAV last modified datetime property constant.
     */
    public static final String PROP_LAST_MODIFIED = "getlastmodified";

    /**
     * The WebDAV resource type property constant.
     */
    public static final String PROP_RESOURCE_TYPE = "resourcetype";

    /**
     * The WebDAV MIME content type property constant.
     */
    public static final String PROP_CONTENT_TYPE = "getcontenttype";

    /**
     * The WebDAV content length property constant.
     */
    public static final String PROP_CONTENT_LENGTH = "getcontentlength";

    /**
     * The WebDAV ETag property constant.
     */
    public static final String PROP_ETAG = "getetag";

    /**
     * The WebDAV supported lock property constant.
     */
    public static final String PROP_SUPPORTED_LOCK = "supportedlock";

    /**
     * The WebDAV lock discovery property constant.
     */
    public static final String PROP_LOCK_DISCOVERY = "lockdiscovery";

    /**
     * The WebDAV source property constant.
     */
    public static final String PROP_SOURCE = "source";

    /**
     * The WebDAV quota used bytes property constant (RFC 4331).
     */
    public static final String PROP_QUOTA_USED_BYTES = "quota-used-bytes";

    /**
     * The WebDAV quota available bytes property constant (RFC 4331).
     */
    public static final String PROP_QUOTA_AVAIL_BYTES = "quota-available-bytes";

    /**
     * The default properties for a collection.
     */
    private static LinkedHashMap<String,String> PROPS_COLLECTION =
        new LinkedHashMap<>();

    /**
     * The default properties for a file.
     */
    private static LinkedHashMap<String,String> PROPS_FILE = new LinkedHashMap<>();

    /**
     * The date format used for the creation date property.
     */
    public static final SimpleDateFormat CREATION_DATE_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * The date format used for the last modified date property.
     */
    public static final SimpleDateFormat LAST_MODIFIED_DATE_FORMAT =
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    // Static initializer for property collections and time zones
    static {
        PROPS_COLLECTION.put(PROP_DISPLAY_NAME, "");
        PROPS_COLLECTION.put(PROP_RESOURCE_TYPE, "");
        PROPS_COLLECTION.put(PROP_CREATION_DATE, "");
        PROPS_COLLECTION.put(PROP_LAST_MODIFIED, "");
        PROPS_COLLECTION.put(PROP_SUPPORTED_LOCK, "");
        PROPS_COLLECTION.put(PROP_LOCK_DISCOVERY, "");
        PROPS_COLLECTION.put(PROP_SOURCE, "");
        PROPS_COLLECTION.put(PROP_QUOTA_USED_BYTES, "");
        PROPS_COLLECTION.put(PROP_QUOTA_AVAIL_BYTES, "");
        PROPS_FILE.putAll(PROPS_COLLECTION);
        PROPS_FILE.put(PROP_CONTENT_TYPE, "");
        PROPS_FILE.put(PROP_CONTENT_LENGTH, "");
        PROPS_FILE.put(PROP_ETAG, "");
        CREATION_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
        LAST_MODIFIED_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
     * The HTTP request being wrapped.
     */
    private Request request;

    /**
     * The property values flag. If set to true, the property values
     * should also be returned. Otherwise only the property names.
     */
    private boolean propertyValues = true;

    /**
     * The properties found in the query. This will contain all
     * properties if the query doesn't specify any properties.
     */
    private LinkedHashMap<String,String> properties = new LinkedHashMap<>();

    /**
     * The property namespace URI to abbreviation map.
     */
    private LinkedHashMap<String,String> propertyNS = null;

    /**
     * The lock request information (if parsed and available).
     */
    private Dict lockInfo = null;

    /**
     * The array with result XML fragments. Each resource added will
     * be converted into XML snipplet added here.
     */
    private ArrayList<String> results = new ArrayList<>();

    /**
     * Creates a new WebDAV request.
     *
     * @param request        the HTTP request to read
     *
     * @throws Exception if the WebDAV request XML couldn't be parsed
     */
    public WebDavRequest(Request request) throws Exception {
        String   xml = request.getInputString();
        boolean  isCollection = request.getPath().endsWith("/");

        LOG.fine(request.getMethod() + " XML:\n" + xml);
        this.request = request;
        if (xml != null && xml.trim().length() > 0) {
            Element root = parseDOM(xml);
            if (request.hasMethod(METHOD.PROPFIND)) {
                if (parseChild(root, "propname") != null) {
                    propertyValues = false;
                }
                parsePropFind(parseChild(root, "prop"), isCollection);
            } else if (request.hasMethod(METHOD.LOCK)) {
                parseLockInfo(root);
            }
        } else if (request.hasMethod(METHOD.PROPFIND)) {
            parsePropFind(null, isCollection);
        }
    }

    /**
     * Parses the specified XML document and returns the document
     * element.
     *
     * @param xml            the XML string to parse
     *
     * @return the root document element
     *
     * @throws Exception if the XML string couldn't be parsed
     */
    private Element parseDOM(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        return doc.getDocumentElement();
    }

    /**
     * Finds a child node by tag name.
     *
     * @param parent         the parent node
     * @param name           the child node name
     *
     * @return the child node found, or
     *         null if not found
     */
    private Element parseChild(Element parent, String name) {
        if (parent != null) {
            Node child = parent.getFirstChild();
            while (child != null) {
                String localName = child.getLocalName();
                if (child instanceof Element && localName.equals(name)) {
                    return (Element) child;
                }
                child = child.getNextSibling();
            }
        }
        return null;
    }

    /**
     * Parses a property find request. All properties found will be
     * added to the query properties map. If no properties node was
     * provided, all standard properties will be added.
     *
     * @param node           the properties node, or null
     * @param isCollection   the collection flag
     */
    private void parsePropFind(Element node, boolean isCollection) {
        LinkedHashMap<String,String> defaults;
         defaults = isCollection ? PROPS_COLLECTION : PROPS_FILE;
        if (node == null) {
            properties.putAll(defaults);
        } else {
            Node child = node.getFirstChild();
            while (child != null) {
                if (child.getNodeType() != Node.ELEMENT_NODE) {
                    // Ignore non-elements
                } else if ("DAV:".equals(child.getNamespaceURI())) {
                    if (defaults.containsKey(child.getLocalName())) {
                        properties.put(child.getLocalName(), "");
                    } else {
                        properties.put(child.getLocalName(), null);
                    }
                } else {
                    String name = child.getNamespaceURI() + ":" +
                                  child.getLocalName();
                    properties.put(name, null);
                }
                child = child.getNextSibling();
            }
        }
    }

    /**
     * Parses the lock request. The lock details will be set in the
     * lock info dictionary.
     *
     * @param root           the XML document node
     */
    private void parseLockInfo(Element root) {
        Element  node;

        lockInfo = new Dict();
        lockInfo.set("href", request.getAbsolutePath());
        lockInfo.set("token", null);
        lockInfo.setInt("depth", depth());
        lockInfo.set("timeout", timeout());
        node = parseChild(root, "lockscope");
        if (parseChild(node, "exclusive") != null) {
            lockInfo.set("scope", "exclusive");
        } else if (parseChild(node, "shared") != null) {
            lockInfo.set("scope", "shared");
        } else {
            lockInfo.set("scope", "unknown");
        }
        lockInfo.set("type", "write");
        node = parseChild(root, "owner");
        if (parseChild(node, "href") != null) {
            lockInfo.set("owner", parseChild(node, "href").getTextContent());
        } else {
            lockInfo.set("owner", request.getHeader(HEADER.USER_AGENT));
        }
    }

    /**
     * Returns the requested depth of properties.
     *
     * @return the requested depth of properties, or
     *         -1 for infinity
     */
    public int depth() {
        try {
            return Integer.parseInt(request.getHeader(HEADER.DEPTH));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Returns the requested lock timeout value.
     *
     * @return the requested lock timeout value, or
     *         null if not specified
     */
    public String timeout() {
        return request.getHeader(HEADER.TIMEOUT);
    }

    /**
     * Returns information about the requested lock (if applicable).
     *
     * @return the requested lock, or
     *         null if the request wasn't a valid lock request
     */
    public Dict lockInfo() {
        return lockInfo;
    }

    /**
     * Adds a resource to the result with the specified dates and size.
     *
     * @param href           the root-relative resource link
     * @param created        the resource creation date
     * @param modified       the resource modification date
     * @param size           the resource size (in bytes)
     */
    public void addResource(String href, Date created, Date modified, long size) {
        LinkedHashMap<String,String>  props = new LinkedHashMap<>();
        String         name;
        String         str;

        props.putAll(properties);
        name = StringUtils.removeEnd(href, "/");
        name = StringUtils.substringAfterLast(href, "/");
        if (props.containsKey(PROP_DISPLAY_NAME)) {
            props.put(PROP_DISPLAY_NAME, name);
        }
        if (props.containsKey(PROP_CREATION_DATE)) {
            str = CREATION_DATE_FORMAT.format(created);
            props.put(PROP_CREATION_DATE, str);
        }
        if (props.containsKey(PROP_LAST_MODIFIED)) {
            str = LAST_MODIFIED_DATE_FORMAT.format(modified);
            props.put(PROP_LAST_MODIFIED, str);
        }
        if (props.containsKey(PROP_CONTENT_TYPE)) {
            props.put(PROP_CONTENT_TYPE, href.endsWith("/") ? null : Mime.type(name));
        }
        if (href.endsWith("/")) {
            if (props.containsKey(PROP_RESOURCE_TYPE)) {
                props.put(PROP_RESOURCE_TYPE, "<D:collection/>");
            }
            if (props.containsKey(PROP_CONTENT_LENGTH)) {
                props.put(PROP_CONTENT_LENGTH, "0");
            }
            if (props.containsKey(PROP_ETAG)) {
                props.put(PROP_ETAG, null);
            }
        } else {
            if (props.containsKey(PROP_CONTENT_LENGTH)) {
                props.put(PROP_CONTENT_LENGTH, String.valueOf(size));
            }
            if (props.containsKey(PROP_ETAG)) {
                str = "W/\"" + size + "-" + modified.getTime() + "\"";
                props.put(PROP_ETAG, str);
            }
        }
        // Fake quota properties to enable read-write access
        if (props.containsKey(PROP_QUOTA_USED_BYTES)) {
            props.put(PROP_QUOTA_USED_BYTES, "0");
        }
        if (props.containsKey(PROP_QUOTA_AVAIL_BYTES)) {
            props.put(PROP_QUOTA_AVAIL_BYTES, "1000000000");
        }
        addResource(href, props);
    }

    /**
     * Adds a resource to the result with the specified properties.
     *
     * @param href           the root-relative resource link
     * @param props          the resource properties
     */
    private void addResource(String href, LinkedHashMap<String,String> props) {
        StringBuilder buffer = new StringBuilder();
        xmlTagBegin(buffer, 1, "response");
        xmlTag(buffer, 2, "href", Helper.encodeUrl(href), false);
        xmlTagBegin(buffer, 2, "propstat");
        xmlTagBegin(buffer, 3, "prop");
        ArrayList<String> fails = new ArrayList<>();
        for (String key : props.keySet()) {
            String value = props.get(key);
            if (value == null) {
                fails.add(key);
            } else if (propertyValues) {
                xmlTag(buffer, 4, key, value, !value.startsWith("<"));
            } else {
                xmlTag(buffer, 4, key);
            }
        }
        xmlTagEnd(buffer, 3, "prop");
        xmlStatus(buffer, 3, STATUS.OK);
        xmlTagEnd(buffer, 2, "propstat");
        if (fails.size() > 0) {
            xmlTagBegin(buffer, 2, "propstat");
            xmlTagBegin(buffer, 3, "prop");
            for (int i = 0; i < fails.size(); i++) {
                String key = fails.get(i);
                if (key.indexOf(':') > 0) {
                    String value = StringUtils.substringBeforeLast(key, ":");
                    key = StringUtils.substringAfterLast(key, ":");
                    buffer.append(StringUtils.repeat("  ", 4));
                    buffer.append("<");
                    buffer.append(namespace(value));
                    buffer.append(":");
                    buffer.append(key);
                    buffer.append(" xmlns:");
                    buffer.append(namespace(value));
                    buffer.append("=\"");
                    buffer.append(value);
                    buffer.append("\"/>\n");
                } else {
                    xmlTag(buffer, 4, key);
                }
            }
            xmlTagEnd(buffer, 3, "prop");
            xmlStatus(buffer, 3, STATUS.NOT_FOUND);
            xmlTagEnd(buffer, 2, "propstat");
        }
        xmlTagEnd(buffer, 1, "response");
        results.add(buffer.toString());
    }

    /**
     * Returns the namespace abbreviation for the specified URL.
     *
     * @param href           the namespace URL
     *
     * @return the namespace abbreviation
     */
    private String namespace(String href) {
        if (propertyNS == null) {
            propertyNS = new LinkedHashMap<>();
        }
        if (!propertyNS.containsKey(href)) {
            char value = (char) ('E' + propertyNS.size());
            propertyNS.put(href, String.valueOf(value));
            LOG.fine("reserved namespace '" + propertyNS.get(href) + "' for " + href);
        }
        return propertyNS.get(href);
    }

    /**
     * Sends a lock response with the specified information.
     *
     * @param lockInfo       the lock information
     * @param timeout        the lock timeout
     */
    public void sendLockResponse(Dict lockInfo, int timeout) {
        StringBuilder  buffer = new StringBuilder();
        String         str;

        buffer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        buffer.append("<D:prop xmlns:D=\"DAV:\">\n");
        xmlTagBegin(buffer, 1, "lockdiscovery");
        xmlTagBegin(buffer, 2, "activelock");
        xmlTagBegin(buffer, 3, "lockroot");
        xmlTag(buffer, 4, "href", lockInfo.getString("href", ""), true);
        xmlTagEnd(buffer, 3, "lockroot");
        xmlTagBegin(buffer, 3, "locktoken");
        xmlTag(buffer, 4, "href", lockInfo.getString("token", ""), true);
        xmlTagEnd(buffer, 3, "locktoken");
        if (lockInfo.getInt("depth", -1) < 0) {
            xmlTag(buffer, 3, "depth", "infinity", true);
        } else {
            xmlTag(buffer, 3, "depth", lockInfo.getString("depth", ""), true);
        }
        str = "Seconds-" + timeout;
        xmlTag(buffer, 3, "timeout", str, false);
        str = "<D:" + lockInfo.get("type") + "/>";
        xmlTag(buffer, 3, "locktype", str, false);
        str = "<D:" + lockInfo.get("scope") + "/>";
        xmlTag(buffer, 3, "lockscope", str, false);
        xmlTagBegin(buffer, 3, "owner");
        xmlTag(buffer, 4, "href", lockInfo.getString("owner", ""), true);
        xmlTagEnd(buffer, 3, "owner");
        xmlTagEnd(buffer, 2, "activelock");
        xmlTagEnd(buffer, 1, "lockdiscovery");
        xmlTagEnd(buffer, 0, "prop");
        str = "<" + lockInfo.getString("token", "") + ">";
        request.setResponseHeader(HEADER.LOCK_TOKEN, str);
        request.sendText(STATUS.OK, Mime.XML[0], buffer.toString());
    }

    /**
     * Sends a multi-status response with the response fragments as
     * the request response.
     *
     * @see #addResource(String, Date, Date, long)
     */
    public void sendMultiResponse() {
        StringBuilder  buffer = new StringBuilder();

        buffer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        buffer.append("<D:multistatus xmlns:D=\"DAV:\">\n");
        for (int i = 0; i < results.size(); i++) {
            buffer.append(results.get(i));
        }
        xmlTagEnd(buffer, 0, "multistatus");
        request.sendText(STATUS.MULTI_STATUS, Mime.XML[0], buffer.toString());
    }

    /**
     * Sends a finite depth error as the request response.
     */
    public void sendErrorFiniteDepth() {
        StringBuilder  buffer = new StringBuilder();

        buffer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        buffer.append("<D:error xmlns:D=\"DAV:\">\n");
        xmlTag(buffer, 1, "propfind-finite-depth");
        xmlTagEnd(buffer, 0, "error");
        request.sendError(STATUS.FORBIDDEN, Mime.XML[0], buffer.toString());
    }

    /**
     * Writes a WebDAV status tag to the specified buffer.
     *
     * @param buffer         the buffer to write to
     * @param indent         the indentation level
     * @param status         the status code
     */
    private void xmlStatus(StringBuilder buffer, int indent, int status) {
        String content = "HTTP/1.1 " + status + " " + STATUS.asText(status);
        xmlTag(buffer, indent, "status", content, false);
    }

    /**
     * Writes an opening WebDAV XML tag to the specified buffer.
     *
     * @param buffer         the buffer to write to
     * @param indent         the indentation level
     * @param tag            the tag name (without namespace)
     */
    private void xmlTagBegin(StringBuilder buffer, int indent, String tag) {
        buffer.append(StringUtils.repeat("  ", indent));
        buffer.append("<D:");
        buffer.append(tag);
        buffer.append(">\n");
    }

    /**
     * Writes a closing WebDAV XML tag to the specified buffer.
     *
     * @param buffer         the buffer to write to
     * @param indent         the indentation level
     * @param tag            the tag name (without namespace)
     */
    private void xmlTagEnd(StringBuilder buffer, int indent, String tag) {
        buffer.append(StringUtils.repeat("  ", indent));
        buffer.append("</D:");
        buffer.append(tag);
        buffer.append(">\n");
    }

    /**
     * Writes a WebDAV XML tag without content to the specified buffer.
     *
     * @param buffer         the buffer to write to
     * @param indent         the indentation level
     * @param tag            the tag name (without namespace)
     */
    private void xmlTag(StringBuilder buffer, int indent, String tag) {
        buffer.append(StringUtils.repeat("  ", indent));
        buffer.append("<D:");
        buffer.append(tag);
        buffer.append("/>\n");
    }

    /**
     * Writes a WebDAV XML tag with content to the specified buffer.
     *
     * @param buffer         the buffer to write to
     * @param indent         the indentation level
     * @param tag            the tag name (without namespace)
     * @param content        the content data
     * @param escapeContent  the escape content flag
     */
    private void xmlTag(StringBuilder buffer,
                        int indent,
                        String tag,
                        String content,
                        boolean escapeContent) {

        buffer.append(StringUtils.repeat("  ", indent));
        buffer.append("<D:");
        buffer.append(tag);
        buffer.append(">");
        if (escapeContent) {
            buffer.append(StringEscapeUtils.escapeXml11(content));
        } else {
            buffer.append(content);
        }
        buffer.append("</D:");
        buffer.append(tag);
        buffer.append(">\n");
    }
}
