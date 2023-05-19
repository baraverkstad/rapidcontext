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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import org.rapidcontext.core.web.Mime;
import org.rapidcontext.util.BinaryUtil;

/**
 * A binary data object, similar to a file on a file system. This
 * interface is used to abstract away the file system when data is
 * retrieved via the Storage API.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public interface Binary {

    /**
     * Returns the size (in bytes) of the binary object, if known.
     *
     * @return the object size (in bytes), or
     *         -1 if unknown
     */
    public long size();

    /**
     * The last modified timestamp for the object, if known.
     *
     * @return the last modified timestamp, or
     *         zero (0) or the current system if unknown
     */
    public long lastModified();

    /**
     * The MIME type of the binary data. Use a standard opaque
     * binary data MIME type or one based on requested file name
     * if unknown.
     *
     * @return the MIME type of the binary data
     */
    public String mimeType();

    /**
     * The SHA-256 of the binary data, if known.
     *
     * @return the hexadecimal string with the SHA-256 hash, or
     *         null if not available
     */
    public String sha256();

    /**
     * Opens a new input stream for reading the data. Note that this
     * method SHOULD be possible to call several times.
     *
     * @return a new input stream for reading the binary data
     *
     * @throws IOException if the data couldn't be opened for reading
     */
    public InputStream openStream() throws IOException;


    /**
     * A binary data object, encapsulating a file.
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
        public BinaryFile(java.io.File file) {
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
         * The SHA-256 of the binary data, if known.
         *
         * @return the hexadecimal string with the SHA-256 hash, or
         *         null if not available
         */
        public String sha256() {
            try (FileInputStream input = new FileInputStream(file)) {
                return BinaryUtil.hashSHA256(input);
            } catch (NoSuchAlgorithmException | IOException e) {
                return null;
            }
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

    /**
     * A binary data object, encapsulating a string.
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
         * Creates a new binary string wrapper.
         *
         * @param data           the string to encapsulate
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
         * The SHA-256 of the binary data, if known.
         *
         * @return the hexadecimal string with the SHA-256 hash, or
         *         null if not available
         */
        public String sha256() {
            try {
                return BinaryUtil.hashSHA256(data);
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                return null;
            }
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


    /**
     * A binary data object, encapsulating an input stream. This
     * wrapper breaks the Binary contract slightly, since new streams
     * cannot be opened.
     *
     * @author   Per Cederberg
     * @version  1.0
     */
    public class BinaryStream implements Binary {

        /**
         * The encapsulated input stream.
         */
        private InputStream is;

        /**
         * The size (in bytes) of the binary object.
         */
        private long size;

        /**
         * Creates a new binary stream wrapper.
         *
         * @param is             the input stream to encapsulate
         * @param size           the size (in bytes), or -1 if unknown
         */
        public BinaryStream(InputStream is, int size) {
            this.is = is;
            this.size = size;
        }

        /**
         * Returns the size (in bytes) of the binary object, if known.
         *
         * @return the object size (in bytes), or
         *         -1 if unknown
         */
        public long size() {
            return size;
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
            return Mime.BIN[0];
        }

        /**
         * The SHA-256 of the binary data, if known.
         *
         * @return the hexadecimal string with the SHA-256 hash, or
         *         null if not available
         */
        public String sha256() {
            return null;
        }

        /**
         * Opens a new input stream for reading the data.
         *
         * @return a new input stream for reading the binary data
         *
         * @throws IOException if the data couldn't be opened for reading
         */
        public InputStream openStream() throws IOException {
            return is;
        }
    }
}
