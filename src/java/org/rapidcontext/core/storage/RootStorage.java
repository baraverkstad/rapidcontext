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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.ObjectUtils;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.task.Scheduler;
import org.rapidcontext.core.task.Task;
import org.rapidcontext.core.type.Type;

/**
 * The root storage that provides a unified view of other storages.
 * This class provides a number of unique storage services:
 *
 * <ul>
 *   <li><strong>Mounting</strong> -- Sub-storages can be mounted
 *       on a "storage/..." subpath, providing a global namespace
 *       for objects. Storage paths are automatically converted to
 *       local paths for all storage operations.
 *   <li><strong>Unification</strong> -- Mounted storages can also
 *       be overlaid or unified with the root path, providing a
 *       storage view where objects from all storages are mixed. The
 *       mount order defines which object names take priority, in
 *       case several objects have the same paths.
 *   <li><strong>Object Initialization</strong> -- Dictionary objects
 *       will be inspected upon retrieval from a mounted and unified
 *       path (i.e. not when retrieved directly from the storage).
 *       If a matching type handler or class can be located, the
 *       corresponding object will be created, initialized and cached
 *       for future references.
 * </ul>
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class RootStorage extends Storage {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(RootStorage.class.getName());

    /**
     * The number of seconds between each run of the object cache
     * cleaner job.
     */
    private static final int PASSIVATE_INTERVAL_SECS = 30;

    /**
     * The meta-data storage for mount points and parent indices.
     * The mounted storages will be added to this storage under
     * their corresponding mount path (appended to form an object
     * path instead of an index path).
     */
    private MemoryStorage metadata = new MemoryStorage("metadata", true, false);

    /**
     * The sorted array of mounted storages. This array is sorted
     * every time a mount point is added or modified.
     */
    private Array mountedStorages = new Array();

    /**
     * The map of cache memory storages. For each mounted and
     * overlaid storage, a corresponding cache storage is created to
     * contain any StorableObject instances. The cache storages are
     * indexed by their parent storage path (not their own actual
     * storage paths).
     */
    private HashMap<Path,MemoryStorage> cacheStorages = new HashMap<>();

    /**
     * Creates a new root storage.
     *
     * @param readWrite      the read write flag
     */
    public RootStorage(boolean readWrite) {
        super("/", "root", readWrite);
        dict.set("storages", mountedStorages);
        try {
            metadata.store(PATH_STORAGEINFO, dict);
        } catch (StorageException e) {
            LOG.severe("error while initializing virtual storage: " +
                       e.getMessage());
        }
        Task cacheCleaner = new Task("storage cache cleaner") {
            public void execute() {
                cacheClean(false);
            }
        };
        long delay = PASSIVATE_INTERVAL_SECS * 1000L;
        Scheduler.schedule(cacheCleaner, delay, delay);
    }

    /**
     * Returns the storage at a specific storage location. If the
     * exact match flag is set, the path must exactly match the mount
     * point of a storage. If exact matching is not required, the
     * parent storage for the path will be returned. This method will
     * also search for matching cache storages.
     *
     * @param path           the storage location
     * @param exact          the exact match flag
     *
     * @return the storage found, or
     *         null if not found
     */
    private Storage getMountedStorage(Path path, boolean exact) {
        if (path == null) {
            return null;
        } else if (exact) {
            return (Storage) metadata.load(path.child("storage", false));
        } else {
            for (Object o : mountedStorages) {
                Storage storage = (Storage) o;
                if (path.startsWith(storage.path())) {
                    return storage;
                }
            }
            for (MemoryStorage storage : cacheStorages.values()) {
                if (path.startsWith(storage.path())) {
                    return storage;
                }
            }
            return null;
        }
    }

    /**
     * Creates or removes the storage metadata. The metadata needs to
     * be updated with the mount points and mount overlay points in
     * order to create all the intermediate indices.
     *
     * @param storage        the storage to update for
     * @param update         the boolean update or remove flag
     *
     * @throws StorageException if the data couldn't be written
     */
    private void updateStorageMetadata(Storage storage, boolean update)
    throws StorageException {

        Path path = storage.path().child("storage", false);
        if (update) {
            metadata.store(path, storage);
        } else {
            metadata.remove(path);
        }
        Path overlay = storage.mountOverlayPath();
        if (overlay != null && !overlay.isRoot()) {
            overlay = overlay.child(".", false);
            if (update) {
                metadata.store(overlay, ObjectUtils.NULL);
            } else {
                metadata.remove(overlay);
            }
        }
    }

    /**
     * Creates or removes a cache memory storage for the specified
     * path.
     *
     * @param path           the storage mount path
     * @param overlay        the root overlay path
     *
     * @throws StorageException if the cache couldn't be created or
     *             removed
     */
    private void updateStorageCache(Path path, Path overlay)
    throws StorageException {

        Path cachePath = Storage.PATH_STORAGE_CACHE.descendant(path.subPath(1));
        MemoryStorage cache = cacheStorages.get(path);
        if (overlay != null && cache == null) {
            cache = new MemoryStorage("cache", true, true);
            cache.setMountInfo(cachePath, true, overlay, 0);
            updateStorageMetadata(cache, true);
            cacheStorages.put(path, cache);
        } else if (overlay == null && cache != null) {
            updateStorageMetadata(cache, false);
            cacheStorages.remove(path);
            cacheRemove(cache, Path.ROOT, false, true);
            cache.destroy();
        }
    }

    /**
     * Mounts a storage to a unique path. The path may not collide
     * with a previously mounted storage, such that it would hide or
     * be hidden by the other storage. Overlapping parent indices
     * will be merged automatically. In addition to adding the
     * storage to the specified path, it's contents may also be
     * overlaid directly on the root path.
     *
     * @param storage        the storage to mount
     * @param path           the mount path
     * @param readWrite      the read write flag
     * @param overlay        the root overlay path
     * @param prio           the root overlay search priority (higher numbers
     *                       are searched before lower numbers)
     *
     * @throws StorageException if the storage couldn't be mounted
     */
    public synchronized void mount(Storage storage,
                                   Path path,
                                   boolean readWrite,
                                   Path overlay,
                                   int prio)
    throws StorageException {

        String  msg;

        if (!path.isIndex()) {
            msg = "cannot mount storage to a non-index path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        } else if (!path.startsWith(PATH_STORAGE)) {
            msg = "cannot mount storage to a non-storage path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        } else if (metadata.lookup(path) != null) {
            msg = "storage mount path conflicts with another mount: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        LOG.fine("mounting " + storage);
        storage.setMountInfo(path, readWrite, overlay, prio);
        updateStorageMetadata(storage, true);
        mountedStorages.add(storage);
        mountedStorages.sort();
        updateStorageCache(path, overlay);
    }

    /**
     * Remounts a storage for a unique path. The path or the storage
     * are not modified, but only the mounting options.
     *
     * @param path           the mount path
     * @param readWrite      the read write flag
     * @param overlay        the root overlay path
     * @param prio           the root overlay search priority (higher numbers
     *                       are searched before lower numbers)
     *
     * @throws StorageException if the storage couldn't be remounted
     */
    public synchronized void remount(Path path, boolean readWrite, Path overlay, int prio)
    throws StorageException {

        Storage storage = getMountedStorage(path, true);
        if (storage == null) {
            String msg = "no mounted storage found matching path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        LOG.fine("remounting " + storage);
        updateStorageMetadata(storage, false);
        storage.setMountInfo(storage.path(), readWrite, overlay, prio);
        updateStorageMetadata(storage, true);
        mountedStorages.sort();
        updateStorageCache(path, overlay);
    }

    /**
     * Unmounts a storage from the specified path. The path must have
     * previously been used to mount a storage, which will also be
     * destroyed by this operation.
     *
     * @param path           the mount path
     *
     * @throws StorageException if the storage couldn't be unmounted
     */
    public synchronized void unmount(Path path) throws StorageException {
        Storage  storage = getMountedStorage(path, true);
        String   msg;

        if (storage == null) {
            msg = "no mounted storage found matching path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        LOG.fine("unmounting " + storage);
        updateStorageCache(path, null);
        mountedStorages.remove(mountedStorages.indexOf(storage));
        updateStorageMetadata(storage, false);
        storage.destroy();
    }

    /**
     * Unmounts and destroys all mounted storages.
     */
    public synchronized void unmountAll() {
        Storage  storage;
        String   msg;

        while (mountedStorages.size() > 0) {
            storage = (Storage) mountedStorages.get(mountedStorages.size() - 1);
            try {
                unmount(storage.path());
            } catch (Exception e) {
                msg = "failed to unmount storage at " + storage.path();
                LOG.log(Level.WARNING, msg, e);
            }
        }
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
        Storage   storage = getMountedStorage(path, false);
        Metadata  meta = null;

        if (storage != null) {
            meta = storage.lookup(storage.localPath(path));
            return (meta == null) ? null : new Metadata(path, meta);
        } else {
            meta = metadata.lookup(path);
            for (Object o : mountedStorages) {
                storage = (Storage) o;
                Path overlay = storage.mountOverlayPath();
                if (overlay != null && path.startsWith(overlay)) {
                    Path subpath = path.subPath(overlay.depth());
                    meta = Metadata.merge(meta, lookupObject(storage, subpath));
                }
            }
            return meta;
        }
    }

    /**
     * Searches for an object in a specified storage. The object will
     * be looked up primarily in the cache and thereafter in the
     * actual storage.
     *
     * @param storage        the storage to search in
     * @param path           the storage location
     *
     * @return the metadata for the object, or
     *         null if not found
     */
    private Metadata lookupObject(Storage storage, Path path) {
        Metadata meta = null;
        MemoryStorage cache = cacheStorages.get(storage.path());
        if (cache != null) {
            meta = cache.lookup(path);
        }
        return Metadata.merge(meta, storage.lookup(path));
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
        Storage  storage = getMountedStorage(path, false);
        Object   res;
        Index    idx = null;

        if (storage != null) {
            res = storage.load(storage.localPath(path));
            if (res instanceof Index) {
                idx = (Index) res;
                return new Index(path, idx);
            } else {
                return res;
            }
        } else {
            res = metadata.load(path);
            if (res instanceof Index) {
                idx = (Index) res;
            } else if (res != null && res != ObjectUtils.NULL) {
                return res;
            }
            for (Object o : mountedStorages) {
                storage = (Storage) o;
                Path overlay = storage.mountOverlayPath();
                if (overlay != null && path.startsWith(overlay)) {
                    Path subpath = path.subPath(overlay.depth());
                    res = loadObject(storage, subpath);
                    if (res instanceof Index) {
                        idx = Index.merge(idx, (Index) res);
                    } else if (res != null) {
                        return res;
                    }
                }
            }
        }
        return idx;
    }

    /**
     * Loads an object from the specified storage. The storage cache
     * will be used primarily, if it exists. If an object is found in
     * the storage that can be cached, it will be initialized and
     * cached by this method.
     *
     * @param storage        the storage to load from
     * @param path           the storage location
     *
     * @return the data read, or
     *         null if not found
     */
    private Object loadObject(Storage storage, Path path) {
        boolean isCacheable = cacheStorages.containsKey(storage.path());
        Index idx = null;
        Object res = cacheGet(storage.path(), path);
        if (res instanceof Index) {
            idx = (Index) res;
        } else if (res instanceof StorableObject) {
            LOG.fine("loaded cached object " + path + " from " + storage.path());
            return res;
        }
        res = storage.load(path);
        if (isCacheable && res instanceof Dict) {
            String id = path.toIdent(1);
            res = initObject(id, (Dict) res);
            cacheAdd(storage.path(), path, res);
        }
        if (res != null) {
            LOG.fine("loaded " + path + " from " + storage.path() + ": " + res);
        }
        if (idx != null && (res == null || res instanceof Index)) {
            return Index.merge(idx, (Index) res);
        } else {
            return res;
        }
    }

    /**
     * Initializes an object with the corresponding object type (if
     * found).
     *
     * @param id             the object id
     * @param dict           the dictionary data
     *
     * @return the StorableObject instance created, or
     *         the input dictionary if no type matched
     */
    private Object initObject(String id, Dict dict) {
        Constructor<?> ctor = Type.constructor(this, dict);
        if (ctor != null) {
            String typeId = dict.getString(KEY_TYPE, null);
            Object[] args = new Object[] { id, typeId, dict };
            try {
                StorableObject obj = (StorableObject) ctor.newInstance(args);
                obj.init();
                obj.activate();
                return obj;
            } catch (Exception e) {
                String msg = "failed to create instance of " +
                    ctor.getClass().getName() + " for object " + id +
                    " of type " + typeId;
                LOG.log(Level.WARNING, msg, e);
                dict.add("_error", msg);
                return dict;
            }
        } else if (!dict.containsKey(KEY_ID)) {
            dict.set(KEY_ID, id);
        }
        return dict;
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
        Storage storage = getMountedStorage(path, false);
        if (storage != null && path.startsWith(PATH_STORAGE_CACHE)) {
            Path storageId = storage.path().subPath(PATH_STORAGE_CACHE.depth());
            Path storagePath = PATH_STORAGE.descendant(storageId);
            cacheAdd(storagePath, storage.localPath(path), data);
        } else {
            store(storage, path, data, true);
        }
        LOG.fine("stored " + path);
    }

    /**
     * Stores an object at the specified location. The path must
     * locate a particular object or file, since direct manipulation
     * of indices is not supported. Any previous data at the
     * specified path will be overwritten or removed. If the caching
     * flag is not set, no updates will be made to the storage cache.
     *
     * @param storage        the storage to write to (or null)
     * @param path           the storage location
     * @param data           the data to store
     * @param caching        the caching update flag
     *
     * @throws StorageException if the data couldn't be written
     */
    private void store(Storage storage, Path path, Object data, boolean caching)
        throws StorageException {

        if (storage != null) {
            if (caching) {
                cacheAdd(storage.path(), storage.localPath(path), data);
            }
            storage.store(storage.localPath(path), data);
        } else {
            boolean stored = false;
            for (Object o : mountedStorages) {
                storage = (Storage) o;
                Path overlay = storage.mountOverlayPath();
                if (overlay != null && path.startsWith(overlay)) {
                    Path subpath = path.subPath(overlay.depth());
                    if (!stored && storage.isReadWrite()) {
                        if (caching) {
                            cacheAdd(storage.path(), subpath, data);
                        }
                        storage.store(subpath, data);
                        stored = true;
                    } else if (caching) {
                        cacheRemove(storage.path(), subpath);
                    }
                }
            }
            if (!stored) {
                throw new StorageException("no writable storage found for " +
                                           path);
            }
        }
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
        Storage storage = getMountedStorage(path, false);
        if (storage != null && path.startsWith(PATH_STORAGE_CACHE)) {
            Path storageId = storage.path().subPath(PATH_STORAGE_CACHE.depth());
            Path storagePath = PATH_STORAGE.descendant(storageId);
            cacheRemove(storagePath, storage.localPath(path));
        } else {
            remove(storage, path, true);
        }
        LOG.fine("removed " + path);
    }

    /**
     * Removes an object or an index at the specified location. If
     * the path refers to an index, all contained objects and indices
     * will be removed recursively. If the caching flag is not set,
     * no updates will be made to the storage cache.
     *
     * @param storage        the storage to write to (or null)
     * @param path           the storage location
     * @param caching        the caching update flag
     *
     * @throws StorageException if the data couldn't be removed
     */
    private void remove(Storage storage, Path path, boolean caching)
        throws StorageException {

        if (storage != null) {
            if (caching) {
                cacheRemove(storage.path(), storage.localPath(path));
            }
            storage.remove(storage.localPath(path));
        } else {
            if (caching) {
                cacheRemove(null, path);
            }
            for (Object o : mountedStorages) {
                storage = (Storage) o;
                Path overlay = storage.mountOverlayPath();
                boolean isMatch = (overlay != null) && path.startsWith(overlay);
                if (storage.isReadWrite() && isMatch && overlay != null) {
                    Path subpath = path.subPath(overlay.depth());
                    storage.remove(subpath);
                }
            }
        }
    }

    /**
     * Retrieves an object from the cache. If the object is a storable
     * object, it will also be activated.
     *
     * @param storagePath    the storage path being cached
     * @param path           the object location
     *
     * @return the object loaded from the cache, or
     *         null if not found
     */
    private Object cacheGet(Path storagePath, Path path) {
        MemoryStorage cache = cacheStorages.get(storagePath);
        Object res = cache.load(path);
        if (res != null) {
            LOG.fine("cache " + cache.path() + ": loaded " + path);
        }
        if (res instanceof StorableObject) {
            LOG.fine("cache " + cache.path() + ": activating object " + path);
            ((StorableObject) res).activate();
        }
        return res;
    }

    /**
     * Adds an object to a storage cache (if possible). The object
     * will only be added if it is an instance of  StorableObject and
     * a memory cache exists for the specified storage path. Any old
     * object will be removed and destroyed.
     *
     * @param storagePath    the storage path to cache for
     * @param path           the object location
     * @param data           the object to store
     */
    private void cacheAdd(Path storagePath, Path path, Object data) {
        MemoryStorage cache = cacheStorages.get(storagePath);
        if (cache != null) {
            cacheReplace(cache, path, data, cache.load(path));
        }
    }

    /**
     * Removes one or more objects from the storage cache. All the
     * objects removed will be destroyed, but not persisted if
     * modified.
     *
     * @param storagePath    the storage path or null for all
     * @param path           the path to remove
     */
    private void cacheRemove(Path storagePath, Path path) {
        if (storagePath == null) {
            LOG.fine("removing " + path + " from all caches");
            for (Path cachePath : cacheStorages.keySet()) {
                cacheRemove(cachePath, path);
            }
        } else {
            MemoryStorage cache = cacheStorages.get(storagePath);
            if (cache != null) {
                LOG.fine("removing " + path + " from cache " + cache.path());
                cacheRemove(cache, path, false, true);
            }
        }
    }

    /**
     * Removes one or more objects from a storage cache. All objects
     * removed will be either persisted (if modified) and/or removed
     * depending on the store and force removal flags.
     *
     * @param cache          the storage cache to modify
     * @param basePath       the object path to remove
     * @param store          the persist modified objects flag
     * @param force          the forced removal flag
     */
    private void cacheRemove(MemoryStorage cache,
                             Path basePath,
                             boolean store,
                             boolean force) {

        String debugPrefix = "cache " + cache.path() + ": ";
        Path[] paths = cache.query(basePath).paths().toArray(Path[]::new);
        for (Path path : paths) {
            boolean keepObject = false;
            Object obj = cache.load(path);
            if (obj instanceof StorableObject) {
                StorableObject storable = (StorableObject) obj;
                if (store && storable.isModified()) {
                    try {
                        LOG.fine(debugPrefix + "persisting modified object " + path);
                        store(null, path, storable, false);
                    } catch (StorageException e) {
                        LOG.log(Level.WARNING, "failed to persist cached object", e);
                    }
                }
                keepObject = !force && storable.isActive();
            }
            if (keepObject) {
                cacheReplace(cache, path, obj, obj);
            } else {
                cacheReplace(cache, path, null, obj);
            }
        }
    }

    /**
     * Replaces a single object in a storage cache. This operation is
     * atomic, meaning that old object are destroyed only after the
     * new object has been inserted into the cache.
     *
     * @param cache          the storage cache to modify
     * @param path           the object path to write
     * @param newData        the new data to insert, or null for none
     * @param oldData        the old data to remove, or null for none
     */
    private void cacheReplace(MemoryStorage cache,
                              Path path,
                              Object newData,
                              Object oldData) {

        String debugPrefix = "cache " + cache.path() + ": ";
        if (newData instanceof StorableObject) {
            StorableObject storable = (StorableObject) newData;
            LOG.fine(debugPrefix + "passivating object " + path);
            storable.passivate();
            if (newData != oldData) {
                try {
                    LOG.fine(debugPrefix + "adding object " + path);
                    cache.store(path, newData);
                } catch (StorageException e) {
                    LOG.log(Level.WARNING, "failed to cache object", e);
                }
            }
        } else if (oldData != null) {
            try {
                LOG.fine(debugPrefix + "removing object " + path);
                cache.remove(path);
            } catch (StorageException e) {
                LOG.log(Level.WARNING, "failed to remove cached data", e);
            }
        }
        if (oldData != newData && oldData instanceof StorableObject) {
            StorableObject storable = (StorableObject) oldData;
            LOG.fine(debugPrefix + "passivating removed object " + path);
            storable.passivate();
            try {
                LOG.fine(debugPrefix + "destroying removed object " + path);
                storable.destroy();
            } catch (StorageException e) {
                LOG.log(Level.WARNING, "failed to destroy object", e);
            }
        }
    }

    /**
     * Destroys all cached objects. The objects will first be
     * passivated and thereafter queried for their status. All
     * modified objects will be stored persistently if possible, but
     * errors will only be logged. If the force clean flag is set,
     * all objects in the cache will be destroyed. Otherwise only
     * inactive objects.<p>
     *
     * This method is called regularly from a background job in
     * order to destroy inactive objects.
     *
     * @param force          the forced clean flag
     */
    public void cacheClean(boolean force) {
        for (MemoryStorage storage : cacheStorages.values()) {
            cacheRemove(storage, Path.ROOT, true, force);
        }
    }
}
