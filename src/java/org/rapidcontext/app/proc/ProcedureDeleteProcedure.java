/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2013 Per Cederberg. All rights reserved.
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

import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;

/**
 * The built-in procedure delete procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ProcedureDeleteProcedure implements Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(ProcedureDeleteProcedure.class.getName());

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.Procedure.Delete";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new procedure delete procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public ProcedureDeleteProcedure() throws ProcedureException {
        defaults.set("name", Bindings.ARGUMENT, "", "The procedure name");
        defaults.seal();
    }

    /**
     * Returns the procedure name.
     *
     * @return the procedure name
     */
    public String getName() {
        return NAME;
    }

    /**
     * Returns the procedure description.
     *
     * @return the procedure description
     */
    public String getDescription() {
        return "Deletes a procedure from the local plug-in. If the " +
               "procedure exists in another plug-in, that version will be " +
               "kept intact.";
    }

    /**
     * Returns the bindings for this procedure. If this procedure
     * requires any special data, adapter connection or input
     * argument binding, those bindings should be set (but possibly
     * to null or blank values).
     *
     * @return the bindings for this procedure
     */
    public Bindings getBindings() {
        return defaults;
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
        if (name.length() == 0) {
            throw new ProcedureException("invalid procedure name");
        }
        CallContext.checkWriteAccess("procedure/" + name);
        LOG.info("deleting procedure " + name);
        cx.getLibrary().deleteProcedure(name);
        return null;
    }
}
