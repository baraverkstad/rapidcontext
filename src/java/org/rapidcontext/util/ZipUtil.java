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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A set of utility methods for handling ZIP files.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public final class ZipUtil {

    /**
     * Unpacks a ZIP file into a specified directory.
     *
     * @param zipFile        the ZIP file to unpack
     * @param dir            the destination directory
     *
     * @throws IOException if the ZIP file couldn't be read or the
     *             destination files couldn't be written
     */
    public static void unpackZip(File zipFile, File dir) throws IOException {
        try (
            ZipFile zip = new ZipFile(zipFile);
        ) {
            unpackZip(zip, dir);
        }
    }

    /**
     * Unpacks a ZIP file into a specified directory. This function
     * will not close the ZIP file specified.
     *
     * @param zip            the ZIP file to unpack
     * @param dir            the destination directory
     *
     * @throws IOException if the ZIP file couldn't be read or the
     *             destination files couldn't be written
     */
    public static void unpackZip(ZipFile zip, File dir) throws IOException {
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            while (name.startsWith("/")) {
                name = name.substring(1);
            }
            File file = new File(dir, name);
            if (file.getCanonicalPath().startsWith(dir.toString())) {
                throw new IOException("invalid file path in zip: " + zip.getName());
            }
            file.getParentFile().mkdirs();
            if (!entry.isDirectory()) {
                try (InputStream is = zip.getInputStream(entry)) {
                    FileUtil.copy(is, file);
                }
            }
        }
    }

    // No instances
    private ZipUtil() {}
}
