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
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.util.FileUtil;

/**
 * The built-in storage copy procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StorageCopyProcedure implements Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(StorageCopyProcedure.class.getName());

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.Storage.Copy";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new storage copy procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public StorageCopyProcedure() throws ProcedureException {
        defaults.set("src", Bindings.ARGUMENT, "", "The source object path");
        defaults.set("dst", Bindings.ARGUMENT, "", "The destination object path");
        defaults.set("flags", Bindings.ARGUMENT, "",
            "The optional flags, available values are:\n" +
            "  update -- copy only if the source is newer");
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
        return "Copies an object from one storage path to another.";
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

        String src = ((String) bindings.getValue("src", "")).trim();
        if (src.length() <= 0) {
            throw new ProcedureException("source path cannot be empty");
        } else if (src.endsWith("/")) {
            throw new ProcedureException("source path cannot be an index");
        }
        CallContext.checkAccess(src, cx.readPermission(1));
        String dst = ((String) bindings.getValue("dst", "")).trim();
        if (dst.length() <= 0) {
            throw new ProcedureException("destination path cannot be empty");
        } else if (dst.endsWith("/")) {
            dst += StringUtils.substringAfterLast("/" + src, "/");
        }
        CallContext.checkWriteAccess(dst);
        String flags = ((String) bindings.getValue("flags", "")).toLowerCase();
        boolean update = flags.contains("update");
        return Boolean.valueOf(copy(new Path(src), new Path(dst), update));
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
    public static boolean copy(Path src, Path dst, boolean updateOnly) {
        Storage storage = ApplicationContext.getInstance().getStorage();
        if (updateOnly) {
            Metadata srcMeta = storage.lookup(src);
            Metadata dstMeta = storage.lookup(dst);
            Date srcTime = (srcMeta == null) ? new Date(0) : srcMeta.lastModified();
            Date dstTime = (dstMeta == null) ? new Date(0) : dstMeta.lastModified();
            if (!dstTime.before(srcTime)) {
                return false;
            }
        }
        Object data = storage.load(src);
        if (data instanceof Dict) {
            try {
                storage.store(dst, data);
                return true;
            } catch (Exception e) {
                LOG.log(Level.WARNING, "failed to copy " + src + " to " + dst, e);
                return false;
            }
        } else if (data instanceof Binary) {
            try {
                File file = FileUtil.tempFile(src.name());
                FileUtil.copy(((Binary) data).openStream(), file);
                storage.store(dst, file);
                return true;
            } catch (Exception e) {
                LOG.log(Level.WARNING, "failed to copy " + src + " to " + dst, e);
                return false;
            }
        } else {
            return false;
        }
    }
}
