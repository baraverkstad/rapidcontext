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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.type.Channel;

/**
 * A JavaScript connection wrapper. This class encapsulates a
 * connection channel and forwards calls to the Java methods.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ConnectionWrapper extends ScriptableObject implements Wrapper {

    // Hidden method names
    private static final Set<String> HIDDEN = Set.of(
        "getConnection", "validate", "invalidate",
        "equals", "getClass", "hashCode", "notify", "notifyAll", "wait"
    );

    /**
     * The procedure call context in use.
     */
    private CallContext cx;

    /**
     * The encapsulated connection channel.
     */
    private Channel channel;

    /**
     * The visible connection methods.
     */
    private HashSet<String> methods = new HashSet<>();

    /**
     * Creates a new JavaScript connection wrapper.
     *
     * @param cx             the procedure call context
     * @param channel        the connection channel
     * @param parentScope    the object parent scope
     */
    public ConnectionWrapper(CallContext cx, Channel channel, Scriptable parentScope) {
        super(parentScope, getObjectPrototype(parentScope));
        this.cx = cx;
        this.channel = channel;
        for (Method m : channel.getClass().getMethods()) {
            boolean isPublic = (m.getModifiers() & Modifier.PUBLIC) > 0;
            String name = m.getName();
            if (isPublic && !HIDDEN.contains(name)) {
                methods.add(name);
                setAttributes(name, READONLY | PERMANENT);
            }
        }
    }

    /**
     * Returns the encapsulated adapter connection.
     *
     * @return the encapsulated adapter connection
     */
    public Channel getConnection() {
        return this.channel;
    }

    /**
     * Returns the class name.
     *
     * @return the class name
     */
    public String getClassName() {
        return "ConnectionWrapper";
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
     * Returns a named property from this object.
     *
     * @param name           the name of the property
     * @param start          the object in which the lookup began
     *
     * @return the value of the property, or
     *         NOT_FOUND if not found
     */
    public Object get(String name, Scriptable start) {
        if (methods.contains(name)) {
            return new ConnectionMethodWrapper(name);
        } else {
            return super.get(name, start);
        }
    }

    /**
     * Returns the wrapped object.
     *
     * @return the unwrapped object
     */
    public Object unwrap() {
        return channel;
    }


    /**
     * A JavaScript connection channel method wrapper. This class
     * encapsulates a method in a channel. Any call will be forwarded
     * to the best matching method and all objects will be unwrapped
     * from JavaScript.
     *
     * @author   Per Cederberg
     * @version  1.0
     */
    private class ConnectionMethodWrapper extends BaseFunction {

        /**
         * The method name.
         */
        private String methodName;

        /**
         * Creates a new connection method wrapper.
         *
         * @param methodName     the method name
         */
        public ConnectionMethodWrapper(String methodName) {
            super(ConnectionWrapper.this, getFunctionPrototype(ConnectionWrapper.this));
            this.methodName = methodName;
        }

        /**
         * Returns the class name.
         *
         * @return the class name
         */
        public String getClassName() {
            return "ConnectionMethodWrapper";
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
            case "name":
                return methodName;
            case "arity":
            case "length":
                Channel target = ConnectionWrapper.this.getConnection();
                for (Method m : target.getClass().getMethods()) {
                    boolean isPublic = (m.getModifiers() & Modifier.PUBLIC) > 0;
                    if (isPublic && m.getName().equals(methodName)) {
                        return Integer.valueOf(m.getParameterCount());
                    }
                }
                return Integer.valueOf(0);
            default:
                return super.get(name, start);
            }
        }

        /**
         * Calls this function.
         *
         * @param ctx            the current script context
         * @param scope          the scope to execute the function in
         * @param thisObj        the script <code>this</code> object
         * @param args           the array of arguments
         *
         * @return the result of the function call
         */
        public Object call(Context ctx,
                           Scriptable scope,
                           Scriptable thisObj,
                           Object[] args) {

            for (int i = 0; i < args.length; i++) {
                args[i] = JsRuntime.unwrap(args[i]);
            }
            Channel target = ConnectionWrapper.this.getConnection();
            for (Method m : target.getClass().getMethods()) {
                if (isMatching(m, args)) {
                    // TODO: call context stack should be pushed & popped
                    String signature = target.getConnection().path() + "#" +
                                       this.methodName;
                    cx.logCall(signature, args);
                    try {
                        m.setAccessible(true);
                        Object res = m.invoke(target, args);
                        cx.logResponse(res);
                        return JsRuntime.wrap(res, scope);
                    } catch (Exception e) {
                        cx.logError(e);
                        String msg = "call to " + this.methodName + " failed: " +
                                     e.getClass().getName() + ": " + e.getMessage();
                        throw new EvaluatorException(msg);
                    }
                }
            }
            String msg = "connection has no matching call method " +
                         this.methodName + " for the specified arguments";
            throw new EvaluatorException(msg);
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
            if (!m.getName().equals(this.methodName)) {
                return false;
            } else if ((m.getModifiers() & Modifier.PUBLIC) <= 0) {
                return false;
            }
            Class<?>[] types = m.getParameterTypes();
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
        private boolean isMatching(Class<?> type, Object obj) {
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
