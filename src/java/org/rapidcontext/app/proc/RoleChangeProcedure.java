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

import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.Restricted;
import org.rapidcontext.core.security.Role;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.StorageException;

/**
 * The built-in role modification procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class RoleChangeProcedure implements Procedure, Restricted {

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.Role.Change";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new role modification procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public RoleChangeProcedure() throws ProcedureException {
        defaults.set("name", Bindings.ARGUMENT, "", "The unique role name");
        defaults.set("description", Bindings.ARGUMENT, "", "The description text");
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
        return "Creates or updates a user role.";
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

        Role     role;
        String   name;
        String   descr;

        name = bindings.getValue("name").toString();
        if (name.equals("")) {
            throw new ProcedureException("role name cannot be blank");
        } else if (!name.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            throw new ProcedureException("role name contains invalid character");
        }
        role = SecurityContext.getRole(name);
        descr = bindings.getValue("description").toString();
        try {
            if (role == null) {
                role = new Role(name);
                role.setDescription(descr);
                SecurityContext.saveRole(role);
                return role.getName() + " created";
            } else {
                role.setDescription(descr);
                SecurityContext.saveRole(role);
                return role.getName() + " modified";
            }
        } catch (StorageException e) {
            throw new ProcedureException(e.getMessage());
        } catch (SecurityException e) {
            throw new ProcedureException(e.getMessage());
        }
    }
}
