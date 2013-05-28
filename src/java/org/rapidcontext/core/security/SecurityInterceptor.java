/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2013 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.security;

import java.util.logging.Logger;

import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Interceptor;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Role;

/**
 * A security procedure call interceptor. This interceptor checks
 * for procedure and connection permissions before allowing any
 * resource to be reserved. It also filters the results from
 * procedures returning data known to be sensitive.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class SecurityInterceptor extends Interceptor {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(SecurityInterceptor.class.getName());

    /**
     * Creates a new security interceptor.
     *
     * @param parent         the parent interceptor
     */
    public SecurityInterceptor(Interceptor parent) {
        super(parent);
    }

    /**
     * Reserves all adapter connections needed for executing the
     * specified procedure. All connections needed by imported
     * procedures will also be reserved recursively.
     *
     * @param cx             the procedure context
     * @param proc           the procedure definition
     *
     * @throws ProcedureException if the connections couldn't be
     *             reserved
     */
    public void reserve(CallContext cx, Procedure proc)
        throws ProcedureException {

        boolean internal = cx.getCallStack().height() > 0;
        String perm = internal ? Role.PERM_INTERNAL : Role.PERM_READ;
        if (!SecurityContext.hasAccess("procedure/" + proc.getName(), perm)) {
            LOG.info("permission denied for procedure/" + proc.getName() +
                     " for user " + SecurityContext.currentUser());
            throw new ProcedureException("Permission denied");
        }
        // TODO: check access permissions for connections too
        super.reserve(cx, proc);
    }
}
