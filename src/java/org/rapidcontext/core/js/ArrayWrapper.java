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
public class ArrayWrapper implements Scriptable, Wrapper {

    /**
     * The encapsulated array.
     */
    private Array arr;

    /**
     * The object prototype.
     */
    private Scriptable prototype;

    /**
     * The object parent scope.
     */
    private Scriptable parentScope;

    /**
     * Creates a new JavaScript array wrapper.
     *
     * @param arr            the array object
     * @param parentScope    the object parent scope
     */
    public ArrayWrapper(Array arr, Scriptable parentScope) {
        this.arr = arr;
        this.prototype = ScriptableObject.getClassPrototype(parentScope, "Array");
        this.parentScope = parentScope;
    }

    /**
     * Returns the class name.
     *
     * @return the class name
     */
    public String getClassName() {
        return "Array";
    }

    /**
     * Returns the prototype of the object.
     *
     * @return the prototype of the object
     */
    public Scriptable getPrototype() {
        return this.prototype;
    }

    /**
     * Sets the prototype of the object.
     *
     * @param prototype      the prototype object
     */
    public void setPrototype(Scriptable prototype) {
        this.prototype = prototype;
    }

    /**
     * Returns the parent (enclosing) scope of the object.
     *
     * @return the parent (enclosing) scope of the object
     */
    public Scriptable getParentScope() {
        return this.parentScope;
    }

    /**
     * Sets the parent (enclosing) scope of the object.
     *
     * @param parentScope    the parent scope of the object
     */
    public void setParentScope(Scriptable parentScope) {
        this.parentScope = parentScope;
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
        return name.equals("length");
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
     * Returns an array of defined property keys.
     *
     * @return an array of defined property keys
     */
    public Object[] getIds() {
        return new Object[0];
    }

    /**
     * Returns the default value of this object.
     *
     * @param typeHint       type type hint class
     *
     * @return the default value of this object
     */
    public Object getDefaultValue(Class<?> typeHint) {
        return ScriptableObject.getDefaultValue(this, typeHint);
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
            return new Integer(arr.size());
        }
        return NOT_FOUND;
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
     * Removes a property from this object.
     *
     * @param name           the name of the property
     */
    public void delete(String name) {
        // Do nothing
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
