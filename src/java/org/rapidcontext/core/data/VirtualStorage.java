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

import java.util.Comparator;
import java.util.Date;
import java.util.logging.Logger;

/**
 * A virtual storage handler that provides a unified view of other
 * storages. The sub-storages are mounted to specific storage paths
 * and may also be merged together to form a unified storage with
 * data and files from all storages mixed (in a prioritized order).
 * The unified storage tree provides an overlay of all storages for
 * reading, but only a single storage will be used for writing. Any
 * mounted storage can be accessed directly from its mounted path,
 * however.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class VirtualStorage extends Storage {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(VirtualStorage.class.getName());

    /**
     * The storage information path.
     */
    public static final Path PATH_STORAGEINFO = new Path("/storageinfo");

    /**
     * The dictionary key for the overlay flag.
     */
    private static final String KEY_OVERLAY = "overlay";

    /**
     * The dictionary key for the overlay priority.
     */
    private static final String KEY_OVERLAY_PRIO = "prio";

    /**
     * The dictionary key for the mount timestamp.
     */
    private static final String KEY_MOUNT_TIME = "mountTime";

    /**
     * The system time of the last mount or remount operation.
     */
    private static long lastMountTime = 0L;

    /**
     * The meta-data storage for mount points and parent indices.
     * The mount point objects will be added to this storage under
     * their corresponding path (slightly modified to form an object
     * path instead of an index path).
     */
    private MemoryStorage metaStorage = new MemoryStorage(Path.ROOT, true);

    /**
     * The sorted array of mounted storages. This array is sorted
     * every time a mount point is added or modified.
     */
    private Array storages = new Array();

    /**
     * Creates a new overlay storage.
     *
     * @param path           the base storage path
     * @param readWrite      the read write flag
     */
    public VirtualStorage(Path path, boolean readWrite) {
        super("virtual", path, readWrite);
        dict.set("storages", storages);
        try {
            metaStorage.store(PATH_STORAGEINFO, dict);
        } catch (StorageException e) {
            LOG.severe("error while initializing virtual storage: " +
                       e.getMessage());
        }
    }

    /**
     * Returns the storage at a specific storage location. If the
     * path does not exactly match an existing mount point, null will
     * be returned.
     *
     * @param path           the storage mount path
     *
     * @return the storage found, or
     *         null if not found
     */
    private Storage getMountedStorage(Path path) {
        return (Storage) metaStorage.load(path.child("storageinfo", false));
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

        path = path.child("storageinfo", false);
        if (storage == null) {
            metaStorage.remove(path);
        } else {
            metaStorage.store(path, storage);
        }
    }

    /**
     * Returns the parent storage for a storage location. All mounted
     * storages will be searched in order to find a matching parent
     * (if one exists).
     *
     * @param path           the storage location
     *
     * @return the parent storage found, or
     *         null if not found
     */
    private Storage getParentStorage(Path path) {
        for (int i = 0; i < storages.size(); i++) {
            Storage storage = (Storage) storages.get(i);
            if (path.startsWith(storage.path())) {
                return storage;
            }
        }
        return null;
    }

    /**
     * Checks if the specified storage has the root overlay flag set.
     *
     * @param storage        the storage to check
     *
     * @return true if the root overlay flag is set, or
     *         false otherwise
     */
    private boolean isOverlay(Storage storage) {
        return storage.dict.getBoolean(KEY_OVERLAY, false);
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
        storage.dict.setBoolean(KEY_READWRITE, readWrite);
        storage.dict.setBoolean(KEY_OVERLAY, overlay);
        storage.dict.setInt(KEY_OVERLAY_PRIO, overlay ? prio : -1);
        storage.dict.set(KEY_MOUNT_TIME, new Date(lastMountTime));
    }

    /**
     * Mounts a storage to a unique path. The path may not collide
     * with a previously mounted storage, such that it would hide or
     * be hidden by the other storage. Overlapping parent indices
     * will be merged automatically. In addition to adding the
     * storage to the specified path, it may also be overlaid
     * directly to the root path.
     *
     * @param storage        the storage to mount
     * @param readWrite      the read write flag
     * @param overlay        the root overlay flag
     * @param prio           the root overlay search priority (higher numbers
     *                       are searched before lower numbers)
     *
     * @throws StorageException if the storage couldn't be mounted
     */
    public void mount(Storage storage,
                      boolean readWrite,
                      boolean overlay,
                      int prio)
    throws StorageException {

        String  msg;

        if (!storage.path().isIndex()) {
            msg = "cannot mount storage to a non-index path: " + storage.path();
            LOG.warning(msg);
            throw new StorageException(msg);
        } else if (metaStorage.lookup(storage.path()) != null) {
            msg = "storage mount path conflicts with another mount: " +
                  storage.path();
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        updateMountInfo(storage, readWrite, overlay, prio);
        setMountedStorage(storage.path(), storage);
        storages.add(storage);
        storages.sort(StorageComparator.INSTANCE);
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

        Storage  storage = getMountedStorage(path);
        String   msg;

        if (storage == null) {
            msg = "no mounted storage found matching path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        updateMountInfo(storage, readWrite, overlay, prio);
        storages.sort(StorageComparator.INSTANCE);
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
        Storage  storage = getMountedStorage(path);
        String   msg;

        if (storage == null) {
            msg = "no mounted storage found matching path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        storages.remove(storages.indexOf(storage));
        setMountedStorage(path, null);
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
        Storage   storage = getParentStorage(path);
        Metadata  meta = null;
        Metadata  idx = null;

        if (storage != null) {
            return storage.lookup(storage.localPath(path));
        } else {
            meta = metaStorage.lookup(path);
            if (meta != null && meta.isIndex()) {
                idx = meta;
            } else if (meta != null) {
                return meta;
            }
            for (int i = 0; i < storages.size(); i++) {
                storage = (Storage) storages.get(i);
                if (isOverlay(storage)) {
                    meta = storage.lookup(path);
                    if (meta != null && meta.isIndex()) {
                        idx = new Metadata(Metadata.CATEGORY_INDEX,
                                           Index.class,
                                           path().descendant(path),
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
        Storage  storage = getParentStorage(path);
        Object   res;
        Index    idx = null;

        if (storage != null) {
            return storage.load(storage.localPath(path));
        } else {
            res = metaStorage.load(path);
            if (res instanceof Index) {
                idx = (Index) res;
            } else if (res != null) {
                return res;
            }
            for (int i = 0; i < storages.size(); i++) {
                storage = (Storage) storages.get(i);
                if (isOverlay(storage)) {
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
        Storage  storage = getParentStorage(path);
        String   msg;

        if (storage != null) {
            if (!storage.isReadWrite()) {
                msg = "cannot write to read-only storage at " + storage.path();
                LOG.warning(msg);
                throw new StorageException(msg);
            }
            storage.store(storage.localPath(path), data);
        } else {
            for (int i = 0; i < storages.size(); i++) {
                storage = (Storage) storages.get(i);
                if (isOverlay(storage) && storage.isReadWrite()) {
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
        Storage  storage = getParentStorage(path);
        String   msg;

        if (storage != null) {
            if (!storage.isReadWrite()) {
                msg = "cannot remove from read-only storage at " + storage.path();
                LOG.warning(msg);
                throw new StorageException(msg);
            }
            storage.remove(storage.localPath(path));
        } else {
            for (int i = 0; i < storages.size(); i++) {
                storage = (Storage) storages.get(i);
                if (isOverlay(storage) && storage.isReadWrite()) {
                    storage.remove(path);
                }
            }
        }
    }


    /**
     * A mounted storage comparator. The comparison is based on the
     * overlay priority order and the mount time.
     */
    private static class StorageComparator implements Comparator {

        /**
         * The comparator instance.
         */
        public static final StorageComparator INSTANCE = new StorageComparator();

        /**
         * Compares two storages with one another.
         *
         * @param o1             the first object
         * @param o2             the second object
         *
         * @return a negative integer, zero, or a positive integer as
         *         the first argument is less than, equal to, or
         *         greater than the second
         *
         * @throws ClassCastException if the values were not comparable
         */
        public int compare(Object o1, Object o2) {
            Storage  s1 = (Storage) o1;
            Storage  s2 = (Storage) o2;
            int      cmp1 = prio(s1) - prio(s2);
            int      cmp2 = mountTime(s2).compareTo(mountTime(s2));

            return (cmp1 != 0) ? -cmp1 : cmp2;
        }

        /**
         * Returns the overlay priority for a specified storage.
         *
         * @param storage        the storage object
         *
         * @return the storage overlay priority, or
         *         -1 if no overlay is enabled
         */
        private int prio(Storage storage) {
            return storage.dict.getInt(KEY_OVERLAY_PRIO, -1);
        }

        /**
         * Returns the last mount time for a specified storage.
         *
         * @param storage        the storage object
         *
         * @return the last mount time
         */
        private Date mountTime(Storage storage) {
            return (Date) storage.dict.get(KEY_MOUNT_TIME);
        }
    }
}
