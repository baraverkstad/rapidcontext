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

import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.Restricted;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.StorageException;
import org.rapidcontext.core.type.User;

/**
 * The built-in user modification procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class UserChangeProcedure implements Procedure, Restricted {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.User.Change";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new user modification procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public UserChangeProcedure() throws ProcedureException {
        defaults.set("id", Bindings.ARGUMENT, "",
                     "The unique user id");
        defaults.set("name", Bindings.ARGUMENT, "",
                     "The user real name");
        defaults.set("description", Bindings.ARGUMENT, "",
                     "The user description");
        defaults.set("enabled", Bindings.ARGUMENT, "",
                     "The enabled flag (0 or 1)");
        defaults.set("password", Bindings.ARGUMENT, "",
                     "The new password, or blank for unchanged");
        defaults.set("roles", Bindings.ARGUMENT, "",
                     "The list of roles (separated by spaces)");
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
        return "Creates or updates a user.";
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

        User      user;
        String    id;
        String    name;
        String    descr;
        boolean   enabled;
        String    pwd;
        String[]  roles;
        Array     list;
        String    str;
        Object    obj;

        // Validate arguments
        id = bindings.getValue("id").toString();
        if (id.equals("")) {
            throw new ProcedureException("user id cannot be blank");
        } else if (!id.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            throw new ProcedureException("user id contains invalid character");
        }
        user = User.find(cx.getStorage(), id);
        name = bindings.getValue("name").toString();
        descr = bindings.getValue("description").toString();
        str = bindings.getValue("enabled").toString();
        enabled = (!str.equals("") && !str.equals("false") && !str.equals("0"));
        pwd = bindings.getValue("password").toString();
        if ((user == null || pwd.length() > 0) && pwd.length() < 5) {
            throw new ProcedureException("password must be at least 5 characters");
        }
        obj = bindings.getValue("roles");
        if (obj instanceof Array) {
            list = (Array) obj;
            roles = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                roles[i] = list.get(i).toString();
            }
        } else {
            roles = obj.toString().split("[ ,]+");
        }

        // Create or modify user
        if (user == null) {
            user = new User(id);
            str = user.id() + " created";
        } else {
            str = user.id() + " modified";
        }
        user.setName(name);
        user.setDescription(descr);
        user.setEnabled(enabled);
        if (pwd.length() > 0) {
            user.setPassword(pwd);
        }
        user.setRoles(roles);
        try {
            SecurityContext.saveUser(user);
        } catch (StorageException e) {
            throw new ProcedureException(e.getMessage());
        } catch (SecurityException e) {
            throw new ProcedureException(e.getMessage());
        }
        return str;
    }
}
