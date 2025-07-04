/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2025 Per Cederberg. All rights reserved.
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

import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.util.BaseException;

/**
 * A procedure exception. This class encapsulates all procedure
 * execution errors, both run-time and compile-time.
 *
 * @author Per Cederberg
 */
public class ProcedureException extends BaseException {

    /**
     * Creates a new generic procedure exception.
     *
     * @param message        the detailed error message
     */
    public ProcedureException(String message) {
        super(message);
    }

    /**
     * Creates a new in-call procedure exception.
     *
     * @param proc           the procedure called
     * @param message        the detailed error message
     */
    public ProcedureException(Procedure proc, String message) {
        super("in '" + proc.id() + "': " + message);
    }

    /**
     * Creates a new new in-call procedure exception.
     *
     * @param proc           the procedure called
     * @param e              the root cause
     */
    public ProcedureException(Procedure proc, Throwable e) {
        super("in '" + proc.id() + "': " + e.getMessage(), e);
    }
}
