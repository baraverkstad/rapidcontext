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
 * A storable object interface. Any object implementing this
 * interface will be informed by the appropriate storage containers
 * when inserted or removed from the storage.
 *
 * @author Per Cederberg
 * @version 1.0
 */
public interface Storable {

    /**
     * Initializes this object. This method is used to perform any
     * initialization required before accessing this object from the
     * storage. This method will be called exactly once before the
     * object is made accessible in a storage.
     *
     * @throws StorageException if the initialization failed
     */
    void init() throws StorageException;

    /**
     * Destroys this object. This method is used to free any
     * resources used once this object is no longer accessible from
     * the storage. This method will be called exactly once after the
     * object is no longer accessible in a storage.
     *
     * @throws StorageException if the destruction failed
     */
    void destroy() throws StorageException;

    /**
     * Returns a serialized representation of this object. Used when
     * accessing the object storage from outside pure Java. Ideally
     * the dictionary returned should contain all the data required
     * to recreate this object, but it may also be treated as a way
     * to introspect this object.
     *
     * @return the serialized representation of this object
     *
     * @throws StorageException if serialization is not supported
     */
    Dict serialize() throws StorageException;
}
