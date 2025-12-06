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

package org.rapidcontext.core.js;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Set;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.type.Channel;

/**
 * A JavaScript connection wrapper. This class encapsulates a
 * connection channel and forwards calls to the Java methods.
 *
 * @author Per Cederberg
 */
public final class ConnectionWrapper extends ScriptableObject implements Wrapper {

    /**
     * The methods hidden.
     */
    private static final Set<String> HIDDEN = Set.of(
        "getConnection", "validate", "revalidate", "invalidate", "report",
        "equals", "getClass", "hashCode", "notify", "notifyAll", "wait"
    );

    /**
     * The encapsulated connection channel.
     */
    private Channel channel;

    /**
     * The visible connection methods.
     */
    private HashMap<String, Function> methods = new HashMap<>();

    /**
     * Creates a new JavaScript connection wrapper.
     *
     * @param channel        the connection channel
     * @param parentScope    the object parent scope
     */
    public ConnectionWrapper(Channel channel, Scriptable parentScope) {
        super(parentScope, getObjectPrototype(parentScope));
        this.channel = channel;
        for (Method m : channel.getClass().getMethods()) {
            boolean isPublic = (m.getModifiers() & Modifier.PUBLIC) > 0;
            String name = m.getName();
            if (isPublic && !HIDDEN.contains(name)) {
                methods.put(name, new ConnectionMethodWrapper(this, m));
                setAttributes(name, READONLY | PERMANENT);
            }
        }
    }

    /**
     * Returns the class name.
     *
     * @return the class name
     */
    @Override
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
        if (methods.containsKey(name)) {
            return methods.get(name);
        } else {
            return super.get(name, start);
        }
    }

    /**
     * Returns the wrapped object.
     *
     * @return the unwrapped object
     */
    @Override
    public Object unwrap() {
        return channel;
    }


    /**
     * A JavaScript connection channel method wrapper. This class
     * encapsulates a method in a channel. Any call will be forwarded
     * to the best matching method and all objects will be unwrapped
     * from JavaScript.
     */
    private class ConnectionMethodWrapper extends NativeJavaMethod {

        /**
         * The method wrapped.
         */
        private final Method method;

        /**
         * Creates a new connection method wrapper.
         *
         * @param scope         the parent scope
         * @param method        the method to invoke
         */
        public ConnectionMethodWrapper(Scriptable scope, Method method) {
            super(method, method.getName());
            setParentScope(scope);
            setPrototype(getFunctionPrototype(scope));
            this.method = method;
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
        @Override
        public Object call(Context ctx, Scriptable scope, Scriptable thisObj, Object[] args) {
            Channel target = ConnectionWrapper.this.channel;
            String signature = target.getConnection().path() + "#" + method.getName();
            for (int i = 0; i < args.length; i++) {
                args[i] = JsRuntime.unwrap(args[i]);
            }
            CallContext cx = CallContext.active();
            cx.logRequest(signature, args);
            try {
                method.setAccessible(true);
                Object res = method.invoke(target, args);
                cx.logResponse(res);
                return JsRuntime.wrap(res, scope);
            } catch (Exception e) {
                cx.logError(e);
                String msg = "call to " + method.getName() + " failed: " + e;
                throw new EvaluatorException(msg);
            }
        }
    }
}
