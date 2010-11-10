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

package org.rapidcontext.app.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.apache.commons.lang.SystemUtils;
import org.rapidcontext.app.ServerApplication;

/**
 * The server control panel UI.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ControlPanel extends JFrame {

    /**
     * The server being controlled.
     */
    private ServerApplication server = new ServerApplication();

    /**
     * The menu bar.
     */
    protected MenuBar menuBar = new MenuBar();

    /**
     * The server URL link.
     */
    protected JButton linkButton = new JButton("http://...");

    /**
     * The status label.
     */
    protected JLabel statusLabel = new JLabel("Not running");

    /**
     * The start button.
     */
    protected JButton startButton = new JButton("Start");

    /**
     * The start button.
     */
    protected JButton stopButton = new JButton("Stop");

    /**
     * Creates a new control panel frame.
     *
     * @param server         the server to control
     */
    public ControlPanel(ServerApplication server) {
        this.server = server;
        initialize();
    }

    /**
     * Initializes the panel UI.
     */
    private void initialize() {
        Rectangle           bounds = new Rectangle();
        Dimension           size;
        GridBagConstraints  c;

        // Set system UI looks
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignore) {
            // Ah well... at least we tried.
        }

        // Set title, menu & layout
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("RapidContext Server");
        setMenuBar(menuBar);
        initializeMenu();
        getContentPane().setLayout(new GridBagLayout());

        // Add link label
        c = new GridBagConstraints();
        c.insets = new Insets(6, 10, 2, 10);
        c.anchor = GridBagConstraints.WEST;
        getContentPane().add(new JLabel("Server URL:"), c);

        // Add link
        linkButton.setText("http://localhost:" + server.port + "/");
        linkButton.setHorizontalAlignment(SwingConstants.LEFT);
        linkButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 1, 0));
        linkButton.setOpaque(false);
        linkButton.setForeground(Color.BLUE);
        linkButton.setBackground(getBackground());
        linkButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        linkButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                try {
                    AppUtils.openURL(linkButton.getText());
                } catch (Exception e) {
                    error(e.getMessage());
                }
            }
        });
        c = new GridBagConstraints();
        c.gridx = 1;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 0, 2, 10);
        getContentPane().add(linkButton, c);

        // Add status label
        c = new GridBagConstraints();
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 10, 6, 10);
        getContentPane().add(new JLabel("Status:"), c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 6, 10);
        getContentPane().add(statusLabel, c);

        // Add buttons
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                start();
            }
        });
        c = new GridBagConstraints();
        c.gridy = 2;
        getContentPane().add(startButton, c);
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                stop();
            }
        });
        c = new GridBagConstraints();
        c.gridy = 2;
        getContentPane().add(stopButton, c);

        // Set size & position
        pack();
        size = Toolkit.getDefaultToolkit().getScreenSize();
        bounds = this.getBounds();
        bounds.width = 300;
        bounds.x = 100;
        bounds.y = 100;
        setBounds(bounds);
    }

    /**
     * Initializes the frame menu.
     */
    private void initializeMenu() {
        Menu       menu;
        MenuItem   item;

        // Create file menu
        if (!SystemUtils.IS_OS_MAC_OSX) {
            menu = new Menu("File");
            item = new MenuItem("Exit", new MenuShortcut(KeyEvent.VK_Q));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    quit();
                }
            });
            menu.add(item);
            menuBar.add(menu);
        }

        // Fix Mac OS specific menus
        if (SystemUtils.IS_OS_MAC_OSX) {
            new MacApplication();
        }
    }

    /**
     * Starts the server if it wasn't running.
     */
    public void start() {
        try {
            server.start();
        } catch (Exception e) {
            error(e.getMessage());
        }
        update();
    }

    /**
     * Stops the server if it was running.
     */
    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            error(e.getMessage());
        }
        update();
    }

    /**
     * Updates the UI with the current server status.
     */
    public void update() {
        linkButton.setText("http://localhost:" + server.port + "/");
        if (server.isRunning()) {
            statusLabel.setText("Running");
            statusLabel.setForeground(Color.GREEN.darker().darker());
        } else {
            statusLabel.setText("Not Running");
            statusLabel.setForeground(Color.RED.darker().darker());
        }
        startButton.setEnabled(!server.isRunning());
        stopButton.setEnabled(server.isRunning());
    }

    /**
     * Displays the specified error message.
     *
     * @param message        the error message to show
     */
    public void error(String message) {
        JOptionPane.showMessageDialog(this,
                                      message,
                                      "RapidContext Error",
                                      JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Quits the application.
     */
    public void quit() {
        try {
            server.stop();
        } catch (Exception ignore) {
            // Not much to be done
        }
        System.exit(0);
    }


    /**
     * A Mac app helper class.
     */
    private class MacApplication implements InvocationHandler {

        /**
         * Creates a new Mac app helper.
         */
        public MacApplication() {
            try {
                Class cls = Class.forName("com.apple.eawt.Application");
                Object app = call(cls, "getApplication");
                Object handler = proxy("com.apple.eawt.AboutHandler");
                call(app, "setAboutHandler", handler);
                call(app, "setPreferencesHandler", null);
            } catch (Exception e) {
                System.err.println("Failed to initialize Mac OS Application:");
                e.printStackTrace();
            }
        }

        /**
         * Finds a method with a specified name.
         *
         * @param cls            the class to search
         * @param name           the method name (must be unique)
         *
         * @return the method found, or
         *         null if not found
         */
        Method find(Class cls, String name) {
            Method[]  methods = cls.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals(name)) {
                    return methods[i];
                }
            }
            return null;
        }

        /**
         * Calls a static class method without arguments.
         *
         * @param cls            the class object
         * @param name           the method name (must be unique)
         *
         * @return the call result, or
         *         null if none was provided
         *
         * @throws Exception if a reflection error occurred
         */
        Object call(Class cls, String name) throws Exception {
            return find(cls, name).invoke(null, new Object[] {});
        }

        /**
         * Calls a method with a single argument.
         *
         * @param obj            the object instance
         * @param name           the method name (must be unique)
         * @param arg            the argument value
         *
         * @return the call result, or
         *         null if none was provided
         *
         * @throws Exception if a reflection error occurred
         */
        Object call(Object obj, String name, Object arg) throws Exception {
            return find(obj.getClass(), name).invoke(obj, new Object[] { arg });
        }

        /**
         * Creates a proxy object for the specified class. The calls
         * will be delegated to the local invoke() method.
         *
         * @param className      the interface class name
         *
         * @return the proxy object to use in reflection calls
         *
         * @throws ClassNotFoundException if the class couldn't be found
         */
        private Object proxy(String className) throws ClassNotFoundException {
            ClassLoader loader = getClass().getClassLoader();
            Class cls = Class.forName(className);
            return Proxy.newProxyInstance(loader, new Class[] { cls }, this);
        }

        /**
         * Handles calls on registered listener interfaces.
         *
         * @param p              the proxy object
         * @param m              the method being called
         * @param args           the call arguments
         *
         * @return the call response
         *
         * @throws Exception if an error occurred
         */
        public Object invoke(Object p, Method m, Object[] args) throws Exception {
            if (m.getName().equals("handleAbout")) {
                //frame.showAbout();
            }
            return null;
        }
    }
}
