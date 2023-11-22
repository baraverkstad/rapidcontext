/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2023 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.security;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
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
public final class SecurityContext {

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
    private static ThreadLocal<User> authUser = new ThreadLocal<>();

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
        long count = dataStorage.query(User.PATH).paths().count();
        if (count <= 0) {
            LOG.info("creating default 'admin' user");
            User user = new User("admin");
            user.setName("Administrator User");
            user.setDescription("The default administrator user (automatically created).");
            user.setEnabled(true);
            user.setRoles(new String[] { "admin" });
            User.store(dataStorage, user);
        }
        roleCache = Role.all(dataStorage).toArray(Role[]::new);
    }

    /**
     * Returns the currently authenticated user for this thread.
     *
     * @return the currently authenticated user, or
     *         null if no user is currently authenticated
     */
    public static User currentUser() {
        return authUser.get();
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
        return hasAccess(currentUser(), path, Role.PERM_INTERNAL);
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
        return hasAccess(currentUser(), path, Role.PERM_READ);
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
        return hasAccess(currentUser(), path, Role.PERM_SEARCH);
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
        return hasAccess(currentUser(), path, Role.PERM_WRITE);
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
        return hasAccess(currentUser(), path, permission);
    }

    /**
     * Checks if the specified user has has access permission for a
     * storage path.
     *
     * @param user           the user to check, or null or anonymous
     * @param path           the object storage path
     * @param permission     the requested permission
     *
     * @return true if the current user has access, or
     *         false otherwise
     *
     * @see Role#hasAccess(String, String)
     */
    public static boolean hasAccess(User user, String path, String permission) {
        path = StringUtils.removeStart(path, "/");
        permission = permission.toLowerCase().trim();
        for (Role role : roleCache) {
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
     * @return the authenticated user, same as currentUser()
     *
     * @throws SecurityException if the user failed authentication
     */
    public static User auth(String id) throws SecurityException {
        User user = User.find(dataStorage, id);
        if (user == null) {
            String msg = "user " + id + " does not exist";
            LOG.info("failed authentication: " + msg);
            throw new SecurityException(msg);
        } else if (!user.isEnabled()) {
            String msg = "user " + id + " is disabled";
            LOG.info("failed authentication: " + msg);
            throw new SecurityException(msg);
        }
        authUser.set(user);
        return user;
    }

    /**
     * Authenticates the specified user with an MD5 two-step hash.
     * This method will verify that the user exists, is enabled and
     * that the password hash plus the specified suffix will MD5 hash
     * to the specified string, After a successful authentication the
     * current user will be set to the specified user.
     *
     * @param id             the unique user id
     * @param suffix         the user password hash suffix to append
     * @param hash           the expected hashed result
     *
     * @return the authenticated user
     *
     * @throws SecurityException if the authentication failed
     */
    public static User authHash(String id, String suffix, String hash)
    throws SecurityException {

        User user = User.find(dataStorage, id);
        if (user == null) {
            String msg = "user " + id + " does not exist";
            LOG.info("failed authentication: " + msg);
            throw new SecurityException(msg);
        } else if (!user.isEnabled()) {
            String msg = "user " + id + " is disabled";
            LOG.info("failed authentication: " + msg);
            throw new SecurityException(msg);
        }
        try {
            String test = BinaryUtil.hashMD5(user.passwordHash() + suffix);
            if (user.passwordHash().length() > 0 && !test.equals(hash)) {
                String msg = "invalid password for user " + id;
                LOG.info("failed authentication: " + msg +
                         ", expected: " + test + ", received: " + hash);
                throw new SecurityException(msg);
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            String msg = "invalid environment, MD5 not supported";
            LOG.log(Level.SEVERE, msg, e);
            throw new SecurityException(msg, e);
        }
        authUser.set(user);
        return user;
    }

    /**
     * Authenticates with a user authentication token. This method
     * will verify that the user exists, is enabled and that the
     * token is valid for the current user password. After a
     * successful authentication the current user will be set to the
     * user in the token.
     *
     * @param token          the authentication token
     *
     * @return the authenticated user
     *
     * @throws Exception if the authentication failed
     */
    public static User authToken(String token) throws Exception {
        String[] parts = User.decodeAuthToken(token);
        User user = User.find(dataStorage, parts[0]);
        long expiry = Long.parseLong(parts[1]);
        if (user == null) {
            String msg = "user " + parts[0] + " does not exist";
            LOG.info("failed authentication: " + msg);
            throw new SecurityException(msg);
        } else if (!user.isEnabled()) {
            String msg = "user " + user.id() + " is disabled";
            LOG.info("failed authentication: " + msg);
            throw new SecurityException(msg);
        } else if (expiry < System.currentTimeMillis()) {
            String msg = "token has expired";
            LOG.info("failed authentication: " + msg);
            throw new SecurityException(msg);
        } else if (!user.verifyAuthToken(token)) {
            String msg = "invalid auth token for user " + user.id();
            LOG.info("failed token authentication: " + msg +
                     ", expected: " + user.createAuthToken(expiry) +
                     ", received: " + token);
            throw new SecurityException(msg);
        }
        authUser.set(user);
        return user;
    }

    /**
     * Deauthenticates this context, i.e. the current user will be
     * reset to the anonymous user.
     */
    public static void deauth() {
        authUser.remove();
    }

    // No instances
    private SecurityContext() {}
}
