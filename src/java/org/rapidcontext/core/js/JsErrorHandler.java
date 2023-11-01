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

package org.rapidcontext.core.js;

import java.util.ArrayList;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

/**
 * A JavaScript error handler. This class collects any JavaScript
 * errors and warnings and makes them available in a handy format.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
class JsErrorHandler implements ErrorReporter {

    /**
     * The list of errors found.
     */
    private ArrayList<String> errors = new ArrayList<>();

    /**
     * The list of warnings found.
     */
    private ArrayList<String> warnings = new ArrayList<>();

    /**
     * Creates a new JavaScript error handler.
     */
    public JsErrorHandler() {
        // Nothing to do here
    }

    /**
     * Returns the number of errors found.
     *
     * @return the number of errors found
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Returns the complete error text.
     *
     * @return the complete error text, or
     *         an empty string if no errors have occurred
     */
    public String getErrorText() {
        StringBuilder buffer = new StringBuilder();
        for (String s : errors) {
            if (buffer.length() > 0) {
                buffer.append("\n\n");
            }
            buffer.append(s);
        }
        return buffer.toString();
    }

    /**
     * Returns the number of warnings found.
     *
     * @return the number of warnings found
     */
    public int getWarningCount() {
        return warnings.size();
    }

    /**
     * Reports an error. This method is called by the JavaScript
     * engine when an error is encountered.
     *
     * @param msg            the error message
     * @param file           the source file name
     * @param line           the source file line number
     * @param src            the source file line text (or null)
     * @param col            the source file line offset
     */
    @Override
    public void error(String msg, String file, int line, String src, int col) {
        errors.add(format(msg, file, line, src, col));
    }

    /**
     * Reports a warning. This method is called by the JavaScript
     * engine when a warning is encountered.
     *
     * @param msg            the warning message
     * @param file           the source file name
     * @param line           the source file line number
     * @param src            the source file line text (or null)
     * @param col            the source file line offset
     */
    @Override
    public void warning(String msg, String file, int line, String src, int col) {
        warnings.add(format(msg, file, line, src, col));
    }

    /**
     * Creates a new run-time evaluator exception. This method is
     * called by the JavaScript engine when a run-time error is
     * encountered.
     *
     * @param msg            the warning message
     * @param file           the source file name
     * @param line           the source file line number
     * @param src            the source file line text (or null)
     * @param col            the source file line offset
     *
     * @return a new run-time evaluator exception
     */
    @Override
    public EvaluatorException runtimeError(String msg,
                                           String file,
                                           int line,
                                           String src,
                                           int col) {

        return new EvaluatorException(msg, file, line, src, col);
    }

    /**
     * Formats an error or a warning message.
     *
     * @param msg            the detailed message
     * @param file           the source file name
     * @param line           the source file line number
     * @param src            the source file line text (or null)
     * @param col            the source file line offset
     *
     * @return the formatted message
     */
    private String format(String msg, String file, int line, String src, int col) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(file);
        buffer.append(", line ");
        buffer.append(line);
        buffer.append(": ");
        buffer.append(msg);
        if (src != null) {
            buffer.append("\n\n");
            buffer.append(src);
            buffer.append("\n");
            for (int i = 1; i < col; i++) {
                buffer.append(" ");
            }
            buffer.append("^");
        }
        return buffer.toString();
    }
}
