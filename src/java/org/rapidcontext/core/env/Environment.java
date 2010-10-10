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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
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
     * The environment object storage path.
     */
    public static final Path PATH_ENV = new Path("/environment/");

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
     * Creates an environment from the first environment found.
     *
     * @param storage        the data storage to use
     *
     * @return the loaded environment, or
     *         null if no environment could be found
     *
     * @throws EnvironmentException if the environment couldn't be
     *             loaded successfully
     */
    public static Environment init(Storage storage) throws EnvironmentException {
        Object[]     objs;
        Dict         data;
        Array        list;
        Dict         pool;
        Environment  env;
        Adapter      adapter;
        Dict         params;
        String       poolName;
        String       str;

        objs = storage.loadAll(PATH_ENV);
        if (objs.length <= 0) {
            return null;
        }
        data = (Dict) objs[0];
        if (data.getString("name", null) == null) {
            str = "failed to find required environment property 'name'";
            LOG.warning(str);
            throw new EnvironmentException(str);
        }
        env = new Environment(data.getString("name", ""),
                              data.getString("description", ""));
        list = data.getArray("pool");
        for (int i = 0; list != null && i < list.size(); i++) {
            pool = list.getDict(i);
            poolName = pool.getString("name", null);
            if (poolName != null) {
                str = pool.getString("adapter", "");
                adapter = AdapterRegistry.find(str);
                if (adapter == null) {
                    str = "failed to create pool '" + poolName +
                          "' in environment '" + env.getName() + "': " +
                          "no adapter '" + str + "' found";
                    LOG.warning(str);
                    throw new EnvironmentException(str);
                }
                params = pool.getDict("param");
                try {
                    env.addPool(new Pool(poolName, adapter, params));
                } catch (AdapterException e) {
                    str = "failed to create pool '" + poolName +
                          "' in environment '" + env.getName() + "': " +
                          e.getMessage();
                    LOG.warning(str);
                    throw new EnvironmentException(str);
                }
            }
        }
        return env;
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
     * Returns a collection with all the pool names.
     *
     * @return a collection with all the pool names
     */
    public Collection getPoolNames() {
        return pools.keySet();
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
        return (Pool) pools.get(poolName);
    }

    /**
     * Adds a connection pool to the environment. Any existing pool
     * with the specified name will first be removed.
     *
     * @param pool           the connection pool to add
     */
    public void addPool(Pool pool) {
        removePool(pool.getName());
        pools.put(pool.getName(), pool);
    }

    /**
     * Removes a connection pool from the environment. This will
     * close the connection pool and free any resources currently
     * used by the pool, such as any open connections or similar.
     *
     * @param poolName       the connection pool name.
     */
    public void removePool(String poolName) {
        Pool  pool;

        pool = findPool(poolName);
        pools.remove(poolName);
        if (pool != null) {
            pool.close();
        }
    }

    /**
     * Removes all pools from the environment. This will free all
     * resources currently used by this environment, such as open
     * connections or similar.
     */
    public void removeAllPools() {
        ArrayList  list = new ArrayList();

        list.addAll(pools.keySet());
        for (int i = 0; i < list.size(); i++) {
            removePool(list.get(i).toString());
        }
    }
}
