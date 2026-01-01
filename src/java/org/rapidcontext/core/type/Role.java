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

package org.rapidcontext.core.type;

import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.Strings;
import org.rapidcontext.core.ctx.Context;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.util.RegexUtil;

/**
 * A user access role. Each role contains an access rule list for
 * declaring which objects that the role provides access to.
 *
 * @author Per Cederberg
 */
public class Role extends StorableObject {

    /**
     * The class logger.
     */
    private static final Logger LOG = Logger.getLogger(Role.class.getName());

    /**
     * The dictionary key for the role name.
     */
    public static final String KEY_NAME = "name";

    /**
     * The dictionary key for the role description.
     */
    public static final String KEY_DESCRIPTION = "description";

    /**
     * The dictionary key for automatic user match.
     */
    public static final String KEY_AUTO = "auto";

    /**
     * The dictionary key for the role access array. The value stored
     * is an array of access rules.
     */
    public static final String KEY_ACCESS = "access";

    /**
     * The dictionary key for the path in the access dictionary. The
     * value stored is an absolute path to an object, with optional
     * glob characters ('*', '**' or '?').
     */
    public static final String ACCESS_PATH = "path";

    /**
     * The dictionary key for the regex path in the access
     * dictionary. The value stored is a regular expression matching
     * an absolute path to an object (without leading '/' chars).
     */
    public static final String ACCESS_REGEX = "regex";

    /**
     * The dictionary key for the caller path in the access dictionary.
     * The value stored is a glob pattern for matching a storage object
     * path.
     */
    public static final String ACCESS_VIA = "via";

    /**
     * The dictionary key for the permission list in the access
     * dictionary. The value stored is a string with permissions
     * separated by comma (',').
     *
     * @see #PERM_NONE
     * @see #PERM_INTERNAL
     * @see #PERM_READ
     * @see #PERM_SEARCH
     * @see #PERM_WRITE
     * @see #PERM_ALL
     */
    public static final String ACCESS_PERMISSION = "permission";

    /**
     * The permission key for no access.
     */
    public static final String PERM_NONE = "none";

    /**
     * The permission key for internal access.
     *
     * @deprecated Internal access is now achieved by combining
     *     {@link #PERM_READ} with {@link #ACCESS_VIA} "procedure/**"
     *     (or preferably a more restricted pattern).
     */
    @Deprecated(forRemoval = true)
    public static final String PERM_INTERNAL = "internal";

    /**
     * The permission key for read access.
     */
    public static final String PERM_READ = "read";

    /**
     * The permission key for search access.
     */
    public static final String PERM_SEARCH = "search";

    /**
     * The permission key for write access.
     */
    public static final String PERM_WRITE = "write";

    /**
     * The permission key for full access.
     */
    public static final String PERM_ALL = "all";

    /**
     * The via pattern for non-internal procedures. Translates to
     * proper regex in {@link #initViaRegex(Dict)}.
     */
    static final String VIA_NON_INTERNAL = "non-internal-procedures";

    /**
     * The role object storage path.
     */
    public static final Path PATH = Path.from("/role/");

    /**
     * Returns a stream of all roles found in the storage.
     *
     * @param storage        the storage to search
     *
     * @return a stream of role instances found
     */
    public static Stream<Role> all(Storage storage) {
        return storage.query(PATH).objects(Role.class);
    }

    /**
     * Normalizes a role data object if needed. This method will
     * modify legacy data into the proper keys and values.
     *
     * @param id             the object identifier
     * @param dict           the storage data
     *
     * @return the storage data (possibly modified)
     */
    public static Dict normalize(String id, Dict dict) {
        if (!dict.containsKey(KEY_TYPE)) {
            LOG.warning("deprecated: role " + id + " data: missing 'type' property");
            dict.set(KEY_TYPE, "role");
            dict.set(KEY_ID, id);
        }
        for (Object o : dict.getArray(KEY_ACCESS)) {
            normalizeAccess(id, (Dict) o);
        }
        return dict;
    }

    /**
     * Normalizes a role access object. This method will change
     * legacy data into the proper keys and values as needed.
     *
     * @param id             the object identifier
     * @param dict           the access data
     */
    private static void normalizeAccess(String id, Dict dict) {
        String type = dict.get("type", String.class);
        String name = dict.get("name", String.class);
        String regex = dict.get("regexp", String.class);
        String perms = dict.get(ACCESS_PERMISSION, String.class, "");
        if (type != null && name != null) {
            LOG.warning("deprecated: role " + id + " data: legacy access path permission");
            dict.remove("type");
            dict.remove("name");
            dict.set(ACCESS_PATH, type + "/" + name);
            dict.set(ACCESS_PERMISSION, PERM_READ);
        } else if (type != null && regex != null) {
            LOG.warning("deprecated: role " + id + " data: legacy access regex permission");
            dict.remove("type");
            dict.remove("regexp");
            dict.set(ACCESS_REGEX, type + "/" + regex);
            dict.set(ACCESS_PERMISSION, PERM_READ);
        }
        if (regex != null && !dict.containsKey(ACCESS_REGEX)) {
            LOG.warning("deprecated: role " + id + " data: uses 'regexp' instead of 'regex' property");
            dict.set(ACCESS_REGEX, regex);
            dict.remove("regexp");
        }
        if (dict.containsKey("caller")) {
            LOG.warning("deprecated: role " + id + " data: legacy 'caller' property");
            dict.remove("caller");
            dict.set(ACCESS_PERMISSION, PERM_READ);
            dict.set(ACCESS_VIA, VIA_NON_INTERNAL);
        }
        if (Strings.CI.contains(perms, PERM_INTERNAL)) {
            LOG.warning("deprecated: role " + id + " data: legacy 'internal' permission");
            dict.set(ACCESS_PERMISSION, Strings.CS.replace(perms, PERM_INTERNAL, PERM_READ));
            dict.set(ACCESS_VIA, VIA_NON_INTERNAL);
        }
    }

    /**
     * Creates a new role from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public Role(String id, String type, Dict dict) {
        super(id, type, normalize(id, dict));
        this.dict.setDefault(KEY_NAME, "");
        this.dict.setDefault(KEY_DESCRIPTION, "");
    }

    /**
     * Initializes this role after loading it from a storage.
     */
    @Override
    protected void init() {
        for (Object o : dict.getArray(KEY_ACCESS)) {
            if (o instanceof Dict dict) {
                dict.set(PREFIX_COMPUTED + ACCESS_REGEX, initPathRegex(dict));
                if (dict.containsKey(ACCESS_VIA)) {
                    dict.set(PREFIX_COMPUTED + ACCESS_VIA, initViaRegex(dict));
                }
                dict.set(PREFIX_COMPUTED + ACCESS_PERMISSION, initPermissions(dict));
            }
        }
    }

    /**
     * Creates a path regex pattern from the access dictionary.
     *
     * @param dict           the access dictionary
     *
     * @return the storage path pattern regex
     */
    protected Pattern initPathRegex(Dict dict) {
        Pattern m = Pattern.compile("^invalid-pattern$");
        if (dict.get(ACCESS_PATH) instanceof String glob) {
            glob = Strings.CS.removeStart(glob, "/");
            try {
                m = Pattern.compile("^" + RegexUtil.fromGlob(glob) + "$", Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "invalid path pattern in role " + id(), e);
            }
        } else if (dict.get(ACCESS_REGEX) instanceof String regex) {
            regex = Strings.CS.removeStart(regex, "^");
            regex = Strings.CS.removeStart(regex, "/");
            regex = Strings.CS.removeEnd(regex, "$");
            try {
                m = Pattern.compile("^" + regex + "$", Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "invalid regex pattern in role " + id(), e);
            }
        }
        return m;
    }

    /**
     * Creates a via path regex pattern from the access dictionary.
     *
     * @param dict           the access dictionary
     *
     * @return the via storage path pattern regex
     */
    protected Pattern initViaRegex(Dict dict) {
        Pattern m = Pattern.compile("^invalid-pattern$");
        if (dict.get(ACCESS_VIA) instanceof String glob) {
            if (VIA_NON_INTERNAL.equals(glob)) {
                return Pattern.compile("^procedure/(?!system/).*$", Pattern.CASE_INSENSITIVE);
            }
            glob = Strings.CS.removeStart(glob, "/");
            try {
                m = Pattern.compile("^" + RegexUtil.fromGlob(glob) + "$", Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "invalid via pattern in role " + id(), e);
            }
        }
        return m;
    }

    /**
     * Initialize the access dictionary permissions set.
     *
     * @param dict           the access dictionary
     *
     * @return the permissions set
     */
    protected HashSet<String> initPermissions(Dict dict) {
        HashSet<String> set = new HashSet<>(4);
        String perms = dict.get(ACCESS_PERMISSION, String.class, "");
        for (String s : perms.trim().split("[,;\\s]+")) {
            if (s.isBlank() || s.equalsIgnoreCase(PERM_NONE)) {
                set.clear();
                break;
            } else {
                set.add(s.toLowerCase());
            }
        }
        return set;
    }

    /**
     * Returns the role name.
     *
     * @return the role name.
     */
    public String name() {
        return dict.get(KEY_NAME, String.class, "");
    }

    /**
     * Returns the role description.
     *
     * @return the role description.
     */
    public String description() {
        return dict.get(KEY_DESCRIPTION, String.class, "");
    }

    /**
     * Returns the automatic role attachment type. The values "all"
     * and "auth" are the only ones with defined meaning.
     *
     * @return the automatic role attachment type
     */
    public String auto() {
        return dict.get(KEY_AUTO, String.class, "none");
    }

    /**
     * Checks if the specified user has this role. The user may be
     * null, in which case only automatic roles for "all" will be
     * considered a match.
     *
     * @param user           the user to check, or null
     *
     * @return true if the user has this role, or
     *         false otherwise
     */
    public boolean hasUser(User user) {
        boolean matchAll = auto().equalsIgnoreCase("all");
        boolean matchAuth = auto().equalsIgnoreCase("auth");
        if (user == null) {
            return matchAll;
        } else  {
            return matchAll || matchAuth || user.hasRole(id());
        }
    }

    /**
     * Checks if the role has access permission for a storage path.
     * The access list is processed from top to bottom to find a
     * matching path entry. If a matching path with the PERM_NONE
     * permission is encountered, false will be returned. Otherwise
     * true will be returned only if the permission matches the
     * requested one.
     *
     * @param path           the requested object storage path
     * @param permission     the requested permission
     *
     * @return true if the role provides access, or
     *         false otherwise
     */
    public boolean hasAccess(String path, String permission) {
        return hasAccess(path, null, permission);
    }

    /**
     * Checks if the role has access permission for a storage path.
     * The access list is processed from top to bottom to find a
     * matching path entry. If a matching path with the PERM_NONE
     * permission is encountered, false will be returned. Otherwise
     * true will be returned only if the permission matches the
     * requested one.
     *
     * @param path           the requested object storage path
     * @param via            the caller path, or null to use context
     * @param permission     the requested permission
     *
     * @return true if the role provides access, or
     *         false otherwise
     */
    public boolean hasAccess(String path, String via, String permission) {
        LOG.fine(this + ": " + permission + " permission check for " + path);
        for (Object o : dict.getArray(KEY_ACCESS)) {
            if (o instanceof Dict dict && matchPath(dict, path, via)) {
                if (dict.get(PREFIX_COMPUTED + ACCESS_PERMISSION) instanceof HashSet<?> set) {
                    if (set.isEmpty()) {
                        return false;
                    } else if (set.contains(PERM_ALL) || set.contains(permission)) {
                        return true;
                    } else if (set.contains(PERM_READ) && PERM_INTERNAL.equals(permission)) {
                        LOG.warning("deprecated: internal permission check for " + path);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if the access data matches the specified values. If a via
     * storage path is specified, it will be used. Otherwise, the current
     * context chain will be traversed for a match. Specify "-" as the via
     * path to bypass indirect matches (ignoring rules with via patterns).
     *
     * @param dict           the access data
     * @param path           the requested path
     * @param via            the caller path, or null to use context
     *
     * @return true if the requested path is a match, or
     *         false otherwise
     */
    private boolean matchPath(Dict dict, String path, String via) {
        Pattern m = dict.get(PREFIX_COMPUTED + ACCESS_REGEX, Pattern.class);
        Pattern v = dict.get(PREFIX_COMPUTED + ACCESS_VIA, Pattern.class);
        return (
            m != null &&
            m.matcher(path).matches() &&
            (v == null || matchVia(v, via))
        );
    }

    /**
     * Checks if the caller path matches the specified via pattern. If the
     * via path is null, the current context chain will be traversed for a
     * match.
     *
     * @param pattern        the via pattern
     * @param via            the caller path, or null to use context
     *
     * @return true if the caller path matches, or
     *         false otherwise
     */
    private boolean matchVia(Pattern pattern, String via) {
        if (via == null) {
            Context cx = Context.active();
            return cx != null && cx.hasMatchingId(pattern);
        } else {
            return pattern.matcher(via).matches();
        }
    }
}
