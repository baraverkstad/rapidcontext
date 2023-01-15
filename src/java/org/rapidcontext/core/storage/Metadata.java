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

import java.util.Date;

import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;

/**
 * An object metadata container. Used for basic introspection of
 * objects inside storages.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Metadata extends StorableObject {

    /**
     * The dictionary key for the object category, i.e. the type of
     * object being described. The value stored is a string, using
     * one of the predefined constant type values.
     *
     * @see #CATEGORY_INDEX
     * @see #CATEGORY_OBJECT
     * @see #CATEGORY_BINARY
     */
    public static final String KEY_CATEGORY = "category";

    /**
     * The dictionary key for the Java class of the object. The value
     * stored is the actual Class of the object.
     */
    public static final String KEY_CLASS = "class";

    /**
     * The dictionary key for the object path. The value stored is a
     * path object.
     */
    public static final String KEY_PATH = "path";

    /**
     * The dictionary key for the storage paths. The value is an
     * array with path objects for all storages containing the path.
     */
    public static final String KEY_STORAGES = "storages";

    /**
     * The dictionary key for the object MIME type. The value stored
     * is the MIME type of the object (prior to loading) if known.
     */
    public static final String KEY_MIMETYPE = "mimeType";

    /**
     * The dictionary key for the last modified date. The value stored
     * is a Date object.
     */
    public static final String KEY_MODIFIED = "lastModified";

    /**
     * The index category value.
     */
    public static final String CATEGORY_INDEX = "index";

    /**
     * The object category value.
     */
    public static final String CATEGORY_OBJECT = "object";

    /**
     * The binary category value.
     */
    public static final String CATEGORY_BINARY = "binary";

    /**
     * Returns the object category based on the class. This method
     * checks if the class inherits from the Index or Binary classes,
     * otherwise it returns the object class.
     *
     * @param clazz          the object class
     *
     * @return the object category
     */
    public static String category(Class<?> clazz) {
        if (Index.class.isAssignableFrom(clazz)) {
            return CATEGORY_INDEX;
        } else if (Binary.class.isAssignableFrom(clazz)) {
            return CATEGORY_BINARY;
        } else {
            return CATEGORY_OBJECT;
        }
    }

    /**
     * Merges two metadata containers. If either of the two values is
     * null, the other will be returned. Otherwise, a new metadata
     * container is created as a merged copy of two. The first will
     * serve as the base, adding additional storage paths from the
     * second as needed. For index objects, the last modified date
     * will be adjusted to the maximum of the two objects.
     *
     * @param meta1          the first metadata object
     * @param meta2          the second metadata object
     *
     * @return the merged metadata container
     */
    public static Metadata merge(Metadata meta1, Metadata meta2) {
        if (meta1 == null) {
            return meta2;
        } else if (meta2 == null) {
            return meta1;
        } else {
            return new Metadata(meta1, meta2);
        }
    }

    /**
     * Creates a new metadata container with modified path information.
     *
     * @param path           the absolute object path
     * @param meta           the metadata container to copy
     */
    public Metadata(Path path, Metadata meta) {
        super(path.toString(), "metadata");
        dict.setAll(meta.dict);
        dict.set(KEY_PATH, path);
    }

    /**
     * Creates a new metadata container that is a merged copy of two
     * others. The first will serve as the base, adding additional
     * storage paths as needed. For index objects, the last modified
     * date be adjusted to the maximum of the two objects.
     *
     * @param meta1          the first metadata object
     * @param meta2          the second metadata object
     */
    private Metadata(Metadata meta1, Metadata meta2) {
        super(meta1.id(), "metadata");
        dict.setAll(meta1.dict);
        dict.set(KEY_STORAGES, meta1.storages().union(meta2.storages()));
        if (isIndex() && meta2.lastModified().after(meta1.lastModified())) {
            dict.set(KEY_MODIFIED, meta2.lastModified());
        }
    }

    /**
     * Creates a new metadata container.
     *
     * @param clazz          the object class
     * @param path           the absolute object path
     * @param storagePath    the absolute storage path
     * @param mime           the MIME type, or null for unknown
     * @param modified       the last modified date, or null for now
     */
    public Metadata(Class<?> clazz, Path path, Path storagePath, String mime, Date modified) {
        this(category(clazz), clazz, path, storagePath, mime, modified);
    }

    /**
     * Creates a new metadata container.
     *
     * @param category       the object category
     * @param clazz          the object class
     * @param path           the absolute object path
     * @param storagePath    the absolute storage path
     * @param mime           the MIME type, or null for unknown
     * @param modified       the last modified date, or null for now
     */
    public Metadata(String category, Class<?> clazz, Path path, Path storagePath, String mime, Date modified) {
        super(path.toString(), "metadata");
        dict.set(KEY_CATEGORY, category);
        dict.set(KEY_CLASS, clazz);
        dict.set(KEY_PATH, path);
        dict.set(KEY_STORAGES, new Array());
        dict.getArray(KEY_STORAGES).add(storagePath);
        dict.set(KEY_MIMETYPE, mime);
        dict.set(KEY_MODIFIED, (modified == null) ? new Date() : modified);
    }

    /**
     * Checks if the object category is an index.
     *
     * @return true if the object category is an index, or
     *         false otherwise
     */
    public boolean isIndex() {
        return CATEGORY_INDEX.equals(category());
    }

    /**
     * Checks if the object category is an object.
     *
     * @return true if the object category is an object, or
     *         false otherwise
     */
    public boolean isObject() {
        return CATEGORY_OBJECT.equals(category());
    }

    /**
     * Checks if the object category is an object of the specified
     * class (or a subclass).
     *
     * @param clazz          the object class
     *
     * @return true if the object category is a matching object, or
     *         false otherwise
     */
    public boolean isObject(Class<?> clazz) {
        return isObject() && clazz.isAssignableFrom(classInstance());
    }

    /**
     * Checks if the object category is binary.
     *
     * @return true if the object category is binary, or
     *         false otherwise
     */
    public boolean isBinary() {
        return CATEGORY_BINARY.equals(category());
    }

    /**
     * Returns the category for the object.
     *
     * @return the category for the object
     *
     * @see #CATEGORY_INDEX
     * @see #CATEGORY_OBJECT
     * @see #CATEGORY_BINARY
     */
    public String category() {
        return dict.getString(KEY_CATEGORY, null);
    }

    /**
     * Returns the class for the object.
     *
     * @return the class for the object
     */
    public Class<?> classInstance() {
        return (Class<?>) dict.get(KEY_CLASS);
    }

    /**
     * Returns the class name for the object.
     *
     * @return the class name for the object
     */
    public String className() {
        return classInstance().getName();
    }

    /**
     * Returns the absolute object path.
     *
     * @return the absolute object path
     */
    public Path path() {
        return (Path) dict.get(KEY_PATH);
    }

    /**
     * Returns an array with the root paths to all storages
     * containing this object.
     *
     * @return an array with path objects for storage roots
     */
    public Array storages() {
        return (Array) dict.get(KEY_STORAGES);
    }

    /**
     * Returns the MIME type for the object.
     *
     * @return the MIME type for the object, or null
     */
    public String mimeType() {
        return dict.getString(KEY_MIMETYPE, null);
    }

    /**
     * Returns the last modified date for the object.
     *
     * @return the last modified date for the object
     */
    public Date lastModified() {
        return (Date) dict.get(KEY_MODIFIED);
    }

    /**
     * Updates the last modified date for the object.
     *
     * @param date           the date to set, or null for now
     */
    protected void updateLastModified(Date date) {
        dict.set(KEY_MODIFIED, (date == null) ? new Date() : date);
    }

    /**
     * Returns a serialized representation of this object. Used when
     * persisting to permanent storage or when accessing the object
     * from outside pure Java. Returns a shallow copy of the contained
     * dictionary.
     *
     * @return the serialized representation of this object
     */
    public Dict serialize() {
        Dict copy = super.serialize();
        copy.remove(KEY_ID);
        copy.remove(KEY_TYPE);
        return copy;
    }
}
