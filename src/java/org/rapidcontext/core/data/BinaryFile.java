/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2011 Per Cederberg. All rights reserved.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.rapidcontext.core.web.Mime;

/**
 * A binary data file, encapsulating a file as a binary object.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class BinaryFile implements Binary {

    /**
     * The encapsulated file.
     */
    private File file;

    /**
     * Creates a new binary file wrapper.
     *
     * @param file           the file to encapsulate
     */
    public BinaryFile(File file) {
        this.file = file;
    }

    /**
     * Returns the size (in bytes) of the binary object, if known.
     *
     * @return the object size (in bytes), or
     *         -1 if unknown
     */
    public long size() {
        return file.length();
    }

    /**
     * The last modified timestamp for the object, if known.
     *
     * @return the last modified timestamp, or
     *         zero (0) or the current system if unknown
     */
    public long lastModified() {
        return file.lastModified();
    }

    /**
     * The MIME type of the binary data. Use a standard opaque
     * binary data MIME type or one based on requested file name
     * if unknown.
     *
     * @return the MIME type of the binary data
     */
    public String mimeType() {
        return Mime.type(file);
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
        return new FileInputStream(file);
    }
}
