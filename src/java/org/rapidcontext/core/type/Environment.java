/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2024 Per Cederberg. All rights reserved.
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

import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Path;
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
    public static final Path PATH = Path.from("/environment/");

    /**
     * Returns a stream of all environments found in the storage.
     *
     * @param storage        the storage to search
     *
     * @return a stream of environment instances found
     */
    public static Stream<Environment> all(Storage storage) {
        return storage.query(PATH).objects(Environment.class);
    }

    /**
     * Searches for a specific environment in the storage.
     *
     * @param storage        the storage to search in
     * @param id             the environment identifier
     *
     * @return the environment found, or
     *         null if not found
     */
    public static Environment find(Storage storage, String id) {
        return storage.load(Path.resolve(PATH, id), Environment.class);
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
        return dict.get(KEY_DESCRIPTION, String.class, "");
    }

    /**
     * Returns the default connection path for this environment.
     *
     * @return the default connection path for this environment
     */
    public String connectionPath() {
        String prefix;

        prefix = dict.get(KEY_CONNECTIONS, String.class);
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
