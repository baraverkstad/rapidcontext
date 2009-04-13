/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2009 Per Cederberg & Dynabyte AB.
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

import java.util.HashMap;
import java.util.logging.Logger;

/**
 * The global adapter registry.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class AdapterRegistry {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(AdapterRegistry.class.getName());

    /**
     * The map of adapter instances.
     */
    private static HashMap adapters = new HashMap();

    /**
     * Registers an adapter instance.
     *
     * @param id             the unique adapter identifier
     * @param adapter        the adapter instance to register
     *
     * @throws AdapterException if the adapter failed to initialize
     */
    public static void register(String id, Adapter adapter) throws AdapterException {
        adapters.put(id, adapter);
        adapter.init();
    }

    /**
     * Unregisters an adapter instance.
     *
     * @param id             the unique adapter identifier
     *
     * @throws AdapterException if the adapter failed to deinitialize
     */
    public static void unregister(String id) throws AdapterException {
        unregister(id, find(id));
    }

    /**
     * Unregisters an adapter instance.
     *
     * @param id             the unique adapter identifier
     * @param adapter        the adapter instance to unregister
     *
     * @throws AdapterException if the adapter failed to uninitialize
     */
    public static void unregister(String id, Adapter adapter) throws AdapterException {
        if (adapter != null) {
            adapter.destroy();
        }
        adapters.remove(id);
    }

    /**
     * Unregisters all adapter instances in this registry.
     */
    public static void unregisterAll() {
        String[]  ids = findAll();

        for (int i = 0; i < ids.length; i++) {
            try {
                unregister(ids[i]);
            } catch (AdapterException e) {
                LOG.severe("failed to unregister " + ids[i] + "adapter: " +
                           e.getMessage());
            }
        }
    }

    /**
     * Searches for a registered adapter instance.
     *
     * @param id             the unique adapter identifier
     *
     * @return the adapter instance found, or
     *         null if not found
     */
    public static Adapter find(String id) {
        return (Adapter) adapters.get(id);
    }

    /**
     * Returns an array with all registered adapter identifiers.
     *
     * @return an array with all registered adapter identifiers
     */
    public static String[] findAll() {
        String[]  ids;

        ids = new String[adapters.size()];
        adapters.keySet().toArray(ids);
        return ids;
    }
}
