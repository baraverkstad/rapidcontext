/*
 * RapidContext <https://www.rapidcontext.com/>
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

package org.rapidcontext.core.js;

import java.util.logging.Logger;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.proc.ReserveInterceptor;
import org.rapidcontext.core.type.Procedure;

/**
 * A JavaScript procedure compile interceptor. This interceptor ensures that
 * all JavaScript procedures are compiled during the resource reservation
 * phase, prior to executing any calling procedure (i.e. fail early)
 *
 * @author Per Cederberg
 */
public class JsCompileInterceptor extends ReserveInterceptor {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(JsCompileInterceptor.class.getName());

    /**
     * Creates a new interceptor from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public JsCompileInterceptor(String id, String type, Dict dict) {
        super(id, type, dict);
    }

    /**
     * Reserves all resources needed for executing a procedure. All
     * resources needed by sub-procedures will also be reserved.
     *
     * @param cx             the procedure context
     * @param proc           the procedure definition
     *
     * @throws ProcedureException if some resource couldn't be reserved
     */
    @Override
    public void reserve(CallContext cx, Procedure proc)
    throws ProcedureException {

        if (proc instanceof JsProcedure js && !js.isCompiled()) {
            LOG.fine("compiling " + js + "...");
            js.compile();
        }
        next(ReserveInterceptor.class).reserve(cx, proc);
    }
}
