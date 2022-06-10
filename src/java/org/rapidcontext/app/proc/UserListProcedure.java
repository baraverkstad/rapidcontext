/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2022 Per Cederberg. All rights reserved.
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
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.User;

/**
 * The built-in user list procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class UserListProcedure implements Procedure {

    // TODO: Replace this procedure with Query API?

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.User.List";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new user list procedure.
     */
    public UserListProcedure() {
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
        return "Returns a list of all users.";
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

        CallContext.checkSearchAccess(User.PATH.toString());
        Storage storage = cx.getStorage();
        Array res = new Array();
        storage.query(User.PATH)
            .filterReadAccess()
            .objects(User.class)
            .forEach(user -> res.add(serialize(user)));
        res.sort("id");
        return res;
    }

    /**
     * Serializes a user object to a dictionary.
     *
     * @param user           the user to serialize
     *
     * @return the serialized dictionary for the user
     */
    public static Dict serialize(User user) {
        Dict dict = user.serialize();
        dict.remove("password");
        if (!dict.containsKey("role")) {
            dict.set("role", new Array(0));
        }
        return dict;
    }
}
