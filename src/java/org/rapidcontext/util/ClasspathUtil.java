/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2017 Per Cederberg. All rights reserved.
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

package org.rapidcontext.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.jar.Manifest;

/**
 * A set of utility methods for working with the classpath and class loaders.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public abstract class ClasspathUtil {

    /**
     * Returns the class loader for the specified class.
     *
     * @param cls            the class to check
     *
     * @return the class loader for the class, or
     *         the system class loader if null
     */
    public static ClassLoader classLoader(Class<?> cls) {
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
    public static URL locate(Class<?> cls) {
        String path = cls.getName().replace('.', '/') + ".class";
        return locate(cls, path);
    }

    /**
     * Returns the resource URL corresponding the the specified path. The
     * resource will be located using the class loader for the specified class.
     * Also, the resource path will be normalized, removing any "classpath:"
     * prefix and fixing the usage of "/" chars.
     *
     * @param cls            the base class (loader) to use
     * @param path           the resource path
     *
     * @return the resource URL, or
     *         null if not found
     */
    public static URL locate(Class<?> cls, String path) {
        if (path.startsWith("classpath:")) {
            path = path.substring(10);
        }
        path = path.replaceAll("//", "/");
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return cls.getResource(path);
    }

    /**
     * Returns the file containing the specified class. This method uses the
     * class resource URL to attempt to guess the file system location of the
     * class or JAR file.
     *
     * @param cls            the class to check
     *
     * @return the absolute file system location for the class, or
     *         null if not found
     */
    public static File locateFile(Class<?> cls) {
        URL     url = locate(cls);
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
        return file.exists() ? file.getAbsoluteFile() : null;
    }

    /**
     * Returns the manifest for the JAR file containing the specified class.
     * The JAR file will be located via the class loader for the specified
     * class.
     *
     * @param cls            the class to check
     *
     * @return the JAR manifest data, or
     *         null if not found
     */
    public static Manifest manifest(Class<?> cls) {
        URL url = locate(cls);
        if (url.toExternalForm().toLowerCase().startsWith("jar:")) {
            try {
                JarURLConnection con = (JarURLConnection) url.openConnection();
                return con.getManifest();
            } catch (Throwable ignore) {
                // Ignore errors, return null instead
            }
        }
        return null;
    }

    /**
     * Returns a single attribute value from the JAR manifest for a class. The
     * JAR file will be located via the class loader for the specified class.
     *
     * @param cls            the class to check
     * @param name           the main attribute name
     *
     * @return the attribute value, or
     *         null if not found
     */
    public static String manifestAttribute(Class<?> cls, String name) {
        Manifest mf = manifest(cls);
        if (mf != null) {
            return mf.getMainAttributes().getValue(name);
        }
        return null;
    }
}
