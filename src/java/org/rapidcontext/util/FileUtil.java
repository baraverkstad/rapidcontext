/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2026 Per Cederberg. All rights reserved.
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

import static java.nio.file.StandardCopyOption.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;

import org.apache.commons.lang3.StringUtils;

/**
 * A set of utility methods for handling files.
 *
 * @author Per Cederberg
 */
public final class FileUtil {

    /**
     * The base temporary directory to use.
     */
    private static File tempDir = null;

    /**
     * Attempts to resolve a file to a canonical file.
     *
     * @param file           the file to resolve
     *
     * @return the canonical file, or
     *         the original file if the resolution failed
     */
    public static File canonical(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException ignore) {
            return file;
        }
    }

    /**
     * Finds a child file in a directory by case-insensitive search.
     * If no file is found, a new file object is created.
     *
     * @param dir            the parent directory
     * @param child          the child file name
     *
     * @return the corresponding file object
     */
    public static File resolve(File dir, String child) {
        File[] f = dir.listFiles((d, n) -> n.equalsIgnoreCase(child));
        return (f != null && f.length > 0) ? f[0] : new File(dir, child);
    }

    /**
     * Reads a file containing UTF-8 text content.
     *
     * @param file           the input file to read
     *
     * @return the text content of the file
     *
     * @throws IOException if the file couldn't be read
     */
    public static String readText(File file) throws IOException {
        try (FileInputStream is = new FileInputStream(file)) {
            return readText(is);
        }
    }

    /**
     * Reads an input stream containing UTF-8 text content.
     *
     * @param is             the input stream to read
     *
     * @return the text content of the input stream
     *
     * @throws IOException if the input stream couldn't be read
     */
    public static String readText(InputStream is) throws IOException {
        try (
            InputStreamReader stream = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(stream);
        ) {
            StringBuilder res = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                res.append(line);
                res.append('\n');
            }
            return res.toString();
        }
    }

    /**
     * Copies a file or a directory. Directories are copied
     * recursively and file modification dates are kept. If the
     * executable bit is set on the source file, that bit will also
     * be set on the destination file.
     *
     * @param src            the source file or directory
     * @param dst            the destination file or directory
     *
     * @throws IOException if some file couldn't be copied
     */
    @SuppressWarnings("resource")
    public static void copy(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            dst.mkdirs();
            for (File file : src.listFiles()) {
                copy(file, new File(dst, file.getName()));
            }
        } else {
            copy(new FileInputStream(src), dst);
            if (src.canExecute() && !dst.setExecutable(true)) {
                throw new IOException("failed to set executable: " + dst);
            }
        }
        if (!dst.setLastModified(src.lastModified())) {
            throw new IOException("failed to set last modified: " + dst);
        }
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
    @SuppressWarnings("resource")
    public static void copy(InputStream is, File dst) throws IOException {
        copy(is, new FileOutputStream(dst));
    }

    /**
     * Moves a file or directory to a new location. This method attempts
     * to perform an atomic move if possible. If the destination file
     * already exists, it will be overwritten.
     *
     * @param source         the source file or directory
     * @param target         the target file or directory
     *
     * @throws IOException if the move failed
     */
    public static void move(File source, File target) throws IOException {
        try {
            Files.move(source.toPath(), target.toPath(), ATOMIC_MOVE, REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            copy(source, target);
            delete(source);
        }
    }

    /**
     * Copies the data from an input stream to an output stream. Both
     * the streams will be closed when this function returns.
     *
     * @param input          the input stream to read
     * @param output         the output stream to write
     *
     * @throws IOException if the input stream couldn't be read or if
     *             the output stream couldn't be written
     */
    public static void copy(InputStream input, OutputStream output)
    throws IOException {

        try (InputStream is = input; OutputStream os = output) {
            byte[] buffer = new byte[16384];
            int size;
            do {
                size = is.read(buffer);
                if (size > 0) {
                    os.write(buffer, 0, size);
                }
            } while (size > 0);
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
                for (File f : files) {
                    delete(f);
                }
            }
        }
    }

    /**
     * Deletes all empty directories in a directory, but leaves the
     * directory itself unmodified. This method will remove any empty
     * directories recursively, making it possible to remove a tree
     * of empty directories.
     *
     * @param dir            the directory to clean
     *
     * @throws IOException if some files couldn't be deleted
     *
     * @see #deleteFiles(File)
     */
    public static void deleteEmptyDirs(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteEmptyDirs(f);
                    File[] subfiles = f.listFiles();
                    if (subfiles == null || subfiles.length == 0) {
                        delete(f);
                    }
                }
            }
        }
    }

    /**
     * Sets the temporary directory to use.
     *
     * @param dir            the new temporary directory
     */
    public static void setTempDir(File dir) {
        tempDir = canonical(dir);
    }

    /**
     * Creates a new temporary file. If a temporary directory has
     * been set, it will be used. Otherwise the default temporary
     * directory will be used. The generated file is guaranteed to
     * keep the same file extension as the desired file name.
     *
     * @param name           the desired file name
     *
     * @return a new empty temporary file
     *
     * @throws IOException if the temporary file couldn't be created
     */
    public static File tempFile(String name) throws IOException {
        String prefix = StringUtils.substringBeforeLast(name, ".");
        String suffix = StringUtils.substringAfterLast(name, ".");
        if (prefix == null || prefix.isBlank() || prefix.length() < 3) {
            prefix = "file";
        }
        if (suffix != null && !suffix.isBlank()) {
            suffix = "." + suffix;
        }
        File file = File.createTempFile(prefix + "-", suffix, tempDir);
        if (tempDir != null && !file.getCanonicalPath().startsWith(tempDir.toString())) {
            throw new IOException("invalid temporary file name: " + file);
        }
        file.deleteOnExit();
        return file;
    }

    // No instances
    private FileUtil() {}
}
