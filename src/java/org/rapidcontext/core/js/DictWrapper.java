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

package org.rapidcontext.core.js;

import java.util.HashMap;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import org.rapidcontext.core.data.Dict;

/**
 * A JavaScript dictionary wrapper. This class encapsulates a dictionary and
 * forwards all reads and modifications.
 *
 * @author Per Cederberg
 */
public final class DictWrapper extends ScriptableObject implements Wrapper {

    /**
     * The encapsulated dictionary.
     */
    private Dict dict;

    /**
     * The cache of wrapped objects.
     */
    private HashMap<String, Object> cache = new HashMap<>();

    /**
     * Creates a new JavaScript dictionary wrapper.
     *
     * @param dict           the dictionary object
     * @param parentScope    the object parent scope
     */
    public DictWrapper(Dict dict, Scriptable parentScope) {
        super(parentScope, getObjectPrototype(parentScope));
        this.dict = dict;
        for (String key : dict.keys()) {
            setAttributes(key, EMPTY);
        }
    }

    /**
     * Returns the class name.
     *
     * @return the class name
     */
    @Override
    public String getClassName() {
        return "DictWrapper";
    }

    /**
     * Checks for JavaScript instance objects (always returns false).
     *
     * @param instance       the object to check
     *
     * @return always returns false (no instances possible)
     */
    @Override
    public boolean hasInstance(Scriptable instance) {
        return false;
    }

    /**
     * Returns a named property from this object.
     *
     * @param name           the name of the property
     * @param start          the object in which the lookup began
     *
     * @return the value of the property, or
     *         NOT_FOUND if not found
     */
    @Override
    public Object get(String name, Scriptable start) {
        if (dict.containsKey(name)) {
            if (!cache.containsKey(name)) {
                cache.put(name, JsRuntime.wrap(dict.get(name), this));
            }
            return cache.get(name);
        } else {
            return super.get(name, start);
        }
    }

    /**
     * Sets a property in this object.
     *
     * @param name           the name of the property
     * @param start          the object in which the lookup began
     * @param value          the value to set
     */
    @Override
    public void put(String name, Scriptable start, Object value) {
        if (name != null && !name.isBlank()) {
            dict.set(name, value);
            cache.remove(name);
            setAttributes(name, EMPTY);
        }
    }

    /**
     * Removes a property from this object.
     *
     * @param name           the name of the property
     */
    @Override
    public void delete(String name) {
        dict.remove(name);
        cache.remove(name);
        super.delete(name);
    }

    /**
     * Returns the wrapped object. Recursively replaces all JavaScript
     * classes inside values and replaces them with the corresponding
     * Java objects.
     *
     * @return the unwrapped object
     */
    @Override
    public Object unwrap() {
        cache.clear();
        return JsRuntime.unwrap(dict);
    }
}
