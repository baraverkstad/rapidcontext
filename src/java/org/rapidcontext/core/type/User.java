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

package org.rapidcontext.core.type;

import java.util.Date;
import java.util.logging.Logger;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.StorageException;
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
     * The dictionary key for the user email address.
     */
    public static final String KEY_EMAIL = "email";

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
     * The dictionary key for the authentication last modified timestamp.
     */
    public static final String KEY_ACCREDITED_TIME = "accreditedTime";

    /**
     * The dictionary key for the user settings dictionary.
     */
    public static final String KEY_SETTINGS = "settings";

    /**
     * The user object storage path.
     */
    public static final Path PATH = Path.from("/user/");

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
        Object obj = storage.load(Path.resolve(PATH, id));
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

        storage.store(user.path(), user);
    }

    /**
     * Normalizes a user data object if needed. This method will
     * modify legacy data into the proper keys and values.
     *
     * @param path           the storage location
     * @param dict           the storage data
     *
     * @return the storage data (possibly modified)
     */
    public static Dict normalize(Path path, Dict dict) {
        if (!dict.containsKey(KEY_TYPE)) {
            LOG.warning("deprecated: " + path + " data: missing object type");
            dict.set(KEY_TYPE, "user");
            dict.set(KEY_ID, path.toIdent(1));
            dict.set(KEY_NAME, dict.getString(KEY_DESCRIPTION, ""));
            dict.set(KEY_DESCRIPTION, "");
            Array list = new Array();
            for (Object o : dict.getArray(User.KEY_ROLE)) {
                list.add(o.toString().toLowerCase());
            }
            dict.set(User.KEY_ROLE, list);
        }
        return dict;
    }

    /**
     * Decodes a user authentication token. If the token isn't valid, the
     * missing parts will be filled with empty values.
     *
     * @param token          the token string
     *
     * @return the array of user id, expiry time and validation hash
     */
    public static String[] decodeAuthToken(String token) {
        String raw = new String(BinaryUtil.decodeBase64(token));
        String[] parts = raw.split(":", 3);
        if (parts.length != 3) {
            String[] copy = new String[3];
            copy[0] = (parts.length > 0) ? parts[0] : "";
            copy[1] = (parts.length > 1) ? parts[1] : "";
            copy[2] = (parts.length > 2) ? parts[2] : "";
            parts = copy;
        }
        if (parts[1].length() <= 0 || !StringUtils.isNumeric(parts[1])) {
            parts[1] = "0";
        }
        return parts;
    }

    /**
     * Encodes a user authentication token.
     *
     * @param id             the user id
     * @param expiry         the expire timestamp (in millis)
     * @param hash           the data validation hash
     *
     * @return the authentication token to be used for login
     */
    public static String encodeAuthToken(String id, long expiry, String hash) {
        String raw = id + ':' + expiry + ':' + hash;
        return BinaryUtil.encodeBase64(raw.getBytes());
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
        dict.set(KEY_EMAIL, email());
        dict.set(KEY_DESCRIPTION, description());
        dict.setBoolean(KEY_ENABLED, isEnabled());
        dict.set(KEY_REALM, realm());
        dict.set(KEY_PASSWORD, passwordHash());
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
        dict.set(KEY_EMAIL, email());
        dict.set(KEY_DESCRIPTION, description());
        dict.setBoolean(KEY_ENABLED, isEnabled());
        dict.set(KEY_REALM, realm());
        dict.set(KEY_PASSWORD, passwordHash());
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
     * Returns the user email address.
     *
     * @return the user email address.
     */
    public String email() {
        return dict.getString(KEY_EMAIL, "");
    }

    /**
     * Sets the user email address.
     *
     * @param email          the user email address
     */
    public void setEmail(String email) {
        dict.set(KEY_EMAIL, email);
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
     * string. Avoid using this method to verify the current user
     * password, since it may be blank (any password) or the user
     * might be disabled. Use verifyPasswordHash() instead.
     *
     * @return the user password hash
     *
     * @see #verifyPasswordHash(String)
     */
    public String passwordHash() {
        return dict.getString(KEY_PASSWORD, "");
    }

    /**
     * Sets the user password MD5 hash. The password hash should be
     * created from the string "id:realm:password" and converted to a
     * lower-case hexadecimal string before being sent to this
     * method.
     *
     * @param passwordHash   the new user password MD5 hash
     *
     * @see #setPassword(String)
     */
    public void setPasswordHash(String passwordHash) {
        dict.set(KEY_PASSWORD, passwordHash.toLowerCase());
        dict.set(KEY_ACCREDITED_TIME, new Date());
    }

    /**
     * Sets the user password. This method will create a password
     * MD5 hash from the string "id:realm:password" and store that
     * result in the password field. This is an irreversible process,
     * so the original password cannot be retrieved from the object.
     *
     * @param password       the new user password (in clear text)
     *
     * @see #setPasswordHash(String)
     */
    public void setPassword(String password) {
        try {
            String str = id() + ":" + realm() + ":" + password;
            setPasswordHash(BinaryUtil.hashMD5(str));
        } catch (Exception e) {
            LOG.severe("failed to create password hash: " + e.getMessage());
        }
    }

    /**
     * Verifies that the specified password MD5 hash is a match. This
     * method checks that the user is enabled and that the current
     * user password hash is identical to the specified one. If the
     * current password hash is blank, this method will also return
     * true.
     *
     * @param passwordHash   the password hash to check
     *
     * @return true if the password hashes are identical, or
     *         false otherwise
     */
    public boolean verifyPasswordHash(String passwordHash) {
        String hash = passwordHash();
        return isEnabled() && (hash.length() == 0 || hash.equals(passwordHash));
    }

    /**
     * Creates an authentication token for this user. The token contains the
     * user id, an expire timestamp and a validation hash containing both
     * these values and the current user password. The authentication token
     * can be used for password recovery via email or some other out-of-band
     * delivery mechanism.
     *
     * @param expiryTime     the authentication token expire time (in millis)
     *
     * @return the authentication token
     */
    public String createAuthToken(long expiryTime) {
        try {
            String str = id() + ":" + expiryTime + ":" + passwordHash();
            String hash = BinaryUtil.hashSHA256(str);
            return encodeAuthToken(id(), expiryTime, hash);
        } catch (Exception e) {
            LOG.severe("failed to create auth token: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verifies that the specified authentication token is valid for this user.
     *
     * @param token          the authentication token
     *
     * @return true if the token is valid, or
     *         false otherwise
     */
    public boolean verifyAuthToken(String token) {
        String[]  parts = User.decodeAuthToken(token);
        long      expiry = Long.parseLong(parts[1]);
        boolean   isExpired = expiry < System.currentTimeMillis();

        return isEnabled() && !isExpired && createAuthToken(expiry).equals(token);
    }

    /**
     * Checks if the user has the specified role. Note that this method
     * doesn't check for automatic roles.
     *
     * @param name           the role name
     *
     * @return true if the user has the role, or
     *         false otherwise
     *
     * @see Role#hasUser(User)
     */
    public boolean hasRole(String name) {
        for (Object o : dict.getArray(KEY_ROLE)) {
            if (name.equalsIgnoreCase(o.toString())) {
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
        return dict.getArray(KEY_ROLE).values(ArrayUtils.EMPTY_STRING_ARRAY);
    }

    /**
     * Sets all the all the roles for the user.
     *
     * @param roles          the array with all roles
     */
    public void setRoles(String[] roles) {
        Array list = dict.getArray(KEY_ROLE);
        for (int i = 0; i < list.size(); i++) {
            if (ArrayUtils.indexOf(roles, list.getString(i, "")) < 0) {
                list.remove(i);
                i--;
            }
        }
        if (roles != null) {
            for (String name : roles) {
                name = name.trim();
                if (name.length() > 0 && !list.containsValue(name)) {
                    list.add(name);
                }
            }
        }
        dict.set(KEY_ROLE, list);
    }

    /**
     * Returns the authentication last modified timestamp.
     *
     * @return the authentication last modified timestamp
     */
    public Date accreditedTime() {
        return dict.getDate(KEY_ACCREDITED_TIME, new Date(0));
    }

    /**
     * Returns the user settings dictionary.
     *
     * @return a dictionary with user settings, or
     *         a new empty dictionary if not set
     */
    public Dict settings() {
        return dict.getDict(KEY_SETTINGS);
    }

    /**
     * Merges updates into the user settings dictionary. Keys with null values
     * will be removed from settings and other keys will be overwritten. Any
     * key not listed in the updates will remain unmodified.
     *
     * @param updates        the dictionary with updates
     */
    public void updateSettings(Dict updates) {
        Dict settings = this.settings();
        for (String key : updates.keys()) {
            Object val = updates.get(key);
            if (val == null) {
                settings.remove(key);
            } else {
                settings.set(key, val);
            }
        }
        if (settings.size() > 0) {
            dict.set(KEY_SETTINGS, settings);
        } else {
            dict.remove(KEY_SETTINGS);
        }
    }
}
