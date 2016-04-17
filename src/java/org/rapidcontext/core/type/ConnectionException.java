/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.type;

/**
 * A general connection or channel exception. This class encapsulates
 * all types of errors that may happen on a connection.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ConnectionException extends Exception {

    /**
     * Creates a new connection exception.
     *
     * @param message        the detailed error message
     */
    public ConnectionException(String message) {
        super(message);
    }

    /**
     * Creates a new connection exception.
     *
     * @param message        the detailed error message
     * @param e              the base exception
     */
    public ConnectionException(String message, Throwable e) {
        super(message, e);
    }
}
