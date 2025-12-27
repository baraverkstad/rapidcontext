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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.SystemUtils;
import org.rapidcontext.app.ui.ControlPanel;
import org.rapidcontext.util.ClasspathUtil;
import org.rapidcontext.util.FileUtil;

/**
 * The application start point, handling command-line parsing and
 * launching the application in the appropriate mode.
 *
 * @author Per Cederberg
 */
public final class Main {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(Main.class.getName());

    /**
     * The command-line usage information.
     */
    public static final String USAGE = """
        Usage: [1] rapidcontext [--app] [<options>]
               [2] rapidcontext --server [<options>]
               [3] rapidcontext [--script] [<options>] [<procedure> [<arg1> ...]]

        Alternative [1] is assumed when no procedure is specified.
        Alternative [3] is assumed when a procedure is specified.

        Options:
             --app                 Launch in interactive application mode.
             --server              Launch in server mode.
             --script              Launch in script execution mode.
          -h,--help                Displays this help message,
          -l,--local <dir>         Use a specified local app directory.
             --properties <file>   Load system properties file at startup.
          -p,--port <number>       Use a specified port number (non-script mode).
          -d,--delay <secs>        Add a delay after each command (script mode).
          -t,--trace               Print detailed execution trace (script mode).
          -u,--user <name>         Authenticate as another user (script mode).
             --stdin               Read commands from stdin (script mode).
          -f,--file <file>         Read commands from a file (script mode).
        """;

    // Static initializer (fix for Mac UI)
    static {
        String str = "com.apple.mrj.application.apple.menu.about.name";
        System.setProperty(str, "RapidContext");
        System.setProperty("apple.awt.brushMetalLook", "true");
    }

    /**
     * Application entry point.
     *
     * @param args           the command-line parameters
     */
    public static void main(String[] args) {
        Options opts = new Options();
        ArrayList<String> remains = new ArrayList<>();

        // Parse command-line arguments
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--app")) {
                opts.app = true;
            } else if (arg.equals("--server")) {
                opts.server = true;
            } else if (arg.equals("--script")) {
                opts.script = true;
            } else if (arg.equals("-h") || arg.equals("--help")) {
                opts.help = true;
            } else if (arg.equals("-l") || arg.equals("--local")) {
                opts.local = (i + 1 < args.length) ? args[++i] : null;
            } else if (arg.equals("--properties")) {
                opts.properties = (i + 1 < args.length) ? args[++i] : null;
            } else if (arg.equals("-p") || arg.equals("--port")) {
                opts.port = (i + 1 < args.length) ? args[++i] : null;
            } else if (arg.equals("-d") || arg.equals("--delay")) {
                opts.delay = (i + 1 < args.length) ? args[++i] : null;
            } else if (arg.equals("-t") || arg.equals("--trace")) {
                opts.trace = true;
            } else if (arg.equals("-u") || arg.equals("--user")) {
                opts.user = (i + 1 < args.length) ? args[++i] : null;
            } else if (arg.equals("--stdin")) {
                opts.stdin = true;
            } else if (arg.equals("-f") || arg.equals("--file")) {
                opts.file = (i + 1 < args.length) ? args[++i] : null;
            } else {
                remains.add(arg);
            }
        }

        // Execute command
        if (opts.help) {
            exit(true, null);
        } else if (opts.app && !remains.isEmpty()) {
            exit(true, "No arguments supported for app launch mode.");
        } else if (opts.app) {
            runApp(opts);
        } else if (opts.server && !remains.isEmpty()) {
            exit(true, "No arguments supported for server launch mode.");
        } else if (opts.server) {
            runServer(opts);
        } else if (opts.script) {
            runScript(opts, remains);
        } else if (remains.isEmpty()) {
            runApp(opts);
        } else {
            runScript(opts, remains);
        }
    }

    /**
     * Launches the interactive application mode.
     *
     * @param opts           the command-line options
     */
    private static void runApp(Options opts) {
        ServerApplication app = createServer(opts.local, opts.properties, opts.port);
        System.setProperty("apple.awt.application.name", "RapidContext");
        ControlPanel panel = new ControlPanel(app);
        panel.setVisible(true);
        panel.start();
    }

    /**
     * Launches the server mode.
     *
     * @param opts           the command-line options
     */
    private static void runServer(Options opts) {
        ServerApplication app = createServer(opts.local, opts.properties, opts.port);
        System.out.println();
        System.out.print("RapidContext Server -- http://localhost");
        if (app.port != 80) {
            System.out.print(":");
            System.out.print(app.port);
        }
        System.out.println("/");
        System.out.println("Press Ctrl-C to quit");
        System.out.println();
        try {
            app.start();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "error starting server", e);
            exit(false, e.getMessage());
        }
    }

    /**
     * Launches the script mode.
     *
     * @param opts           the command-line options
     * @param args           the additional arguments
     */
    private static void runScript(Options opts, ArrayList<String> args) {
        ScriptApplication app = new ScriptApplication();
        ServerApplication server = createServer(opts.local, opts.properties, "0");
        app.appDir = server.appDir;
        app.localDir = server.localDir;
        app.user = (opts.user != null) ? opts.user : System.getProperty("user.name");
        try {
            app.delay = Integer.parseInt((opts.delay != null) ? opts.delay : "0");
        } catch (Exception e) {
            exit(true, "Invalid delay number: " + opts.delay);
        }
        if (app.delay < 0 || app.delay > 3600) {
            exit(true, "Invalid delay number, must be between 0 and 3600.");
        }
        app.trace = opts.trace;
        try {
            if (opts.stdin) {
                app.runFile(args.toArray(new String[0]), null);
            } else if (opts.file != null) {
                app.runFile(args.toArray(new String[0]), new File(opts.file));
            } else if (args.isEmpty()) {
                exit(true, "No command specified for --script mode.");
            } else {
                app.runSingle(args.toArray(new String[0]));
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "error running script", e);
            exit(false, e.getMessage());
        }
    }

    /**
     * Creates a server application instance from the command-line
     * arguments.
     *
     * @param local          the local app directory option
     * @param properties     the system properties file option
     * @param port           the port number option
     *
     * @return the server application created (not started)
     */
    private static ServerApplication createServer(String local, String properties, String port) {
        ServerApplication app = new ServerApplication();
        try {
            app.port = Integer.parseInt((port != null) ? port : "0");
        } catch (Exception e) {
            exit(false, "Invalid port number: " + port);
        }
        if (app.port < 0 || app.port > 65535) {
            exit(false, "Invalid port number, must be between 0 and 65535.");
        }
        app.appDir = locateAppDir();
        if (app.appDir == null) {
            exit(false, "Failed to locate application directory.");
        }
        try {
            app.appDir = app.appDir.getCanonicalFile();
        } catch (IOException e) {
            exit(false, "Failed to normalize application directory: " + e);
        }
        app.localDir = app.appDir;
        if (local != null) {
            app.localDir = new File(local);
            if (!app.localDir.exists()) {
                app.localDir.mkdirs();
            }
        }
        File pluginDir = new File(app.localDir, "plugin");
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
        }
        if (!isDir(app.localDir, true)) {
            exit(false, "Cannot write to directory: " + app.localDir);
        } else if (!isDir(pluginDir, true)) {
            exit(false, "Cannot write to plug-in directory: " + pluginDir);
        }
        app.localDir = app.localDir.getAbsoluteFile();
        try {
            setupLocalAppDir(app.appDir, app.localDir);
        } catch (IOException e) {
            exit(false, "Failed to setup local directory: " + e.getMessage());
        }
        if (properties != null) {
            File f = new File(properties);
            try (FileInputStream is = new FileInputStream(f)) {
                Properties props = new Properties();
                props.load(is);
                System.getProperties().putAll(props);
            } catch (Exception e) {
                exit(false, "Failed to load properties: " + e.getMessage());
            }
        }
        app.init();
        return app;
    }

    /**
     * Exits the application with optional help and/or error messages.
     * This method WILL NOT RETURN.
     *
     * @param usage          print usage message
     * @param error          the error message, or null
     */
    private static void exit(boolean usage, String error) {
        PrintWriter out = new PrintWriter(System.err);
        if (usage) {
            out.println(USAGE);
        }
        if (error != null && !error.isBlank()) {
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
        File[] dirs = {
            new File("."),
            SystemUtils.getUserDir(),
            ClasspathUtil.locateFile(ServerApplication.class)
        };
        for (File f : dirs) {
            for (int j = 0; f != null && j < 4; j++) {
                if (isAppDir(f)) {
                    return f;
                }
                f = f.getParentFile();
            }
        }
        return null;
    }

    /**
     * Sets up the local application directory if it is different
     * from the built-in application directory.
     *
     * @param appDir         the built-in application directory
     * @param localDir       the local application directory
     *
     * @throws IOException if the local directory couldn't be created
     */
    private static void setupLocalAppDir(File appDir, File localDir)
    throws IOException {

        if (!appDir.equals(localDir)) {
            appDir = new File(appDir, "plugin/local");
            localDir = new File(localDir, "plugin/local");
            if (!localDir.exists()) {
                FileUtil.copy(appDir, localDir);
            }
        }
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
               isDir(file, false) &&
               isDir(new File(file, "plugin"), false);
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

    // No instances
    private Main() {}

    // Command-line options
    private static class Options {
        boolean app = false;
        boolean server = false;
        boolean script = false;
        boolean help = false;
        boolean trace = false;
        boolean stdin = false;
        String local = null;
        String properties = null;
        String port = null;
        String delay = null;
        String user = null;
        String file = null;
    }
}
