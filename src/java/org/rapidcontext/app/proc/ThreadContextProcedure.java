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

package org.rapidcontext.app.proc;

import java.util.Date;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.User;
import org.rapidcontext.util.DateUtil;

/**
 * The built-in thread context retrieval procedure.
 *
 * @author Per Cederberg
 *
 * @deprecated Thread introspection will be removed in a future release.
 */
@Deprecated(forRemoval = true)
public class ThreadContextProcedure extends Procedure {

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public ThreadContextProcedure(String id, String type, Dict dict) {
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
    @SuppressWarnings("removal")
    public Object call(CallContext cx, Bindings bindings)
        throws ProcedureException {

        String str = bindings.getValue("threadId").toString();
        int threadId = 0;
        try {
            threadId = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new ProcedureException(this, "invalid thread id: " + str);
        }
        CallContext tcx = ApplicationContext.active().findContext(threadId);
        if (tcx == null) {
            return null;
        }
        User user = tcx.user();
        boolean isOwner = user != null && user == cx.user();
        if (isOwner) {
            return getContextData(tcx);
        } else {
            cx.requireReadAccess("thread/" + threadId);
            return getContextData(tcx);
        }
    }

    /**
     * Converts a procedure call context into a data object.
     *
     * @param cx             the procedure call context
     *
     * @return the data object
     */
    @SuppressWarnings("removal")
    static Dict getContextData(CallContext cx) {
        Dict res = new Dict();
        CallContext top = cx.top();
        Procedure proc = top.procedure();
        res.set("procedure", (proc == null) ? null : proc.id());
        Date startTime = top.created();
        if (startTime == null) {
            res.set("startMillis", null);
            res.set("startTime", null);
        } else {
            res.set("startMillis", String.valueOf(startTime.getTime()));
            res.set("startTime", DateUtil.formatIsoTime(startTime));
        }
        Date endTime = (Date) top.getAttribute(CallContext.ATTRIBUTE_END_TIME);
        if (endTime == null) {
            res.set("endMillis", null);
            res.set("endTime", null);
        } else {
            res.set("endMillis", String.valueOf(endTime.getTime()));
            res.set("endTime", DateUtil.formatIsoTime(endTime));
        }
        Number progress = (Number) top.getAttribute(CallContext.ATTRIBUTE_PROGRESS);
        if (endTime != null) {
            res.set("progress", "1.0");
        } else if (startTime == null || progress == null) {
            res.set("progress", "0.0");
        } else if (progress.doubleValue() / 100.0 < 0.0) {
            res.set("progress", "0.0");
        } else if (progress.doubleValue() / 100.0 > 1.0) {
            res.set("progress", "1.0");
        } else {
            res.set("progress", String.valueOf(progress.doubleValue() / 100.0));
        }
        User user = cx.user();
        if (user == null) {
            res.set("user", null);
        } else {
            res.set("user", user.id());
        }
        res.set("source", top.getAttribute(CallContext.ATTRIBUTE_SOURCE));
        res.set("result", top.getAttribute(CallContext.ATTRIBUTE_RESULT));
        res.set("error", top.getAttribute(CallContext.ATTRIBUTE_ERROR));
        res.set("log", cx.log());
        res.set("stack", Array.from(cx.getCallStack().toStackTrace(20)));
        return res;
    }
}
