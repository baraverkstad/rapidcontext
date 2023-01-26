/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2023 Per Cederberg. All rights reserved.
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

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.logging.Logger;

import org.apache.commons.lang3.ObjectUtils;
import org.rapidcontext.core.data.Dict;

/**
 * A persistent data storage and retrieval handler based on an
 * in-memory hash table. Naturally, this is not really persistent
 * in case of server shutdown, so should normally be used only for
 * run-time objects that need to be available. An advantage of the
 * memory storage compared to other implementations is that no
 * object serialization is performed, so any type of objects may
 * be stored and retrieved.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class MemoryStorage extends Storage {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(MemoryStorage.class.getName());

    /**
     * The data storage map. Indexed by the storage path. It also contains all
     * parent indices, all the way back to the root index.
     */
    private LinkedHashMap<Path,Object> objects = new LinkedHashMap<>();

    /**
     * The metadata storage map. Indexed by the storage path. This map contains
     * metadata objects corresponding to each data or index object.
     */
    private LinkedHashMap<Path,Metadata> meta = new LinkedHashMap<>();

    /**
     * The show storage info flag. When set to true, the
     * "/storageinfo" path will be enabled for this storage.
     */
    private boolean storageInfo;

    /**
     * Creates a new memory storage.
     *
     * @param id             the storage identifier
     * @param readWrite      the read write flag
     * @param storageInfo    the show storage info flag
     */
    public MemoryStorage(String id, boolean readWrite, boolean storageInfo) {
        super(id, "memory", readWrite);
        this.storageInfo = storageInfo;
        if (storageInfo) {
            objects.put(PATH_STORAGEINFO, dict);
            meta.put(PATH_STORAGEINFO, new Metadata(Dict.class, PATH_STORAGEINFO, path()));
            indexInsert(PATH_STORAGEINFO);
        }
    }

    /**
     * Checks if the specified object is supported in this storage.
     *
     * @param obj            the object instance to check
     *
     * @return true if the object is supported, or
     *         false otherwise
     */
    public boolean isStorable(Object obj) {
        return obj instanceof Dict ||
               obj instanceof StorableObject ||
               obj == ObjectUtils.NULL;
    }

    /**
     * Destroys this storage. Note that the objects in the storage
     * will NOT be destroyed by this method.
     */
    public synchronized void destroy() {
        objects.clear();
        meta.clear();
        objects = null;
        meta = null;
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
    public synchronized Metadata lookup(Path path) {
        if (storageInfo && PATH_STORAGEINFO.equals(path)) {
            Metadata m = new Metadata(Dict.class, PATH_STORAGEINFO, path());
            return m.modified(mountTime());
        }
        return meta.get(path);
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
    public synchronized Object load(Path path) {
        if (storageInfo && PATH_STORAGEINFO.equals(path)) {
            return serialize();
        }
        Object obj = objects.get(path);
        return (obj instanceof Index) ? new Index((Index) obj, false) : obj;
    }

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
    public synchronized void store(Path path, Object data) throws StorageException {
        if (path.isIndex()) {
            String msg = "cannot write to index " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        } else if (data == null) {
            String msg = "cannot store null data, use remove() instead: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        } else if (!isReadWrite()) {
            String msg = "cannot store to read-only storage at " + path();
            LOG.warning(msg);
            throw new StorageException(msg);
        } else if (!isStorable(data)) {
            String msg = "cannot store unsupported data type at " + path() + ": " +
                         data.getClass().getName();
            LOG.warning(msg);
            throw new StorageException(msg);
        } else if (storageInfo && PATH_STORAGEINFO.equals(path)) {
            String msg = "storage info is read-only: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        if (objects.containsKey(path)) {
            remove(path);
        }
        objects.put(path, data);
        meta.put(path, new Metadata(data.getClass(), path, path()));
        indexInsert(path);
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
    public synchronized void remove(Path path) throws StorageException {
        if (!isReadWrite()) {
            String msg = "cannot remove from read-only storage at " + path();
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        if (!storageInfo || !PATH_STORAGEINFO.equals(path)) {
            remove(path, true);
        }
    }

    /**
     * Removes an object or an index at the specified location. If
     * the path refers to an index, all contained objects and indices
     * will be removed recursively.
     *
     * @param path           the storage location
     * @param updateParent   the parent index update flag
     */
    private void remove(Path path, boolean updateParent) {
        Object obj = objects.get(path);
        if (obj instanceof Index) {
            Index idx = (Index) obj;
            idx.paths(path).forEach((item) -> remove(item, false));
        }
        if (obj != null) {
            objects.remove(path);
            meta.remove(path);
            if (updateParent) {
                indexRemove(path);
            }
        }
    }

    /**
     * Inserts a path into its parent index. If the index doesn't exist, it
     * will be created recursively. This method also updates the index last
     * modified timestamp.
     *
     * @param path           the path previously added
     */
    private void indexInsert(Path path) {
        Path parent = path.parent();
        Index idx = Objects.requireNonNullElse((Index) objects.get(parent), new Index());
        if (path.isIndex()) {
            idx.addIndex(path.name());
        } else {
            idx.addObject(path.name());
        }
        idx.setModified(null);
        if (objects.containsKey(parent)) {
            meta.get(parent).modified(null);
        } else {
            objects.put(parent, idx);
            meta.put(parent, new Metadata(Index.class, parent, path()));
            if (!parent.isRoot()) {
                indexInsert(parent);
            }
        }
    }

    /**
     * Removes a path from its parent index. If the index becomes empty, it
     * will be removed recursively. This method also updates the index last
     * modified timestamp.
     *
     * @param path           the path previously removed
     */
    private void indexRemove(Path path) {
        Path parent = path.parent();
        Index idx = (Index) objects.get(parent);
        if (path.isIndex()) {
            idx.removeIndex(path.name());
        } else {
            idx.removeObject(path.name());
        }
        idx.setModified(null);
        if (idx.isEmpty()) {
            objects.remove(parent);
            meta.remove(parent);
            if (!parent.isRoot()) {
                indexRemove(parent);
            }
        } else {
            meta.get(parent).modified(null);
        }
    }

    /**
     * Returns a serialized representation of this object. Used when
     * persisting to permanent storage or when accessing the object
     * from outside pure Java. Returns a shallow copy of the contained
     * dictionary.
     *
     * @return the serialized representation of this object
     */
    public Dict serialize() {
        Dict copy = super.serialize();
        copy.setInt(PREFIX_COMPUTED + "objectCount", objects.size());
        return copy;
    }
}
