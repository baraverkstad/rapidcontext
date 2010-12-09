/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg & Dynabyte AB.
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

package org.rapidcontext.util;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A set of utility methods for handling ZIP files.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ZipUtil {

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
        ZipFile  zip;

        zip = new ZipFile(zipFile);
        try {
            unpackZip(zip, dir);
        } finally {
            zip.close();
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
        Enumeration  entries;
        ZipEntry     entry;
        File         file;
        String       name;

        entries = zip.entries();
        while (entries.hasMoreElements()) {
            entry = (ZipEntry) entries.nextElement();
            name = entry.getName();
            while (name.startsWith("/")) {
                name = name.substring(1);
            }
            file = new File(dir, name);
            file.getParentFile().mkdirs();
            if (!entry.isDirectory()) {
                FileUtil.copy(zip.getInputStream(entry), file);
            }
        }
    }
}
