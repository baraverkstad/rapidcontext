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

package org.rapidcontext.app.model;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.type.Vault;

/**
 * An environment variable vault. Fetches values (case-sensitive)
 * from the run-time environment.
 *
 * @author Per Cederberg
 */
public class EnvironmentVault extends Vault {

    /**
     * Creates a new environment vault from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public EnvironmentVault(String id, String type, Dict dict) {
        super(id, type, dict);
    }

    /**
     * Lookup a specified key value in this vault.
     *
     * @param key            the key identifier
     *
     * @return the vault value corresponding to the key, or
     *         null if the key cannot be found
     */
    @Override
    public String lookup(String key) {
        return System.getenv(key);
    }
}
