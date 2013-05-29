/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2013 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE.txt file for more details.
 */

package org.rapidcontext.app.proc;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.BinaryString;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.PropertiesSerializer;
import org.rapidcontext.core.data.XmlSerializer;
import org.rapidcontext.core.js.JsSerializer;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;

/**
 * The built-in storage write procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StorageWriteProcedure implements Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(StorageWriteProcedure.class.getName());

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.Storage.Write";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new storage write procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public StorageWriteProcedure() throws ProcedureException {
        defaults.set("path", Bindings.ARGUMENT, "", "The object path");
        defaults.set("data", Bindings.ARGUMENT, "", "The data to write");
        defaults.set("format", Bindings.ARGUMENT, "",
            "The data format, available values are:\n" +
            "  binary -- (default) save as binary data\n" +
            "  properties -- serialize to properties format\n" +
            "  json -- serialize to JSON format\n" +
            "  xml -- serialize to XML format");
        defaults.seal();
    }

    /**
     * Returns the procedure name.
     *
     * @return the procedure name
     */
    public String getName() {
        return NAME;
    }

    /**
     * Returns the procedure description.
     *
     * @return the procedure description
     */
    public String getDescription() {
        return "Writes an object to storage.";
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
        return defaults;
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
            throw new ProcedureException("path cannot be empty");
        }
        CallContext.checkWriteAccess(path);
        Object data = bindings.getValue("data", "");
        boolean isString = data instanceof String;
        boolean isDict = data instanceof Dict;
        boolean isBinary = data instanceof Binary ||
                           data instanceof File;
        if (isString) {
            data = new BinaryString((String) data);
        } else if (!isString && !isDict && !isBinary) {
            throw new ProcedureException("input data type not supported");
        }
        String fmt = ((String) bindings.getValue("format", "")).trim();
        if (fmt.equalsIgnoreCase("binary") || fmt.length() <= 0) {
            if (isDict) {
                try {
                    String str = PropertiesSerializer.serialize(data);
                    data = new BinaryString(str);
                } catch (Exception e) {
                    String msg = "invalid data: " + e.getMessage();
                    LOG.log(Level.WARNING, msg, e);
                    throw new ProcedureException(msg);
                }
            }
        } else if (fmt.equalsIgnoreCase("properties")) {
            // Conversion handled in storage layer
        } else if (fmt.equalsIgnoreCase("json")) {
            if (isDict) {
                String str = JsSerializer.serialize(data, true);
                data = new BinaryString(str);
            }
        } else if (fmt.equalsIgnoreCase("xml")) {
            if (isDict) {
                String str = XmlSerializer.serialize("data", data);
                data = new BinaryString(str);
            }
        } else {
            throw new ProcedureException("invalid data format: " + fmt);
        }

        return Boolean.valueOf(store(new Path(path), data));
    }

    /**
     * Copies a storage object to a new destination.
     *
     * @param src            the source object path
     * @param dst            the destination object path
     * @param updateOnly     the copy-only-on-newer flag
     *
     * @return true if the data was successfully copied, or
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
