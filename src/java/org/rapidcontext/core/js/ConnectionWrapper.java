/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2009 Per Cederberg & Dynabyte AB.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE.txt file for more details.
 */

package org.rapidcontext.core.js;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.rapidcontext.core.env.AdapterConnection;

/**
 * A JavaScript connection wrapper. This class encapsulates an
 * adapter connection object and forwards calls to the connection
 * methods.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class ConnectionWrapper implements Scriptable {

    /**
     * The encapsulated adapter connection object. 
     */
    private AdapterConnection con;

    /**
     * The object prototype.
     */
    private Scriptable prototype;

    /**
     * The object parent scope.
     */
    private Scriptable parentScope;

    /**
     * Creates a new JavaScript data object wrapper.
     *
     * @param con            the adapter connection object
     * @param parentScope    the object parent scope
     */
    public ConnectionWrapper(AdapterConnection con, Scriptable parentScope) {
        this.con = con;
        this.prototype = ScriptableObject.getObjectPrototype(parentScope);
        this.parentScope = parentScope;
    }

    /**
     * Returns the encapsulated adapter connection.
     *
     * @return the encapsulated adapter connection
     */
    public AdapterConnection getConnection() {
        return this.con;
    }

    /**
     * Returns the class name.
     *
     * @return the class name
     */
    public String getClassName() {
        return "Connection";
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
        if (has(name, start)) {
            return new ConnectionMethodWrapper(name, this);
        } else {
            return NOT_FOUND;
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
        Method[]  methods = con.getClass().getMethods();

        if (name.equals("activate") ||
            name.equals("passivate") ||
            name.equals("close")) {
            // Hide internal methods for connection handling
            return false;
        }
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equals(name) &&
                (methods[i].getModifiers() & Modifier.PUBLIC) > 0) {
                return true;
            }
        }
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
        // Do nothing, connection is read-only
    }

    /**
     * Sets an indexed property in this object.
     *
     * @param index          the index of the property
     * @param start          the object in which the lookup began
     * @param value          the value to set
     */
    public void put(int index, Scriptable start, Object value) {
        // Do nothing, connection is read-only
    }

    /**
     * Removes a property from this object.
     *
     * @param name           the name of the property
     */
    public void delete(String name) {
        // Do nothing, connection is read-only
    }

    /**
     * Removes an indexed property from this object.
     *
     * @param index          the index of the property
     */
    public void delete(int index) {
        // Do nothing, connection is read-only
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
        // Do nothing, connection is read-only
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
        // Do nothing, connection is read-only
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
    public Object getDefaultValue(Class typeHint) {
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
     * A JavaScript connection method wrapper. This class
     * encapsulates a method in an adapter connection object. Any
     * call will be forwarded to the best matching connection method
     * and all objects will be unwrapped from JavaScript.
     *
     * @author   Per Cederberg, Dynabyte AB
     * @version  1.0
     */
    private class ConnectionMethodWrapper implements Function {

        /**
         * The method name.
         */
        private String methodName;

        /**
         * The function prototype.
         */
        private Scriptable functionPrototype;

        /**
         * The parent connection wrapper.
         */
        private ConnectionWrapper parent;

        /**
         * Creates a new connection method wrapper.
         *
         * @param methodName     the method name
         * @param parent         the object parent scope
         */
        public ConnectionMethodWrapper(String methodName,
                                       ConnectionWrapper parent) {
            this.methodName = methodName;
            this.functionPrototype =
                ScriptableObject.getFunctionPrototype(parent);
            this.parent = parent;
        }

        /**
         * Returns the class name.
         *
         * @return the class name
         */
        public String getClassName() {
            return "ConnectionFunction";
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
            // Do nothing, connection method is read-only
        }

        /**
         * Sets an indexed property in this object.
         *
         * @param index          the index of the property
         * @param start          the object in which the lookup began
         * @param value          the value to set
         */
        public void put(int index, Scriptable start, Object value) {
            // Do nothing, connection method is read-only
        }

        /**
         * Removes a property from this object.
         *
         * @param name           the name of the property
         */
        public void delete(String name) {
            // Do nothing, connection method is read-only
        }

        /**
         * Removes an indexed property from this object.
         *
         * @param index          the index of the property
         */
        public void delete(int index) {
            // Do nothing, connection method is read-only
        }

        /**
         * Returns the prototype of the object.
         *
         * @return the prototype of the object
         */
        public Scriptable getPrototype() {
            return this.functionPrototype;
        }

        /**
         * Sets the prototype of the object.
         *
         * @param prototype      the prototype object
         */
        public void setPrototype(Scriptable prototype) {
            // Do nothing, connection method is read-only
        }

        /**
         * Returns the parent (enclosing) scope of the object.
         *
         * @return the parent (enclosing) scope of the object
         */
        public Scriptable getParentScope() {
            return this.parent;
        }

        /**
         * Sets the parent (enclosing) scope of the object.
         *
         * @param parentScope    the parent scope of the object
         */
        public void setParentScope(Scriptable parentScope) {
            // Do nothing, connection method is read-only
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
        public Object getDefaultValue(Class typeHint) {
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
         * Calls this function.
         *
         * @param cx             the current script context
         * @param scope          the scope to execute the function in
         * @param thisObj        the script <code>this</code> object
         * @param args           the array of arguments
         *
         * @return the result of the function call
         */
        public Object call(Context cx,
                           Scriptable scope,
                           Scriptable thisObj,
                           Object[] args) {

            Object    target = parent.getConnection();
            Method[]  methods = target.getClass().getMethods();
            Object    res;
            String    msg;

            for (int i = 0; i < args.length; i++) {
                args[i] = JsSerializer.unwrap(args[i]);
            }
            for (int i = 0; i < methods.length; i++) {
                if (isMatching(methods[i], args)) {
                    try {
                        methods[i].setAccessible(true);
                        res = methods[i].invoke(target, args);
                    } catch (Exception e) {
                        msg = "call to " + this.methodName + " failed: " +
                              e.getClass().getName() + ": " + e.getMessage();
                        throw new EvaluatorException(msg);
                    }
                    return JsSerializer.wrap(res, scope);
                }
            }
            msg = "connection has no matching call method " +
                  this.methodName + " for the specified arguments";
            throw new EvaluatorException(msg);
        }

        /**
         * Calls this function as a constructor. This method will
         * not do anything.
         *
         * @param cx             the current script context
         * @param scope          the scope to execute the function in
         * @param args           the array of arguments
         *
         * @return always returns null
         */
        public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
            return null;
        }

        /**
         * Checks if an array of objects matches a method signature.
         * This method will always return false if the method name
         * does not match the method name of this wrapper.
         *
         * @param m              the method to check
         * @param args           the array or arguments
         *
         * @return true if the arguments matches, or
         *         false otherwise
         */
        private boolean isMatching(Method m, Object[] args) {
            Class[]  types;

            if (!m.getName().equals(this.methodName)) {
                return false;
            } else if ((m.getModifiers() & Modifier.PUBLIC) <= 0) {
                return false;
            }
            types = m.getParameterTypes();
            if (types.length != args.length) {
                return false;
            }
            for (int i = 0; i < types.length; i++) {
                if (!isMatching(types[i], args[i])) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Checks if an object matches the specified type.
         *
         * @param type           the type class
         * @param obj            the object instance
         *
         * @return true if the object matches the type, or
         *         false otherwise
         */
        private boolean isMatching(Class type, Object obj) {
            if (type == Boolean.TYPE) {
                return obj instanceof Boolean;
            } else if (type == Integer.TYPE ) {
                return obj instanceof Integer;
            } else if (!type.isPrimitive() && obj == null) {
                return true;
            } else {
                return type.isInstance(obj);
            }
        }
    }
}
