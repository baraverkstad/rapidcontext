/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2017 Per Cederberg. All rights reserved.
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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.Wrapper;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;

/**
 * A JavaScript procedure function wrapper. This class encapsulates a
 * procedure and allows it to be called from JavaScript.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
class ProcedureWrapper implements Function, Wrapper {

    /**
     * The procedure library.
     */
    private CallContext cx = null;

    /**
     * The procedure name.
     */
    private Procedure proc = null;

    /**
     * The object prototype.
     */
    private Scriptable prototype;

    /**
     * The object parent scope.
     */
    private Scriptable parentScope;

    /**
     * Creates a new procedure wrapper call function.
     *
     * @param cx             the procedure call context
     * @param proc           the procedure definition
     * @param parentScope    the object parent scope
     */
    ProcedureWrapper(CallContext cx, Procedure proc, Scriptable parentScope) {
        this.cx = cx;
        this.proc = proc;
        this.prototype = ScriptableObject.getFunctionPrototype(parentScope);
        this.parentScope = parentScope;
    }

    /**
     * Returns the class name.
     *
     * @return the class name
     */
    public String getClassName() {
        return "Procedure";
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
        return NOT_FOUND;
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
        return false;
    }

    /**
     * Sets a property in this object.
     *
     * @param name           the name of the property
     * @param start          the object in which the lookup began
     * @param value          the value to set
     */
    public void put(String name, Scriptable start, Object value) {
        // Do nothing, procedure is read-only
    }

    /**
     * Sets an indexed property in this object.
     *
     * @param index          the index of the property
     * @param start          the object in which the lookup began
     * @param value          the value to set
     */
    public void put(int index, Scriptable start, Object value) {
        // Do nothing, procedure is read-only
    }

    /**
     * Removes a property from this object.
     *
     * @param name           the name of the property
     */
    public void delete(String name) {
        // Do nothing, procedure is read-only
    }

    /**
     * Removes an indexed property from this object.
     *
     * @param index          the index of the property
     */
    public void delete(int index) {
        // Do nothing, procedure is read-only
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
        // Do nothing, procedure is read-only
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
        // Do nothing, procedure is read-only
    }

    /**
     * Returns an array of defined property keys.
     *
     * @return an array of defined property keys
     */
    public Object[] getIds() {
        return new String[0];
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
     * Calls this function. All the values in the argument list are
     * passed on to the procedure being called.
     *
     * @param scriptContext  the current script context
     * @param scope          the scope to execute the function in
     * @param thisObj        the JavaScript <code>this</code> object
     * @param args           the array of arguments
     *
     * @return the result of the procedure call
     */
    public Object call(Context scriptContext,
                       Scriptable scope,
                       Scriptable thisObj,
                       Object[] args) {

        if (!(proc instanceof JsProcedure)) {
            for (int i = 0; i < args.length; i++) {
                args[i] = JsSerializer.unwrap(args[i]);
            }
        }
        try {
            return JsSerializer.wrap(cx.call(proc, args), scope);
        } catch (ProcedureException e) {
            throw new WrappedException(e);
        }
    }

    /**
     * Calls this function as a constructor. This method will not do
     * anything.
     *
     * @param scriptContext  the current script context
     * @param scope          the enclosing scope of the caller except
     *                       when the function is called from a closure
     * @param args           the array of arguments
     *
     * @return always returns null
     */
    public Scriptable construct(Context scriptContext,
                                Scriptable scope,
                                Object[] args) {
        return null;
    }

    /**
     * Returns the wrapped object.
     *
     * @return the unwrapped object
     */
    public Object unwrap() {
        return proc;
    }
}
