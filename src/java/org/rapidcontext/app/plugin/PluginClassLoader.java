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

package org.rapidcontext.app.plugin;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Logger;

/**
 * Custom class loader for plug-in jars. This class loader supports
 * loading classes from a set of library directories containing
 * JAR files.
 *
 * @author Jonas Ekstrand, Dynabyte AB
 * @author Per Cederberg, Dynabyte AB
 * @version 1.0
 */
public class PluginClassLoader extends URLClassLoader {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(PluginClassLoader.class.getName());

    /**
     * The name of the lib folder under each plug-in directory.
     */
    public static final String LIB_DIR = "lib";

    /**
     * Creates a new plug-in class loader.
     */
    public PluginClassLoader() {
        super(new URL[0], PluginClassLoader.class.getClassLoader());
    }

    /**
     * Adds all JAR files found under the plug-in 'lib' directory to
     * the class loader
     *
     * @param pluginDir      the plug-in base directory
     */
    public void addPluginJars(File pluginDir) {
        File libDir = new File(pluginDir, LIB_DIR);

        if (libDir.exists()) {
            // Find all jar files
            File[] jarFiles = libDir.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.getName().toLowerCase().endsWith(".jar");
                }
            });

            // Add the jar files to the class loader
            for (int i = 0; i < jarFiles.length; i++) {
                try {
                    LOG.fine("Adding JAR to class loader: " + jarFiles[i]);
                    addURL(jarFiles[i].toURI().toURL());
                } catch (MalformedURLException e) {
                    LOG.severe("Failed to create JAR URL: " + e.getMessage());
                }
            }
        }
    }
}
