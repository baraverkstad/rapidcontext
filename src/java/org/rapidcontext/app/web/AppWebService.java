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

package org.rapidcontext.app.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.plugin.PluginManager;
import org.rapidcontext.app.proc.StatusProcedure;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.JsonSerializer;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.security.SecurityContext;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.type.Session;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.core.web.Request;
import org.rapidcontext.util.FileUtil;

/**
 * An app web service. This service extends the file web service with
 * a RapidContext API and a default app launcher page. The API pages
 * are available under the "/rapidcontext/" path.
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
     * The log web service used for the "rapidcontext/log" URLs.
     */
    protected LogWebService logger;

    /**
     * The procedure web service used for the "rapidcontext/procedure/" URLs.
     */
    protected ProcedureWebService procedure;

    /**
     * The storage web service used for the "rapidcontext/storage/" URLs.
     */
    protected StorageWebService storage;

    /**
     * Returns the platform version number.
     *
     * @return the platform version number
     */
    protected static String version() {
        ApplicationContext ctx = ApplicationContext.getInstance();
        Dict dict = (Dict) ctx.getStorage().load(PluginManager.PATH_INFO);
        return dict.getString("version", "1");
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
        Path storagePath = new Path(PATH_FILES, type + "/");
        String rootPath = PATH_FILES.toString();
        String basePath = base.toString();
        String ver = version();
        return storage.query(storagePath)
            .filterExtension("." + type)
            .paths()
            .map(path -> {
                String file = path.toString();
                if (file.startsWith(basePath)) {
                    file = StringUtils.removeStart(file, basePath);
                } else {
                    file = StringUtils.removeStart(file, rootPath);
                    file = "rapidcontext/files/" + file;
                }
                return file + "?" + ver;
            })
            .sorted()
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
        dict.set(KEY_APP, appId());
        dict.set(KEY_TITLE, title());
        dict.set(KEY_LANG, lang());
        logger = new LogWebService("id", "type", new Dict());
        procedure = new ProcedureWebService("id", "type", new Dict());
        storage = new StorageWebService("id", "type", new Dict());
    }

    /**
     * Returns the app identifier for the default launcher.
     *
     * @return the app identifier for the default launcher, or
     *         "start" if no app identifier was specified
     */
    public String appId() {
        return dict.getString(KEY_APP, "start");
    }

    /**
     * Returns the title for the HTML web page.
     *
     * @return the configured page title, or
     *         "RapidContext" if not defined
     */
    public String title() {
        return dict.getString(KEY_TITLE, "RapidContext");
    }

    /**
     * Returns the page language ISO code.
     *
     * @return the page language ISO code, or
     *         "en" if not defined
     */
    public String lang() {
        return dict.getString(KEY_LANG, "en");
    }

    /**
     * Returns the page viewport meta data.
     *
     * @return the page viewport meta data, or
     *         "width=700" if not defined
     */
    public String viewport() {
        return dict.getString(KEY_VIEWPORT, "width=device-width, initial-scale=1");
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
    protected String[] methodsImpl(Request request) {
        if (request.matchPath("rapidcontext/log")) {
            return logger.methodsImpl(request);
        } else if (request.matchPath("rapidcontext/procedure/")) {
            return procedure.methodsImpl(request);
        } else if (request.matchPath("rapidcontext/storage/")) {
            return storage.methodsImpl(request);
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
    public void process(Request request) {
        if (request.matchPath("rapidcontext/log")) {
            logger.process(request);
        } else if (request.matchPath("rapidcontext/procedure/")) {
            procedure.process(request);
        } else if (request.matchPath("rapidcontext/storage/")) {
            storage.process(request);
        } else if (!request.hasResponse()) {
            super.process(request);
        }
    }

    /**
     * Processes an HTTP GET request.
     *
     * @param request        the request to process
     */
    protected void doGet(Request request) {
        super.doGet(request);
        if (!request.hasResponse()) {
            String path = request.getPath();
            String baseUrl = StringUtils.removeEnd(request.getUrl(), path);
            boolean isRoot = path.equals("") || path.startsWith("index.htm");
            if (request.matchPath("rapidcontext/files/")) {
                processFile(request, new Path(PATH_FILES, request.getPath()));
            } else if (request.matchPath("rapidcontext/app/")) {
                String appId = StringUtils.removeEnd(request.getPath(), "/");
                processApp(request, appId, baseUrl);
            } else if (request.matchPath("rapidcontext/status")) {
                processStatus(request);
            } else if (isRoot) {
                processApp(request, appId(), baseUrl);
            }
        }
    }

    /**
     * Processes an HTTP POST request.
     *
     * @param request        the request to process
     */
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
     * Processes an HTML app launcher request. This method loads the
     * app launcher template from storage and replaces all template
     * variables with their corresponding search results and values.
     *
     * @param request        the request to process
     * @param appId          the app identifier to launch (or null)
     * @param baseUrl        the base URL for requests
     */
    protected void processApp(Request request, String appId, String baseUrl) {
        session(request, true);
        Storage storage = ApplicationContext.getInstance().getStorage();
        Path appPath = new Path("/app/" + appId);
        Object app = storage.load(appPath);
        if (app instanceof Dict) {
            String loginAppId = ((Dict) app).getString("login", "login");
            if (!SecurityContext.hasReadAccess(appPath.toString())) {
                String msg = "unauthorized access to app " + appId +
                             ", launching login app " + loginAppId;
                LOG.fine(msg);
                appId = loginAppId;
            }
        } else {
            appId = null;
        }
        Object obj = storage.load(PATH_FILES.child("index.tmpl", false));
        if (obj instanceof Binary) {
            Binary template = (Binary) obj;
            try {
                String str = processAppTemplate(template, baseUrl, appId);
                request.sendText(Mime.HTML[0], str);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "failed to launch app: " + appId, e);
                errorInternal(request, e.getMessage());
            }
        } else {
            String str = "app template 'index.tmpl' not found";
            LOG.log(Level.WARNING, "app template 'index.tmpl' not found");
            errorInternal(request, str);
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
            InputStreamReader is = new InputStreamReader(template.openStream(), "UTF-8");
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
            if (PATH_FILES.equals(path())) {
                // Skip this line, no config needed
                return;
            }
            String str = StringUtils.removeStart(path().toString(),
                                                 PATH_FILES.toString());
            line = line.replace("%BASE_PATH%", str);
        }

        // Complex text replacement & printout
        if (line.contains("%HEADER%")) {
            for (String str : headerLines()) {
                res.append(line.replace("%HEADER%", str));
                res.append("\n");
            }
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
            session.removeFile(fileId);
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

    /**
     * Processes a system status request.
     *
     * @param request        the request to process
     */
    protected void processStatus(Request request) {
        try {
            ApplicationContext ctx = ApplicationContext.getInstance();
            Object[] args = ArrayUtils.EMPTY_OBJECT_ARRAY;
            String source = "web [" + request.getRemoteAddr() + "]";
            Object obj = ctx.execute(StatusProcedure.NAME, args, source, null);
            request.sendText(Mime.JSON[0], JsonSerializer.serialize(obj, true));
        } catch (ProcedureException e) {
            LOG.warning("error in system status check: " + e.getMessage());
            Dict res = new Dict();
            res.set("error", e.getMessage());
            request.sendText(Mime.JSON[0], JsonSerializer.serialize(res, true));
        }
    }
}
