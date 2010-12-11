/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE.txt file for more details.
 */

package org.rapidcontext.app.web;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.core.web.Request;
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
public class WebDavRequest {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(WebDavRequest.class.getName());

    /**
     * The WebDAV HTTP 207 (Multi-Status) status code provides status
     * for multiple independent operations.
     */
    public static final int SC_MULTI_STATUS = 207;

    /**
     * The WebDAV HTTP 422 (Unprocessable Entity) status code means
     * that both request content type and syntax is correct, but that
     * the server is unable to process the contained instructions.
     * For example, this error condition may occur if an XML request
     * body contains well-formed (i.e., syntactically correct), but
     * semantically erroneous, XML instructions.
     */
    public static final int SC_UNPROCESSABLE_ENTITY = 422;

    /**
     * The WebDAV HTTP 423 (Locked) status code means the source or
     * destination resource of a method is locked.
     */
    public static final int SC_LOCKED = 423;

    /**
     * The WebDAV HTTP 424 (Failed Dependency) status code means that
     * the method could not be performed on the resource because the
     * requested action depended on another action and that action
     * failed.
     */
    public static final int SC_FAILED_DEPENDENCY = 424;

    /**
     * The WebDAV HTTP 507 (Insufficient Storage) status code means
     * that the server is unable to store the representation needed
     * to successfully complete the request. This condition is
     * considered to be temporary.
     */
    public static final int SC_INSUFFICIENT_STORAGE = 507;

    /**
     * The WebDAV display name property constant.
     */
    public static final String PROP_DISPLAY_NAME = "displayname";

    /**
     * The WebDAV creation date & time property constant.
     */
    public static final String PROP_CREATION_DATE = "creationdate";

    /**
     * The WebDAV last modified date & time property constant.
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
     * The WebDAV quota property constant (non-standard).
     */
    public static final String PROP_QUOTA = "quota";

    /**
     * The WebDAV quota used property constant (non standard).
     */
    public static final String PROP_QUOTA_USED = "quotaused";

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
    private static LinkedHashMap PROPS_COLLECTION = new LinkedHashMap();

    /**
     * The default properties for a file.
     */
    private static LinkedHashMap PROPS_FILE = new LinkedHashMap();

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
        PROPS_COLLECTION.put(PROP_QUOTA, "");
        PROPS_COLLECTION.put(PROP_QUOTA_USED, "");
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
     * Returns the HTTP status name corresponding to a status code.
     *
     * @param status         the HTTP status code
     *
     * @return the corresponding HTTP status name
     */
    public static String httpStatus(int status) {
        switch (status) {
        case HttpServletResponse.SC_OK:
            return "OK";
        case HttpServletResponse.SC_CREATED:
            return "Created";
        case HttpServletResponse.SC_ACCEPTED:
            return "Accepted";
        case HttpServletResponse.SC_NO_CONTENT:
            return "No Content";
        case HttpServletResponse.SC_MOVED_PERMANENTLY:
            return "Moved Permanently";
        case HttpServletResponse.SC_MOVED_TEMPORARILY:
            return "Moved Temporarily";
        case HttpServletResponse.SC_NOT_MODIFIED:
            return "Not Modified";
        case HttpServletResponse.SC_BAD_REQUEST:
            return "Bad Request";
        case HttpServletResponse.SC_UNAUTHORIZED:
            return "Unauthorized";
        case HttpServletResponse.SC_FORBIDDEN:
            return "Forbidden";
        case HttpServletResponse.SC_NOT_FOUND:
            return "Not Found";
        case HttpServletResponse.SC_INTERNAL_SERVER_ERROR:
            return "Internal Server Error";
        case HttpServletResponse.SC_NOT_IMPLEMENTED:
            return "Not Implemented";
        case HttpServletResponse.SC_BAD_GATEWAY:
            return "Bad Gateway";
        case HttpServletResponse.SC_SERVICE_UNAVAILABLE:
            return "Service Unavailable";
        case HttpServletResponse.SC_CONTINUE:
            return "Continue";
        case HttpServletResponse.SC_METHOD_NOT_ALLOWED:
            return "Method Not Allowed";
        case HttpServletResponse.SC_CONFLICT:
            return "Conflict";
        case HttpServletResponse.SC_PRECONDITION_FAILED:
            return "Precondition Failed";
        case HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE:
            return "Request Entity Too Large";
        case HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE:
            return "Unsupported Media Type";
        case SC_MULTI_STATUS:
            return "Multi-Status";
        case SC_UNPROCESSABLE_ENTITY:
            return "Unprocessable Entity";
        case SC_LOCKED:
            return "Locked";
        case SC_FAILED_DEPENDENCY:
            return "Failed Dependency";
        case SC_INSUFFICIENT_STORAGE:
            return "Insufficient Storage";
        default:
            return "Unknown";
        }
    }

    /**
     * The HTTP request being wrapped.
     */
    private Request request;

    /**
     * The query values flag. If set to true, the property values
     * should also be returned. Otherwise only the property names.
     */
    private boolean queryValues;

    /**
     * The properties found in the query. This will contain all
     * properties if the query doesn't specify any properties.
     */
    private LinkedHashMap queryProperties = new LinkedHashMap();

    /**
     * The namespace URI to abbreviation map.
     */
    private LinkedHashMap namespaces = null;

    /**
     * The array with result XML fragments. Each resource added will
     * be converted into XML snipplet added here.
     */
    private ArrayList results = new ArrayList();

    /**
     * Creates a new WebDAV request.
     *
     * @param request        the HTTP request to read
     *
     * @throws Exception if the WebDAV request XML couldn't be parsed
     */
    public WebDavRequest(Request request) throws Exception {
        String   xml = request.inputDataString();
        boolean  isCollection = request.getPath().endsWith("/");

        LOG.fine(request.getMethod() + " XML:\n" + xml);
        this.request = request;
        this.queryValues = true;
        if (xml != null && xml.trim().length() > 0) {
            Element root = parseDOM(xml);
            if (parseChild(root, "propname") != null) {
                queryValues = false;
            }
            parseProperties(parseChild(root, "prop"), isCollection);
        } else {
            parseProperties(null, isCollection);
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
     * Parses the requested properties. All properties found will be
     * added to the query properties map. If no properties node was
     * provided, all standard properties will be added.
     *
     * @param node           the properties node, or null
     * @param isCollection   the collection flag
     */
    private void parseProperties(Element node, boolean isCollection) {
        LinkedHashMap  defaults;
        String         name;

        defaults = isCollection ? PROPS_COLLECTION : PROPS_FILE;
        if (node == null) {
            queryProperties.putAll(defaults);
        } else {
            Node child = node.getFirstChild();
            while (child != null) {
                if (child.getNodeType() != Node.ELEMENT_NODE) {
                    // Ignore non-elements
                } else if ("DAV:".equals(child.getNamespaceURI())) {
                    if (defaults.containsKey(child.getLocalName())) {
                        queryProperties.put(child.getLocalName(), "");
                    } else {
                        queryProperties.put(child.getLocalName(), null);
                    }
                } else {
                    name = child.getNamespaceURI() + ":" + child.getLocalName();
                    queryProperties.put(name, null);
                }
                child = child.getNextSibling();
            }
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
            return Integer.parseInt(request.getHeader("Depth"));
        } catch (NumberFormatException e) {
            return -1;
        }
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
        LinkedHashMap  props = new LinkedHashMap();
        String         name;
        String         str;

        props.putAll(queryProperties);
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
                props.put(PROP_CONTENT_LENGTH, null);
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
        if (props.containsKey(PROP_QUOTA)) {
            props.put(PROP_QUOTA, "1000000000");
        }
        if (props.containsKey(PROP_QUOTA_USED)) {
            props.put(PROP_QUOTA_USED, "0");
        }
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
    private void addResource(String href, LinkedHashMap props) {
        StringBuilder  buffer = new StringBuilder();
        Iterator       iter = props.keySet().iterator();
        ArrayList      fails = new ArrayList();
        String         key;
        String         value;

        xmlResponseBegin(buffer, href);
        xmlTagBegin(buffer, 2, "propstat");
        xmlTagBegin(buffer, 3, "prop");
        while (iter.hasNext()) {
            key = (String) iter.next();
            value = (String) props.get(key);
            if (value == null) {
                fails.add(key);
            } else if (queryValues) {
                xmlTag(buffer, 4, key, value, !value.startsWith("<"));
            } else {
                xmlTag(buffer, 4, key);
            }
        }
        xmlTagEnd(buffer, 3, "prop");
        xmlStatus(buffer, 3, HttpServletResponse.SC_OK);
        xmlTagEnd(buffer, 2, "propstat");
        if (fails.size() > 0) {
            xmlTagBegin(buffer, 2, "propstat");
            xmlTagBegin(buffer, 3, "prop");
            for (int i = 0; i < fails.size(); i++) {
                key = (String) fails.get(i);
                if (key.indexOf(':') > 0) {
                    value = StringUtils.substringBeforeLast(key, ":");
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
            xmlStatus(buffer, 3, HttpServletResponse.SC_NOT_FOUND);
            xmlTagEnd(buffer, 2, "propstat");
        }
        xmlResponseEnd(buffer);
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
        if (namespaces == null) {
            namespaces = new LinkedHashMap();
        }
        if (!namespaces.containsKey(href)) {
            char value = (char) ('E' + namespaces.size());
            namespaces.put(href, String.valueOf(value));
            LOG.fine("reserved namespace '" + namespaces.get(href) + "' for " + href);
        }
        return (String) namespaces.get(href);
    }

    /**
     * Sends the added resources as the request response.
     */
    public void sendResponse() {
        StringBuilder  buffer = new StringBuilder();

        xmlMultiStatusBegin(buffer);
        for (int i = 0; i < results.size(); i++) {
            buffer.append(results.get(i));
        }
        xmlMultiStatusEnd(buffer);
        request.sendData(SC_MULTI_STATUS, Mime.XML[0], buffer.toString());
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
        request.sendError(HttpServletResponse.SC_FORBIDDEN, Mime.XML[0], buffer.toString());
    }

    /**
     * Writes an opening WebDAV multistatus tag to the specified buffer.
     *
     * @param buffer         the buffer to write to
     */
    private void xmlMultiStatusBegin(StringBuilder buffer) {
        buffer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        buffer.append("<D:multistatus xmlns:D=\"DAV:\">\n");
    }

    /**
     * Writes a closing WebDAV multistatus tag to the specified buffer.
     *
     * @param buffer         the buffer to write to
     */
    private void xmlMultiStatusEnd(StringBuilder buffer) {
        xmlTagEnd(buffer, 0, "multistatus");
    }

    /**
     * Writes an opening WebDAV response tag to the specified buffer.
     *
     * @param buffer         the buffer to write to
     * @param href           the root-relative resource link
     */
    private void xmlResponseBegin(StringBuilder buffer, String href) {
        String[]  parts = href.split("/");

        xmlTagBegin(buffer, 1, "response");
        buffer.append(StringUtils.repeat("  ", 2));
        buffer.append("<D:href>");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                buffer.append("/");
            }
            try {
                buffer.append(URLEncoder.encode(parts[i], "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                LOG.log(Level.SEVERE, "unsupported character encoding", e);
                buffer.append(StringEscapeUtils.escapeXml(parts[i]));
            }
        }
        buffer.append("</D:href>\n");
    }

    /**
     * Writes a closing WebDAV response tag to the specified buffer.
     *
     * @param buffer         the buffer to write to
     */
    private void xmlResponseEnd(StringBuilder buffer) {
        xmlTagEnd(buffer, 1, "response");
    }
    
    /**
     * Writes a WebDAV status tag to the specified buffer.
     *
     * @param buffer         the buffer to write to
     * @param indent         the indentation level
     * @param status         the status code
     */
    private void xmlStatus(StringBuilder buffer, int indent, int status) {
        String content = "HTTP/1.1 " + status + " " + httpStatus(status);
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
            buffer.append(StringEscapeUtils.escapeXml(content));
        } else {
            buffer.append(content);
        }
        buffer.append("</D:");
        buffer.append(tag);
        buffer.append(">\n");
    }
}
