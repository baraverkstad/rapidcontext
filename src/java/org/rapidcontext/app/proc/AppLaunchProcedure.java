/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2026 Per Cederberg. All rights reserved.
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

import org.apache.commons.lang3.Strings;
import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.model.ApiUtil;
import org.rapidcontext.app.model.AuthHelper;
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
import org.rapidcontext.core.type.Role;
import org.rapidcontext.core.type.Session;
import org.rapidcontext.util.RegexUtil;

/**
 * The built-in app launch procedure.
 *
 * @author Per Cederberg
 */
public class AppLaunchProcedure extends Procedure {

    /**
     * The class logger.
     */
    private static final Logger LOG = Logger.getLogger(AppLaunchProcedure.class.getName());

    /**
     * The app object storage path.
     */
    public static final Path PATH_APP = Path.from("/app/");

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id   the object identifier
     * @param type the object type name
     * @param dict the serialized representation
     */
    public AppLaunchProcedure(String id, String type, Dict dict) {
        super(id, type, dict);
    }

    /**
     * Executes a call of this procedure in the specified context
     * and with the specified call bindings.
     *
     * @param cx       the procedure call context
     * @param bindings the call bindings to use
     *
     * @return the result of the call, or
     *         null if the call produced no result
     *
     * @throws ProcedureException if the call execution caused an
     *                            error
     */
    @Override
    public Object call(CallContext cx, Bindings bindings)
            throws ProcedureException {

        String appId = (String) bindings.getValue("app");
        if (appId.isBlank()) {
            throw new ProcedureException("missing app identifier");
        }
        Path path = Path.resolve(PATH_APP, appId);
        cx.requireReadAccess(path.toString());
        Metadata meta = cx.storage().lookup(path);
        if (meta == null) {
            throw new ProcedureException("app not found: " + appId);
        }
        return loadApp(cx, path, meta.id());
    }

    /**
     * Loads an app from the storage and returns a normalized copy.
     *
     * @param cx       the procedure call context
     * @param path     the app storage path
     * @param id       the app identifier
     *
     * @return the app dictionary
     */
    private Dict loadApp(CallContext cx, Path path, String id) {
        Dict dict = cx.storage().load(path, Dict.class).copy();
        if (!dict.containsKey(KEY_ID)) {
            LOG.warning("deprecated: app missing id: " + path);
            dict.set(KEY_ID, id);
        }
        dict.set("resources",
            dict.getArray("resources").stream()
            .flatMap(o -> resources(cx.storage(), ApiUtil.options("url", o)))
            .map(d -> d.set("type", resourceType(d)))
            .filter(d -> d.get("type", String.class) != null)
            .collect(Array::new, Array::add, Array::addAll)
        );
        dict.set("procedures",
            dict.getArray("procedures").stream()
            .map(o -> procedure(cx.session(), id, ApiUtil.options("id", o)))
            .collect(Array::new, Array::add, Array::addAll)
        );
        return dict;
    }

    /**
     * Returns the normalized app resources. This will resolve any
     * glob patterns in the resource URL.
     *
     * @param storage        the storage to use
     * @param res            the resource dictionary
     *
     * @return a stream of resource dictionaries
     */
    private Stream<Dict> resources(Storage storage, Dict res) {
        String url = res.get("url", String.class);
        if (url == null || url.isBlank() || url.contains(":") || url.startsWith("/")) {
            return Stream.of(res);
        } else {
            Path base = Path.resolve(RootStorage.PATH_FILES, url);
            while (Strings.CI.containsAny(base.toString(), "*", "?")) {
                base = base.parent();
            }
            Pattern re = Pattern.compile(RegexUtil.fromGlob(url) + "$");
            String cache = ApplicationContext.active().cachePath();
            int start = RootStorage.PATH_FILES.length();
            return storage.query(base)
                    .filterAccess(Role.PERM_READ)
                    .filter(p -> re.matcher(p.toString()).find())
                    .paths()
                    .map(p -> res.copy().set("url", cache + "/" + p.toIdent(start)));
        }
    }

    /**
     * Returns a normalized resource type (if possible).
     *
     * @param res            the resource dictionary
     *
     * @return the resource type
     */
    private String resourceType(Dict res) {
        String type = res.get("type", String.class);
        String url = res.get("url", String.class, "").toLowerCase();
        boolean isJs = type == null && Strings.CI.endsWithAny(url, ".js");
        boolean isCss = type == null && Strings.CI.endsWithAny(url, ".css");
        boolean isData = type == null && Strings.CI.endsWithAny(url, ".json", ".xml");
        if (Strings.CI.equalsAny(type, "code") || isJs) {
            return "code";
        } else if (Strings.CI.equalsAny(type, "js", "javascript")) {
            LOG.warning("deprecated: app resource type '" + type + "' is deprecated, use 'code' instead");
            return "code";
        } else if (type == null && Strings.CI.endsWithAny(url, ".mjs")) {
            return "module";
        } else if (Strings.CI.equalsAny(type, "style", "css") || isCss) {
            return "style";
        } else if (type == null && Strings.CI.endsWithAny(url, "ui.xml")) {
            return "ui";
        } else if (Strings.CI.equalsAny(type, "data") || isData) {
            return "data";
        } else if (Strings.CI.equalsAny(type, "json", "xml")) {
            LOG.warning("deprecated: app resource type '" + type + "' is deprecated, use 'data' instead");
            return "data";
        } else if (type == null && Strings.CI.endsWithAny(url, ".gif", ".jpg", ".jpeg", ".png", ".svg")) {
            return "icon";
        } else if (type == null && Strings.CI.endsWithAny(url, ".htm", ".html", ".md", ".markdown")) {
            return "doc";
        } else {
            return type;
        }
    }

    /**
     * Returns a normalized procedure object (if possible).
     *
     * @param session        the session to use
     * @param appId          the app identifier
     * @param proc           the procedure mapping dictionary
     *
     * @return the procedure object
     */
    private Object procedure(Session session, String appId, Dict proc) {
        if (session != null) {
            appId = Strings.CS.removeStart(appId, "/");
            String procId = proc.get("id", String.class);
            String token = AuthHelper.createProcToken(session, appId, procId);
            return proc.set("token", token);
        } else {
            return proc;
        }
    }
}
