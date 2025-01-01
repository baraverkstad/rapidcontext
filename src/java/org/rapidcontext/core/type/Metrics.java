/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2024 Per Cederberg. All rights reserved.
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
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.stat.MovingUsage;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.StorageException;

/**
 * A metrics set for tracking resource usage.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Metrics extends StorableObject {

    /**
     * The metrics object storage path.
     */
    public static final Path PATH = Path.from("/.metrics/");

    /**
     * The dictionary key for the last updated timestamp.
     */
    public static final String KEY_UPDATED = "updated";

    /**
     * The dictionary key for the map of data points.
     */
    public static final String KEY_DATA = "data";

    /**
     * Finds or creates a metrics set in the storage.
     *
     * @param storage        the storage to search in
     * @param id             the metrics identifier
     *
     * @return the metrics set found or newly created
     *
     * @throws StorageException if a new metrics set couldn't be stored
     */
    public static Metrics findOrCreate(Storage storage, String id)
    throws StorageException {
        Metrics obj = storage.load(Path.resolve(PATH, id), Metrics.class);
        if (obj == null) {
            String type = "metrics/usage";
            Dict dict = new Dict().set(KEY_ID, id).set(KEY_TYPE, type);
            obj = new Metrics(id, type, dict);
            storage.store(Path.resolve(PATH, id + Storage.EXT_JSON), obj);
        }
        return obj;
    }

    /**
     * The last updated timestamp.
     */
    private long updated = 0;

    /**
     * The map of data points.
     */
    private HashMap<String, MovingUsage> points = new HashMap<>();

    /**
     * The modified data flag.
     */
    private boolean modified = false;

    /**
     * Creates a new metrics set from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public Metrics(String id, String type, Dict dict) {
        super(id, type, dict);
    }

    /**
     * Initializes this object after loading it from a storage.
     */
    @Override
    protected void init() {
        updated = dict.get(KEY_UPDATED, Date.class, new Date()).getTime();
        Dict data = dict.getDict(KEY_DATA);
        for (String key : data.keys()) {
            points.put(key, new MovingUsage(updated, data.getDict(key)));
        }
        dict.remove(KEY_UPDATED);
        dict.remove(KEY_DATA);
    }

    /**
     * Checks if this object has been modified since initialized from
     * storage. This method is used to allow "dirty" objects to be
     * written back to persistent storage before being evicted from
     * the in-memory cache. By default this method always returns
     * false.
     *
     * @return true if the object has been modified, or
     *         false otherwise
     */
    @Override
    protected boolean isModified() {
        return modified;
    }

    /**
     * Discards the modified flag for this object.
     */
    @Override
    protected void passivate() {
        modified = false;
    }

    /**
     * Returns a stream of all resource usage metrics.
     *
     * @return the metrics entry stream
     */
    public Stream<Entry<String, MovingUsage>> stream() {
        return points.entrySet().stream();
    }

    /**
     * Reports usage for a specified key.
     *
     * @param key        the object identifier or other key to track
     * @param now        the timestamp (in millis) for the usage time
     * @param value      the usage count to add, or zero (0) for none
     * @param duration   the duration (in millis), or zero (0) to skip
     * @param success    the success flag
     * @param error      the optional error message
     */
    public void report(String key,
                       long now,
                       int value,
                       long duration,
                       boolean success,
                       String error) {

        MovingUsage usage = points.get(key);
        if (usage == null) {
            synchronized (points) {
                usage = new MovingUsage(now);
                points.put(key, usage);
            }
        }
        usage.move(now);
        usage.add(value, duration, success, error);
        updated = Math.max(updated, now);
        modified = true;
    }

    /**
     * Returns a serialized representation of this object. Used when
     * persisting to permanent storage or when accessing the object
     * from outside pure Java. Returns a shallow copy of the contained
     * dictionary.
     *
     * @return the serialized representation of this object
     */
    @Override
    public Dict serialize() {
        Dict copy = super.serialize();
        copy.set(KEY_UPDATED, new Date(updated));
        Dict data = new Dict();
        for (Entry<String, MovingUsage> e : points.entrySet()) {
            MovingUsage usage = e.getValue();
            usage.move(updated);
            data.set(e.getKey(), usage.serialize());
        }
        copy.set(KEY_DATA, data);
        return copy;
    }
}
