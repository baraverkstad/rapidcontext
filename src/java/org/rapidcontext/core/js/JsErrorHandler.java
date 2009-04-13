/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2009 Per Cederberg & Dynabyte AB.
 * All rights reserved.
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

package org.rapidcontext.core.js;

import java.util.ArrayList;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

/**
 * A JavaScript error handler. This class collects any JavaScript
 * errors and warnings and makes them available in a handy format.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
class JsErrorHandler implements ErrorReporter {

    /**
     * The list of errors found.
     */
    private ArrayList errors = new ArrayList();

    /**
     * The list of warnings found.
     */
    private ArrayList warnings = new ArrayList();

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
        StringBuffer  buffer = new StringBuffer();

        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) {
                buffer.append("\n\n");
            }
            buffer.append(errors.get(i));
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
     * @param srcName        the source file name
     * @param line           the source file line number
     * @param lineSrc        the source file line text (or null)
     * @param lineOffset     the source file line offset
     */
    public void error(String msg,
                      String srcName,
                      int line,
                      String lineSrc,
                      int lineOffset) {

        errors.add(format(msg, srcName, line, lineSrc, lineOffset));
    }

    /**
     * Reports a warning. This method is called by the JavaScript
     * engine when a warning is encountered.
     *
     * @param msg            the warning message
     * @param srcName        the source file name
     * @param line           the source file line number
     * @param lineSrc        the source file line text (or null)
     * @param lineOffset     the source file line offset
     */
    public void warning(String msg,
                        String srcName,
                        int line,
                        String lineSrc,
                        int lineOffset) {

        warnings.add(format(msg, srcName, line, lineSrc, lineOffset));
    }

    /**
     * Creates a new run-time evaluator exception. This method is
     * called by the JavaScript engine when a run-time error is
     * encountered.
     *
     * @param msg            the warning message
     * @param srcName        the source file name
     * @param line           the source file line number
     * @param lineSrc        the source file line text (or null)
     * @param lineOffset     the source file line offset
     *
     * @return a new run-time evaluator exception
     */
    public EvaluatorException runtimeError(String msg,
                                           String srcName,
                                           int line,
                                           String lineSrc,
                                           int lineOffset) {

        return new EvaluatorException(msg, srcName, line, lineSrc, lineOffset);
    }

    /**
     * Formats an error or a warning message.
     *
     * @param msg            the detailed message
     * @param srcName        the source file name
     * @param line           the source file line number
     * @param lineSrc        the source file line text (or null)
     * @param lineOffset     the source file line offset
     *
     * @return the formatted message
     */
    private String format(String msg,
                          String srcName,
                          int line,
                          String lineSrc,
                          int lineOffset) {

        StringBuffer  buffer = new StringBuffer();

        buffer.append(srcName);
        buffer.append(", line ");
        buffer.append(line);
        buffer.append(": ");
        buffer.append(msg);
        if (lineSrc != null) {
            buffer.append("\n\n");
            buffer.append(lineSrc);
            buffer.append("\n");
            for (int i = 1; i < lineOffset; i++) {
                buffer.append(" ");
            }
            buffer.append("^");
        }
        return buffer.toString();
    }
}
