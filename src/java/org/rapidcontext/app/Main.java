/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg. All rights reserved.
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

import java.io.File;
import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.SystemUtils;
import org.rapidcontext.app.ui.ControlPanel;
import org.rapidcontext.util.ClassLoaderUtil;

/**
 * The application start point, handling command-line parsing and
 * launching the application in the appropriate mode.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Main {

    /**
     * The command-line usage information.
     */
    public static final String USAGE =
        "Usage: [1] rapidcontext [--app]\n" +
        "       [2] rapidcontext --server [<options>]\n" +
        "       [3] rapidcontext [--script] [<options>] [<procedure> [<arg1> ...]]\n" +
        "\n" +
        "Alternative [1] is assumed when no (other) arguments are used.\n" +
        "Alternative [3] is assumed when options or arguments are used.";

    // Static initializer (fix for Mac UI)
    static {
        String str = "com.apple.mrj.application.apple.menu.about.name";
        System.setProperty(str, "RapidContext");
    }

    /**
     * Application entry point.
     *
     * @param args           the command-line parameters
     */
    public static void main(String[] args) {
        Options      options = new Options();
        OptionGroup  grp;
        Option       opt;
        CommandLine  cli;

        // Create main command-line options
        grp = new OptionGroup();
        opt = new Option(null, "app", false, "Launch in interactive application mode.");
        grp.addOption(opt);
        opt = new Option(null, "server", false, "Launch in server mode.");
        grp.addOption(opt);
        opt = new Option(null, "script", false, "Launch in script execution mode.");
        grp.addOption(opt);
        options.addOptionGroup(grp);

        // Create other command-line options
        options.addOption("h", "help", false, "Displays this help message,");
        opt = new Option("p", "port", true, "Use a specified port number (server mode).");
        opt.setArgName("number");
        options.addOption(opt);
        opt = new Option("d", "delay", true, "Add a delay after each command (script mode).");
        opt.setArgName("secs");
        options.addOption(opt);
        options.addOption("t", "trace", false, "Print detailed execution trace (script mode).");
        opt = new Option("u", "user", true, "Authenticate as a another user (script mode).");
        opt.setArgName("name");
        options.addOption(opt);
        options.addOption(null, "stdin", false, "Read commands from stdin (script mode).");
        opt = new Option("f", "file", true, "Read commands from a file (script mode).");
        opt.setArgName("file");
        options.addOption(opt);

        // Parse command-line arguments
        try {
            cli = new DefaultParser().parse(options, args);
            if (cli.hasOption("help")) {
                exit(options, null);
            }
            args = cli.getArgs();
            if (cli.hasOption("app")) {
                runApp(cli, args, options);
            } else if (cli.hasOption("server")) {
                runServer(cli, args, options);
            } else if (cli.hasOption("script")) {
                runScript(cli, args, options);
            } else if (args.length == 0 && !SystemUtils.isJavaAwtHeadless()) {
                runApp(cli, args, options);
            } else {
                runScript(cli, args, options);
            }
        } catch (ParseException e) {
            exit(options, e.getMessage());
        }
    }

    /**
     * Launches the interactive application mode.
     *
     * @param cli            the parsed command line
     * @param args           the additional arguments array
     * @param opts           the command-line options object
     */
    private static void runApp(CommandLine cli, String[] args, Options opts) {
        ServerApplication  app = new ServerApplication();
        ControlPanel       panel;

        if (args.length > 0) {
            exit(opts, "No arguments supported for app launch mode.");
        }
        if (SystemUtils.isJavaAwtHeadless()) {
            exit(opts, "Cannot launch app without graphical display.");
        }
        app.appDir = locateAppDir();
        if (app.appDir == null) {
            exit(null, "Failed to locate application directory.");
        }
        panel = new ControlPanel(app);
        panel.setVisible(true);
        panel.start();
    }

    /**
     * Launches the server mode.
     *
     * @param cli            the parsed command line
     * @param args           the additional arguments array
     * @param opts           the command-line options object
     */
    private static void runServer(CommandLine cli, String[] args, Options opts) {
        ServerApplication  app = new ServerApplication();

        if (args.length > 0) {
            exit(opts, "No arguments supported for server launch mode.");
        }
        try {
            app.port = Integer.parseInt(cli.getOptionValue("port", "0"));
        } catch (Exception e) {
            exit(opts, "Invalid port number: " + cli.getOptionValue("port"));
        }
        if (app.port < 0 || app.port > 65535) {
            exit(opts, "Invalid port number, must be between 0 and 65535.");
        }
        app.appDir = locateAppDir();
        if (app.appDir == null) {
            exit(null, "Failed to locate application directory.");
        }
        try {
            app.start();
        } catch (Exception e) {
            exit(null, e.getMessage());
        }
        System.out.print("Server started -- http://localhost");
        if (app.port != 80) {
            System.out.print(":");
            System.out.print(app.port);
        }
        System.out.println("/");
        System.out.println("Press Ctrl-C to shutdown (or terminate process by other means).");
        System.out.println();
    }

    /**
     * Launches the script mode.
     *
     * @param cli            the parsed command line
     * @param args           the additional arguments array
     * @param opts           the command-line options object
     */
    private static void runScript(CommandLine cli, String[] args, Options opts) {
        ScriptApplication  app = new ScriptApplication();

        app.appDir = locateAppDir();
        app.user = cli.getOptionValue("user", System.getProperty("user.name"));
        try {
            app.delay = Integer.parseInt(cli.getOptionValue("delay", "0"));
        } catch (Exception e) {
            exit(opts, "Invalid delay number: " + cli.getOptionValue("delay"));
        }
        if (app.delay < 0 || app.delay > 3600) {
            exit(opts, "Invalid delay number, must be between 0 and 3600.");
        }
        app.trace = cli.hasOption("trace");
        try {
            if (cli.hasOption("stdin")) {
                app.runStdin(args);
            } else if (cli.hasOption("file")) {
                app.runFile(args, new File(cli.getOptionValue("file")));
            } else if (args.length <= 0) {
                exit(opts, "No command specified for --script mode.");
            } else {
                app.runSingle(args);
            }
        } catch (Exception e) {
            exit(null, e.getMessage());
        }
    }

    /**
     * Exits the application with optional help and/or error messages.
     * This method WILL NOT RETURN.
     *
     * @param options        the command-line options, or null
     * @param error          the error message, or null
     */
    private static void exit(Options options, String error) {
        PrintWriter    out = new PrintWriter(System.err);
        HelpFormatter  fmt = new HelpFormatter();

        if (options != null) {
            out.println(USAGE);
            out.println();
            out.println("Options:");
            fmt.setOptionComparator(null);
            fmt.printOptions(out, 74, options, 2, 3);
            out.println();
        }
        if (error != null && error.length() > 0) {
            out.println("ERROR:");
            out.print("    ");
            out.println(error);
            out.println();
        }
        out.flush();
        System.exit(error == null ? 0 : 1);
    }

    /**
     * Attempts to locate the application directory based on the
     * current working directory and the class path.
     *
     * @return the application directory found, or
     *         null otherwise
     */
    private static File locateAppDir() {
        File[] dirs = { new File("."),
                        SystemUtils.getUserDir(),
                        ClassLoaderUtil.getLocation(ServerApplication.class) };

        for (int i = 0; i < dirs.length; i++) {
            File file = dirs[i];
            for (int j = 0; file != null && j < 4; j++) {
                if (isAppDir(file)) {
                    return file;
                }
                file = file.getParentFile();
            }
        }
        return null;
    }

    /**
     * Checks if the specified file is the application directory.
     *
     * @param file           the file to check
     *
     * @return true if the file matches, or
     *         false otherwise
     */
    private static boolean isAppDir(File file) {
        return file != null &&
               isDir(file, true) &&
               isDir(new File(file, "plugins"), true) &&
               isDir(new File(file, "doc"), false);
    }

    /**
     * Checks if the specified file is a readable directory.
     *
     * @param file           the file to check
     * @param write          the write check flag
     *
     * @return true if the file is a directory, or
     *         false otherwise
     */
    private static boolean isDir(File file, boolean write) {
        return file != null &&
               file.isDirectory() &&
               file.canRead() &&
               (!write || file.canWrite());
    }
}
