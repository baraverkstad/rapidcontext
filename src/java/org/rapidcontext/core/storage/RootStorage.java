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

package org.rapidcontext.core.storage;

import java.util.Date;
import java.util.logging.Logger;

import org.rapidcontext.core.data.Array;

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
 *       storage. If a matching type handler or class can be located,
 *       the corresponding object will be created, initialized and
 *       cached for future references.
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
     * The system time of the last mount or remount operation.
     */
    private static long lastMountTime = 0L;

    /**
     * The meta-data storage for mount points and parent indices.
     * The mounted storages will be added to this storage under
     * their corresponding mount path (appended to form an object
     * path instead of an index path).
     */
    private MemoryStorage metadata = new MemoryStorage(true);

    /**
     * The sorted array of mounted storages. This array is sorted
     * every time a mount point is added or modified.
     */
    private Array mountedStorages = new Array();

    /**
     * Creates a new root storage.
     *
     * @param readWrite      the read write flag
     */
    public RootStorage(boolean readWrite) {
        super("root", readWrite);
        dict.set("storages", mountedStorages);
        try {
            metadata.store(PATH_STORAGEINFO, dict);
        } catch (StorageException e) {
            LOG.severe("error while initializing virtual storage: " +
                       e.getMessage());
        }
    }

    /**
     * Returns the storage at a specific storage location. If the
     * exact match flag is set, the path must exactly match the mount
     * point of a storage. If exact matching is not required, the
     * parent storage for the path will be returned.
     *
     * @param path           the storage location
     * @param exact          the exact match flag
     *
     * @return the storage found, or
     *         null if not found
     */
    private Storage getMountedStorage(Path path, boolean exact) {
        if (exact) {
            return (Storage) metadata.load(path.child("storage", false));
        } else {
            for (int i = 0; i < mountedStorages.size(); i++) {
                Storage storage = (Storage) mountedStorages.get(i);
                if (path.startsWith(storage.path())) {
                    return storage;
                }
            }
            return null;
        }
    }

    /**
     * Sets or removes a storage at a specific storage location.
     *
     * @param path           the storage mount path
     * @param storage        the storage to add, or null to remove
     *
     * @throws StorageException if the data couldn't be written
     */
    private void setMountedStorage(Path path, Storage storage)
    throws StorageException {

        path = path.child("storage", false);
        if (storage == null) {
            metadata.remove(path);
        } else {
            metadata.store(path, storage);
        }
    }

    /**
     * Updates the mount information in a storage object.
     *
     * @param storage        the storage to update
     * @param readWrite      the read write flag
     * @param overlay        the root overlay flag
     * @param prio           the root overlay search priority (higher numbers
     *                       are searched before lower numbers)
     */
    private void updateMountInfo(Storage storage,
                                 boolean readWrite,
                                 boolean overlay,
                                 int prio) {

        lastMountTime = Math.max(System.currentTimeMillis(), lastMountTime + 1);
        storage.dict.set(KEY_MOUNT_TIME, new Date(lastMountTime));
        storage.dict.setBoolean(KEY_READWRITE, readWrite);
        storage.dict.setBoolean(KEY_MOUNT_OVERLAY, overlay);
        storage.dict.setInt(KEY_MOUNT_OVERLAY_PRIO, overlay ? prio : -1);
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
     * @param overlay        the root overlay flag
     * @param prio           the root overlay search priority (higher numbers
     *                       are searched before lower numbers)
     *
     * @throws StorageException if the storage couldn't be mounted
     */
    public void mount(Storage storage,
                      Path path, 
                      boolean readWrite,
                      boolean overlay,
                      int prio)
    throws StorageException {

        String  msg;

        if (!path.isIndex()) {
            msg = "cannot mount storage to a non-index path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        } else if (metadata.lookup(path) != null) {
            msg = "storage mount path conflicts with another mount: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        storage.dict.set(KEY_MOUNT_PATH, path);
        updateMountInfo(storage, readWrite, overlay, prio);
        setMountedStorage(path, storage);
        mountedStorages.add(storage);
        mountedStorages.sort();
    }

    /**
     * Remounts a storage for a unique path. The path or the storage
     * are not modified, but only the mounting options.
     *
     * @param path           the mount path
     * @param readWrite      the read write flag
     * @param overlay        the root overlay flag
     * @param prio           the root overlay search priority (higher numbers
     *                       are searched before lower numbers)
     *
     * @throws StorageException if the storage couldn't be remounted
     */
    public void remount(Path path, boolean readWrite, boolean overlay, int prio)
    throws StorageException {

        Storage  storage = getMountedStorage(path, true);
        String   msg;

        if (storage == null) {
            msg = "no mounted storage found matching path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        updateMountInfo(storage, readWrite, overlay, prio);
        mountedStorages.sort();
    }

    /**
     * Unmounts a storage from the specified path. The path must have
     * previously been used to mount a storage.
     *
     * @param path           the mount path
     *
     * @throws StorageException if the storage couldn't be unmounted
     */
    public void unmount(Path path) throws StorageException {
        Storage  storage = getMountedStorage(path, true);
        String   msg;

        if (storage == null) {
            msg = "no mounted storage found matching path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        mountedStorages.remove(mountedStorages.indexOf(storage));
        setMountedStorage(path, null);
        storage.dict.set(KEY_MOUNT_PATH, Path.ROOT);
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
        Metadata  idx = null;

        if (storage != null) {
            return storage.lookup(storage.localPath(path));
        } else {
            meta = metadata.lookup(path);
            if (meta != null && meta.isIndex()) {
                idx = meta;
            } else if (meta != null) {
                return meta;
            }
            for (int i = 0; i < mountedStorages.size(); i++) {
                storage = (Storage) mountedStorages.get(i);
                if (storage.mountOverlay()) {
                    meta = storage.lookup(path);
                    if (meta != null && meta.isIndex()) {
                        idx = new Metadata(Metadata.CATEGORY_INDEX,
                                           Index.class,
                                           path,
                                           path(),
                                           Metadata.lastModified(idx, meta));
                    } else if (meta != null) {
                        return meta;
                    }
                }
            }
        }
        return idx;
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
            return storage.load(storage.localPath(path));
        } else {
            res = metadata.load(path);
            if (res instanceof Index) {
                idx = (Index) res;
            } else if (res != null) {
                return res;
            }
            for (int i = 0; i < mountedStorages.size(); i++) {
                storage = (Storage) mountedStorages.get(i);
                if (storage.mountOverlay()) {
                    res = storage.load(path);
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
    public void store(Path path, Object data) throws StorageException {
        Storage  storage = getMountedStorage(path, false);

        if (storage != null) {
            storage.store(storage.localPath(path), data);
        } else {
            for (int i = 0; i < mountedStorages.size(); i++) {
                storage = (Storage) mountedStorages.get(i);
                if (storage.isReadWrite() && storage.mountOverlay()) {
                    storage.store(path, data);
                    return;
                }
            }
            throw new StorageException("no writable storage found for " + path);
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
    public void remove(Path path) throws StorageException {
        Storage  storage = getMountedStorage(path, false);

        if (storage != null) {
            storage.remove(storage.localPath(path));
        } else {
            for (int i = 0; i < mountedStorages.size(); i++) {
                storage = (Storage) mountedStorages.get(i);
                if (storage.isReadWrite() && storage.mountOverlay()) {
                    storage.remove(path);
                }
            }
        }
    }
}
