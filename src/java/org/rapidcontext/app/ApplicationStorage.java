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

package org.rapidcontext.app;

import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.RootStorage;
import org.rapidcontext.core.type.Session;
import org.rapidcontext.core.type.User;

/**
 * The application root storage. This overlays a number of storage aliases to
 * fetch current session and user.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class ApplicationStorage extends RootStorage {

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
    public ApplicationStorage() {
        super(true);
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
    public Metadata lookup(Path path) {
        return super.lookup(redirect(path));
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
    public Object load(Path path) {
        return super.load(redirect(path));
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
            Session session = Session.activeSession.get();
            if (session != null) {
                return Path.resolve(Session.PATH, session.id());
            }
        } else if (USER_CURRENT.equals(path)) {
            User user = SecurityContext.currentUser();
            if (user != null) {
                return Path.resolve(User.PATH, user.id());
            }
        }
        return path;
    }
}
