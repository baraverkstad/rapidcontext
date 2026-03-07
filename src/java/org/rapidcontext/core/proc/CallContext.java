/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2026 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.proc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.ctx.Context;
import org.rapidcontext.core.ctx.ThreadContext;
import org.rapidcontext.core.type.Channel;
import org.rapidcontext.core.type.Connection;
import org.rapidcontext.core.type.ConnectionException;
import org.rapidcontext.core.type.Role;
import org.rapidcontext.core.type.Procedure;

/**
 * A procedure call context. Each procedure call occurs within a
 * specific call context that contains the environment, library and
 * currently used adapter connections. The context also keeps track
 * of the call stack and other information relevant to the procedure
 * call.
 *
 * @author Per Cederberg
 */
public class CallContext extends ThreadContext {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(CallContext.class.getName());

    /**
     * The procedure context attribute.
     */
    public static final String CX_PROCEDURE = "procedure";

    /**
     * The connections context attribute. This contains a map of
     * reserved connection channels, indexed by connection id.
     * Before executing a procedure tree, all the required
     * connection channels are reserved and stored here.
     */
    public static final String CX_CONNECTIONS = "connections";

    /**
     * The call interrupted attribute.
     */
    public static final String CX_INTERRUPTED = "interrupted";

    /**
     * Returns the currently active call context. If no call context
     * is available, null is returned.
     *
     * @return the currently active call context, or null
     */
    public static CallContext active() {
        return Context.active(CallContext.class);
    }

    /**
     * Creates a new procedure call context.
     *
     * @param id             the procedure identifier
     *
     * @return a new call context
     *
     * @throws ProcedureException if the procedure wasn't found
     *             or access was denied
     */
    public static CallContext init(String id) throws ProcedureException {
        Procedure proc = Procedure.find(Context.active().storage(), id);
        if (proc == null) {
            throw new ProcedureException("no procedure '" + id + "' found");
        }
        return init(proc);
    }

    /**
     * Creates a new procedure call context.
     *
     * @param proc           the procedure to call
     *
     * @return a new call context
     *
     * @throws ProcedureException if access was denied
     */
    public static CallContext init(Procedure proc) throws ProcedureException {
        try {
            ThreadContext.active().requireReadAccess(proc.path().toIdent(0));
        } catch (SecurityException e) {
            throw new ProcedureException(e.getMessage());
        }
        CallContext cx = new CallContext(proc.path().toIdent(0));
        cx.set(CX_PROCEDURE, proc);
        cx.open();
        return cx;
    }

    /**
     * Executes a procedure with the specified name and arguments.
     * This is a convenience method that creates a new call context,
     * locates the procedure, and calls reserve(), call() and
     * releaseAll() (when applicable). Before execution all required
     * resources will be reserved, and once the execution terminates
     * they will (eventually) be released. The arguments must be
     * specified in the same order as in the default bindings for the
     * procedure.
     *
     * @param name           the procedure name
     * @param args           the call arguments
     *
     * @return the result of the call, or
     *         null if the call produced no result
     *
     * @throws ProcedureException if the call execution caused an
     *             error
     */
    public static Object execute(String name, Object ...args) throws ProcedureException {
        CallContext cx = CallContext.init(name);
        Procedure proc = cx.procedure();
        boolean commit = false;
        try {
            cx.reserve();
            Object res = cx.call(args);
            commit = true;
            return res;
        } catch (ProcedureException e) {
            LOG.log(Level.FINE, "Execution error in " + proc, e);
            throw e;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unhandled exception in " + proc, e);
            throw new ProcedureException(proc, e);
        } finally {
            if (cx.isTop()) {
                ReserveInterceptor.get().releaseAll(cx, commit);
            }
            cx.close();
        }
    }

    /**
     * Creates a new procedure call context.
     *
     * @param id             the context identifier (name)
     */
    protected CallContext(String id) {
        super(id);
    }

    /**
     * Checks if this is the top call context.
     *
     * @return true if this is the top call context, or
     *         false otherwise
     */
    public boolean isTop() {
        return !(parent instanceof CallContext);
    }

    /**
     * Returns the top call context in the call chain.
     *
     * @return the top call context
     */
    public CallContext top() {
        return (parent instanceof CallContext p) ? p.top() : this;
    }

    /**
     * Returns the call context procedure.
     *
     * @return the call context procedure
     */
    public Procedure procedure() {
        return get(CX_PROCEDURE, Procedure.class);
    }

    /**
     * Checks if the specified procedure exists in the call context chain.
     *
     * @param proc           the procedure to search for
     *
     * @return true if the procedure was found in the call context chain, or
     *         false otherwise
     */
    public boolean isCalledBy(Procedure proc) {
        if (parent instanceof CallContext p) {
            return p.procedure().equals(proc) || p.isCalledBy(proc);
        } else {
            return false;
        }
    }

    /**
     * Returns the parent call context procedure (i.e. the calling procedure)
     *
     * @return the calling procedure, or
     *         null if this is the top call context
     */
    public Procedure caller() {
        return (parent instanceof CallContext p) ? p.procedure() : null;
    }

    /**
     * Returns a procedure stack trace for debugging purposes.
     *
     * @return an array with all procedures in the call context chain
     */
    public List<String> stackTrace() {
        ArrayList<String> res = new ArrayList<>();
        CallContext cx = this;
        while (cx != null) {
            res.add(cx.procedure().id());
            cx = (cx.parent instanceof CallContext p) ? p : null;
        }
        return res;
    }

    /**
     * Returns or creates the top-most call context map of reserved
     * connections.
     *
     * @return the map of reserved connections
     */
    @SuppressWarnings("unchecked")
    private HashMap<String,Channel> connections() {
        if (parent instanceof CallContext p) {
            return p.connections();
        } else {
            return getOrSet(
                CX_CONNECTIONS,
                HashMap.class,
                () -> new HashMap<String,Channel>()
            );
        }
    }

    /**
     * Checks if the call chain has been interrupted. Active threads
     * may continue until completion, but any additional calls will
     * terminate with an error.
     *
     * @return true if this call chain has been interrupted, or
     *         false otherwise
     *
     * @see #interrupt()
     */
    public boolean isInterrupted() {
        return has(CX_INTERRUPTED);
    }

    /**
     * Interrupts the current procedure call. Once a call has been
     * interrupted the procedure call chain will be stopped as soon
     * as possible. Any further procedure calls in this call context
     * will terminate immediately with an error.
     *
     * @see #isInterrupted()
     */
    public void interrupt() {
        if (parent instanceof CallContext p) {
            p.interrupt();
        } else {
            set(CX_INTERRUPTED, true);
        }
    }

    /**
     * Recursively reserves all connections needed for executing the
     * call context procedure (and dependencies). Each reservation
     * will be forwarded to the reserve interceptor.
     *
     * @throws ProcedureException if the connections couldn't be
     *             reserved
     */
    protected void reserve() throws ProcedureException {
        Procedure proc = procedure();
        if (isCalledBy(proc)) {
            // Do nothing on recursion, already reserved
        } else {
            ReserveInterceptor.get().reserve(this, proc);
        }
    }

    /**
     * Recursively reserves all connections needed for executing the
     * call context procedure (and dependencies).
     *
     * @throws ProcedureException if the connections couldn't be
     *             reserved
     */
    protected void reserveImpl() throws ProcedureException {
        Procedure proc = procedure();
        Bindings bindings = proc.getBindings();
        for (String name : bindings.getNames(Bindings.CONNECTION)) {
            String value = (String) bindings.getValue(name, null);
            connectionReserve(value);
        }
        for (String name : bindings.getNames(Bindings.PROCEDURE)) {
            String id = (String) bindings.getValue(name);
            CallContext cx = init(id);
            try {
                cx.reserve();
            } finally {
                cx.close();
            }
        }
    }

    /**
     * Calls the call context procedure with the specified arguments.
     * The arguments must be specified in the same order as in the
     * procedure bindings. All required arguments must be provided
     * and all connections must already have been reserved. The call
     * will be forwarded to the call interceptor.
     *
     * This is an internal method. Use execute() when a procedure is
     * to be called from outside a prepared call context.
     *
     * @param args           the call arguments
     *
     * @return the result of the call, or
     *         null if the call produced no result
     *
     * @throws ProcedureException if the call execution caused an
     *             error
     *
     * @see #execute(String, Object[])
     */
    public Object call(Object[] args) throws ProcedureException {
        Procedure proc = procedure();
        if (isInterrupted()) {
            throw new ProcedureException(proc, "call interrupted");
        }
        String deprecated = proc.deprecated();
        if (deprecated != null) {
            LOG.warning("deprecated: " + proc + " called; " + deprecated);
        }
        Bindings bindings = proc.getBindings();
        try {
            bindings = callBindings(proc.getBindings(), args);
        } catch (Exception e) {
            throw new ProcedureException(proc, e.getMessage());
        }
        return CallInterceptor.get().call(this, proc, bindings);
    }

    /**
     * Creates call bindings from the specified arguments. The
     * arguments must be specified in the same order as in the
     * procedure bindings. All required arguments must be provided
     * and all connections must already have been reserved.
     *
     * @param bindings       the default procedure bindings
     * @param args           the call arguments
     *
     * @return the call bindings
     *
     * @throws ProcedureException if the argument binding failed
     */
    private Bindings callBindings(Bindings bindings, Object[] args)
    throws ProcedureException {
        Bindings callBindings = new Bindings(bindings);
        HashMap<String,Channel> cxns = connections();
        int pos = 0;
        for (String name : bindings.getNames()) {
            if (bindings.getType(name) == Bindings.PROCEDURE) {
                String id = (String) bindings.getValue(name, null);
                Procedure value = Procedure.find(storage(), id);
                if (value != null) {
                    callBindings.set(name, Bindings.PROCEDURE, value, null);
                } else {
                    String msg = "referenced procedure '" + id + "' not found";
                    throw new ProcedureException(msg);
                }
            } else if (bindings.getType(name) == Bindings.CONNECTION) {
                String id = (String) bindings.getValue(name, null);
                if (id == null || id.isBlank()) {
                    callBindings.set(name, Bindings.CONNECTION, null, null);
                } else if (cxns.containsKey(id)) {
                    callBindings.set(name, Bindings.CONNECTION, cxns.get(id), null);
                } else {
                    String msg = "referenced connection '" + name + "' not reserved";
                    throw new ProcedureException(msg);
                }
            } else if (bindings.getType(name) == Bindings.ARGUMENT) {
                try {
                    boolean isOmitted = pos >= args.length;
                    Object val = isOmitted ? bindings.getValue(name) : args[pos];
                    callBindings.set(name, Bindings.ARGUMENT, val, null);
                    pos++;
                } catch (ProcedureException ignore) {
                    String msg = "missing '" + name + "' (" + (pos + 1) + ") argument";
                    throw new ProcedureException(msg);
                }
            }
        }
        if (pos < args.length) {
            String msg = "too many arguments; expected " + pos + ", found " + args.length;
            throw new ProcedureException(msg);
        }
        return callBindings;
    }

    /**
     * Calls the call context procedure with the specified bindings.
     *
     * @param bindings       the procedure call bindings
     *
     * @return the result of the call, or
     *         null if the call produced no result
     *
     * @throws ProcedureException if the call execution caused an
     *             error
     *
     * @see #execute(String, Object[])
     */
    protected Object callImpl(Bindings bindings) throws ProcedureException {
        Procedure proc = procedure();
        long start = System.currentTimeMillis();
        try {
            logRequest(proc.id(), bindings.getArgs());
            Object obj = proc.call(this, bindings);
            logResponse(obj);
            proc.report(start, true, null);
            return obj;
        } catch (Exception e) {
            logError(e);
            proc.report(start, false, e.toString());
            throw (e instanceof ProcedureException pe) ? pe : new ProcedureException(proc, e);
        }
    }

    /**
     * Reserves a connection channel. The reserved channel will be
     * stored in this context until all channels are released. Note
     * that no access controls will be made.
     *
     * @param id             the connection identifier
     *
     * @return the reserved connection channel
     *
     * @throws ProcedureException if the channel couldn't be
     *             reserved
     *
     * @see #connectionReleaseAll(boolean)
     */
    public Channel connectionReserve(String id)
        throws ProcedureException {

        if (id == null || id.isBlank()) {
            return null;
        } else if (parent instanceof CallContext p) {
            return p.connectionReserve(id);
        }
        HashMap<String,Channel> cxns = connections();
        if (!cxns.containsKey(id)) {
            requireReadAccess("connection/" + id);
            logTrace("... Reserving connection channel on '" + id + "'");
            Connection con = Optional.ofNullable(environment())
                .map(env -> env.findConnection(storage(), id))
                .orElseGet(() -> Connection.find(storage(), id));
            if (con == null) {
                String msg = "failed to reserve connection channel: " +
                             "no connection '" + id + "' found";
                logError(msg);
                LOG.warning(msg);
                throw new ProcedureException(msg);
            }
            try {
                cxns.put(id, con.reserve());
            } catch (ConnectionException e) {
                String msg = "failed to reserve connection channel on '" + id +
                             "': " + e.getMessage();
                logError(msg);
                LOG.warning(msg);
                throw new ProcedureException(msg);
            }
        }
        return cxns.get(id);
    }

    /**
     * Releases all reserved adapter connections. The connections
     * will either be committed or rolled back, depending on the
     * commit flag.
     *
     * @param commit         the commit (or rollback) flag
     *
     * @see #connectionReserve(String)
     */
    public void connectionReleaseAll(boolean commit) {
        if (parent instanceof CallContext) {
            throw new IllegalStateException("connectionReleaseAll only callable on top call context");
        }
        HashMap<String,Channel> cxns = connections();
        for (Channel channel : cxns.values()) {
            if (commit) {
                channel.commit();
            } else {
                channel.rollback();
            }
            channel.getConnection().release(channel);
        }
        cxns.clear();
    }

    /**
     * Checks if trace logging is enabled. This method also verifies that the
     * current user has read permission for the procedure, as internal
     * procedure calls should *not* be traced logged.
     *
     * @return true if trace logging is enabled, or
     *         false otherwise
     */
    @Override
    public boolean isLogging() {
        return super.isLogging() &&
            hasDirectAccess(procedure().path().toIdent(0), Role.PERM_READ);
    }
}
