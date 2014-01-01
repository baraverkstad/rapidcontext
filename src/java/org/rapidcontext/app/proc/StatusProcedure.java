/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2014 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE.txt file for more details.
 */

package org.rapidcontext.app.proc;

import java.util.Date;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.plugin.PluginManager;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Environment;
import org.rapidcontext.core.type.User;

/**
 * The built-in status information procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StatusProcedure implements Procedure {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.Status";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new status information procedure.
     */
    public StatusProcedure() {
        this.defaults.seal();
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
        return "Returns information about the current status.";
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

        ApplicationContext ctx = ApplicationContext.getInstance();
        Dict res = (Dict) cx.getStorage().load(PluginManager.PATH_INFO);
        if (res == null) {
            return null;
        }
        res.set("guid", ctx.getConfig().get("guid"));
        res.set("environment", getEnvironmentData(cx.getEnvironment()));
        // TODO: Allow multiple security realms (depending on web site)
        res.set("realm", User.DEFAULT_REALM);
        res.set("initTime", ApplicationContext.INIT_TIME);
        res.set("startTime", ApplicationContext.START_TIME);
        res.set("currentTime", new Date());
        return res;
    }

    /**
     * Returns a data object for an environment.
     *
     * @param env            the environment object
     *
     * @return the corresponding data object
     */
    private Dict getEnvironmentData(Environment env) {
        if (env != null) {
            Dict res = new Dict();
            res.set("name", env.id());
            res.set("description", env.description());
            return res;
        } else {
            return null;
        }
    }
}
