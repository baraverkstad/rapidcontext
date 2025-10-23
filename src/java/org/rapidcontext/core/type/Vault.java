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

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Strings;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;

/**
 * An external configuration value source. Commonly used for storing
 * secrets, such as passwords and tokens outside the normal storage
 * tree.
 *
 * @author Per Cederberg
 */
public abstract class Vault extends StorableObject {

    /**
     * The dictionary key for the vault description.
     */
    public static final String KEY_DESCRIPTION = "description";

    /**
     * The dictionary key for the global flag.
     */
    public static final String KEY_GLOBAL = "global";

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
     * The cached vaults, indexed by their id.
     */
    private static final LinkedHashMap<String,Vault> cache = new LinkedHashMap<>();

    /**
     * Loads all vaults found in the storage to the cache.
     *
     * @param storage        the storage to search
     */
    public static void loadAll(Storage storage) {
        storage.query(PATH).objects(Vault.class).forEach(v -> cache.put(v.id(), v));
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
            Vault vault = cache.get(id);
            return (vault == null) ? null : vault.lookup(key);
        } else {
            for (Vault vault : cache.values()) {
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
