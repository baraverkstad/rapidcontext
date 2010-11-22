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

package org.rapidcontext.core.env;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.RootStorage;
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
     * The dictionary key for the initializer class name.
     */
    public static final String KEY_INITIALIZER = "initializer";

    /**
     * The connection object storage path.
     */
    public static final Path PATH_TYPE = new Path("/type/");

    /**
     * Initializes all type mappings found in the storage.
     *
     * @param storage        the root storage
     */
    public static void initAll(RootStorage storage) {
        try {
            Storage.registerInitializer("type", Type.class);
        } catch (StorageException e) {
            LOG.log(Level.SEVERE, "failed to set type initializer in storage", e);
        }
        storage.loadAll(PATH_TYPE);
    }

    /**
     * Removes all type mappings found in the storage.
     *
     * @param storage        the root storage
     */
    public static void destroyAll(RootStorage storage) {
        storage.flush(PATH_TYPE);
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
        Class  cls = initializer();

        if (cls == null) {
            throw new StorageException("missing initializer for " + this);
        }
        Storage.registerInitializer(id(), cls);
    }

    /**
     * Destroys this type mapping by unregistering it.
     */
    protected void destroy() {
        Storage.unregisterInitializer(id());
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
}
