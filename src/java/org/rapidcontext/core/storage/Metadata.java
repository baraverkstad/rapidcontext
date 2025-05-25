/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2025 Per Cederberg. All rights reserved.
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

import org.apache.commons.lang3.ObjectUtils;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.type.Plugin;

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
    public static final String KEY_MODIFIED = "modified";

    /**
     * The dictionary key for the size (in bytes) of the stored data.
     * The value stored is a Long value.
     */
    public static final String KEY_SIZE = "size";

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
     * storage paths as needed. The last modified date be adjusted to
     * the maximum of the two objects.
     *
     * @param meta1          the first metadata object
     * @param meta2          the second metadata object
     */
    private Metadata(Metadata meta1, Metadata meta2) {
        super(meta1.id(), "metadata");
        dict.setAll(meta1.dict);
        dict.set(PREFIX_COMPUTED + KEY_STORAGES, meta1.storages().union(meta2.storages()));
        if (mimeType() == null) {
            mimeType(meta2.mimeType());
        }
        modified(ObjectUtils.max(meta1.modified(), meta2.modified()));
        size(Math.max(meta1.size(), meta2.size()));
    }

    /**
     * Creates a new metadata container.
     *
     * @param clazz          the object class
     * @param path           the absolute object path
     * @param storagePath    the absolute storage path
     * @param modified       the last modified date, or null for unknown
     */
    public Metadata(Class<?> clazz, Path path, Path storagePath, Date modified) {
        super(path.toString(), "metadata");
        dict.set(KEY_CATEGORY, category(clazz));
        dict.set(KEY_CLASS, clazz);
        dict.set(KEY_PATH, path);
        dict.set(KEY_MIMETYPE, null);
        dict.set(KEY_MODIFIED, modified);
        dict.set(KEY_SIZE, 0L);
        dict.set(PREFIX_COMPUTED + KEY_STORAGES, Array.of(storagePath));
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
        return dict.get(KEY_CATEGORY, String.class);
    }

    /**
     * Returns the class for the object.
     *
     * @return the class for the object
     */
    public Class<?> classInstance() {
        return dict.get(KEY_CLASS, Class.class);
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
    @Override
    public Path path() {
        return dict.get(KEY_PATH, Path.class);
    }

    /**
     * Returns an array with the root paths to all storages
     * containing this object.
     *
     * @return an array with path objects for storage roots
     */
    public Array storages() {
        return dict.getArray(dictKey(KEY_STORAGES));
    }

    /**
     * Returns the MIME type for the object.
     *
     * @return the MIME type for the object, or null
     */
    public String mimeType() {
        return dict.get(KEY_MIMETYPE, String.class);
    }

    /**
     * Sets the MIME type for the object.
     *
     * @param mime           the MIME type, or null for unknown
     *
     * @return this metadata object (for chaining)
     */
    protected Metadata mimeType(String mime) {
        dict.set(KEY_MIMETYPE, mime);
        return this;
    }

    /**
     * Returns the last modified date for the object.
     *
     * @return the last modified date for the object, or
     *         null if unknown
     */
    public Date modified() {
        return dict.get(KEY_MODIFIED, Date.class);
    }

    /**
     * Sets the last modified date for the object.
     *
     * @param date           the date to set, or null for unknown
     *
     * @return this metadata object (for chaining)
     */
    protected Metadata modified(Date date) {
        dict.set(KEY_MODIFIED, date);
        return this;
    }

    /**
     * Returns the size (in bytes) of the object.
     *
     * @return the size (in bytes) of the object, or
     *         zero (0) if unknown
     */
    public long size() {
        return dict.get(KEY_SIZE, Long.class, 0L);
    }

    /**
     * Sets the size (in bytes) of the object.
     *
     * @param size           the size to set
     *
     * @return this metadata object (for chaining)
     */
    protected Metadata size(long size) {
        dict.set(KEY_SIZE, size);
        return this;
    }

    /**
     * Returns a serialized representation of this object. Used when
     * persisting to permanent storage or when accessing the object
     * from outside pure Java. Returns a shallow copy of the contained
     * dictionary.
     *
     * @return the serialized representation of this object
     */
    @Override
    public Dict serialize() {
        Dict copy = super.serialize();
        copy.remove(KEY_ID);
        copy.remove(KEY_TYPE);
        copy.remove(PREFIX_COMPUTED + KEY_ACTIVATED_TIME);
        copy.setIfNull(KEY_MODIFIED, () -> new Date());
        copy.add(PREFIX_COMPUTED + "plugin", Plugin.source(this));
        return copy;
    }
}
