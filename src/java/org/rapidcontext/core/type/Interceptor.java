/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2026 Per Cederberg. All rights reserved.
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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;

/**
 * A system call interceptor. This generic type only provides the basic
 * mechanism of loading and caching an interceptor chain.
 *
 * @author Per Cederberg
 */
public class Interceptor extends StorableObject {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(Interceptor.class.getName());

    /**
     * The dictionary key for the description.
     */
    public static final String KEY_DESCRIPTION = "description";

    /**
     * The dictionary key for the ordering priority.
     */
    public static final String KEY_PRIO = "prio";

    /**
     * The interceptor object storage path.
     */
    public static final Path PATH = Path.from("/interceptor/");

    /**
     * Cache for interceptor chains, keyed by interceptor type class.
     */
    private static Map<Class<?>, Interceptor> cache =
        new HashMap<>();

    /**
     * The next interceptor in the chain. This is supposed to be a non-null
     * value for all but the default interceptor.
     */
    private Interceptor next = null;

    /**
     * Initializes the interceptor chain cache from the storage. This
     * is intended to be called once during application initialization.
     *
     * @param storage        the storage to use
     */
    public static void init(Storage storage) {
        Map<Class<?>, Interceptor> newCache = new HashMap<>();
        Type.find(storage, "interceptor")
            .subTypes(storage)
            .forEach(t -> {
                Class<?> clazz = t.initializer();
                Interceptor interceptor = storage.query(PATH)
                    .objects(Interceptor.class)
                    .filter(i -> clazz.isInstance(i))
                    .sorted(Comparator.comparingInt(Interceptor::prio).reversed())
                    .reduce((prev, curr) -> curr.setNext(prev))
                    .orElse(null);
                newCache.put(clazz, interceptor);
                LOG.fine("initialized " + t.id() + " chain; " + ((interceptor == null) ? "null" : interceptor.chain()));
            });
        cache = newCache;
    }

    /**
     * Returns the interceptor chain for a given class.
     *
     * @param <T>            the interceptor type to return
     * @param clazz          the interceptor class
     *
     * @return the interceptor chain, or
     *         null if no interceptor chain is found
     */
    @SuppressWarnings("unchecked")
    public static <T extends Interceptor> T get(Class<T> clazz) {
        Interceptor res = cache.get(clazz);
        return clazz.isInstance(res) ? clazz.cast(res) : null;
    }

    /**
     * Creates a new interceptor from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public Interceptor(String id, String type, Dict dict) {
        super(id, type, dict);
    }

    /**
     * Returns the interceptor description.
     *
     * @return the interceptor description.
     */
    public String description() {
        return dict.get(KEY_DESCRIPTION, String.class, "");
    }

    /**
     * Returns the ordering priority.
     *
     * @return the ordering priority
     */
    public int prio() {
        return dict.get(KEY_PRIO, Integer.class, 50);
    }

    /**
     * Returns the next interceptor in the chain.
     *
     * @return the next interceptor, or
     *         null if no next is available
     */
    public Interceptor next() {
        return next;
    }

    /**
     * Returns the next interceptor in the chain.
     *
     * @param <T>            the interceptor type to return
     * @param clazz          the interceptor class
     *
     * @return the next interceptor, or
     *         null if no next is available
     */
    public <T extends Interceptor> T next(Class<T> clazz) {
        return clazz.isInstance(next) ? clazz.cast(next) : null;
    }

    /**
     * Sets the next interceptor in the chain.
     *
     * @param <T>            the interceptor type
     * @param next           the next interceptor
     *
     * @return this interceptor
     */
    protected <T extends Interceptor> Interceptor setNext(T next) {
        this.next = next;
        return this;
    }

    /**
     * Returns a string representation of the interceptor chain.
     *
     * @return a string representation of the interceptor chain
     */
    protected String chain() {
        return id() + ((next == null) ? "" : " -> " + next.chain());
    }
}
