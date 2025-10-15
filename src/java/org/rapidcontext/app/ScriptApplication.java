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

package org.rapidcontext.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;

import org.rapidcontext.app.model.RequestContext;
import org.rapidcontext.core.data.JsonSerializer;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;

/**
 * The main command-line application.
 *
 * @author Per Cederberg
 */
public class ScriptApplication {

    /**
     * The base application directory. Defaults to the current
     * directory.
     */
    public File appDir = new File(".");

    /**
     * The local add-on directory. Defaults to the current
     * directory.
     */
    public File localDir = new File(".");

    /**
     * The user name used for authentication. Defaults to the
     * current user name.
     */
    public String user = System.getProperty("user.name");

    /**
     * The command source text, as logged and/or visible for
     * introspection.
     */
    public String source = "command-line [" + user + "]";

    /**
     * The post command execution delay in seconds. Default to
     * zero (0).
     */
    public int delay = 0;

    /**
     * The command trace flag. Defaults to false.
     */
    public boolean trace = false;

    /**
     * Creates a new command-line application instance.
     */
    public ScriptApplication() {}

    /**
     * Runs a single command.
     *
     * @param params         the procedure name and arguments
     *
     * @throws SecurityException if the user couldn't authenticate
     */
    public void runSingle(String[] params) throws SecurityException {
        ApplicationContext.init(appDir, localDir, true);
        try {
            RequestContext cx = RequestContext.initLocal(user);
            try {
                exec(cx, new LinkedList<>(Arrays.asList(params)));
            } finally {
                cx.close();
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        } finally {
            ApplicationContext.destroy();
        }
    }

    /**
     * Runs the commands specified by the prefix and lines read from
     * a file or standard input.
     *
     * @param prefix         the procedure name and argument prefixes
     * @param file           the file to read, or null for stdin
     *
     * @throws SecurityException if the user couldn't authenticate
     * @throws FileNotFoundException if the file couldn't be opened
     * @throws IOException if the input stream couldn't be read
     */
    public void runFile(String[] prefix, File file)
    throws SecurityException, FileNotFoundException, IOException {

        ApplicationContext.init(appDir, localDir, true);
        try {
            RequestContext cx = RequestContext.initLocal(user);
            try {
                @SuppressWarnings("resource")
                long lines = (file == null) ? 0 : Files.lines(file.toPath()).count();
                try (Reader r = (file == null) ? new InputStreamReader(System.in) : new FileReader(file)) {
                    execStream(cx, prefix, new BufferedReader(r), lines);
                }
            } finally {
                cx.close();
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        } finally {
            ApplicationContext.destroy();
        }
    }

    /**
     * Executes a single procedure call.
     *
     * @param cx             the request context
     * @param params         the procedure name and arguments
     */
    private void exec(RequestContext cx, LinkedList<String> params) {
        try {
            String name = params.removeFirst();
            Object[] args = params.toArray(new Object[params.size()]);
            Object res = CallContext.execute(name, args);
            System.out.println(JsonSerializer.serialize(res, true));
        } catch (ProcedureException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        if (trace) {
            System.out.println();
            System.out.print(cx.log());
        }
    }

    /**
     * Executes a stream of procedure calls.
     *
     * @param cx             the request context
     * @param prefix         the procedure name and argument prefixes
     * @param reader         the input stream to process
     * @param lines          the number of lines in the file
     *
     * @throws IOException if the input stream couldn't be read
     */
    private void execStream(
        RequestContext cx,
        String[] prefix,
        BufferedReader reader,
        long lines
    ) throws IOException {

        long startTime = System.currentTimeMillis();
        Date doneTime = null;
        String line;
        int pos = 0;
        while ((line = reader.readLine()) != null) {
            pos++;
            System.out.print("Processing command " + pos);
            if (lines > 0) {
                System.out.print(" of " + lines);
            }
            if (doneTime != null) {
                System.out.print(", until " + doneTime);
            }
            System.out.println(":");
            if (line.isBlank()) {
                System.out.println("    <empty, skipped>");
            } else {
                LinkedList<String> params = new LinkedList<>(Arrays.asList(prefix));
                params.addAll(Arrays.asList(line.split("\\s+")));
                System.out.println("  " + toString(params));
                System.out.print("  ==> ");
                exec(cx, params);
                System.out.println();
                if (delay > 0) {
                    try {
                        Thread.sleep(delay * 1000L);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
            if (lines > 0 && pos % 10 == 0) {
                double millis = (double) (System.currentTimeMillis() - startTime);
                double d = (millis / pos) * (lines - pos);
                doneTime = new Date(System.currentTimeMillis() + (long) d);
            }
        }
    }

    /**
     * Returns a string representation of a procedure parameter list.
     *
     * @param params         the procedure parameters
     *
     * @return the string representation
     */
    private String toString(LinkedList<String> params) {
        StringBuilder buffer = new StringBuilder();
        for (String str : params) {
            if (buffer.length() > 0) {
                buffer.append(" ");
            }
            buffer.append(str);
        }
        return buffer.toString();
    }
}
