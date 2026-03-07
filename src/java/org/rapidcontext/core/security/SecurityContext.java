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

package org.rapidcontext.core.security;

import java.util.logging.Logger;

import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.time.DateUtils;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.StorageException;
import org.rapidcontext.core.type.Role;
import org.rapidcontext.core.type.User;

/**
 * The application security context. Manages the role cache for
 * access control checks and provides nonce generation for
 * authentication challenges. Must be initialized via init() before
 * use.
 *
 * @author Per Cederberg
 */
public final class SecurityContext {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(SecurityContext.class.getName());

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
     * data storage.
     *
     * @param storage        the data storage to use
     *
     * @throws StorageException if the storage couldn't be read or
     *             written
     */
    public static void init(Storage storage) throws StorageException {
        long count = storage.query(User.PATH).paths().count();
        if (count <= 0) {
            LOG.info("creating default 'admin' user");
            User user = new User("admin");
            user.setName("Administrator User");
            user.setDescription("The default administrator user (automatically created).");
            user.setEnabled(true);
            user.setRoles(new String[] { "admin" });
            User.store(storage, user);
        }
        roleCache = Role.all(storage).toArray(Role[]::new);
    }

    /**
     * Checks if the specified user has has access permission for a
     * storage path.
     *
     * @param user           the user to check, or null or anonymous
     * @param path           the object storage path
     * @param via            the caller path, or null to use context
     * @param permission     the requested permission
     *
     * @return true if the current user has access, or
     *         false otherwise
     *
     * @see Role#hasAccess(String, String, String)
     */
    public static boolean hasAccess(User user, String path, String via, String permission) {
        path = Strings.CS.removeStart(path, "/");
        permission = permission.toLowerCase().trim();
        for (Role role : roleCache) {
            if (role.hasUser(user) && role.hasAccess(path, via, permission)) {
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
            if (since > 4 * DateUtils.MILLIS_PER_HOUR) {
                LOG.info("stale authentication one-off number");
                throw new SecurityException("stale authentication one-off number");
            }
        } catch (NumberFormatException e) {
            LOG.info("invalid authentication one-off number");
            throw new SecurityException("invalid authentication one-off number");
        }
    }

    // No instances
    private SecurityContext() {}
}
