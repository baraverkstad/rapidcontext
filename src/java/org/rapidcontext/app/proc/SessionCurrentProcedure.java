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

import java.io.File;
import java.util.Date;

import org.rapidcontext.app.model.RequestContext;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.Session;
import org.rapidcontext.core.type.User;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.util.DateUtil;

/**
 * The built-in current session info procedure.
 *
 * @author Per Cederberg
 */
public class SessionCurrentProcedure extends Procedure {

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public SessionCurrentProcedure(String id, String type, Dict dict) {
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

        Session session = RequestContext.active().session();
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
        Dict res = session.serialize();
        res.set("creationDate", DateUtil.asDateTimeUTC(session.createTime()));
        res.set("lastAccessDate", DateUtil.asDateTimeUTC(session.accessTime()));
        String userId = session.userId();
        User user = (userId == null) ? null : User.find(storage, userId);
        res.set("user", StorableObject.sterilize(user, true, true, true));
        res.set("nonce", SecurityContext.nonce());
        Dict dict = new Dict();
        for (String id : session.files().keys()) {
            dict.set(id, serialize(session.file(id)));
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
        Date date = new Date(file.lastModified());
        return new Dict()
            .set("name", file.getName())
            .set("size", String.valueOf(file.length()))
            .set("mimeType", Mime.type(file))
            .set("creationTime", date)
            .set("creationDate", DateUtil.asDateTimeUTC(date));
    }
}
