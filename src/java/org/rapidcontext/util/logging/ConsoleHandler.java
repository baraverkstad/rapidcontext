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

package org.rapidcontext.util.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * A console log handler that separates output streams based on log level.
 * Error and warning level messages are sent to stderr, while info and lower
 * level messages are sent to stdout.
 *
 * <p>Configured by the following properties:</p>
 * <ul>
 * <li><code>[class-name].level</code> -- a minimum log level (Level.ALL)
 * <li><code>[class-name].filter</code> -- an optional log filter (none)
 * <li><code>[class-name].formatter</code> -- a log formatter
 *     (org.rapidcontext.util.logging.LogFormatter)
 * <li><code>[class-name].encoding</code> -- an optional encoding (platform
 *     default)
 * <li><code>[class-name].stderr-level</code> -- the minimum level for stderr
 *     output (Level.WARNING)
 * </ul>
 *
 * @author Per Cederberg
 */
public class ConsoleHandler extends StreamHandler {

    /**
     * The minimum level for stderr output. Default is Level.WARNING.
     */
    protected Level errorLevel = Level.WARNING;

    /**
     * The error output writer.
     */
    private Writer err = null;

    /**
     * Creates a new log console handler using LogManager configuration
     * properties.
     */
    @SuppressWarnings("this-escape")
    public ConsoleHandler() {
        // Sets .level, .filter, .encoding and .formatter from LogManager props
        super();
        setOutputStream(System.out);

        // Set custom defaults + cutoff level for stderr
        LogManager manager = LogManager.getLogManager();
        String cname = getClass().getName();
        String val = manager.getProperty(cname + ".level");
        if (val == null || val.isBlank()) {
            setLevel(Level.ALL);
        }
        val = manager.getProperty(cname + ".formatter");
        if (val == null || val.isBlank()) {
            setFormatter(new LogFormatter());
        }
        val = manager.getProperty(cname + ".stderr-level");
        if (val != null && !val.isBlank()) {
            errorLevel = Level.parse(val);
        }
    }

    /**
     * Creates a new log console handler.
     *
     * @param errorLevel     the minimum level for stderr output
     */
    public ConsoleHandler(Level errorLevel) {
        this();
        this.errorLevel = errorLevel == null ? Level.WARNING : errorLevel;
    }

    /**
     * Closes this handler and releases any associated resources.
     *
     * @throws SecurityException if logging control permission is missing
     */
    @Override
    public synchronized void close() throws SecurityException {
        flush();
        err = null;
    }

    /**
     * Flushes any buffered output.
     */
    @Override
    public synchronized void flush() {
        super.flush();
        try {
            if (err != null) {
                err.flush();
            }
        } catch (IOException e) {
            reportError(null, e, ErrorManager.FLUSH_FAILURE);
        }
    }

    /**
     * Formats and publishes a log record to the appropriate output stream.
     *
     * @param record         the log entry to publish
     *
     * @throws SecurityException if the log stream couldn't be modified
     */
    @Override
    public synchronized void publish(LogRecord record) {
        if (isLoggable(record)) {
            if (record.getLevel().intValue() >= errorLevel.intValue()) {
                try {
                    if (err == null) {
                        err = createWriter(System.err);
                    }
                    err.write(getFormatter().format(record));
                    err.flush();
                } catch (Exception e) {
                    reportError(null, e, ErrorManager.WRITE_FAILURE);
                }
            } else {
                super.publish(record);
                super.flush();
            }
        }
    }

    /**
     * Creates a new writer for the specified output stream.
     *
     * @param os             the output stream
     *
     * @return the output stream writer created
     */
    private Writer createWriter(OutputStream os) {
        try {
            String enc = getEncoding();
            if (enc != null && !enc.isBlank()) {
                return new OutputStreamWriter(os, enc);
            }
        } catch (UnsupportedEncodingException e) {
            reportError(null, e, ErrorManager.GENERIC_FAILURE);
        }
        return new OutputStreamWriter(os);
    }

    /**
     * Sets the error output stream.
     *
     * @param err            the output stream
     */
    protected synchronized void setErrorStream(OutputStream err) {
        this.err = createWriter(err == null ? System.err : err);
    }
}
