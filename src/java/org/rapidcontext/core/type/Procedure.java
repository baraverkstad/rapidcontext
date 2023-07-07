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

import java.util.stream.Stream;

import org.apache.commons.lang3.time.DateUtils;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;

/**
 * A server-side function or operation. Procedures may take arguments, modify
 * data and/or return values. They can be invoked either server-side or
 * client-side (via API) if permissions allow.
 *
 * This generic type is used only for direct Java implementations. A number
 * of subtypes allows creating procedures from configurable parameters (e.g.
 * SQL text, HTTP requests, etc) instead, sharing a common reusable Java
 * implementation. When implementing procedures in Java, care must be taken so
 * that all operations are properly checked for security.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public abstract class Procedure extends StorableObject implements org.rapidcontext.core.proc.Procedure {

    /**
     * The dictionary key for the description.
     */
    public static final String KEY_DESCRIPTION = "description";

    /**
     * The dictionary key for the optional alias.
     */
    public static final String KEY_ALIAS = "alias";

    /**
     * The dictionary key for the optional deprecation message.
     */
    public static final String KEY_DEPRECATED = "deprecated";

    /**
     * The dictionary key for the binding array.
     */
    public static final String KEY_BINDING = "binding";

    /**
     * The procedure object storage path.
     */
    public static final Path PATH = Path.from("/procedure/");

    /**
     * The default active procedure time (5 minutes).
     */
    public static final long ACTIVE_MILLIS = 5L * DateUtils.MILLIS_PER_MINUTE;

    /**
     * Returns a stream of all environments found in the storage.
     *
     * @param storage        the storage to search
     *
     * @return a stream of environment instances found
     */
    public static Stream<Procedure> all(Storage storage) {
        return storage.query(PATH).objects(Procedure.class);
    }

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     *
     * @see #init()
     */
    protected Procedure(String id, String type, Dict dict) {
        super(id, type, dict);
    }

    /**
     * Checks if this object is in active use. This method will return
     * true if the object was activated during the last 5 minutes.
     *
     * @return true if the object is considered active, or
     *         false otherwise
     */
    protected boolean isActive() {
        return System.currentTimeMillis() - activatedTime().getTime() <= ACTIVE_MILLIS;
    }

    /**
     * Returns the procedure name.
     *
     * @return the procedure name
     *
     * @deprecated Use id() instead.
     */
    @Deprecated
    public String getName() {
        return id();
    }

    /**
     * Returns the procedure description.
     *
     * @return the procedure description
     */
    public String description() {
        return dict.getString(KEY_DESCRIPTION, "");
    }

    /**
     * Returns the procedure description.
     *
     * @return the procedure description
     *
     * @deprecated Use description() instead.
     */
    @Deprecated
    public String getDescription() {
        return description();
    }

    /**
     * Returns the optional procedure alias.
     *
     * @return the procedure alias, or null for none
     */
    public String alias() {
        return dict.getString(KEY_ALIAS, null);
    }

    /**
     * Returns the optional deprecation message.
     *
     * @return the deprecation message, or null for none
     */
    public String deprecated() {
        return dict.getString(KEY_DEPRECATED, null);
    }

    /**
     * Returns the bindings for this procedure. If this procedure
     * requires any special data, adapter connection or input
     * argument binding, those bindings should be set (but possibly
     * to null or blank values).
     *
     * @return the bindings for this procedure
     */
    public Bindings getBindings() {
        return new Bindings(null, dict.getArray(KEY_BINDING));
    }
}
