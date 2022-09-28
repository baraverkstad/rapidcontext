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

import org.rapidcontext.core.data.Dict;

/**
 * The base class for all storable Java objects. This class provides
 * a number of basic services required for managing objects in the
 * storage subsystem:
 *
 * <ul>
 *   <li><strong>Object Type</strong> -- The object type identifies
 *       the kind of object, e.g. "index", "file", "plugin", etc. By
 *       registering a Java class corresponding to an object type,
 *       the instance creation is handled automatically when a new
 *       object is loaded from storage.
 *   <li><strong>Object Id</strong> -- The object identifier is used
 *       to locate the object in a storage. The full object storage
 *       path is normally formed as "&lt;type&gt;/&lt;id&gt;". Note
 *       that the object id may contain additional "/" characters.
 *   <li><strong>Serialization</strong> -- A data dictionary instance
 *       is encapsulated to enable simple and efficient serialization
 *       and unserialization of object instances. It is recommended
 *       to store all persistent data in this dictionary, although
 *       both serialization and unserialization can be overridden.
 *   <li><strong>Life-cycle Handling</strong> -- Object instances are
 *       initialized, destroyed and cached automatically by the root
 *       storage. Whenever an instance reports being inactive, it is
 *       eligible for automatic removal (destruction) from the cache.
 * </ul>
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StorableObject {

    /**
     * The dictionary key for the object identifier. The value stored
     * is a string and is used to form the object storage path.
     */
    public static final String KEY_ID = "id";

    /**
     * The dictionary key for the object type. The value stored is a
     * string and is used as the prefix when forming the object
     * storage path.
     */
    public static final String KEY_TYPE = "type";

    /**
     * The dictionary key for the Java class name. The value stored
     * is a string with the fully qualified Java class name for
     * initializing the object from storage. The class is used
     * instead of the default type initializer, but the type id must
     * still be specified.
     */
    public static final String KEY_CLASSNAME = "className";

    /**
     * The dictionary containing the serializable data for this
     * object.
     */
    protected Dict dict = null;

    /**
     * Creates a new object. This is the default constructor for
     * creating storable object instances.
     *
     * @param id             the object identifier
     * @param type           the type name
     */
    protected StorableObject(String id, String type) {
        this.dict = new Dict();
        this.dict.add(KEY_ID, id);
        this.dict.add(KEY_TYPE, type);
    }

    /**
     * Creates a new object from a serialized representation. This
     * constructor should normally only be used for unserialization.
     * The key-value pairs from the specified dictionary will be
     * copied (shallow copy) into a new dictionary.<p>
     *
     * <strong>Note:</strong> This constructor signature is used for
     * automatic object creation (unserialization). Subclasses using
     * this feature MUST implement a public constructor with this
     * exact signature.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     *
     * @see #init()
     */
    protected StorableObject(String id, String type, Dict dict) {
        this.dict = new Dict(dict.size() + 2);
        this.dict.add(KEY_ID, id);
        this.dict.add(KEY_TYPE, type);
        this.dict.setAll(dict);
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    public String toString() {
        return type() + " " + id();
    }

    /**
     * Checks if this object is in active use. This method should
     * return true if the object is in use, was used recently, or is
     * likely to be requested again shortly. The outcome is used to
     * remove non-active object instances from the storage cache. By
     * default, this method always returns true.
     *
     * @return true if the object is active, or
     *         false otherwise
     */
    protected boolean isActive() {
        return true;
    }

    /**
     * Checks if this object has been modified since initialized from
     * storage. This method is used to allow "dirty" objects to be
     * written back to persistent storage before being evicted from
     * the in-memory cache. By default this method always returns
     * false.
     *
     * @return true if the object has been modified, or
     *         false otherwise
     */
    protected boolean isModified() {
        return false;
    }

    /**
     * Returns the object identifier.
     *
     * @return the object identifier
     */
    public String id() {
        return dict.getString(KEY_ID, "");
    }

    /**
     * Returns the object type name.
     *
     * @return the object type name
     */
    public String type() {
        return dict.getString(KEY_TYPE, "");
    }

    /**
     * Returns the (recommended) object storage path. The storage
     * path is created by concatenating the object type and id.
     *
     * @return the object storage path
     */
    public Path path() {
        return Path.from(type() + "/" + id());
    }

    /**
     * Initializes this object after loading it from a storage. Any
     * object initialization that may fail or that causes the object
     * to interact with any other part of the system (or external
     * systems) should be implemented here.<p>
     *
     * This method is guaranteed to be called before the object is
     * returned from the storage. If this method throws an exception,
     * the destroy() method will NOT be called.
     *
     * @throws StorageException if the initialization failed
     */
    protected void init() throws StorageException {
        // Default implementation does nothing
    }

    /**
     * Destroys this object. This method is used to free any
     * resources used when this object is no longer used. This method
     * is called when an object is removed from the in-memory storage
     * (object cache).<p>
     *
     * <strong>Note:</strong> The object destruction cannot be halted
     * by throwing an exception. The exception message will only be
     * logged by the storage.
     *
     * @throws StorageException if the destruction failed
     */
    protected void destroy() throws StorageException {
        // Default implementation does nothing
    }

    /**
     * Activates this object. This method is called whenever the
     * object instance is returned from storage and may be called by
     * several threads in parallel. The default implementation does
     * nothing.
     */
    protected void activate() {
        // Default implementation does nothing
    }

    /**
     * Attempts to deactivate this object. This method is called by a
     * background task, roughly every 30 seconds for all object
     * instances in the storage cache. It may be called earlier on
     * the first invocation, since all objects share a common timer.
     * The default implementation does nothing.
     */
    protected void passivate() {
        // Default implementation does nothing
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
        return dict.copy();
    }
}
