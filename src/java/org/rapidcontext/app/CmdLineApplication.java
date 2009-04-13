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

package org.rapidcontext.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

import org.rapidcontext.core.js.JsSerializer;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;

/**
 * The main command-line application.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class CmdLineApplication {

    /**
     * Runs the command-line application.
     *
     * @param args           the command-line parameters
     */
    public static void main(String[] args) {
        ApplicationContext  ctx;
        StringBuffer        trace = null;
        int                 delay = 0;

        if (args.length >= 1 && args[0].equals("-t")) {
            String[] tmp = new String[args.length - 1];
            for (int i = 1; i < args.length; i++) {
                tmp[i - 1] = args[i];
            }
            args = tmp;
            trace = new StringBuffer();
        } else if (System.getProperty("trace") != null) {
            trace = new StringBuffer();
        }
        if (args.length >= 2 && args[0].equals("-d")) {
            try {
                delay = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("ERROR: invalid delay value: " + args[1]);
                return;
            }
            String[] tmp = new String[args.length - 2];
            for (int i = 2; i < args.length; i++) {
                tmp[i - 2] = args[i];
            }
            args = tmp;
        }
        if (args.length < 1) {
            System.err.println("Syntax: rapidcontext [-t] <procedure> [<argument(s)>]");
            System.err.println("    Or: rapidcontext [-d <secs>] -f <command-file>");
            System.err.println();
            System.err.println("Options: -t     -- Prints a detailed execution trace.");
            System.err.println("         -d     -- Adds a delay between each command.");
            return;
        }
        ctx = new ApplicationContext(new File("."));
        ctx.init();
        try {
            SecurityContext.auth(System.getProperty("user.name"));
        } catch (SecurityException e) {
            System.err.println("ERROR: failed to authenticate user " +
                               System.getProperty("user.name"));
            return;
        }
        if (args.length >= 2 && args[0].equals("-f")) {
            execFile(ctx, args[1], delay * 1000);
        } else {
            exec(ctx, args, trace);
        }
        ctx.destroy();
    }

    /**
     * Executes a procedure call.
     *
     * @param ctx            the application context
     * @param params         the procedure name and arguments
     * @param trace          the trace buffer, or null for no trace
     */
    private static void exec(ApplicationContext ctx,
                             String[] params,
                             StringBuffer trace) {

        String[]  args;
        Object    res;
        String    source;

        try {
            args = new String[params.length - 1];
            for (int i = 1; i < params.length; i++) {
                args[i - 1] = params[i];
            }
            source = "command-line [" + System.getProperty("user.name") + "]";
            res = ctx.execute(params[0], args, source, trace);
            System.out.println(JsSerializer.serialize(res));
        } catch (ProcedureException e) {
            System.err.println("ERROR: " + e.getMessage());
        }
        if (trace != null) {
            System.out.println();
            System.out.print(trace.toString());
        }
    }

    /**
     * Executes procedure calls from a file.
     *
     * @param ctx            the application context
     * @param file           the file containing commands
     * @param delay          the optional delay after each command
     */
    private static void execFile(ApplicationContext ctx,
                                 String file,
                                 int delay) {

        BufferedReader  in;
        String          line;
        int             lines = 0;
        int             pos = 0;
        long            startTime;
        Date            doneTime = null;
        double          d;

        try {
            in = new BufferedReader(new FileReader(file));
            while ((line = in.readLine()) != null) {
                lines++;
            }
            in.close();
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: failed to open file: " + file);
            return;
        } catch (IOException e) {
            System.err.println("ERROR: " + e.getMessage());
            return;
        }
        try {
            startTime = System.currentTimeMillis();
            in = new BufferedReader(new FileReader(file));
            while ((line = in.readLine()) != null) {
                pos++;
                System.out.print("Processing " + pos + " of " + lines);
                if (doneTime != null) {
                    System.out.print(", until " + doneTime);
                }
                System.out.println(":");
                System.out.println("    " + line);
                if (line.trim().length() > 0) {
                    exec(ctx, line.split(" ") , null);
                    if (delay > 0) {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            // Nothing to do here
                        }
                    }
                }
                if (pos % 10 == 0) {
                    d = System.currentTimeMillis() - startTime;
                    d = (d / pos) * (lines - pos);
                    doneTime = new Date(System.currentTimeMillis() + (long) d);
                }
            }
            in.close();
        } catch (IOException e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }
}
