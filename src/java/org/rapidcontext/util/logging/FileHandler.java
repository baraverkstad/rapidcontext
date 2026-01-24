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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.rapidcontext.util.ValueUtil;

/**
 * A java.util.logging file handler with support for date patterns in
 * log file name. Log file rotation is based on the file name pattern
 * date format.
 *
 * <p>Configured by the following properties:</p>
 * <ul>
 * <li><code>[class-name].level</code> -- a minimum log level (Level.ALL)
 * <li><code>[class-name].filter</code> -- an optional log filter (none)
 * <li><code>[class-name].formatter</code> -- a log formatter
 *     (org.rapidcontext.util.logging.LogFormatter)
 * <li><code>[class-name].encoding</code> -- an optional encoding (platform
 *     default)
 * <li><code>[class-name].pattern</code> -- a log file name pattern,
 *     including the file path ("%t/rapidcontext-%d{yyyy-MM-dd}.log")
 * <li><code>[class-name].append</code> -- a log append flag (true)
 * </ul>
 *
 * <p>A log file pattern may contain the following special character
 * sequences:</p>
 * <ul>
 * <li><code>"%t"</code> -- the system temporary directory
 * <li><code>"%h"</code> -- the value of the "user.home" system property
 * <li><code>"%d"</code> -- the current date (in YYYY-MM-DD format)
 * <li><code>"%d{...}"</code> -- a generic date and time format (using
 *     SimpleDateFormat pattern)
 * <li><code>"%%"</code> -- a single percent sign "%"
 * </ul>
 *
 * @author Per Cederberg
 */
public final class FileHandler extends StreamHandler {

    // The UTC time zone
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    // The UTC date and time format
    private static final FastDateFormat DATEFMT =
        FastDateFormat.getInstance("yyyy-MM-dd", UTC);

    // The filename date pattern regexp
    private static final Pattern DATE_PATTERN_RE = Pattern.compile("%d\\{([^}]+)\\}");

    // The log file name pattern
    private String pattern;

    // The log file append flag
    private boolean append;

    // The log rotation interval
    private long interval = DateUtils.MILLIS_PER_DAY;

    // The current log rotation
    private long rotation = 0;

    // The currently open log file name
    private String openFile = null;

    /**
     * Creates a new log file handler using LogManager configuration
     * properties.
     *
     * @throws SecurityException if logging control permission is missing
     * @throws IOException if the configured log file couldn't be created
     */
    public FileHandler() throws SecurityException, IOException {
        this(null, true);
    }

    /**
     * Creates a new log file handler.
     *
     * @param pattern        the log file pattern
     * @param append         the append existing file flag
     *
     * @throws SecurityException if logging control permission is missing
     * @throws IOException if the configured log file couldn't be created
     */
    @SuppressWarnings("this-escape")
    public FileHandler(String pattern, boolean append)
    throws SecurityException, IOException {
        // Sets .level, .filter, .encoding and .formatter from LogManager props
        super();

        // Configure pattern and append
        LogManager manager = LogManager.getLogManager();
        String cname = getClass().getName();
        this.pattern = Optional.ofNullable(pattern)
            .filter(s -> !s.isBlank())
            .or(() -> Optional.ofNullable(manager.getProperty(cname + ".pattern")))
            .filter(s -> !s.isBlank())
            .orElse("%t/rapidcontext-%d{yyyy-MM-dd}.log");
        this.append = ValueUtil.bool(manager.getProperty(cname + ".append"), append);

        // Set custom defaults
        String val = manager.getProperty(cname + ".level");
        if (val == null || val.isBlank()) {
            setLevel(Level.ALL);
        }
        if (manager.getProperty(cname + ".formatter") == null) {
            setFormatter(new LogFormatter());
        }

        // Create log file
        analyzePattern();
        open(System.currentTimeMillis());
    }

    /**
     * Detects the proper log rotation interval based on file name
     * pattern.
     */
    private void analyzePattern() {
        Matcher m = DATE_PATTERN_RE.matcher(pattern);
        while (m.find()) {
            String s = m.group(1);
            if (s.contains("m") || s.contains("s") || s.contains("S")) {
                interval = Math.min(interval, DateUtils.MILLIS_PER_MINUTE);
            } else if (s.contains("H") || s.contains("k") || s.contains("K") || s.contains("h")) {
                interval = Math.min(interval, DateUtils.MILLIS_PER_HOUR);
            }
        }
    }

    /**
     * Generates a new log file name from the defined pattern. Uses
     * the current log rotation as the basis for the timestamp for
     * date formatting.
     *
     * @return the new log file name
     */
    private String generateFileName() {
        Date start = new Date(rotation * interval);
        String name = DATE_PATTERN_RE.matcher(pattern).replaceAll(
            m -> FastDateFormat.getInstance(m.group(1), UTC).format(start)
        );
        return name
            .replace("%t", System.getProperty("java.io.tmpdir"))
            .replace("%h", System.getProperty("user.home"))
            .replace("%d", DATEFMT.format(start))
            .replace("%%", "%");
    }

    /**
     * Opens the log file if not open or name has changed.
     *
     * @throws SecurityException if the log stream couldn't be modified
     * @throws IOException if the new log file couldn't be opened
     */
    @SuppressWarnings("resource")
    protected void open(long now) throws SecurityException, IOException {
        rotation = now / interval;
        String fileName = generateFileName();
        if (!Objects.equals(openFile, fileName)) {
            if (openFile != null) {
                close();
            }
            openFile = fileName;
            File parent = new File(openFile).getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            setOutputStream(new FileOutputStream(openFile, append));
        }
    }

    /**
     * Formats and publishes a log record.
     *
     * @param entry          the log entry to publish
     */
    @Override
    public synchronized void publish(LogRecord entry) {
        if (isLoggable(entry)) {
            long now = System.currentTimeMillis();
            if (now / interval > rotation) {
                try {
                    open(now);
                } catch (Exception e) {
                    reportError("Failed to rotate log file", e, ErrorManager.GENERIC_FAILURE);
                }
            }
            super.publish(entry);
            flush();
        }
    }
}
