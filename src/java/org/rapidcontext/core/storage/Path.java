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

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * A data storage path. This class encapsulates the path (directory
 * plus name) of an object, a file or an index. It also provides
 * some simple help methods to access and work with the path and to
 * locate the object addressed by it.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Path {

    /**
     * A root path constant. Note that several paths may be root
     * paths, so this is not a unique instance. It is only here
     * for convenience.
     */
    public static final Path ROOT = new Path(null, "", true);

    /**
     * The parent path reference. Will be null for the path root.
     */
    private Path parent;

    /**
     * The path element name. The full name is constructed by
     * prepending all parent names with separators.
     */
    private String name;

    /**
     * The index flag. This flag is set if the path element
     * corresponds to an index (i.e. a directory, a list of objects
     * and files).
     */
    private boolean index;

    /**
     * The total number of path elements (including this one). The
     * root path element is not counted.
     */
    private int length;

    /**
     * Creates a new path from a string representation (similar to
     * a file system path).
     *
     * @param path           the string path to parse
     *
     * @return the path created
     */
    public static Path from(String path) {
        return resolve(ROOT, path);
    }

    /**
     * Resolves a path starting at a parent path. Leading and duplicate
     * separator ('/') characters are ignored. A trailing separator ('/')
     * character indicates that the resulting path is an index. Any
     * relative ('../') path components will be resolved to the extent
     * possible.
     *
     * @param parent         the parent index path
     * @param path           the descendant path
     *
     * @return the resolved path
     */
    public static Path resolve(Path parent, String path) {
        String normalized = StringUtils.strip(path, "/");
        if (normalized == null || normalized.equals("")) {
            return parent;
        } else {
            parent = parent.isIndex() ? parent : parent.parent();
            boolean rooted = false;
            String[] parts = StringUtils.splitByWholeSeparator(normalized, "/");
            for (int i = 0; i < parts.length - 1; i++) {
                rooted = rooted || parent.isRoot();
                if (!rooted && parts[i].equals("..")) {
                    parent = parent.parent();
                } else {
                    parent = new Path(parent, parts[i], true);
                }
            }
            return new Path(parent, parts[parts.length - 1], path.endsWith("/"));
        }
    }

    /**
     * Resolves a path starting at a parent path. Any relative ('../')
     * path components will be resolved to the extent possible.
     *
     * @param parent         the parent index path
     * @param path           the descendant path
     *
     * @return the resolved path
     */
    public static Path resolve(Path parent, Path path) {
        if (path == null || path.isRoot()) {
            return parent;
        } else {
            parent = parent.isIndex() ? parent : parent.parent();
            parent = resolve(parent, path.parent());
            if (!parent.isRoot() && path.name().equals("..")) {
                return parent.parent();
            } else {
                return new Path(parent, path.name, path.index);
            }
        }
    }

    /**
     * Creates a new path from the specified parts.
     *
     * @param parent         the parent index path
     * @param name           the element name
     * @param index          the index flag
     */
    private Path(Path parent, String name, boolean index) {
        this.parent = parent;
        this.name = name;
        this.index = index;
        this.length = isRoot() ? 0 : this.parent.length + 1;
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    public String toString() {
        return "/" + toIdent(0);
    }

    /**
     * Returns an object identifier based on this path. The
     * identifier will start at the specified position in this path.
     *
     * @param pos            the position, from 0 to length()
     *
     * @return an object identifier for this path (without prefix)
     */
    public String toIdent(int pos) {
        if (pos < 0) {
            pos = Math.max(0, length() + pos);
        }
        if (pos + 1 > length) {
            return "";
        } else if (pos + 1 == length) {
            return name + (index ? "/" : "");
        } else {
            return parent.toIdent(pos) + name + (index ? "/" : "");
        }
    }

    /**
     * Checks if this path is identical to another path. The two
     * paths will be considered equal if they have the same length,
     * all elements are equal and the index flag is identical.
     *
     * @param obj            the object to compare with
     *
     * @return true if the two paths are equal, or
     *         false otherwise
     */
    public boolean equals(Object obj) {
        Path path = (obj instanceof Path) ? (Path) obj : null;
        return path != null &&
               this.length == path.length &&
               this.index == path.index &&
               this.name.equalsIgnoreCase(path.name) &&
               Objects.equals(this.parent, path.parent);
    }

    /**
     * Returns a hash code for this object.
     *
     * @return a hash code for this object
     */
    public int hashCode() {
        int code = (this.parent == null) ? 0 : this.parent.hashCode();
        return code + this.name.toLowerCase().hashCode();
    }

    /**
     * Checks if this path corresponds to the root index.
     *
     * @return true if the path is for the root index, or
     *         false otherwise
     */
    public boolean isRoot() {
        return this.parent == null;
    }

    /**
     * Checks if this path corresponds to an index.
     *
     * @return true if the path is an index, or
     *         false otherwise
     */
    public boolean isIndex() {
        return this.index;
    }

    /**
     * Checks if this path starts with the specified path. All the
     * path elements must match up to the length of the specified
     * path. As a special case, this method will return true if the
     * two paths are identical. It will also return true for a null
     * path.
     *
     * @param path           the path to compare with
     *
     * @return true if this path starts with the specified path, or
     *         false otherwise
     */
    public boolean startsWith(Path path) {
        return path == null ||
               equals(path) ||
               (this.parent != null && this.parent.startsWith(path));
    }

    /**
     * Returns the directory depth. The root index, and any objects
     * located directly there, have depth zero (0). For each
     * additional sub-level traversed, the depth is increased by
     * one (1). Objects and files in the storage tree will not
     * affect the depth.
     *
     * @return the path directory depth
     */
    public int depth() {
        return this.length - (this.index ? 0 : 1);
    }

    /**
     * Returns the path length. The length contains the number of
     * elements in the path, counting both indices and any named
     * object or file. The length is always greater or equal to
     * the depth.
     *
     * @return the path length
     */
    public int length() {
        return this.length;
    }

    /**
     * Returns the name of the last element in the path. This is
     * normally the object name.
     *
     * @return the object or index name, or
     *         null for the root index
     */
    public String name() {
        return this.name;
    }

    /**
     * Returns the name of the path element at the specified position.
     * A zero position will return the first element traversed, i.e.
     * the one located in the root.
     *
     * @param pos            the position, from 0 to length()
     *
     * @return the name of the element at the specified position, or
     *         null if the position is out of range
     */
    public String name(int pos) {
        if (pos + 1 > this.length) {
            return null;
        } else if (pos + 1 == this.length) {
            return this.name;
        } else {
            return this.parent.name(pos);
        }
    }

    /**
     * Creates a new path to the parent index.
     *
     * @return a new path to the parent index
     */
    public Path parent() {
        return isRoot() ? ROOT : this.parent;
    }

    /**
     * Creates a new path to a child index or object.
     *
     * @param name           the child name
     * @param isIndex        the index flag
     *
     * @return a new path to a child index or object
     */
    public Path child(String name, boolean isIndex) {
        return new Path(this, name, isIndex);
    }

    /**
     * Creates a new path to a sibling index or object.
     *
     * @param name           the sibling name
     *
     * @return a new path to a sibling index or object
     */
    public Path sibling(String name) {
        return parent().child(name, this.index);
    }

    /**
     * Creates a new path with the specified prefix removed.
     *
     * @param prefix         the prefix path to remove
     *
     * @return a new path with the common prefix removed
     */
    public Path removePrefix(Path prefix) {
        if (isRoot() || equals(prefix)) {
            return ROOT;
        } else {
            return new Path(this.parent.removePrefix(prefix), this.name, this.index);
        }
    }
}
