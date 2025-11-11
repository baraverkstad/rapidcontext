/*
 * RapidContext <https://www.rapidcontext.com/>
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

package org.rapidcontext.app.proc;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.rapidcontext.app.model.ApiUtil;
import org.rapidcontext.app.model.RequestContext;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.Session;
import org.rapidcontext.core.type.User;

/**
 * The built-in procedure call procedure.
 *
 * @author Per Cederberg
 */
public class ProcedureCallProcedure extends Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(ProcedureCallProcedure.class.getName());

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public ProcedureCallProcedure(String id, String type, Dict dict) {
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

        String name = (String) bindings.getValue("name");
        if (cx.isCalledBy(cx.procedure())) {
            throw new SecurityException("recursive calls not allowed");
        }
        cx.requireReadAccess("procedure/" + name);
        Object[] args = null;
        Object obj = bindings.getValue("arguments");
        if (obj instanceof Array a) {
            args = a.values();
        } else {
            args = new Object[1];
            args[0] = obj;
        }
        Dict opts = ApiUtil.options("-", bindings.getValue("opts", ""));
        int delay = opts.get("delay", Integer.class, 0);
        if (delay > 60 * 60 * 1000) {
            throw new ProcedureException(this, "delay must be less than 1 hour");
        }
        if (delay > 0) {
            callAsync(cx, name, args, delay);
            return null;
        } else {
            return CallContext.execute(name, args);
        }
    }

    /**
     * Calls a procedure asynchronously.
     *
     * @param cx             the current call context
     * @param name           the procedure name
     * @param args           the procedure arguments
     * @param delay          the delay in milliseconds
     */
    @SuppressWarnings("resource")
    private void callAsync(CallContext cx,String name, Object[] args, int delay) {
        Session session = cx.session();
        User user = cx.user();
        cx.scheduler().schedule(
            () -> {
                RequestContext ctx = RequestContext.initAsync(session, user);
                try {
                    return CallContext.execute(name, args);
                } catch (Exception e) {
                    LOG.info("async call to " + name + " by " + user + " failed: " + e);
                    return null;
                } finally {
                    ctx.close();
                }
            },
            delay,
            TimeUnit.MILLISECONDS
        );
    }
}
