/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2019 Per Cederberg. All rights reserved.
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

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import org.rapidcontext.core.data.Array;

/**
 * A JavaScript array wrapper. This class encapsulates an array and
 * forwards all reads and modifications.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ArrayWrapper extends ScriptableObject implements Wrapper {

    /**
     * The encapsulated array.
     */
    private Array arr;

    /**
     * Creates a new JavaScript array wrapper.
     *
     * @param arr            the array object
     * @param parentScope    the object parent scope
     */
    public ArrayWrapper(Array arr, Scriptable parentScope) {
        super(parentScope, getArrayPrototype(parentScope));
        this.arr = arr;
        // Warning: This apparently also calls put() with the specified value...
        //          which works this time, but not always.
        defineProperty("length", (Object) null, DONTENUM | PERMANENT);
        defineProperty("toJSON", (Object) null, DONTENUM | PERMANENT);
    }

    /**
     * Returns the class name.
     *
     * @return the class name
     */
    public String getClassName() {
        return "ArrayWrapper";
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
        if (name.equals("length")) {
            return Integer.valueOf(arr.size());
        } else if (name.equals("toJSON")) {
            return new BaseFunction(start, getFunctionPrototype(start)) {
                public String getFunctionName() {
                    return "toJSON";
                }
                public int getArity() {
                    return 1;
                }
                public int getLength() {
                    return 1;
                }
                public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                    return new NativeArray(getArray());
                }
            };
        } else {
            return super.get(name, start);
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
            Object val = JsSerializer.wrap(arr.get(index), this);
            if (!arr.isSealed()) {
                arr.set(index, val);
            }
            return val;
        }
        return NOT_FOUND;
    }

    /**
     * Returns an array with all indexed properties from this object.
     *
     * @return an array with all values
     */
    public Object[] getArray() {
        return arr.values();
    }

    /**
     * Sets a property in this object.
     *
     * @param name           the name of the property
     * @param start          the object in which the lookup began
     * @param value          the value to set
     */
    public void put(String name, Scriptable start, Object value) {
        if (name.equals("length") && value instanceof Number) {
            int len = ((Number) value).intValue();
            while (arr.size() < len) {
                arr.add(null);
            }
            while (arr.size() > len) {
                arr.remove(arr.size() - 1);
            }
        } else {
            super.put(name, start, value);
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
    }

    /**
     * Removes an indexed property from this object.
     *
     * @param index          the index of the property
     */
    public void delete(int index) {
        // Emulates JS semantics by not renumbering array
        arr.set(index, null);
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
                arr.set(i, JsSerializer.unwrap(arr.get(i)));
            }
        }
        return arr;
    }
}
