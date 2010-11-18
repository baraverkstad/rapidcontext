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
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.Metadata;
import org.rapidcontext.core.data.Path;
import org.rapidcontext.core.data.Storage;

/**
 * An external connectivity environment. The environment contains a
 * list of adapter connection pool, each with their own set of
 * configuration parameter values.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Environment {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(Environment.class.getName());

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
     * The map of environments. The environments are indexed by name.
     */
    private static LinkedHashMap environments = new LinkedHashMap();

    /**
     * The environment name.
     */
    private String name;

    /**
     * The environment description.
     */
    private String description;

    /**
     * The list of connection pools. The pools are indexed by name.
     */
    private LinkedHashMap pools = new LinkedHashMap();

    /**
     * Initializes all environments and connections found in the
     * storage.
     *
     * @param storage        the data storage to use
     *
     * @return one of the loaded environments, or
     *         null if no environments could be found
     */
    public static Environment init(Storage storage) {
        Metadata[]   metas;
        Object       obj;
        Dict         dict;
        String       name;
        Environment  env = null;

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

        // Initialize environment
        metas = storage.lookupAll(PATH_ENV);
        for (int i = 0; i < metas.length; i++) {
            obj = storage.load(metas[i].path());
            if (obj instanceof Dict) {
                dict = (Dict) obj;
                name = metas[i].path().subPath(PATH_ENV.length()).toString();
                name = StringUtils.removeStart(name, "/");
                env = initEnv(name, dict);
            }
        }

        // TODO: Remove the single environment reference
        return env;
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
     * Initializes a new environment from the specified name and
     * dictionary.
     *
     * @param name           the environment name
     * @param dict           the configuration dictionary
     *
     * @return the environment created, or
     *         null if it was invalid
     */
    protected static Environment initEnv(String name, Dict dict) {
        Environment  env = null;
        String       type;
        String       prefix;
        String       msg;

        type = dict.getString("type", "environment");
        if (!type.equals("environment")) {
            msg = "invalid object type for environment/" + name + ": " + type;
            LOG.warning(msg);
        } else {
            env = new Environment(name, dict.getString("description", ""));
            prefix = dict.getString("connections", null);
            if (prefix != null) {
                Iterator iter = connections.keySet().iterator();
                while (iter.hasNext()) {
                    name = iter.next().toString();
                    if (name.startsWith(prefix)) {
                        env.addPool((Pool) connections.get(name));
                    }
                }
            }
            environments.put(name, env);
        }
        // TODO: Don't return environment
        return env;
    }

    /**
     * Destroys all loaded environments and connections. This will
     * free all resources currently used.
     */
    public static void destroy() {
        Iterator  iter;

        iter = connections.values().iterator();
        while (iter.hasNext()) {
            ((Pool) iter.next()).close();
        }
        connections.clear();
        environments.clear();
    }

    /**
     * Returns a named environment.
     *
     * @param envName        the environment name
     *
     * @return the environment found, or
     *         null if not found
     */
    public static Environment environment(String envName) {
        return (Environment) environments.get(envName);
    }

    /**
     * Returns a collection with all the environment names.
     *
     * @return a collection with all the environment names
     */
    public static Collection environmentNames() {
        return environments.keySet();
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
            res = poolAlias(connections.values(), poolName);
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
     * @param pools          the pools to search
     * @param poolName       the pool name to search for
     *
     * @return the connection pool found, or
     *         null if not found
     */
    protected static Pool poolAlias(Collection pools, String poolName) {
        Iterator  iter = pools.iterator();
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
     * Creates a new environment with the specified name and
     * description.
     *
     * @param name           the name to use
     * @param description    the description to use
     */
    public Environment(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Returns a string representation of this environment.
     *
     * @return a string representation of this environment
     */
    public String toString() {
        return name;
    }

    /**
     * Returns the environment name.
     *
     * @return the environment name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the environment name.
     *
     * @param name           the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the environment description.
     *
     * @return the environment description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the environment description.
     *
     * @param description    the new description
     */
    public void setDescription(String description) {
        this.description = description;
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
        Pool res = (Pool) connections.get(poolName);

        if (res == null) {
            res = poolAlias(connections.values(), poolName);
        }
        if (res == null) {
            res = pool(poolName);
        }
        return res;
    }

    /**
     * Adds a connection pool to the environment. Any existing pool
     * with the specified name will first be removed.
     *
     * @param pool           the connection pool to add
     */
    protected void addPool(Pool pool) {
        pools.put(pool.getName(), pool);
    }

    /**
     * Removes a connection pool from the environment. This will
     * close the connection pool and free any resources currently
     * used by the pool, such as any open connections or similar.
     *
     * @param poolName       the connection pool name.
     */
    protected void removePool(String poolName) {
        pools.remove(poolName);
    }
}
