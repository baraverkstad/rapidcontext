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
     * The storage path for public web files.
     */
    public static final Path PATH_FILES = Path.from("/files/");

    /**
     * The storage path to the JAR library files.
     */
    public static final Path PATH_LIB = Path.from("/lib/");

    /**
     * The storage path for mounted storages.
     */
    public static final Path PATH_STORAGE = Path.from("/storage/");

    /**
     * The storage path for mounted storage caches.
     */
    public static final Path PATH_STORAGE_CACHE = Path.resolve(PATH_STORAGE, "cache/");

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
     * The map of memory storages (caches). For each mounted storage
     * (with overlay), a corresponding cache storage is created to
     * contain any StorableObject instances loaded. The caches are
     * indexed by the mount path (of the corresponding storage).
     */
    private HashMap<Path,MemoryStorage> cacheStorages = new HashMap<>();

    /**
     * Checks if a path corresponds to a known binary file path.
     *
     * @param path           the path to check
     *
     * @return true if the path is known to contain only files, or
     *         false otherwise
     */
    public static boolean isBinaryPath(Path path) {
        return (
            path.startsWith(PATH_FILES) ||
            path.startsWith(PATH_LIB) ||
            path.startsWith(PATH_STORAGE)
        );
    }

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
     * @param mountPath      the storage mount path
     * @param overlay        the root overlay path
     *
     * @throws StorageException if the cache couldn't be created or
     *             removed
     */
    private void updateStorageCache(Path mountPath, Path overlay)
    throws StorageException {

        Path ident = mountPath.removePrefix(PATH_STORAGE);
        Path cachePath = Path.resolve(PATH_STORAGE_CACHE, ident);
        MemoryStorage cache = cacheStorages.get(mountPath);
        if (overlay != null && cache == null) {
            cache = new MemoryStorage("cache", true, true);
            cache.setMountInfo(cachePath, true, overlay, 0);
            updateStorageMetadata(cache, true);
            cacheStorages.put(mountPath, cache);
        } else if (overlay == null && cache != null) {
            updateStorageMetadata(cache, false);
            cacheStorages.remove(mountPath);
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
        Storage storage = getMountedStorage(path, true);
        if (storage == null) {
            String msg = "no mounted storage found matching path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        LOG.fine("unmounting " + storage);
        updateStorageCache(path, null);
        mountedStorages.remove(storage);
        updateStorageMetadata(storage, false);
        storage.destroy();
    }

    /**
     * Unmounts and destroys all mounted storages.
     */
    public synchronized void unmountAll() {
        while (mountedStorages.size() > 0) {
            Storage storage = (Storage) mountedStorages.get(-1);
            try {
                unmount(storage.path());
            } catch (Exception e) {
                String msg = "failed to unmount storage at " + storage.path();
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
        Storage storage = getMountedStorage(path, false);
        if (storage != null) {
            Metadata meta = storage.lookup(storage.localPath(path));
            if (meta == null) {
                return null;
            } else {
                return new Metadata(Path.resolve(storage.path(), meta.path()), meta);
            }
        } else {
            Metadata meta = metadata.lookup(path);
            for (Object o : mountedStorages) {
                meta = Metadata.merge(meta, lookupOverlay((Storage) o, path));
            }
            LOG.fine("metadata lookup on " + path + ": " + meta);
            return meta;
        }
    }

    /**
     * Searches for an object in an overlay storage. The object will
     * be looked up in both the cache and the actual storage. If the
     * storage doesn't have an overlay path that matches the query
     * path, null will be returned.
     *
     * @param storage        the overlay storage
     * @param path           the storage location
     *
     * @return the metadata for the object, or
     *         null if not found
     */
    private Metadata lookupOverlay(Storage storage, Path path) {
        Path overlay = storage.mountOverlayPath();
        if (overlay == null || !path.startsWith(overlay)) {
            return null;
        } else {
            MemoryStorage cache = cacheStorages.get(storage.path());
            Path queryPath = path.removePrefix(overlay);
            Metadata meta = Metadata.merge(
                (cache == null) ? null : cache.lookup(queryPath),
                storage.lookup(queryPath)
            );
            if (meta == null || overlay.isRoot()) {
                return meta;
            } else {
                return new Metadata(Path.resolve(overlay, meta.path()), meta);
            }
        }
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
        Storage storage = getMountedStorage(path, false);
        if (storage != null) {
            return storage.load(storage.localPath(path));
        } else if (path.isIndex()) {
            Index idx = (Index) metadata.load(path);
            for (Object o : mountedStorages) {
                idx = Index.merge(idx, loadOverlayIndex((Storage) o, path));
            }
            return idx;
        } else {
            Object res = metadata.load(path);
            if (res == null || res == ObjectUtils.NULL) {
                for (Object o : mountedStorages) {
                    res = loadOverlayObject((Storage) o, path);
                    if (res != null) {
                        break;
                    }
                }
            }
            return res;
        }
    }

    /**
     * Loads an index from all overlay storages. The returned index
     * will be merged from all matching overlay storages and their
     * corresponding caches.
     *
     * @param storage        the storage to load from
     * @param path           the storage location
     *
     * @return the merged index, or
     *         null if not found
     */
    private Index loadOverlayIndex(Storage storage, Path path) {
        Path overlay = storage.mountOverlayPath();
        if (overlay == null || !path.startsWith(overlay)) {
            return null;
        } else {
            Path queryPath = path.removePrefix(overlay);
            return Index.merge(
                (Index) cacheGet(storage.path(), queryPath),
                (Index) storage.load(queryPath)
            );
        }
    }

    /**
     * Loads an object from an overlay storage. The storage cache
     * will be used primarily, if it exists. If an object is found in
     * the storage that can be cached, it will be initialized and
     * cached.
     *
     * @param storage        the storage to load from
     * @param path           the storage location
     *
     * @return the data read, or
     *         null if not found
     */
    private Object loadOverlayObject(Storage storage, Path path) {
        Path overlay = storage.mountOverlayPath();
        if (overlay == null || !path.startsWith(overlay)) {
            return null;
        } else {
            Path storagePath = storage.path();
            Path queryPath = path.removePrefix(overlay);
            Object res = cacheGet(storagePath, queryPath);
            if (res instanceof StorableObject) {
                LOG.fine("loaded cached object " + queryPath + " from " + storagePath);
                return res;
            }
            res = storage.load(queryPath);
            if (res instanceof Dict && cacheStorages.containsKey(storagePath)) {
                res = initObject(queryPath.toIdent(1), (Dict) res);
                cacheAdd(storagePath, queryPath, res);
            }
            if (res != null) {
                LOG.fine("loaded " + queryPath + " from " + storagePath + ": " + res);
            }
            return res;
        }
    }

    /**
     * Initializes a loaded object with the corresponding object type.
     * If no type is registered, the dictionary is returned as-is.
     *
     * @param id             the object id
     * @param dict           the dictionary data
     *
     * @return the StorableObject instance created, or
     *         the input dictionary if no type matched
     */
    private Object initObject(String id, Dict dict) {
        if (!dict.containsKey(KEY_ID)) {
            Dict copy = new Dict();
            copy.set(KEY_ID, id);
            copy.setAll(dict);
            dict = copy;
        }
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
            Path storageId = storage.path().removePrefix(PATH_STORAGE_CACHE);
            Path storagePath = Path.resolve(PATH_STORAGE, storageId);
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
            Path localPath = storage.localPath(path);
            if (caching) {
                cacheAdd(storage.path(), localPath, data);
            }
            storage.store(localPath, data);
        } else {
            boolean stored = false;
            for (Object o : mountedStorages) {
                storage = (Storage) o;
                Path overlay = storage.mountOverlayPath();
                if (overlay != null && path.startsWith(overlay)) {
                    Path localPath = path.removePrefix(overlay);
                    if (storage.isReadWrite() && !stored) {
                        if (caching) {
                            cacheAdd(storage.path(), localPath, data);
                        }
                        storage.store(localPath, data);
                        stored = true;
                    } else if (caching) {
                        cacheRemove(storage.path(), localPath);
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
            Path storageId = storage.path().removePrefix(PATH_STORAGE_CACHE);
            Path storagePath = Path.resolve(PATH_STORAGE, storageId);
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
            Path localPath = storage.localPath(path);
            if (caching) {
                cacheRemove(storage.path(), localPath);
            }
            storage.remove(localPath);
        } else {
            for (Object o : mountedStorages) {
                storage = (Storage) o;
                Path overlay = storage.mountOverlayPath();
                if (overlay != null && path.startsWith(overlay)) {
                    Path localPath = path.removePrefix(overlay);
                    if (caching) {
                        cacheRemove(storage.path(), localPath);
                    }
                    if (storage.isReadWrite()) {
                        storage.remove(localPath);
                    }
                }
            }
        }
    }

    /**
     * Retrieves an object from the cache. If the object is a storable
     * object, it will also be activated.
     *
     * @param storagePath    the cached storage path
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
     * @param storagePath    the cached storage path
     * @param path           the object location
     * @param data           the object to store
     */
    private void cacheAdd(Path storagePath, Path path, Object data) {
        MemoryStorage cache = cacheStorages.get(storagePath);
        if (cache != null) {
            String debugPrefix = "cache " + cache.path() + ": ";
            path = objectPath(path);
            Object old = cache.load(path);
            if (data instanceof StorableObject) {
                StorableObject storable = (StorableObject) data;
                LOG.fine(debugPrefix + "passivating object " + path);
                storable.passivate();
                if (data != old) {
                    try {
                        LOG.fine(debugPrefix + "storing object " + path);
                        cache.store(path, data);
                    } catch (StorageException e) {
                        LOG.log(Level.WARNING, "failed to cache object", e);
                    }
                    cacheDestroyObject(cache, path, old);
                }
            } else if (old != null) {
                cacheRemoveDestroy(cache, path, old);
            } else {
                LOG.fine(debugPrefix + "object " + path + " not cacheable");
            }
        }
    }

    /**
     * Removes one or more objects from the storage cache. All the
     * objects removed will be destroyed, but not persisted if
     * modified.
     *
     * @param storagePath    the cached storage path
     * @param path           the path to remove
     */
    private void cacheRemove(Path storagePath, Path path) {
        MemoryStorage cache = cacheStorages.get(storagePath);
        if (cache != null) {
            path = objectPath(path);
            LOG.fine("removing " + path + " from cache " + cache.path());
            cacheRemove(cache, path, false, true);
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
                if (force || !storable.isActive()) {
                    cacheRemoveDestroy(cache, path, obj);
                } else {
                    LOG.fine(debugPrefix + "passivating cached object " + path);
                    storable.passivate();
                }
            } else {
                cacheRemoveDestroy(cache, path, obj);
            }
        }
    }

    /**
     * Removes and destroys an object from a storage cache.
     *
     * @param cache          the storage cache to modify
     * @param path           the object path to remove
     * @param data           the storable object to destroy
     */
    private void cacheRemoveDestroy(MemoryStorage cache, Path path, Object data) {
        String debugPrefix = "cache " + cache.path() + ": ";
        try {
            LOG.fine(debugPrefix + "removing object " + path);
            cache.remove(path);
        } catch (StorageException e) {
            LOG.log(Level.WARNING, "failed to remove cached data", e);
        }
        cacheDestroyObject(cache, path, data);
    }

    /**
     * Destroys a previously removed storable object.
     *
     * @param cache          the storage cache (for logging)
     * @param path           the object path (for logging)
     * @param data           the storable object to destroy
     */
    private void cacheDestroyObject(MemoryStorage cache, Path path, Object data) {
        if (data instanceof StorableObject) {
            String debugPrefix = "cache " + cache.path() + ": ";
            StorableObject storable = (StorableObject) data;
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
