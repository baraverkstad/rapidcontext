/*
 * RapidContext JDBC plug-in <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2026 Per Cederberg. All rights reserved.
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

import java.util.Date;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Procedure;

/**
 * The built-in native values test procedure.
 *
 * @author Per Cederberg
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
        Dict res = new Dict();
        add(res, "int-std", 1234);
        add(res, "int-max", Integer.MAX_VALUE);
        add(res, "int-min", Integer.MIN_VALUE);
        add(res, "long-std", 1234L);
        add(res, "long-max", Long.MAX_VALUE);
        add(res, "long-min", Long.MIN_VALUE);
        add(res, "float-std", 12.34f);
        add(res, "float-max", Float.MAX_VALUE);
        add(res, "float-min", Float.MIN_VALUE);
        add(res, "double-std", 12.34d);
        add(res, "double-max", Double.MAX_VALUE);
        add(res, "double-min", Double.MIN_VALUE);
        add(res, "bool-true", true);
        add(res, "bool-false", false);
        res.add("str", "string value");
        res.add("date", new Date(1234567890));
        add(res, "input", input);
        res.add("input-type", input.getClass().getName());
        return res;
    }

    private void add(Dict dict, String key, Object val) {
        dict.add(key + "-val", val);
        dict.add(key + "-str", val.toString());
    }
}
