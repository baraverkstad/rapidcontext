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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.util.ArrayUtil;

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
     * Checks if the specified password is correct. This method will
     * hash and encode the specified password, and compare the
     * result with the user password hash. If the user password hash
     * is blank, this method will always return true.
     *
     * @param password       the password to check
     *
     * @return true if the password is correct, or
     *         false otherwise
     */
    public boolean hasPassword(String password) {
        return hasPasswordHash(createPasswordHash(password));
    }

    /**
     * Sets the user password. This method will hash and encode the
     * specified password, which is an irreversible process.
     *
     * @param password       the user password
     */
    public void setPassword(String password) {
        this.data.set("password", createPasswordHash(password));
    }

    /**
     * Checks if the specified password hash is correct. This method
     * will compare the specified hash value with the user password
     * hash. If the user password hash is blank, this method will
     * always return true.
     *
     * @param hash           the password hash to check
     *
     * @return true if the password hash is correct, or
     *         false otherwise
     */
    public boolean hasPasswordHash(String hash) {
        String str = this.data.getString("password", "");
        return str.equals("") || str.equals(hash);
    }

    /**
     * Creates a password hash string (in ASCII). The hash value
     * calculation is irreversible, and is calculated with the
     * SHA-256 algorithm and encoded as a hexadecimal lower-case
     * ASCII string. The password will also be salted with the user
     * name, so that separate password cracking attempts must be
     * made for each user.
     *
     * @param password       the password text
     *
     * @return the hash value as a hexadecimal string
     */
    protected String createPasswordHash(String password) {
        StringBuffer   res = new StringBuffer();
        MessageDigest  digest;
        byte           bytes[];
        String         str;

        // Compute SHA-256 digest
        try {
            str = getName() + ":" + password;
            digest = MessageDigest.getInstance("SHA-256");
            digest.reset();
            digest.update(str.getBytes());
            bytes = digest.digest();
        } catch (NoSuchAlgorithmException e) {
            LOG.severe("failed to create SHA-256 password hash: " +
                       e.getMessage());
            return "";
        }

        // Encode digest as hex string
        for (int i = 0; i < bytes.length; i++) {
            str = Integer.toHexString(bytes[i] & 0xFF);
            if (str.length() < 2) {
                res.append("0");
            }
            res.append(str);
        }
        return res.toString();
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
