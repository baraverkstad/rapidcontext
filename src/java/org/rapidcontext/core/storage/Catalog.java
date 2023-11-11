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

package org.rapidcontext.core.storage;

import java.util.HashMap;

import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;

public class Catalog {

    /**
     * The map of object aliases. The map is indexed by the
     * old procedure name and points to the new one.
     */
    private HashMap<Path,Path> aliases = new HashMap<>();

//    protected void refresh() {
//        aliases.clear();
//        storage.query(Path.ROOT).metadatas(Dict.class).forEach(meta -> {
//            Path path = meta.path();
//            Dict dict = load(path, Dict.class);
//        });
//    }
//
//    protected void aliasAdd(Path path, Dict dict) {
//        if (dict.containsKey(KEY_ID) && dict.containsKey(KEY_TYPE)) {
//            Object alias = dict.get(KEY_ALIAS);
//            if (alias instanceof Array) {
//                for (Object o : (Array) alias) {
//                    aliasAdd(path, o.toString());
//                }
//            } else {
//                aliasAdd(path, alias.toString());
//            }
//        }
//    }

    protected void aliasAdd(Path path, String alias) {
        Path type = Path.from(path.name(0));
        aliases.put(Path.resolve(type, alias), path);
    }

    protected void aliasRemove(Path path) {
        // FIXME: aliases.remove(path);
    }
}
