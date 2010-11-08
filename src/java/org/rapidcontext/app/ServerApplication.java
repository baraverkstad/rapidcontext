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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.logging.Logger;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

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
    public static final int[] PORTS = { 80, 8080, 8180, 8081, 8082, 8888 };

    /**
     * The base application directory. Default to the current
     * directory.
     */
    public File appDir = new File(".");

    /**
     * The port number to use.
     */
    public int port = 0;

    /**
     * The Jetty web server used.
     */
    private Server server = null;

    /**
     * Checks if the specified server port can be used.
     *
     * @param port           the suggested port number
     *
     * @return true if the port number can be used, or
     *         false otherwise
     */
    public static boolean isPortAvailable(int port) {
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
     * Searches for an available server port to use.
     *
     * @param port           the initial port number to test
     *
     * @return the suggested port number, or
     *         zero (0) if none of the suggestions worked
     */
    public static int findAvailablePort(int port) {
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
     * Checks if the server is currently running.
     *
     * @return true if the server is running, or
     *         false otherwise
     */
    public boolean isRunning() {
        return server != null;
    }

    /**
     * Starts the stand-alone server application.
     *
     * @throws Exception if the server failed to start correctly
     */
    public void start() throws Exception {
        if (isRunning()) {
            stop();
        }
        appDir = appDir.getAbsoluteFile();
        ApplicationContext.init(appDir);
        server = new Server(findAvailablePort(port));
        Context root = new Context(server, "/", Context.SESSIONS);
        root.setResourceBase(appDir.toString());
        root.getSessionHandler().getSessionManager().setMaxInactiveInterval(240 * 60);
        root.addServlet(new ServletHolder(new ServletApplication()), "/*");
        server.setStopAtShutdown(true);
        port = server.getConnectors()[0].getPort();
        writePortFile(appDir, port);
        try {
            server.start();
        } catch (Exception e) {
            server = null;
            throw e;
        }
    }

    /**
     * Shuts down a running server application. The start and stop
     * methods can be called multiple times.
     *
     * @throws Exception if the server failed to stop correctly
     */
    public void stop() throws Exception {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    /**
     * Creates a file containing the current server port number.
     *
     * @param baseDir        the base directory
     * @param port           the port number
     */
    private void writePortFile(File baseDir, int port) {
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
