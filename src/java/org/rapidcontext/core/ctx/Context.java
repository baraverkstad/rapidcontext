/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2025 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.ctx;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.Environment;

/**
 * The base execution context. The context provides access to settings,
 * storage, current user, arguments, etc. Each context links to a parent
 * context, creating a hierarchy that allows adding or updating
 * attributes in a way that is only visible in a particular context (and
 * its children).
 *
 * All execution contexts, except the root (global) context, are bound to
 * a single execution thread. Each thread has (at most) a single active
 * execution context, which is stored in a thread-local variable.
 *
 * The shared root (global) context is NOT thread-local and holds data
 * that pertains to the whole system.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public abstract class Context {

    /**
     * The base directory context attribute.
     */
    public static String CX_DIRECTORY = "directory";

    /**
     * The environment context attribute.
     */
    public static String CX_ENVIRONMENT = "environment";

    /**
     * The storage context attribute.
     */
    public static String CX_STORAGE = "storage";

    /**
     * The shared root context at the base of the hierarchy.
     */
    protected static Context root = null;

    /**
     * The currently active thread-local context. This is not synchronized
     * for fast access.
     */
    protected static ThreadLocal<Context> active = new ThreadLocal<>();

    /**
     * The currently active threads with contexts.
     */
    protected static Map<Thread, Context> actives =
        Collections.synchronizedMap(new HashMap<>());

    /**
     * Returns the currently active context. If no thread-local context
     * is available, the root context is returned.
     *
     * @return the currently active context
     */
    @SuppressWarnings("resource")
    public static Context get() {
        return Objects.requireNonNullElse(active.get(), root);
    }

    /**
     * Returns the currently active context for a specified thread.
     *
     * @param thread         the thread to fetch for
     *
     * @return the thread-local context, or
     *         null if not set
     */
    public static Context get(Thread thread) {
        return actives.get(thread);
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
     * Creates a new context and sets it either as the root context (if
     * no root previously set), or as the active context for the currently
     * executing thread. The previously active (or root) context is set as
     * the parent of this context.
     *
     * @param id             the context identifier (name)
     */
    public Context(String id) {
        this.id = id;
        this.parent = get();
        if (parent == null) {
            root = this;
        } else {
            active.set(this);
            actives.put(Thread.currentThread(), this);
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
     * Returns the base directory.
     *
     * @return the base directory
     */
    public File baseDir() {
        return get(CX_DIRECTORY, File.class);
    }

    /**
     * Returns the context connectivity environment.
     *
     * @return the context connectivity environment
     */
    public Environment environment() {
        return get(CX_ENVIRONMENT, Environment.class);
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
}
