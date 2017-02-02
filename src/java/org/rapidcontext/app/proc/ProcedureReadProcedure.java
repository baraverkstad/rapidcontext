/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2017 Per Cederberg. All rights reserved.
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

import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.AddOnProcedure;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Library;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;

/**
 * The built-in procedure read procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ProcedureReadProcedure implements Procedure {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.Procedure.Read";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new procedures read procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public ProcedureReadProcedure() throws ProcedureException {
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
        return "Returns detailed information about a procedure.";
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

        String name = (String) bindings.getValue("name");
        CallContext.checkAccess("procedure/" + name, cx.readPermission(1));
        Procedure proc = cx.getLibrary().getProcedure(name);
        return getProcedureData(cx.getLibrary(), proc);
    }

    /**
     * Converts a procedure object into a data object.
     *
     * @param library        the library to use for plug-in info
     * @param proc           the procedure
     *
     * @return the data object created
     *
     * @throws ProcedureException if the bindings data access
     *             failed
     */
    static Dict getProcedureData(Library library, Procedure proc)
    throws ProcedureException {
        Dict res = new Dict();
        res.set("name", proc.getName());
        if (proc instanceof AddOnProcedure) {
            res.set("type", ((AddOnProcedure) proc).getType());
        } else {
            res.set("type", "built-in");
        }
        res.set("plugin", library.getProcedurePluginId(proc.getName()));
        res.set("description", proc.getDescription());
        res.set("bindings", getBindingsData(proc.getBindings()));
        return res;
    }

    /**
     * Converts a procedure bindings object into a data object.
     *
     * @param bindings       the procedure bindings
     *
     * @return the bindings data array object
     *
     * @throws ProcedureException if the bindings data access
     *             failed
     */
    static Array getBindingsData(Bindings bindings) throws ProcedureException {
        String[] names = bindings.getNames();
        Array res = new Array(names.length);
        for (int i = 0; i < names.length; i++) {
            Dict dict = new Dict();
            dict.set("name", names[i]);
            dict.set("type", bindings.getTypeName(names[i]));
            dict.set("value", bindings.getValue(names[i], ""));
            dict.set("description", bindings.getDescription(names[i]));
            res.add(dict);
        }
        return res;
    }
}
