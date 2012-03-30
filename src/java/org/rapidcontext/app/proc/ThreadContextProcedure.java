/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2012 Per Cederberg. All rights reserved.
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
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.CallStack;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.Restricted;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.type.User;
import org.rapidcontext.util.DateUtil;

/**
 * The built-in thread context retrieval procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ThreadContextProcedure implements Procedure, Restricted {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.Thread.Context";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new thread context retrieval procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public ThreadContextProcedure() throws ProcedureException {
        defaults.set("threadId", Bindings.ARGUMENT, "", "The thread id");
        defaults.seal();
    }

    /**
     * Checks if the currently authenticated user has access to this
     * object.
     *
     * @return true if the current user has access, or
     *         false otherwise
     */
    public boolean hasAccess() {
        return SecurityContext.currentUser() != null;
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
        return "Returns the call context for a specified (running) thread.";
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

        int     threadId;
        User    user;
        String  str;

        str = bindings.getValue("threadId").toString();
        try {
            threadId = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new ProcedureException("invalid thread id: " + str);
        }
        cx = ApplicationContext.getInstance().findContext(threadId);
        if (cx == null) {
            return null;
        }
        user = (User) cx.getAttribute(CallContext.ATTRIBUTE_USER);
        if (SecurityContext.hasAdmin() ||
            user != null && user == SecurityContext.currentUser()) {

            return getContextData(cx);
        } else {
            return null;
        }
    }

    /**
     * Converts a procedure call context into a data object.
     *
     * @param cx             the procedure call context
     *
     * @return the data object
     */
    static Dict getContextData(CallContext cx) {
        Dict          res = new Dict();
        Procedure     proc;
        Date          startTime;
        Date          endTime;
        Number        progress;
        User          user;
        StringBuffer  log;
        CallStack     stack = cx.getCallStack();
        Procedure[]   procs;
        Array         list;

        proc = (Procedure) cx.getAttribute(CallContext.ATTRIBUTE_PROCEDURE);
        if (proc == null) {
            res.set("procedure", null);
        } else {
            res.set("procedure", proc.getName());
        }
        startTime = (Date) cx.getAttribute(CallContext.ATTRIBUTE_START_TIME);
        if (startTime == null) {
            res.set("startMillis", null);
            res.set("startTime", null);
        } else {
            res.set("startMillis", String.valueOf(startTime.getTime()));
            res.set("startTime", DateUtil.formatIsoTime(startTime));
        }
        endTime = (Date) cx.getAttribute(CallContext.ATTRIBUTE_END_TIME);
        if (endTime == null) {
            res.set("endMillis", null);
            res.set("endTime", null);
        } else {
            res.set("endMillis", String.valueOf(endTime.getTime()));
            res.set("endTime", DateUtil.formatIsoTime(endTime));
        }
        progress = (Number) cx.getAttribute(CallContext.ATTRIBUTE_PROGRESS);
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
        user = (User) cx.getAttribute(CallContext.ATTRIBUTE_USER);
        if (user == null) {
            res.set("user", null);
        } else {
            res.set("user", user.id());
        }
        res.set("source", cx.getAttribute(CallContext.ATTRIBUTE_SOURCE));
        res.set("result", cx.getAttribute(CallContext.ATTRIBUTE_RESULT));
        res.set("error", cx.getAttribute(CallContext.ATTRIBUTE_ERROR));
        log = (StringBuffer) cx.getAttribute(CallContext.ATTRIBUTE_LOG_BUFFER);
        res.set("log", (log == null) ? "" : log.toString());
        procs = stack.toArray();
        list = new Array(procs.length);
        for (int i = 0; i < procs.length; i++) {
            list.add(procs[i].getName());
        }
        res.set("stack", list);
        return res;
    }
}
