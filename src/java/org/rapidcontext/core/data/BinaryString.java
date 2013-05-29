/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2013 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.rapidcontext.core.web.Mime;

/**
 * A binary data string, encapsulating a string as a binary object.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class BinaryString implements Binary {

    /**
     * The encapsulated data.
     */
    private String data;

    /**
     * Creates a new binary file wrapper.
     *
     * @param file           the file to encapsulate
     */
    public BinaryString(String data) {
        this.data = data;
    }

    /**
     * Returns the size (in bytes) of the binary object, if known.
     *
     * @return the object size (in bytes), or
     *         -1 if unknown
     */
    public long size() {
        return data.length();
    }

    /**
     * The last modified timestamp for the object, if known.
     *
     * @return the last modified timestamp, or
     *         zero (0) or the current system if unknown
     */
    public long lastModified() {
        return System.currentTimeMillis();
    }

    /**
     * The MIME type of the binary data. Use a standard opaque
     * binary data MIME type or one based on requested file name
     * if unknown.
     *
     * @return the MIME type of the binary data
     */
    public String mimeType() {
        return Mime.TEXT[0];
    }

    /**
     * Opens a new input stream for reading the data. Note that this
     * method SHOULD be possible to call several times.
     *
     * @return a new input stream for reading the binary data
     *
     * @throws IOException if the data couldn't be opened for reading
     */
    public InputStream openStream() throws IOException {
        return new ByteArrayInputStream(data.getBytes("UTF-8"));
    }
}
