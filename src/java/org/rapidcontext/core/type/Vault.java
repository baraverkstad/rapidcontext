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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Strings;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.util.RegexUtil;

/**
 * An external configuration value source. Commonly used for storing
 * secrets, such as passwords and tokens outside the normal storage
 * tree.
 *
 * @author Per Cederberg
 */
public abstract class Vault extends StorableObject {

    /**
     * The class logger.
     */
    private static final Logger LOG = Logger.getLogger(Vault.class.getName());

    /**
     * The dictionary key for the vault description.
     */
    public static final String KEY_DESCRIPTION = "description";

    /**
     * The dictionary key for the global flag.
     */
    public static final String KEY_GLOBAL = "global";

    /**
     * The vault configuration storage path.
     */
    public static final Path PATH_CONFIG = Path.from("/config/vault");

    /**
     * The vault object storage path.
     */
    public static final Path PATH = Path.from("/vault/");

    /**
     * The property expansion regex pattern.
     */
    private static final Pattern RE_EXPANSION = Pattern.compile(
        "\\$\\{\\{([a-z0-9_-]+!)?([a-z0-9._-]+)(:.*?)?\\}\\}",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * The list of path glob patterns allowed for expansion.
     */
    private static final ArrayList<Pattern> PATHS = new ArrayList<>();

    /**
     * The cached vaults, indexed by their id.
     */
    private static final LinkedHashMap<String,Vault> CACHE = new LinkedHashMap<>();

    /**
     * Loads all vaults found in the storage to the cache.
     *
     * @param storage        the storage to search
     */
    public static void loadAll(Storage storage) {
        PATHS.clear();
        Dict config = storage.load(PATH_CONFIG, Dict.class);
        for (Object o : config.getArray("expand")) {
            try {
                String glob = Strings.CS.removeStart(o.toString(), "/");
                String re = "^" + RegexUtil.fromGlob(glob) + "$";
                PATHS.add(Pattern.compile(re, Pattern.CASE_INSENSITIVE));
            } catch (Exception e) {
                LOG.warning("invalid path pattern in vault configuration: " + o);
            }
        }
        CACHE.clear();
        storage.query(PATH).objects(Vault.class).forEach(v -> CACHE.put(v.id(), v));
    }

    /**
     * Searches for a specific vault in the storage.
     *
     * @param storage        the storage to search in
     * @param id             the vault identifier
     *
     * @return the vault found, or
     *         null if not found
     */
    public static Vault find(Storage storage, String id) {
        return storage.load(Path.resolve(PATH, id), Vault.class);
    }

    /**
     * Checks if a path allows variable expansion.
     *
     * @param path           the path to check
     *
     * @return true if the path allows variable expansion, or
     *         false otherwise
     */
    public static boolean canExpand(Path path) {
        String ident = path.toIdent(0);
        return PATHS.stream().anyMatch(p -> p.matcher(ident).find());
    }

    /**
     * Checks if a string contains variable references.
     *
     * @param text           the text to check
     *
     * @return true if a variable reference is found, or
     *         false otherwise
     */
    public static boolean canExpand(String text) {
        return RE_EXPANSION.matcher(text).find();
    }

    /**
     * Expands variable references in a string with the values found
     * in the loaded vaults.
     *
     * @param text           the text to process
     *
     * @return the text with all variable references replaced
     */
    public static String expand(String text) {
        String res = text;
        Matcher m = RE_EXPANSION.matcher(text);
        while (m != null && m.find()) {
            String id = Strings.CS.removeEnd(m.group(1), "!");
            String key = m.group(2);
            String val = Objects.requireNonNullElseGet(lookup(id, key), () -> {
                String def = Strings.CS.removeStart(m.group(3), ":");
                return Objects.requireNonNullElse(def, m.group());
            });
            res = res.replace(m.group(), val);
        }
        return res;
    }

    /**
     * Lookup a specified key value in one or more vaults.
     *
     * @param id             the vault id, or null for global keys
     * @param key            the key identifier
     *
     * @return the vault value corresponding to the key, or
     *         null if the key cannot be found
     */
    public static String lookup(String id, String key) {
        if (id != null && !id.isBlank()) {
            Vault vault = CACHE.get(id);
            return (vault == null) ? null : vault.lookup(key);
        } else {
            for (Vault vault : CACHE.values()) {
                if (vault.global() && vault.lookup(key) instanceof String s) {
                    return s;
                }
            }
            return null;
        }
    }

    /**
     * Creates a new vault from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public Vault(String id, String type, Dict dict) {
        super(id, type, dict);
    }

    /**
     * Returns the vault description.
     *
     * @return the vault description.
     */
    public String description() {
        return dict.get(KEY_DESCRIPTION, String.class, "");
    }

    /**
     * Returns the vault global keys flag.
     *
     * @return the vault global keys flag
     */
    public boolean global() {
        return dict.get(KEY_GLOBAL, Boolean.class, false);
    }

    /**
     * Lookup a specified key value in this vault.
     *
     * @param key            the key identifier
     *
     * @return the vault value corresponding to the key, or
     *         null if the key cannot be found
     */
    public abstract String lookup(String key);
}
