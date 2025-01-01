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
import java.net.ServerSocket;

import org.eclipse.jetty.ee8.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/**
 * The stand-alone server application.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ServerApplication {

    /**
     * The array of default ports to attempt using.
     */
    public static final int[] PORTS = { 80, 8080, 8180, 8081, 8082, 8888 };

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
    @SuppressWarnings("try")
    public static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return true;
        } catch (Exception e) {
            return false;
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
        port = ApplicationContext.getInstance().getConfig().get("port", Integer.class, 0);
        if (port > 0 && isPortAvailable(port)) {
            return port;
        }
        for (int p : PORTS) {
            if (isPortAvailable(p)) {
                return p;
            }
        }
        return 0;
    }

    /**
     * Creates a new stand-alone application instance.
     */
    public ServerApplication() {}

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
     * Initializes the stand-alone server application.
     */
    public void init() {
        ApplicationContext.init(appDir, localDir, false);
        port = findAvailablePort(port);
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
        server = new Server(port);
        server.setStopTimeout(10000L);
        server.setStopAtShutdown(true);
        ServletContextHandler root = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        root.setContextPath("/");
        root.setBaseResourceAsPath(appDir.toPath());
        root.addServlet(ServletApplication.class, "/*");
        server.setHandler(root);
        port = ((ServerConnector) server.getConnectors()[0]).getPort();
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
}
