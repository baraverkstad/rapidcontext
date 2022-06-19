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

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.app.plugin.PluginManager;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.RootStorage;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.util.RegexUtil;

/**
 * The built-in app list procedure.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class AppListProcedure implements Procedure {

    /**
     * The app object storage path.
     */
    public static final Path PATH_APP = new Path("/app/");

    /**
     * The procedure name constant.
     */
    public static final String NAME = "System.App.List";

    /**
     * The default bindings.
     */
    private Bindings defaults = new Bindings();

    /**
     * Creates a new app list procedure.
     */
    public AppListProcedure() {
        this.defaults.seal();
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
        return "List all available apps.";
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

        CallContext.checkSearchAccess(PATH_APP.toString());
        Storage storage = cx.getStorage();
        Dict[] apps = storage.query(PATH_APP)
            .filterReadAccess()
            .metadatas(Dict.class)
            .map(meta -> loadApp(storage, meta))
            .toArray(Dict[]::new);
        Array res = new Array(apps.length);
        for (Dict app : apps) {
            res.add(app);
        }
        res.sort("name");
        return res;
    }

    private Dict loadApp(Storage storage, Metadata meta) {
        Dict dict = ((Dict) storage.load(meta.path())).copy();
        dict.set("plugin", PluginManager.pluginId(meta));
        Array arr = dict.getArray("resources");
        for (int i = 0; i < arr.size(); i++) {
            Object obj = arr.get(i);
            Dict resource = (obj instanceof Dict) ? (Dict) obj : new Dict();
            String url = resource.getString("url", obj.toString());
            if (!url.contains("//:") && StringUtils.containsAny(url, "*?")) {
                arr.remove(i--);
                for (String str : resolveFiles(storage, url)) {
                    Dict copy = resource.copy();
                    copy.set("url", str);
                    arr.add(copy);
                }
            }
        }
        return dict;
    }

    private String[] resolveFiles(Storage storage, String url) {
        Path base = new Path(RootStorage.PATH_FILES, url);
        while (StringUtils.containsAny(base.toString(), "*?")) {
            base = base.parent();
        }
        Pattern re = Pattern.compile(RegexUtil.fromGlob(url) + "$");
        return storage.query(base)
            .filterReadAccess()
            .paths()
            .filter(path -> re.matcher(path.toString()).find())
            .map(path -> path.toIdent(1))
            .toArray(String[]::new);
    }
}
