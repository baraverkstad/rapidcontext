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

import java.util.logging.Logger;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.User;

/**
 * The built-in thread interrupt procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 *
 * @deprecated Background thread execution will be removed in a future release.
 */
@Deprecated(forRemoval = true)
public class ThreadInterruptProcedure extends Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(ThreadInterruptProcedure.class.getName());

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public ThreadInterruptProcedure(String id, String type, Dict dict) {
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

        String str = bindings.getValue("threadId").toString();
        int threadId = 0;
        try {
            threadId = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new ProcedureException(this, "invalid thread id: " + str);
        }
        CallContext tcx = ApplicationContext.getInstance().findContext(threadId);
        if (tcx == null) {
            throw new ProcedureException(this, "cannot interrupt thread without context");
        }
        User user = (User) tcx.getAttribute(CallContext.ATTRIBUTE_USER);
        boolean isOwner = user != null && user == SecurityContext.currentUser();
        if (!isOwner) {
            CallContext.checkWriteAccess("thread/" + threadId);
        }
        LOG.info("interrupting thread " + threadId);
        tcx.interrupt();
        return null;
    }
}
