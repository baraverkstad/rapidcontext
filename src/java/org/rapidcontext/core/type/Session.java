/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2019 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.type;

import java.io.File;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.time.DateUtils;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.StorageException;

/**
 * A logged-in user session. The session allows the user client to
 * avoid re-sending authentication data on each request. The browser
 * instead sends a persistent HTTP cookie, allowing the session to be
 * loaded from disk or other storages.
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
     * The dictionary key for the creation timestamp.
     */
    public static final String KEY_CREATE_TIME = "createTime";

    /**
     * The dictionary key for the destruction timestamp.
     */
    public static final String KEY_DESTROY_TIME = "destroyTime";

    /**
     * The dictionary key for the last access timestamp.
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
     * The expiry timeout (after last access) for anonymous users (30 minutes).
     */
    public static final long EXPIRY_ANON_MILLIS = 30L * DateUtils.MILLIS_PER_MINUTE;

    /**
     * The expiry timeout (after last access) for logged in users (30 days).
     */
    public static final long EXPIRY_AUTH_MILLIS = 30L * DateUtils.MILLIS_PER_DAY;

    /**
     * The default active session time (5 minutes).
     */
    public static final long ACTIVE_MILLIS = 5L * DateUtils.MILLIS_PER_MINUTE;

    /**
     * The currently active session (for the current thread).
     */
    // TODO: Remove this variable and use some other mechanism to
    //       store request, session and context information.
    public static ThreadLocal<Session> activeSession = new ThreadLocal<>();

    /**
     * The initial creation flag.
     */
    private boolean created = false;

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
        return find(storage, new Path(PATH, id));
    }

    /**
     * Searches for a specific session in the storage.
     *
     * @param storage        the storage to search in
     * @param path           the path to the session
     *
     * @return the session found, or
     *         null if not found
     */
    private static Session find(Storage storage, Path path) {
        Object obj = storage.load(path);
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

        storage.store(session.path(), session);
    }

    /**
     * Removes the specified session id from the provided storage.
     *
     * @param storage        the storage to use
     * @param id             the session id to remove
     */
    public static void remove(Storage storage, String id) {
        try {
            storage.remove(new Path(PATH, id));
        } catch (StorageException e) {
            LOG.log(Level.WARNING, "failed to delete session " + id, e);
        }
    }

    /**
     * Removes all expired sessions from the provided storage. This
     * method will load and examine sessions that have not been
     * modified in 30 minutes.
     *
     * @param storage        the storage to use
     */
    public static void removeExpired(Storage storage) {
        Metadata[] meta = storage.lookupAll(PATH);
        // TODO: session expiry must be handled with iterator
        for (int i = 0; i < meta.length; i++) {
            Session session = find(storage, meta[i].path());
            if (session != null) {
                String userId = session.userId();
                if (!session.isValid()) {
                    LOG.fine("deleting " + session + ", invalid or expired");
                    remove(storage, session.id());
                } else if (userId.length() > 0) {
                    User user = User.find(storage, userId);
                    if (user == null || !user.isEnabled()) {
                        String msg = "no enabled user " + userId;
                        LOG.fine("deleting " + session + ", " + msg);
                        remove(storage, session.id());
                    }
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
     * @param userId         the user id
     * @param ip             the source IP address
     * @param client         the browser user agent string
     */
    public Session(String userId, String ip, String client) {
        super(UUID.randomUUID().toString(), "session");
        long now = System.currentTimeMillis();
        userId = (userId == null) ? null : userId.trim();
        dict.set(KEY_USER, userId);
        dict.set(KEY_CREATE_TIME, new Date(now));
        if (userId != null && userId.length() > 0) {
            dict.set(KEY_DESTROY_TIME, new Date(now + EXPIRY_AUTH_MILLIS));
        } else {
            dict.set(KEY_DESTROY_TIME, new Date(now + EXPIRY_ANON_MILLIS));
        }
        dict.set(KEY_ACCESS_TIME, new Date(now));
        dict.set(KEY_IP, ip);
        dict.set(KEY_CLIENT, client);
        dict.set(KEY_FILES, new Dict());
        created = true;
        modified = true;
    }

    /**
     * Checks if this object is in active use. This method returns
     * true during some minutes after the last access, thereafter
     * false.
     *
     * @return true if the object is active, or
     *         false otherwise
     *
     * @see #ACTIVE_MILLIS
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
        created = false;
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
     * Checks if this session is new (hasn't been stored).
     *
     * @return true if the session is new, or
     *         false otherwise
     */
    public boolean isNew() {
        return created;
    }

    /**
     * Checks if this session is valid (hasn't expired).
     *
     * @return true if the session is valid, or
     *         false otherwise
     */
    public boolean isValid() {
        long now = System.currentTimeMillis();
        return now < destroyTime().getTime();
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
     * Sets the session user identifier if it was previously blank.
     * Once a session has been bound to a user, it cannot be bound to
     * another user (or reset to a blank user).
     *
     * @param userId         the new session user identifier
     *
     * @throws SecurityException if the session couldn't be bound to
     *     the specified user identifier
     */
    public void setUserId(String userId) {
        if (userId == null || userId.trim().length() <= 0) {
            String msg = "Attempt to re-bind HTTP session to blank user id";
            throw new SecurityException(msg);
        }
        userId = userId.trim();
        if (this.userId().length() > 0 && !this.userId().equals(userId)) {
            String msg = "Attempt to re-bind HTTP session from user '" +
                         this.userId() + "' to '" + userId + "'";
            throw new SecurityException(msg);
        }
        dict.set(KEY_USER, userId);
        modified = true;
    }

    /**
     * Returns the session creation timestamp.
     *
     * @return the session creation timestamp.
     */
    public Date createTime() {
        return dict.getDate(KEY_CREATE_TIME, new Date(0));
    }

    /**
     * Returns the scheduled session destruction timestamp.
     *
     * @return the session destruction timestamp.
     */
    public Date destroyTime() {
        return dict.getDate(KEY_DESTROY_TIME, new Date(0));
    }

    /**
     * Returns the session last access timestamp.
     *
     * @return the session last access timestamp.
     */
    public Date accessTime() {
        return dict.getDate(KEY_ACCESS_TIME, new Date(0));
    }

    /**
     * Updates the session last access timestamp to the current system time.
     */
    public void updateAccessTime() {
        long now = System.currentTimeMillis();
        dict.set(KEY_ACCESS_TIME, new Date(now));
        if (userId().length() > 0) {
            dict.set(KEY_DESTROY_TIME, new Date(now + EXPIRY_AUTH_MILLIS));
        } else {
            dict.set(KEY_DESTROY_TIME, new Date(now + EXPIRY_ANON_MILLIS));
        }
        modified = true;
    }

    /**
     * Returns the session source IP address. May be in either IPv4 or IPv6
     * format.
     *
     * @return the session source IP address.
     */
    public String ip() {
        return dict.getString(KEY_IP, "");
    }

    /**
     * Sets the session source IP address. May be in either IPv4 or IPv6
     * format.
     *
     * @param ip             the new session source IP address.
     */
    public void setIp(String ip) {
        dict.set(KEY_IP, ip);
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
     * Sets the session user agent string of the web browser.
     *
     * @param client         the session user agent string.
     */
    public void setClient(String client) {
        dict.set(KEY_CLIENT, client);
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
     * Validates this session. If the session isn't valid any more (expired),
     * a security exception is thrown.
     *
     * @throws SecurityException if the session didn't match the
     *     the provided values
     */
    public void validate() throws SecurityException {
        if (!isValid()) {
            throw new SecurityException("Session has expired");
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
