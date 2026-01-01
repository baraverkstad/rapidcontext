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

package org.rapidcontext.app.model;

import org.rapidcontext.core.ctx.ThreadContext;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.RootStorage;
import org.rapidcontext.core.type.Session;
import org.rapidcontext.core.type.User;

/**
 * The application root storage. This overlays some storage aliases
 * and performs additional path validation.
 *
 * @author Per Cederberg
 */
public class AppStorage extends RootStorage {

    /**
     * The current session path.
     */
    public static final Path SESSION_CURRENT = Path.resolve(Session.PATH, "@self");

    /**
     * The current user path.
     */
    public static final Path USER_CURRENT = Path.resolve(User.PATH, "@self");

    /**
     * Creates a new application storage.
     */
    public AppStorage() {
        super(true);
    }

    /**
     * Checks if a storage path is valid for read access. Only binary
     * paths or paths without file extensions are accepted.
     *
     * @param path           the path to check
     *
     * @return true if the path is valid, or false otherwise
     */
    public boolean isAccessible(Path path) {
        return !isObjectPath(path) || objectPath(path).equals(path);
    }

    /**
     * Searches for an object at the specified location and returns
     * metadata about the object if found. The path may locate either
     * an index or a specific object.
     *
     * @param path           the storage location
     *
     * @return the metadata for the object, or null if not found
     */
    @Override
    public synchronized Metadata lookup(Path path) {
        return isAccessible(path) ? super.lookup(redirect(path)) : null;
    }

    /**
     * Loads an object from the specified location. The path may
     * locate either an index or a specific object. In case of an
     * index, the data returned is an index dictionary listing of
     * all objects in it.
     *
     * @param path           the storage location
     *
     * @return the data read, or
     *         null if not found
     */
    @Override
    public synchronized Object load(Path path) {
        return isAccessible(path) ? super.load(redirect(path)) : null;
    }

    /**
     * Redirect a path if it matches one of the supported aliases.
     *
     * @param path           the storage location
     *
     * @return the possibly modified storage location
     */
    private Path redirect(Path path) {
        if (SESSION_CURRENT.equals(path)) {
            Session session = RequestContext.active().session();
            if (session != null) {
                return Path.resolve(Session.PATH, session.id());
            }
        } else if (USER_CURRENT.equals(path)) {
            User user = ThreadContext.active().user();
            if (user != null) {
                return Path.resolve(User.PATH, user.id());
            }
        }
        return path;
    }
}
