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

package org.rapidcontext.app.proc;

import java.util.Date;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.plugin.PluginManager;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Environment;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.User;

/**
 * The built-in status information procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StatusProcedure extends Procedure {

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public StatusProcedure(String id, String type, Dict dict) {
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

        return getStatusData();
    }

    /**
     * Returns a data object for the current server status.
     *
     * @return the status data object
     */
    public static Dict getStatusData() {
        ApplicationContext ctx = ApplicationContext.getInstance();
        Dict res = (Dict) ctx.getStorage().load(PluginManager.PATH_INFO);
        if (res == null) {
            return null;
        }
        res.set("guid", ctx.getConfig().get("guid"));
        res.set("environment", getEnvironmentData(ctx.getEnvironment()));
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
    private static Dict getEnvironmentData(Environment env) {
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
