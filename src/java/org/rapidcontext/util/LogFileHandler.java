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

package org.rapidcontext.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;

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
 *     (org.rapidcontext.util.LogFormatter)
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
 * @author   Per Cederberg
 * @version  1.0
 */
public class LogFileHandler extends StreamHandler {

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
     * @throws ReflectiveOperationException if configuration was incorrect
     * @throws IOException if the configured log file couldn't be created
     */
    public LogFileHandler()
    throws SecurityException, ReflectiveOperationException, IOException {
        initialize();
        analyzePattern();
        open();
    }

    /**
     * Creates a new log file handler.
     *
     * @param pattern        the log file pattern
     * @param append         the append existing file flag
     *
     * @throws SecurityException if logging control permission is missing
     * @throws ReflectiveOperationException if configuration was incorrect
     * @throws IOException if the configured log file couldn't be created
     */
    public LogFileHandler(String pattern, boolean append)
    throws SecurityException, ReflectiveOperationException, IOException {
        initialize();
        this.pattern = pattern;
        this.append = append;
        analyzePattern();
        open();
    }

    /**
     * Initializes the instance variables based on the log manager
     * properties.
     *
     * @throws SecurityException if logging control permission is missing
     * @throws ReflectiveOperationException if configuration was incorrect
     * @throws IOException if the configured log file couldn't be created
     */
    private void initialize()
    throws SecurityException, ReflectiveOperationException, IOException {
        LogManager manager = LogManager.getLogManager();
        String prefix = getClass().getName();
        String str = manager.getProperty(prefix + ".pattern");
        pattern = Objects.toString(str, "%t/rapidcontext-%d{yyyy-MM-dd}.log");
        append = !ValueUtil.isOff(manager.getProperty(prefix + ".append"));
        str = manager.getProperty(prefix + ".filter");
        if (!StringUtils.isBlank(str)) {
            Class<?> cls = getClass().getClassLoader().loadClass(str);
            setFilter((Filter) cls.getDeclaredConstructor().newInstance());
        }
        str = manager.getProperty(prefix + ".formatter");
        if (!StringUtils.isBlank(str)) {
            Class<?> cls = getClass().getClassLoader().loadClass(str);
            setFormatter((Formatter) cls.getDeclaredConstructor().newInstance());
        } else {
            setFormatter(new LogFormatter());
        }
        setEncoding(manager.getProperty(prefix + ".encoding"));
    }

    /**
     * Detects the proper log rotation interval based on file name
     * pattern.
     */
    private void analyzePattern() {
        Matcher m = DATE_PATTERN_RE.matcher(pattern);
        while (m.find()) {
            String str = m.group(1);
            if (StringUtils.containsAny(str, 'm', 's', 'S')) {
                interval = Math.min(interval, DateUtils.MILLIS_PER_MINUTE);
            } else if (StringUtils.containsAny(str, 'H', 'k', 'K', 'h')) {
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
    private void open() throws SecurityException, IOException {
        rotation = System.currentTimeMillis() / interval;
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
            if (System.currentTimeMillis() / interval > rotation) {
                try {
                    open();
                } catch (Exception e) {
                    // Print exception on failed log rotation
                    e.printStackTrace();
                }
            }
            super.publish(entry);
            flush();
        }
    }
}
