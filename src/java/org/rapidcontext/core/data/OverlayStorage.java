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

import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * An overlay storage providing a unified view of other storages. The
 * sub-storages are mounted to specific paths, but may also be
 * overlaid over the root path. The overlay mechanism supports
 * multiple writable storages and the unified root path can be
 * controlled by setting storage priorities.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class OverlayStorage implements Storage {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(OverlayStorage.class.getName());

    /**
     * The sorted list of mount points. This list should be sorted
     * whenever the mount points are modified.
     */
    private ArrayList mountpoints = new ArrayList();

    /**
     * The memory storage containing meta-data for the mount points.
     * The mount points will be added to this storage with their
     * mount path slightly adapted to form a valid object path.
     */
    private MemoryStorage pathInfo = new MemoryStorage();

    // TODO: retrieve individual storage
    // TODO: retrieve mount points

    /**
     * Mounts a storage to a unique path. The path may not collide
     * with a previously mounted storage, such that it would hide or
     * be hidden by the other storage. Overlapping parent indices
     * will be merged automatically. In addition to adding the
     * storage to the specified path, it may also be overlaid
     * directly to the root path.
     *
     * @param storage        the storage to mount
     * @param path           the mount path (must be an index)
     * @param readWrite      the read-write flag, use false for read-only
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

        Path        storagePath = path.child("storage", false);
        MountPoint  mount;
        String      msg;

        if (!path.isIndex()) {
            msg = "cannot mount storage to a non-index path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        } else if (pathInfo.lookup(storagePath) != null) {
            msg = "storage mount path conflicts with another mount: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        mount = new MountPoint(storage, path, readWrite, overlay ? prio : -1);
        pathInfo.store(storagePath, mount);
        mountpoints.add(mount);
        Collections.sort(mountpoints);
    }

    /**
     * Remounts a storage for a unique path. The path or the storage
     * are not modified, but only the mounting options.
     *
     * @param path           the mount path
     * @param readWrite      the read-write flag, use false for read-only
     * @param overlay        the root overlay flag
     * @param prio           the root overlay search priority (higher numbers
     *                       are searched before lower numbers)
     *
     * @throws StorageException if the storage couldn't be remounted
     */
    public void remount(Path path, boolean readWrite, boolean overlay, int prio)
    throws StorageException {

        Path        storagePath = path.child("storage", false);
        MountPoint  mount = (MountPoint) pathInfo.load(storagePath);
        String      msg;

        if (mount == null) {
            msg = "no mounted storage found matching path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        mount.update(readWrite, overlay ? prio : -1);
        Collections.sort(mountpoints);
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
        Path        storagePath = path.child("storage", false);
        MountPoint  mount = (MountPoint) pathInfo.load(storagePath);
        String      msg;

        if (mount == null) {
            msg = "no mounted storage found matching path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        mountpoints.remove(mount);
        pathInfo.remove(storagePath);
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
     *
     * @throws StorageException if the storage couldn't be accessed
     */
    public Dict lookup(Path path) throws StorageException {
        MountPoint  mount = findMountPoint(path);
        Dict        meta = null;
        Dict        idx = null;

        if (mount != null) {
            return mount.storage.lookup(path.subPath(mount.path.length()));
        } else {
            idx = pathInfo.lookup(path);
            for (int i = 0; i < mountpoints.size(); i++) {
                mount = (MountPoint) mountpoints.get(i);
                if (mount.isOverlay()) {
                    meta = mount.storage.lookup(path);
                    if (meta != null) {
                        if (meta.getString(KEY_TYPE, "").equals(TYPE_INDEX)) {
                            if (idx != null) {
                                long mod1 = ((Long) idx.get(KEY_MODIFIED)).longValue();
                                long mod2 = ((Long) meta.get(KEY_MODIFIED)).longValue();
                                long mod3 = Math.max(mod1, mod2);
                                meta = meta.copy();
                                meta.set(KEY_MODIFIED, new Long(mod3));
                            }
                            idx = meta;
                        } else {
                            meta = meta.copy();
                            meta.set("storage", mount.path);
                            return meta;
                        }
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
     *
     * @throws StorageException if the data couldn't be read
     */
    public Object load(Path path) throws StorageException {
        MountPoint  mount = findMountPoint(path);
        Object      res;
        Index       idx = null;

        if (mount != null) {
            return mount.storage.load(path.subPath(mount.path.length()));
        } else {
            idx = (Index) pathInfo.load(path);
            for (int i = 0; i < mountpoints.size(); i++) {
                mount = (MountPoint) mountpoints.get(i);
                if (mount.isOverlay()) {
                    res = mount.storage.load(path);
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
        MountPoint  mount = findMountPoint(path);
        String      msg;

        if (mount != null) {
            if (!mount.readWrite) {
                msg = "cannot write to read-only storage at " + mount.path;
                LOG.warning(msg);
                throw new StorageException(msg);
            }
            mount.storage.store(path.subPath(mount.path.length()), data);
        } else {
            for (int i = 0; i < mountpoints.size(); i++) {
                mount = (MountPoint) mountpoints.get(i);
                if (mount.isOverlay() && mount.readWrite) {
                    mount.storage.store(path, data);
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
        MountPoint  mount = findMountPoint(path);

        if (mount != null) {
            mount.storage.remove(path.subPath(mount.path.length()));
        } else {
            for (int i = 0; i < mountpoints.size(); i++) {
                mount = (MountPoint) mountpoints.get(i);
                if (mount.isOverlay() && mount.readWrite) {
                    mount.storage.remove(path);
                }
            }
        }
    }

    /**
     * Searches for a partially matching mount point. If the path
     * starts with a mounted path, that mount point will be returned.
     *
     * @param path           the storage location
     *
     * @return the mount point found, or
     *         null if not found
     */
    private MountPoint findMountPoint(Path path) {
        for (int i = 0; i < mountpoints.size(); i++) {
            MountPoint mount = (MountPoint) mountpoints.get(i);
            if (path.startsWith(mount.path)) {
                return mount;
            }
        }
        return null;
    }


    /**
     * A storage mount point. This class encapsulates all the
     * relevant meta-data and allows sorting the mount points in
     * overlay priority order.
     */
    private static class MountPoint implements Comparable {

        /**
         * The system time of the last mount or remount operation.
         */
        private static long lastMountTime = 0L;

        /**
         * The storage mounted.
         */
        public Storage storage;

        /**
         * The storage location.
         */
        public Path path;

        /**
         * The read-write flag.
         */
        public boolean readWrite;

        /**
         * The overlay priority. Higher values have precedence over
         * lower values, meaning that those storages will be
         * consulted before others.
         */
        public int overlayPrio;

        /**
         * The system time of the mount or remount operation.
         */
        public long mountTime;

        /**
         * Creates a new mount point.
         *
         * @param storage        the storage mounted
         * @param path           the storage location
         * @param readWrite      the read-write flag
         * @param overlayPrio    the overlay priority, negative for
         *                       no overlay
         */
        public MountPoint(Storage storage,
                          Path path,
                          boolean readWrite,
                          int overlayPrio) {

            this.storage = storage;
            this.path = path;
            update(readWrite, overlayPrio);
        }

        /**
         * Checks if this object is equal to another.
         *
         * @param obj            the object to compare with
         *
         * @return true if the objects are equal, or
         *         false otherwise
         */
        public boolean equals(Object obj) {
            return obj instanceof MountPoint && compareTo(obj) == 0;
        }

        /**
         * Returns a hash code for this object.
         *
         * @return a hash code for this object
         */
        public int hashCode() {
            // Correct, since equals() ensures that the only objects
            // being equal are also identical (see mountTime).
            return super.hashCode();
        }

        /**
         * Compared this object with another.
         *
         * @param obj            the object to compare with
         *
         * @return a negative integer, zero, or a positive integer as
         *         this object is less than, equal to, or greater than
         *         the specified one
         *
         * @throws ClassCastException if the specified object wasn't
         *             a mount point
         */
        public int compareTo(Object obj) {
            MountPoint  other = (MountPoint) obj;
            int         cmp1 = overlayPrio - other.overlayPrio;
            long        cmp2 = mountTime - other.mountTime;

            cmp2 = Math.max(Math.min(cmp2, -1L), 1L);
            return (cmp1 != 0) ? -cmp1 : (int) cmp2;
        }

        /**
         * Checks if the storage should also be overlaid over the
         * root directory.
         *
         * @return true if the storage is overlaid, or
         *         false otherwise
         */
        public boolean isOverlay() {
            return this.overlayPrio >= 0;
        }

        /**
         * Updates the mount point flags.
         *
         * @param readWrite      the read-write flag
         * @param overlayPrio    the overlay priority, negative for
         *                       no overlay
         */
        public void update(boolean readWrite, int overlayPrio) {
            lastMountTime = Math.max(System.currentTimeMillis(), lastMountTime + 1);
            this.readWrite = readWrite;
            this.overlayPrio = overlayPrio;
            this.mountTime = lastMountTime;
        }
    }
}
