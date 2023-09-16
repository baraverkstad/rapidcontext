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

package org.rapidcontext.core.type;

import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
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
     * The dictionary key for the optional legacy type id.
     */
    public static final String KEY_ALIAS = "alias";

    /**
     * The dictionary key for the description text.
     */
    public static final String KEY_DESCRIPTION = "description";

    /**
     * The dictionary key for the remote-only flag.
     */
    public static final String KEY_REMOTE = "remote";

    /**
     * The dictionary key for the initializer class name.
     */
    public static final String KEY_INITIALIZER = "initializer";

    /**
     * The dictionary key for the property array.
     */
    public static final String KEY_PROPERTY = "property";

    /**
     * The connection object storage path.
     */
    public static final Path PATH = Path.from("/type/");

    /**
     * The storable object initializer constructor arguments.
     */
    private static final Class<?>[] CONSTRUCTOR_ARGS = new Class<?>[] {
        String.class, String.class, Dict.class
    };

    /**
     * Returns a stream of all types found in the storage.
     *
     * @param storage        the storage to search
     *
     * @return a stream of type instances found
     */
    public static Stream<Type> all(Storage storage) {
        return storage.query(PATH).objects(Type.class);
    }

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
        Type res = storage.load(Path.resolve(PATH, id), Type.class);
        if (res != null) {
            return res;
        } else {
            return all(storage).filter(t -> t.alias().equals(id)).findFirst().orElse(null);
        }
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
    public static Constructor<?> constructor(Storage storage, Dict dict) {
        String typeId = dict.getString(KEY_TYPE, null);
        String className = dict.getString(KEY_CLASSNAME, null);
        Class<?> cls = null;
        if (typeId != null && typeId.equals("type")) {
            cls = Type.class;
        } else if (typeId != null) {
            Type type = find(storage, typeId);
            boolean instantiable = type != null && !type.remote();
            if (instantiable && className != null) {
                cls = loadClass(className, typeId + " " + dict.get(KEY_ID));
            } else if (instantiable) {
                cls = type.initializer();
            }
        }
        if (cls != null) {
            try {
                return cls.getConstructor(CONSTRUCTOR_ARGS);
            } catch (Exception e) {
                try {
                    Constructor<?> ctor = cls.getConstructor(new Class<?>[] { Dict.class });
                    LOG.warning("deprecated: " + typeId + " initializer missing " +
                                cls.getName() + "(String, String, Dict) constructor");
                    return ctor;
                } catch (Exception ex) {
                    LOG.warning("invalid " + typeId + "initializer: missing " +
                                cls.getName() + "(String, String, Dict) constructor");
                }
            }
        }
        return null;
    }

    /**
     * Loads and returns a specified class.
     *
     * @param className      the fully qualified class name to load
     * @param objId          the object identifier for logging
     *
     * @return the class found in the class loader, or
     *         null if not found
     */
    protected static Class<?> loadClass(String className, String objId) {
        ClassLoader loader = ApplicationContext.getInstance().getClassLoader();
        try {
            return (className == null) ? null : loader.loadClass(className);
        } catch (Exception e) {
            String msg = "couldn't find or load " + objId +
                         " initializer class " + className;
            LOG.log(Level.WARNING, msg, e);
            return null;
        }
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
        this.dict.set(KEY_PROPERTY, properties());
    }

    /**
     * Initializes this type mapping by registering it via the
     * methods in the Storage class.
     *
     * @throws StorageException if the initialization failed
     */
    protected void init() throws StorageException {
        Class<?> cls = initializer();
        if (cls != null) {
            if (!StorableObject.class.isAssignableFrom(cls)) {
                String msg = "invalid initializer class for " + this +
                             ": class " + cls.getName() +
                             " is not a subclass of StorableObject";
                LOG.warning(msg);
                throw new StorageException(msg);
            }
            try {
                cls.getConstructor(CONSTRUCTOR_ARGS);
            } catch (Exception e) {
                String msg = "invalid initializer class for " + this +
                             ": no constructor " + cls.getName() +
                             "(String, String, Dict) found";
                LOG.warning(msg);
                throw new StorageException(msg);
            }
        }
    }

    /**
     * Returns the optional legacy type id (or alias).
     *
     * @return the type alias, or an empty string
     */
    public String alias() {
        return dict.getString(KEY_ALIAS, "");
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
     * Returns the remote-only flag. Remote objects are only initialized
     * client-side, so no corresponding Java class exist.
     *
     * @return true if objects are remote-only, or
     *         false otherwise (default)
     */
    public boolean remote() {
        return dict.get(KEY_REMOTE, Boolean.class, false);
    }

    /**
     * Returns the type initializer class.
     *
     * @return the type initializer class
     */
    public Class<?> initializer() {
        return loadClass(dict.getString(KEY_INITIALIZER, null), toString());
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
        try {
            return dict.getArray(KEY_PROPERTY);
        } catch (ClassCastException e) {
            String msg = this + " contains 'property' attribute that " +
                         "isn't a proper array";
            LOG.warning(msg);
            return new Array(0);
        }
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
        String[] parts = id().split("/");
        for (int i = parts.length - 1; i > 0; i++) {
            Type type = find(storage, StringUtils.join(parts, '/', 0, i));
            if (type != null) {
                return type;
            }
        }
        return null;
    }
}
