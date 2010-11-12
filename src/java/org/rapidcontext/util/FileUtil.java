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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A set of utility methods for handling files.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class FileUtil {

    /**
     * Copies a file or a directory. Directories are copied
     * recursively and file modification dates are kept.
     *
     * @param src            the source file or directory
     * @param dst            the destination file or directory
     *
     * @throws IOException if some file couldn't be copied
     */
    public static void copy(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            dst.mkdirs();
            File[] files = src.listFiles();
            for (int i = 0; i < files.length; i++) {
                copy(files[i], new File(dst, files[i].getName()));
            }
        } else {
            writeStream(new FileInputStream(src), dst);
            dst.setWritable(src.canWrite());
            dst.setExecutable(src.canExecute());
        }
        dst.setLastModified(src.lastModified());
    }

    /**
     * Deletes a file or a directory. This function will delete all
     * files and sub-directories inside a directory recursively.
     *
     * @param file           the file or directory to delete
     *
     * @throws IOException if some file couldn't be deleted
     */
    public static void delete(File file) throws IOException {
        if (file != null && file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    delete(files[i]);
                }
            }
        }
        if (file != null && !file.delete()) {
            throw new IOException("failed to delete " + file);
        }
    }

    /**
     * Deletes the contents of a directory. This function will delete all
     * files and sub-directories inside a directory recursively. The parent
     * directory won't be deleted.
     *
     * @param dir            the directory to clean
     *
     * @throws IOException if some file couldn't be deleted
     */
    public static void deleteContents(File dir) throws IOException {
        if (dir != null && dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (int i = 0; files != null && i < files.length; i++) {
                delete(files[i]);
            }
        }
    }

    /**
     * Writes the data from an input stream to a destination file.
     * If the file exists, it will be overwritten. Also, any file
     * parent directory must have been creates, for example by
     * calling mkdirs(). The input stream will be closed by this
     * function.
     *
     * @param is             the input stream to read
     * @param destFile       the destination output file
     *
     * @throws IOException if the file couldn't be deleted
     */
    public static void writeStream(InputStream is, File destFile)
        throws IOException {

        FileOutputStream  os;
        byte[]            buffer = new byte[4096];
        int               size;

        os = new FileOutputStream(destFile);
        try {
            do {
                size = is.read(buffer);
                if (size > 0) {
                    os.write(buffer, 0, size);
                }
            } while (size > 0);
        } finally {
            try {
                is.close();
            } catch (IOException ignore) {
                // Do nothing
            }
            try {
                os.close();
            } catch (IOException ignore) {
                // Do nothing
            }
        }
    }

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
                writeStream(zip.getInputStream(entry), file);
            }
        }
    }
}
