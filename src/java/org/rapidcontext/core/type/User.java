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

import java.util.logging.Logger;

import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.StorageException;
import org.rapidcontext.util.ArrayUtil;
import org.rapidcontext.util.BinaryUtil;

/**
 * A system user.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class User extends StorableObject {

    /**
     * The class logger.
     */
    private static final Logger LOG = Logger.getLogger(User.class.getName());

    /**
     * The default user realm.
     */
    public static final String DEFAULT_REALM = "RapidContext";

    /**
     * The dictionary key for the user name.
     */
    public static final String KEY_NAME = "name";

    /**
     * The dictionary key for the user description.
     */
    public static final String KEY_DESCRIPTION = "description";

    /**
     * The dictionary key for the user enabled flag.
     */
    public static final String KEY_ENABLED = "enabled";

    /**
     * The dictionary key for the user realm.
     */
    public static final String KEY_REALM = "realm";

    /**
     * The dictionary key for the user password hash.
     */
    public static final String KEY_PASSWORD = "password";

    /**
     * The dictionary key for the user role array.
     */
    public static final String KEY_ROLE = "role";

    /**
     * The user object storage path.
     */
    public static final Path PATH = new Path("/user/");

    /**
     * Searches for a specific user in the storage.
     *
     * @param storage        the storage to search in
     * @param id             the user identifier
     *
     * @return the user found, or
     *         null if not found
     */
    public static User find(Storage storage, String id) {
        Object obj = storage.load(PATH.descendant(new Path(id)));
        return (obj instanceof User) ? (User) obj : null;
    }

    /**
     * Stores the specified used in the provided storage.
     *
     * @param storage        the storage to use
     * @param user           the user to store
     *
     * @throws StorageException if the user couldn't be stored
     */
    public static void store(Storage storage, User user)
        throws StorageException {

        storage.store(PATH.child(user.id(), false), user);
    }

    /**
     * Creates a new user from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public User(String id, String type, Dict dict) {
        super(id, type, dict);
        dict.set(KEY_NAME, name());
        dict.set(KEY_DESCRIPTION, description());
        dict.setBoolean(KEY_ENABLED, isEnabled());
        dict.set(KEY_REALM, realm());
        dict.set(KEY_PASSWORD, getPasswordHash());
    }

    /**
     * Creates a new user with the specified user identifier. The
     * user will be created with a blank password.
     *
     * @param id             the user identifier
     */
    public User(String id) {
        super(id, "user");
        dict.set(KEY_NAME, name());
        dict.set(KEY_DESCRIPTION, description());
        dict.setBoolean(KEY_ENABLED, isEnabled());
        dict.set(KEY_REALM, realm());
        dict.set(KEY_PASSWORD, getPasswordHash());
    }

    /**
     * Returns the user name.
     *
     * @return the user name.
     */
    public String name() {
        return dict.getString(KEY_NAME, "");
    }

    /**
     * Sets the user name.
     *
     * @param name           the user full name
     */
    public void setName(String name) {
        dict.set(KEY_NAME, name);
    }

    /**
     * Returns the user description.
     *
     * @return the user description.
     */
    public String description() {
        return dict.getString(KEY_DESCRIPTION, "");
    }

    /**
     * Sets the user description.
     *
     * @param descr          the user description
     */
    public void setDescription(String descr) {
        dict.set(KEY_DESCRIPTION, descr);
    }

    /**
     * Checks if the user is enabled.
     *
     * @return true if the user is enabled, or
     *         false otherwise
     */
    public boolean isEnabled() {
        return dict.getBoolean(KEY_ENABLED, true);
    }

    /**
     * Sets the user enabled flag.
     *
     * @param enabled        the enabled flag
     */
    public void setEnabled(boolean enabled) {
        dict.setBoolean(KEY_ENABLED, enabled);
    }

    /**
     * Returns the user realm.
     *
     * @return the user realm.
     */
    public String realm() {
        return dict.getString(KEY_REALM, DEFAULT_REALM);
    }

    /**
     * Sets the user realm. Note that this method will make the old
     * password impossible to use, since the password hash contains
     * the old realm name. A new password has should be calculated.
     *
     * @param realm          the new user realm
     */
    public void setRealm(String realm) {
        dict.set(KEY_REALM, realm);
    }

    /**
     * Returns the user password MD5 hash, encoded as a hexadecimal
     * string.
     *
     * @return the user password hash
     *
     * @see #setPassword(String)
     */
    public String getPasswordHash() {
        return dict.getString(KEY_PASSWORD, "");
    }

    /**
     * Sets the user password. This method will create a password
     * MD5 hash from the string "id:realm:password" and store that
     * result in the password field. This is an irreversible process,
     * so the original password cannot be retrieved from the object.
     *
     * @param password       the new user password (in clear text)
     */
    public void setPassword(String password) {
        String str = id() + ":" + realm() + ":" + password;
        try {
            dict.set(KEY_PASSWORD, BinaryUtil.hashMD5(str));
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
        Array roles = dict.getArray(KEY_ROLE);
        for (int i = 0; roles != null && i < roles.size(); i++) {
            if (name.equalsIgnoreCase(roles.getString(i, ""))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an array with all the roles for the user.
     *
     * @return an array with all the roles
     */
    public String[] roles() {
        Array roles = dict.getArray(KEY_ROLE);
        if (roles == null || roles.size() <= 0) {
            return new String[0];
        } else {
            String[] res = new String[roles.size()];
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
        Array list = dict.getArray(KEY_ROLE);
        if (list == null) {
            list = new Array();
            dict.set(KEY_ROLE, list);
        }
        for (int i = 0; i < list.size(); i++) {
            if (ArrayUtil.indexOf(roles, list.getString(i, "")) < 0) {
                list.remove(i);
                i--;
            }
        }
        for (int i = 0; roles != null && i < roles.length; i++) {
            String name = roles[i].trim();
            if (name.length() <= 0) {
                // Skip whitespace
            } else if (!list.containsValue(name)) {
                list.add(name);
            }
        }
    }
}
