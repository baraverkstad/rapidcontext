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

import java.io.File;
import java.util.Date;

/**
 * An object metadata container. Used for basic introspection of
 * objects inside storages. 
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Metadata extends DynamicObject {

    /**
     * The dictionary key for the object category, i.e. the type of
     * object being described. The value stored is a string, using
     * one of the predefined constant type values.
     *
     * @see #CATEGORY_INDEX
     * @see #CATEGORY_OBJECT
     * @see #CATEGORY_FILE
     */
    public static final String KEY_CATEGORY = "category";

    /**
     * The dictionary key for the Java class of the object. The value
     * stored is the actual Class of the object.
     */
    public static final String KEY_CLASS = "class";

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
     * The file category value.
     */
    public static final String CATEGORY_FILE = "file";

    /**
     * Returns the last modified of two metadata objects.
     *
     * @param meta1          the first metadata object
     * @param meta2          the second metadata object
     *
     * @return the metadata object with the latest modified date
     */
    public static Metadata lastModified(Metadata meta1, Metadata meta2) {
        if (meta1 == null) {
            return meta2;
        } else if (meta2 == null) {
            return meta1;
        } else if (meta2.lastModified().after(meta1.lastModified())) {
            return meta2;
        } else {
            return meta1;
        }
    }

    /**
     * Creates a new metadata container for an index.
     *
     * @param idx            the index to inspect
     */
    public Metadata(Index idx) {
        this(CATEGORY_INDEX, Index.class, idx.lastModified());
    }

    /**
     * Creates a new metadata container for a file.
     *
     * @param file           the file to inspect
     */
    public Metadata(File file) {
        this(CATEGORY_FILE, File.class, new Date(file.lastModified()));
    }

    /**
     * Creates a new metadata container for a generic object.
     *
     * @param obj            the object to inspect
     */
    public Metadata(Object obj) {
        this(CATEGORY_OBJECT, obj.getClass(), new Date());
    }

    /**
     * Creates a new metadata container.
     *
     * @param category       the object category
     * @param clazz          the object class
     * @param lastModified   the last modified date, or null for now
     */
    public Metadata(String category, Class clazz, Object lastModified) {
        super("metadata");
        dict.set(KEY_CATEGORY, category);
        dict.set(KEY_CLASS, clazz);
        if (lastModified instanceof Date) {
            dict.set(KEY_MODIFIED, lastModified);
        } else if (lastModified instanceof Long) {
            dict.set(KEY_MODIFIED, new Date(((Long) lastModified).longValue()));
        } else {
            dict.set(KEY_MODIFIED, new Date());
        }
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
     * Checks if the object category is a file.
     *
     * @return true if the object category is a file, or
     *         false otherwise
     */
    public boolean isFile() {
        return CATEGORY_FILE.equals(category());
    }

    /**
     * Returns the category for the object.
     *
     * @return the category for the object
     *
     * @see #CATEGORY_INDEX
     * @see #CATEGORY_OBJECT
     * @see #CATEGORY_FILE
     */
    public String category() {
        return dict.getString(KEY_CATEGORY, null);
    }

    /**
     * Returns the class for the object.
     *
     * @return the class for the object
     */
    public Class classInstance() {
        return (Class) dict.get(KEY_CLASS);
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
     * Returns the last modified date for the object.
     *
     * @return the last modified date for the object
     */
    public Date lastModified() {
        return (Date) dict.get(KEY_MODIFIED);
    }
}
