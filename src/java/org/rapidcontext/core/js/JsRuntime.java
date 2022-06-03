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

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.UniqueTag;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.Wrapper;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.util.DateUtil;

/**
 * An interface to the JavaScript engine (Mozilla Rhino).
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public final class JsRuntime {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(JsRuntime.class.getName());

    /**
     * Compiles a JavaScript function for later use.
     *
     * @param name           the function name (must be valid JS)
     * @param args           the argument names
     * @param body           the function body (i.e. source code)
     *
     * @return a compiled function object
     *
     * @throws JsException if the source code didn't compile
     */
    public static Function compile(String name, String[] args, String body)
    throws JsException {

        JsErrorHandler errors = new JsErrorHandler();
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setErrorReporter(errors);
            Scriptable scope = cx.initSafeStandardObjects();
            StringBuilder code = new StringBuilder();
            code.append("function ");
            code.append(name);
            code.append("(");
            for (String arg : args) {
                if (code.charAt(code.length() - 1) != '(') {
                    code.append(", ");
                }
                code.append(arg);
            }
            code.append(") {\n");
            code.append(body);
            code.append("\n}");
            Function f = cx.compileFunction(scope, code.toString(), name, 1, null);
            if (errors.getErrorCount() > 0) {
                throw new JsException(errors.getErrorText());
            }
            return f;
        } catch (Exception e) {
            if (errors.getErrorCount() > 0) {
                throw new JsException(errors.getErrorText());
            }
            throw createException(e);
        }
    }

    /**
     * Calls a previously compiled JavaScript function.
     *
     * @param f              the compiled function
     * @param args           the argument values (will be wrapped)
     *
     * @return the function return value (possibly wrapped)
     *
     * @throws JsException if the call failed or threw an exception
     */
    public static Object call(Function f, Object[] args) throws JsException {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initSafeStandardObjects();
            Object[] safeArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                safeArgs[i] = wrap(args[i], scope);
            }
            return f.call(cx, scope, null, safeArgs);
        } catch (Exception e) {
            throw createException(e);
        }
    }

    /**
     * Creates a procedure exception from any exception type.
     *
     * @param e              the exception to convert
     *
     * @return the procedure exception
     */
    private static JsException createException(Throwable e) {
        if (e instanceof JsException) {
            return (JsException) e;
        } else if (e instanceof WrappedException) {
            return createException(((WrappedException) e).getWrappedException());
        } else {
            LOG.log(Level.WARNING, "Caught unhandled exception", e);
            return new JsException("Caught unhandled exception", e);
        }
    }

    /**
     * Wraps a Java object for JavaScript access. This method only
     * handles String, Number, Boolean and Data instances.
     *
     * @param obj            the object to wrap
     * @param scope          the parent scope
     *
     * @return the wrapped object
     *
     * @see org.rapidcontext.core.data.Array
     * @see org.rapidcontext.core.data.Dict
     */
    public static Object wrap(Object obj, Scriptable scope) {
        if (obj instanceof Dict && scope != null) {
            return new DictWrapper((Dict) obj, scope);
        } else if (obj instanceof Array && scope != null) {
            return new ArrayWrapper((Array) obj, scope);
        } else if (obj instanceof StorableObject) {
            return wrap(((StorableObject) obj).serialize(), scope);
        } else if (obj instanceof Date) {
            return DateUtil.asEpochMillis((Date) obj);
        } else {
            return Context.javaToJS(obj, scope);
        }
    }

    /**
     * Removes all JavaScript classes and replaces them with the
     * corresponding Java objects. This method will use instances of
     * Dict and Array to replace native JavaScript objects and arrays.
     * Also, it will replace both JavaScript "null" and "undefined"
     * with null. Any Dict or Array object encountered will be
     * traversed and copied recursively. Other objects will be
     * returned as-is.
     *
     * @param obj            the object to unwrap
     *
     * @return the unwrapped object
     *
     * @see org.rapidcontext.core.data.Array
     * @see org.rapidcontext.core.data.Dict
     */
    public static Object unwrap(Object obj) {
        if (obj == null || obj == UniqueTag.NULL_VALUE) {
            return null;
        } else if (obj instanceof Undefined || obj == UniqueTag.NOT_FOUND) {
            return null;
        } else if (obj instanceof Wrapper) {
            return ((Wrapper) obj).unwrap();
        } else if (obj instanceof CharSequence) {
            return obj.toString();
        } else if (obj instanceof NativeArray) {
            NativeArray nativeArr = (NativeArray) obj;
            int length = (int) nativeArr.getLength();
            Array arr = new Array(length);
            for (int i = 0; i < length; i++) {
                arr.set(i, unwrap(nativeArr.get(i, nativeArr)));
            }
            return arr;
        } else if (obj instanceof Scriptable) {
            Scriptable scr = (Scriptable) obj;
            Object[] keys = scr.getIds();
            Dict dict = new Dict(keys.length);
            for (Object k : keys) {
                String str = k.toString();
                Object value = null;
                if (k instanceof Integer) {
                    value = scr.get(((Integer) k).intValue(), scr);
                } else {
                    value = scr.get(str, scr);
                }
                if (str != null && str.length() > 0) {
                    dict.set(str, unwrap(value));
                }
            }
            return dict;
        } else {
            return obj;
        }
    }

    // No instances
    private JsRuntime() {}
}
