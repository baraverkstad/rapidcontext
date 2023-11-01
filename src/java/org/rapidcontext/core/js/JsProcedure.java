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

import java.util.ArrayList;
import java.util.logging.Logger;

import org.apache.commons.lang3.RegExUtils;
import org.mozilla.javascript.Function;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Procedure;

/**
 * A JavaScript procedure. This procedure will execute generic
 * JavaScript code allowing other procedures to be called.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class JsProcedure extends Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG = Logger.getLogger(JsProcedure.class.getName());

    /**
     * The binding name for the JavaScript code.
     */
    public static final String BINDING_CODE = "code";

    /**
     * The compiled JavaScript function.
     */
    private Function func = null;

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public JsProcedure(String id, String type, Dict dict) {
        super(id, type, dict);
        if (!type.equals("procedure/javascript")) {
            this.dict.set(KEY_TYPE, "procedure/javascript");
            LOG.warning("deprecated: procedure " + id + " references legacy type: " + type);
        }
    }

    /**
     * Checks if this script has been compiled.
     *
     * @return true if this script has been compiled, or
     *         false otherwise
     */
    public boolean isCompiled() {
        return func != null;
    }

    /**
     * Executes a call of this procedure in the specified context
     * and with the specified call bindings. The semantics of what
     * the procedure actually does, is up to each implementation.
     * Note that the call bindings are normally inherited from the
     * procedure bindings with arguments bound to their call values.
     *
     * @param cx             the procedure call context
     * @param bindings       the call bindings to use
     *
     * @return the result of the call, or
     *         null if the call produced no result
     *
     * @throws ProcedureException if the call execution caused an
     *             error
     */
    @Override
    public Object call(CallContext cx, Bindings bindings)
        throws ProcedureException {

        if (func == null) {
            compile();
        }
        ArrayList<Object> args = new ArrayList<>();
        for (String arg : bindings.getNames()) {
            // Note: All bindings are added as arguments here, as scope
            //       variables are bound at compile-time...
            if (!BINDING_CODE.equals(arg)) {
                args.add(bindings.getValue(arg, null));
            }
        }
        try {
            Object res = JsRuntime.call(func, args.toArray(), cx);
            boolean unwrapResult = (cx.getCallStack().height() <= 1);
            return unwrapResult ? JsRuntime.unwrap(res) : res;
        } catch (JsException e) {
            throw new ProcedureException(this, e);
        }
    }

    /**
     * Compiles the script code. This will be done automatically the
     * first time the procedure is run, but may be practical to do at
     * other times as well in order to detect errors.
     *
     * @throws ProcedureException if the script couldn't be compiled
     *             correctly
     */
    public void compile() throws ProcedureException {
        String name = RegExUtils.replaceAll(id(), "[^a-zA-Z0-9_$]", "_");
        String code = (String) getBindings().getValue(BINDING_CODE);
        ArrayList<String> args = new ArrayList<>();
        for (String arg : getBindings().getNames()) {
            // Note: All bindings are added as arguments here, as scope
            //       variables are bound at compile-time...
            if (!BINDING_CODE.equals(arg)) {
                args.add(arg);
            }
        }
        this.func = null;
        try {
            String[] argNames = args.toArray(new String[args.size()]);
            this.func = JsRuntime.compile(name, argNames, code);
        } catch (Exception e) {
            throw new ProcedureException(this, e);
        }
    }
}
