/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2022 Per Cederberg. All rights reserved.
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
import java.io.InputStream;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.util.FileUtil;

/**
 * The built-in storage copy procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class StorageCopyProcedure extends Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(StorageCopyProcedure.class.getName());

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public StorageCopyProcedure(String id, String type, Dict dict) {
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

        String src = ((String) bindings.getValue("src", "")).trim();
        if (src.length() <= 0) {
            throw new ProcedureException("source path cannot be empty");
        }
        CallContext.checkAccess(src, cx.readPermission(1));
        String dst = ((String) bindings.getValue("dst", "")).trim();
        if (dst.length() <= 0) {
            throw new ProcedureException("destination path cannot be empty");
        }
        CallContext.checkWriteAccess(dst);
        String flags = ((String) bindings.getValue("flags", "")).toLowerCase();
        boolean update = flags.contains("update");
        boolean recursive = flags.contains("recursive");
        String ext = null;
        if (flags.contains("properties")) {
            ext = Storage.EXT_PROPERTIES;
        } else if (flags.contains("json")) {
            ext = Storage.EXT_JSON;
        } else if (flags.contains("xml")) {
            ext = Storage.EXT_XML;
        } else if (flags.contains("yaml")) {
            ext = Storage.EXT_YAML;
        }
        if (src.endsWith("/") && !recursive) {
            throw new ProcedureException("source path cannot be an index (unless recursive)");
        } else if (src.endsWith("/") && !dst.endsWith("/")) {
            throw new ProcedureException("destination path must also be an index");
        } else if (StringUtils.startsWithIgnoreCase(src, dst)) {
            throw new ProcedureException("source and destination paths cannot overlap");
        } else if (StringUtils.startsWithIgnoreCase(dst, src)) {
            throw new ProcedureException("source and destination paths cannot overlap");
        } else if (!src.endsWith("/") && dst.endsWith("/")) {
            dst += StringUtils.substringAfterLast("/" + src, "/");
        }
        LOG.info("copying storage path " + src + " to " + dst);
        return Boolean.valueOf(copy(Path.from(src), Path.from(dst), update, ext));
    }

    /**
     * Copies a storage path (index or object) to a new destination.
     *
     * @param src            the source path
     * @param dst            the destination path
     * @param update         the copy-only-on-newer flag
     * @param ext            the optional file extension (data format)
     *
     * @return true if all objects were successfully copied, or
     *         false otherwise
     */
    public static boolean copy(Path src, Path dst, boolean update, String ext) {
        if (src.isIndex()) {
            Storage storage = ApplicationContext.getInstance().getStorage();
            return storage.query(src).paths().map(p -> {
                return copyObject(p, Path.resolve(dst, p.removePrefix(src)), update, ext);
            }).allMatch(res -> res);
        } else {
            return copyObject(src, dst, update, ext);
        }
    }

    /**
     * Copies a single storage object to a new destination.
     *
     * @param src            the source object path
     * @param dst            the destination object path
     * @param update         the copy-only-on-newer flag
     * @param ext            the optional file extension (data format)
     *
     * @return true if the data was successfully copied, or
     *         false otherwise
     */
    public static boolean copyObject(Path src, Path dst, boolean update, String ext) {
        Storage storage = ApplicationContext.getInstance().getStorage();
        if (update) {
            Metadata srcMeta = storage.lookup(src);
            Metadata dstMeta = storage.lookup(dst);
            Date srcTime = (srcMeta == null) ? new Date(0) : srcMeta.lastModified();
            Date dstTime = (dstMeta == null) ? new Date(0) : dstMeta.lastModified();
            if (!dstTime.before(srcTime)) {
                return false;
            }
        }
        Object data = storage.load(src);
        if (data instanceof Binary) {
            try (InputStream is = ((Binary) data).openStream()) {
                File file = FileUtil.tempFile(src.name());
                FileUtil.copy(is, file);
                storage.store(dst, file);
                return true;
            } catch (Exception e) {
                LOG.log(Level.WARNING, "failed to copy " + src + " to " + dst, e);
                return false;
            }
        } else {
            try {
                if (ext != null) {
                    dst = dst.sibling(Storage.objectName(dst.name()) + ext);
                }
                storage.store(dst, data);
                return true;
            } catch (Exception e) {
                LOG.log(Level.WARNING, "failed to copy " + src + " to " + dst, e);
                return false;
            }
        }
    }
}
