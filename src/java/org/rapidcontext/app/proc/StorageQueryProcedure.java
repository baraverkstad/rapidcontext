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

import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Query;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.Procedure;

/**
 * The built-in storage query procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StorageQueryProcedure extends Procedure {

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public StorageQueryProcedure(String id, String type, Dict dict) {
        super(id, type, dict);
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
    public Object call(CallContext cx, Bindings bindings)
    throws ProcedureException {

        Object obj = bindings.getValue("query");
        Dict dict = (obj instanceof Dict) ? (Dict) obj : new Dict();
        String path = dict.getString("path", obj.toString());
        if (!path.startsWith("/")) {
            throw new ProcedureException("invalid query path, must start with /");
        } else if (!path.endsWith("/")) {
            throw new ProcedureException("invalid query path, must be an index");
        }
        CallContext.checkSearchAccess(path);
        Storage storage = ApplicationContext.getInstance().getStorage();
        Query query = storage.query(Path.from(path)).filterReadAccess();
        if (dict.containsKey("depth")) {
            query.filterDepth(dict.getInt("depth", -1));
        }
        if (dict.containsKey("fileType")) {
            query.filterFileExtension("." + dict.getString("fileType", ""));
        }
        int limit = dict.getInt("limit", 1000);
        Stream<Metadata> stream = query.metadatas().limit(limit);
        if (dict.containsKey("mimeType")) {
            String mimeType = dict.getString("mimeType", "");
            stream = stream.filter(meta -> {
                return StringUtils.startsWithIgnoreCase(meta.mimeType(), mimeType);
            });
        }
        if (dict.containsKey("category")) {
            String category = dict.getString("category", "");
            stream = stream.filter(meta -> {
                return StringUtils.equalsIgnoreCase(meta.category(), category);
            });
        }
        Dict[] objs = stream.map(this::serialize).toArray(Dict[]::new);
        Array res = new Array(objs.length);
        for (Dict item : objs) {
            res.add(item);
        }
        return res;
    }

    /**
     * Returns a serialized representation of a metadata object.
     *
     * @param meta           the storage metadata
     *
     * @return the serialized representation of the object, or
     *         null if no suitable serialization existed
     */
    private Dict serialize(Metadata meta) {
        Dict res = new Dict();
        res.set(Metadata.KEY_PATH, meta.path().toString());
        res.set(Metadata.KEY_CATEGORY, meta.category());
        res.set(Metadata.KEY_MIMETYPE, meta.mimeType());
        res.set(Metadata.KEY_MODIFIED, meta.lastModified());
        return res;
    }
}
