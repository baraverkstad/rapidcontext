/*
 * RapidContext command-line plug-in <https://www.rapidcontext.com/>
 * Copyright (c) 2008-2026 Per Cederberg. All rights reserved.
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

package org.rapidcontext.app.plugin.cmdline;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.rapidcontext.core.ctx.Context;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.TextEncoding;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Procedure;

/**
 * A command-line execution procedure. This procedure provides the
 * functionality of executing local command-line programs and
 * capturing their output.
 *
 * @author Per Cederberg
 */
public class CmdLineExecProcedure extends Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(CmdLineExecProcedure.class.getName());

    /**
     * The binding name for the command to execute.
     */
    public static final String BINDING_COMMAND = "command";

    /**
     * The binding name for the working directory to use.
     */
    public static final String BINDING_DIRECTORY = "directory";

    /**
     * The binding name for the environment settings to use.
     */
    public static final String BINDING_ENVIRONMENT = "environment";

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public CmdLineExecProcedure(String id, String type, Dict dict) {
        super(id, type, dict);
        if (!type.equals("procedure/cmdline/exec")) {
            this.dict.set(KEY_TYPE, "procedure/cmdline/exec");
            LOG.warning("deprecated: procedure " + id + " references legacy type: " + type);
        }
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

        return execCall(cx, bindings);
    }

    /**
     * Executes a call of this procedure in the specified context
     * and with the specified call bindings.
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
    static Object execCall(CallContext cx, Bindings bindings)
        throws ProcedureException {

        File dir = Context.active().baseDir();
        String str = bindings.getValue(BINDING_COMMAND).toString();
        // TODO: parse command-line properly, avoid StringTokenizer
        String cmd = bindings.processTemplate(str, TextEncoding.NONE).trim();
        str = ((String) bindings.getValue(BINDING_DIRECTORY, "")).trim();
        if (!str.isBlank()) {
            str = bindings.processTemplate(str, TextEncoding.NONE);
            if (str.startsWith("/")) {
                dir = new File(str);
            } else {
                dir = new File(dir, str);
            }
        }
        String[] env = null;
        str = ((String) bindings.getValue(BINDING_ENVIRONMENT, "")).trim();
        if (!str.isBlank()) {
            env = bindings.processTemplate(str, TextEncoding.NONE).split(";");
        }
        Runtime runtime = Runtime.getRuntime();
        LOG.fine("init exec: " + cmd);
        cx.logTrace("Command: " + cmd);
        cx.logTrace("Directory: " + dir);
        if (env != null) {
            cx.logTrace("Environment: " + Arrays.toString(env));
        }
        try {
            // TODO: investigate using ProcessBuilder instead
            Process process = runtime.exec(cmd.split(" "), env, dir);
            return waitFor(process, cx);
        } catch (IOException e) {
            str = "error executing '" + cmd + "': " + e.getMessage();
            LOG.log(Level.WARNING, str, e);
            throw new ProcedureException(str);
        } finally {
            LOG.fine("done exec: " + cmd);
        }
    }

    /**
     * Waits for the specified process to terminate, reading its
     * standard output and error streams meanwhile.
     *
     * @param process        the process to monitor
     * @param cx             the procedure call context
     *
     * @return the data object with the process output results
     *
     * @throws IOException if the stream reading failed
     */
    private static Dict waitFor(Process process, CallContext cx)
        throws IOException {

        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();
        int exitValue = 0;
        process.getOutputStream().close();
        try (
            InputStream stdout = process.getInputStream();
            InputStream stderr = process.getErrorStream()
        ) {
            while (true) {
                if (cx.isInterrupted()) {
                    process.destroy();
                    throw new IOException("procedure call interrupted");
                }
                try {
                    output.append(readAvail(stdout));
                    String err = readAvail(stderr);
                    error.append(err);
                    log(cx, err);
                    exitValue = process.exitValue();
                    break;
                } catch (IllegalThreadStateException e) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignore) {
                        // Ignore this exception
                    }
                }
            }
        }
        return new Dict()
            .set("exitValue", exitValue)
            .set("output", output.toString())
            .set("error", error.toString());
    }

    /**
     * Reads all available bytes off an input stream.
     *
     * @param is             the input stream to read
     *
     * @return the string read
     *
     * @throws IOException if the reading failed
     */
    private static String readAvail(InputStream is)
        throws IOException {

        StringBuilder res = new StringBuilder();
        byte[] buf = new byte[4096];
        int avail;
        while ((avail = is.available()) > 0) {
            avail = Math.min(avail, buf.length);
            is.read(buf, 0, avail);
            res.append(new String(buf, 0, avail, StandardCharsets.UTF_8));
        }
        return res.toString();
    }

    /**
     * Logs process error output while filtering progress status.
     *
     * @param cx             the procedure call context
     * @param msg            the process error messages
     */
    private static void log(CallContext cx, String msg) {
        Stream.of(msg.split("\n")).forEach(line -> {
            cx.logTrace(line);
        });
    }
}
