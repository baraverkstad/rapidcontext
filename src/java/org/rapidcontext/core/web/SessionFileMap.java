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

package org.rapidcontext.core.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

/**
 * The session file map. This class handles storage of files in the
 * HTTP session. Instances of this class listens on the session
 * invalidation event, in which case all remaining session files
 * will be deleted.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class SessionFileMap implements HttpSessionBindingListener {

    /**
     * The session file map attribute name.
     */
    private static final String FILES_ATTRIBUTE = "filemap";

    /**
     * The file creation progress counter. This value is set to
     * zero (0.0) when initiating a file upload and progresses to
     * one (1.0) when completed. It is used when adding files to the
     * session via file upload and also blocks other file uploads to
     * the session while in progress.
     */
    private double progress = 1.0d;

    /**
     * The map with all files. The file names are mapped to the
     * actual File objects.
     */
    private HashMap files = new HashMap();

    /**
     * Returns the session file map included in a session. If none
     * exists, a new empty one may be created if desired.
     *
     * @param session        the HTTP session
     * @param create         the create file map flag
     *
     * @return the session file map, or
     *         null if not found and not created
     */
    public static SessionFileMap getFiles(HttpSession session, boolean create) {
        SessionFileMap  map;

        map = (SessionFileMap) session.getAttribute(FILES_ATTRIBUTE);
        if (map == null && create) {
            map = new SessionFileMap();
            session.setAttribute(FILES_ATTRIBUTE, map);
        }
        return map;
    }

    /**
     * Creates a new session file map.
     */
    private SessionFileMap() {
        // No further initialization needed
    }

    /**
     * Returns the file creation progress counter. This value is set
     * to zero (0.0) when initiating a file upload and progresses to
     * one (1.0) when completed. It is used when adding files to the
     * session via file upload and also blocks other file uploads to
     * the session while in progress.
     *
     * @return the file creation progress counter
     */
    public double getProgress() {
        return progress;
    }

    /**
     * Returns a file with a specified id.
     *
     * @param id             the file id
     *
     * @return the file found, or
     *         null if no such file exists
     */
    public File getFile(String id) {
        return (File) files.get(id);
    }

    /**
     * Returns a map of all files. The files are indexed by their
     * unique id.
     *
     * @return a map of all files
     */
    public Map getAllFiles() {
        return files;
    }

    /**
     * Adds a file. After this point, the file will be managed by
     * this object, meaning that it will be deleted once the session
     * is terminated.
     *
     * @param id             the file id
     * @param file           the file to add
     */
    public void addFile(String id, File file) {
        File  oldFile;

        oldFile = (File) files.get(id);
        if (oldFile != null && !oldFile.equals(file)) {
            removeFile(id);
        }
        files.put(id, file);
    }

    /**
     * Adds a file with data from an input stream. This method will
     * update the progress counter while creating the file, thereby
     * locking other threads from accessing this method for the same
     * file object. After creating the file, it will be managed by
     * this object, meaning that it will be deleted once the session
     * is terminated. If a non-zero delay value is specified, the
     * stream processing will be delayed to facilitate testing.
     *
     * @param id             the file id
     * @param file           the file name
     * @param size           the estimated file size
     * @param is             the input stream to read
     * @param delay          the additional delay in seconds
     *
     * @throws IOException if the file creation failed
     */
    public void addFile(String id, File file, int size, InputStream is, int delay)
        throws IOException {

        FileOutputStream  os;
        int               count;
        byte[]            buffer = new byte[1024];
        int               bufSize;

        if (progress < 1.0d) {
            throw new IOException("another file upload is in progress");
        }
        try {
            progress = 0.0d;
            if (size > 0) {
                delay *= 1000;
                delay = (int) Math.round(delay / Math.ceil(size / (double) buffer.length));
            } else if (delay > 0 ) {
                delay = 10;
            }
            os = new FileOutputStream(file);
            count = 0;
            while ((bufSize = is.read(buffer)) > 0) {
                os.write(buffer, 0, bufSize);
                count += bufSize;
                if (size > 0) {
                    progress = Math.min(1.0d, (double) count / (double) size);
                }
                if (delay > 0) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ignore) {
                        // Ignore thread interrupts
                    }
                }
            }
            os.close();
            is.close();
            addFile(id, file);
        } finally {
            progress = 1.0;
        }
    }

    /**
     * Removes a file.
     *
     * @param id             the file id
     */
    public void removeFile(String id) {
        File  file;

        file = (File) files.get(id);
        if (file != null) {
            files.remove(id);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * Removes all files.
     */
    public void removeAllFiles() {
        Iterator  iter;
        File      file;

        iter = files.values().iterator();
        while (iter.hasNext()) {
            file = (File) iter.next();
            if (file.exists()) {
                file.delete();
            }
        }
        files.clear();
    }

    /**
     * Notifies the manager that it is being bound to a session.
     *
     * @param event          the binding event
     */
    public void valueBound(HttpSessionBindingEvent event) {
        // No need to do anything
    }

    /**
     * Notifies the manager that it is being unbound from a
     * session.
     *
     * @param event          the unbinding event
     */
    public void valueUnbound(HttpSessionBindingEvent event) {
        removeAllFiles();
    }
}
