/**
 * RapidContext command-line plug-in <http://www.rapidcontext.com/>
 * Copyright (c) 2008-2013 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE.txt file for more details.
 */

package org.rapidcontext.app.plugin.cmdline;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.AddOnProcedure;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;

/**
 * A command-line execution procedure. This procedure provides the
 * functionality of executing local command-line programs and
 * capturing their output.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class CmdLineExecProcedure extends AddOnProcedure {

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
     * The progress information regular expression string.
     */
    private static final String PROGRESS_RE =
        "progress: (\\d+(\\.\\d+)?)%";

    /**
     * The progress information regular expression pattern.
     */
    private static final Pattern PROGRESS_PATTERN =
        Pattern.compile(PROGRESS_RE, Pattern.CASE_INSENSITIVE);

    /**
     * Creates a new command-line execution procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public CmdLineExecProcedure() throws ProcedureException {
        defaults.set(BINDING_COMMAND, Bindings.DATA, "",
                     "The command-line to execute.");
        defaults.set(BINDING_DIRECTORY, Bindings.DATA, "",
                     "The working directory or blank for current.");
        defaults.set(BINDING_ENVIRONMENT, Bindings.DATA, "",
                     "The environment variable bindings or blank for current.");
        defaults.seal();
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

        File dir = ApplicationContext.getInstance().getBaseDir();
        String str = bindings.getValue(BINDING_COMMAND).toString();
        // TODO: parse command-line properly, avoid StringTokenizer
        String cmd = replaceArguments(str, bindings).trim();
        str = ((String) bindings.getValue(BINDING_DIRECTORY, "")).trim();
        if (str.length() > 0) {
            str = replaceArguments(str, bindings);
            if (str.startsWith("/")) {
                dir = new File(str);
            } else {
                dir = new File(dir, str);
            }
        }
        String[] env = null;
        str = ((String) bindings.getValue(BINDING_ENVIRONMENT, "")).trim();
        if (str.length() > 0) {
            env = replaceArguments(str, bindings).split(";");
        }
        Runtime runtime = Runtime.getRuntime();
        LOG.fine("init exec: " + cmd);
        cx.log("Command: " + cmd);
        cx.log("Directory: " + dir);
        if (env != null) {
            cx.log("Environment: " + Arrays.toString(env));
        }
        try {
            // TODO: investigate using ProcessBuilder instead
            Process process = runtime.exec(cmd, env, dir);
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
     * Replaces any parameters with the corresponding argument value
     * from the bindings.
     *
     * @param data           the data string to process
     * @param bindings       the bindings to use
     *
     * @return the processed data string
     *
     * @throws ProcedureException if some parameter couldn't be found
     */
    private static String replaceArguments(String data, Bindings bindings)
        throws ProcedureException {

        String[] names = bindings.getNames();
        for (int i = 0; i < names.length; i++) {
            if (bindings.getType(names[i]) == Bindings.ARGUMENT) {
                Object value = bindings.getValue(names[i], null);
                if (value == null) {
                    value = "";
                }
                data = data.replaceAll("\\:" + names[i], value.toString());
            }
        }
        return data;
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

        StringBuffer output = new StringBuffer();
        StringBuffer error = new StringBuffer();
        InputStream isOut = process.getInputStream();
        InputStream isErr = process.getErrorStream();
        byte[] buffer = new byte[4096];
        int exitValue = 0;
        process.getOutputStream().close();
        while (true) {
            if (cx.isInterrupted()) {
                // TODO: isOut.close();
                // TODO: isErr.close();
                process.destroy();
                throw new IOException("procedure call interrupted");
            }
            try {
                readStream(isOut, buffer, output);
                readStream(isErr, buffer, error);
                log(cx, error);
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
        Dict res = new Dict();
        res.setInt("exitValue", exitValue);
        res.set("output", output);
        return res;
    }

    /**
     * Reads all available bytes off an input stream.
     *
     * @param is             the input stream to read
     * @param buffer         the temporary read buffer
     * @param result         the result buffer to append to
     *
     * @throws IOException if the reading failed
     */
    private static void readStream(InputStream is, byte[] buffer, StringBuffer result)
        throws IOException {

        int avail = is.available();
        while (avail > 0) {
            if (avail > buffer.length) {
                avail = buffer.length;
            }
            is.read(buffer, 0, avail);
            result.append(new String(buffer, 0, avail));
            avail = is.available();
        }
    }

    /**
     * Analyzes the error output buffer from a process and adds the
     * relevant log messages
     *
     * @param cx             the procedure call context
     * @param buffer         the process error output buffer
     */
    private static void log(CallContext cx, StringBuffer buffer) {
        int pos;
        while ((pos = buffer.indexOf("\n")) >= 0) {
            String text = buffer.substring(0, pos).trim();
            if (text.length() > 0 && text.charAt(0) == '#') {
                if (cx.getCallStack().height() <= 1) {
                    Matcher m = PROGRESS_PATTERN.matcher(text);
                    if (m.find()) {
                        double progress = Double.parseDouble(m.group(1));
                        cx.setAttribute(CallContext.ATTRIBUTE_PROGRESS,
                                        Double.valueOf(progress));
                    }
                }
            } else {
                cx.log(buffer.substring(0, pos));
            }
            buffer.delete(0, pos + 1);
        }
    }
}
