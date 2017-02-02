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

import java.io.File;
import java.util.Date;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.Session;
import org.rapidcontext.core.type.User;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.util.DateUtil;

/**
 * The built-in current session info procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class SessionCurrentProcedure implements Procedure {

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

        Session session = (Session) Session.activeSession.get();
        return (session == null) ? null : serialize(cx.getStorage(), session);
    }

    /**
     * Serializes a session for usage in a procedure response.
     *
     * @param storage        the storage to use for lookups
     * @param session        the session object
     *
     * @return the serialized session dictionary
     */
    public static Dict serialize(Storage storage, Session session) {
        Dict res = session.serialize().copy();
        res.set("creationDate", DateUtil.formatIsoDateTime(session.createTime()));
        res.set("lastAccessDate", DateUtil.formatIsoDateTime(session.accessTime()));
        String userId = session.userId();
        User user = (userId == null) ? null : User.find(storage, userId);
        if (user == null) {
            res.set("user", null);
            res.set("nonce", "" + session.accessTime().getTime());
        } else {
            res.set("user", UserListProcedure.serialize(user));
        }
        Dict dict = new Dict();
        String[] ids = session.files().keys();
        for (int i = 0; i < ids.length; i++) {
            dict.set(ids[i], serialize(session.file(ids[i])));
        }
        res.remove(Session.KEY_FILES);
        res.set("files", dict);
        return res;
    }

    /**
     * Serializes a file for usage in a procedure response.
     *
     * @param file           the file object
     *
     * @return the serialized file dictionary
     */
    public static Dict serialize(File file) {
        Dict dict = new Dict();
        dict.set("name", file.getName());
        dict.set("size", String.valueOf(file.length()));
        dict.set("mimeType", Mime.type(file));
        Date date = new Date(file.lastModified());
        dict.set("creationTime", date);
        dict.set("creationDate", DateUtil.formatIsoDateTime(date));
        return dict;
    }
}
