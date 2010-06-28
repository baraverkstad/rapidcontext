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
 * @author   Per Cederberg, Dynabyte AB
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
     * @param args           the command-line parameters
     */
    public static void main(String[] args) {
        Server   server;
        Context  root;
        int      port;

        ApplicationContext.init(new File("."));
        server = new Server(findAvailablePort());
        root = new Context(server, "/", Context.SESSIONS);
        root.setResourceBase(".");
        root.getSessionHandler().getSessionManager().setMaxInactiveInterval(240 * 60);
        root.addServlet(new ServletHolder(new ServletApplication()), "/*");
        server.setStopAtShutdown(true);
        port = server.getConnectors()[0].getPort();
        writePortFile(port);
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
     * Searches for an available server port to use.
     *
     * @return the suggested port number, or
     *         zero (0) if none of the suggestions worked
     */
    private static int findAvailablePort() {
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
     * @param port           the port number
     */
    private static void writePortFile(int port) {
        File         dir = new File("var");
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
