/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg & Dynabyte AB.
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

package org.rapidcontext.core.security;

import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;

/**
 * A user security role. Each role is identified by a unique name.
 * A special role "Admin" is reserved for providing users with full
 * access to all sensitive system calls. Otherwise each role also
 * contains an access list for all objects that the role provides
 * read access to. The access list is processed from top to bottom
 * to find a matching entry. Once found, the value of the "allow"
 * boolean property will be returned (defaults to true). The object
 * matching is based on "type", "name" (or "regexp") and optionally
 * a "caller" (i.e. a calling procedure).
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Role {

    /**
     * The user dictionary object.
     */
    private Dict data;

    /**
     * Creates a new role with the specified name.
     *
     * @param name           the unique role name
     */
    public Role(String name) {
        this.data = new Dict();
        this.data.set("name", name);
    }

    /**
     * Creates a new role with the specified data object.
     *
     * @param data           the role data object
     */
    // TODO: Make this method package internal.
    public Role(Dict data) {
        this.data = data;
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    public String toString() {
        return getName();
    }

    /**
     * Returns the role dictionary. Note that changes to the
     * dictionary will also affect this role object.
     *
     * @return the role data object
     */
    public Dict getData() {
        return this.data;
    }

    /**
     * Returns the unique role name.
     *
     * @return the role name
     */
    public String getName() {
        return this.data.getString("name", null);
    }

    /**
     * Returns the role description.
     *
     * @return the role description
     */
    public String getDescription() {
        return this.data.getString("description", "");
    }

    /**
     * Sets the role description.
     *
     * @param descr          the role description
     */
    public void setDescription(String descr) {
        this.data.set("description", descr);
    }

    /**
     * Checks if the role has access to an object.
     *
     * @param type           the object type
     * @param name           the object name
     * @param caller         the caller procedure, or null for none
     *
     * @return true if the user has access, or
     *         false otherwise
     */
    public boolean hasAccess(String type, String name, String caller) {
        Array  list = this.data.getArray("access");
        Dict   match;

        if (list == null) {
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
