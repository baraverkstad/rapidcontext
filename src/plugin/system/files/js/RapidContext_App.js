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

/**
 * The base RapidContext namespace.
 * @namespace RapidContext
 * @private
 */
if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}

/**
 * Provides functions for application bootstrap and server communication.
 * @namespace RapidContext.App
 */
if (typeof(RapidContext.App) == "undefined") {
    RapidContext.App = {};
}

/**
 * Initializes the platform, API:s and RapidContext UI. If an app
 * identifier is provided, the default platform UI will not be
 * created. Instead the app will be launched with the root document
 * as its UI container.
 *
 * @param {string} [app] the app id to start
 *
 * @return {Promise} a promise that will resolve when initialization has
 *         either completed or failed
 */
RapidContext.App.init = function (app) {
    // Initialize UI
    RapidContext.Log.context("RapidContext.App.init()");
    console.info("Initializing RapidContext");
    document.body.innerHTML = "";
    document.body.append(new RapidContext.Widget.Overlay({ message: "Loading..." }));

    // Load platform data (into cache)
    const cachedData = [
        RapidContext.App.callProc("system/status"),
        RapidContext.App.callProc("system/session/current"),
        RapidContext.App.callProc("system/app/list")
    ];

    // Launch app
    return Promise.all(cachedData)
        .then(() => {
            const hourly = 60 * 60 * 1000;
            setInterval(() => RapidContext.App.callProc("system/session/current"), hourly);
            const isStart = !app || app === "start";
            return RapidContext.App.startApp(app ?? "start")
                .catch((err) => Promise.reject(RapidContext.UI.showError(err)))
                .catch(() => isStart ? null : RapidContext.App.startApp("start"));
        })
        .catch((err) => Promise.reject(RapidContext.UI.showError(err)))
        .finally(() => RapidContext.Log.context(null));
};

/**
 * Returns an object with status information about the platform and
 * currently loaded environment. The object returned is a copy of
 * the internal data structure and can be modified without affecting
 * the real data.
 *
 * @return {Object} the status data object
 */
RapidContext.App.status = function () {
    return { ...RapidContext.App._Cache.status };
};

/**
 * Returns an object with information about the user. The object
 * returned is a copy of the internal data structure and can be
 * modified without affecting the real data.
 *
 * @return {Object} the user data object
 */
RapidContext.App.user = function () {
    return { ...RapidContext.App._Cache.user };
};

/**
 * Returns an array with app launchers. The data returned is an
 * internal data structure and should not be modified.
 *
 * @return {Array} the loaded app launchers (read-only)
 */
RapidContext.App.apps = function () {
    return Object.values(RapidContext.App._Cache.apps);
};

/**
 * Returns an array with running app instances.
 *
 * @return {Array} the array of app instances
 */
RapidContext.App._instances = function () {
    let res = [];
    const apps = RapidContext.App.apps();
    for (let i = 0; i < apps.length; i++) {
        res = res.concat(apps[i].instances ?? []);
    }
    return res;
};

/**
 * Finds the app launcher from an app instance, class name or
 * launcher. In the last case, the matching cached launcher will be
 * returned.
 *
 * @param {string|Object} app the app id, instance, class name or
 *        launcher
 *
 * @return {Object} the read-only app launcher, or
 *         `null` if not found
 */
RapidContext.App.findApp = function (app) {
    if (app == null) {
        return null;
    }
    const apps = RapidContext.App.apps();
    for (let i = 0; i < apps.length; i++) {
        const l = apps[i];
        if (l.id == app || l.className == app || l.id == app.id) {
            return l;
        }
    }
    return null;
};

/**
 * Creates and starts an app instance. Normally apps are started in the default
 * app tab container or in a provided widget DOM node. By specifying a newly
 * created window object as the parent container, apps can also be launched
 * into separate windows.
 *
 * Note that when a new window is used, the returned deferred will callback
 * immediately with a `null` app instance (normal app communication is not
 * possible cross-window).
 *
 * @param {string|Object} app the app id, class name or launcher
 * @param {Widget/Window} [container] the app container widget or
 *            window, defaults to create a new pane in the app tab
 *            container
 *
 * @return {Promise} a `RapidContext.Async` promise that will
 *         resolve when the app has launched
 *
 * @example
 * // Starts the help app in a new tab
 * RapidContext.App.startApp('help');
 *
 * // Starts the help app in a new window
 * RapidContext.App.startApp('help', window.open());
 */
RapidContext.App.startApp = function (app, container) {
    function loadResource(launcher, res) {
        if (res.type == "code") {
            return RapidContext.App.loadScript(res.url);
        } else if (res.type == "module") {
            return import(new URL(res.url, document.baseURI)).then((mod) => {
                launcher.creator = launcher.creator ?? mod["default"] ?? mod["create"];
            });
        } else if (res.type == "style") {
            return RapidContext.App.loadStyles(res.url);
        } else if (res.type == "ui") {
            return RapidContext.App.loadXML(res.url).then((node) => launcher.ui = node);
        } else if (res.type == "json" && res.id != null) {
            return RapidContext.App.loadJSON(res.url).then((data) => launcher.resource[res.id] = data);
        } else {
            if (res.id != null) {
                launcher.resource[res.id] = res.url;
            }
            return Promise.resolve(res);
        }
    }
    function load(launcher, data) {
        console.info(`Loading app/${launcher.id} resources`, data.resources);
        launcher.resource = {};
        const promises = data.resources.map((res) => loadResource(launcher, res));
        return Promise.all(promises).then(() => {
            launcher.creator = launcher.creator ?? window[launcher.className];
            if (launcher.creator == null) {
                const msg = `App constructor ${launcher.className} not defined`;
                console.error(msg, launcher);
                throw new Error(msg);
            }
            return data;
        });
    }
    function buildUI(parent, ids, ui) {
        const root = ui.documentElement;
        for (let i = 0; i < root.attributes.length; i++) {
            const attr = root.attributes[i];
            if (typeof(parent.setAttrs) === "function") {
                parent.setAttrs({ [attr.name]: attr.value });
            } else if (attr.name === "class") {
                attr.value.split(/\s+/g).forEach((cls) => parent.classList.add(cls));
            } else {
                parent.setAttribute(attr.name, attr.value);
            }
        }
        const arr = Array.from(ui.documentElement.childNodes);
        parent.append(...arr.map((o) => RapidContext.UI.create(o)).filter(Boolean));
        parent.querySelectorAll("[id]").forEach((el) => ids[el.attributes.id.value] = el);
    }
    function launch(launcher, ui) {
        RapidContext.Log.context(`RapidContext.App.startApp(${launcher.id})`);
        return launcher.starter = RapidContext.App.callProc("system/app/launch", [launcher.id])
            .then((data) => launcher.creator ? data : load(launcher, data))
            .then((data) => {
                console.info(`Starting app/${launcher.id}`, launcher);
                /* eslint new-cap: "off" */
                const instance = new launcher.creator();
                Object.assign(instance, {
                    id: data.id,
                    name: data.name,
                    resource: launcher.resource,
                    proc: RapidContext.Procedure.mapAll(data.procedures),
                    ui: ui
                });
                launcher.instances.push(instance);
                MochiKit.Signal.disconnectAll(ui.root, "onclose");
                const halt = () => RapidContext.App.stopApp(instance);
                MochiKit.Signal.connect(ui.root, "onclose", halt);
                if (launcher.ui != null) {
                    buildUI(ui.root, ui, launcher.ui);
                }
                ui.overlay.hide();
                ui.overlay.setAttrs({ message: "Working..." });
                return RapidContext.Async.wait(0) // Wait for initial UI events
                    .then(() => RapidContext.App.callApp(instance, "start"));
            })
            .catch((err) => {
                console.error("Failed to start app", err);
                MochiKit.Signal.disconnectAll(ui.root, "onclose");
                const div = document.createElement("div");
                div.append(String(err));
                ui.root.append(div);
                if (err.url) {
                    const link = document.createElement("a");
                    link.className = "block mt-2";
                    link.setAttribute("href", err.url);
                    link.append(err.url);
                    ui.root.append(link);
                }
                ui.overlay.hide();
                return Promise.reject(err);
            })
            .finally(() => {
                delete launcher.starter;
                RapidContext.Log.context(null);
            });
    }
    function moveAppToStart(instance, elems) {
        const start = RapidContext.App.findApp("start").instances[0];
        const opts = { title: instance.name, closeable: false, background: true };
        const ui = start.initAppPane(null, opts);
        ui.root.removeAll();
        ui.root.addAll(elems);
    }
    return new RapidContext.Async((resolve, reject) => {
        const launcher = RapidContext.App.findApp(app);
        if (launcher == null) {
            const msg = "No matching app launcher found";
            console.error(msg, app);
            throw new Error([msg, ": ", app].join(""));
        }
        const instances = RapidContext.App._instances();
        const start = RapidContext.App.findApp("start");
        if ($.isWindow(container)) {
            // Launch app into separate window/tab
            const href = `rapidcontext/app/${launcher.id}`;
            container.location.href = new URL(href, document.baseURI).toString();
            resolve();
        } else if (start?.instances?.length > 0) {
            // Launch app into start app tab
            const paneOpts = { title: launcher.name, closeable: (launcher.launch != "once") };
            const paneUi = start.instances[0].initAppPane(container, paneOpts);
            resolve(launch(launcher, paneUi));
        } else if (instances.length > 0) {
            // Switch from single-app to multi-app mode
            const elems = Array.from(document.body.childNodes);
            const overlay = new RapidContext.Widget.Overlay({ message: "Loading..." });
            document.body.insertBefore(overlay, document.body.childNodes[0]);
            const ui = { root: document.body, overlay: overlay };
            const move = () => moveAppToStart(instances[0], elems);
            const recall = () => (launcher.id === "start") ? true : RapidContext.App.startApp(launcher.id);
            resolve(launch(start, ui).then(move).then(recall));
        } else {
            // Launch single-app mode
            const ui = { root: document.body, overlay: document.body.childNodes[0] };
            resolve(launch(launcher, ui));
        }
    });
};

/**
 * Stops an app instance. If only the class name or launcher is specified, the
 * most recently created instance will be stopped.
 *
 * @param {string|Object} app the app id, instance, class name or launcher
 *
 * @return {Promise} a `RapidContext.Async` promise that will
 *         resolve when the app has been stopped
 */
RapidContext.App.stopApp = function (app) {
    return new RapidContext.Async((resolve, reject) => {
        const launcher = RapidContext.App.findApp(app);
        if (!launcher || launcher.instances.length <= 0) {
            const msg = "No running app instance found";
            console.error(msg, app);
            throw new Error([msg, ": ", app].join(""));
        }
        console.info(`Stopping app ${launcher.name}`);
        const pos = launcher.instances.indexOf(app);
        if (pos < 0) {
            app = launcher.instances.pop();
        } else {
            launcher.instances.splice(pos, 1);
        }
        app.stop();
        if (app.ui.root != null) {
            RapidContext.Widget.destroyWidget(app.ui.root);
        }
        for (const k in app.ui) {
            delete app.ui[k];
        }
        for (const n in app) {
            delete app[n];
        }
        MochiKit.Signal.disconnectAllTo(app);
        resolve();
    });
};

/**
 * Performs an asynchronous call to a method in an app. If only the class name
 * or launcher is specified, the most recently created instance will be used.
 * If no instance is running, one will be started. Also, before calling the app
 * method, the app UI will be focused.
 *
 * @param {string|Object} app the app id, instance, class name or launcher
 * @param {string} method the app method name
 * @param {Mixed} [args] additional parameters sent to method
 *
 * @return {Promise} a `RapidContext.Async` promise that will
 *         resolve with the result of the call on success
 */
RapidContext.App.callApp = function (app, method) {
    const args = Array.from(arguments).slice(2);
    return new RapidContext.Async((resolve, reject) => {
        const launcher = RapidContext.App.findApp(app);
        if (launcher == null) {
            const msg = "No matching app launcher found";
            console.error(msg, app);
            throw new Error([msg, ": ", app].join(""));
        }
        const starter = launcher.instances[0] ?? launcher.starter ?? RapidContext.App.startApp(app);
        const promise = Promise.resolve(starter)
            .then(() => {
                RapidContext.Log.context(`RapidContext.App.callApp(${launcher.id},${method})`);
                const pos = launcher.instances.indexOf(app);
                const instance = (pos >= 0) ? app : launcher.instances[launcher.instances.length - 1];
                const child = instance.ui.root;
                const parent = child.parentNode.closest(".widget");
                if (typeof(parent?.selectChild) == "function") {
                    parent.selectChild(child);
                }
                const methodName = `${launcher.className}.${method}`;
                if (instance[method] == null) {
                    const msg = `No app method ${methodName} found`;
                    console.error(msg);
                    throw new Error(msg);
                }
                console.log(`Calling app method ${methodName}`, args);
                try {
                    return instance[method](...args);
                } catch (e) {
                    const reason = `Caught error in ${methodName}`;
                    console.error(reason, e);
                    throw new Error(`${reason}: ${e.toString()}`, { cause: e });
                }
            })
            .finally(() => RapidContext.Log.context(null));
        resolve(promise);
    });
};

/**
 * Performs an asynchronous procedure call.
 *
 * @param {string} name the procedure name
 * @param {Array|Object} [args] the arguments array, dictionary, or `null`
 * @param {Object} [opts] the procedure call options
 * @param {boolean} [opts.session] the HTTP session required flag
 * @param {boolean} [opts.trace] the procedure call trace flag
 * @param {number} [opts.timeout] the timeout in milliseconds, default is 60s
 *
 * @return {Promise} a `RapidContext.Async` promise that will
 *         resolve with the response data on success
 */
RapidContext.App.callProc = function (name, args, opts) {
    args = args ?? [];
    opts = opts ?? {};
    console.log(`Call request ${name}`, args);
    let params = RapidContext.Data.map(RapidContext.Encode.toJSON, args);
    if (Array.isArray(params)) {
        params = RapidContext.Data.object(params.map((val, idx) => [`arg${idx}`, val]));
    }
    params["system:session"] = !!opts.session;
    params["system:trace"] = !!opts.trace || ["all", "log"].includes(RapidContext.Log.level());
    params["system:token"] = opts.token;
    const url = `rapidcontext/procedure/${name}`;
    const options = { method: "POST", timeout: opts.timeout ?? 60000 };
    return RapidContext.App.loadJSON(url, params, options).then((res) => {
        if (res.trace) {
            console.log(`${name} trace:`, res.trace);
        }
        if (res.error) {
            console.info(`${name} error:`, res.error);
            RapidContext.App._Cache.handleError(res.error);
            throw new Error(res.error);
        } else {
            console.log(`${name} response:`, res.data);
            if (name.startsWith("system/")) {
                RapidContext.App._Cache.update(name, res.data);
            }
            return res.data;
        }
    });
};

/**
 * Performs an asynchronous login. If the current session is already bound to a
 * user, that session will be terminated and a new one will be created. If an
 * authentication token is specified, the login and password fields are not
 * used (can be null).
 *
 * @param {string} login the user login name or email address
 * @param {string} password the password to authenticate the user
 * @param {string} [token] the authentication token to identify user/password
 *
 * @return {Promise} a `RapidContext.Async` promise that resolves when
 *         the authentication has either succeeded or failed
 */
RapidContext.App.login = function (login, password, token) {
    function searchLogin() {
        const proc = "system/user/search";
        return RapidContext.App.callProc(proc, [login]).then((user) => {
            if (user?.id) {
                return login = user.id;
            } else {
                throw new Error("no user with that email address");
            }
        });
    }
    function getNonce() {
        const proc = "system/session/current";
        const opts = { session: true };
        return RapidContext.App.callProc(proc, [], opts).then((session) => session.nonce);
    }
    function passwordAuth(nonce) {
        const realm = RapidContext.App.status().realm;
        let hash = CryptoJS.MD5(`${login}:${realm}:${password}`);
        hash = CryptoJS.MD5(`${hash.toString()}:${nonce}`).toString();
        const args = [login, nonce, hash];
        return RapidContext.App.callProc("system/session/authenticate", args);
    }
    function tokenAuth() {
        return RapidContext.App.callProc("system/session/authenticatetoken", [token]);
    }
    function verifyAuth(res) {
        if (!res.success || res.error) {
            console.info("login failed", login, res.error);
            throw new Error(res.error ?? "authentication failed");
        }
    }
    let promise = RapidContext.Async.wait(0);
    const user = RapidContext.App.user();
    if (user?.id) {
        promise = promise.then(RapidContext.App.logout);
    }
    if (token) {
        return promise.then(tokenAuth).then(verifyAuth);
    } else {
        if (/@/.test(login)) {
            promise = promise.then(searchLogin);
        }
        return promise.then(getNonce).then(passwordAuth).then(verifyAuth);
    }
};

/**
 * Performs an asynchronous logout. This function terminates the current
 * session and either reloads the browser window or returns a deferred object
 * that will produce either a `callback` or an `errback` response.
 *
 * @param {boolean} [reload=true] the reload browser flag
 *
 * @return {Promise} a `RapidContext.Async` promise that will
 *         resolve when user is logged out
 */
RapidContext.App.logout = function (reload) {
    const promise = RapidContext.App.callProc("system/session/terminate", [null]);
    if (reload !== false) {
        promise.then(() => window.location.reload());
    }
    return promise;
};

/**
 * Performs an asynchronous HTTP request and parses the JSON response. The
 * request parameters are automatically encoded to query string or JSON format,
 * depending on the `Content-Type` header. The parameters will be sent either
 * in the URL or as the request payload (depending on the HTTP `method`).
 *
 * @param {string} url the URL to request
 * @param {Object} [params] the request parameters, or `null`
 * @param {Object} [opts] the request options, or `null`
 * @param {string} [opts.method] the HTTP method, default is `GET`
 * @param {number} [opts.timeout] the timeout in milliseconds, default is 30s
 * @param {Object} [opts.headers] the specific HTTP headers to use
 *
 * @return {Promise} a `RapidContext.Async` promise that will
 *         resolve with the parsed response JSON on success
 */
RapidContext.App.loadJSON = function (url, params, opts) {
    opts = { responseType: "json", ...opts };
    return RapidContext.App.loadXHR(url, params, opts).then((xhr) => xhr.response);
};

/**
 * Performs an asynchronous HTTP request for a text document. The request
 * parameters are automatically encoded to query string or JSON format,
 * depending on the `Content-Type` header. The parameters will be sent either
 * in the URL or as the request payload (depending on the HTTP `method`).
 *
 * @param {string} url the URL to request
 * @param {Object} [params] the request parameters, or `null`
 * @param {Object} [opts] the request options, or `null`
 * @param {string} [opts.method] the HTTP method, "GET" or "POST"
 * @param {number} [opts.timeout] the timeout in milliseconds, default is 30s
 * @param {Object} [opts.headers] the specific HTTP headers to use
 *
 * @return {Promise} a `RapidContext.Async` promise that will
 *         resolve with the response text on success
 */
RapidContext.App.loadText = function (url, params, opts) {
    opts = { responseType: "text", ...opts };
    return RapidContext.App.loadXHR(url, params, opts).then((xhr) => xhr.response);
};

/**
 * Performs an asynchronous HTTP request for an XML document. The request
 * parameters are automatically encoded to query string or JSON format,
 * depending on the `Content-Type` header. The parameters will be sent either
 * in the URL or as the request payload (depending on the HTTP `method`).
 *
 * @param {string} url the URL to request
 * @param {Object} [params] the request parameters, or `null`
 * @param {Object} [opts] the request options, or `null`
 * @param {string} [opts.method] the HTTP method, "GET" or "POST"
 * @param {number} [opts.timeout] the timeout in milliseconds, default is 30s
 * @param {Object} [opts.headers] the specific HTTP headers to use
 *
 * @return {Promise} a `RapidContext.Async` promise that will
 *         resolve with the parsed response XML document on success
 */
RapidContext.App.loadXML = function (url, params, opts) {
    opts = { responseType: "document", ...opts };
    return RapidContext.App.loadXHR(url, params, opts).then((xhr) => xhr.response);
};

/**
 * Performs an asynchronous HTTP request. The request parameters are
 * automatically encoded to query string or JSON format, depending on the
 * `Content-Type` header. The parameters will be sent either in the URL or as
 * the request payload (depending on the HTTP `method`).
 *
 * @param {string} url the URL to request
 * @param {Object} [params] the request parameters, or `null`
 * @param {Object} [opts] the request options, or `null`
 * @param {string} [opts.method] the HTTP method, default is `GET`
 * @param {number} [opts.timeout] the timeout in milliseconds, default is 30s
 * @param {Object} [opts.headers] the specific HTTP headers to use
 *
 * @return {Promise} a `RapidContext.Async` promise that will
 *         resolve with the XMLHttpRequest instance on success
 */
RapidContext.App.loadXHR = function (url, params, opts) {
    opts = { method: "GET", headers: {}, timeout: 30000, ...opts };
    opts.timeout = (opts.timeout < 1000) ? opts.timeout * 1000 : opts.timeout;
    const hasBody = params && ["PATCH", "POST", "PUT"].includes(opts.method);
    const hasJsonBody = opts.headers["Content-Type"] === "application/json";
    if (!hasBody) {
        const op = url.includes("?") ? "&" : "?";
        url += params ? `${op}${RapidContext.Encode.toUrlQuery(params)}` : "";
    } else if (params && hasBody && hasJsonBody) {
        opts.body = RapidContext.Encode.toJSON(params);
    } else if (params && hasBody) {
        opts.headers["Content-Type"] = "application/x-www-form-urlencoded";
        opts.body = RapidContext.Encode.toUrlQuery(params);
    }
    console.log("Starting XHR loading", url, opts);
    return RapidContext.Async.xhr(url, opts).then(
        (res) => {
            console.log("Completed XHR loading", url);
            return res;
        },
        (err) => {
            const logger = /timeout/i.test(err) ? console.info : console.warn;
            logger("Failed XHR loading", url, err);
            return Promise.reject(err);
        }
    );
};

/**
 * Loads a JavaScript to the current page. The script is loaded by
 * inserting a `<script>` tag in the document `<head>`. Function definitions
 * and values must therefore be stored to global variables by the script to
 * become accessible after loading. If the script is already loaded, the
 * promise will resolve immediately.
 *
 * @param {string} url the URL to the script
 *
 * @return {Promise} a `RapidContext.Async` promise that will
 *         resolve when the script has loaded
 *
 * @see Use dynamic `import()` instead, if supported by the environment.
 */
RapidContext.App.loadScript = function (url) {
    const selector = ["script[src*='", url, "']"].join("");
    if (document.querySelectorAll(selector).length > 0) {
        console.log("script already loaded, skipping", url);
        return RapidContext.Async.wait(0);
    } else {
        console.log("loading script", url);
        return RapidContext.Async.script(url);
    }
};

/**
 * Loads a CSS stylesheet to the current page asynchronously. The
 * stylesheet is loaded by inserting a `<link>` tag in the document `head`.
 * If the stylesheet is already loaded, the promise will resolve immediately.
 *
 * @param {string} url the URL to the stylesheet
 *
 * @return {Promise} a `RapidContext.Async` promise that will
 *         callback when the stylesheet has been loaded
 */
RapidContext.App.loadStyles = function (url) {
    const selector = ["link[href*='", url, "']"].join("");
    if (document.querySelectorAll(selector).length > 0) {
        console.log("stylesheet already loaded, skipping", url);
        return RapidContext.Async.wait(0);
    } else {
        console.log("loading stylesheet", url);
        return RapidContext.Async.css(url);
    }
};

/**
 * Downloads a file to the user desktop. This works by creating a new
 * window or an inner frame which downloads the file from the server.
 * Due to `Content-Disposition` headers being set on the server, the
 * web browser will popup a dialog for the user to save the file.
 * This function can also be used for saving a file that doesn't
 * exist by first posting the file (text) content to the server.
 *
 * @param {string} url the URL or filename to download
 * @param {string} [data] the optional file data (if not available
 *            on the server-side)
 */
RapidContext.App.downloadFile = function (url, data) {
    if (data == null) {
        const op = url.includes("?") ? "&" : "?";
        url += `${op}download`;
        const attrs = {
            src: url,
            border: "0",
            frameborder: "0",
            height: "0",
            width: "0"
        };
        const iframe = RapidContext.UI.create("iframe", attrs);
        document.body.append(iframe);
    } else {
        const name = RapidContext.UI.INPUT({ name: "fileName", value: url });
        const file = RapidContext.UI.INPUT({ name: "fileData", value: data });
        const flag = RapidContext.UI.INPUT({ name: "download", value: "1" });
        const attrs = {
            action: "rapidcontext/download",
            method: "POST",
            target: "_blank",
            style: { display: "none" }
        };
        const form = RapidContext.UI.FORM(attrs, name, file, flag);
        document.body.append(form);
        form.submit();
    }
};

/**
 * Uploads a file to the server. The upload is handled by an
 * asynchronous HTTP request.
 *
 * @param {string} id the upload identifier to use
 * @param {File} file the file object to upload
 * @param {function} [onProgress] the progress event handler
 */
RapidContext.App.uploadFile = function (id, file, onProgress) {
    const formData = new FormData();
    formData.append("file", file);
    const opts = {
        method: "POST",
        body: formData,
        timeout: 300000,
        progress: onProgress,
        log: `${id} upload`
    };
    return RapidContext.Async.xhr(`rapidcontext/upload/${id}`, opts);
};

/**
 * The application data cache. Contains the most recently retrieved
 * data for some commonly used objects in the execution environment.
 */
RapidContext.App._Cache = {
    version: new Date().getTime().toString(16).slice(-8),
    status: null,
    user: null,
    apps: {},

    // Normalizes an app manifest and its resources
    _normalizeApp(app) {
        function toType(type, url) {
            const isJs = !type && /\.js$/i.test(url);
            const isCss = !type && /\.css$/i.test(url);
            if (["code", "js", "javascript"].includes(type) || isJs) {
                return "code";
            } else if (!type && /\.mjs$/i.test(url)) {
                return "module";
            } else if (["style", "css"].includes(type) || isCss) {
                return "style";
            } else if (!type && /\.json$/i.test(url)) {
                return "json";
            } else if (!type && /ui\.xml$/i.test(url)) {
                return "ui";
            } else if (!type && !app.icon && /\.(gif|jpg|jpeg|png|svg)$/i.test(url)) {
                return "icon";
            } else {
                return type;
            }
        }
        function toIcon(res) {
            if (res.url) {
                return $("<img/>").attr({ src: res.url }).addClass("-app-icon").get(0);
            } else if (res.html) {
                const node = $("<span/>").html(res.html).addClass("-app-icon").get(0);
                return node.childNodes.length === 1 ? node.childNodes[0] : node;
            } else if (res["class"]) {
                return $("<i/>").addClass(res["class"]).addClass("-app-icon").get(0);
            } else {
                return null;
            }
        }
        function toResource(res) {
            res = (typeof(res) === "string") ? { url: res } : res;
            res.type = toType(res.type, res.url);
            if (!app.icon && res.type === "icon") {
                app.icon = toIcon(res);
            }
            return res;
        }
        app = { ...app };
        app.launch = app.launch ?? "manual";
        app.resources = [].concat(app.resources).filter(Boolean).map(toResource);
        if (!app.icon) {
            app.icon = toIcon({ "class": "fa fa-4x fa-question-circle unimportant" });
        }
        return app;
    },

    // Updates the cache data with the procedure results.
    update(proc, data) {
        switch (proc) {
        case "system/status":
            this.status = { ...data };
            console.log("Updated cached status", this.status);
            break;
        case "system/session/current":
            if (this.user && this.user.id !== data?.user?.id) {
                RapidContext.UI.Msg.error.loggedOut();
                RapidContext.App.callProc = () => Promise.reject(new Error("logged out"));
            } else if (!this.user && data?.user) {
                const o = this.user = RapidContext.Data.clone(data.user);
                o.longName = o.name ? `${o.name} (${o.id})` : o.id;
                console.log("Updated cached user", this.user);
            }
            break;
        case "system/app/list":
            if (data) {
                for (const o of data) {
                    const launcher = this._normalizeApp(o);
                    launcher.instances = this.apps[launcher.id]?.instances ?? [];
                    this.apps[launcher.id] = Object.assign(this.apps[launcher.id] ?? {}, launcher);
                }
                console.log("Updated cached apps", this.apps);
            }
            break;
        }
    },

    // Updates the cache data on some procedure errors.
    handleError(error) {
        if (this.user && /permission denied/i.test(error)) {
            setTimeout(() => RapidContext.App.callProc("system/session/current"));
        }
    }
};
