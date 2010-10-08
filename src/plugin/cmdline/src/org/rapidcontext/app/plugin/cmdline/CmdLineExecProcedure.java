/**
 * RapidContext command-line plug-in <http://www.rapidcontext.com/>
 * Copyright (c) 2008-2010 Per Cederberg & Dynabyte AB.
 * All rights reserved.
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
        Runtime   runtime;
        Process   process;
        String    cmd;
        File      dir;
        String[]  env = null;
        String    str;
        Dict      res;

        dir = ApplicationContext.getInstance().getBaseDir();
        str = bindings.getValue(BINDING_COMMAND).toString();
        cmd = replaceArguments(str, bindings);
        str = (String) bindings.getValue(BINDING_DIRECTORY, "");
        if (str.length() > 0) {
            dir = new File(dir, str);
        }
        str = (String) bindings.getValue(BINDING_ENVIRONMENT, "");
        if (str.length() > 0) {
            env = replaceArguments(str, bindings).split(";");
        }
        runtime = Runtime.getRuntime();
        LOG.fine("init exec: " + cmd);
        try {
            process = runtime.exec(cmd, env, dir);
            res = waitFor(process, cx);
        } catch (IOException e) {
            str = "error executing '" + cmd + "': " + e.getMessage();
            LOG.log(Level.WARNING, str, e);
            throw new ProcedureException(str);
        } finally {
            LOG.fine("done exec: " + cmd);
        }
        return res;
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

        String[]  names = bindings.getNames();
        Object    value;

        for (int i = 0; i < names.length; i++) {
            if (bindings.getType(names[i]) == Bindings.ARGUMENT) {
                value = bindings.getValue(names[i], null);
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

        Dict          res = new Dict();
        boolean       running = true;
        int           exitValue = 0;
        StringBuffer  output = new StringBuffer();
        StringBuffer  error = new StringBuffer();
        InputStream   isOut = process.getInputStream();
        InputStream   isErr = process.getErrorStream();
        byte[]        buffer = new byte[4096];

        process.getOutputStream().close();
        do {
            if (cx.isInterrupted()) {
                process.destroy();
                throw new IOException("procedure call interrupted");
            }
            try {
                readStream(isOut, buffer, output);
                readStream(isErr, buffer, error);
                log(cx, error);
                exitValue = process.exitValue();
                running = false;
            } catch (IllegalThreadStateException e) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignore) {
                    // Ignore this exception
                }
            }
        } while (running);
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

        int  avail;

        avail = is.available();
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
        int      pos;
        String   text;
        Matcher  m;
        double   progress;

        while ((pos = buffer.indexOf("\n")) >= 0) {
            text = buffer.substring(0, pos).trim();
            if (text.length() > 0 && text.charAt(0) == '#') {
                if (cx.getCallStack().height() <= 1) {
                    m = PROGRESS_PATTERN.matcher(text);
                    if (m.find()) {
                        progress = Double.parseDouble(m.group(1));
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
