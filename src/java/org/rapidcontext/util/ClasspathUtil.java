/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2023 Per Cederberg. All rights reserved.
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
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.jar.Manifest;

import org.apache.commons.lang3.StringUtils;

/**
 * A set of utility methods for working with the classpath and class loaders.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public final class ClasspathUtil {

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
        String path = cls.getName().replace('.', File.separatorChar) + ".class";
        return locate(cls, path);
    }

    /**
     * Returns the resource URL corresponding the the specified path. The
     * resource will be located using the class loader for the specified class.
     * Also, the resource path will be normalized, removing any "classpath:"
     * prefix and normalizing the file path.
     *
     * @param cls            the base class (loader) to use
     * @param path           the resource path
     *
     * @return the resource URL, or
     *         null if not found
     */
    public static URL locate(Class<?> cls, String path) {
        path = StringUtils.removeStartIgnoreCase(path, "classpath:");
        path = Path.of(path).normalize().toString();
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
        URL url = locate(cls);
        if (url != null) {
            String path = url.toExternalForm();
            if (StringUtils.startsWithIgnoreCase(path, "jar:")) {
                path = StringUtils.substringBefore(path.substring(4), "!/");
            }
            if (StringUtils.startsWithIgnoreCase(path, "file:")) {
                path = path.substring(5);
                path = Path.of(path).normalize().toString();
                path = StringUtils.prependIfMissing(path, File.separator);
            }
            File file = new File(URLDecoder.decode(path, StandardCharsets.UTF_8));
            return file.exists() ? file.getAbsoluteFile() : null;
        }
        return null;
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
            } catch (Exception ignore) {
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

    // No instances
    private ClasspathUtil() {}
}
