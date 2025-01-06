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

import java.util.logging.Logger;
import java.util.regex.Pattern;
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
import org.rapidcontext.core.storage.RootStorage;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.Procedure;
import org.rapidcontext.util.RegexUtil;

/**
 * The built-in app list procedure.
 *
 * @author Per Cederberg
 */
public class AppListProcedure extends Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG = Logger.getLogger(AppListProcedure.class.getName());

    /**
     * The app object storage path.
     */
    public static final Path PATH_APP = Path.from("/app/");

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public AppListProcedure(String id, String type, Dict dict) {
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

        CallContext.checkSearchAccess(PATH_APP.toString());
        Storage storage = cx.getStorage();
        return Array.from(
            storage.query(PATH_APP)
            .filterAccess(cx.readPermission(1))
            .metadatas(Dict.class)
            .map(meta -> loadApp(storage, meta, cx.readPermission(1)))
        );
    }

    private Dict loadApp(Storage storage, Metadata meta, String permission) {
        Dict dict = storage.load(meta.path(), Dict.class).copy();
        if (!dict.containsKey(KEY_ID)) {
            dict.set(KEY_ID, meta.id());
            LOG.warning("deprecated: app " + meta.id() + ": missing 'id' property");
        }
        Array arr = new Array();
        for (Object o : dict.getArray("resources")) {
            Dict res = (o instanceof Dict d) ? d : new Dict();
            String url = (o == res) ? res.get("url", String.class) : o.toString();
            if (url == null) {
                arr.add(res);
            } else {
                resources(storage, res, url, permission).forEach(d -> arr.add(d));
            }
        }
        dict.set("resources", arr);
        return dict;
    }

    private Stream<Dict> resources(Storage storage, Dict res, String url, String perm) {
        if (url.contains(":") || url.startsWith("/")) {
            return Stream.of(res.copy().set("url", url));
        } else {
            Path base = Path.resolve(RootStorage.PATH_FILES, url);
            while (StringUtils.containsAny(base.toString(), "*?")) {
                base = base.parent();
            }
            Pattern re = Pattern.compile(RegexUtil.fromGlob(url) + "$");
            String cache = ApplicationContext.active().cachePath();
            int start = RootStorage.PATH_FILES.length();
            // Search permission granted by default (not yet configurable)
            return storage.query(base)
                .filterAccess(perm)
                .filter(p -> re.matcher(p.toString()).find())
                .paths()
                .map(p -> res.copy().set("url", cache + "/" + p.toIdent(start)));
        }
    }
}
