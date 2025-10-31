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

package org.rapidcontext.app.proc;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.rapidcontext.core.ctx.Context;
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
 * @author Per Cederberg
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
    @Override
    public Object call(CallContext cx, Bindings bindings)
        throws ProcedureException {

        Path src = Path.from(((String) bindings.getValue("src", "/")).trim());
        if (src.isRoot()) {
            throw new ProcedureException(this, "source path cannot be empty");
        }
        cx.requireAccess(src.toString(), cx.readPermission(1));
        Path dst = Path.from(((String) bindings.getValue("dst", "/")).trim());
        if (dst.isRoot()) {
            throw new ProcedureException(this, "destination path cannot be empty");
        }
        cx.requireWriteAccess(dst.toString());
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
        if (src.isIndex() && !recursive) {
            throw new ProcedureException(this, "source path cannot be an index (unless recursive)");
        } else if (src.isIndex() && !dst.isIndex()) {
            throw new ProcedureException(this, "destination path must also be an index");
        } else if (dst.startsWith(src)) {
            throw new ProcedureException(this, "destination cannot be a descendant of source");
        } else if (!src.isIndex() && dst.isIndex()) {
            dst = dst.sibling(src.name());
        }
        LOG.info("copying storage path " + src + " to " + dst);
        return copy(src, dst, update, ext);
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
            Storage storage = Context.active().storage();
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
        Storage storage = Context.active().storage();
        if (update) {
            Metadata srcMeta = storage.lookup(src);
            Metadata dstMeta = storage.lookup(dst);
            Date srcTime = (srcMeta == null) ? null : srcMeta.modified();
            Date dstTime = (dstMeta == null) ? null : dstMeta.modified();
            if (ObjectUtils.compare(srcTime, dstTime) <= 0) {
                return false;
            }
        }
        Object data = storage.load(src);
        if (data instanceof Binary b) {
            try (InputStream is = b.openStream()) {
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
