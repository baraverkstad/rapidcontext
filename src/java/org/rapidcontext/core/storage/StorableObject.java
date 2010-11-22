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

import org.rapidcontext.core.data.Dict;

/**
 * The base class for all storable Java objects. This class
 * implements a number of basic services required for object
 * storage:
 *
 * <ul>
 *   <li><strong>Object Type</strong> -- The object type identifies
 *       the kind of object, e.g. "index", "file", "plugin", etc. By
 *       registering a Java class corresponding to an object type,
 *       the instance creation can be handled automatically when the
 *       object is loaded from storage.
 *   <li><strong>Object Id</strong> -- The object identifier is used
 *       to locate the object in a storage. The full object storage
 *       path is normally formed as "&lt;type&gt;/&lt;id&gt;". Note
 *       that the object id may contain additional "/" characters.
 *   <li><strong>Data Dictionary</strong> -- A dictionary instance
 *       is provided for storing all serializable values in the
 *       object.
 *   <li><strong>Serialization</strong> -- The default serialization
 *       simply returns the data dictionary, as it is assumed to
 *       contain all relevant data. Unserialization is likewise also
 *       provided via default constructors.
 *   <li><strong>Lifecycle Handling</strong> -- The object
 *       initialization and destruction is controllable via instance
 *       methods in this class.
 * </ul>
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StorableObject {

    // TODO: transient interface to add?

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
        return new Path(type() + "/" + id());
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
     * Returns a serialized representation of this object. Used when
     * accessing the object from outside pure Java. By default this
     * method will return the contained dictionary.
     *
     * @return the serialized representation of this object
     */
    public Dict serialize() {
        return dict;
    }
}
