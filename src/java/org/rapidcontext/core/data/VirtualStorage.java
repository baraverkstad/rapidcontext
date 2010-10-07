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
     * The meta-data storage for mount points and parent indices.
     * The mount point objects will be added to this storage under
     * their corresponding path (slightly modified to form an object
     * path instead of an index path).
     */
    private MemoryStorage metaStorage = new MemoryStorage(Path.ROOT, true);

    /**
     * The sorted array of mount points. This array is sorted every
     * time a mount point is added or modified.
     */
    // TODO: rename!
    private Array mountpoints = new Array();

    /**
     * Creates a new overlay storage.
     *
     * @param path           the base storage path
     * @param readWrite      the read write flag
     */
    public VirtualStorage(Path path, boolean readWrite) {
        super("virtual", path, readWrite);
        dict.set("storages", mountpoints);
        try {
            metaStorage.store(new Path("/storageinfo"), dict);
        } catch (StorageException e) {
            LOG.severe("error while initializing virtual storage: " +
                       e.getMessage());
        }
    }

    /**
     * Returns the mount point at a specific storage location. If the
     * specified path does not match an existing mount point exactly,
     * null will be returned.
     *
     * @param path           the mount storage location
     *
     * @return the mount point found, or
     *         null if not found
     */
    private MountPoint getMountPoint(Path path) {
        return (MountPoint) metaStorage.load(path.child("storage", false));
    }

    /**
     * Removes a mount point at a specific storage location.
     *
     * @param path           the mount storage location
     * @param mount          the mount point, or null to remove
     *
     * @throws StorageException if the data couldn't be written
     */
    private void setMountPoint(Path path, MountPoint mount)
    throws StorageException {

        path = path.child("storage", false);
        if (mount == null) {
            metaStorage.remove(path);
        } else {
            metaStorage.store(path, mount);
        }
    }

    /**
     * Returns the parent mount point for a storage location. All
     * mount points will be searched in order to find a matching
     * parent mount point (if one exists).
     *
     * @param path           the storage location
     *
     * @return the parent mount point found, or
     *         null if not found
     */
    private MountPoint getParentMountPoint(Path path) {
        for (int i = 0; i < mountpoints.size(); i++) {
            MountPoint mount = (MountPoint) mountpoints.get(i);
            if (path.startsWith(mount.getPath())) {
                return mount;
            }
        }
        return null;
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

        MountPoint  mount;
        String      msg;

        if (!path.isIndex()) {
            msg = "cannot mount storage to a non-index path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        } else if (metaStorage.lookup(path) != null) {
            msg = "storage mount path conflicts with another mount: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        mount = new MountPoint(storage, path, readWrite, overlay, prio);
        setMountPoint(path, mount);
        mountpoints.add(mount);
        mountpoints.sort();
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

        MountPoint  mount = getMountPoint(path);
        String      msg;

        if (mount == null) {
            msg = "no mounted storage found matching path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        mount.update(readWrite, overlay, prio);
        mountpoints.sort();
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
        MountPoint  mount = getMountPoint(path);
        String      msg;

        if (mount == null) {
            msg = "no mounted storage found matching path: " + path;
            LOG.warning(msg);
            throw new StorageException(msg);
        }
        mountpoints.remove(mountpoints.indexOf(mount));
        setMountPoint(path, null);
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
     *
     * @throws StorageException if the storage couldn't be accessed
     */
    public Metadata lookup(Path path) throws StorageException {
        MountPoint  mount = getParentMountPoint(path);
        Metadata    meta = null;
        Metadata    idx = null;

        if (mount != null) {
            return mount.getStorage().lookup(path.subPath(mount.getPath().length()));
        } else {
            meta = metaStorage.lookup(path);
            if (meta != null && meta.isIndex()) {
                idx = meta;
            } else if (meta != null) {
                return meta;
            }
            for (int i = 0; i < mountpoints.size(); i++) {
                mount = (MountPoint) mountpoints.get(i);
                if (mount.isOverlay()) {
                    meta = mount.getStorage().lookup(path);
                    if (meta != null && meta.isIndex()) {
                        idx = Metadata.lastModified(idx, meta);
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
     *
     * @throws StorageException if the data couldn't be read
     */
    public Object load(Path path) throws StorageException {
        MountPoint  mount = getParentMountPoint(path);
        Object      res;
        Index       idx = null;

        if (mount != null) {
            return mount.getStorage().load(path.subPath(mount.getPath().length()));
        } else {
            res = metaStorage.load(path);
            if (res instanceof Index) {
                idx = (Index) res;
            } else if (res != null) {
                return res;
            }
            for (int i = 0; i < mountpoints.size(); i++) {
                mount = (MountPoint) mountpoints.get(i);
                if (mount.isOverlay()) {
                    res = mount.getStorage().load(path);
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
        MountPoint  mount = getParentMountPoint(path);
        String      msg;

        if (mount != null) {
            if (!mount.isReadWrite()) {
                msg = "cannot write to read-only storage at " + mount.getPath();
                LOG.warning(msg);
                throw new StorageException(msg);
            }
            mount.getStorage().store(path.subPath(mount.getPath().length()), data);
        } else {
            for (int i = 0; i < mountpoints.size(); i++) {
                mount = (MountPoint) mountpoints.get(i);
                if (mount.isOverlay() && mount.isReadWrite()) {
                    mount.getStorage().store(path, data);
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
        MountPoint  mount = getParentMountPoint(path);

        if (mount != null) {
            mount.getStorage().remove(path.subPath(mount.getPath().length()));
        } else {
            for (int i = 0; i < mountpoints.size(); i++) {
                mount = (MountPoint) mountpoints.get(i);
                if (mount.isOverlay() && mount.isReadWrite()) {
                    mount.getStorage().remove(path);
                }
            }
        }
    }


    /**
     * A storage mount point. This class encapsulates all the
     * relevant meta-data and allows sorting the mount points in
     * overlay priority order.
     */
    // TODO: export mount point class properly!
    private static class MountPoint extends Dict implements Comparable {

        /**
         * The system time of the last mount or remount operation.
         */
        private static long lastMountTime = 0L;

        /**
         * Creates a new mount point.
         *
         * @param storage        the storage mounted
         * @param path           the storage location
         * @param readWrite      the read-write flag
         * @param overlay        the root overlay flag
         * @param prio           the root overlay search priority (higher
         *                       numbers are searched before lower numbers)
         */
        public MountPoint(Storage storage,
                          Path path,
                          boolean readWrite,
                          boolean overlay,
                          int prio) {

            set("type", "storage");
            set("storage", storage);
            set("path", path);
            update(readWrite, overlay, prio);
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
            int         cmp1 = getPrio() - other.getPrio();
            int         cmp2 = getMountTime().compareTo(other.getMountTime());

            return (cmp1 != 0) ? -cmp1 : cmp2;
        }

        /**
         * Returns the mounted storage.
         *
         * @return the mounted storage
         */
        public Storage getStorage() {
            return (Storage) get("storage");
        }

        /**
         * Returns the mount path.
         *
         * @return the mount path
         */
        public Path getPath() {
            return (Path) get("path");
        }

        /**
         * Returns the root overlay priority from this mount point.
         *
         * @return the root overlay priority, or
         *         -1 if no root overlay should be used
         */
        public int getPrio() {
            return getInt("prio", -1);
        }

        /**
         * Returns the last mount or remount time for this mount point.
         *
         * @return the last mount time (in milliseconds)
         */
        public Date getMountTime() {
            return (Date) get("mountTime");
        }

        /**
         * Checks if this mount point is writable.
         *
         * @return true if this mount point is writable, or
         *         false otherwise
         */
        public boolean isReadWrite() {
            return getBoolean("readWrite", false);
        }

        /**
         * Checks if the storage should also be overlaid over the
         * root directory.
         *
         * @return true if the storage is overlaid, or
         *         false otherwise
         */
        public boolean isOverlay() {
            return getBoolean("overlay", false);
        }

        /**
         * Updates the mount point flags.
         *
         * @param readWrite      the read-write flag, use false for read-only
         * @param overlay        the root overlay flag
         * @param prio           the root overlay search priority (higher
         *                       numbers are searched before lower numbers)
         */
        public void update(boolean readWrite, boolean overlay, int prio) {
            lastMountTime = Math.max(System.currentTimeMillis(), lastMountTime + 1);
            setBoolean("readWrite", readWrite);
            setBoolean("overlay", overlay);
            setInt("prio", overlay ? prio : -1);
            set("mountTime", new Date(lastMountTime));
        }
    }
}
