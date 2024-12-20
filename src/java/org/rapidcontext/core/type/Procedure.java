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

import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.lang3.time.DateUtils;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
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
public abstract class Procedure extends StorableObject {

    /**
     * The class logger.
     */
    private static final Logger LOG = Logger.getLogger(Procedure.class.getName());

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
     * Returns a stream of all procedures found in the storage.
     *
     * @param storage        the storage to search
     *
     * @return a stream of procedure instances found
     */
    public static Stream<Procedure> all(Storage storage) {
        return storage.query(PATH).objects(Procedure.class);
    }

    /**
     * Searches for a specific procedure in the storage.
     *
     * @param storage        the storage to search in
     * @param id             the procedure identifier
     *
     * @return the procedure found, or
     *         null if not found
     */
    public static Procedure find(Storage storage, String id) {
        return storage.load(Path.resolve(PATH, id), Procedure.class);
    }

    /**
     * Normalizes a procedure data object if needed. This method will
     * modify legacy data into the proper keys and values.
     *
     * @param id             the object identifier
     * @param dict           the storage data
     *
     * @return the storage data (possibly modified)
     */
    public static Dict normalize(String id, Dict dict) {
        for (Object o : dict.getArray(KEY_BINDING)) {
            if (o instanceof Dict d) {
                String type = d.get("type", String.class);
                if (type.equals("1")) {
                    LOG.warning("deprecated: procedure/" + id + " binding type: " + o);
                    d.set("type", "data");
                } else if (type.equals("2")) {
                    LOG.warning("deprecated: procedure/" + id + " binding type: " + o);
                    d.set("type", "procedure");
                } else if (type.equals("3")) {
                    LOG.warning("deprecated: procedure/" + id + " binding type: " + o);
                    d.set("type", "connection");
                } else if (type.equals("4")) {
                    LOG.warning("deprecated: procedure/" + id + " binding type: " + o);
                    d.set("type", "argument");
                }
            } else {
                LOG.warning("invalid procedure/" + id + " binding: " + o);
            }
        }
        return dict;
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
        super(id, type, normalize(id, dict));
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
     * Returns the procedure description.
     *
     * @return the procedure description
     */
    public String description() {
        return dict.get(KEY_DESCRIPTION, String.class, "");
    }

    /**
     * Returns the optional procedure alias.
     *
     * @return the procedure alias, or null for none
     */
    public String alias() {
        return dict.get(KEY_ALIAS, String.class);
    }

    /**
     * Returns the optional deprecation message.
     *
     * @return the deprecation message, or null for none
     */
    public String deprecated() {
        return dict.get(KEY_DEPRECATED, String.class);
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

    /**
     * Executes a call of this procedure in the specified context
     * and with the specified call bindings. The semantics of what
     * the procedure actually does, is up to each implementation.
     * Note that the call bindings are normally inherited from the
     * procedure bindings with arguments bound to their call values.
     *
     * @param cx             the procedure call context
     * @param bindings       the call bindings to use
     *
     * @return the result of the call, or
     *         null if the call produced no result
     *
     * @throws ProcedureException if the call execution caused an
     *             error
     */
    public abstract Object call(CallContext cx, Bindings bindings)
        throws ProcedureException;
}
