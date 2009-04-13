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

import java.io.File;

/**
 * A generic data storage and retrieval interface.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public interface DataStore {

    // TODO: Create secure data store that wraps a normal data store
    //       and filters the results by user permissions.

    /**
     * Checks if a file exists in this data store. Any existing file
     * will be checked for readability.
     *
     * @param path           the file path
     *
     * @return true if a readable file was found, or
     *         false otherwise
     */
    boolean hasFile(String path);

    /**
     * Checks if a data object exists in this data store.
     *
     * @param type           the type name, or null for generic
     * @param id             the unique object id
     *
     * @return true if the object was found, or
     *         false otherwise
     */
    boolean hasData(String type, String id);

    /**
     * Finds a file in this data store. If the exist flag is
     * specified, the file returned will be checked for both
     * existence and readability. If no file is found or if it
     * didn't pass the existence test, null will be returned
     *
     * @param path           the file path
     * @param exist          the existence check flag
     *
     * @return the file object found, or
     *         null if not found
     */
    File findFile(String path, boolean exist);

    /**
     * Finds all data object identifiers of a certain type.
     *
     * @param type           the type name, or null for generic
     *
     * @return an array or data object identifiers
     */
    String[] findDataIds(String type);

    /**
     * Returns the last modified timestamp for a data object. This
     * operation should be implemented as a fast path, without need
     * for complete parsing of the data. It is intended to be used
     * for automatically invalidating objects in data object caches. 
     *
     * @param type           the type name, or null for generic
     * @param id             the unique object id
     *
     * @return the last modified timestamp, or
     *         zero (0) if unknown
     */
    long findDataTimeStamp(String type, String id);

    /**
     * Reads an identified data object of a certain type.
     *
     * @param type           the type name, or null for generic
     * @param id             the unique object id
     *
     * @return the data object read, or
     *         null if not found
     *
     * @throws DataStoreException if the data couldn't be read
     */
    Data readData(String type, String id) throws DataStoreException;

    /**
     * Writes a data object of a certain type.
     *
     * @param type           the type name, or null for generic
     * @param id             the unique object id
     * @param data           the data to write
     *
     * @throws DataStoreException if the data couldn't be written
     */
    void writeData(String type, String id, Data data) throws DataStoreException;
}
