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

import java.util.Objects;
import java.util.stream.Stream;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.model.AppStorage;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.Index;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.type.Plugin;
import org.rapidcontext.core.type.Procedure;

/**
 * The built-in plug-in list procedure.
 *
 * @author Jonas Ekstrand
 * @author Per Cederberg
 */
public class PluginListProcedure extends Procedure {

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public PluginListProcedure(String id, String type, Dict dict) {
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

        CallContext.checkSearchAccess(Plugin.PATH_STORAGE.toString());
        ApplicationContext ctx = ApplicationContext.getInstance();
        AppStorage storage = (AppStorage) cx.getStorage();
        return Array.from(
            storage.mounts(Plugin.PATH_STORAGE)
            .map(mount -> {
                String pluginId = mount.path().name();
                Path storagePath = Plugin.storagePath(pluginId);
                Path instancePath = Plugin.instancePath(pluginId);
                Path configPath = Plugin.configPath(pluginId);
                boolean hasAccess =
                    SecurityContext.hasReadAccess(storagePath.toString()) &&
                    SecurityContext.hasReadAccess(instancePath.toString()) &&
                    SecurityContext.hasReadAccess(configPath.toString());
                if (hasAccess) {
                    Index idx = storage.load(storagePath, Index.class);
                    Plugin instance = storage.load(instancePath, Plugin.class);
                    Dict config = storage.load(configPath, Dict.class);
                    if (instance != null) {
                        config = instance.serialize();
                        config.set("_loaded", true);
                    } else if (config != null) {
                        config.set("_loaded", false);
                    } else {
                        config = new Dict();
                        config.set(Plugin.KEY_ID, pluginId);
                        config.set("_loaded", false);
                    }
                    Stream<?> types = idx.indices(false).filter(s -> !s.equals("plugin"));
                    config.set("_content", Array.from(types));
                    config.set("_builtin", ctx.isPluginBuiltIn(pluginId));
                    return config;
                } else {
                    return null;
                }
            })
            .filter(Objects::nonNull)
        );
    }
}
