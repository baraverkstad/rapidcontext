/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2012 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.type;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.StorageException;

/**
 * The object type initializer. This class maps type and Java class
 * mappings found in the storage to the actual storage type registry.
 * Since this type mapping class is itself uses the storage object
 * initialization feature, it will register itself twice (once for
 * bootstrapping and once from the proper storage file).
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Type extends StorableObject {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(Type.class.getName());

    /**
     * The dictionary key for the description text.
     */
    public static final String KEY_DESCRIPTION = "description";

    /**
     * The dictionary key for the initializer class name.
     */
    public static final String KEY_INITIALIZER = "initializer";

    /**
     * The connection object storage path.
     */
    public static final Path PATH = new Path("/type/");

    /**
     * The storable object initializer constructor arguments.
     */
    private static final Class[] CONSTRUCTOR_ARGS = new Class[] {
        String.class, String.class, Dict.class
    };

    /**
     * Searches for a specific type in the storage.
     *
     * @param storage        the storage to search in
     * @param id             the type identifier
     *
     * @return the type found, or
     *         null if not found
     */
    public static Type find(Storage storage, String id) {
        Object obj = storage.load(PATH.descendant(new Path(id)));
        return (obj instanceof Type) ? (Type) obj : null;
    }

    /**
     * Searches for all types in the storage.
     *
     * @param storage        the storage to search in
     *
     * @return an array of all types found
     */
    public static Type[] findAll(Storage storage) {
        Object[] objs = storage.loadAll(PATH);
        ArrayList list = new ArrayList(objs.length);
        for (int i = 0; i < objs.length; i++) {
            if (objs[i] instanceof Type) {
                list.add(objs[i]);
            }
        }
        return (Type[]) list.toArray(new Type[list.size()]);
    }

    /**
     * Returns a constructor for creating a Java object instance. If
     * no object type or initializer was found, or if an error
     * occurred, null is returned. This method will lookup the
     * corresponding type in the storage dynamically.
     *
     * @param storage        the storage to use for type lookups
     * @param dict           the dictionary data
     *
     * @return the Java object constructor, or
     *         null if not found
     */
    public static Constructor constructor(Storage storage, Dict dict) {
        String          typeId = dict.getString(KEY_TYPE, null);
        Type            type;
        Class           cls = null;
        String          msg;

        if (typeId == null) {
            return null;
        } else if (typeId.equals("type")) {
            cls = Type.class;
        } else {
            type = find(storage, typeId);
            if (type != null) {
                cls = type.initializer();
            }
        }
        if (cls != null) {
            try {
                return cls.getConstructor(CONSTRUCTOR_ARGS);
            } catch (Exception e) {
                msg = "invalid initializer class for type " + typeId +
                      ": no constructor " + cls.getName() +
                      "(String, String, Dict) found";
                LOG.log(Level.WARNING, msg, e);
            }
        }
        return null;
    }

    /**
     * Creates a new type mapping from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public Type(String id, String type, Dict dict) {
        super(id, type, dict);
    }

    /**
     * Initializes this type mapping by registering it via the
     * methods in the Storage class.
     *
     * @throws StorageException if the initialization failed
     */
    protected void init() throws StorageException {
        Class   cls = initializer();
        String  msg;

        if (cls != null) {
            if (!StorableObject.class.isAssignableFrom(cls)) {
                msg = "invalid initializer class for " + this + ": class " +
                      cls.getName() + " is not a subclass of StorableObject";
                LOG.warning(msg);
                throw new StorageException(msg);
            }
            try {
                cls.getConstructor(CONSTRUCTOR_ARGS);
            } catch (Exception e) {
                msg = "invalid initializer class for " + this +
                      ": no constructor " + cls.getName() +
                      "(String, String, Dict) found";
                LOG.warning(msg);
                throw new StorageException(msg);
            }
        }
    }

    /**
     * Returns the type description.
     *
     * @return the type description
     */
    public String description() {
        return dict.getString(KEY_DESCRIPTION, "");
    }

    /**
     * Returns the type initializer class.
     *
     * @return the type initializer class
     */
    public Class initializer() {
        ClassLoader  loader = ApplicationContext.getInstance().getClassLoader();
        String       className = dict.getString(KEY_INITIALIZER, null);
        String       msg;

        try {
            return (className == null) ? null : loader.loadClass(className);
        } catch (Exception e) {
            msg = "couldn't find or load " + this + " initializer class " +
                  className;
            LOG.warning(msg);
            return null;
        }
    }

    /**
     * Returns an array of type properties. Each property should be a
     * dictionary object containing property information. Note that
     * parent type properties are normally also applicable, but must
     * be retrieved separately.
     *
     * @return the array of type properties, or
     *         an empty array if it didn't exist
     */
    public Array properties() {
        Array   arr = null;
        String  msg;

        try {
            arr = dict.getArray("property");
        } catch (ClassCastException e) {
            msg = this + " contains 'property' attribute that isn't a proper array";
            LOG.warning(msg);
        }
        return (arr == null) ? new Array(0) : arr;
    }

    /**
     * Searches for the parent type in the type hierarchy. A parent
     * type normally declares additional properties that may or may
     * not be applicable also for this type.
     *
     * @param storage        the storage to search in
     *
     * @return the parent type, or
     *         null if not found
     */
    public Type parentType(Storage storage) {
        String[]  parts = id().split("/");
        Type      type = null;

        for (int i = parts.length - 1; i > 0; i++) {
            type = find(storage, StringUtils.join(parts, '/', 0, i));
            if (type != null) {
                return type;
            }
        }
        return null;
    }
}
