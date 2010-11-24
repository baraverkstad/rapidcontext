/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg & Dynabyte AB.
 * All rights reserved.
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

import org.apache.commons.lang.StringUtils;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.RootStorage;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;

/**
 * An external connectivity environment. The environment contains a
 * list of adapter connection pool, each with their own set of
 * configuration parameter values.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Environment extends StorableObject {

    /**
     * The dictionary key for the environment description.
     */
    public static final String KEY_DESCRIPTION = "description";

    /**
     * The dictionary key for the environment connection path. The
     * value stored will be a path object.
     */
    public static final String KEY_CONNECTIONS = "connections";

    /**
     * The environment object storage path.
     */
    public static final Path PATH_ENV = new Path("/environment/");

    /**
     * Initializes all environments and connections found in the
     * storage.
     *
     * @param storage        the data storage to use
     *
     * @return one of the loaded environments, or
     *         null if no environments could be found
     */
    public static Environment initAll(RootStorage storage) {
        Object[]  objs;

        // TODO: remove this initialization entirely, shouldn't be needed!

        // Initialize environments
        objs = storage.loadAll(PATH_ENV);

        // TODO: Remove the single environment reference
        if (objs.length > 0 && objs[0] instanceof Environment) {
            return (Environment) objs[0];
        } else {
            return null;
        }
    }

    /**
     * Creates a new environment from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public Environment(String id, String type, Dict dict) {
        super(id, type, dict);
    }

    /**
     * Returns the environment description.
     *
     * @return the environment description.
     */
    public String description() {
        return dict.getString(KEY_DESCRIPTION, "");
    }

    /**
     * Returns the default connection path for this environment.
     *
     * @return the default connection path for this environment
     */
    public String connectionPath() {
        String prefix;

        prefix = dict.getString(KEY_CONNECTIONS, null);
        if (prefix != null) {
            prefix = StringUtils.removeStart(prefix, "/");
            if (!prefix.endsWith("/")) {
                prefix += "/";
            }
        }
        return prefix;
    }

    /**
     * Searches for a connection with the specified id.
     *
     * @param storage        the storage to search in
     * @param id             the connection id to search for
     *
     * @return the connection found, or
     *         null if not found
     */
    public Connection findConnection(Storage storage, String id) {
        String      prefix = connectionPath();
        Connection  res = null;

        if (prefix != null) {
            res = Connection.find(storage, prefix + id);
        }
        if (res == null) {
            res = Connection.find(storage, id);
        }
        return res;
    }
}
