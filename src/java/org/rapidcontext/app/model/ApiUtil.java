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

import java.io.InputStream;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.lang3.Strings;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Index;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Query;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.util.FileUtil;

/**
 * A set of utility methods for API responses, etc.
 *
 * @author Per Cederberg
 */
public class ApiUtil {

    /**
     * The class logger.
     */
    private static final Logger LOG = Logger.getLogger(ApiUtil.class.getName());

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
    public static Dict options(String defaultKey, Object value) {
        return (value instanceof Dict d) ? d : new Dict().set(defaultKey, value);
    }

    /**
     * Search for object metadata in storage.
     *
     * @param query          the storage query to filter
     * @param opts           the query options
     *
     * @return a stream of metadata for matching objects
     */
    public static Stream<Metadata> lookup(Query query, Dict opts) {
        query.filterShowHidden(opts.get("hidden", Boolean.class, false));
        if (opts.containsKey("depth")) {
            query.filterDepth(opts.get("depth", Integer.class, -1));
        }
        if (opts.containsKey("fileType")) {
            String ext = opts.get("fileType", String.class, "").toLowerCase();
            if (!ext.isBlank()) {
                query.filterFileExtension("." + ext);
            }
        }
        Stream<Metadata> stream = query.metadatas();
        if (opts.containsKey("mimeType")) {
            String mimeType = opts.get("mimeType", String.class, "");
            if (!mimeType.isBlank()) {
                stream = stream.filter(meta -> Strings.CI.startsWith(meta.mimeType(), mimeType));
            }
        }
        if (opts.containsKey("category")) {
            String category = opts.get("category", String.class, "");
            if (!category.isBlank()) {
                stream = stream.filter(meta -> Strings.CI.equals(meta.category(), category));
            }
        }
        int limit = opts.get("limit", Integer.class, 1000);
        if (limit > 0) {
            stream = stream.limit(limit);
        }
        return stream;
    }

    /**
     * Load objects from storage and serializes the results.
     *
     * @param query          the storage query to filter
     * @param opts           the query options
     *
     * @return a stream of serialized matching objects
     */
    public static Stream<Object> load(Query query, Dict opts) {
        return lookup(query, opts)
            .map(m -> serialize(m, query.storage().load(m.path()), opts, true))
            .filter(Objects::nonNull);
    }

    /**
     * Writes a data object to the storage.
     *
     * @param storage        the storage to modify
     * @param path           the storage path
     * @param data           the data object
     *
     * @return true if the data was successfully written, or
     *         false otherwise
     */
    public static boolean store(Storage storage, Path path, Object data) {
        try {
            LOG.fine("writing to storage path " + path);
            if (!(data instanceof Binary)) {
                data = StorableObject.sterilize(data, false, true, true);
            }
            storage.store(path, data);
            return true;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed to store " + path, e);
            return false;
        }
    }

    /**
     * Updates and/or moves an existing data object in the storage.
     * The object must be serializable to a dictionary, which may
     * be merged with a dictionary of changes. Keys with a value will
     * be overwritten, and keys with a null value will be removed.
     * Omitted keys will be will be left unmodified in the source
     * object.
     *
     * @param storage        the storage to modify
     * @param src            the source storage path
     * @param dst            the destination storage path
     * @param patch          the data object changes, or null
     *
     * @return true if the data was successfully written, or
     *         false otherwise
     */
    public static boolean update(Storage storage, Path src, Path dst, Dict patch) {
        try {
            src = Storage.objectPath(src);
            boolean rename = !src.equals(Storage.objectPath(dst));
            LOG.fine("updating storage path " + src + (rename ? " -> " + dst : ""));
            Object prev = StorableObject.sterilize(storage.load(src), false, true, false);
            patch = (Dict) StorableObject.sterilize(patch, false, true, true);
            if (prev instanceof Dict d) {
                storage.store(dst, d.merge(patch));
                if (rename) {
                    storage.remove(src);
                }
                return true;
            } else {
                LOG.log(Level.WARNING, "failed to update " + src + ": not a dictionary");
                return false;
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed to update " + src, e);
            return false;
        }
    }

    /**
     * Deletes a storage object or path.
     *
     * @param storage        the storage to modify
     * @param path           the storage path
     *
     * @return true if the data was successfully removed, or
     *         false otherwise
     */
    public static boolean delete(Storage storage, Path path) {
        try {
            LOG.fine("deleting storage path " + path);
            storage.remove(path);
            return true;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed to delete " + path, e);
            return false;
        }
    }

    /**
     * Returns a serialized representation of an object and its metadata.
     *
     * @param meta           the object metadata
     * @param obj            the object to serialize
     * @param opts           the serialization options
     * @param limitedTypes   limit allowed object value types
     *
     * @return the serialized representation of the object, or
     *         null if no suitable serialization existed
     */
    public static Object serialize(Metadata meta,
                                   Object obj,
                                   Dict opts,
                                   boolean limitedTypes) {
        if (opts.get("metadata", Boolean.class, false)) {
            return new Dict()
                .set("data", serialize(meta.path(), obj, opts, limitedTypes))
                .set("metadata", serialize(meta.path(), meta, opts, limitedTypes));
        } else {
            return serialize(meta.path(), obj, opts, limitedTypes);
        }
    }

    /**
     * Returns a serialized representation of an object.
     *
     * @param path           the storage path
     * @param obj            the object to serialize
     * @param opts           the serialization options
     * @param limitedTypes   limit allowed object value types
     *
     * @return the serialized representation of the object, or
     *         null if no suitable serialization existed
     */
    public static Object serialize(Path path,
                                   Object obj,
                                   Dict opts,
                                   boolean limitedTypes) {
        if (obj instanceof Index idx) {
            boolean hidden = opts.get("hidden", Boolean.class, false);
            return new Dict()
                .set("type", "index")
                .set("modified", Objects.requireNonNullElse(idx.modified(), new Date()))
                .set("paths", Array.from(idx.paths(path, hidden)));
        } else if (obj instanceof Binary b) {
            Dict dict = new Dict()
                .set("type", "file")
                .set("path", path)
                .set("name", path.name())
                .set("mimeType", b.mimeType())
                .set("modified", new Date(b.lastModified()))
                .set("size", b.size());
            if (opts.get("computed", Boolean.class, false)) {
                try (InputStream is = b.openStream()) {
                    if (Mime.isText(b.mimeType())) {
                        dict.set("_text", FileUtil.readText(is));
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
        } else {
            boolean skipComputed = !opts.get("computed", Boolean.class, false);
            return StorableObject.sterilize(obj, true, skipComputed, limitedTypes);
        }
    }

    // No instances
    private ApiUtil() {}
}
