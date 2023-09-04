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

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.app.model.ApiUtil;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.Procedure;

/**
 * The built-in storage write procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StorageWriteProcedure extends Procedure {

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public StorageWriteProcedure(String id, String type, Dict dict) {
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

        Dict opts = ApiUtil.options("path", bindings.getValue("path"));
        Path path = Path.from(opts.getString("path", "/"));
        if (path.isIndex()) {
            throw new ProcedureException(this, "cannot write to index: " + path);
        }
        CallContext.checkWriteAccess(path.toString());
        String updateTo = opts.getString("updateTo", null);
        boolean update = opts.getBoolean("update", false) || updateTo != null;
        Path dst = (updateTo == null) ? path : Path.from(updateTo);
        Object data = bindings.getValue("data", null);
        boolean delete = data == null;
        boolean isString = data instanceof String;
        boolean isDict = data instanceof Dict;
        boolean isArray = data instanceof Array;
        boolean isBinary = data instanceof Binary || data instanceof File;
        if (update && !isDict) {
            throw new ProcedureException(this, "update only supported for dictionary data");
        } else if (isString) {
            data = new Binary.BinaryString((String) data);
        } else if (!delete && !isString && !isDict && !isArray && !isBinary) {
            throw new ProcedureException(this, "input data type not supported");
        }
        boolean isObjectPath = path.equals(Storage.objectPath(path));
        String fmt = ((String) bindings.getValue("format", "")).trim();
        if (fmt.equals("")) {
            // Format is selected in storage layer
        } else if (fmt.equalsIgnoreCase("binary")) {
            if (!isString && !isBinary) {
                throw new ProcedureException(this, "binary format requires binary data");
            }
        } else if (fmt.equalsIgnoreCase("properties") && isObjectPath) {
            path = path.sibling(path.name() + Storage.EXT_PROPERTIES);
        } else if (fmt.equalsIgnoreCase("json") && isObjectPath) {
            path = path.sibling(path.name() + Storage.EXT_JSON);
        } else if (fmt.equalsIgnoreCase("xml") && isObjectPath) {
            path = path.sibling(path.name() + Storage.EXT_XML);
        } else if (fmt.equalsIgnoreCase("yaml") && isObjectPath) {
            path = path.sibling(path.name() + Storage.EXT_YAML);
        } else if (!isObjectPath && StringUtils.endsWithIgnoreCase(path.name(), "." + fmt)) {
            // Path and specified format match
        } else {
            String msg = "invalid data format '" + fmt + "' for path " + path;
            throw new ProcedureException(this, msg);
        }
        if (delete) {
            return ApiUtil.delete(cx.getStorage(), path);
        } else if (update) {
            return ApiUtil.update(cx.getStorage(), path, dst, (Dict) data);
        } else {
            return ApiUtil.store(cx.getStorage(), path, data);
        }
    }
}
