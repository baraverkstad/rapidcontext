/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2025 Per Cederberg. All rights reserved.
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
import org.apache.commons.lang3.time.DateUtils;
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
     * The dictionary key for the oldest valid authentication timestamp.
     */
    public static final String KEY_AUTHORIZED_TIME = "authorizedTime" ;

    /**
     * The dictionary key for the user settings dictionary.
     */
    public static final String KEY_SETTINGS = "settings";

    /**
     * The user object storage path.
     */
    public static final Path PATH = Path.from("/user/");

    /**
     * The default active user time (5 minutes).
     */
    public static final long ACTIVE_MILLIS = 5L * DateUtils.MILLIS_PER_MINUTE;

    /**
     * The default user data (copied for new users).
     */
    private static final Dict DEFAULTS = new Dict()
        .set(KEY_ID, "")
        .set(KEY_TYPE, "user")
        .set(KEY_NAME, "")
        .set(KEY_EMAIL, "")
        .set(KEY_DESCRIPTION, "")
        .set(KEY_ENABLED, true)
        .set(KEY_REALM, DEFAULT_REALM)
        .set(PREFIX_HIDDEN + KEY_PASSWORD, "")
        .set(KEY_ROLE, new Array());

    /**
     * The shared request metrics for all users.
     */
    private static Metrics metrics = null;

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
        return storage.load(Path.resolve(PATH, id), User.class);
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
     * Returns the user request metrics. The metrics will be loaded
     * from storage if not already in memory.
     *
     * @param storage        the storage to load from
     *
     * @return the user request metrics
     */
    public static Metrics metrics(Storage storage) {
        if (metrics == null) {
            metrics = Metrics.findOrCreate(storage, "user");
        }
        return metrics;
    }

    /**
     * Reports user request metrics for a single request.
     *
     * @param start          the start time (in millis)
     * @param success        the success flag
     * @param error          the optional error message
     */
    public static void report(User user, long start, boolean success, String error) {
        if (metrics != null) {
            String id = (user == null) ? "anonymous" : user.id();
            long now = System.currentTimeMillis();
            int duration = (int) (now - start);
            metrics.report(id, now, 1, duration, success, error);
        }
    }

    /**
     * Normalizes a user data object if needed. This method will
     * modify legacy data into the proper keys and values.
     *
     * @param id             the object identifier
     * @param dict           the storage data
     *
     * @return the storage data (possibly modified)
     */
    public static Dict normalize(String id, Dict dict) {
        if (!dict.containsKey(KEY_TYPE)) {
            LOG.warning("deprecated: user " + id + " data: missing 'type' property");
            dict.set(KEY_TYPE, "user");
            dict.set(KEY_ID, id);
            dict.set(KEY_NAME, dict.get(KEY_DESCRIPTION, String.class, ""));
            dict.set(KEY_DESCRIPTION, "");
            dict.set(KEY_ROLE, Array.from(
                dict.getArray(KEY_ROLE).stream()
                .map(o -> o.toString().toLowerCase())
            ));
        }
        if (dict.containsKey(KEY_PASSWORD)) {
            LOG.warning("deprecated: user " + id + " data: password not hidden");
            String pwd = dict.get(KEY_PASSWORD, String.class, "");
            dict.remove(KEY_PASSWORD);
            dict.set(PREFIX_HIDDEN + KEY_PASSWORD, pwd);
        }
        if (!dict.containsKey(KEY_EMAIL) || !dict.containsKey(KEY_ROLE)) {
            Dict copy = DEFAULTS.copy();
            copy.setAll(dict);
            dict = copy;
        }
        if (!dict.containsKey(KEY_AUTHORIZED_TIME)) {
            dict.set(KEY_AUTHORIZED_TIME, new Date(0));
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
        if (parts[1].isBlank() || !StringUtils.isNumeric(parts[1])) {
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
        super(id, type, normalize(id, dict));
    }

    /**
     * Creates a new user with the specified user identifier. The
     * user will be created with a blank password.
     *
     * @param id             the user identifier
     */
    public User(String id) {
        this(id, "user", DEFAULTS.copy().set(KEY_ID, id));
    }

    /**
     * Checks if this object is in active use. This method will return
     * true if the object was activated during the last 5 minutes.
     *
     * @return true if the object is considered active, or
     *         false otherwise
     */
    @Override
    protected boolean isActive() {
        return System.currentTimeMillis() - activatedTime().getTime() <= ACTIVE_MILLIS;
    }

    /**
     * Returns the user name.
     *
     * @return the user name.
     */
    public String name() {
        return dict.get(KEY_NAME, String.class, "");
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
        return dict.get(KEY_EMAIL, String.class, "");
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
        return dict.get(KEY_DESCRIPTION, String.class, "");
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
        return dict.get(KEY_ENABLED, Boolean.class, true);
    }

    /**
     * Sets the user enabled flag.
     *
     * @param enabled        the enabled flag
     */
    public void setEnabled(boolean enabled) {
        if (isEnabled() != enabled) {
            dict.set(KEY_ENABLED, enabled);
            dict.set(KEY_AUTHORIZED_TIME, new Date());
        }
    }

    /**
     * Returns the user realm.
     *
     * @return the user realm.
     */
    public String realm() {
        return dict.get(KEY_REALM, String.class, DEFAULT_REALM);
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
        return dict.get(PREFIX_HIDDEN + KEY_PASSWORD, String.class, "");
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
        dict.set(PREFIX_HIDDEN + KEY_PASSWORD, passwordHash.toLowerCase());
        dict.set(KEY_AUTHORIZED_TIME, new Date());
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
        return isEnabled() && (hash.isBlank() || hash.equals(passwordHash));
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
            if (ArrayUtils.indexOf(roles, list.get(i, String.class, "")) < 0) {
                list.remove(i);
                i--;
            }
        }
        if (roles != null) {
            for (String name : roles) {
                name = name.trim();
                if (!name.isBlank() && !list.containsValue(name)) {
                    list.add(name);
                }
            }
        }
        dict.set(KEY_ROLE, list);
    }

    /**
     * Returns the oldest valid authentication timestamp. Any session,
     * auth token or similar created prior is considered invalid.
     *
     * @return the oldest valid authentication timestamp
     */
    public Date authorizedTime() {
        return dict.get(KEY_AUTHORIZED_TIME, Date.class, new Date(0));
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
