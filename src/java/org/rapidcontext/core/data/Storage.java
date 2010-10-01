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
 * See the RapidContext LICENSE.txt file for more details.
 */

package org.rapidcontext.core.data;

/**
 * The persistent data storage and retrieval interface. This
 * interface is implemented by services offering persistent
 * storage and lookup of files and objects.
 *
 * @author Per Cederberg
 * @version 1.0
 */
public interface Storage {

    /**
     * The meta-data key for the object type. The value stored is a
     * string, using one of the predefined constant type values.
     *
     * @see #TYPE_INDEX
     * @see #TYPE_OBJECT
     * @see #TYPE_FILE
     */
    public static final String KEY_TYPE = "type";

    /**
     * The meta-data key for the Java class of the object. The value
     * stored is the actual Class of the object.
     */
    public static final String KEY_CLASS = "class";

    /**
     * The meta-data key for the last modified date. The value stored
     * is a Long representing the epoch time in milliseconds.
     */
    public static final String KEY_MODIFIED = "lastModified";

    /**
     * The meta-data value for the index type.
     */
    public static final String TYPE_INDEX = "index";

    /**
     * The meta-data value for the object type.
     */
    public static final String TYPE_OBJECT = "object";

    /**
     * The meta-data value for the file type.
     */
    public static final String TYPE_FILE = "file";

    /**
     * Searches for an object at the specified location and returns
     * meta-data about the object if found. The path may locate
     * either an index or a specific object. 
     *
     * @param path           the storage location
     *
     * @return the meta-data dictionary for the object, or
     *         null if not found
     *
     * @throws StorageException if the storage couldn't be accessed
     */
    Dict lookup(Path path) throws StorageException;

    /**
     * Loads an object from the specified location. The path may
     * locate either an index or a specific object. In case of an
     * index, the data returned is an index dictionary listing of
     * all objects in it.
     *
     * @param path           the storage location
     *
     * @return the data read, or
     *         null if not found
     *
     * @throws StorageException if the data couldn't be read
     */
    Object load(Path path) throws StorageException;

    /**
     * Stores or removes an object at the specified location. The
     * path must locate a particular object or file, since direct
     * manipulation of indices is not supported. Any previous data
     * at the specified path will be overwritten or removed without
     * any notice. The data types supported for storage depends on
     * implementation, but normally files and dictionaries are
     * accepted.
     *
     * @param path           the storage location
     * @param data           the data to store, or null to delete
     *
     * @throws StorageException if the data couldn't be written
     */
    void store(Path path, Object data) throws StorageException;
}
