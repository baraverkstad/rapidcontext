/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2012 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.storage;

import org.apache.commons.lang.StringUtils;

/**
 * A storage wrapper class. This class allows any storage object to
 * be wrapped and modified or monitored. It is useful when
 * transformations must be applied to the storage results or the data
 * lookup routines (such as for backwards compatibility). This class
 * routes all storage requests unmodified to and from the wrapped
 * storage. It must be subclassed to provide any changes.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class WrappedStorage extends Storage {

    /**
     * The storage to encapsulate.
     */
    private Storage wrapped;

    /**
     * Creates a new wrapped (or encapsulated) storage.
     *
     * @param storageType    the storage type name
     * @param wrapped        the encapsulated storage
     */
    public WrappedStorage(String storageType, Storage wrapped) {
        super(storageType + StringUtils.removeStart(wrapped.type(), "storage"),
              wrapped.isReadWrite());
        this.wrapped = wrapped;
    }

    /**
     * Destroys this object. This method is used to free any
     * resources used when this object is no longer used. This method
     * is called when an object is removed from the in-memory storage
     * (object cache).
     *
     * @throws StorageException if the destruction failed
     */
    protected void destroy() throws StorageException {
        wrapped.destroy();
        super.destroy();
    }

    /**
     * Updates the mount information for this storage. This method
     * will update the mount time to the current timestamp.
     *
     * @param path           the mount path (or storage root path)
     * @param readWrite      the storage read-write flag
     * @param overlay        the mount overlay path
     * @param prio           the mount overlay priority
     */
    public void setMountInfo(Path path,
                             boolean readWrite,
                             Path overlay,
                             int prio) {

        wrapped.setMountInfo(path, readWrite, overlay, prio);
        super.setMountInfo(path, readWrite, overlay, prio);
    }

    /**
     * Searches for an object at the specified location and returns
     * metadata about the object if found. The path may locate either
     * an index or a specific object.
     *
     * @param path           the storage location
     *
     * @return the metadata for the object, or
     *         null if not found
     */
    public Metadata lookup(Path path) {
        return wrapped.lookup(path);
    }

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
     */
    public Object load(Path path) {
        return wrapped.load(path);
    }

    /**
     * Stores an object at the specified location. The path must
     * locate a particular object or file, since direct manipulation
     * of indices is not supported. Any previous data at the
     * specified path will be overwritten or removed. Note that only
     * dictionaries and files can be stored in a file storage.
     *
     * @param path           the storage location
     * @param data           the data to store
     *
     * @throws StorageException if the data couldn't be written
     */
    public void store(Path path, Object data) throws StorageException {
        wrapped.store(path, data);
    }

    /**
     * Removes an object or an index at the specified location. If
     * the path refers to an index, all contained objects and indices
     * will be removed recursively.
     *
     * @param path           the storage location
     *
     * @throws StorageException if the data couldn't be removed
     */
    public void remove(Path path) throws StorageException {
        wrapped.remove(path);
    }
}
