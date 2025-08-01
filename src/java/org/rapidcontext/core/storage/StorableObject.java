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
import java.util.Objects;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.core.data.Array;
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
 * @author Per Cederberg
 */
public class StorableObject {

    /**
     * The class logger.
     */
    private static final Logger LOG = Logger.getLogger(StorableObject.class.getName());

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
     * The dictionary key for the most recent object activation time.
     * The value is stored as a date object. Note that this is always
     * stored as a computed property, so it will not be written to
     * storage.
     */
    public static final String KEY_ACTIVATED_TIME = "activatedTime";

    /**
     * The prefix for hidden dictionary keys. These will not be returned
     * by API calls, but will be written to storage.
     */
    public static final String PREFIX_HIDDEN = ".";

    /**
     * The prefix for computed dictionary keys. These will not be written
     * to persistent storage, but may be returned from API calls.
     */
    public static final String PREFIX_COMPUTED = "_";

    /**
     * The dictionary containing the serializable data for this
     * object.
     */
    protected Dict dict = null;

    /**
     * Serializes an object and recursively removes hidden and computed
     * keys. This is typically performed before returning a storable
     * object via API or when writing to storage.
     *
     * @param obj            the object to sterilize
     * @param skipHidden     filter out hidden key-value pairs
     * @param skipComputed   filter out computed key-value pairs
     * @param limitedTypes   limit allowed object value types
     *
     * @return the sterilized object
     */
    public static Object sterilize(Object obj,
                                   boolean skipHidden,
                                   boolean skipComputed,
                                   boolean limitedTypes) {
        if (obj == null ||
            obj instanceof Boolean ||
            obj instanceof Number ||
            obj instanceof String ||
            obj instanceof Date) {
            return obj;
        } else if (obj instanceof Dict src) {
            Dict dst = new Dict();
            for (String k : src.keys()) {
                boolean skip = (
                    (skipHidden && k.startsWith(PREFIX_HIDDEN)) ||
                    (skipComputed && k.startsWith(PREFIX_COMPUTED))
                );
                if (!skip) {
                    dst.set(k, sterilize(src.get(k), skipHidden, skipComputed, limitedTypes));
                };
            }
            return dst;
        } else if (obj instanceof Array a) {
            return Array.from(
                a.stream().map(o -> sterilize(o, skipHidden, skipComputed, limitedTypes))
            );
        } else if (obj instanceof StorableObject o) {
            return sterilize(o.serialize(), skipHidden, skipComputed, limitedTypes);
        } else {
            return limitedTypes ? obj.toString() : obj;
        }
    }

    /**
     * Creates a new object. This is the default constructor for
     * creating storable object instances.
     *
     * @param id             the object identifier
     * @param type           the type name
     */
    protected StorableObject(String id, String type) {
        this.dict = new Dict().set(KEY_ID, id).set(KEY_TYPE, type);
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
        this(id, type);
        this.dict.setAll(dict);
        if (!Objects.equals(id, dict.get(KEY_ID, String.class))) {
            LOG.warning("invalid " + type + "/" + id + " 'id' value: " + dict.get(KEY_ID));
            this.dict.set(KEY_ID, id);
        }
        String dictType = dict.get(KEY_TYPE, String.class);
        if (!Objects.requireNonNullElse(dictType, "").startsWith(type)) {
            LOG.warning("invalid " + type + "/" + id + " 'type' value: " + dict.get(KEY_TYPE));
            this.dict.set(KEY_TYPE, type);
        }
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return StringUtils.split(type(), "/")[0] + " " + id();
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
        return dict.get(KEY_ID, String.class, "");
    }

    /**
     * Returns the object type name.
     *
     * @return the object type name
     */
    public String type() {
        return dict.get(KEY_TYPE, String.class, "");
    }

    /**
     * Returns the (recommended) object storage path. The storage
     * path is created by concatenating the object type and id.
     *
     * @return the object storage path
     */
    public Path path() {
        String dir = StringUtils.split(type(), "/")[0];
        return Path.from(dir + "/" + id());
    }

    /**
     * Returns the timestamp of the latest object activation. This is
     * updated each time the object is fetched from storage.
     *
     * @return the timestamp of the latest object activation, or
     *         null if not activated
     */
    protected Date activatedTime() {
        return dict.get(dictKey(KEY_ACTIVATED_TIME), Date.class);
    }

    /**
     * Finds the dictionary key to use. Tests for the existence of
     * either computed or hidden prefixes to the key. If none were
     * found, the input key is returned unmodified.
     *
     * @param key            the dictionary key name
     *
     * @return the dictionary key to use
     */
    protected String dictKey(String key) {
        if (dict.containsKey(PREFIX_COMPUTED + key)) {
            return PREFIX_COMPUTED + key;
        } else if (dict.containsKey(PREFIX_HIDDEN + key)) {
            return PREFIX_HIDDEN + key;
        } else {
            return key;
        }
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
        dict.set(PREFIX_COMPUTED + KEY_ACTIVATED_TIME, new Date());
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
