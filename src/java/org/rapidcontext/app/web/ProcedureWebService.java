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

package org.rapidcontext.app.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.JsonSerializer;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.type.User;
import org.rapidcontext.core.type.WebService;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.core.web.Request;
import org.rapidcontext.util.ValueUtil;

/**
 * A procedure API web service. This service is used for executing procedures
 * through HTTP GET or POST calls. Arguments are automatically matched by name
 * or from 'arg0'... request parameters.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ProcedureWebService extends WebService {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(ProcedureWebService.class.getName());

    /**
     * The dictionary key for the procedure name prefix.
     */
    public static final String KEY_PREFIX = "prefix";

    /**
     * The dictionary key for the input arguments data format.
     */
    public static final String KEY_INPUT_TYPE = "inputType";

    /**
     * The dictionary key for the output response data format.
     */
    public static final String KEY_OUTPUT_TYPE = "outputType";

    /**
     * Creates a new file web service from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public ProcedureWebService(String id, String type, Dict dict) {
        super(id, type, dict);
        this.dict.setDefault(KEY_PREFIX, "");
        this.dict.setDefault(KEY_INPUT_TYPE, "json");
        this.dict.setDefault(KEY_OUTPUT_TYPE, "json+metadata");
    }

    /**
     * Returns the procedure name prefix.
     *
     * @return the procedure name prefix, or
     *         an empty string for none
     */
    public String prefix() {
        return dict.get(KEY_PREFIX, String.class, "");
    }

    /**
     * Returns the input arguments data format.
     *
     * @return the input arguments data format
     */
    public String inputType() {
        return dict.get(KEY_INPUT_TYPE, String.class, "json");
    }

    /**
     * Returns the output response data format.
     *
     * @return the output response data format
     */
    public String outputType() {
        return dict.get(KEY_OUTPUT_TYPE, String.class, "json+metadata");
    }

    /**
     * Returns the HTTP methods implemented for the specified
     * request. The OPTIONS or HEAD methods doesn't have to be added
     * to the result (added automatically later).
     *
     * @param request        the request to check
     *
     * @return the array of HTTP method names supported
     *
     * @see #methods(Request)
     */
    @Override
    protected String[] methodsImpl(Request request) {
        return METHODS_GET_POST;
    }

    /**
     * Processes an HTTP GET request.
     *
     * @param request        the request to process
     */
    @Override
    protected void doGet(Request request) {
        processProcedure(request);
    }

    /**
     * Processes an HTTP POST request.
     *
     * @param request        the request to process
     */
    @Override
    protected void doPost(Request request) {
        processProcedure(request);
    }

    /**
     * Processes a procedure execution request. This is used to trigger
     * server-side procedure execution with the POST:ed data.
     *
     * @param request        the request to process
     */
    protected void processProcedure(Request request) {
        String name = prefix() + request.getPath();
        String source = "web [" + request.getRemoteAddr() + "]";
        long startTime = System.currentTimeMillis();
        Dict res = processCall(name, request, source);
        long execTime = System.currentTimeMillis() - startTime;
        if (execTime > 10000L) {
            LOG.info(() -> source + ": slow procedure call to " + name + ", " + execTime + " millis");
        }
        res.set("execStart", new Date(startTime));
        res.set("execTime", (int) execTime);
        String err = res.get("error", String.class);
        User.report(SecurityContext.currentUser(), startTime, err == null, err);
        boolean isTextOutput = outputType().equalsIgnoreCase("text");
        boolean isJsonOutput = outputType().equalsIgnoreCase("json");
        if (isTextOutput || isJsonOutput) {
            if (res.containsKey("error")) {
                String error = res.get("error", String.class, "internal error");
                request.sendError(STATUS.BAD_REQUEST, Mime.TEXT[0], error);
            } else if (isTextOutput) {
                request.sendText(Mime.TEXT[0], res.get("data", String.class, ""));
            } else {
                Object data = res.get("data");
                request.sendText(Mime.JSON[0], JsonSerializer.serialize(data, false));
            }
        } else {
            request.sendText(Mime.JSON[0], JsonSerializer.serialize(res, false));
        }
    }

    /**
     * Processes a procedure call and returns the result dictionary.
     *
     * @param name           the procedure name
     * @param request        the request to process
     * @param source         the call source information
     *
     * @return the process result dictionary (with "data" or "error" keys)
     */
    @SuppressWarnings("removal")
    protected Dict processCall(String name, Request request, String source) {
        boolean isSession = ValueUtil.bool(request.getParameter("system:session"), false);
        boolean isTracing = ValueUtil.bool(request.getParameter("system:trace"), false);
        String logPrefix = source + "-->" + name + "(): ";
        StringBuilder trace = null;
        Dict res = new Dict();
        try {
            LOG.fine(() -> logPrefix + "init procedure call");
            super.session(request, isSession); // Create session if needed
            ApplicationContext ctx = ApplicationContext.getInstance();
            Procedure proc = Procedure.find(ctx.getStorage(), name);
            if (proc == null) {
                String msg = "no procedure '" + name + "' found";
                throw new ProcedureException(msg);
            }
            Object[] args = processArgs(proc, request, logPrefix);
            if (isTracing || ctx.getLibrary().isTracing(name)) {
                trace = new StringBuilder();
            }
            res.set("data", ctx.execute(name, args, source, trace));
            LOG.fine(() -> logPrefix + "done procedure call");
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            res.set("error", msg);
            if (e instanceof ProcedureException) {
                LOG.info(logPrefix + msg);
            } else {
                LOG.log(Level.WARNING, e, () -> logPrefix + "internal error in procedure");
            }
        }
        if (trace != null) {
            String logTrace = trace.toString();
            res.set("trace", logTrace);
            LOG.info(() -> logPrefix + "execution trace:\n" + logTrace);
        }
        return res;
    }

    /**
     * Extracts procedure arguments from the request parameters. The arguments
     * will be unserialized from JSON unless input format isn't "text".
     *
     * @param proc           the procedure
     * @param request        the request to process
     * @param logPrefix      the log prefix
     *
     * @return an array with procedure arguments
     *
     * @throws IOException if an argument wasn't valid JSON
     * @throws ProcedureException if an argument was missing
     */
    protected Object[] processArgs(Procedure proc, Request request, String logPrefix)
    throws IOException, ProcedureException {

        boolean isTextFormat = inputType().equalsIgnoreCase("text");
        ArrayList<Object> args = new ArrayList<>();
        Bindings bindings = proc.getBindings();
        Dict jsonArgs = null;
        if (Mime.isInputMatch(request, Mime.JSON)) {
            String input = request.getInputString();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine(logPrefix + "arguments JSON: " + input);
            }
            Object obj = JsonSerializer.unserialize(input);
            if (obj instanceof Dict d) {
                jsonArgs = d;
            }
        }
        for (String name : bindings.getNames()) {
            if (bindings.getType(name) == Bindings.ARGUMENT) {
                Object defval = bindings.getValue(name, null);
                Object val = null;
                if (jsonArgs != null) {
                    boolean isNamed = jsonArgs.containsKey(name);
                    boolean isRaw = !isNamed && args.isEmpty() && name.equals("json");
                    val = isNamed ? jsonArgs.get(name) : (isRaw ? jsonArgs : defval);
                } else {
                    String param = "arg" + args.size();
                    String str = request.getParameter(name, request.getParameter(param));
                    if (str == null) {
                        val = defval;
                    } else {
                        val = isTextFormat ? str : JsonSerializer.unserialize(str);
                    }
                }
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine(logPrefix + "argument '" + name + "': " + val);
                }
                args.add(val);
            }
        }
        return args.toArray();
    }
}
