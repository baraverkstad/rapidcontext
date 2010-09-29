/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg & Dynabyte AB.
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

import java.io.File;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.http.HttpSession;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.Restricted;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.web.SessionFileMap;
import org.rapidcontext.core.web.SessionManager;
import org.rapidcontext.util.DateUtil;

/**
 * The built-in current session info procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class SessionCurrentProcedure implements Procedure, Restricted {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.Session.Current";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new current session info procedure.
     */
    public SessionCurrentProcedure() {
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
        return true;
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
        return "Returns information about the originating user session.";
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

        HttpSession  session;

        session = SessionManager.getCurrentSession();
        return (session == null) ? null : getSessionData(session);
    }

    /**
     * Creates a data object with information about a session.
     *
     * @param session        the HTTP session
     *
     * @return a data object with session information
     */
    public static Dict getSessionData(HttpSession session) {
        Dict            res = new Dict();
        Dict            files = new Dict();
        Dict            data;
        Date            date;
        SessionFileMap  fileMap;
        Iterator        iter;
        String          name;
        File            file;

        res.set("id", session.getId());
        date = new Date(session.getCreationTime());
        res.set("creationMillis", String.valueOf(date.getTime()));
        res.set("creationDate", DateUtil.formatIsoDateTime(date));
        date = new Date(session.getLastAccessedTime());
        res.set("lastAccessMillis", String.valueOf(date.getTime()));
        res.set("lastAccessDate", DateUtil.formatIsoDateTime(date));
        res.set("ip", SessionManager.getIp(session));
        data = null;
        name = SessionManager.getUser(session);
        if (name != null) {
            data = SecurityContext.getUser(name).getData().copy();
            data.remove("password");
        }
        res.set("user", data);
        fileMap = SessionFileMap.getFiles(session, false);
        if (fileMap != null) {
            if (fileMap.getProgress() < 1.0d) {
                files.set("progress", String.valueOf(fileMap.getProgress()));
            }
            iter = fileMap.getAllFiles().keySet().iterator();
            while (iter.hasNext()) {
                name = (String) iter.next();
                file = fileMap.getFile(name);
                data = new Dict();
                data.set("name", file.getName());
                data.set("size", String.valueOf(file.length()));
                data.set("mimeType",
                         session.getServletContext().getMimeType(file.getName()));
                date = new Date(file.lastModified());
                data.set("creationMillis", String.valueOf(date.getTime()));
                data.set("creationDate", DateUtil.formatIsoDateTime(date));
                files.set(name, data);
            }
        }
        res.set("files", files);
        return res;
    }
}
