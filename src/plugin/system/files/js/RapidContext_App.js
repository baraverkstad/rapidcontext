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

/**
 * @name RapidContext
 * @namespace The base RapidContext namespace.
 */
if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}

/**
 * @name RapidContext.App
 * @namespace Provides functions for application bootstrap and server
 *     communication.
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
 * @param {String/Object} [app] the app id or class name to start
 *
 * @return {Deferred} a `MochiKit.Async.Deferred` object that will
 *         callback when the initialization has completed
 */
RapidContext.App.init = function (app) {
    // Setup libraries
    RapidContext.Log.context("RapidContext.App.init()");
    RapidContext.Log.info("Initializing RapidContext");
    RapidContext.Util.registerFunctionNames(RapidContext, "RapidContext");
    if (!RapidContext.Browser.isSupported()) {
        MochiKit.DOM.removeElementClass("unsupported-browser", "hidden");
        return MochiKit.Async.fail(new Error("Unsupported browser"));
    }

    // Setup UI
    RapidContext.Util.registerSizeConstraints(document.body, "100%-20", "100%-20");
    var resizer = MochiKit.Base.partial(RapidContext.Util.resizeElements, document.body);
    MochiKit.Signal.connect(window, "onresize", resizer);
    RapidContext.Util.resizeElements(document.body);
    var overlay = new RapidContext.Widget.Overlay({ message: "Loading..." });
    MochiKit.DOM.replaceChildNodes(document.body, overlay);

    // Load platform data
    var list = [
        RapidContext.App.callProc("System.Status"),
        RapidContext.App.callProc("System.Session.Current"),
        RapidContext.App.callProc("System.App.List")
    ];
    var d = MochiKit.Async.gatherResults(list);

    // Launch app
    d.addBoth(function () {
        RapidContext.Log.context(null);
    });
    d.addCallback(function () {
        try {
            return RapidContext.App.startApp(app || "start");
        } catch (e) {
            RapidContext.UI.showError(e);
            return RapidContext.App.startApp("start");
        }
    });
    d.addErrback(RapidContext.UI.showError);
    return d;
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
    // TODO: use deep clone
    return MochiKit.Base.clone(RapidContext.App._Cache.status);
};

/**
 * Returns an object with information about the user. The object
 * returned is a copy of the internal data structure and can be
 * modified without affecting the real data.
 *
 * @return {Object} the user data object
 */
RapidContext.App.user = function () {
    // TODO: use deep clone
    return MochiKit.Base.clone(RapidContext.App._Cache.user);
};

/**
 * Returns an array with app launchers. The data returned is an
 * internal data structure and should not be modified.
 *
 * @return {Array} the loaded app launchers (read-only)
 */
RapidContext.App.apps = function () {
    return MochiKit.Base.values(RapidContext.App._Cache.apps);
};

/**
 * Returns an array with running app instances.
 *
 * @return {Array} the array of app instances
 */
RapidContext.App._instances = function () {
    var res = [];
    var apps = RapidContext.App.apps();
    for (var i = 0; i < apps.length; i++) {
        res = res.concat(apps[i].instances || []);
    }
    return res;
};

/**
 * Finds the app launcher from an app instance, class name or
 * launcher. In the last case, the matching cached launcher will be
 * returned.
 *
 * @param {String/Object} app the app id, instance, class name or
 *        launcher
 *
 * @return {Object} the read-only app launcher, or
 *         `null` if not found
 */
RapidContext.App.findApp = function (app) {
    if (app == null) {
        return null;
    }
    var apps = RapidContext.App.apps();
    for (var i = 0; i < apps.length; i++) {
        var l = apps[i];
        if (l.className == null) {
            RapidContext.Log.error("Launcher does not have 'className' property", l);
        } else if (l.id == app || l.className == app || l.id == app.id) {
            return l;
        }
    }
    return null;
};

RapidContext.App._cbAssign = function (obj, key) {
    return function (data) {
        obj[key] = data;
        return data;
    };
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
 * @param {String/Object} app the app id, class name or launcher
 * @param {Widget/Window} [container] the app container widget or
 *            window, defaults to create a new pane in the app tab
 *            container
 *
 * @return {Deferred} a `MochiKit.Async.Deferred` object that will
 *         callback with the app instance (or `null` if not available)
 *
 * @example
 * // Starts the help app in a new tab
 * RapidContext.App.startApp('help');
 *
 * // Starts the help app in a new window
 * RapidContext.App.startApp('help', window.open());
 */
RapidContext.App.startApp = function (app, container) {

    // Setup variables and find app launcher
    var launcher = RapidContext.App.findApp(app);
    if (launcher == null) {
        RapidContext.Log.error("No matching app launcher found", app);
        throw new Error("No matching app launcher found: " + app);
    }
    var instance = null;
    var instances = RapidContext.App._instances();
    var startApp = RapidContext.App.findApp("start");
    var d = MochiKit.Async.wait(0);
    var ui = null;

    // Initialize app UI container
    launcher.instances = launcher.instances || [];
    if ($.isWindow(container)) {
        var href = "rapidcontext/app/" + launcher.id;
        container.location.href = RapidContext.Util.resolveURI(href);
        return d;
    } else if (startApp && startApp.instances && startApp.instances.length) {
        var paneOpts = { title: launcher.name, closeable: (launcher.launch != "once") };
        ui = startApp.instances[0].initAppPane(container, paneOpts);
    } else if (instances.length == 1 && launcher.id != "start") {
        // Switch from single-app to multi-app mode
        d = RapidContext.App.startApp("start");
        d.addCallback(function (instance) {
            return RapidContext.App.startApp(app, container);
        });
        return d;
    } else {
        ui = {
            root: document.body,
            overlay: document.body.childNodes[0]
        };
    }
    MochiKit.Signal.connect(ui.root, "onclose", d, "cancel");

    // Load app resources
    var logCtx = "RapidContext.App.startApp(" + launcher.id + ")";
    RapidContext.Log.context(logCtx);
    if (launcher.creator == null) {
        RapidContext.Log.info("Loading app/" + launcher.id + " resources", launcher);
        launcher.resource = {};
        for (var i = 0; i < launcher.resources.length; i++) {
            var res = launcher.resources[i];
            if (res.type == "code") {
                d.addCallback(MochiKit.Base.partial(RapidContext.App.loadScript, res.url));
            } else if (res.type == "style") {
                d.addCallback(MochiKit.Base.partial(RapidContext.App.loadStyles, res.url));
            } else if (res.type == "ui") {
                d.addCallback(MochiKit.Base.partial(RapidContext.App.loadXML, res.url));
                d.addCallback(RapidContext.App._cbAssign(launcher, "ui"));
            } else if (res.type == "json" && res.id != null) {
                d.addCallback(MochiKit.Base.partial(RapidContext.App.loadJSON, res.url, null, null));
                d.addCallback(RapidContext.App._cbAssign(launcher.resource, res.id));
            } else if (res.id != null) {
                launcher.resource[res.id] = res.url;
            }
        }
        d.addCallback(function () {
            launcher.creator = this[launcher.className] || window[launcher.className];
            if (launcher.creator == null) {
                RapidContext.Log.error("App constructor " + launcher.className + " not defined", launcher);
                throw new Error("App constructor " + launcher.className + " not defined");
            }
            RapidContext.Util.registerFunctionNames(launcher.creator, launcher.className);
        });
    }

    // Create app instance, build UI and start app
    d.addCallback(function () {
        RapidContext.Log.info("Starting app/" + launcher.id, launcher);
        /* eslint new-cap: "off" */
        instance = new launcher.creator();
        launcher.instances.push(instance);
        var props = MochiKit.Base.setdefault({ ui: ui }, launcher);
        delete props.creator;
        delete props.instances;
        MochiKit.Base.setdefault(instance, props);
        MochiKit.Signal.disconnectAll(ui.root, "onclose");
        var halt = MochiKit.Base.partial(RapidContext.App.stopApp, instance);
        MochiKit.Signal.connect(ui.root, "onclose", halt);
        if (launcher.ui != null) {
            var widgets = RapidContext.UI.buildUI(launcher.ui, ui);
            MochiKit.DOM.appendChildNodes(ui.root, widgets);
            RapidContext.Util.resizeElements(ui.root);
        }
        ui.overlay.hide();
        ui.overlay.setAttrs({ message: "Working..." });
        return RapidContext.App.callApp(instance, "start");
    });

    // Convert to start app UI (if previously in single-app mode)
    if (launcher.id == "start" && instances.length == 1) {
        var elems = MochiKit.Base.extend([], document.body.childNodes);
        var opts = { title: instances[0].name, closeable: false, background: true };
        d.addCallback(function () {
            return RapidContext.App.callApp(instance, "initAppPane", null, opts);
        });
        d.addCallback(function (ui) {
            ui.root.removeAll();
            ui.root.addAll(elems);
            RapidContext.Util.resizeElements(ui.root);
        });
    }

    // Report errors and return app instance
    d.addErrback(function (err) {
        if (err instanceof MochiKit.Async.CancelledError) {
            // Ignore cancellation errors
        } else {
            RapidContext.Log.error("Failed to start app", err);
            MochiKit.Signal.disconnectAll(ui.root, "onclose");
            var label = MochiKit.DOM.STRONG(null, "Error: ");
            MochiKit.DOM.appendChildNodes(ui.root, label, err.message);
            ui.overlay.hide();
        }
        RapidContext.Log.context(null);
        return err;
    });
    d.addCallback(function () {
        RapidContext.Log.context(null);
        return instance;
    });
    RapidContext.App._addErrbackLogger(d, logCtx);
    return d;
};

/**
 * Stops an app instance. If only the class name or launcher is specified, the
 * most recently created instance will be stopped.
 *
 * @param {String/Object} app the app id, instance, class name or launcher
 *
 * @return {Deferred} a `MochiKit.Async.Deferred` object that will
 *         callback when the app has been stopped
 */
RapidContext.App.stopApp = function (app) {
    var launcher = RapidContext.App.findApp(app);
    if (!(launcher && launcher.instances && launcher.instances.length)) {
        RapidContext.Log.error("No running app instance found", app);
        throw new Error("No running app instance found");
    }
    RapidContext.Log.info("Stopping app " + launcher.name);
    var pos = MochiKit.Base.findIdentical(launcher.instances, app);
    if (pos < 0) {
        app = launcher.instances.pop();
    } else {
        launcher.instances.splice(pos, 1);
    }
    app.stop();
    if (app.ui.root != null) {
        RapidContext.Widget.destroyWidget(app.ui.root);
    }
    for (var k in app.ui) {
        delete app.ui[k];
    }
    for (var n in app) {
        delete app[n];
    }
    MochiKit.Signal.disconnectAllTo(app);
    return MochiKit.Async.wait(0);
};

/**
 * Performs an asynchronous call to a method in an app. If only the class name
 * or launcher is specified, the most recently created instance will be used.
 * If no instance is running, one will be started. Also, before calling the app
 * method, the app UI will be focused.
 *
 * @param {String/Object} app the app id, instance, class name or launcher
 * @param {String} method the app method name
 * @param {Mixed} [args] additional parameters sent to method
 *
 * @return {Deferred} a `MochiKit.Async.Deferred` object that will
 *         callback with the result of the call on success
 */
RapidContext.App.callApp = function (app, method) {
    var args = MochiKit.Base.extend([], arguments, 2);
    var launcher = RapidContext.App.findApp(app);
    var d;
    if (launcher == null) {
        RapidContext.Log.error("No matching app launcher found", app);
        throw new Error("No matching app launcher found");
    }
    var logCtx = "RapidContext.App.callApp(" + launcher.id + "," + method + ")";
    if (!(launcher.instances && launcher.instances.length)) {
        d = RapidContext.App.startApp(app);
    } else {
        var pos = MochiKit.Base.findIdentical(launcher.instances, app);
        var instance = (pos >= 0) ? app : launcher.instances[launcher.instances.length - 1];
        d = MochiKit.Async.wait(0, instance);
        RapidContext.App._addErrbackLogger(d, logCtx);
    }
    d.addCallback(function (instance) {
        RapidContext.Log.context(logCtx);
        var child = instance.ui.root;
        var parent = MochiKit.DOM.getFirstParentByTagAndClassName(child, null, "widget");
        if (parent != null && typeof(parent.selectChild) == "function") {
            parent.selectChild(child);
        }
        var methodName = launcher.className + "." + method;
        if (instance == null || instance[method] == null) {
            var msg = "No app method " + methodName + " found";
            RapidContext.Log.error(msg);
            RapidContext.Log.context(null);
            throw new Error(msg);
        }
        RapidContext.Log.log("Calling app method " + methodName, args);
        try {
            return instance[method].apply(instance, args);
        } catch (e) {
            var reason = "Caught error in " + methodName;
            RapidContext.Log.error(reason, e);
            throw new Error(reason + ": " + e.toString(), { cause: e });
        } finally {
            RapidContext.Log.context(null);
        }
    });
    return d;
};

/**
 * Performs an asynchronous procedure call.
 *
 * @param {String} name the procedure name
 * @param {Array} [args] the array of arguments, or `null`
 *
 * @return {Promise} a `RapidContext.Async` promise that will
 *         resolve with the response data on success
 */
RapidContext.App.callProc = function (name, args) {
    // TODO: remove this legacy name conversion
    if (name.indexOf("RapidContext.") == 0) {
        name = "System" + name.substring(8);
    }
    RapidContext.Log.log("Call request " + name, args);
    var params = {};
    for (var i = 0; args != null && i < args.length; i++) {
        if (args[i] == null) {
            params["arg" + i] = "null";
        } else {
            params["arg" + i] = JSON.stringify(args[i]);
        }
    }
    var logLevel = RapidContext.Log.level();
    if (logLevel == "log" || logLevel == "all") {
        params["system:trace"] = 1;
    }
    var url = "rapidcontext/procedure/" + name;
    var options = { method: "POST", timeout: 60000 };
    return RapidContext.App.loadJSON(url, params, options).then(function (res) {
        if (res.trace != null) {
            RapidContext.Log.log("Server trace " + name, res.trace);
        }
        if (res.error != null) {
            RapidContext.Log.warn("Call error " + name, res.error);
            throw new Error(res.error);
        } else {
            RapidContext.Log.log("Call response " + name, res.data);
        }
        if (name.indexOf("System.") == 0 && !res.error && res.data) {
            RapidContext.App._Cache.update(name, res.data);
        }
        return res.data;
    });
};

/**
 * Performs an asynchronous login. This function returns a deferred object
 * that will produce either a `callback` or an `errback` depending on the
 * success of the login attempt. If the current session is already bound to a
 * user, that session will be terminated and a new one will be created. If an
 * authentication token is specified, the login and password fields are not
 * used (can be null).
 *
 * @param {String} login the user login name or email address
 * @param {String} password the password to autheticate the user
 * @param {String} [token] the authentication token to indentify user/password
 *
 * @return {Deferred} a `MochiKit.Async.Deferred` object that will
 *         callback with the response data on success
 */
RapidContext.App.login = function (login, password, token) {
    var d = MochiKit.Async.wait(0, false);
    var user = RapidContext.App.user();
    if (user && user.id) {
        d.addCallback(RapidContext.App.logout);
    }
    if (token) {
        d.addCallback(function () {
            var args = [token];
            return RapidContext.App.callProc("System.Session.AuthenticateToken", args);
        });
    } else {
        if (/@/.test(login)) {
            d.addCallback(function () {
                return RapidContext.App.callProc("System.User.Search", [login]);
            });
            d.addCallback(function (user) {
                if (user && user.id) {
                    login = user.id;
                    return login;
                } else {
                    throw new Error("no user with that email address");
                }
            });
        }
        d.addCallback(function () {
            return RapidContext.App.callProc("System.Session.Current");
        });
        d.addCallback(function (session) {
            var realm = RapidContext.App.status().realm;
            var hash = CryptoJS.MD5(login + ":" + realm + ":" + password);
            hash = CryptoJS.MD5(hash.toString() + ":" + session.nonce).toString();
            var args = [login, session.nonce, hash];
            return RapidContext.App.callProc("System.Session.Authenticate", args);
        });
    }
    d.addCallback(function (res) {
        if (!res.success || res.error) {
            RapidContext.Log.info("login failed", login, res.error);
            throw new Error(res.error || "authentication failed");
        }
    });
    return d;
};

/**
 * Performs an asyncronous logout. This function terminates the current
 * session and either reloads the browser window or returns a deferred object
 * that will produce either a `callback` or an `errback` response.
 *
 * @param {Boolean} [reload] the reload browser flag, defaults to true
 *
 * @return {Deferred} a `MochiKit.Async.Deferred` object that will
 *         callback with the response data on success
 */
RapidContext.App.logout = function (reload) {
    var d = RapidContext.App.callProc("System.Session.Terminate", [null]);
    if (typeof(reload) === "undefined" || reload) {
        d.addBoth(function () {
            window.location.reload();
        });
    }
};

/**
 * Performs an asynchronous HTTP request and parses the JSON response. The
 * request parameters are automatically encoded to query string or JSON format,
 * depending on the `Content-Type` header. The parameters will be sent either
 * in the URL or as the request payload (depending on the HTTP `method`).
 *
 * @param {String} url the URL to request
 * @param {Object} [params] the request parameters, or `null`
 * @param {Object} [options] the request options, or `null`
 * @config {String} [method] the HTTP method, default is `GET`
 * @config {Number} [timeout] the timeout in milliseconds, default is 30s
 * @config {Object} [headers] the specific HTTP headers to use
 *
 * @return {Promise} a `RapidContext.Async` promise that will
 *         resolve with the parsed response JSON on success
 */
RapidContext.App.loadJSON = function (url, params, options) {
    var opts = MochiKit.Base.update({ responseType: "json" }, options);
    return RapidContext.App.loadXHR(url, params, opts).then(function (xhr) {
        return xhr.response;
    });
};

/**
 * Performs an asynchronous HTTP request for a text document. The request
 * parameters are automatically encoded to query string or JSON format,
 * depending on the `Content-Type` header. The parameters will be sent either
 * in the URL or as the request payload (depending on the HTTP `method`).
 *
 * @param {String} url the URL to request
 * @param {Object} [params] the request parameters, or `null`
 * @param {Object} [options] the request options, or `null`
 * @config {String} [method] the HTTP method, "GET" or "POST"
 * @config {Number} [timeout] the timeout in milliseconds, default is 30s
 * @config {Object} [headers] the specific HTTP headers to use
 *
 * @return {Promise} a `RapidContext.Async` promise that will
 *         resolve with the response text on success
 */
RapidContext.App.loadText = function (url, params, options) {
    var opts = MochiKit.Base.update({ responseType: "text" }, options);
    return RapidContext.App.loadXHR(url, params, opts).then(function (xhr) {
        return xhr.responseText;
    });
};

/**
 * Performs an asynchronous HTTP request for an XML document. The request
 * parameters are automatically encoded to query string or JSON format,
 * depending on the `Content-Type` header. The parameters will be sent either
 * in the URL or as the request payload (depending on the HTTP `method`).
 *
 * @param {String} url the URL to request
 * @param {Object} [params] the request parameters, or `null`
 * @param {Object} [options] the request options, or `null`
 * @config {String} [method] the HTTP method, "GET" or "POST"
 * @config {Number} [timeout] the timeout in milliseconds, default is 30s
 * @config {Object} [headers] the specific HTTP headers to use
 *
 * @return {Promise} a `RapidContext.Async` promise that will
 *         resolve with the parsed response XML document on success
 */
RapidContext.App.loadXML = function (url, params, options) {
    var opts = MochiKit.Base.update({ responseType: "document" }, options);
    return RapidContext.App.loadXHR(url, params, opts).then(function (xhr) {
        return xhr.responseXML;
    });
};

/**
 * Performs an asynchronous HTTP request. The request parameters are
 * automatically encoded to query string or JSON format, depending on the
 * `Content-Type` header. The parameters will be sent either in the URL or as
 * the request payload (depending on the HTTP `method`).
 *
 * @param {String} url the URL to request
 * @param {Object} [params] the request parameters, or `null`
 * @param {Object} [options] the request options, or `null`
 * @config {String} [method] the HTTP method, default is `GET`
 * @config {Number} [timeout] the timeout in milliseconds, default is 30s
 * @config {Object} [headers] the specific HTTP headers to use
 *
 * @return {Promise} a `RapidContext.Async` promise that will
 *         resolve with the XMLHttpRequest instance on success
 */
RapidContext.App.loadXHR = function (url, params, options) {
    var opts = MochiKit.Base.update({ method: "GET", headers: {}, timeout: 30000 }, options);
    opts.timeout = (opts.timeout < 1000) ? opts.timeout * 1000 : opts.timeout;
    var hasBody = params && ["PATCH", "POST", "PUT"].indexOf(opts.method) >= 0;
    url += (params && !hasBody) ? "?" + MochiKit.Base.queryString(params) : "";
    if (params && hasBody && opts.headers["Content-Type"] === "application/json") {
        opts.body = JSON.stringify(params);
    } else if (params && hasBody) {
        opts.headers["Content-Type"] = "application/x-www-form-urlencoded";
        opts.body = MochiKit.Base.queryString(params);
    }
    var nonCachedUrl = RapidContext.App._nonCachedUrl(url);
    RapidContext.Log.log("Starting XHR loading", nonCachedUrl, opts);
    return RapidContext.Async.xhr(nonCachedUrl, opts).then(
        function (res) {
            RapidContext.Log.log("Completed XHR loading", nonCachedUrl);
            return res;
        },
        function (err) {
            RapidContext.Log.warn("Failed XHR loading", nonCachedUrl, err);
            return Promise.reject(err);
        }
    );
};

/**
 * Loads a JavaScript to the the current page. The script is loaded by
 * inserting a `<script>` tag in the document `<head>`. Function definitions
 * and values must therefore be stored to global variables by the script to
 * become accessible after loading. If the script is already loaded, the
 * promise will resolve immediately.
 *
 * @param {String} url the URL to the script
 *
 * @return {Promise} a `RapidContext.Async` promise that will
 *         resolve when the script has loaded
 *
 * @see Use dynamic `import()` instead, if supported by the environment.
 */
RapidContext.App.loadScript = function (url) {
    var selector = ["script[src*='", url, "']"].join("");
    if (document.querySelectorAll(selector).length > 0) {
        RapidContext.Log.log("script already loaded, skipping", url);
        return RapidContext.Async.wait(0);
    } else {
        RapidContext.Log.log("loading script", url);
        return RapidContext.Async.script(RapidContext.App._nonCachedUrl(url));
    }
};

/**
 * Loads a CSS stylesheet to the the current page asynchronously. The
 * stylesheet is loaded by inserting a `<link>` tag in the document `head`.
 * If the stylesheet is already loaded, the promise will resolve immediately.
 *
 * @param {String} url the URL to the stylesheet
 *
 * @return {Promise} a `RapidContext.Async` promise that will
 *         callback when the stylesheet has been loaded
 */
RapidContext.App.loadStyles = function (url) {
    var selector = ["link[href*='", url, "']"].join("");
    if (document.querySelectorAll(selector).length > 0) {
        RapidContext.Log.log("stylesheet already loaded, skipping", url);
        return RapidContext.Async.wait(0);
    } else {
        RapidContext.Log.log("loading stylesheet", url);
        return RapidContext.Async.css(RapidContext.App._nonCachedUrl(url));
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
 * @param {String} url the URL or filename to download
 * @param {String} [data] the optional file data (if not available
 *            on the server-side)
 */
RapidContext.App.downloadFile = function (url, data) {
    if (data == null) {
        url = url + ((url.indexOf("?") < 0) ? "?" : "&") + "download";
        var attrs = {
            src: url,
            border: "0",
            frameborder: "0",
            height: "0",
            width: "0"
        };
        var iframe = MochiKit.DOM.createDOM("iframe", attrs);
        document.body.appendChild(iframe);
    } else {
        var name = MochiKit.DOM.INPUT({ name: "fileName", value: url });
        var file = MochiKit.DOM.INPUT({ name: "fileData", value: data });
        var flag = MochiKit.DOM.INPUT({ name: "download", value: "1" });
        var formAttrs = {
            action: "rapidcontext/download",
            method: "POST",
            target: "_blank",
            style: { display: "none" }
        };
        var form = MochiKit.DOM.FORM(formAttrs, name, file, flag);
        document.body.appendChild(form);
        form.submit();
    }
};

/**
 * Returns a new relative URL adapted for a non-standard base path.
 *
 * @param {String} url the URL to modify
 *
 * @return {String} the rebased URL
 */
RapidContext.App._rebaseUrl = function (url) {
    url = url || "";
    if (RapidContext._basePath) {
        if (url.indexOf(RapidContext._basePath) == 0) {
            url = url.substring(RapidContext._basePath.length);
        } else if (url.indexOf(":") < 0) {
            url = "rapidcontext/files/" + url;
        }
    }
    return url;
};

/**
 * Returns a non-cacheable version of the specified URL. This
 * function will add a request query parameter consisting of the
 * current time in milliseconds. Most web servers will ignore this
 * additional parameter, but due to the URL containing a unique
 * value, the web browser will not use its cache for the content.
 *
 * @param {String} url the URL to modify
 *
 * @return {String} the URL including the current timestamp
 */
RapidContext.App._nonCachedUrl = function (url) {
    var timestamp = new Date().getTime() % 100000;
    return url + ((url.indexOf("?") < 0) ? "?" : "&") + timestamp;
};

/**
 * Adds an error logger to a `MochiKit.Async.Deferred` object. The
 * logger will actually be added in the next JavaScript event loop,
 * ensuring that any other callbacks or handlers have already been
 * added to the deferred object. This is useful for catching
 * programmer errors and similar that cause exceptions inside
 * callback functions.
 *
 * @param {Deferred} d the `MochiKit.Async.Deferred` object to modify
 * @param {String} logCtx the log context to identify the source
 */
RapidContext.App._addErrbackLogger = function (d, logCtx) {
    function logger(err) {
        if (!d.chained && !(err instanceof MochiKit.Async.CancelledError)) {
            RapidContext.Log.warn(logCtx + " deferred: unhandled error", err);
        }
        return err;
    }
    function adder() {
        var pair = d.chain && d.chain[d.chain.length - 1];
        var errback = pair && pair[1];
        if (!d.chained && !errback) {
            var msg = logCtx + " deferred: no error handler, chain length:";
            RapidContext.Log.warn(msg, d.chain.length);
            d.addErrback(logger);
        }
    }
    setTimeout(adder);
};

/**
 * The application data cache. Contains the most recently retrieved
 * data for some commonly used objects in the execution environment.
 */
RapidContext.App._Cache = {
    status: null,
    user: null,
    apps: {},

    // Compares two object on the 'id' property
    compareId: function (a, b) {
        // TODO: replace with MochiKit.Base.keyComparator once #331 is fixed
        if (a == null || b == null) {
            return MochiKit.Base.compare(a, b);
        } else {
            return this._cmpId(a, b);
        }
    },

    // Object comparator for 'id' property
    _cmpId: MochiKit.Base.keyComparator("id"),

    // Normalizes an app manifest and its resources
    _normalizeApp: function (app) {
        function toType(type, url) {
            var isJs = !type && /\.js$/i.test(url);
            var isCss = !type && /\.css$/i.test(url);
            if (type == "code" || type == "js" || type == "javascript" || isJs) {
                return "code";
            } else if (type == "style" || type == "css" || isCss) {
                return "style";
            } else if (type == "json" || (!type && /\.json$/i.test(url))) {
                return "json";
            } else if (type == "ui" || (!type && /ui\.xml$/i.test(url))) {
                return "ui";
            } else {
                return type;
            }
        }
        function toIcon(res) {
            if (res.url) {
                return $("<img/>").attr({ src: res.url }).get(0);
            } else if (res.html) {
                var node = $("<span/>").html(res.html).get(0);
                return node.childNodes.length === 1 ? node.childNodes[0] : node;
            } else if (res["class"]) {
                return $("<i/>").addClass(res["class"]).get(0);
            } else {
                return null;
            }
        }
        function toResource(res) {
            res = (typeof(res) === "string") ? { url: res } : res;
            res.type = toType(res.type, res.url);
            if (res.url) {
                res.url = RapidContext.App._rebaseUrl(res.url);
            }
            if (!app.icon && res.type === "icon") {
                app.icon = toIcon(res);
            }
            return res;
        }
        app = MochiKit.Base.update(null, app);
        app.launch = app.launch || "manual";
        app.resources = [].concat(app.resources).filter(Boolean).map(toResource);
        if (!app.icon) {
            var cssClass = "fa fa-4x fa-question-circle unimportant";
            app.icon = toIcon({ "class": cssClass });
        }
        return app;
    },

    // Updates the cache data with the results from a procedure.
    update: function (proc, data) {
        switch (proc) {
        case "System.Status":
            // TODO: use deep clone
            data = MochiKit.Base.update(null, data);
            this.status = data;
            RapidContext.Log.log("Updated cached status", this.status);
            break;
        case "System.Session.Current":
            if (this.compareId(this.user, data.user) != 0) {
                // TODO: use deep clone
                data = MochiKit.Base.update(null, data.user);
                if (data.name != "") {
                    data.longName = data.name + " (" + data.id + ")";
                } else {
                    data.longName = data.id;
                }
                this.user = data;
                RapidContext.Log.log("Updated cached user", this.user);
            }
            break;
        case "System.App.List":
            for (var i = 0; i < data.length; i++) {
                var launcher = this._normalizeApp(data[i]);
                if (launcher.className == null) {
                    RapidContext.Log.error("App missing 'className' property", launcher);
                }
                launcher.instances = (this.apps[launcher.id] || {}).instances || [];
                this.apps[launcher.id] = MochiKit.Base.update(this.apps[launcher.id], launcher);
            }
            RapidContext.Log.log("Updated cached apps", this.apps);
            break;
        }
    }
};
