/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2024 Per Cederberg. All rights reserved.
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

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A set of memory storage caches. For each mounted storage a corresponding
 * cache may be created to contain any StorableObject instances loaded.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
class Caches {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(Caches.class.getName());

    /**
     * The map of memory storages. Indexed by the origin storage path.
     */
    private HashMap<Path,MemoryStorage> cacheStorages = new HashMap<>();

    /**
     * Returns the set of origin storage paths currently mounted.
     *
     * @return the set of origin storage paths
     */
    public Set<Path> origins() {
        return cacheStorages.keySet();
    }

    /**
     * Adds a new cache for the specified storage path.
     *
     * @param storagePath    the origin storage path
     * @param mountPath      the cache storage mount path
     */
    public void mount(Path storagePath, Path mountPath) {
        if (!cacheStorages.containsKey(storagePath)) {
            MemoryStorage cache = new MemoryStorage(mountPath.toIdent(0), true, true);
            cache.setMountInfo(mountPath, true, null, 0);
            cacheStorages.put(storagePath, cache);
        }
    }

    /**
     * Removes a cache and destroys all its objects.
     *
     * @param storagePath    the origin storage path
     */
    public void unmount(Path storagePath) {
        MemoryStorage cache = cacheStorages.get(storagePath);
        if (cache != null) {
            remove(storagePath, Path.ROOT, true);
            cacheStorages.remove(storagePath);
            cache.destroy();
        }
    }

    /**
     * Retrieves all modified objects from the cache. These are storable
     * objects (like sessions) that have not yet been persisted.
     *
     * @param storagePath    the cached storage path, or null for all
     * @param path           the object location
     *
     * @return the paths of all modified objects
     */
    public Path[] listModified(Path storagePath, Path path) {
        MemoryStorage cache = cacheStorages.get(storagePath);
        return cache.query(path).filterShowHidden(true).paths().filter(p -> {
            Object obj = cache.load(p);
            if (obj instanceof StorableObject o) {
                return o.isModified();
            }
            return false;
        }).toArray(Path[]::new);
    }

    /**
     * Searches for an object in the caches.
     *
     * @param storagePath    the cached storage path, or null
     * @param path           the object location
     *
     * @return the metadata for the object, or null if not found
     */
    public Metadata lookup(Path storagePath, Path path) {
        if (storagePath == null) {
            for (MemoryStorage cache : cacheStorages.values()) {
                if (path.startsWith(cache.path())) {
                    return cache.lookup(cache.localPath(path));
                }
            }
        } else if (cacheStorages.containsKey(storagePath)) {
            return cacheStorages.get(storagePath).lookup(path);
        }
        return null;
    }

    /**
     * Loads an object from the caches. If the object is a storable
     * object, it will also be activated.
     *
     * @param storagePath    the cached storage path, or null
     * @param path           the object location
     *
     * @return the object loaded from the cache, or
     *         null if not found
     */
    public Object load(Path storagePath, Path path) {
        if (storagePath == null) {
            for (MemoryStorage cache : cacheStorages.values()) {
                if (path.startsWith(cache.path())) {
                    return cache.load(cache.localPath(path));
                }
            }
        } else if (cacheStorages.containsKey(storagePath)) {
            MemoryStorage cache = cacheStorages.get(storagePath);
            Object res = cache.load(path);
            if (res != null) {
                LOG.fine("cache " + cache.path() + ": loaded " + path);
                if (res instanceof StorableObject o) {
                    LOG.fine("cache " + cache.path() + ": activating object " + path);
                    o.activate();
                }
                return res;
            }
        }
        return null;
    }

    /**
     * Stores an object to the caches (if possible). The object will
     * only be added if it is an instance of  StorableObject and a
     * memory cache exists for the specified storage path. Existing
     * objects at the same path will be removed and destroyed.
     *
     * @param storagePath    the cached storage path, or null
     * @param path           the object location
     * @param data           the object to store
     *
     * @return true if the path matched one of the caches, or
     *         false otherwise
     */
    public boolean store(Path storagePath, Path path, Object data) {
        if (storagePath == null) {
            for (Entry<Path, MemoryStorage> e : cacheStorages.entrySet()) {
                MemoryStorage cache = e.getValue();
                if (path.startsWith(cache.path())) {
                    return store(e.getKey(), cache.localPath(path), data);
                }
            }
        } else if (cacheStorages.containsKey(storagePath)) {
            MemoryStorage cache = cacheStorages.get(storagePath);
            String debugPrefix = "cache " + cache.path() + ": ";
            path = Storage.objectPath(path);
            Object old = cache.load(path);
            if (data instanceof StorableObject o) {
                LOG.fine(debugPrefix + "passivating object " + path);
                o.passivate();
                if (data != old) {
                    try {
                        LOG.fine(debugPrefix + "storing object " + path);
                        cache.store(path, data);
                    } catch (StorageException e) {
                        LOG.log(Level.WARNING, "failed to cache object", e);
                    }
                    destroyObject(cache, path, old);
                }
            } else if (old != null) {
                removeDestroy(cache, path, old);
            } else {
                LOG.fine(debugPrefix + "object " + path + " not cacheable");
            }
            return true;
        }
        return false;
    }

    /**
     * Removes one or more objects from the caches. All objects removed
     * will also be destroyed. If the force flag is false, active objects
     * will only be passivated.
     *
     * @param storagePath    the cached storage path, or null
     * @param path           the path to remove
     * @param force          the forced removal flag
     *
     * @return true if the path matched one of the caches, or
     *         false otherwise
     */
    public boolean remove(Path storagePath, Path path, boolean force) {
        if (storagePath == null) {
            for (Entry<Path, MemoryStorage> e : cacheStorages.entrySet()) {
                MemoryStorage cache = e.getValue();
                if (path.startsWith(cache.path())) {
                    return remove(e.getKey(), cache.localPath(path), true);
                }
            }
        } else if (cacheStorages.containsKey(storagePath)) {
            MemoryStorage cache = cacheStorages.get(storagePath);
            path = Storage.objectPath(path);
            LOG.fine("removing " + path + " from cache " + cache.path());
            for (Path p : cache.query(path).paths().toArray(Path[]::new)) {
                Object obj = cache.load(p);
                if (obj instanceof StorableObject o) {
                    if (force || !o.isActive()) {
                        removeDestroy(cache, p, obj);
                    } else {
                        LOG.fine("cache " + cache.path() + ": passivating " + p);
                        o.passivate();
                    }
                } else {
                    removeDestroy(cache, p, obj);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Removes and destroys an object from a storage cache.
     *
     * @param cache          the storage cache to modify
     * @param path           the object path to remove
     * @param data           the storable object to destroy
     */
    private void removeDestroy(MemoryStorage cache, Path path, Object data) {
        String debugPrefix = "cache " + cache.path() + ": ";
        try {
            LOG.fine(debugPrefix + "removing object " + path);
            cache.remove(path);
        } catch (StorageException e) {
            LOG.log(Level.WARNING, "failed to remove cached data", e);
        }
        destroyObject(cache, path, data);
    }

    /**
     * Destroys a previously removed storable object.
     *
     * @param cache          the storage cache (for logging)
     * @param path           the object path (for logging)
     * @param data           the storable object to destroy
     */
    private void destroyObject(MemoryStorage cache, Path path, Object data) {
        if (data instanceof StorableObject o) {
            String debugPrefix = "cache " + cache.path() + ": ";
            LOG.fine(debugPrefix + "passivating removed object " + path);
            o.passivate();
            try {
                LOG.fine(debugPrefix + "destroying removed object " + path);
                o.destroy();
            } catch (StorageException e) {
                LOG.log(Level.WARNING, "failed to destroy object", e);
            }
        }
    }
}
