/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2019 Per Cederberg. All rights reserved.
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

import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.rapidcontext.core.js.JsSerializer;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.Channel;
import org.rapidcontext.core.type.Connection;
import org.rapidcontext.core.type.ConnectionException;
import org.rapidcontext.core.type.Environment;
import org.rapidcontext.core.type.Role;
import org.rapidcontext.core.type.User;
import org.rapidcontext.util.DateUtil;

/**
 * A procedure call context. Each procedure call occurs within a
 * specific call context that contains the environment, library and
 * currently used adapter connections. The context also keeps track
 * of the call stack and other information relevant to the procedure
 * call.
 *
 * @author   Per Cederberg
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
     * The data storage to use.
     */
    private Storage storage;

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
     * The map of reserved connection channels. Before executing a
     * procedure tree, all the required connection channels are
     * reserved and stored here. The channels stored in this map are
     * indexed by their connection id.
     */
    private HashMap<String,Channel> connections = new HashMap<>();

    /**
     * The context call stack.
     */
    private CallStack stack = new CallStack();

    /**
     * The map of call attributes. Call attributes can be used for
     * storing generic objects in the call context, which is useful
     * for setting flags or storing objects across procedures.
     */
    private HashMap<String,Object> attributes = new HashMap<>();

    /**
     * The call interrupted flag. Once this flag has been set, it
     * cannot be reversed for this call context and all further
     * procedure calls will be stopped immediately with an error.
     */
    private boolean interrupted = false;

    /**
     * Creates a new procedure call context.
     *
     * @param storage        the data storage to use
     * @param env            the environment to use
     * @param library        the procedure library to use
     */
    public CallContext(Storage storage, Environment env, Library library) {
        this.thread = Thread.currentThread();
        this.storage = storage;
        this.env = env;
        this.library = library;
    }

    /**
     * Returns the data storage used by this context.
     *
     * @return the data storage used by this context
     */
    public Storage getStorage() {
        return storage;
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
     * Returns the permission required to read a path at the current
     * call stack height. If the current call stack height is less or
     * equal to the specified depth, a normal read permission is
     * returned. Otherwise an internal permission is returned.
     *
     * @param depth          the max stack depth for read permission
     *
     * @return the permission required for reading (read or internal)
     *
     * @see Role#PERM_INTERNAL
     * @see Role#PERM_READ
     */
    public String readPermission(int depth) {
        return (stack.height() <= depth) ? Role.PERM_READ : Role.PERM_INTERNAL;
    }

    /**
     * Checks if the currently authenticated user has the specified
     * access permission to a storage path.
     *
     * @param path           the object storage path
     * @param permission     the requested permission
     *
     * @throws ProcedureException if the current user didn't have
     *             the requested access permission
     */
    public static void checkAccess(String path, String permission)
    throws ProcedureException {

        if (!SecurityContext.hasAccess(path, permission)) {
            User user = SecurityContext.currentUser();
            String id = (user == null) ? "anonymous user" : user.toString();
            LOG.info(permission + " permission denied for " + path + ", " + id);
            throw new ProcedureException("permission denied");
        }
    }

    /**
     * Checks if the currently authenticated user has internal access
     * to a storage path.
     *
     * @param path           the object storage path
     *
     * @throws ProcedureException if the current user didn't have
     *             internal access
     */
    public static void checkInternalAccess(String path) throws ProcedureException {
        checkAccess(path, Role.PERM_INTERNAL);
    }

    /**
     * Checks if the currently authenticated user has read access to
     * a storage path.
     *
     * @param path           the object storage path
     *
     * @throws ProcedureException if the current user didn't have
     *             read access
     */
    public static void checkReadAccess(String path) throws ProcedureException {
        checkAccess(path, Role.PERM_READ);
    }

    /**
     * Checks if the currently authenticated user has search access to
     * a storage path.
     *
     * @param path           the object storage path
     *
     * @throws ProcedureException if the current user didn't have
     *             search access
     */
    public static void checkSearchAccess(String path) throws ProcedureException {
        checkAccess(path, Role.PERM_SEARCH);
    }

    /**
     * Checks if the currently authenticated user has write access to
     * a storage path.
     *
     * @param path           the object storage path
     *
     * @throws ProcedureException if the current user didn't have
     *             write access
     */
    public static void checkWriteAccess(String path) throws ProcedureException {
        checkAccess(path, Role.PERM_WRITE);
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
     * released (if at the bottom of the call stack). The arguments
     * must be specified in the same order as in the default bindings
     * for the procedure.
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

        Procedure proc = library.getProcedure(name);
        boolean commit = false;
        if (stack.height() == 0) {
            setAttribute(ATTRIBUTE_PROCEDURE, proc);
            setAttribute(ATTRIBUTE_START_TIME, new Date());
        }
        try {
            reserve(proc);
            Object res = call(proc, args);
            commit = true;
            return res;
        } catch (Exception e) {
            if (e instanceof ProcedureException) {
                String msg = "Execution error in procedure '" + name + "'";
                LOG.log(Level.FINE, msg, e);
                throw (ProcedureException) e;
            } else {
                String msg = "Caught unhandled exception in procedure '" +
                             name + "'";
                LOG.log(Level.WARNING, msg, e);
                throw new ProcedureException(msg, e);
            }
        } finally {
            if (stack.height() == 0) {
                releaseAll(commit);
                setAttribute(ATTRIBUTE_END_TIME, new Date());
            }
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
        checkAccess("procedure/" + proc.getName(), readPermission(0));
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

        Bindings bindings = proc.getBindings();
        Bindings callBindings = new Bindings(bindings);
        String[] names = bindings.getNames();
        int pos = 0;
        for (int i = 0; i < names.length; i++) {
            if (bindings.getType(names[i]) == Bindings.PROCEDURE) {
                Object value = bindings.getValue(names[i]);
                value = library.getProcedure((String) value);
                callBindings.set(names[i], Bindings.PROCEDURE, value, null);
            } else if (bindings.getType(names[i]) == Bindings.CONNECTION) {
                Object value = bindings.getValue(names[i], null);
                if (value != null) {
                    value = connections.get(value);
                    if (value == null) {
                        String msg = "no connection defined for '" + names[i] +
                                     "'";
                        throw new ProcedureException(msg);
                    }
                }
                callBindings.set(names[i], Bindings.CONNECTION, value, null);
            } else if (bindings.getType(names[i]) == Bindings.ARGUMENT) {
                if (pos >= args.length) {
                    String msg = "missing argument " + (pos + 1) + " '" +
                                 names[i] + "' in call to '" + proc.getName() +
                                 "'";
                    throw new ProcedureException(msg);
                }
                callBindings.set(names[i], Bindings.ARGUMENT, args[pos], null);
                pos++;
            }
        }
        if (pos != args.length) {
            String msg = "too many arguments in call to '" + proc.getName() +
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
     * Reserves a connection channel. The reserved channel will be
     * stored in this context until all channels are released.
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

        if (id != null && !connections.containsKey(id)) {
            checkInternalAccess("connection/" + id);
            if (isTracing()) {
                logInternal(0, "... Reserving connection channel on '" + id + "'");
            }
            Connection con = null;
            if (env == null) {
                con = Connection.find(storage, id);
            } else {
                con = env.findConnection(storage, id);
            }
            if (con == null) {
                String msg = "failed to reserve connection channel: " +
                             "no connection '" + id + "' found";
                if (isTracing()) {
                    log("ERROR: " + msg);
                }
                LOG.warning(msg);
                throw new ProcedureException(msg);
            }
            try {
                connections.put(id, con.reserve());
            } catch (ConnectionException e) {
                String msg = "failed to reserve connection channel on '" + id +
                             "': " + e.getMessage();
                if (isTracing()) {
                    log("ERROR: " + msg);
                }
                LOG.warning(msg);
                throw new ProcedureException(msg);
            }
        }
        return connections.get(id);
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
        for (Channel channel : connections.values()) {
            if (commit) {
                channel.commit();
            } else {
                channel.rollback();
            }
            channel.getConnection().release(channel);
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
     * Logs the specified message to the log if tracing is enabled.
     *
     * @param message        the message text
     */
    public void log(String message) {
        if (isTracing()) {
            int indent = 2 * getCallStack().height() - 2;
            logInternal(indent, logIndent(4, "... " + message));
        }
    }

    /**
     * Logs the specified call to the log if tracing is enabled.
     *
     * @param name           the procedure or object method
     * @param args           the arguments, or null for none
     */
    public void logCall(String name, Object[] args) {
        if (isTracing()) {
            StringBuilder buffer = new StringBuilder();
            buffer.append(name);
            if (args != null) {
                buffer.append("(");
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) {
                        buffer.append(", ");
                    }
                    String str = JsSerializer.serialize(args[i], false);
                    if (str.length() > 250) {
                        str = str.substring(0, 250) + "...";
                    }
                    buffer.append(str);
                }
                buffer.append(")");
            }
            int indent = 2 * getCallStack().height() - 2;
            logInternal(indent, logIndent(4, "--> " + buffer.toString()));
        }
    }

    /**
     * Logs the specified call response to the log if tracing is
     * enabled.
     *
     * @param obj            the call response
     */
    public void logResponse(Object obj) {
        if (isTracing()) {
            String str = JsSerializer.serialize(obj, true);
            if (str.length() > 1000) {
                str = str.substring(0, 1000) + "...";
            }
            int indent = 2 * getCallStack().height() - 2;
            logInternal(indent, logIndent(4, "<-- " + str));
        }
    }

    /**
     * Logs the specified call error to the log if tracing is
     * enabled.
     *
     * @param e              the exception to log
     */
    public void logError(Exception e) {
        if (isTracing()) {
            int indent = 2 * getCallStack().height() - 2;
            logInternal(indent, logIndent(4, "<-- ERROR: " + e.getMessage()));
        }
    }

    /**
     * Logs the specified message to the call log with indentation.
     *
     * @param indent         the indentation level
     * @param message        the message text
     */
    private void logInternal(int indent, String message) {
        StringBuilder buffer = (StringBuilder) attributes.get(ATTRIBUTE_LOG_BUFFER);
        if (buffer == null) {
            buffer = new StringBuilder();
            attributes.put(ATTRIBUTE_LOG_BUFFER, buffer);
        }
        String prefix = DateUtil.formatIsoTime(new Date()) + ": ";
        buffer.append(prefix);
        buffer.append(StringUtils.repeat(" ", indent));
        buffer.append(logIndent(prefix.length() + indent, message));
        if (!message.endsWith("\n")) {
            buffer.append("\n");
        }
        if (buffer.length() > MAX_LOG_LENGTH) {
            buffer.delete(0, buffer.length() - MAX_LOG_LENGTH);
        }
    }

    /**
     * Indents all lines after the first one in a text string.
     *
     * @param indent         the indentation depth (chars)
     * @param text           the text to indent
     *
     * @return the indented text
     */
    private String logIndent(int indent, String text) {
        if (indent <= 0 || text.indexOf('\n') < 0) {
            return text;
        } else {
            StringBuilder buffer = new StringBuilder();
            String indentStr = StringUtils.repeat(" ", indent);
            String[] lines = text.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].trim().length() <= 0) {
                    if (i > 0) {
                        buffer.append("\n");
                    }
                } else {
                    if (i > 0) {
                        buffer.append("\n");
                        buffer.append(indentStr);
                    }
                    buffer.append(lines[i]);
                }
            }
            return buffer.toString();
        }
    }
}
