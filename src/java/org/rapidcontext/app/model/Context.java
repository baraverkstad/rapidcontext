/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2023 Per Cederberg. All rights reserved.
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

package org.rapidcontext.app.model;

import java.util.HashMap;

import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.User;

/**
 * The execution context. This provides access to settings, storage, call
 * parameters, etc. The context hierarchy allows overriding or extending
 * attributes per procedure call.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Context {

    /**
     * The storage context attribute.
     */
    public static String CX_STORAGE = "storage";

    /**
     * The user context attribute.
     */
    public static String CX_USER = "user";

    /**
     * The currently active context (for this thread).
     */
    private static ThreadLocal<Context> current = new ThreadLocal<>();

    /**
     * Returns the current context (for this thread).
     *
     * @return the current context
     */
    public static Context get() {
        return current.get();
    }

    /**
     * Adds a new context to the top of the stack. The previous context
     * will be set as the parent to the new context.
     *
     * @param cx             the new context
     */
    public static void push(Context cx) {
        // FIXME: Check if cx already in chain?
        cx.parent = current.get();
        current.set(cx);
    }

    /**
     * Removes the current context from the top of the stack. The parent
     * context will replace it at the top.
     */
    public static void pop() {
        Context cx = current.get();
        if (cx.parent != null) {
            current.set(cx.parent);
            cx.parent = null;
        }
    }

    /**
     * The parent context (if available).
     */
    private Context parent = null;

    /**
     * The map of attributes. Attributes can be used for storing generic
     * objects in the context, which is useful in some situations.
     */
    private HashMap<String,Object> attributes = new HashMap<>();

    /**
     * Creates a new empty context.
     */
    public Context() {
        // Nothing to do here
    }

    /**
     * Returns an attribute value. If the attribute isn't set in this
     * context, the parent context is searched.
     *
     * @param key            the attribute name
     *
     * @return the attribute value, or
     *         null if not defined
     */
    public Object get(String key) {
        return get(key, Object.class);
    }

    /**
     * Returns an attribute value of a specified type. If the attribute
     * isn't set in this context, the parent context is searched. If the
     * attribute type doesn't match, null is returned.
     *
     * @param <T>            the type to cast result to
     * @param key            the attribute name
     * @param clazz          the value class required
     *
     * @return the attribute value, or
     *         null if not defined or mismatching type
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object val = attributes.get(key);
        if (val != null) {
            return clazz.isInstance(val) ? (T) val : null;
        } else {
            return (parent == null) ? null : parent.get(key, clazz);
        }
    }

    /**
     * Returns the context data store. This may be either the root
     * data storage or one that restricts or modifies access.
     *
     * @return the context data store
     */
    public Storage getStorage() {
        return get(CX_STORAGE, Storage.class);
    }

    /**
     * Returns the context user if available.
     *
     * @return the context user, or
     *         null if not set (anonymous)
     */
    public User getUser() {
        return get(CX_USER, User.class);
    }

    /**
     * Sets an attribute value. The value is set or removed in this
     * context, regardless if it is also defined in a parent context.
     *
     * @param key            the attribute name
     * @param value          the attribute value
     */
    public void set(String key, Object value) {
        if (value == null) {
            attributes.remove(key);
        } else {
            attributes.put(key, value);
        }
    }
}
