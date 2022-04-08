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

import java.util.logging.Logger;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.StorageException;
import org.rapidcontext.core.type.User;

/**
 * The built-in user settings procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class UserSettingsProcedure implements Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(UserSettingsProcedure.class.getName());

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.User.Settings";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new user settings procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public UserSettingsProcedure() throws ProcedureException {
        defaults.set("userId", Bindings.ARGUMENT, "",
                     "The unique user id, or null for current user");
        defaults.set("settings", Bindings.ARGUMENT, "",
                     "The dictionary of settings keys to modify, " +
                     "use null values to remove existing keys");
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
        return "Updates settings data for a user.";
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

        User user = null;
        String id = (String) bindings.getValue("userId", "");
        Object data = bindings.getValue("settings");
        if (id == null || id.trim().equals("")) {
            user = SecurityContext.currentUser();
        } else {
            CallContext.checkWriteAccess("user/" + id);
            user = User.find(cx.getStorage(), id);
        }
        if (user == null) {
            throw new ProcedureException("cannot find user with id " + id);
        }
        if (data instanceof Dict) {
            user.updateSettings((Dict) data);
            try {
                LOG.info("updating " + user + " settings");
                User.store(cx.getStorage(), user);
            } catch (StorageException e) {
                throw new ProcedureException(e.getMessage());
            }
        }
        return user.settings();
    }
}
