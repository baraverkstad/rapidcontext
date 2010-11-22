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

package org.rapidcontext.core.env;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.StorageException;

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
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(Environment.class.getName());

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
     * The connection object storage path.
     */
    public static final Path PATH_CON = new Path("/connection/");

    /**
     * The environment object storage path.
     */
    public static final Path PATH_ENV = new Path("/environment/");

    /**
     * The map of connection pools. The pools are indexed by name.
     */
    private static LinkedHashMap connections = new LinkedHashMap();

    /**
     * Initializes all environments and connections found in the
     * storage.
     *
     * @param storage        the data storage to use
     *
     * @return one of the loaded environments, or
     *         null if no environments could be found
     */
    public static Environment initAll(Storage storage) {
        Metadata[]   metas;
        Object       obj;
        Dict         dict;
        String       name;

        // Initialize connections
        metas = storage.lookupAll(PATH_CON);
        for (int i = 0; i < metas.length; i++) {
            obj = storage.load(metas[i].path());
            if (obj instanceof Dict) {
                dict = (Dict) obj;
                name = metas[i].path().subPath(PATH_CON.length()).toString();
                name = StringUtils.removeStart(name, "/");
                initPool(name, dict);
            }
        }

        // Initialize environments
        try {
            Storage.registerInitializer("environment", Environment.class);
        } catch (StorageException e) {
            LOG.log(Level.SEVERE, "failed to set environment initializer in storage", e);
        }
        Object[] envs = storage.loadAll(PATH_ENV);

        // TODO: Remove the single environment reference
        if (envs.length > 0 && envs[0] instanceof Environment) {
            return (Environment) envs[0];
        } else {
            return null;
        }
    }

    /**
     * Initializes a new connection pool from the specified name
     * and configuration dictionary.
     *
     * @param name           the connection pool name
     * @param dict           the configuration dictionary
     */
    protected static void initPool(String name, Dict dict) {
        String   type;
        Adapter  adapter;
        Dict     config;
        String   msg;

        // TODO: add support for connection aliases
        type = dict.getString("type", "connection");
        adapter = AdapterRegistry.find(dict.getString("adapter", ""));
        config = dict.getDict("config");
        if (config == null) {
            config = new Dict();
        }
        if (!type.equals("connection")) {
            msg = "invalid object type for connection/" + name + ": " + type;
            LOG.warning(msg);
        } else if (adapter == null) {
            msg = "failed to create connection " + name + ": no adapter '" +
                  dict.getString("adapter", "") + "' found";
            LOG.warning(msg);
        } else {
            try {
                connections.put(name, new Pool(name, adapter, config));
            } catch (AdapterException e) {
                msg = "failed to create connection " + name + ": " +
                      e.getMessage();
                LOG.warning(msg);
            }
        }
    }

    /**
     * Destroys all loaded environments and connections. This will
     * free all resources currently used.
     */
    public static void destroyAll() {
        Iterator  iter;

        iter = connections.values().iterator();
        while (iter.hasNext()) {
            ((Pool) iter.next()).close();
        }
        connections.clear();
    }

    /**
     * Returns a named connection pool.
     *
     * @param poolName       the connection pool name
     *
     * @return the connection pool found, or
     *         null if not found
     */
    public static Pool pool(String poolName) {
        Pool res = (Pool) connections.get(poolName);

        if (res == null) {
            res = poolAlias(poolName);
        }
        return res;
    }

    /**
     * Returns a collection with all the connection pool names.
     *
     * @return a collection with all the connection pool names
     */
    public static Collection poolNames() {
        return connections.keySet();
    }

    /**
     * Searches for a connection pool with a matching alias.
     *
     * @param poolName       the pool name to search for
     *
     * @return the connection pool found, or
     *         null if not found
     */
    protected static Pool poolAlias(String poolName) {
        Iterator  iter = connections.values().iterator();
        Pool      p;

        while (iter.hasNext()) {
            p = (Pool) iter.next();
            if (p.hasName(poolName)) {
                return p;
            }
        }
        return null;
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
     * Searches for a connection pool with the specified name.
     *
     * @param poolName       the pool name to search for
     *
     * @return the connection pool found, or
     *         null if not found
     */
    public Pool findPool(String poolName) {
        String  prefix = connectionPath();
        Pool    res = null;

        if (prefix != null) {
            res = (Pool) connections.get(prefix + poolName);
        }
        if (res == null) {
            res = poolAlias(prefix + poolName);
        }
        if (res == null) {
            res = pool(poolName);
        }
        return res;
    }
}
