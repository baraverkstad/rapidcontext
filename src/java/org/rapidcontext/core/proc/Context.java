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

package org.rapidcontext.core.proc;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.Supplier;

import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.User;

/**
 * The execution context. This provides access to settings, storage,
 * current user, arguments, etc. Each context links to a parent context,
 * creating a hierarchy that allows adding or changing attributes for a
 * particular sub-context (i.e. procedure call).
 *
 * An execution thread can only have a single active context at a time,
 * which is managed by thread-local storage in this class. The shared
 * root context is NOT thread-local in order to be replaced via dynamic
 * reload, etc.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Context implements AutoCloseable {
    // FIXME: How to inspect current context from another thread? Java reflection?

    /**
     * The storage context attribute.
     */
    public static String CX_STORAGE = "storage";

    /**
     * The user context attribute.
     */
    public static String CX_USER = "user";

    /**
     * The shared root context at the base of the hierarchy.
     */
    protected static Context root = null;

    /**
     * The currently active thread-local context.
     */
    protected static ThreadLocal<Context> active = new ThreadLocal<>();

    /**
     * Returns the currently active context. If no thread-local context
     * is available, the root context is returned.
     *
     * @return the current context
     */
    @SuppressWarnings("resource")
    public static Context get() {
        return Objects.requireNonNullElse(active.get(), root);
    }

    /**
     * The context identifier (for stack traces, etc).
     */
    protected String id;

    /**
     * The parent context (if available).
     */
    protected Context parent = null;

    /**
     * The map of attributes. Attributes can be used for storing generic
     * objects in the context, which is useful in some situations.
     */
    protected HashMap<String,Object> attributes = new HashMap<>();

    /**
     * Creates a new context and sets it as the currently active context.
     * The previously active context is set as the parent of this context.
     *
     * @param id             the context identifier (name)
     */
    public Context(String id) {
        this.id = id;
        this.parent = get();
        if (!isShared()) {
            active.set(this);
        }
    }

    /**
     * Closes the current context sets its parent as the active context.
     * Also releases all resources in preparation for garbage collection.
     */
    @Override
    @SuppressWarnings("resource")
    public void close() {
        if (this == active.get()) {
            if (parent != null && !parent.isShared()) {
                active.set(parent);
            } else {
                active.remove();
            }
            parent = null;
        }
        if (attributes != null) {
            attributes.clear();
            attributes = null;
        }
    }

    /**
     * Checks if this context is shared (i.e. if it is thread-local).
     *
     * @return true if this context is shared, or
     *         false otherwise
     */
    public boolean isShared() {
        return parent == null;
    }

    /**
     * Returns the context identifier. This is used to identify the
     * current context in a stack trace or similar.
     *
     * @return the context identifier
     */
    public String id() {
        return id;
    }

    /**
     * Checks if an attribute value is set. If the attribute isn't set
     * in this context, the parent context is checked.
     *
     * @param key            the attribute name
     *
     * @return true if the attribute is set, or
     *         false otherwise
     */
    public boolean has(String key) {
        return attributes.containsKey(key) || (parent != null && parent.has(key));
    }

    /**
     * Returns an attribute value of a specified type. If the attribute
     * isn't set in this context, the parent context is searched. If the
     * attribute type doesn't match, null is returned.
     *
     * @param <T>            the attribute type
     * @param key            the attribute name
     * @param clazz          the value class required
     *
     * @return the attribute value, or
     *         null if not defined or mismatching type
     */
    public <T> T get(String key, Class<T> clazz) {
        Object val = attributes.get(key);
        if (val != null) {
            return clazz.isInstance(val) ? clazz.cast(val) : null;
        } else {
            return (parent == null) ? null : parent.get(key, clazz);
        }
    }

    /**
     * Returns or sets an attribute value of a specified type. If the
     * attribute isn't set in this or any parent context, it is created
     * using the provided supplier.
     *
     * @param <T>            the attribute type
     * @param key            the attribute name
     * @param clazz          the value class required
     * @param init           the initializer if not set
     *
     * @return the attribute value found or created
     */
    public <T> T getOrSet(String key, Class<T> clazz, Supplier<T> init) {
        return has(key) ? get(key, clazz) : set(key, init.get());
    }

    /**
     * Sets an attribute value. The value is set or removed in this
     * context, regardless if it is also defined in a parent context.
     *
     * @param <T>            the attribute type
     * @param key            the attribute name
     * @param value          the attribute value
     *
     * @return the attribute value set
     */
    public <T> T set(String key, T value) {
        if (value == null) {
            attributes.remove(key);
        } else {
            attributes.put(key, value);
        }
        return value;
    }

    /**
     * Returns the context data store. This may be either the root
     * data storage or one that restricts or modifies access.
     *
     * @return the context data store
     */
    public Storage storage() {
        return get(CX_STORAGE, Storage.class);
    }

    /**
     * Returns the context user if available.
     *
     * @return the context user, or
     *         null if not set (anonymous)
     */
    public User user() {
        return get(CX_USER, User.class);
    }
}
