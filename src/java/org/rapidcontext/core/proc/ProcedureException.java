/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2022 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.proc;

/**
 * A procedure exception. This class encapsulates all procedure
 * execution errors, both run-time and compile-time.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ProcedureException extends Exception {

    /**
     * Creates a new procedure exception.
     *
     * @param message        the detailed error message
     */
    public ProcedureException(String message) {
        super(message);
    }

    /**
     * Creates a new procedure exception.
     *
     * @param message        the detailed error message
     * @param e              the base exception
     */
    public ProcedureException(String message, Throwable e) {
        super(message + ": " + e.getMessage(), e);
    }
}
