/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2025 Per Cederberg. All rights reserved.
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

import java.io.File;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.ArrayUtils;

/**
 * A MIME type helper class.
 *
 * @author Per Cederberg
 */
public final class Mime {

    /**
     * The MIME types commonly used for plain text.
     */
    public static final String[] TEXT = {
        "text/plain"
    };

    /**
     * The MIME types commonly used for HTML.
     */
    public static final String[] HTML = {
        "text/html",
        "text/xhtml",
        "application/xhtml",
        "application/xhtml+xml"
    };

    /**
     * The MIME types commonly used for CSS.
     */
    public static final String[] CSS = {
        "text/css"
    };

    /**
     * The MIME types commonly used for JavaScript.
     */
    public static final String[] JS = {
        "text/javascript",
        "text/x-javascript",
        "application/x-javascript"
    };

    /**
     * The MIME types commonly used for JSON.
     */
    public static final String[] JSON = {
        "application/json",
        "application/x-javascript",
        "text/json",
        "text/x-json",
        "text/javascript",
        "text/x-javascript"
    };

    /**
     * The MIME types commonly used for Java Properties.
     */
    public static final String[] PROPERTIES = {
        "text/x-java-properties",
        "text/x-properties",
        "text/properties"
    };

    /**
     * The MIME types commonly used for XML.
     */
    public static final String[] XML = {
        "text/xml",
        "application/xml"
    };

    /**
     * The MIME types commonly used for YAML.
     */
    public static final String[] YAML = {
        "text/yaml",
        "text/x-yaml",
        "text/vnd.yaml",
        "application/yaml",
        "application/x-yaml"
    };

    /**
     * The MIME types commonly used for Markdown.
     */
    public static final String[] MARKDOWN = {
        "text/markdown",
        "text/x-markdown"
    };

    /**
     * The MIME types commonly used for GIF images.
     */
    public static final String[] GIF = {
        "image/gif"
    };

    /**
     * The MIME types commonly used for JPEG images.
     */
    public static final String[] JPEG = {
        "image/jpeg"
    };

    /**
     * The MIME types commonly used for PNG images.
     */
    public static final String[] PNG = {
        "image/png"
    };

    /**
     * The MIME types commonly used for SVG images.
     */
    public static final String[] SVG = {
        "image/svg+xml"
    };

    /**
     * The MIME types commonly used for ICO images.
     */
    public static final String[] ICO = {
        "image/vnd.microsoft.icon",
        "image/x-icon"
    };

    /**
     * The MIME types commonly used for binary data.
     */
    public static final String[] BIN = {
        "application/octet-stream"
    };

    /**
     * The MIME types commonly used for web form data.
     */
    public static final String[] WWW_FORM = {
        "application/x-www-form-urlencoded"
    };

    /**
     * The helper servlet context. This can be set by the application
     * to access the servlet container MIME type configuration.
     */
    public static ServletContext context = null;

    /**
     * Attempts to guess the MIME type for a file, based on the file
     * name (extension). This method will always return a valid MIME
     * type, defaulting to the binary MIME type if unknown.
     *
     * @param file           the file to check
     *
     * @return the file MIME type, or
     *         a binary MIME type if unknown
     */
    public static String type(File file) {
        return type(file.getName());
    }

    /**
     * Attempts to guess the MIME type for a file name (extension).
     * This method will always return a valid MIME type, defaulting
     * to the binary MIME type if unknown.
     *
     * @param fileName           the file name to check
     *
     * @return the file MIME type, or
     *         a binary MIME type if unknown
     */
    public static String type(String fileName) {
        fileName = fileName.toLowerCase();
        if (fileName.endsWith(".txt")) {
            return TEXT[0];
        } else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return HTML[0];
        } else if (fileName.endsWith(".css")) {
            return CSS[0];
        } else if (fileName.endsWith(".js") || fileName.endsWith(".cjs") || fileName.endsWith(".mjs")) {
            return JS[0];
        } else if (fileName.endsWith(".json")) {
            return JSON[0];
        } else if (fileName.endsWith(".properties")) {
            return PROPERTIES[0];
        } else if (fileName.endsWith(".xml")) {
            return XML[0];
        } else if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            return YAML[0];
        } else if (fileName.endsWith(".md")) {
            return MARKDOWN[0];
        } else if (fileName.endsWith(".gif")) {
            return GIF[0];
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return JPEG[0];
        } else if (fileName.endsWith(".png")) {
            return PNG[0];
        } else if (fileName.endsWith(".svg")) {
            return SVG[0];
        } else if (fileName.endsWith(".ico")) {
            return ICO[0];
        } else if (context != null) {
            String mime = context.getMimeType(fileName);
            if (mime != null && !mime.isBlank()) {
                return mime;
            }
        }
        return BIN[0];
    }

    /**
     * Checks if a specified content type is a known MIME type for text
     * content.
     *
     * @param contentType    the content type to analyze
     *
     * @return true if the MIME type is for text content, or
     *         false otherwise
     */
    public static boolean isText(String contentType) {
        return contentType.startsWith("text/") ||
               isMatch(contentType, HTML) ||
               isMatch(contentType, JS) ||
               isMatch(contentType, JSON) ||
               isMatch(contentType, PROPERTIES) ||
               isMatch(contentType, XML) ||
               isMatch(contentType, YAML) ||
               isMatch(contentType, SVG);
    }

    /**
     * Checks if a specified content type matches one of the specified
     * MIME types. Note that this method checks for any match, without
     * regard for any embedded quality or encoding value.
     *
     * @param contentType    the content type to analyze
     * @param mimes          the MIME types to check for
     *
     * @return true if one of the MIME types is accepted, or
     *         false otherwise
     */
    public static boolean isMatch(String contentType, String[] mimes) {
        String mime = contentType.split(";")[0];
        return ArrayUtils.contains(mimes, mime);
    }

    /**
     * Checks if the request input matches one of the specified MIME
     * types. The input MIME type is read from the 'Content-Type'
     * HTTP header, not from the data stream itself.
     *
     * @param request        the request to analyze
     * @param mimes          the MIME types to check for
     *
     * @return true if one of the MIME types match, or
     *         false otherwise
     */
    public static boolean isInputMatch(Request request, String[] mimes) {
        if (request.getContentType() == null) {
            return false;
        } else {
            return isMatch(request.getContentType(), mimes);
        }
    }

    /**
     * Checks if the accepted request output matches one of the
     * specified MIME types. The accepted output MIME type is read
     * from the 'Accept' HTTP header. Note that this method accept
     * matches without regard for quality values in the header.
     *
     * @param request        the request to analyze
     * @param mimes          the MIME types to check for
     *
     * @return true if one of the MIME types is accepted, or
     *         false otherwise
     */
    public static boolean isOutputMatch(Request request, String[] mimes) {
        String header = request.getHeader("Accept");
        if (header != null) {
            for (String s : header.split(",")) {
                if (isMatch(s, mimes)) {
                    return true;
                }
            }
        }
        return false;
    }

    // No instances
    private Mime() {}
}
