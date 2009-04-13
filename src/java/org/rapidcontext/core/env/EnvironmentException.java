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

package org.rapidcontext.core.env;

/**
 * An environment exception. This class encapsulates all environment
 * loading errors.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class EnvironmentException extends Exception {

    /**
     * Creates a new environment exception.
     *
     * @param message        the detailed error message
     */
    public EnvironmentException(String message) {
        super(message);
    }

    /**
     * Creates a new environment exception.
     *
     * @param message        the detailed error message
     * @param e              the base exception
     */
    public EnvironmentException(String message, Throwable e) {
        super(message, e);
    }
}
