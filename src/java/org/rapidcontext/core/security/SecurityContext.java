/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2013 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.security;

import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.StorageException;
import org.rapidcontext.core.type.Role;
import org.rapidcontext.core.type.User;
import org.rapidcontext.util.BinaryUtil;

/**
 * The application security context. This class provides static
 * methods for authentication and resource authorization. It stores
 * the currently authenticated user in a thread-local storage, so
 * user credentials must be provided separately for each execution
 * thread. It is important that the manager is initialized before
 * any authentication calls are made, or they will fail.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class SecurityContext {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(SecurityContext.class.getName());

    /**
     * The data storage used for reading and writing configuration
     * data.
     */
    private static Storage dataStorage = null;

    /**
     * The currently authenticated users. This is a thread-local
     * variable containing one user object per thread. If the value
     * is set to null, no user is currently authenticated for the
     * thread.
     */
    private static ThreadLocal authUser = new ThreadLocal();

    /**
     * The cache of all user roles available. This array will be reset
     * on each call to init(), i.e. only when the system is reset.
     *
     * @see #init(Storage)
     */
    private static Role[] roleCache = null;

    /**
     * Initializes the security context. It can be called multiple
     * times in order to re-read the configuration data from the
     * data storage. The data store specified will be used for
     * reading and writing users and roles both during initialization
     * and later.
     *
     * @param storage        the data storage to use
     *
     * @throws StorageException if the storage couldn't be read or
     *             written
     */
    public static void init(Storage storage) throws StorageException {
        dataStorage = storage;
        int count = dataStorage.lookupAll(User.PATH).length;
        if (count <= 0) {
            LOG.info("creating default 'admin' user");
            User user = new User("admin");
            user.setName("Administrator User");
            user.setDescription("The default administrator user (automatically created).");
            user.setEnabled(true);
            user.setRoles(new String[] { "admin" });
            User.store(dataStorage, user);
        }
        roleCache = Role.findAll(dataStorage);
    }

    /**
     * Returns the currently authenticated user for this thread.
     *
     * @return the currently authenticated user, or
     *         null if no user is currently authenticated
     */
    public static User currentUser() {
        return (User) authUser.get();
    }

    /**
     * Checks if the currently authenticated user has internal access
     * to a storage path.
     *
     * @param path           the object storage path
     *
     * @return true if the current user has internal access, or
     *         false otherwise
     */
    public static boolean hasInternalAccess(String path) {
        return hasAccess(path, Role.PERM_INTERNAL);
    }

    /**
     * Checks if the currently authenticated user has read access to
     * a storage path.
     *
     * @param path           the object storage path
     *
     * @return true if the current user has read access, or
     *         false otherwise
     */
    public static boolean hasReadAccess(String path) {
        return hasAccess(path, Role.PERM_READ);
    }

    /**
     * Checks if the currently authenticated user has search access to
     * a storage path.
     *
     * @param path           the object storage path
     *
     * @return true if the current user has search access, or
     *         false otherwise
     */
    public static boolean hasSearchAccess(String path) {
        return hasAccess(path, Role.PERM_SEARCH);
    }

    /**
     * Checks if the currently authenticated user has write access to
     * a storage path.
     *
     * @param path           the object storage path
     *
     * @return true if the current user has write access, or
     *         false otherwise
     */
    public static boolean hasWriteAccess(String path) {
        return hasAccess(path, Role.PERM_WRITE);
    }

    /**
     * Checks if the currently authenticated user has has access
     * permission for a storage path.
     *
     * @param path           the object storage path
     * @param permission     the requested permission
     *
     * @return true if the current user has access, or
     *         false otherwise
     *
     * @see Role#hasAccess(String, String)
     */
    public static boolean hasAccess(String path, String permission) {
        User user = currentUser();
        path = StringUtils.removeStart(path, "/");
        permission = permission.toLowerCase().trim();
        for (int i = 0; i < roleCache.length; i++) {
            Role role = roleCache[i];
            if (role.hasUser(user) && role.hasAccess(path, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a unique number to be used once for hashing.
     *
     * @return the unique hash number
     */
    public static String nonce() {
        return String.valueOf(System.currentTimeMillis());
    }

    /**
     * Verifies that the specified nonce is sufficiently recently
     * generated to be acceptable.
     *
     * @param nonce          the nonce to check
     *
     * @throws SecurityException if the nonce was invalid
     */
    public static void verifyNonce(String nonce) throws SecurityException {
        try {
            long since = System.currentTimeMillis() - Long.parseLong(nonce);
            if (since > DateUtils.MILLIS_PER_MINUTE * 240) {
                LOG.info("stale authentication one-off number");
                throw new SecurityException("stale authentication one-off number");
            }
        } catch (NumberFormatException e) {
            LOG.info("invalid authentication one-off number");
            throw new SecurityException("invalid authentication one-off number");
        }
    }

    /**
     * Authenticates the specified user. This method will verify
     * that the user exists and is enabled. It should only be called
     * if a previous user authentication can be trusted, either via
     * a cookie, command-line login or similar. After a successful
     * authentication the current user will be set to the specified
     * user.
     *
     * @param id             the unique user id
     *
     * @throws SecurityException if the user failed authentication
     */
    public static void auth(String id) throws SecurityException {
        User    user = User.find(dataStorage, id);
        String  msg;

        if (user == null) {
            msg = "user " + id + " does not exist";
            LOG.info("failed authentication: " + msg);
            throw new SecurityException(msg);
        } else if (!user.isEnabled()) {
            msg = "user " + id + " is disabled";
            LOG.info("failed authentication: " + msg);
            throw new SecurityException(msg);
        }
        authUser.set(user);
    }

    /**
     * Authenticates the specified used with an MD5 two-step hash.
     * This method will verify that the user exists, is enabled and
     * that the password hash plus the specified suffix will MD5 hash
     * to the specified string, After a successful authentication the
     * current user will be set to the specified user.
     *
     * @param id             the unique user id
     * @param suffix         the user password hash suffix to append
     * @param hash           the expected hashed result
     *
     * @throws Exception if the user failed authentication
     */
    public static void authHash(String id, String suffix, String hash)
    throws Exception {

        User    user = User.find(dataStorage, id);
        String  test;
        String  msg;

        if (user == null) {
            msg = "user " + id + " does not exist";
            LOG.info("failed authentication: " + msg);
            throw new SecurityException(msg);
        } else if (!user.isEnabled()) {
            msg = "user " + id + " is disabled";
            LOG.info("failed authentication: " + msg);
            throw new SecurityException(msg);
        }
        test = BinaryUtil.hashMD5(user.getPasswordHash() + suffix);
        if (user.getPasswordHash().length() > 0 && !test.equals(hash)) {
            msg = "invalid password for user " + id;
            LOG.info("failed authentication: " + msg +
                     ", expected: " + test + ", received: " + hash);
            throw new SecurityException(msg);
        }
        authUser.set(user);
    }

    /**
     * Removes any previous authentication. I.e. the current user
     * will be reset to the anonymous user.
     */
    public static void authClear() {
        authUser.set(null);
    }
}
