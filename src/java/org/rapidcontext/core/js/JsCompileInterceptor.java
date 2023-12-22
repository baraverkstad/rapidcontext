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

import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Interceptor;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;

/**
 * A JavaScript compile procedure call interceptor. This interceptor
 * makes sure that all JavaScript procedures are compiled during the
 * reservation phase before the actual calls.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class JsCompileInterceptor extends Interceptor {

    /**
     * Creates a new JavaScript compile interceptor.
     *
     * @param parent         the parent interceptor
     */
    public JsCompileInterceptor(Interceptor parent) {
        super(parent);
    }

    /**
     * Reserves all adapter connections needed for executing the
     * specified procedure. All connections needed by imported
     * procedures will also be reserved recursively. This method also
     * compiles the JavaScript source code.
     *
     * @param cx             the procedure context
     * @param proc           the procedure definition
     *
     * @throws ProcedureException if the connections couldn't be
     *             reserved
     */
    @Override
    public void reserve(CallContext cx, Procedure proc)
    throws ProcedureException {

        if (proc instanceof JsProcedure js) {
            if (!js.isCompiled()) {
                js.compile();
            }
        }
        super.reserve(cx, proc);
    }
}
