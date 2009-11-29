/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2009 Per Cederberg & Dynabyte AB.
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

package org.rapidcontext.core.proc;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.rapidcontext.core.data.DataStore;
import org.rapidcontext.core.env.AdapterConnection;
import org.rapidcontext.core.env.AdapterException;
import org.rapidcontext.core.env.Environment;
import org.rapidcontext.util.DateUtil;

/**
 * A procedure call context. Each procedure call occurs within a
 * specific call context that contains the environment, library and
 * currently used adapter connections. The context also keeps track
 * of the call stack and other information relevant to the procedure
 * call.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class CallContext {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(CallContext.class.getName());

    /**
     * The attribute used for storing the execution root procedure.
     * This attribute value is automatically stored by the execute()
     * method.
     */
    public static final String ATTRIBUTE_PROCEDURE = "procedure";

    /**
     * The attribute used for storing the execution start time. This
     * attribute value is automatically stored by the execute()
     * method.
     */
    public static final String ATTRIBUTE_START_TIME = "startTime";

    /**
     * The attribute used for storing the execution end time. This
     * attribute value is automatically stored by the execute()
     * method.
     */
    public static final String ATTRIBUTE_END_TIME = "endTime";

    /**
     * The attribute used for storing the progress ratio. The
     * progress value should be a double between 0.0 and 1.0,
     * corresponding to the "percent complete" of the overall call.
     * It is generally only safe to set this value for a top-level
     * procedure, i.e. when the call stack height is one (1).
     */
    public static final String ATTRIBUTE_PROGRESS = "progress";

    /**
     * The attribute used for storing the user information.
     */
    public static final String ATTRIBUTE_USER = "user";

    /**
     * The attribute used for storing call source information.
     */
    public static final String ATTRIBUTE_SOURCE = "source";

    /**
     * The attribute used for storing the result data.
     */
    public static final String ATTRIBUTE_RESULT = "result";

    /**
     * The attribute used for storing the error message.
     */
    public static final String ATTRIBUTE_ERROR = "error";

    /**
     * The attribute used for storing the trace flag.
     */
    public static final String ATTRIBUTE_TRACE = "trace";

    /**
     * The attribute used for storing the log string buffer.
     */
    public static final String ATTRIBUTE_LOG_BUFFER = "log";

    /**
     * The maximum number of characters to store in the log buffer.
     */
    public static final int MAX_LOG_LENGTH = 500000;

    /**
     * The thread that is executing this call context.
     */
    private Thread thread;

    /**
     * The data store to use.
     */
    private DataStore dataStore;

    /**
     * The connectivity environment to use.
     */
    private Environment env;

    /**
     * The procedure library to use.
     */
    private Library library;

    /**
     * The local procedure call interceptor. This variable is only
     * set if the default library procedure call interceptor should
     * be overridden.
     */
    private Interceptor interceptor = null;

    /**
     * The map of reserved connections. Before executing a procedure
     * tree, all the required adapter connections are reserved and
     * stored here. The adapter connections stored in this map are
     * indexed by their pool name.
     */
    private HashMap connections = new HashMap();

    /**
     * The context call stack.
     */
    private CallStack stack = new CallStack();

    /**
     * The map of call attributes. Call attributes can be used for
     * storing generic objects in the call context, which is useful
     * for setting flags or storing objects across procedures.
     */
    private HashMap attributes = new HashMap();

    /**
     * The call interrupted flag. Once this flag has been set, it
     * cannot be reversed for this call context and all further
     * procedure calls will be stopped immediately with an error.
     */
    private boolean interrupted = false;

    /**
     * Creates a new procedure call context.
     *
     * @param dataStore      the data store to use
     * @param env            the environment to use
     * @param library        the procedure library to use
     */
    public CallContext(DataStore dataStore, Environment env, Library library) {
        this.thread = Thread.currentThread();
        this.dataStore = dataStore;
        this.env = env;
        this.library = library;
    }

    /**
     * Returns the data store used by this context.
     *
     * @return the data store used by this context
     */
    public DataStore getDataStore() {
        return dataStore;
    }

    /**
     * Returns the connectivity environment used by this context.
     *
     * @return the connectivity environment
     */
    public Environment getEnvironment() {
        return env;
    }

    /**
     * Returns the procedure library used by this context.
     *
     * @return the procedure library
     */
    public Library getLibrary() {
        return library;
    }

    /**
     * Returns the local procedure call interceptor. If no local
     * interceptor has been set, the library procedure call
     * interceptor will be returned instead.
     *
     * @return the call interceptor to use
     */
    public Interceptor getInterceptor() {
        if (interceptor != null) {
            return interceptor;
        } else {
            return library.getInterceptor();
        }
    }

    /**
     * Sets the local procedure call interceptor, overriding the
     * default library procedure call interceptor for calls in this
     * context.
     *
     * @param i              the interceptor to use
     */
    public void setInterceptor(Interceptor i) {
        this.interceptor = i;
    }

    /**
     * Returns a call attribute value.
     *
     * @param name           the attribute name
     *
     * @return the call attribute value, or
     *         null if not defined
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * Sets a call attribute value.
     *
     * @param name           the attribute name
     * @param value          the attribute value
     */
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    /**
     * Returns the procedure call stack.
     *
     * @return the procedure call stack
     */
    public CallStack getCallStack() {
        return stack;
    }

    /**
     * Checks if the call has been interrupted. Once a call has been
     * interrupted the procedure call chain will be stopped as soon
     * as possible. Any further procedure calls in this call context
     * will terminate immediately with an error.
     *
     * @return true if this call has been interrupted, or
     *         false otherwise
     *
     * @see #interrupt()
     */
    public boolean isInterrupted() {
        return interrupted;
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
        interrupted = true;
        thread.interrupt();
    }

    /**
     * Executes a procedure with the specified name and arguments.
     * This is a convenience method for performing a procedure
     * lookup and proper calls to reserve(), call() and
     * releaseAll(). Before execution all required resources will be
     * reserved, and once the execution terminates they will be
     * released. The arguments must be specified in the same order
     * as in the default bindings for the procedure.
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
    public Object execute(String name, Object[] args)
        throws ProcedureException {

        Procedure  proc = library.getProcedure(name);
        boolean    commit = false;
        Object     res;
        String     msg;

        setAttribute(ATTRIBUTE_PROCEDURE, proc);
        setAttribute(ATTRIBUTE_START_TIME, new Date());
        try {
            reserve(proc);
            res = call(proc, args);
            commit = true;
            return res;
        } catch (Exception e) {
            if (e instanceof ProcedureException) {
                msg = "Execution error in procedure '" + name + "'";
                LOG.log(Level.FINE, msg, e);
                throw (ProcedureException) e;
            } else {
                msg = "Caught unhandled exception in procedure '" + name + "'";
                LOG.log(Level.WARNING, msg, e);
                throw new ProcedureException(msg, e);
            }
        } finally {
            releaseAll(commit);
            setAttribute(ATTRIBUTE_END_TIME, new Date());
        }
    }

    /**
     * Reserves all adapter connections needed for executing the
     * specified procedure. The reservation will be forwarded to the
     * current call interceptor. Any connection or procedure in the
     * default procedure bindings will be reserved recursively in
     * this call context.
     *
     * @param proc           the procedure definition
     *
     * @throws ProcedureException if the connections couldn't be
     *             reserved
     */
    public void reserve(Procedure proc) throws ProcedureException {
        getInterceptor().reserve(this, proc);
    }

    /**
     * Releases all currently reserved adapter connections. The
     * release will be forwarded to the current call interceptor.
     * Each reserved connection will either be committed or rolled
     * back, depending on the commit flag.
     *
     * @param commit         the commit (or rollback) flag
     */
    public void releaseAll(boolean commit) {
        getInterceptor().releaseAll(this, commit);
    }

    /**
     * Calls a procedure with the specified arguments. The call will
     * be forwarded to the current call interceptor. The arguments
     * must be specified in the same order as in the default bindings
     * for the procedure. All the required arguments must be provided
     * and all connections must already have been reserved. Use
     * execute() when a procedure is to be called from outside a
     * prepared call context.
     *
     * @param proc           the procedure definition
     * @param args           the call arguments
     *
     * @return the call bindings
     *
     * @throws ProcedureException if the argument binding failed
     */
    public Object call(Procedure proc, Object[] args)
        throws ProcedureException {

        Bindings  bindings = proc.getBindings();
        Bindings  callBindings;
        String[]  names;
        int       pos = 0;
        Object    value;
        String    msg;

        callBindings = new Bindings(bindings);
        names = bindings.getNames();
        for (int i = 0; i < names.length; i++) {
            if (bindings.getType(names[i]) == Bindings.PROCEDURE) {
                value = bindings.getValue(names[i]);
                value = library.getProcedure((String) value);
                callBindings.set(names[i], Bindings.PROCEDURE, value, null);
            } else if (bindings.getType(names[i]) == Bindings.CONNECTION) {
                value = bindings.getValue(names[i], null);
                if (value != null) {
                    value = connections.get(value);
                    if (value == null) {
                        msg = "no connection defined for '" + names[i] + "'";
                        throw new ProcedureException(msg);
                    }
                }
                callBindings.set(names[i], Bindings.CONNECTION, value, null);
            } else if (bindings.getType(names[i]) == Bindings.ARGUMENT) {
                if (pos >= args.length) {
                    msg = "missing argument " + (pos + 1) + " '" +
                          names[i] + "' in call to '" +
                          proc.getName() + "'";
                    throw new ProcedureException(msg);
                }
                callBindings.set(names[i], Bindings.ARGUMENT, args[pos], null);
                pos++;
            }
        }
        if (pos != args.length) {
            msg = "too many arguments in call to '" + proc.getName() +
                  "'; expected " + pos + ", found " + args.length;
            throw new ProcedureException(msg);
        }
        return call(proc, callBindings);
    }

    /**
     * Calls a procedure with the specified arguments. The call will
     * be forwarded to the current call interceptor. All the
     * required arguments must be provided and all connections must
     * already have been reserved. Use execute() when a procedure is
     * to be called from outside a prepared call context.
     *
     * @param proc           the procedure definition
     * @param bindings       the bindings to use
     *
     * @return the result of the call, or
     *         null if the call produced no result
     *
     * @throws ProcedureException if the call execution caused an
     *             error
     *
     * @see #execute(String, Object[])
     */
    public Object call(Procedure proc, Bindings bindings)
        throws ProcedureException {

        if (isInterrupted()) {
            throw new ProcedureException("procedure call interrupted");
        }
        stack.push(proc, bindings);
        try {
            return getInterceptor().call(this, proc, bindings);
        } finally {
            stack.pop();
        }
    }

    /**
     * Reserves an adapter connection for the specified pool. The
     * reserved connection will be stored in this context until all
     * connections are released.
     *
     * @param poolName       the adapter pool name
     *
     * @return the adapter connection for the specified pool
     *
     * @throws ProcedureException if the connection couldn't be
     *             reserved
     *
     * @see #connectionReleaseAll(boolean)
     */
    public AdapterConnection connectionReserve(String poolName)
        throws ProcedureException {

        AdapterConnection  con;
        String             msg;

        if (poolName != null && !connections.containsKey(poolName)) {
            if (isTracing()) {
                log("Reserving adapter connection on '" + poolName + "'");
            }
            if (env == null) {
                msg = "failed to reserve adapter connection on '" +
                      poolName + "': no environment loaded";
                if (isTracing()) {
                    log("ERROR: " + msg);
                }
                LOG.warning(msg);
                throw new ProcedureException(msg);
            } else if (env.findPool(poolName) == null) {
                msg = "failed to reserve adapter connection: " +
                      "no adapter pool '" + poolName + "' found";
                if (isTracing()) {
                    log("ERROR: " + msg);
                }
                LOG.warning(msg);
                throw new ProcedureException(msg);
            }
            try {
                con = env.findPool(poolName).reserveConnection();
                connections.put(poolName, con);
            } catch (AdapterException e) {
                msg = "failed to reserve adapter connection on '" +
                      poolName + "': " + e.getMessage();
                if (isTracing()) {
                    log("ERROR: " + msg);
                }
                LOG.warning(msg);
                throw new ProcedureException(msg);
            }
        }
        return (AdapterConnection) connections.get(poolName);
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
        Iterator           iter = connections.keySet().iterator();
        String             poolName;
        AdapterConnection  con;
        String             msg;

        while (iter.hasNext()) {
            poolName = (String) iter.next();
            con = (AdapterConnection) connections.get(poolName);
            try {
                if (commit) {
                    con.commit();
                } else {
                    con.rollback();
                }
            } catch (AdapterException e) {
                msg = "failed to ";
                msg += (commit) ? "commit" : "rollback";
                msg += " connection '" + poolName + "': " + e.getMessage();
                LOG.warning(msg);
            }
            env.findPool(poolName).releaseConnection(con);
        }
        connections.clear();
    }

    /**
     * Checks if this call context has call trace logging enabled.
     *
     * @return true if call trace logging is enabled, or
     *         false otherwise
     */
    public boolean isTracing() {
        return getAttribute(ATTRIBUTE_TRACE) != null;
    }

    /**
     * Logs the specified message to the call log.
     *
     * @param message        the message text
     */
    public void log(String message) {
        log(0, message);
    }

    /**
     * Logs the specified message to the call log with indentation.
     *
     * @param indent         the indentation level
     * @param message        the message text
     */
    public void log(int indent, String message) {
        StringBuffer  buffer;
        String        prefix;
        String[]      lines;

        buffer = (StringBuffer) attributes.get(ATTRIBUTE_LOG_BUFFER);
        if (buffer == null) {
            buffer = new StringBuffer();
            attributes.put(ATTRIBUTE_LOG_BUFFER, buffer);
        }
        prefix = DateUtil.formatIsoTime(new Date()) + ": ";
        buffer.append(prefix);
        lines = message.split("\n");
        for (int i = 0; i < lines.length; i++) {
            for (int j = 0; j < indent; j++) {
                buffer.append(" ");
            }
            if (lines[i].trim().length() > 0) {
                buffer.append(lines[i]);
            }
            buffer.append("\n");
            if (i == 0) {
                indent += prefix.length();
            }
        }
        if (buffer.length() > MAX_LOG_LENGTH) {
            buffer.delete(0, buffer.length() - MAX_LOG_LENGTH);
        }
    }
}
