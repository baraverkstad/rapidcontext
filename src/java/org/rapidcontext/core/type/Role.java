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

import java.util.ArrayList;

import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;

/**
 * A user access role. Each role may contain an access rule list for
 * declaring which objects that the role provides access to.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Role extends StorableObject {

    /**
     * The dictionary key for the role name.
     */
    public static final String KEY_NAME = "name";

    /**
     * The dictionary key for the role description.
     */
    public static final String KEY_DESCRIPTION = "description";

    /**
     * The dictionary key for the role access array. The value stored
     * is an array of access rules.
     */
    public static final String KEY_ACCESS = "access";

    /**
     * The role object storage path.
     */
    public static final Path PATH = new Path("/role/");

    /**
     * Searches for a specific role in the storage. If the
     * case-insensitive search flag is set, a fallback search for the
     * lower-case name will be performed.
     *
     * @param storage        the storage to search in
     * @param id             the role identifier
     * @param search         the search case-insensitive flag
     *
     * @return the role found, or
     *         null if not found
     */
    public static Role find(Storage storage, String id, boolean search) {
        Object obj = storage.load(PATH.descendant(new Path(id)));
        if (obj == null && search && !id.equals(id.toLowerCase())) {
            obj = storage.load(PATH.descendant(new Path(id.toLowerCase())));
        }
        return (obj instanceof Role) ? (Role) obj : null;
    }

    /**
     * Searches for all roles in the storage.
     *
     * @param storage        the storage to search in
     *
     * @return an array of all roles found
     */
    public static Role[] findAll(Storage storage) {
        Object[]   objs = storage.loadAll(PATH);
        ArrayList  list = new ArrayList(objs.length);

        for (int i = 0; i < objs.length; i++) {
            if (objs[i] instanceof Role) {
                list.add(objs[i]);
            }
        }
        return (Role[]) list.toArray(new Role[list.size()]);
    }

    /**
     * Creates a new role from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public Role(String id, String type, Dict dict) {
        super(id, type, dict);
        dict.set(KEY_NAME, name());
        dict.set(KEY_DESCRIPTION, description());
    }

    /**
     * Returns the role name.
     *
     * @return the role name.
     */
    public String name() {
        return dict.getString(KEY_NAME, "");
    }

    /**
     * Returns the role description.
     *
     * @return the role description.
     */
    public String description() {
        return dict.getString(KEY_DESCRIPTION, "");
    }

    /**
     * Checks if the role has access to an object. The access list is
     * processed from top to bottom to find a matching entry. Once
     * found, the value of the "allow" boolean property will be
     * returned (defaults to true). The object matching is based on
     * "type", "name" (or "regexp") and optionally a "caller" (i.e. a
     * calling procedure).
     *
     * @param type           the object type
     * @param name           the object name
     * @param caller         the caller procedure, or null for none
     *
     * @return true if the user has access, or
     *         false otherwise
     */
    public boolean hasAccess(String type, String name, String caller) {
        Array  list = dict.getArray("access");
        Dict   match;

        if (list == null) {
            // TODO: return true if this is the admin role
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            match = list.getDict(i);
            if (matches(match, type, name, caller)) {
                return match.getBoolean("allow", true);
            }
        }
        return false;
    }

    /**
     * Checks if the access data matches the specified values.
     *
     * @param m              the access data
     * @param type           the object type
     * @param name           the object name
     * @param caller         the caller procedure, or null for none
     *
     * @return true if the data matches, or
     *         false otherwise
     */
    private boolean matches(Dict m, String type, String name, String caller) {
        String  str;

        if (!m.getString("type", "").equals(type)) {
            return false;
        }
        str = m.getString("regexp", null);
        if (str != null && !name.matches("^" + str + "$")) {
            return false;
        }
        str = m.getString("name", null);
        if (str != null && !str.equals(name)) {
            return false;
        }
        str = m.getString("caller", null);
        if (str != null && (caller == null || !caller.matches("^" + str + "$"))) {
            return false;
        }
        return true;
    }
}
