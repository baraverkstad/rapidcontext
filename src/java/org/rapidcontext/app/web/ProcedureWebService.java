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

package org.rapidcontext.app.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.JsonSerializer;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.WebService;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.core.web.Request;

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
        this.dict.set(KEY_PREFIX, prefix());
        this.dict.set(KEY_INPUT_TYPE, inputType());
        this.dict.set(KEY_OUTPUT_TYPE, outputType());
    }

    /**
     * Returns the procedure name prefix.
     *
     * @return the procedure name prefix, or
     *         an empty string for none
     */
    public String prefix() {
        return dict.getString(KEY_PREFIX, "");
    }

    /**
     * Returns the input arguments data format.
     *
     * @return the input arguments data format
     */
    public String inputType() {
        return dict.getString(KEY_INPUT_TYPE, "json");
    }

    /**
     * Returns the output response data format.
     *
     * @return the output response data format
     */
    public String outputType() {
        return dict.getString(KEY_OUTPUT_TYPE, "json+metadata");
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
    protected String[] methodsImpl(Request request) {
        return METHODS_GET_POST;
    }

    /**
     * Processes an HTTP GET request.
     *
     * @param request        the request to process
     */
    protected void doGet(Request request) {
        processProcedure(request);
    }

    /**
     * Processes an HTTP POST request.
     *
     * @param request        the request to process
     */
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
            LOG.info(source + ": slow procedure call to " + name + ", " +
                     execTime + " millis");
        }
        res.set("execStart", new Date(startTime));
        res.setInt("execTime", (int) execTime);
        if (outputType().equalsIgnoreCase("text")) {
            if (res.containsKey("error")) {
                String error = res.getString("error", "internal error");
                request.sendError(STATUS.BAD_REQUEST, Mime.TEXT[0], error);
            } else {
                request.sendText(Mime.TEXT[0], res.getString("data", ""));
            }
        } else if (outputType().equalsIgnoreCase("json")) {
            if (res.containsKey("error")) {
                String error = res.getString("error", "internal error");
                request.sendError(STATUS.BAD_REQUEST, Mime.TEXT[0], error);
            } else {
                Object data = res.get("data");
                request.sendText(Mime.JSON[0], JsonSerializer.serialize(data, true));
            }
        } else {
            request.sendText(Mime.JSON[0], JsonSerializer.serialize(res, true));
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
    protected Dict processCall(String name, Request request, String source) {
        boolean isTraceReq = request.getParameter("system:trace", null) != null;
        String logPrefix = source + "-->" + name + "(): ";
        StringBuilder trace = null;
        Dict res = new Dict();
        try {
            LOG.fine(logPrefix + "init procedure call");
            ApplicationContext ctx = ApplicationContext.getInstance();
            Procedure proc = ctx.getLibrary().getProcedure(name);
            Object[] args = processArgs(proc, request, logPrefix);
            if (isTraceReq || ctx.getLibrary().isTracing(name)) {
                trace = new StringBuilder();
            }
            res.set("data", ctx.execute(name, args, source, trace));
            LOG.fine(logPrefix + "done procedure call");
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            res.set("error", msg);
            if (e instanceof ProcedureException) {
                LOG.info(logPrefix + msg);
            } else {
                LOG.log(Level.WARNING, logPrefix + "internal error in procedure", e);
            }
        }
        if (trace != null) {
            String logTrace = trace.toString();
            res.set("trace", logTrace);
            LOG.log(Level.INFO, logPrefix + "execution trace:\n" + logTrace);
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
     * @throws Exception if an argument wasn't valid JSON
     */
    protected Object[] processArgs(Procedure proc, Request request, String logPrefix)
        throws Exception {

        boolean isTextFormat = inputType().equalsIgnoreCase("text");
        ArrayList<Object> args = new ArrayList<>();
        Bindings bindings = proc.getBindings();
        Dict jsonArgs = null;
        if (Mime.isInputMatch(request, Mime.JSON)) {
            String input = request.getInputString();
            LOG.fine(logPrefix + "arguments JSON: " + input);
            Object obj = JsonSerializer.unserialize(input);
            if (obj instanceof Dict) {
                jsonArgs = (Dict) obj;
            }
        }
        for (String name : bindings.getNames()) {
            if (bindings.getType(name) == Bindings.ARGUMENT) {
                if (jsonArgs != null && name.equals("json")) {
                    args.add(jsonArgs);
                } else if (jsonArgs != null) {
                    args.add(jsonArgs.get(name, null));
                } else {
                    String str = request.getParameter("arg" + args.size(), null);
                    if (str == null) {
                        str = request.getParameter(name, null);
                    }
                    LOG.fine(logPrefix + "argument '" + name + "': " + str);
                    args.add(isTextFormat ? str : JsonSerializer.unserialize(str));
                }
            }
        }
        return args.toArray();
    }
}
