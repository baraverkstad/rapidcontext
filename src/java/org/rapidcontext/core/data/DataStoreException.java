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

package org.rapidcontext.core.data;

/**
 * A data store exception. This class encapsulates all data store
 * errors.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class DataStoreException extends Exception {

    /**
     * Creates a new data store exception.
     *
     * @param message        the detailed error message
     */
    public DataStoreException(String message) {
        super(message);
    }
}
