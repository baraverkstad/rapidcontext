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

package org.rapidcontext.app.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.Metadata;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.RootStorage;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.Session;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.core.web.Request;
import org.rapidcontext.util.FileUtil;

/**
 * An app web service. This service extends the file web service with
 * a RapidContext API and a default app launcher page. All provided
 * APIs are available under the "rapidcontext/" sub-path.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class AppWebService extends FileWebService {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(AppWebService.class.getName());

    /**
     * The dictionary key for the app identifier.
     */
    public static final String KEY_APP = "app";

    /**
     * The dictionary key for the login app identifier.
     */
    public static final String KEY_LOGIN = "login";

    /**
     * The dictionary key for the page title.
     */
    public static final String KEY_TITLE = "title";

    /**
     * The dictionary key for the page language meta-data.
     */
    public static final String KEY_LANG = "lang";

    /**
     * The dictionary key for the page viewport meta-data.
     */
    public static final String KEY_VIEWPORT = "viewport";

    /**
     * The dictionary key for additional HTML headers.
     */
    public static final String KEY_HEADER = "header";

    /**
     * The regular expression for checking for a file version number.
     */
    private static final Pattern RE_VERSION = Pattern.compile("\\d\\.\\d");

    /**
     * The log web service used for the "rapidcontext/log" URL.
     */
    protected LogWebService logger;

    /**
     * The status web service used for the "rapidcontext/status" URL.
     */
    protected StatusWebService statusService;

    /**
     * The procedure web service used for the "rapidcontext/procedure/" URLs.
     */
    protected ProcedureWebService procedureService;

    /**
     * The storage web service used for the "rapidcontext/storage/" URLs.
     */
    protected StorageWebService storageService;

    /**
     * Returns the platform version number.
     *
     * @return the platform version number
     */
    protected static String version() {
        ApplicationContext ctx = ApplicationContext.getInstance();
        Dict dict = ctx.getStorage().load(ApplicationContext.PATH_PLATFORM, Dict.class);
        return dict.get("version", String.class, "1");
    }

    /**
     * Finds binary files of a specified type from the storage. The
     * file type is both used as a subdirectory (i.e. "files/css")
     * and as a suffix (i.e. "*.css") when performing the search. The
     * returned files will be prefixed with the proper path (e.g.
     * "rapidcontext/files/") relative to the specified base path for
     * the web service. The current platform version will be added as
     * a parameter to the file URL:s.
     *
     * @param type           the file type to find
     * @param base           the web service root path
     *
     * @return a sorted list of all matching files found in storage
     */
    protected static String[] resources(String type, Path base) {
        Storage storage = ApplicationContext.getInstance().getStorage();
        Path storagePath = Path.resolve(RootStorage.PATH_FILES, type + "/");
        String rootPath = RootStorage.PATH_FILES.toString();
        String basePath = base.toString();
        String ver = version();
        return storage.query(storagePath)
            .filterFileExtension("." + type)
            .paths()
            .map(path -> {
                boolean isVersioned = RE_VERSION.matcher(path.name()).find();
                String file = path.toString();
                if (isVersioned && file.startsWith(basePath)) {
                    return StringUtils.removeStart(file, basePath);
                } else {
                    file = StringUtils.removeStart(file, rootPath);
                    return "rapidcontext/files@" + ver + "/" + file;
                }
            })
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toArray(String[]::new);
    }

    /**
     * Creates a new app web service from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public AppWebService(String id, String type, Dict dict) {
        super(id, type, dict);
        this.dict.setDefault(KEY_APP, null);
        this.dict.setDefault(KEY_TITLE, "RapidContext");
        this.dict.setDefault(KEY_LANG, "en");
        logger = new LogWebService("id", "type", new Dict());
        statusService = new StatusWebService("id", "type", new Dict());
        procedureService = new ProcedureWebService("id", "type", new Dict());
        storageService = new StorageWebService("id", "type", new Dict());
    }

    /**
     * Returns the app identifier for the default launcher.
     *
     * @return the app identifier for the default launcher, or
     *         null if the "index.html" file contains the app
     */
    public String appId() {
        return dict.get(KEY_APP, String.class);
    }

    /**
     * Returns the app identifier for non-authenticated users.
     *
     * @return the login app identifier, or
     *         "login" if not specified
     */
    public String loginId() {
        return dict.get(KEY_LOGIN, String.class, "login");
    }

    /**
     * Returns the title for the HTML web page.
     *
     * @return the configured page title, or
     *         "RapidContext" if not defined
     */
    public String title() {
        return dict.get(KEY_TITLE, String.class, "RapidContext");
    }

    /**
     * Returns the page language ISO code.
     *
     * @return the page language ISO code, or
     *         "en" if not defined
     */
    public String lang() {
        return dict.get(KEY_LANG, String.class, "en");
    }

    /**
     * Returns the page viewport meta data.
     *
     * @return the page viewport meta data, or
     *         a full device width setting if not defined
     */
    public String viewport() {
        return dict.get(KEY_VIEWPORT, String.class, "width=device-width, initial-scale=1");
    }

    /**
     * Returns the list of optional HTML headers.
     *
     * @return the list of HTML headers, or
     *         an empty list if none defined
     */
    public ArrayList<String> headerLines() {
        ArrayList<String> res = new ArrayList<>();
        for (Object o : dict.getArray(KEY_HEADER)) {
            res.add(o.toString());
        }
        return res;
    }

    /**
     * Returns the HTTP methods implemented for the specified
     * request. The OPTIONS or HEAD methods doesn't have to be added
     * to the result (added automatically later).
     *
     * @param request        the request to check
     *
     * @return the array of HTTP method names supported
     *
     * @see #methods(Request)
     */
    @Override
    protected String[] methodsImpl(Request request) {
        if (request.matchPath("rapidcontext/log")) {
            return logger.methodsImpl(request);
        } else if (request.matchPath("rapidcontext/status")) {
            return statusService.methodsImpl(request);
        } else if (request.matchPath("rapidcontext/procedure/")) {
            return procedureService.methodsImpl(request);
        } else if (request.matchPath("rapidcontext/storage/")) {
            return storageService.methodsImpl(request);
        } else if (request.matchPath("rapidcontext/download")) {
            return METHODS_POST;
        } else if (request.matchPath("rapidcontext/upload")) {
            return METHODS_POST;
        } else {
            return METHODS_GET;
        }
    }

    /**
     * Processes a request for this handler. This method assumes
     * local request paths (removal of the mapped URL base).
     *
     * @param request the request to process
     */
    @Override
    public void process(Request request) {
        if (request.matchPath("rapidcontext/log")) {
            logger.process(request);
        } else if (request.matchPath("rapidcontext/status")) {
            statusService.process(request);
        } else if (request.matchPath("rapidcontext/procedure/")) {
            procedureService.process(request);
        } else if (request.matchPath("rapidcontext/storage/")) {
            storageService.process(request);
        } else if (!request.hasResponse()) {
            super.process(request);
        }
    }

    /**
     * Processes an HTTP GET request.
     *
     * @param request        the request to process
     */
    @Override
    protected void doGet(Request request) {
        String baseUrl = StringUtils.removeEnd(request.getUrl(), request.getPath());
        if (request.getPath().startsWith("rapidcontext/files@")) {
            String path = Path.from(request.getPath()).toIdent(2);
            processFile(request, Path.resolve(RootStorage.PATH_FILES, path), true);
        } else if (request.matchPath("rapidcontext/files/")) {
            processFile(request, Path.resolve(RootStorage.PATH_FILES, request.getPath()), true);
        } else if (request.matchPath("rapidcontext/app/")) {
            String appId = StringUtils.removeEnd(request.getPath(), "/");
            processApp(request, appId, baseUrl);
        } else if (appId() != null) {
            processFile(request, Path.resolve(path(), request.getPath()), true);
            if (!request.hasResponse() && !request.getPath().contains(".")) {
                processApp(request, appId(), baseUrl);
            }
        } else {
            super.doGet(request);
        }
    }

    /**
     * Processes an HTTP POST request.
     *
     * @param request        the request to process
     */
    @Override
    protected void doPost(Request request) {
        if (request.matchPath("rapidcontext/download")) {
            processDownload(request);
        } else if (request.matchPath("rapidcontext/upload")) {
            processUpload(request);
        } else {
            super.doPost(request);
        }
    }

    /**
     * Processes an app launch request. This loads the app launcher
     * template from storage and replaces all template variables with
     * their corresponding search results and values.
     *
     * @param request        the request to process
     * @param appId          the app identifier to launch
     * @param baseUrl        the base URL for requests
     */
    protected void processApp(Request request, String appId, String baseUrl) {
        session(request, true);
        Storage storage = ApplicationContext.getInstance().getStorage();
        Metadata meta = storage.lookup(Path.from("/app/" + appId));
        if (meta == null || !meta.isObject(Dict.class)) {
            LOG.warning(this + " misconfigured; app '" + appId + "' not found,");
            appId = null;
        } else if (!SecurityContext.hasReadAccess(meta.path().toString())) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("unauthorized access to app '" + appId + "', launching login");
            }
            appId = loginId();
        }
        Object tpl = storage.load(RootStorage.PATH_FILES.child("index.tmpl", false));
        if (appId != null && tpl instanceof Binary b) {
            try {
                String str = processAppTemplate(b, baseUrl, appId);
                request.sendText(Mime.HTML[0], str);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "failed to launch app: " + appId, e);
                errorInternal(request, e.getMessage());
            }
        } else if (appId == null) {
            errorNotFound(request);
        } else {
            LOG.warning("app template 'index.tmpl' not found");
            errorInternal(request, "app template 'index.tmpl' not found");
        }
    }

    /**
     * Processes an HTML template file for a app and returns the text.
     *
     * @param template       the template file to read
     * @param baseUrl        the base URL for requests
     * @param appId          the app identifier, or null for 'start'
     *
     * @return the processed contents of the HTML template file
     *
     * @throws IOException if the template file couldn't be read
     */
    protected String processAppTemplate(Binary template, String baseUrl, String appId)
    throws IOException {

        StringBuilder res = new StringBuilder();
        try (
            InputStreamReader is = new InputStreamReader(template.openStream(), StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(is);
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                processAppTemplateLine(res, line, baseUrl, appId);
            }
        }
        return res.toString();
    }

    /**
     * Processes a line from the HTMP template file for an app.
     *
     * @param res            the result buffer
     * @param line           the line to process
     * @param baseUrl        the base URL for requests
     * @param appId          the app identifier, or null for 'start'
     */
    private void processAppTemplateLine(StringBuilder res, String line, String baseUrl, String appId) {
        // Simple text replacement
        if (line.contains("%APP_ID%")) {
            if (appId == null || appId.equals("")) {
                line = line.replace("%APP_ID%", "null");
            } else {
                line = line.replace("%APP_ID%", "'" + appId + "'");
            }
        }
        if (line.contains("%TITLE%")) {
            line = line.replace("%TITLE%", title());
        }
        if (line.contains("%LANG%")) {
            line = line.replace("%LANG%", lang());
        }
        if (line.contains("%VIEWPORT%")) {
            line = line.replace("%VIEWPORT%", viewport());
        }
        if (line.contains("%BASE_URL%")) {
            line = line.replace("%BASE_URL%", baseUrl);
        }
        if (line.contains("%BASE_PATH%")) {
            if (RootStorage.PATH_FILES.equals(path())) {
                // Skip this line, no config needed
                return;
            }
            String str = StringUtils.removeStart(path().toString(),
                                                 RootStorage.PATH_FILES.toString());
            line = line.replace("%BASE_PATH%", str);
        }

        // Complex text replacement & printout
        if (line.contains("%HEADER%")) {
            for (String str : headerLines()) {
                res.append(line.replace("%HEADER%", str));
                res.append("\n");
            }
        } else if (line.contains("%FILES%")) {
            String str = "rapidcontext/files@" + version();
            res.append(line.replace("%FILES%", str));
        } else if (line.contains("%JS_FILES%")) {
            for (String str : resources("js", path())) {
                res.append(line.replace("%JS_FILES%", str));
                res.append("\n");
            }
        } else if (line.contains("%CSS_FILES%")) {
            for (String str : resources("css", path())) {
                res.append(line.replace("%CSS_FILES%", str));
                res.append("\n");
            }
        } else {
            res.append(line);
            res.append("\n");
        }
    }

    /**
     * Processes a file download request. This is used when file data
     * is POST:ed to the special download URL, which makes the handler
     * send the data back with the specified MIME type. Optionally, the
     * "Content-Disposition" header is also set to force the browser
     * "save as" dialog.
     *
     * @param request        the request to process
     */
    protected void processDownload(Request request) {
        String name = request.getParameter("fileName", request.getPath());
        String data = request.getParameter("fileData");
        if (data == null) {
            errorBadRequest(request, "Missing 'fileData' parameter");
        } else {
            if (request.getParameter("download") != null) {
                if (name.indexOf("/") >= 0) {
                    name = StringUtils.substringAfterLast(name, "/");
                }
                String disp = "attachment";
                if (name.length() > 0) {
                    disp += "; filename=" + name;
                }
                request.setResponseHeader("Content-Disposition", disp);
            }
            String mimeType = request.getParameter("mimeType", Mime.type(name));
            request.sendText(mimeType, data);
        }
    }

    /**
     * Processes a file upload request. This is used when files are
     * POST:ed to the special upload URL.
     *
     * @param request        the request to process
     */
    protected void processUpload(Request request) {
        Session session = Session.activeSession.get();
        if (session == null) {
            errorUnauthorized(request);
            return;
        }
        try {
            FileItemStream stream = request.getNextFile();
            if (stream == null) {
                errorBadRequest(request, "Missing file data");
                return;
            }
            String fileName = stream.getName();
            if (fileName.lastIndexOf("/") >= 0) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
            if (fileName.lastIndexOf("\\") >= 0) {
                fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
            }
            String fileId = request.getPath();
            while (fileId != null && fileId.startsWith("/")) {
                fileId = fileId.substring(1);
            }
            if (fileId == null || fileId.trim().length() == 0) {
                fileId = fileName;
            }
            File file = FileUtil.tempFile(fileName);
            try (InputStream is = stream.openStream()) {
                FileUtil.copy(is, file);
            }
            session.addFile(fileId, file);
            request.sendText(Mime.TEXT[0], "Session file " + fileId + " uploaded");
        } catch (IOException e) {
            LOG.log(Level.WARNING, "failed to process file upload", e);
            errorBadRequest(request, e.getMessage());
        }
    }
}
