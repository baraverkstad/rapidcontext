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

package org.rapidcontext.core.storage;

/**
 * A storage exception. This class encapsulates all storage errors.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StorageException extends Exception {

    /**
     * Creates a new storage exception.
     *
     * @param message        the detailed error message
     */
    public StorageException(String message) {
        super(message);
    }
}
