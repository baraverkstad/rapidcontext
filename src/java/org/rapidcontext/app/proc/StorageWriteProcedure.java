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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.app.ApplicationContext;
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
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(StorageWriteProcedure.class.getName());

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

        String path = ((String) bindings.getValue("path", "")).trim();
        if (path.length() <= 0) {
            throw new ProcedureException(this, "path cannot be empty");
        }
        CallContext.checkWriteAccess(path);
        LOG.fine("writing to storage path " + path);
        Object data = bindings.getValue("data", "");
        boolean isString = data instanceof String;
        boolean isStruct = data instanceof Dict || data instanceof Array;
        boolean isBinary = data instanceof Binary || data instanceof File;
        if (isString) {
            data = new Binary.BinaryString((String) data);
        } else if (!isString && !isStruct && !isBinary) {
            throw new ProcedureException(this, "input data type not supported");
        }
        String fmt = ((String) bindings.getValue("format", "")).trim();
        if (fmt.equals("") || fmt.equalsIgnoreCase("binary")) {
            if (!isString && !isBinary) {
                throw new ProcedureException(this, "binary format requires binary data");
            }
        } else if (fmt.equalsIgnoreCase("properties")) {
            if (!StringUtils.endsWithIgnoreCase(path, Storage.EXT_PROPERTIES)) {
                path += Storage.EXT_PROPERTIES;
            }
        } else if (fmt.equalsIgnoreCase("json")) {
            if (!StringUtils.endsWithIgnoreCase(path, Storage.EXT_JSON)) {
                path += Storage.EXT_JSON;
            }
        } else if (fmt.equalsIgnoreCase("xml")) {
            if (!StringUtils.endsWithIgnoreCase(path, Storage.EXT_XML)) {
                path += Storage.EXT_XML;
            }
        } else if (fmt.equalsIgnoreCase("yaml")) {
            if (!StringUtils.endsWithIgnoreCase(path, Storage.EXT_YAML)) {
                path += Storage.EXT_YAML;
            }
        } else {
            throw new ProcedureException(this, "invalid data format: " + fmt);
        }

        return Boolean.valueOf(store(Path.from(path), data));
    }

    /**
     * Writes a data object to the storage.
     *
     * @param path           the storage path
     * @param data           the data object
     *
     * @return true if the data was successfully written, or
     *         false otherwise
     */
    public static boolean store(Path path, Object data) {
        Storage storage = ApplicationContext.getInstance().getStorage();
        try {
            storage.store(path, data);
            return true;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed to store " + path, e);
            return false;
        }
    }
}
