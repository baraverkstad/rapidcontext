/*
 * RapidContext JDBC plug-in <https://www.rapidcontext.com/>
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

package org.rapidcontext.app.plugin.test;

import java.math.BigInteger;
import java.util.Random;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Procedure;

/**
 * The built-in native values test procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class NativeValuesProcedure extends Procedure {

	/**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public NativeValuesProcedure(String id, String type, Dict dict) {
        super(id, type, dict);
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

        Object input = bindings.getValue("input");
        Random r = new Random();
        Dict res = new Dict();
        add(res, "int-max", Integer.MAX_VALUE);
        add(res, "int-min", Integer.MIN_VALUE);
        add(res, "int-random", r.nextInt());
        add(res, "long-max", Long.MAX_VALUE);
        add(res, "long-min", Long.MIN_VALUE);
        add(res, "long-random", r.nextLong());
        add(res, "bigint-10", BigInteger.TEN);
        add(res, "bigint-any", new BigInteger("12345678901234567890"));
        add(res, "float-max-value", Float.MAX_VALUE);
        add(res, "float-min-value", Float.MIN_VALUE);
        add(res, "float-min-normal", Float.MIN_NORMAL);
        add(res, "float-10", 10.0f);
        add(res, "double-max-value", Double.MAX_VALUE);
        add(res, "double-min-value", Double.MIN_VALUE);
        add(res, "double-min-normal", Double.MIN_NORMAL);
        add(res, "double-10", 10.0d);
        add(res, "bool-true", true);
        add(res, "bool-false", false);
        res.add("str", "string value");
        add(res, "input", input);
        res.add("input-type", input.getClass().getName());
        return res;
    }

    private void add(Dict dict, String key, Object val) {
        dict.add(key + "-val", val);
        dict.add(key + "-str", val.toString());
    }
}
