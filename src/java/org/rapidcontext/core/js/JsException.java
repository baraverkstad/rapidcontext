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

/**
 * A JavaScript processing exception. This class encapsulates all
 * JavaScript errors.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class JsException extends Exception {

    /**
     * Creates a new JavaScript processing exception.
     *
     * @param message        the detailed error message
     */
    public JsException(String message) {
        super(message);
    }

    /**
     * Creates a new JavaScript processing exception.
     *
     * @param message        the detailed error message
     * @param e              the base exception
     */
    public JsException(String message, Throwable e) {
        super(message, e);
    }
}
