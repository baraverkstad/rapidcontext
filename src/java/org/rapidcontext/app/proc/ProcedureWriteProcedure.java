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

package org.rapidcontext.app.proc;

import java.util.logging.Logger;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Procedure;

/**
 * The built-in procedure write procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ProcedureWriteProcedure extends Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(ProcedureWriteProcedure.class.getName());

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public ProcedureWriteProcedure(String id, String type, Dict dict) {
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
    public Object call(CallContext cx, Bindings bindings)
        throws ProcedureException {

        String name = ((String) bindings.getValue("name")).trim();
        String type = ((String) bindings.getValue("type")).trim();
        if (name.isEmpty()) {
            throw new ProcedureException("missing procedure name");
        } else if (type.isEmpty()) {
            throw new ProcedureException("missing procedure type");
        }
        CallContext.checkWriteAccess("procedure/" + name);
        LOG.info("writing procedure " + name);
        Dict dict = new Dict();
        dict.set("description", bindings.getValue("description", ""));
        dict.set("binding", bindings.getValue("bindings"));
        org.rapidcontext.core.proc.Procedure proc = cx.getLibrary().storeProcedure(name, type, dict);
        return ProcedureReadProcedure.getProcedureData(cx.getLibrary(), proc);
    }
}
