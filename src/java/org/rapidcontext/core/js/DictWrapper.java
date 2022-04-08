/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2022 Per Cederberg. All rights reserved.
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

import java.util.Arrays;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import org.rapidcontext.core.data.Dict;

/**
 * A JavaScript dictionary wrapper. This class encapsulates a dictionary and
 * forwards all reads and modifications.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class DictWrapper extends ScriptableObject implements Wrapper {

    /**
     * The encapsulated dictionary.
     */
    private Dict dict;

    /**
     * Creates a new JavaScript dictionary wrapper.
     *
     * @param dict           the dictionary object
     * @param parentScope    the object parent scope
     */
    public DictWrapper(Dict dict, Scriptable parentScope) {
        super(parentScope, getObjectPrototype(parentScope));
        this.dict = dict;
    }

    /**
     * Returns the class name.
     *
     * @return the class name
     */
    public String getClassName() {
        return "DictWrapper";
    }

    /**
     * Checks for JavaScript instance objects (always returns false).
     *
     * @param instance       the object to check
     *
     * @return always returns false as this is not a class
     */
    public boolean hasInstance(Scriptable instance) {
        return false;
    }

    /**
     * Checks if a property is defined in this object.
     *
     * @param name           the name of the property
     * @param start          the object in which the lookup began
     *
     * @return true if the property is defined, or
     *         false otherwise
     */
    public boolean has(String name, Scriptable start) {
        return dict.containsKey(name) || super.has(name, start);
    }

    /**
     * Returns an array of defined property keys.
     *
     * @return an array of defined property keys
     */
    public Object[] getIds() {
        String[] keys = dict.keys();
        return Arrays.copyOf(keys, keys.length, Object[].class);
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
    public Object get(String name, Scriptable start) {
        if (dict.containsKey(name)) {
            Object val = JsSerializer.wrap(dict.get(name), this);
            if (!dict.isSealed()) {
                dict.set(name, val);
            }
            return val;
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
    public void put(String name, Scriptable start, Object value) {
        if (name != null && name.length() > 0) {
            dict.set(name, value);
        }
    }

    /**
     * Removes a property from this object.
     *
     * @param name           the name of the property
     */
    public void delete(String name) {
        if (dict.containsKey(name)) {
            dict.remove(name);
        }
        super.delete(name);
    }

    /**
     * Returns the wrapped object. Recursively replaces all JavaScript
     * classes inside values and replaces them with the corresponding
     * Java objects.
     *
     * @return the unwrapped object
     */
    public Object unwrap() {
        if (!dict.isSealed()) {
            for (String key : dict.keys()) {
                dict.set(key, JsSerializer.unwrap(dict.get(key)));
            }
        }
        return dict;
    }
}
