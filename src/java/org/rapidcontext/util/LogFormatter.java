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

package org.rapidcontext.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * A java.util.logging formatter with more output options. Adds to the default
 * simple formatter by indenting multiple line entries, shortening stack
 * traces, and removing the "org.rapidcontext" prefix for class names.
 *
 * <p>Configured by the following properties:</p>
 * <ul>
 * <li><code>org.rapidcontext.logging.format</code> -- same format
 *     parameters and options as java.util.logging.SimpleFormatter.format
 * <li><code>org.rapidcontext.logging.format.indent</code> -- indentation
 *     prefix for subsequent log lines (default is 4 spaces)
 * </ul>
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class LogFormatter extends Formatter {

    /**
     * Default logging format.
     */
    protected static final String DEFAULT_FORMAT =
        "%1$tF %1$tT %4$s: %2$s %5$s%6$s%n";

    /**
     * The log output format. Configured via the logging property
     * "org.rapidcontext.logging.format". Uses same available formats and
     * parameters as the java.util.logging.SimpleFormatter.format
     */
    protected static final String FORMAT;

    /**
     * The indentation prefix for subsequent log lines. Configured via the
     * logging property "org.rapidcontext.logging.format.indent". Default is
     * 4 spaces.
     */
    protected static final String INDENT;

    // Static initializer for log properties
    static {
        LogManager manager = LogManager.getLogManager();
        String val = manager.getProperty("org.rapidcontext.logging.format");
        FORMAT = (val == null) ? DEFAULT_FORMAT : val;
        val = manager.getProperty("org.rapidcontext.logging.format.indent");
        INDENT = (val == null) ? "    " : val;
    }

    /**
     * Formats the specified log record for output.
     *
     * @param record         the log record to format
     *
     * @return the formatted log record
     */
    public String format(LogRecord record) {
        Date dttm = new Date(record.getMillis());
        String src = getSource(record);
        String log = record.getLoggerName();
        String level = record.getLevel().getLocalizedName();
        String msg = formatMessage(record);
        String stack = getStackTrace(record);
        return indent(String.format(FORMAT, dttm, src, log, level, msg, stack));
    }

    /**
     * Returns the source class and method for a log record.
     *
     * @param record         the log record to use
     *
     * @return the source class and method, or
     *         the logger name if not set
     */
    protected String getSource(LogRecord record) {
        if (record.getSourceClassName() == null) {
            return record.getLoggerName();
        }
        String className = record.getSourceClassName();
        if (className.startsWith("org.rapidcontext.")) {
            className = className.substring(17);
        }
        if (record.getSourceMethodName() == null) {
            return className;
        } else {
            return className + "." + record.getSourceMethodName();
        }
    }

    /**
     * Returns the stack trace from a log record.
     *
     * @param record         the log record to use
     *
     * @return the formatted stack trace, or
     *         an empty string if not set
     */
    protected String getStackTrace(LogRecord record) {
        if (record.getThrown() == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            pw.println();
            record.getThrown().printStackTrace(pw);
        }
        String trace = sw.toString().replaceAll("\\(", " (");
        if (record.getSourceClassName() != null) {
            String prefix = "\tat " + record.getSourceClassName();
            String[] lines = trace.split("\n");
            int firstLine = lines.length;
            int lastLine = lines.length;
            for (int i = 2; i < lines.length; i++) {
                if (lines[i].startsWith(prefix)) {
                    firstLine = i + 1;
                } else if (!lines[i].startsWith("\tat ")) {
                    lastLine = i - 1;
                    break;
                }
            }
            StringBuilder filtered = new StringBuilder();
            filtered.append(lines[0]);
            for (int i = 1; i < lines.length; i++) {
                if (i < firstLine || i > lastLine) {
                    filtered.append("\n");
                    filtered.append(lines[i]);
                } else if (i == firstLine) {
                    filtered.append("\n");
                    filtered.append("\t... " + (lastLine - firstLine) + " more");
                }
            }
            trace = filtered.toString();
        }
        if (INDENT.length() <= 0 || INDENT.trim().length() > 0) {
            return trace;
        } else {
            return trace.replaceAll("\t", INDENT);
        }
    }

    /**
     * Returns an indented log output string.
     *
     * @param log            the log output string
     *
     * @return the indented version of the log output
     */
    protected String indent(String log) {
        String str = log.trim();
        if (INDENT.length() <= 0 || !str.contains("\n")) {
            return log;
        } else {
            str = str.replaceAll("\n", "\n" + INDENT);
            return log.endsWith("\n") ? str + "\n" : str;
        }
    }
}
