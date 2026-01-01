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

package org.rapidcontext.util;

/**
 * An abstract base exception. This class exists to make it easy to
 * catch any type of checked exception.
 *
 * @author Per Cederberg
 */
public abstract class BaseException extends Exception {

    /**
     * Creates a new base exception.
     *
     * @param message        the detailed error message
     */
    public BaseException(String message) {
        super(message);
    }

    /**
     * Creates a new base exception.
     *
     * @param message        the detailed error message
     * @param e              the base exception
     */
    public BaseException(String message, Throwable e) {
        super(message, e);
    }
}
