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

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.Wrapper;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Procedure;

/**
 * A JavaScript procedure function wrapper. This class encapsulates a
 * procedure and allows it to be called from JavaScript.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
class ProcedureWrapper extends BaseFunction implements Wrapper {

    /**
     * The procedure library.
     */
    private CallContext cx;

    /**
     * The wrapped procedure.
     */
    private Procedure proc;

    /**
     * Creates a new procedure wrapper call function.
     *
     * @param cx             the procedure call context
     * @param proc           the procedure definition
     * @param parentScope    the object parent scope
     */
    ProcedureWrapper(CallContext cx, Procedure proc, Scriptable parentScope) {
        super(parentScope, getFunctionPrototype(parentScope));
        this.cx = cx;
        this.proc = proc;
    }

    /**
     * Returns the class name.
     *
     * @return the class name
     */
    @Override
    public String getClassName() {
        return "ProcedureWrapper";
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
        switch (name) {
        case "name":
            return "wrapped " + this.proc.id();
        case "arity", "length":
            Bindings bindings = proc.getBindings();
            int args = 0;
            try {
                for (String key : bindings.getNames()) {
                    if (bindings.getType(key) == Bindings.ARGUMENT) {
                        args++;
                    }
                }
            } catch (ProcedureException ignore) {
                // Ignore this exception
            }
            return args;
        default:
            return super.get(name, start);
        }
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
    @Override
    public Object call(Context scriptContext,
                       Scriptable scope,
                       Scriptable thisObj,
                       Object[] args) {

        if (!(proc instanceof JsProcedure)) {
            for (int i = 0; i < args.length; i++) {
                args[i] = JsRuntime.unwrap(args[i]);
            }
        }
        try {
            return JsRuntime.wrap(cx.call(proc, args), scope);
        } catch (ProcedureException e) {
            throw new WrappedException(e);
        }
    }

    /**
     * Returns the wrapped object.
     *
     * @return the unwrapped object
     */
    @Override
    public Object unwrap() {
        return proc;
    }
}
