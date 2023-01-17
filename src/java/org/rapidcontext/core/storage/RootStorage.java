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

import java.lang.reflect.Constructor;
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
public class RootStorage extends MemoryStorage {

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
     * The number of seconds between each run of the object cache
     * cleaner job.
     */
    private static final int PASSIVATE_INTERVAL_SECS = 30;

    /**
     * The sorted array of mounted storages. This array is sorted
     * every time a mount point is added or modified.
     */
    private Array mountedStorages = new Array();

    /**
     * The caches for (some) mounted storages.
     */
    private Caches caches = new Caches();

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
        super("/", readWrite, true);
        dict.set(KEY_TYPE, "storage/root");
        dict.set("storages", mountedStorages);
        Task cacheCleaner = new Task("storage cache cleaner") {
            public void execute() {
                cacheClean(false);
            }
        };
        long delay = PASSIVATE_INTERVAL_SECS * 1000L;
        Scheduler.schedule(cacheCleaner, delay, delay);
    }

    /**
     * Returns the mounted storage with the exact storage path.
     *
     * @param path           the storage path
     *
     * @return the storage found, or null if not found
     */
    private Storage getMountedStorage(Path path) {
        for (Object o : mountedStorages) {
            Storage storage = (Storage) o;
            if (storage.path().equals(path)) {
                return storage;
            }
        }
        return null;
    }

    /**
     * Mounts a storage to a unique path. The path may not collide
     * with a previously mounted storage, such that it would hide or
     * be hidden by the other storage. Overlapping parent indices
     * will be merged automatically. The storage will be added as
     * read-only and must be remounted to enable writes, root
     * overlays or caches.
     *
     * @param storage        the storage to mount
     * @param path           the storage mount path
     *
     * @throws StorageException if the storage couldn't be mounted
     */
    public synchronized void mount(Storage storage, Path path)
    throws StorageException {

        if (!path.isIndex()) {
            String msg = "cannot mount storage to a non-index path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        } else if (!path.startsWith(PATH_STORAGE)) {
            String msg = "cannot mount storage to a non-storage path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        } else if (super.lookup(path) != null) {
            String msg = "storage mount path conflicts with another mount: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        LOG.fine("mounting " + storage);
        storage.setMountInfo(path, false, null, 0);
        mountedStorages.add(storage);
        mountedStorages.sort();
        metadataMount(path);
    }

    /**
     * Remounts an already mounted storage to enable/disable read-write,
     * caching and root overlay options. The storage or its mount path
     * are not modified, only the mount options.
     *
     * @param path           the storage mount path
     * @param readWrite      the read write flag
     * @param cache          the storage cache path, or null
     * @param overlay        the root overlay path
     * @param prio           the root overlay search priority (higher numbers
     *                       are searched before lower numbers)
     *
     * @throws StorageException if the storage couldn't be remounted
     */
    public synchronized void remount(Path path,
                                     boolean readWrite,
                                     Path cache,
                                     Path overlay,
                                     int prio)
    throws StorageException {

        Storage storage = getMountedStorage(path);
        if (storage == null) {
            String msg = "no mounted storage found matching path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        LOG.fine("remounting " + storage);
        metadataUnmount(storage.mountOverlayPath());
        storage.setMountInfo(storage.path(), readWrite, overlay, prio);
        mountedStorages.sort();
        metadataMount(storage.mountOverlayPath());
        cacheRemount(storage.path(), cache);
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
        Storage storage = getMountedStorage(path);
        if (storage == null) {
            String msg = "no mounted storage found matching path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        LOG.fine("unmounting " + storage);
        mountedStorages.remove(storage);
        metadataUnmount(storage.path());
        metadataUnmount(storage.mountOverlayPath());
        storage.setMountInfo(storage.path(), storage.isReadWrite(), null, 0);
        cacheRemount(storage.path(), null);
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
     * Creates an empty storage info object at the specified path. This is
     * needed to ensure that intermediate paths are available in indices.
     *
     * @param path           the storage (mount point or overlay) path
     *
     * @throws StorageException if the metadata couldn't be updated
     */
    private void metadataMount(Path path) throws StorageException {
        if (path != null && !path.isRoot()) {
            super.store(Path.resolve(path, PATH_STORAGEINFO), ObjectUtils.NULL);
        }
    }

    /**
     * Removes a previously created storage info object at a path.
     *
     * @param path           the storage (mount point or overlay) path
     *
     * @throws StorageException if the metadata couldn't be updated
     */
    private void metadataUnmount(Path path) throws StorageException {
        if (path != null && !path.isRoot()) {
            super.remove(Path.resolve(path, PATH_STORAGEINFO));
        }
    }

    /**
     * Creates or removes a cache memory storage for a storage.
     *
     * @param storagePath    the origin storage path
     * @param cachePath      the cache storage path, or null to remove
     *
     * @throws StorageException if the cache couldn't be updated
     */
    private void cacheRemount(Path storagePath, Path cachePath)
    throws StorageException {
        if (cachePath != null && !caches.origins().contains(storagePath)) {
            caches.mount(storagePath, cachePath);
            metadataMount(cachePath);
        } else if (cachePath == null && caches.origins().contains(storagePath)) {
            metadataUnmount(cachePath);
            caches.unmount(storagePath);
        }
    }

    /**
     * Searches for an object at the specified location and returns
     * metadata about the object if found. The path may locate either
     * an index or a specific object.
     *
     * @param path           the storage location
     *
     * @return the metadata for the object, or null if not found
     */
    public Metadata lookup(Path path) {
        if (path.startsWith(PATH_STORAGE)) {
            Metadata meta = caches.lookup(null, path);
            for (Object o : mountedStorages) {
                Storage storage = (Storage) o;
                if (meta == null && path.startsWith(storage.path())) {
                    meta = storage.lookup(storage.localPath(path));
                }
            }
            return (meta == null) ? super.lookup(path) : new Metadata(path, meta);
        } else {
            boolean managed = path.isIndex() || path.equals(PATH_STORAGEINFO);
            Metadata meta = managed ? super.lookup(path) : null;
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
            Path queryPath = path.removePrefix(overlay);
            Metadata meta = Metadata.merge(
                caches.lookup(storage.path(), queryPath),
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
        if (path.startsWith(PATH_STORAGE)) {
            Object res = caches.load(null, path);
            if (res != null) {
                return res;
            }
            for (Object o : mountedStorages) {
                Storage storage = (Storage) o;
                if (path.startsWith(storage.path())) {
                    return storage.load(storage.localPath(path));
                }
            }
            return super.load(path);
        } else if (path.equals(PATH_STORAGEINFO)) {
            return StorableObject.sterilize(super.load(path), true, true, true);
        } else if (path.isIndex()) {
            Index idx = (Index) super.load(path);
            for (Object o : mountedStorages) {
                idx = Index.merge(idx, loadOverlayIndex((Storage) o, path));
            }
            return idx;
        } else {
            for (Object o : mountedStorages) {
                Object res = loadOverlayObject((Storage) o, path);
                if (res != null) {
                    return res;
                }
            }
            return null;
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
            Index cached = (Index) caches.load(storage.path(), queryPath);
            Index stored = (Index) storage.load(queryPath);
            if (!isBinaryPath(path) && stored != null) {
                stored = new Index(stored, true);
            }
            return Index.merge(cached, stored);
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
            Object res = caches.load(storagePath, queryPath);
            if (res instanceof StorableObject) {
                LOG.fine("loaded cached object " + queryPath + " from " + storagePath);
                return res;
            }
            res = storage.load(queryPath);
            if (res instanceof Dict && caches.origins().contains(storagePath)) {
                res = initObject(queryPath.toIdent(1), (Dict) res);
                caches.store(storagePath, queryPath, res);
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
        if (!dict.containsKey(KEY_ID) && !id.isEmpty()) {
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
        if (path.startsWith(PATH_STORAGE)) {
            if (caches.store(null, path, data)) {
                return;
            }
            for (Object o : mountedStorages) {
                Storage storage = (Storage) o;
                if (path.startsWith(storage.path())) {
                    Path localPath = storage.localPath(path);
                    caches.store(storage.path(), localPath, data);
                    storage.store(localPath, data);
                    return;
                }
            }
            throw new StorageException("no mounted storage found for " + path);
        } else {
            boolean stored = false;
            for (Object o : mountedStorages) {
                Storage storage = (Storage) o;
                Path overlay = storage.mountOverlayPath();
                if (overlay != null && path.startsWith(overlay)) {
                    Path localPath = path.removePrefix(overlay);
                    if (storage.isReadWrite() && !stored) {
                        caches.store(storage.path(), localPath, data);
                        storage.store(localPath, data);
                        stored = true;
                    } else {
                        caches.remove(storage.path(), localPath, true);
                    }
                }
            }
            if (stored) {
                LOG.fine("stored " + path);
            } else {
                throw new StorageException("no writable storage found for " + path);
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
        if (path.startsWith(PATH_STORAGE)) {
            if (caches.remove(null, path, true)) {
                return;
            }
            for (Object o : mountedStorages) {
                Storage storage = (Storage) o;
                if (path.startsWith(storage.path())) {
                    Path localPath = storage.localPath(path);
                    caches.remove(storage.path(), localPath, true);
                    storage.remove(localPath);
                    return;
                }
            }
            throw new StorageException("no mounted storage found for " + path);
        } else {
            for (Object o : mountedStorages) {
                Storage storage = (Storage) o;
                Path overlay = storage.mountOverlayPath();
                if (overlay != null && path.startsWith(overlay)) {
                    Path localPath = path.removePrefix(overlay);
                    caches.remove(storage.path(), localPath, true);
                    if (storage.isReadWrite()) {
                        storage.remove(localPath);
                    }
                }
            }
            LOG.fine("removed " + path);
        }
    }

    /**
     * Cleans up (inactive) cached objects. Cached objects will be stored (if
     * modified), passivated and finally evicted from cache if no longer
     * active (or if force flag set). This method is called regularly from a
     * background job.
     *
     * @param force          the force removal flag
     */
    public void cacheClean(boolean force) {
        for (Path storagePath : caches.origins()) {
            for (Path path : caches.listModified(storagePath, Path.ROOT)) {
                try {
                    LOG.fine("cache " + storagePath + ": persisting modified " + path);
                    store(path, caches.load(storagePath, path));
                } catch (StorageException e) {
                    LOG.log(Level.WARNING, "failed to persist cached object", e);
                }
            }
            caches.remove(storagePath, Path.ROOT, force);
        }
    }
}
