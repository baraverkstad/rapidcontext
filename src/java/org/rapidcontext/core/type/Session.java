/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2012 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.type;

import java.io.File;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.time.DateUtils;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.StorageException;

import com.eaio.uuid.UUID;

/**
 * A logged-in user session. The session allows the user client to
 * avoid re-sending authentication data on each request. The browser
 * instead sends a persistent HTTP cookie, allowing the session to be
 * loaded from disk or other storages. An external connectivity
 * environment. The environment contains a list of adapter connection
 * pool, each with their own set of configuration parameter values.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Session extends StorableObject {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(Session.class.getName());

    /**
     * The session object storage path.
     */
    public static final Path PATH = new Path("/session/");

    /**
     * The dictionary key for the user id.
     */
    public static final String KEY_USER = "user";

    /**
     * The dictionary key for the creation date & time.
     */
    public static final String KEY_CREATE_TIME = "createTime";

    /**
     * The dictionary key for the destruction date & time.
     */
    public static final String KEY_DESTROY_TIME = "destroyTime";

    /**
     * The dictionary key for the last access date & time.
     */
    public static final String KEY_ACCESS_TIME = "accessTime";

    /**
     * The dictionary key for the source IP address.
     */
    public static final String KEY_IP = "ip";

    /**
     * The dictionary key for the user agent string of the web browser.
     */
    public static final String KEY_CLIENT = "client";

    /**
     * The dictionary key for the temporary session files. All these
     * files will be deleted when the session instance is destroyed
     * (removed from in-memory storage).
     */
    public static final String KEY_FILES = "_files";

    /**
     * The default maximum session age (30 days).
     */
    public static final long MAX_AGE_MILLIS = 30L * DateUtils.MILLIS_PER_DAY;

    /**
     * The default active session time (10 minutes).
     */
    public static final long ACTIVE_MILLIS = 10L * DateUtils.MILLIS_PER_MINUTE;

    /**
     * The currently active session (for the current thread).
     */
    // TODO: Remove this variable and use some other mechanism to
    //       store request, session and context information.
    public static ThreadLocal activeSession = new ThreadLocal();

    /**
     * The modified data flag.
     */
    private boolean modified = false;

    /**
     * Searches for a specific session in the storage.
     *
     * @param storage        the storage to search in
     * @param id             the session identifier
     *
     * @return the session found, or
     *         null if not found
     */
    public static Session find(Storage storage, String id) {
        Object obj = storage.load(PATH.child(id, false));
        return (obj instanceof Session) ? (Session) obj : null;
    }

    /**
     * Stores the specified session in the provided storage.
     *
     * @param storage        the storage to use
     * @param session        the session to store
     *
     * @throws StorageException if the session couldn't be stored
     */
    public static void store(Storage storage, Session session)
        throws StorageException {

        storage.store(PATH.child(session.id(), false), session);
    }

    /**
     * Removes the specified session id from the provided storage.
     *
     * @param storage        the storage to use
     * @param id             the session id to remove
     */
    public static void remove(Storage storage, String id) {
        try {
            storage.remove(PATH.child(id, false));
        } catch (StorageException e) {
            LOG.log(Level.WARNING, "failed to delete session " + id, e);
        }
    }

    /**
     * Removes all expired sessions from the provided storage. This
     * method will load and examine all sessions that have not been
     * modified in a few days.
     *
     * @param storage        the storage to use
     */
    public static void removeExpired(Storage storage) {
        long        now = System.currentTimeMillis();
        Date        oldDate = new Date(now - 5L * DateUtils.MILLIS_PER_DAY);
        Metadata[]  meta = storage.lookupAll(PATH);

        for (int i = 0; i < meta.length; i++) {
            if (meta[i].lastModified().before(oldDate)) {
                Session session = find(storage, meta[i].id());
                if (!session.isValid()) {
                    remove(storage, session.id());
                }
            }
        }
    }

    /**
     * Creates a new session from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public Session(String id, String type, Dict dict) {
        super(id, type, dict);
        this.dict.set(KEY_FILES, new Dict());
    }

    /**
     * Creates a new session for the specified user.
     *
     * @param user           the user id
     * @param ip             the source IP address
     * @param client         the browser user agent string
     */
    public Session(String user, String ip, String client) {
        super(new UUID().toString(), "session");
        long now = System.currentTimeMillis();
        dict.set(KEY_USER, user);
        dict.set(KEY_CREATE_TIME, new Date(now));
        dict.set(KEY_DESTROY_TIME, new Date(now + MAX_AGE_MILLIS));
        dict.set(KEY_ACCESS_TIME, new Date(now));
        dict.set(KEY_IP, ip);
        dict.set(KEY_CLIENT, client);
        dict.set(KEY_FILES, new Dict());
    }

    /**
     * Checks if this object is in active use. This method returns
     * true during 600 seconds after the last access, thereafter
     * false.
     *
     * @return true if the object is active, or
     *         false otherwise
     */
    protected boolean isActive() {
        long now = System.currentTimeMillis();
        long lastActive = accessTime().getTime() + ACTIVE_MILLIS;
        return now <= lastActive;
    }

    /**
     * Checks if this object has been modified since initialized from
     * storage.
     *
     * @return true if the object has been modified, or
     *         false otherwise
     */
    protected boolean isModified() {
        return modified;
    }

    /**
     * Destroys this session. This method is used to free resources
     * used when the session is no longer in active use. It is called
     * when the session instance is removed from in-memory storage
     * (the object cache).
     */
    protected void destroy() {
        removeAllFiles();
    }

    /**
     * Discards the modified flag for this object.
     */
    protected void passivate() {
        modified = false;
    }

    /**
     * Returns a serialized representation of this object. Used when
     * accessing the object from outside pure Java.
     *
     * @return the serialized representation of this object
     */
    public Dict serialize() {
        dict.setBoolean("_valid", isValid());
        return dict;
    }

    /**
     * Checks if this session is valid (hasn't expired).
     *
     * @return true if the session is valid, or
     *         false otherwise
     */
    public boolean isValid() {
        return destroyTime().after(new Date());
    }

    /**
     * Returns the session user identifier.
     *
     * @return the session user identifier.
     */
    public String userId() {
        return dict.getString(KEY_USER, "");
    }

    /**
     * Returns the session creation date & time.
     *
     * @return the session creation date & time.
     */
    public Date createTime() {
        return dict.getDate(KEY_CREATE_TIME, new Date(0));
    }

    /**
     * Returns the scheduled session destruction date & time.
     *
     * @return the session destruction date & time.
     */
    public Date destroyTime() {
        return dict.getDate(KEY_DESTROY_TIME, new Date(0));
    }

    /**
     * Returns the session last access date & time.
     *
     * @return the session last access date & time.
     */
    public Date accessTime() {
        return dict.getDate(KEY_ACCESS_TIME, new Date(0));
    }

    /**
     * Updates the session last access date & time to the current
     * system time.
     */
    public void updateAccessTime() {
        modified = true;
        dict.set(KEY_ACCESS_TIME, new Date());
    }

    /**
     * Returns the session source IP address.
     *
     * @return the session source IP address.
     */
    public String ip() {
        return dict.getString(KEY_IP, "");
    }

    /**
     * Returns the session user agent string of the web browser.
     *
     * @return the session user agent string.
     */
    public String client() {
        return dict.getString(KEY_CLIENT, "");
    }

    /**
     * Returns a dictionary of all session files. The files are
     * indexed by their unique id.
     *
     * @return a dictionary of all files
     */
    public Dict files() {
        return dict.getDict(KEY_FILES);
    }

    /**
     * Returns a session file with the specified unique id.
     *
     * @param id             the file id
     *
     * @return the session file, or
     *         null if no such file was found
     */
    public File file(String id) {
        return (File) files().get(id);
    }

    /**
     * Adds a file to the session. The file will be automatically
     * deleted when the session expires or is moved from memory to
     * persistent storage.
     *
     * @param id             the file id
     * @param file           the file to add
     */
    public void addFile(String id, File file) {
        if (this.file(id) != null) {
            removeFile(id);
        }
        files().set(id, file);
    }

    /**
     * Removes and deletes a session file. If the file has been moved
     * from its original location, it wont be deleted.
     *
     * @param id             the file id
     */
    public void removeFile(String id) {
        File file = this.file(id);
        if (file != null && file.canWrite()) {
            file.delete();
        }
        files().remove(id);
    }

    /**
     * Removes and deletes all session files. If the files have been
     * moved from their original location, they wont be deleted.
     */
    public void removeAllFiles() {
        String ids[] = files().keys();
        for (int i = 0; i < ids.length; i++) {
            removeFile(ids[i]);
        }
    }

    /**
     * Validates this session with the specified IP address and user
     * agent string. If the session IP and user agent string doesn't
     * match the values provided, a security exception is thrown and
     * the session is invalidated.
     *
     * @param ip             the IP address
     * @param client         the user agent string
     *
     * @throws SecurityException if the session didn't match the
     *     the provided values
     */
    public void validate(String ip, String client) throws SecurityException {
        String  msg;

        if (!isValid()) {
            throw new SecurityException("Session has expired");
        }
        if (!ip().equals(ip)) {
            msg = "Attempt to re-bind HTTP session from IP '" + ip() +
                  "' to '" + ip + "', invalidating session";
            invalidate();
            throw new SecurityException(msg);
        }
        if (!client().equals(client)) {
            msg = "Attempt to re-bind HTTP session from user agent '" +
                  client() + "' to '" + client + "', invalidating session";
            invalidate();
            throw new SecurityException(msg);
        }
    }

    /**
     * Invalidates this session by marking it as expired. This
     * operation is irreversible and will eventually cause the
     * removal of the session in the storage.
     */
    public void invalidate() {
        dict.set(KEY_DESTROY_TIME, new Date(0));
    }
}
