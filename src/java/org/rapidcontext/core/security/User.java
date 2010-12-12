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

import java.util.logging.Logger;

import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.util.ArrayUtil;
import org.rapidcontext.util.BinaryUtil;

/**
 * An application user.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class User {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(User.class.getName());

    /**
     * The user data object.
     */
    private Dict data;

    /**
     * Creates a new user with the specified name.
     *
     * @param name           the unique user name
     */
    public User(String name) {
        this.data = new Dict();
        this.data.set("name", name);
    }

    /**
     * Creates a new user with the specified data object.
     *
     * @param data           the user data object
     */
    // TODO: Make this method package internal.
    public User(Dict data) {
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
     * Returns the user data object. Note that changes to the data
     * object will also affect this role object. Also note that the
     * data object contain the hashed password, which is unsuitable
     * to send to web clients.
     *
     * @return the user data object
     */
    public Dict getData() {
        return this.data;
    }

    /**
     * Returns the unique user name.
     *
     * @return the user name
     */
    public String getName() {
        return this.data.getString("name", null);
    }

    /**
     * Returns the user description.
     *
     * @return the user description
     */
    public String getDescription() {
        return this.data.getString("description", "");
    }

    /**
     * Sets the user description.
     *
     * @param descr          the user description
     */
    public void setDescription(String descr) {
        this.data.set("description", descr);
    }

    /**
     * Checks if the user is enabled.
     *
     * @return true if the user is enabled, or
     *         false otherwise
     */
    public boolean isEnabled() {
        return this.data.getBoolean("enabled", true);
    }

    /**
     * Sets the user enabled flag.
     *
     * @param enabled        the enabled flag
     */
    public void setEnabled(boolean enabled) {
        this.data.setBoolean("enabled", enabled);
    }

    /**
     * Returns the user password hash, encoded as a hexadecimal
     * string.
     *
     * @return the user password hash
     *
     * @see #setPassword(String)
     */
    public String getPasswordHash() {
        return this.data.getString("password", "");
    }

    /**
     * Sets the user password. This method will create a password
     * MD5 hash from the string "user:realm:password" and store that
     * result in the dictionary. This is an irreversible process, so
     * the original password cannot be retrieved once this is
     * performed.
     *
     * @param password       the user password
     */
    public void setPassword(String password) {
        String  str;

        str = getName() + ":" + SecurityContext.REALM + ":" + password;
        try {
            this.data.set("password", BinaryUtil.hashMD5(str));
        } catch (Exception e) {
            LOG.severe("failed to create MD5 password hash: " + e.getMessage());
        }
    }

    /**
     * Checks if the user has the specified role.
     *
     * @param name           the role name
     *
     * @return true if the user has the role, or
     *         false otherwise
     */
    public boolean hasRole(String name) {
        Array  roles = this.data.getArray("role");

        return roles != null && roles.containsValue(name);
    }

    /**
     * Returns an array with all the roles for the user.
     *
     * @return an array with all the roles
     */
    public String[] getRoles() {
        Array     roles = this.data.getArray("role");
        String[]  res;

        if (roles == null || roles.size() <= 0) {
            return new String[0];
        } else {
            res = new String[roles.size()];
            for (int i = 0; i < roles.size(); i++) {
                res[i] = roles.get(i).toString();
            }
            return res;
        }
    }

    /**
     * Sets all the all the roles for the user.
     *
     * @param roles          the array with all roles
     */
    public void setRoles(String[] roles) {
        Array   list = this.data.getArray("role");
        String  name;

        if (list == null) {
            list = new Array();
            this.data.set("role", list);
        }
        for (int i = 0; i < list.size(); i++) {
            if (ArrayUtil.indexOf(roles, list.getString(i, "")) < 0) {
                list.remove(i);
                i--;
            }
        }
        for (int i = 0; roles != null && i < roles.length; i++) {
            name = roles[i].trim();
            if (name.length() <= 0) {
                // Skip whitespace
            } else if (!list.containsValue(name)) {
                list.add(name);
            }
        }
    }
}
