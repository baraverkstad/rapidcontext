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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.jar.Manifest;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class ClasspathUtilTest {

    @Test
    public void testClassLoader() {
        assertNotNull(ClasspathUtil.classLoader(null));
        assertNotNull(ClasspathUtil.classLoader(String.class));
    }

    @Test
    public void testLocate() {
        URL url = ClasspathUtil.locate(ClasspathUtil.class);
        assertNotNull(url);
        assertTrue(url.toString().endsWith("org/rapidcontext/util/ClasspathUtil.class"));

        url = ClasspathUtil.locate(ClasspathUtil.class, "BinaryUtil.class");
        assertNotNull(url);
        assertTrue(url.toString().endsWith("org/rapidcontext/util/BinaryUtil.class"));

        url = ClasspathUtil.locate(ClasspathUtil.class, "./BinaryUtil.class");
        assertNotNull(url);
        assertTrue(url.toString().endsWith("org/rapidcontext/util/BinaryUtil.class"));

        url = ClasspathUtil.locate(ClasspathUtil.class, "/META-INF/MANIFEST.MF");
        assertNotNull(url);
        assertTrue(url.toString().endsWith("/META-INF/MANIFEST.MF"));

        String path = "/org/rapidcontext/app/ui/logotype.png";
        url = ClasspathUtil.locate(ClasspathUtil.class, "classpath:" + path);
        assertNotNull(url);
        assertTrue(url.toString().endsWith(path));

        assertNull(ClasspathUtil.locate(ClasspathUtil.class, "not-found.txt"));
    }

    @Test
    public void testLocateFile() {
        File file = ClasspathUtil.locateFile(ClasspathUtil.class);
        assertTrue(file.exists());
        assertTrue(file.getName().endsWith(".class") || file.getName().endsWith(".jar"));
    }

    @Test
    public void testManifest() {
        Manifest mf = ClasspathUtil.manifest(ClasspathUtil.class);
        assertNotNull(mf);
        assertNotNull(mf.getMainAttributes());
    }

    @Test
    public void testManifestAttribute() {
        String str = ClasspathUtil.manifestAttribute(ClasspathUtil.class, "Package-Title");
        assertEquals("rapidcontext", str);
        str = ClasspathUtil.manifestAttribute(ClasspathUtil.class, "NonExistentAttribute");
        assertNull(str);
    }
}
