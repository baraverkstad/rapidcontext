/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2009 Per Cederberg & Dynabyte AB.
 * All rights reserved.
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

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Data;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.Restricted;
import org.rapidcontext.core.security.SecurityContext;

/**
 * The built-in thread list procedure.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class ThreadListProcedure implements Procedure, Restricted {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.Thread.List";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new thread list procedure.
     */
    public ThreadListProcedure() {
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
        return SecurityContext.hasAdmin();
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
        return "Returns a list of all currently running threads.";
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

        Data         res = new Data(40);
        ThreadGroup  root;

        root = Thread.currentThread().getThreadGroup().getParent();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        listThreads(root, res);
        return res;
    }

    /**
     * Retrieves information about all threads running in the JVM
     * and adds information about them to a list.
     *
     * @param group          the thread group to visit
     * @param list           the data list
     */
    private void listThreads(ThreadGroup group, Data list) {
        Thread[]       threads;
        ThreadGroup[]  groups;
        int            size;
        Data           data;
        CallContext    cx;

        size = group.activeCount();
        threads = new Thread[size * 2];
        size = group.enumerate(threads, false);
        for (int i = 0; i < size; i++) {
            data = new Data();
            data.setInt("id", threads[i].hashCode());
            data.set("name", threads[i].getName());
            data.setInt("priority", threads[i].getPriority());
            data.set("group", group.getName());
            data.setBoolean("daemon", threads[i].isDaemon());
            data.setBoolean("alive", threads[i].isAlive());
            cx = ApplicationContext.getInstance().findContext(threads[i]);
            if (cx == null) {
                data.set("context", null);
            } else {
                data.set("context", ThreadContextProcedure.getContextData(cx));
            }
            list.add(data);
        }
        size = group.activeGroupCount();
        groups = new ThreadGroup[size * 2];
        size = group.enumerate(groups, false);
        for (int i = 0; i < size; i++) {
            listThreads(groups[i], list);
        }
    }
}
