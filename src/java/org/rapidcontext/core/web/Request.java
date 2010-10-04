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

package org.rapidcontext.core.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;

/**
 * A request wrapper class. This class encapsulates the HTTP servlet
 * request and response objects for simplified handling. It also
 * provides limited support for file uploads.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Request {

    /**
     * The no response type. This type is used when no request
     * response has been issued.
     */
    private static final int NO_RESPONSE = 0;

    /**
     * The authentication failure response type. This type is used
     * when an authentication failure has been set as the request
     * response.
     */
    private static final int AUTH_RESPONSE = 1;

    /**
     * The data response type. This type is used when a data string
     * has been set as the request response.
     */
    private static final int DATA_RESPONSE = 2;

    /**
     * The file response type. This type is used when a file has been
     * set as the request response. The response data contains the
     * absolute file name when this type is set.
     */
    private static final int FILE_RESPONSE = 3;

    /**
     * The redirect response type. This type is used when a request
     * redirect has been issued. The response data contains the
     * redirect URI (absolute or relative) when this type is set.
     */
    private static final int REDIRECT_RESPONSE = 4;

    /**
     * The error response type. This type is used when a response
     * error code should be sent. The response code, MIME type and
     * data string may be set when sending this response.
     */
    private static final int ERROR_RESPONSE = 5;

    /**
     * The system time when creating this request.
     */
    private long requestTime = System.currentTimeMillis();

    /**
     * The servlet request.
     */
    private HttpServletRequest request = null;

    /**
     * The servlet response.
     */
    private HttpServletResponse response = null;

    /**
     * The response type. This flag is set to true if the response
     * object has been modified.
     */
    private int responseType = NO_RESPONSE;

    /**
     * The response HTTP code. Only used when sending error responses.
     */
    private int responseCode = HttpServletResponse.SC_OK;

    /**
     * The response MIME type.
     */
    private String responseMimeType = null;

    /**
     * The response data.
     */
    private String responseData = null;

    /**
     * The multi-part request file iterator.
     */
    private FileItemIterator fileIter = null;

    /**
     * Creates a new request wrapper.
     *
     * @param request        the servlet request
     * @param response       the servlet response
     */
    public Request(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
        if (request.getCharacterEncoding() == null) {
            try {
                request.setCharacterEncoding("UTF-8");
            } catch (UnsupportedEncodingException ignore) {
                // Do nothing
            }
        }
    }

    /**
     * Returns a string representation of this request.
     *
     * @return a string representation of this request
     */
    public String toString() {
        return getUrl();
    }

    /**
     * Checks if this request contains a file upload. These requests
     * must be handled differently from ordinary requests, since the
     * normal request parameter access is limited to query
     * parameters. Files can only be accessed by iterating through
     * them with the getNextFile() method.
     *
     * @return true if this request contains a file upload, or
     *         false otherwise
     *
     * @see #getNextFile()
     */
    public boolean isFileUpload() {
        return ServletFileUpload.isMultipartContent(request);
    }

    /**
     * Checks if the request method is the specified one. Normally
     * the "GET" or "POST" methods are used, but other HTTP are
     * sometimes also used (with other expected semantics).
     *
     * @param method         the HTTP method name
     *
     * @return true if the method is the specified one, or
     *         false otherwise
     */
    public boolean hasMethod(String method) {
        return getMethod().equals(method);
    }

    /**
     * Checks if this request was sent in a session. If a new session
     * was created as a result of processing, this method will still
     * return false.
     *
     * @return true if the request had an associated session, or
     *         false otherwise
     */
    public boolean hasSession() {
        return request.getSession(false) != null
            && !request.getSession().isNew();
    }

    /**
     * Checks if this request contains a response.
     *
     * @return true if the request contains a response, or
     *         false otherwise
     */
    public boolean hasResponse() {
        return responseType != NO_RESPONSE;
    }

    /**
     * Returns the request method name. Normally this is "GET" or
     * "POST", but other HTTP methods can also be used.
     *
     * @return the request method name
     */
    public String getMethod() {
        return request.getMethod();
    }

    /**
     * Returns the request path with file name. This will NOT include
     * the servlet portion of the path.
     *
     * @return the request path with file name
     */
    public String getPath() {
        String  path = request.getPathInfo();

        return (path == null) ? "" : path;
    }

    /**
     * Returns the relative path to the servlet root. This method
     * will add an "../" part for each directory in the current path
     * so that site-relative links can be created easily.
     *
     * @return the relative path to the servlet root
     */
    public String getRootPath() {
        int count = StringUtils.countMatches(getPath(), "/");
        return StringUtils.repeat("../", count - 1);
    }

    /**
     * Returns the full request URL with protocol, hostname and path.
     * No query parameters will be included in the URL, however.
     *
     * @return the full request URL
     */
    public String getUrl() {
        return request.getRequestURL().toString();
    }

    /**
     * Returns the IP address of the request sender.
     *
     * @return the IP address of the request sender
     */
    public String getRemoteAddr() {
        return request.getRemoteAddr();
    }

    /**
     * Returns the authentication type of the request. This is the
     * first word in the "Authorization" request header.
     *
     * @return the authentication type, or
     *         null if not present
     */
    public String getAuthenticationType() {
        String  auth = request.getHeader("Authorization");

        if (auth == null || !auth.contains(" ")) {
            return null;
        } else {
            return auth.substring(0, auth.indexOf(" ")).trim();
        }
    }

    /**
     * Returns the authentication data of the request. This is the
     * base-64 encoded text after the first word in the
     * "Authorization" request header.
     *
     * @return the authentication data, or
     *         null if not present
     */
    public byte[] getAuthenticationData() {
        String  auth = request.getHeader("Authorization");

        if (auth == null || !auth.contains(" ")) {
            return null;
        } else {
            auth = auth.substring(auth.indexOf(" ") + 1);
            return Base64Coder.decode(auth);
        }
    }

    /**
     * Returns an HTTP request header.
     *
     * @param name           the request header name
     *
     * @return the header value, or
     *         null if not set
     */
    public String getHeader(String name) {
        return request.getHeader(name);
    }

    /**
     * Returns the value of a request parameter.
     *
     * @param name           the request parameter name
     *
     * @return the request parameter value, or
     *         null if no such parameter was found
     */
    public String getParameter(String name) {
        return getParameter(name, null);
    }

    /**
     * Returns the value of a request parameter. If the specified
     * parameter does not exits, a default value will be returned.
     *
     * @param name           the request parameter name
     * @param defVal         the default parameter value
     *
     * @return the request parameter value, or
     *         the default value if no such parameter was found
     */
    public String getParameter(String name, String defVal) {
        String  value = request.getParameter(name);

        return (value == null) ? defVal : value;
    }

    /**
     * Returns the next available file item stream in the request.
     * If the request is not a file upload request or no further
     * files are available, null will be returned. Note that file
     * upload requests must be handled differently from ordinary
     * requests, since the normal request parameter access is
     * limited to query parameters. Also, the file items returned
     * from this method are controlled to be actual files, and any
     * other request parameters are simply ignored.<p>
     *
     * The recommended way to implement file uploads is by sending
     * them as the sole request parameter in a form and storing the
     * file content for later processing. Preferably the form
     * posting is also performed from an IFRAME in order to process
     * in the background.
     *
     * @return the next file item stream, or
     *         null if none is available
     *
     * @throws IOException if the file upload
     */
    public FileItemStream getNextFile() throws IOException {
        FileItemStream  item;

        if (isFileUpload() && fileIter == null) {
            try {
                fileIter = new ServletFileUpload().getItemIterator(request);
            } catch (FileUploadException e) {
                throw new IOException("failed to parse file upload: " +
                                      e.getMessage());
            }
        }
        if (fileIter != null) {
            try {
                while (fileIter.hasNext()) {
                    item = fileIter.next();
                    if (!item.isFormField()) {
                        return item;
                    }
                }
            } catch (FileUploadException e) {
                throw new IOException("failed to parse file upload: " +
                                      e.getMessage());
            }
        }
        return null;
    }

    /**
     * Returns a named request attribute value.
     *
     * @param name           the attribute name
     *
     * @return the attribute value, or
     *         null if not set
     */
    public Object getAttribute(String name) {
        return request.getAttribute(name);
    }

    /**
     * Sets a request attribute value.
     *
     * @param name           the attribute name
     * @param value          the attribute value
     */
    public void setAttribute(String name, Object value) {
        request.setAttribute(name, value);
    }

    /**
     * Returns the HTTP session. If no session existed a new one
     * will be created. All sessions returned are automatically
     * managed with the SessionManager class.
     *
     * @return the new or existing request session
     *
     * @see SessionManager
     */
    public HttpSession getSession() {
        HttpSession  session = request.getSession();

        SessionManager.manage(session);
        SessionManager.connectThread(session);
        return session;
    }

    /**
     * Returns the time in milliseconds since this request wrapper
     * was created. The number returned will always be one (1) or
     * greater.
     *
     * @return the approximate request processing time
     */
    public long getProcessTime() {
        return Math.max(System.currentTimeMillis() - requestTime, 1);
    }

    /**
     * Clears any previously sent but non-committed response.
     */
    public void sendClear() {
        responseType = NO_RESPONSE;
        responseCode = HttpServletResponse.SC_OK;
        responseMimeType = null;
        responseData = null;
    }

    /**
     * Sends an authentication request as the request response. The
     * request may optionally contain a data payload, which will be
     * base-64 encoded. Any previous response will be cleared.
     *
     * @param type           the authentication type
     * @param data           the authentication data, or null
     *
     * @see #sendClear()
     */
    public void sendAuthenticationRequest(String type, byte[] data) {
        sendClear();
        responseType = AUTH_RESPONSE;
        responseData = type;
        if (data != null) {
            responseData += " " + new String(Base64Coder.encode(data)).trim();
        }
    }

    /**
     * Sends the specified data as the request response. Any previous
     * response will be cleared.
     *
     * @param mimeType       the data MIME type
     * @param data           the data to send
     *
     * @see #sendClear()
     */
    public void sendData(String mimeType, String data) {
        sendClear();
        responseType = DATA_RESPONSE;
        responseMimeType = mimeType;
        responseData = data;
    }

    /**
     * Sends the contents of a file as the request response. The file
     * name extension will be used for determining the MIME type for
     * the file contents. The cache header for the file may be
     * limited to private by setting the limit cache header. Any
     * previous response will be cleared.
     *
     * @param file           the file containing the response
     * @param limitCache     the limited cache flag
     *
     * @see #sendClear()
     */
    public void sendFile(File file, boolean limitCache) {
        sendClear();
        responseType = FILE_RESPONSE;
        responseMimeType = null;
        responseData = file.toString();
        if (limitCache) {
            response.setHeader("Cache-Control", "private");
        }
    }

    /**
     * Redirects this request by sending a temporary redirection URL
     * to the browser. The location specified may be either an
     * absolute or a relative URL. This method will set a request
     * response. Any previous response will be cleared.
     *
     * @param location       the destination location
     *
     * @see #sendClear()
     */
    public void sendRedirect(String location) {
        sendClear();
        responseType = REDIRECT_RESPONSE;
        responseData = location;
    }

    /**
     * Sends the specified error code and data as the request
     * response. Any previous response will be cleared.
     *
     * @param code           the HTTP response code to send
     * @param mimeType       the data MIME type
     * @param data           the data to send
     *
     * @see #sendClear()
     */
    public void sendError(int code, String mimeType, String data) {
        sendClear();
        responseType = ERROR_RESPONSE;
        responseCode = code;
        responseMimeType = mimeType;
        responseData = data;
    }

    /**
     * Sets an HTTP response header value. This method should only be
     * called once a response has been set but before the call to
     * commit(). Normally, this method should be avoided, as the
     * headers set aren't removed unless explicitly overwritten.
     *
     * @param name           the HTTP header name
     * @param value          the HTTP header value
     */
    public void setResponseHeader(String name, String value) {
        response.setHeader(name, value);
    }

    /**
     * Disposes of all resources used by this request object. This
     * method shouldn't be called until a response has been sent to
     * the client.
     */
    public void dispose() {
        SessionManager.disconnectThread(request.getSession(false));
        request = null;
        response = null;
        responseData = null;
    }

    /**
     * Sends the request response to the underlying HTTP response
     * object. This method shouldn't be called more than once per
     * request, and should not be called in case no response has been
     * stored in the request.
     *
     * @param context        the servlet context
     *
     * @throws IOException if an IO error occurred while attempting to
     *             commit the response
     * @throws ServletException if a configuration error was
     *             encountered while sending the response
     */
    public void commit(ServletContext context)
        throws IOException, ServletException {

        response.setHeader("Server", "RapidContext");
        response.setDateHeader("Date", System.currentTimeMillis());
        switch (responseType) {
        case AUTH_RESPONSE:
            commitAuthentication();
            break;
        case DATA_RESPONSE:
            commitData();
            break;
        case FILE_RESPONSE:
            commitFile(context);
            break;
        case REDIRECT_RESPONSE:
            commitRedirect();
            break;
        case ERROR_RESPONSE:
            commitError();
            break;
        default:
            throw new ServletException("No request response available: " +
                                       this);
        }
    }

    /**
     * Sets the dynamic HTTP response headers. The current system time
     * will be used as the last modification time.
     */
    private void commitDynamicHeaders() {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Expires", "-1");
        response.setDateHeader("Last-Modified", System.currentTimeMillis());
    }

    /**
     * Sets the static HTTP response headers. The specified system
     * time will be used as the last modification time.
     *
     * @param lastModified   the last modification time, or
     *                       zero (0) for the current system time
     */
    private void commitStaticHeaders(long lastModified) {
        if (!response.containsHeader("Cache-Control")) {
            response.setHeader("Cache-Control", "public");
        }
        if (lastModified > 0) {
            response.setDateHeader("Last-Modified", lastModified);
        } else {
            response.setDateHeader("Last-Modified",
                                   System.currentTimeMillis());
        }
    }

    /**
     * Sends the NTLM authentication request response to the
     * underlying HTTP response object.
     *
     * @throws IOException if an IO error occurred while attempting to
     *             commit the response
     */
    private void commitAuthentication() throws IOException {
        response.setHeader("WWW-Authenticate", responseData);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Sends the data response to the underlying HTTP response object.
     *
     * @throws IOException if an IO error occurred while attempting to
     *             commit the response
     */
    private void commitData() throws IOException {
        PrintWriter  out;

        commitDynamicHeaders();
        if (responseMimeType == null || responseMimeType.length() == 0) {
            response.setContentType("text/plain; charset=UTF-8");
        } else if (responseMimeType.indexOf("charset") > 0) {
            response.setContentType(responseMimeType);
        } else {
            response.setContentType(responseMimeType + "; charset=UTF-8");
        }
        out = response.getWriter();
        out.write(responseData);
        out.close();
    }

    /**
     * Sends the file response to the underlying HTTP response object.
     *
     * @param context        the servlet context
     *
     * @throws IOException if an IO error occurred while attempting to
     *             commit the response
     */
    private void commitFile(ServletContext context)
        throws IOException {

        File             file;
        long             modified;
        String           mimeType;
        FileInputStream  input;
        OutputStream     output;
        byte[]           buffer = new byte[4096];
        int              length;

        file = new File(responseData);
        modified = request.getDateHeader("If-Modified-Since");
        if (modified != -1 && file.lastModified() < modified + 1000) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
        commitStaticHeaders(file.lastModified());
        mimeType = context.getMimeType(responseData);
        if (mimeType == null || mimeType.length() == 0) {
            response.setContentType("application/octet-stream");
        } else {
            response.setContentType(mimeType);
        }
        response.setContentLength((int) file.length());
        try {
            input = new FileInputStream(file);
        } catch (IOException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        output = response.getOutputStream();
        while ((length = input.read(buffer)) > 0) {
            output.write(buffer, 0, length);
        }
        input.close();
        output.close();
    }

    /**
     * Sends the redirect response to the underlying HTTP response
     * object.
     *
     * @throws IOException if an IO error occurred while attempting to
     *             redirect the request
     */
    private void commitRedirect() throws IOException {
        commitDynamicHeaders();
        response.sendRedirect(responseData);
    }

    /**
     * Sends the error response to the underlying HTTP response object.
     *
     * @throws IOException if an IO error occurred while attempting to
     *             commit the response
     */
    private void commitError() throws IOException {
        PrintWriter  out;

        response.setStatus(responseCode);
        commitDynamicHeaders();
        if (responseMimeType == null || responseMimeType.length() == 0) {
            response.setContentType("text/plain; charset=UTF-8");
        } else if (responseMimeType.indexOf("charset") > 0) {
            response.setContentType(responseMimeType);
        } else {
            response.setContentType(responseMimeType + "; charset=UTF-8");
        }
        out = response.getWriter();
        out.write(responseData);
        out.close();
    }
}
