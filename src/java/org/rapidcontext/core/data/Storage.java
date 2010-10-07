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
 * @author   Per Cederberg
 * @version  1.0
 */
public interface Storage {

    /**
     * Searches for an object at the specified location and returns
     * metadata about the object if found. The path may locate either
     * an index or a specific object. 
     *
     * @param path           the storage location
     *
     * @return the metadata for the object, or
     *         null if not found
     *
     * @throws StorageException if the storage couldn't be accessed
     */
    Metadata lookup(Path path) throws StorageException;

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
     * Stores an object at the specified location. The path must
     * locate a particular object or file, since direct manipulation
     * of indices is not supported. Any previous data at the
     * specified path will be overwritten or removed.
     *
     * @param path           the storage location
     * @param data           the data to store
     *
     * @throws StorageException if the data couldn't be written
     */
    void store(Path path, Object data) throws StorageException;

    /**
     * Removes an object or an index at the specified location. If
     * the path refers to an index, all contained objects and indices
     * will be removed recursively.
     *
     * @param path           the storage location
     *
     * @throws StorageException if the data couldn't be removed
     */
    void remove(Path path) throws StorageException;
}
