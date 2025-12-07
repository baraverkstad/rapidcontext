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

package org.rapidcontext.util;

import static org.junit.Assert.*;
import static org.rapidcontext.util.ClasspathUtil.*;

import java.io.File;
import java.net.URL;
import java.util.jar.Manifest;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class ClasspathUtilTest {

    @Test
    public void testClassLoader() {
        assertNotNull(classLoader(null));
        assertNotNull(classLoader(String.class));
    }

    @Test
    public void testLocate() {
        URL url = locate(ClasspathUtil.class);
        assertNotNull(url);
        assertTrue(url.toString().endsWith("org/rapidcontext/util/ClasspathUtil.class"));

        url = locate(ClasspathUtil.class, "BinaryUtil.class");
        assertNotNull(url);
        assertTrue(url.toString().endsWith("org/rapidcontext/util/BinaryUtil.class"));

        url = locate(ClasspathUtil.class, "./BinaryUtil.class");
        assertNotNull(url);
        assertTrue(url.toString().endsWith("org/rapidcontext/util/BinaryUtil.class"));

        url = locate(ClasspathUtil.class, "/META-INF/MANIFEST.MF");
        assertNotNull(url);
        assertTrue(url.toString().endsWith("/META-INF/MANIFEST.MF"));

        String path = "/org/rapidcontext/app/ui/logotype.png";
        url = locate(ClasspathUtil.class, "classpath:" + path);
        assertNotNull(url);
        assertTrue(url.toString().endsWith(path));

        assertNull(locate(ClasspathUtil.class, "not-found.txt"));
    }

    @Test
    public void testLocateFile() {
        File file = locateFile(ClasspathUtil.class);
        assertTrue(file.exists());
        assertTrue(file.getName().endsWith(".class") || file.getName().endsWith(".jar"));
    }

    @Test
    public void testManifest() {
        Manifest mf = manifest(ClasspathUtil.class);
        assertNotNull(mf);
        assertNotNull(mf.getMainAttributes());
    }

    @Test
    public void testManifestAttribute() {
        String str = manifestAttribute(ClasspathUtil.class, "Package-Title");
        assertEquals("rapidcontext", str);
        str = manifestAttribute(ClasspathUtil.class, "NonExistentAttribute");
        assertNull(str);
    }
}
