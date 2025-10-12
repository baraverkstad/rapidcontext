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
 * @author Per Cederberg
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
        try (ZipFile zip = new ZipFile(zipFile)) {
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
        dir = FileUtil.canonical(dir);
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName().replaceAll("//+", "/");
            while (name.startsWith("/")) {
                name = name.substring(1);
            }
            for (String part : name.split("/")) {
                if (part.equals(".") || part.equals("..")) {
                    String msg = "zip file " + zip.getName() + " entry invalid: " + entry.getName();
                    throw new IOException(msg);
                }
            }
            File file = FileUtil.canonical(new File(dir, name));
            if (!file.toString().startsWith(dir.toString())) {
                String msg = "zip file " + zip.getName() + " entry invalid: " + entry.getName();
                throw new IOException(msg);
            }
            if (entry.isDirectory()) {
                file.mkdirs();
            } else {
                file.getParentFile().mkdirs();
                try (InputStream is = zip.getInputStream(entry)) {
                    FileUtil.copy(is, file);
                }
            }
        }
    }

    // No instances
    private ZipUtil() {}
}
