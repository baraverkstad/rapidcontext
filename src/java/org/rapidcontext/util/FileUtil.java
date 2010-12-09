/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg. All rights reserved.
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

import org.apache.commons.lang.StringUtils;

/**
 * A set of utility methods for handling files.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public abstract class FileUtil {

    /**
     * Copies a file or a directory. Directories are copied
     * recursively and file modification dates are kept. If the
     * read-only or executable bits are set on the source file,
     * those bits will also be set on the destination file.
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
            copy(new FileInputStream(src), dst);
            dst.setExecutable(src.canExecute());
        }
        dst.setWritable(src.canWrite());
        dst.setLastModified(src.lastModified());
    }

    /**
     * Copies the data from an input stream to a destination file.
     * If the file exists, it will be overwritten. The destination
     * file parent directory must have been created or the copy
     * will fail. The input stream will be closed by this function.
     *
     * @param is             the input stream to read
     * @param dst            the destination file
     *
     * @throws IOException if the input stream couldn't be read or if
     *             the destination file couldn't be written
     */
    public static void copy(InputStream is, File dst) throws IOException {
        FileOutputStream  os = null;
        byte[]            buffer = new byte[16384];
        int               size;

        try {
            os = new FileOutputStream(dst);
            do {
                size = is.read(buffer);
                if (size > 0) {
                    os.write(buffer, 0, size);
                }
            } while (size > 0);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ignore) {
                // Do nothing
            }
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException ignore) {
                // Do nothing
            }
        }
    }

    /**
     * Deletes a file or a directory. This function will delete all
     * files and sub-directories inside a directory recursively.
     *
     * @param file           the file or directory to delete
     *
     * @throws IOException if some files couldn't be deleted
     *
     * @see #deleteFiles(File)
     */
    public static void delete(File file) throws IOException {
        if (file != null && file.isDirectory()) {
            deleteFiles(file);
        }
        if (file != null && !file.delete()) {
            throw new IOException("failed to delete " + file);
        }
    }

    /**
     * Deletes all files in a directory, but leaving the directory
     * otherwise unmodified. This function will delete any
     * sub-directories recursively.
     *
     * @param dir            the directory to clean
     *
     * @throws IOException if some files couldn't be deleted
     *
     * @see #delete(File)
     */
    public static void deleteFiles(File dir) throws IOException {
        if (dir != null && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    delete(files[i]);
                }
            }
        }
    }

    /**
     * Creates a unique file that doesn't exist in the specified
     * directory. The desired file name will be used if no name
     * collisions are found. Otherwise, a counter will be inserted
     * into the name to make it unique. The file name extension is
     * guaranteed to be kept.
     *
     * @param dir            the directory to check
     * @param name           the desired file name
     *
     * @return a file that doesn't already exist
     */
    public static File unique(File dir, String name) {
        String  prefix = StringUtils.substringBeforeLast(name, ".");
        String  suffix = StringUtils.substringAfterLast(name, ".");
        File    file;
        int     idx = 0;

        if (prefix.length() <= 0) {
            prefix = "file";
        }
        if (suffix.length() > 0) {
            suffix = "." + suffix;
        }
        file = new File(dir, name);
        while (file.exists()) {
            idx++;
            file = new File(dir, prefix + "-" + idx + suffix);
        }
        return file;
    }
}
