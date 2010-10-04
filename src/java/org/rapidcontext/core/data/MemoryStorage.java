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

import java.util.LinkedHashMap;
import java.util.logging.Logger;

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
public class MemoryStorage implements Storage {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(MemoryStorage.class.getName());

    /**
     * The data storage map. Indexed by the storage path.
     */
    private LinkedHashMap objects = new LinkedHashMap();

    /**
     * The meta-data storage map. Indexed by the storage path. This
     * map contains a dictionary with meta-data for each data object.
     * It will also contain all the parent indices, all the way back
     * to the root index.
     */
    private LinkedHashMap meta = new LinkedHashMap();

    /**
     * Creates a new memory storage.
     */
    public MemoryStorage() {
        // Nothing to do here
    }

    /**
     * Searches for an object at the specified location and returns
     * meta-data about the object if found. The path may locate
     * either an index or a specific object. 
     *
     * @param path           the storage location
     *
     * @return the meta-data dictionary for the object, or
     *         null if not found
     */
    public Dict lookup(Path path) throws StorageException {
        Dict dict = (Dict) meta.get(path);
        if (dict instanceof Index) {
            Object modified = dict.get(KEY_MODIFIED);
            dict = new Dict();
            dict.set(KEY_TYPE, TYPE_INDEX);
            dict.set(KEY_CLASS, Index.class);
            dict.set(KEY_MODIFIED, modified);
        }
        return dict;
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
     *
     * @throws StorageException if the data couldn't be read
     */
    public Object load(Path path) throws StorageException {
        if (path.isIndex()) {
            Object obj = meta.get(path);
            return (obj instanceof Index) ? obj : null;
        } else {
            return objects.get(path);
        }
    }

    /**
     * Stores or removes an object at the specified location. The
     * path must locate a particular object or file, since direct
     * manipulation of indices is not supported. Any previous data
     * at the specified path will be overwritten or removed without
     * any notice.
     *
     * @param path           the storage location
     * @param data           the data to store, or null to delete
     *
     * @throws StorageException if the data couldn't be written
     */
    public void store(Path path, Object data) throws StorageException {
        if (path.isIndex()) {
            String msg = "cannot write to index " + path.toString();
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        if (data == null) {
            Object obj = null;
            if (objects.containsKey(path)) {
                obj = objects.get(path);
            }
            objects.remove(path);
            meta.remove(path);
            indexRemove(path);
            if (obj instanceof Storable) {
                ((Storable) obj).destroy();
            }
        } else {
            if (data instanceof Storable) {
                ((Storable) data).init();
            }
            objects.put(path, data);
            meta.put(path, createMeta(path, data));
            indexInsert(path);
        }
    }

    /**
     * Creates a meta-data representation of the specified data
     * object.
     *
     * @param path           the storage location
     * @param data           the data stored
     *
     * @return the created meta-data dictionary
     */
    private Dict createMeta(Path path, Object data) {
        Dict dict = new Dict();
        dict.set(KEY_TYPE, TYPE_OBJECT);
        dict.set(KEY_CLASS, data.getClass());
        dict.set(KEY_MODIFIED, new Long(System.currentTimeMillis()));
        return dict;
    }

    /**
     * Inserts a path into the meta-data index structure. Each of the
     * parent indices in the path will be updated until either the
     * root index is reached or no changes are required to the index.
     * The meta-data for the specified path itself is not modified,
     * only the parent indices are changed.
     *
     * @param path           the path previously added
     */
    private void indexInsert(Path path) {
        Path     parent = path.parent();
        Index    idx = (Index) meta.get(parent);
        boolean  modified = false;

        if (idx == null) {
            idx = new Index(parent);
        }
        if (path.isIndex()) {
            modified = idx.addIndex(path.name());
        } else {
            modified = idx.addObject(path.name());
        }
        if (modified) {
            idx.set(KEY_MODIFIED, new Long(System.currentTimeMillis()));
            if (!meta.containsKey(parent)) {
                meta.put(parent, idx);
                if (!parent.isRoot()) {
                    indexInsert(parent);
                }
            }
        }
    }

    /**
     * Removes a path from the meta-data index structure. Each of the
     * parent indices in the path will be updated until either the
     * root index is reached or no changes are required to the index.
     * The meta-data for the specified path itself is not modified,
     * only the parent indices are changed.
     *
     * @param path           the path previously removed
     */
    private void indexRemove(Path path) {
        Path     parent = path.parent();
        Index    idx = (Index) meta.get(parent);
        boolean  modified = false;

        if (idx != null) {
            if (path.isIndex()) {
                modified = idx.removeIndex(path.name());
            } else {
                modified = idx.removeObject(path.name());
            }
            if (modified) {
                idx.set(KEY_MODIFIED, new Long(System.currentTimeMillis()));
                if (idx.indices().size() <= 0 && idx.objects().size() <= 0) {
                    meta.remove(parent);
                    if (!parent.isRoot()) {
                        indexRemove(parent);
                    }
                }
            }
        }
    }
}
