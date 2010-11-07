/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg & Dynabyte AB.
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.logging.Logger;

import org.apache.commons.lang.SystemUtils;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.rapidcontext.util.ClassLoaderUtil;

/**
 * The stand-alone server application.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ServerApplication {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(ServerApplication.class.getName());

    /**
     * The array of default ports to attempt using.
     */
    private static final int[] PORTS = { 80, 8080, 8180, 8081, 8082, 8888 };

    /**
     * Runs the stand-alone server application.
     *
     * @param port           the default port number
     */
    public static void run(int port) {
        File     dir = findAppDir();
        Server   server;
        Context  root;

        if (dir == null) {
            LOG.severe("Failed to locate application directory.");
            System.exit(1);
        }
        dir = dir.getAbsoluteFile();
        ApplicationContext.init(dir);
        server = new Server(findAvailablePort(port));
        root = new Context(server, "/", Context.SESSIONS);
        root.setResourceBase(dir.toString());
        root.getSessionHandler().getSessionManager().setMaxInactiveInterval(240 * 60);
        root.addServlet(new ServletHolder(new ServletApplication()), "/*");
        server.setStopAtShutdown(true);
        port = server.getConnectors()[0].getPort();
        writePortFile(dir, port);
        LOG.info("Starting server on localhost:" + port);
        try {
            server.start();
        } catch (Exception e) {
            LOG.severe("Failed to start server: " + e.getMessage());
            LOG.severe("Forced shutdown.");
            System.exit(1);
        }
    }

    /**
     * Attempts to locate the application directory based on the
     * current working directory and the class path.
     *
     * @return the application directory found, or
     *         null otherwise
     */
    private static File findAppDir() {
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

    /**
     * Searches for an available server port to use.
     *
     * @param port           the initial port number to test
     *
     * @return the suggested port number, or
     *         zero (0) if none of the suggestions worked
     */
    private static int findAvailablePort(int port) {
        if (port > 0 && isPortAvailable(port)) {
            return port;
        }
        port = ApplicationContext.getInstance().getConfig().getInt("port", 0);
        if (port > 0 && isPortAvailable(port)) {
            return port;
        }
        for (int i = 0; i < PORTS.length; i++) {
            if (isPortAvailable(PORTS[i])) {
                return PORTS[i];
            }
        }
        return 0;
    }

    /**
     * Checks if the specified server port can be used.
     *
     * @param port           the suggested port number
     *
     * @return true if the port number can be used, or
     *         false otherwise
     */
    private static boolean isPortAvailable(int port) {
        ServerSocket  socket = null;

        try {
            socket = new ServerSocket(port);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore errors on close
                }
            }
        }
    }

    /**
     * Creates a file containing the current server port number.
     *
     * @param baseDir        the base directory
     * @param port           the port number
     */
    private static void writePortFile(File baseDir, int port) {
        File         dir = new File(baseDir, "var");
        File         file;
        PrintWriter  os;

        dir.mkdir();
        file = new File(dir, "server.port");
        try {
            os = new PrintWriter(new FileWriter(file, false));
            os.println(port);
            os.close();
        } catch (IOException e) {
            LOG.severe("Failed to create " + file + ": " + e.getMessage());
        }
        file.deleteOnExit();
    }
}
