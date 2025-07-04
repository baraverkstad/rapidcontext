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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Dict;

/**
 * The about dialog.
 *
 * @author Per Cederberg
 */
public final class AboutDialog extends JDialog {

    /**
     * Creates new about dialog.
     *
     * @param parent         the parent frame
     */
    public AboutDialog(ControlPanel parent) {
        super(parent, true);
        initialize(parent);
        setLocationByPlatform(true);
    }

    /**
     * Initializes the dialog components.
     *
     * @param parent         the parent frame
     */
    private void initialize(final ControlPanel parent) {
        JLabel              label;
        JButton             button;
        HyperLink           link;
        GridBagConstraints  c;
        String              str;

        // Set dialog title
        setTitle("About RapidContext Server");
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new GridBagLayout());
        setBackground(parent.getBackground());

        // Add logotype
        c = new GridBagConstraints();
        c.gridheight = 8;
        c.insets = new Insets(10, 15, 15, 10);
        c.anchor = GridBagConstraints.NORTHWEST;
        getContentPane().add(new JLabel(new ImageIcon(parent.logotype)), c);

        // Add application name
        label = new JLabel("RapidContext Server");
        label.setFont(Font.decode("sans bold 20"));
        label.setForeground(new Color(14, 102, 167));
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(15, 15, 10, 15);
        getContentPane().add(label, c);

        // Add version label
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 15, 0, 10);
        label = new JLabel("Version:");
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        getContentPane().add(label, c);
        Dict info = ApplicationContext.getInstance().version();
        str = info.get("version") + " (built " + info.get("date") + ")";
        label = new JLabel(str);
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 1;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 0, 15);
        getContentPane().add(label, c);

        // Add license label
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 15, 0, 10);
        label = new JLabel("License:");
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        getContentPane().add(label, c);
        link = new HyperLink("BSD License");
        link.addActionListener(evt -> {
            try {
                AppUtils.openURL("https://www.rapidcontext.com/doc/LICENSE.md");
            } catch (Exception e) {
                parent.error(e.getMessage());
            }
        });
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 2;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 0, 0, 15);
        getContentPane().add(link, c);

        // Add copyright
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(10, 15, 0, 15);
        getContentPane().add(new JLabel("Copyright \u00A9 2007-2025 by Per Cederberg."), c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 4;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 15, 0, 15);
        getContentPane().add(new JLabel("All rights reserved."), c);

        // Add web site link
        label = new JLabel("Please visit the project web site:");
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 5;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(10, 15, 0, 15);
        getContentPane().add(label, c);
        link = new HyperLink("https://www.rapidcontext.com/");
        link.addActionListener(evt -> {
            try {
                AppUtils.openURL("https://www.rapidcontext.com/");
            } catch (Exception e) {
                parent.error(e.getMessage());
            }
        });
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 6;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 15, 0, 15);
        getContentPane().add(link, c);

        // Add close button
        button = new JButton("Close");
        button.addActionListener(evt -> dispose());
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 7;
        c.gridwidth = 2;
        c.weighty = 1.0;
        c.anchor = GridBagConstraints.SOUTH;
        c.insets = new Insets(20, 15, 10, 15);
        getContentPane().add(button, c);

        // Layout components
        pack();
    }
}
