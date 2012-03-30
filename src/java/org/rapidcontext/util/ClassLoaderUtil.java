/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2012 Per Cederberg. All rights reserved.
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

package org.rapidcontext.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * A set of utility methods for working with class loaders.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public abstract class ClassLoaderUtil {

    /**
     * Returns the class loader for the specified class.
     *
     * @param cls            the class to check
     *
     * @return the class loader for the class, or
     *         the system class loader if null
     */
    public static ClassLoader getClassLoader(Class cls) {
        if (cls == null || cls.getClassLoader() == null) {
            return ClassLoader.getSystemClassLoader();
        } else {
            return cls.getClassLoader();
        }
    }

    /**
     * Returns the resource URL corresponding the the specified class.
     *
     * @param cls            the class to check
     *
     * @return the resource URL for the class, or
     *         null if not found
     */
    public static URL getResource(Class cls) {
        String  name = cls.getName().replace('.', '/') + ".class";
        return getClassLoader(cls).getResource(name);
    }

    /**
     * Returns the JAR manifest corresponding to the specified class.
     *
     * @param cls            the class to check
     *
     * @return the JAR manifest data, or
     *         null if not found
     */
    public static Manifest getManifest(Class cls) {
        JarFile file = null;
        try {
            file = new JarFile(getLocation(cls));
            return file.getManifest();
        } catch (IOException e) {
            // Ignored, happens if no manifest is found
        } finally {
            try {
                if (file != null) {
                    file.close();
                }
            } catch (Throwable e) {
                // Ignored
            }
        }
        return null;
    }

    /**
     * Returns a JAR manifest main attribute. The JAR manifest will
     * be located from the specified class.
     *
     * @param cls            the class to check
     * @param name           the main attribute name
     *
     * @return the attribute value, or
     *         null if not found
     */
    public static String getManifestAttribute(Class cls, String name) {
        Manifest mf = getManifest(cls);
        if (mf != null) {
            return mf.getMainAttributes().getValue(name);
        }
        return null;
    }

    /**
     * Returns the file containing the specified class. This method
     * uses the class resource URL to attempt to deduce the file
     * system location of the class.
     *
     * @param cls            the class to check
     *
     * @return the absolute file system location for the class, or
     *         null if not found
     */
    public static File getLocation(Class cls) {
        URL     url = getResource(cls);
        String  path;
        String  str;
        File    file;

        if (url == null) {
            return null;
        }
        path = url.toExternalForm();
        do {
            str = path.toLowerCase();
            if (str.startsWith("jar:")) {
                path = path.substring(4);
                if (path.indexOf("!/") >= 0) {
                    path = path.substring(0, path.indexOf("!/"));
                }
            } else if (str.startsWith("file:/")) {
                path = path.substring(6);
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                while (path.length() > 1 && path.charAt(1) == '/') {
                    path = path.substring(1);
                }
            }
        } while (path.length() < str.length());
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException ignore) {
            // Yeah, right... That just HAD to be a checked exception.
        }
        file = new File(path);
        return file.exists() ? file.getAbsoluteFile() : file;
    }
}
