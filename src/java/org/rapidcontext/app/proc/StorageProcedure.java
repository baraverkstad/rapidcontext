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

package org.rapidcontext.app.proc;

import java.io.InputStream;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Query;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.util.FileUtil;

/**
 * The base class for built-in storage procedures.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public abstract class StorageProcedure extends Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(StorageProcedure.class.getName());

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    protected StorageProcedure(String id, String type, Dict dict) {
        super(id, type, dict);
    }

    /**
     * Creates or extracts an options object from a value. If the value isn't
     * a dictionary, a new one will be created with a default key set to the
     * specified value.
     *
     * @param defaultKey     the default key
     * @param value          the value to convert
     *
     * @return an options dictionary
     */
    protected Dict options(String defaultKey, Object value) {
        if (value instanceof Dict) {
            return (Dict) value;
        } else {
            Dict dict = new Dict();
            dict.add(defaultKey, value);
            return dict;
        }
    }

    /**
     * Search for object metadata in storage.
     *
     * @param storage        the storage to search
     * @param path           the base path to query
     * @param opts           the query options
     *
     * @return a stream of metadata for matching objects
     */
    protected Stream<Metadata> lookup(Storage storage, Path path, Dict opts) {
        Query query = storage.query(path).filterReadAccess();
        if (opts.containsKey("depth")) {
            query.filterDepth(opts.getInt("depth", -1));
        }
        if (opts.containsKey("fileType")) {
            query.filterFileExtension("." + opts.getString("fileType", ""));
        }
        Stream<Metadata> stream = query.metadatas();
        if (opts.containsKey("mimeType")) {
            String mimeType = opts.getString("mimeType", "");
            stream = stream.filter(meta -> {
                return StringUtils.startsWithIgnoreCase(meta.mimeType(), mimeType);
            });
        }
        if (opts.containsKey("category")) {
            String category = opts.getString("category", "");
            stream = stream.filter(meta -> {
                return StringUtils.equalsIgnoreCase(meta.category(), category);
            });
        }
        int limit = opts.getInt("limit", 1000);
        if (limit > 0) {
            stream = stream.limit(limit);
        }
        return stream;
    }

    /**
     * Load objects from storage and serializes the results.
     *
     * @param storage        the storage to search
     * @param path           the base path to query
     * @param opts           the query options
     *
     * @return a stream of serialized matching objects
     */
    protected Stream<Object> load(Storage storage, Path path, Dict opts) {
        boolean computed = opts.getBoolean("computed", false);
        boolean metadata = opts.getBoolean("metadata", false);
        return lookup(storage, path, opts)
            .map(m -> {
                Object o = serialize(m.path(), storage.load(m.path()), computed);
                if (metadata) {
                    Dict res = new Dict();
                    res.add("data", o);
                    res.add("metadata", serialize(m.path(), m, computed));
                    return res;
                } else {
                    return o;
                }
            })
            .filter(Objects::nonNull);
    }

    /**
     * Returns a serialized representation of a storage object.
     *
     * @param path           the storage path
     * @param obj            the storage object
     * @param computed       the flag for computed key inclusion
     *
     * @return the serialized representation of the object, or
     *         null if no suitable serialization existed
     */
    protected Object serialize(Path path, Object obj, boolean computed) {
        if (obj instanceof Binary) {
            Binary data = (Binary) obj;
            Dict dict = new Dict();
            dict.set("type", "file");
            dict.set("path", path);
            dict.set("name", path.name());
            dict.set("mimeType", data.mimeType());
            dict.set("modified", new Date(data.lastModified()));
            dict.set("size", Long.valueOf(data.size()));
            if (computed) {
                try (InputStream is = data.openStream()) {
                    if (Mime.isText(data.mimeType())) {
                        dict.set("_text", FileUtil.readText(is, "UTF-8"));
                    } else {
                        dict.set("_data", Base64.getEncoder().encodeToString(is.readAllBytes()));
                    }
                } catch (Exception e) {
                    String msg = "invalid data read: " + e.getMessage();
                    LOG.log(Level.WARNING, msg, e);
                    dict.set("_error", msg);
                }
            }
            return dict;
        } else if (obj instanceof StorableObject) {
            return StorableObject.sterilize(obj, true, !computed, true);
        } else if (obj instanceof Dict) {
            return (Dict) obj;
        } else {
            return (obj == null) ? obj : obj.toString();
        }
    }
}
