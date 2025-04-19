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

package org.rapidcontext.app.ui;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.Rectangle;
import java.awt.Taskbar;
import java.awt.event.KeyEvent;
import java.net.InetAddress;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.apache.commons.lang3.SystemUtils;
import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.ServerApplication;
import org.rapidcontext.core.data.Dict;

/**
 * The server control panel UI.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public final class ControlPanel extends JFrame {

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
    protected HyperLink serverLink = new HyperLink("http://...");

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
     * The application logotype image.
     */
    protected Image logotype = null;

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
        GridBagConstraints  c;
        JLabel              label;
        Font                font;
        String              str;

        // Set system UI looks
        if (SystemUtils.IS_OS_MAC_OSX || SystemUtils.IS_OS_WINDOWS) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignore) {
                // Ah well... at least we tried.
            }
        }

        // Set desktop icon, etc
        try {
            Desktop.getDesktop().setAboutHandler((evt) -> about());
            logotype = ImageIO.read(getClass().getResource("logotype.png"));
            Image img = ImageIO.read(getClass().getResource("logotype-icon-256x256.png"));
            setIconImage(img);
            Taskbar.getTaskbar().setIconImage(img);
        } catch (Exception ignore) {
            // Again, we only do our best effort here
        }

        // Set title, menu & layout
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("RapidContext Server");
        setMenuBar(menuBar);
        initializeMenu();
        getContentPane().setLayout(new GridBagLayout());

        // Add logotype
        c = new GridBagConstraints();
        c.gridheight = 5;
        c.insets = new Insets(6, 15, 10, 10);
        c.anchor = GridBagConstraints.NORTHWEST;
        Image small = logotype.getScaledInstance(128, 128, Image.SCALE_SMOOTH);
        getContentPane().add(new JLabel(new ImageIcon(small)), c);

        // Add link label
        c = new GridBagConstraints();
        c.gridx = 1;
        c.insets = new Insets(10, 10, 2, 10);
        c.anchor = GridBagConstraints.WEST;
        getContentPane().add(new JLabel("Server URL:"), c);
        serverLink.setText("http://localhost:" + server.port + "/");
        serverLink.addActionListener(evt -> {
            try {
                AppUtils.openURL(serverLink.getText());
            } catch (Exception e) {
                error(e.getMessage());
            }
        });
        c = new GridBagConstraints();
        c.gridx = 2;
        c.weightx = 1.0;
        c.insets = new Insets(10, 0, 2, 10);
        c.anchor = GridBagConstraints.WEST;
        getContentPane().add(serverLink, c);

        // Add login info label
        label = new JLabel("Login as 'admin' on a new server.");
        font = label.getFont();
        font = font.deriveFont(Font.ITALIC, font.getSize() - 2.0f);
        label.setFont(font);
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 2;
        c.insets = new Insets(0, 0, 6, 10);
        c.anchor = GridBagConstraints.WEST;
        getContentPane().add(label, c);

        // Add status label
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.insets = new Insets(0, 10, 6, 10);
        c.anchor = GridBagConstraints.WEST;
        getContentPane().add(new JLabel("Status:"), c);
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 2;
        c.insets = new Insets(0, 0, 6, 10);
        c.anchor = GridBagConstraints.WEST;
        getContentPane().add(statusLabel, c);

        // Add version label
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.insets = new Insets(0, 10, 6, 10);
        c.anchor = GridBagConstraints.WEST;
        getContentPane().add(new JLabel("Version:"), c);
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 3;
        c.insets = new Insets(0, 0, 6, 10);
        c.anchor = GridBagConstraints.WEST;
        Dict info = ApplicationContext.getInstance().version();
        str = info.get("version") + " (built " + info.get("date") + ")";
        getContentPane().add(new JLabel(str), c);

        // Add buttons
        startButton.addActionListener(evt -> start());
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 4;
        c.weighty = 1.0;
        c.insets = new Insets(0, 10, 10, 10);
        c.anchor = GridBagConstraints.SOUTH;
        getContentPane().add(startButton, c);
        stopButton.addActionListener(evt -> stop());
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 4;
        c.weighty = 1.0;
        c.insets = new Insets(0, 0, 10, 0);
        c.anchor = GridBagConstraints.SOUTHWEST;
        getContentPane().add(stopButton, c);

        // Set size & position
        pack();
        bounds = this.getBounds();
        bounds.width = 500;
        bounds.x = 100;
        bounds.y = 100;
        setBounds(bounds);
    }

    /**
     * Initializes the frame menu.
     */
    private void initializeMenu() {
        if (!SystemUtils.IS_OS_MAC_OSX) {
            Menu menu = new Menu("File");
            MenuItem item = new MenuItem("Exit", new MenuShortcut(KeyEvent.VK_Q));
            item.addActionListener(evt -> quit());
            menu.add(item);
            menuBar.add(menu);
            menu = new Menu("Help");
            item = new MenuItem("About");
            item.addActionListener(evt -> about());
            menu.add(item);
            menuBar.add(menu);
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
        String  str;

        try {
            str = "http://" + InetAddress.getLocalHost().getHostAddress() +
                  ":" + server.port + "/";
        } catch (Exception e) {
            str = "http://localhost:" + server.port + "/";
        }
        serverLink.setText(str);
        if (server.isRunning()) {
            statusLabel.setText("Running");
            statusLabel.setForeground(Color.GREEN.darker().darker());
        } else {
            statusLabel.setText("Not Running");
            statusLabel.setForeground(Color.RED.darker());
        }
        startButton.setEnabled(!server.isRunning());
        stopButton.setEnabled(server.isRunning());
    }

    /**
     * Displays the about dialog.
     */
    public void about() {
        new AboutDialog(this).setVisible(true);
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
}
