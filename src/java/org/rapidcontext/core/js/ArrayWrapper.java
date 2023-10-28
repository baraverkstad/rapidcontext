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

package org.rapidcontext.core.js;

import java.util.HashMap;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Wrapper;
import org.rapidcontext.core.data.Array;

/**
 * A JavaScript array wrapper. This class encapsulates an array and
 * forwards all reads and modifications.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public final class ArrayWrapper extends ScriptableObject implements Wrapper {

    /**
     * The encapsulated array.
     */
    private Array arr;

    /**
     * The cache of wrapped objects.
     */
    private HashMap<Integer, Object> cache = new HashMap<>();

    /**
     * Creates a new JavaScript array wrapper.
     *
     * @param arr            the array object
     * @param parentScope    the object parent scope
     */
    public ArrayWrapper(Array arr, Scriptable parentScope) {
        super(parentScope, getArrayPrototype(parentScope));
        this.arr = arr;
        setAttributes("length", DONTENUM | PERMANENT);
        setAttributes("toJSON", READONLY | DONTENUM | PERMANENT);
        for (int i = 0; i < arr.size(); i++) {
            setAttributes(String.valueOf(i), EMPTY);
        }
    }

    /**
     * Returns the class name.
     *
     * @return the class name
     */
    public String getClassName() {
        return "Array";  // support Array.isArray()
    }

    /**
     * Checks for JavaScript instance objects (always returns false).
     *
     * @param instance       the object to check
     *
     * @return always returns false (no instances possible)
     */
    public boolean hasInstance(Scriptable instance) {
        return false;
    }

    /**
     * Checks if an index is defined in this object.
     *
     * @param index          the index of the property
     * @param start          the object in which the lookup began
     *
     * @return true if the index is defined, or
     *         false otherwise
     */
    public boolean has(int index, Scriptable start) {
        return arr.containsIndex(index);
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
        switch (name) {
        case "length":
            return arr.size();
        case "toJSON":
            return new LambdaFunction(this, name, 0, new Callable() {
                public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                    Object[] values = new Object[arr.size()];
                    for (int i = 0; i < arr.size(); i++) {
                        values[i] = get(i, thisObj);
                    }
                    return cx.newArray(scope, values);
                }
            });
        default:
            double idx = ScriptRuntime.toNumber(name);
            if (!Double.isNaN(idx) && idx >= 0 && idx < Integer.MAX_VALUE) {
                return get((int) idx, start);
            } else {
                return super.get(name, start);
            }
        }
    }

    /**
     * Returns an indexed property from this object.
     *
     * @param index          the index of the property
     * @param start          the object in which the lookup began
     *
     * @return the value of the property, or
     *         NOT_FOUND if not found
     */
    public Object get(int index, Scriptable start) {
        if (arr.containsIndex(index)) {
            if (!cache.containsKey(index)) {
                cache.put(index, JsRuntime.wrap(arr.get(index), this));
            }
            return cache.get(index);
        }
        return NOT_FOUND;
    }

    /**
     * Sets a property in this object.
     *
     * @param name           the name of the property
     * @param start          the object in which the lookup began
     * @param value          the value to set
     */
    public void put(String name, Scriptable start, Object value) {
        double idx;
        if (name.equals("length")) {
            idx = ScriptRuntime.toNumber(value);
            if (!Double.isNaN(idx) && idx >= 0 && idx < Integer.MAX_VALUE) {
                long len = ScriptRuntime.toUint32(idx);
                while (arr.size() < len) {
                    put(arr.size(), start, null);
                }
                while (arr.size() > len) {
                    int last = arr.size() - 1;
                    arr.remove(last);
                    cache.remove(last);
                    super.delete(String.valueOf(last));
                }
            }
        } else {
            idx = ScriptRuntime.toNumber(name);
            if (!Double.isNaN(idx) && idx >= 0 && idx < Integer.MAX_VALUE) {
                put((int) ScriptRuntime.toUint32(idx), start, value);
            } else {
                super.put(name, start, value);
            }
        }
    }

    /**
     * Sets an indexed property in this object.
     *
     * @param index          the index of the property
     * @param start          the object in which the lookup began
     * @param value          the value to set
     */
    public void put(int index, Scriptable start, Object value) {
        arr.set(index, value);
        cache.remove(index);
        setAttributes(String.valueOf(index), EMPTY);
    }

    /**
     * Removes an indexed property from this object.
     *
     * @param index          the index of the property
     */
    public void delete(int index) {
        // Emulates JS semantics by not renumbering array
        arr.set(index, null);
        cache.remove(index);
    }

    /**
     * Returns the wrapped object. Recursively replaces all JavaScript
     * classes inside values and replaces them with the corresponding
     * Java objects.
     *
     * @return the unwrapped object
     */
    public Object unwrap() {
        if (!arr.isSealed()) {
            for (int i = 0; i < arr.size(); i++) {
                arr.set(i, JsRuntime.unwrap(arr.get(i)));
            }
        }
        cache.clear();
        return arr;
    }
}
