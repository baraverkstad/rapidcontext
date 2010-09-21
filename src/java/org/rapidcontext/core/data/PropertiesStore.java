/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2009 Per Cederberg & Dynabyte AB.
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

package org.rapidcontext.core.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * A data storage and retrieval handler based on properties files.
 * This class will read and write to standard Java property files in
 * a specified directory.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class PropertiesStore implements DataStore {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(PropertiesStore.class.getName());

    /**
     * The base directory for storing data files.
     */
    private File dataDir;

    /**
     * The base directory for storing normal files.
     */
    private File fileDir;

    /**
     * Creates a new properties data store.
     *
     * @param dataDir        the base data directory to use
     * @param fileDir        the base file directory to use
     */
    public PropertiesStore(File dataDir, File fileDir) {
        this.dataDir = dataDir;
        this.fileDir = fileDir;
    }

    /**
     * Checks if a file exists in this data store. Any existing file
     * will be checked for readability.
     *
     * @param path           the file path
     *
     * @return true if a readable file was found, or
     *         false otherwise
     */
    public boolean hasFile(String path) {
        File  file = new File(this.fileDir, path);

        return file.canRead();
    }

    /**
     * Checks if a data object exists in this data store.
     *
     * @param type           the type name, or null for generic
     * @param id             the unique object id
     *
     * @return true if the object was found, or
     *         false otherwise
     */
    public boolean hasData(String type, String id) {
        File  file = new File(getDataDir(type), id + ".properties");

        return file.canRead();
    }

    /**
     * Finds a file in this data store. If the exist flag is
     * specified, the file returned will be checked for both
     * existence and readability. If no file is found or if it
     * didn't pass the existence test, null will be returned
     *
     * @param path           the file path
     * @param exist          the existence check flag
     *
     * @return the file object found, or
     *         null if not found
     */
    public File findFile(String path, boolean exist) {
        File  file = new File(this.fileDir, path);

        if (exist && !file.canRead()) {
            return null;
        }
        return file;
    }

    /**
     * Lists all data type names currently in use in this store. Note
     * that type names may be returned even if there are no actual
     * data objects of that type.
     *
     * @return an array of type names
     */
    public String[] findTypes() {
        ArrayList  list = new ArrayList();
        File[]     files = getDataDir(null).listFiles();

        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory() && !this.fileDir.equals(files[i])) {
                list.add(files[i].getName());
            }
        }
        return (String[]) list.toArray(new String[list.size()]);
    }

    /**
     * Finds all data object identifiers of a certain type.
     *
     * @param type           the type name, or null for generic
     *
     * @return an array of data object identifiers
     */
    public String[] findDataIds(String type) {
        File       dir;
        String[]   files;
        ArrayList  list = new ArrayList();
        String[]   res = null;

        dir = getDataDir(type);
        if (dir.exists()) {
            files = dir.list();
            for (int i = 0; files != null && i < files.length; i++) {
                if (files[i].endsWith(".properties")) {
                    list.add(files[i].substring(0, files[i].length() - 11));
                }
            }
            res = new String[list.size()];
            list.toArray(res);
        }
        if (res == null) {
            res = new String[0];
        }
        return res;
    }

    /**
     * Returns the last modified timestamp for a data object. This
     * operation should be implemented as a fast path, without need
     * for complete parsing of the data. It is intended to be used
     * for automatically invalidating objects in data object caches.
     *
     * @param type           the type name, or null for generic
     * @param id             the unique object id
     *
     * @return the last modified timestamp, or
     *         zero (0) if unknown
     */
    public long findDataTimeStamp(String type, String id) {
        File  file = new File(getDataDir(type), id + ".properties");

        return file.lastModified();
    }

    /**
     * Reads an identified data object of a certain type.
     *
     * @param type           the type name, or null for generic
     * @param id             the unique object id
     *
     * @return the data object read, or
     *         null if not found
     *
     * @throws DataStoreException if the data couldn't be read
     */
    public Data readData(String type, String id)
        throws DataStoreException {

        File    file;
        String  msg;

        file = new File(getDataDir(type), id + ".properties");
        if (file.exists()) {
            try {
                return PropertiesSerializer.read(file);
            } catch (FileNotFoundException e) {
                msg = "failed to find file " + file.toString();
                LOG.warning(msg);
                throw new DataStoreException(msg);
            } catch (IOException e) {
                msg = "failed to read file " + file.toString() + ": " +
                      e.getMessage();
                LOG.warning(msg);
                throw new DataStoreException(msg);
            }
        } else {
            return null;
        }
    }

    /**
     * Writes a data object of a certain type.
     *
     * @param type           the type name, or null for generic
     * @param id             the unique object id
     * @param data           the data to write
     *
     * @throws DataStoreException if the data couldn't be written
     */
    public void writeData(String type, String id, Data data)
        throws DataStoreException {

        File    dir;
        File    file;
        String  msg;

        dir = getDataDir(type);
        if (!dir.exists()) {
            dir.mkdir();
        }
        file = new File(dir, id + ".properties");
        try {
            PropertiesSerializer.write(file, data);
        } catch (IOException e) {
            msg = "failed to write file " + file.toString() + ": " +
                  e.getMessage();
            LOG.warning(msg);
            throw new DataStoreException(msg);
        }
    }

    /**
     * Returns the data directory for the specified data type.
     *
     * @param type           the type name, or null for generic
     *
     * @return the data directory for the type
     */
    private File getDataDir(String type) {
        if (type == null) {
            return this.dataDir;
        } else {
            return new File(this.dataDir, type);
        }
    }
}
