/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2013 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE.txt file for more details.
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

    // Setup UI
    RapidContext.Util.registerSizeConstraints(document.body, "100%-20", "100%-20");
    var resizer = MochiKit.Base.partial(RapidContext.Util.resizeElements, document.body);
    MochiKit.Signal.connect(window, "onresize", resizer);
    RapidContext.Util.resizeElements(document.body);
    var overlay = new RapidContext.Widget.Overlay({ message: "Loading..." });
    MochiKit.DOM.replaceChildNodes(document.body, overlay);

    // Load platform data
    var list = [ RapidContext.App.callProc("System.Status"),
                 RapidContext.App.callProc("System.Session.Current"),
                 RapidContext.App.callProc("System.App.List") ];
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
 * Returns an array with app launchers. The array returned is an
 * internal data structure and should not be modified directly.
 *
 * @return {Array} the loaded app launchers (read-only)
 */
RapidContext.App.apps = function () {
    // TODO: use deep clone, but copy instance array content
    return RapidContext.App._Cache.apps;
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
    }
}

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
    var d = MochiKit.Async.wait(0.1);
    var ui = null;

    // Initialize app UI container
    launcher.instances = launcher.instances || [];
    if ($.isWindow(container)) {
        var url = "rapidcontext/app/" + launcher.id;
        container.location.href = RapidContext.Util.resolveURI(url);
        return d;
    } else if (startApp && startApp.instances && startApp.instances.length) {
        var opts = { title: launcher.name, closeable: (launcher.launch != "once") };
        ui = startApp.instances[0].initAppPane(container, opts);
    } else if (instances.length == 1 && launcher.id != "start") {
        // Switch from single-app to multi-app mode
        d = RapidContext.App.startApp("start");
        d.addCallback(function (instance) {
            return RapidContext.App.startApp(app, container);
        });
        return d;
    } else {
        var ui = { root: document.body, overlay: document.body.childNodes[0] };
    }
    MochiKit.Signal.connect(ui.root, "onclose", d, "cancel");

    // Load app resources
    RapidContext.Log.context("RapidContext.App.startApp(" + launcher.id + ")");
    if (launcher.creator == null) {
        RapidContext.Log.info("Loading app/" + launcher.id + " resources", launcher);
        launcher.resource = {};
        for (var i = 0; i < launcher.resources.length; i++) {
            var res = launcher.resources[i];
            var url = res.url || "";
            if (RapidContext._basePath) {
                if (url.indexOf(RapidContext._basePath) == 0) {
                    url = url.substring(RapidContext._basePath.length);
                } else if (url.indexOf(":") < 0) {
                    url = "rapidcontext/files/" + url;
                }
            }
            if (res.type == "code" || res.type == "js" || res.type == "javascript") {
                d.addCallback(MochiKit.Base.partial(RapidContext.App.loadScript, url));
            } else if (res.type == "style" || res.type == "css") {
                d.addCallback(MochiKit.Base.partial(RapidContext.App.loadStyles, url));
            } else if (res.type == "ui") {
                d.addCallback(MochiKit.Base.partial(RapidContext.App.loadXML, url));
                d.addCallback(RapidContext.App._cbAssign(launcher, "ui"));
            } else if (res.type == "json" && res.id != null) {
                d.addCallback(MochiKit.Base.partial(RapidContext.App.loadJSON, url, null, null));
                d.addCallback(RapidContext.App._cbAssign(launcher.resource, res.id));
            } else if (res.id != null) {
                launcher.resource[res.id] = url;
            }
        }
        d.addCallback(function () {
            launcher.creator = this[launcher.className] || window[launcher.className];
            if (launcher.creator == null) {
                RapidContext.Log.error("app constructor " + launcher.className + " not defined", launcher);
                throw new Error("App constructor " + launcher.className + " not defined");
            }
            RapidContext.Util.registerFunctionNames(launcher.creator, launcher.className);
        });
    }

    // Retrieve license data (if requested)
    if (typeof(launcher.license) == "string") {
        var status = RapidContext.App.status();
        var user = RapidContext.App.user();
        var cbDefer = RapidContext.App._Callback.create();
        var url = "http://api.rapidcontext.com/license/1" +
                  "?app=" + launcher.id +
                  "&version=" + launcher.version +
                  "&plugin=" + launcher.plugin +
                  "&platform=" + status.version +
                  "&server=" + status.guid +
                  "&user=" + user.id +
                  "&cb=" + cbDefer.func.displayName;
        d.addCallback(function () {
            ui.overlay.setAttrs({ message: "Verifying License..." });
            var ld = MochiKit.Async.loadScript(url);
            ld.addErrback(cbDefer.func);
            return cbDefer;
        });
        cbDefer.addBoth(function (res) {
            if (res instanceof Error) {
                RapidContext.Log.error("License retrieval failed", res);
            } else {
                launcher.license = res;
            }
            return null;
        });
    }

    // Create app instance, build UI and start app
    d.addCallback(function () {
        RapidContext.Log.info("Starting app/" + launcher.id, launcher);
        var fun = launcher.creator;
        instance = new fun();
        launcher.instances.push(instance);
        var props = MochiKit.Base.setdefault({ ui: ui }, launcher);
        delete props.creator;
        delete props.instances;
        MochiKit.Base.setdefault(instance, props);
        MochiKit.Signal.disconnectAll(ui.root, "onclose");
        MochiKit.Signal.connect(ui.root, "onclose",
                                MochiKit.Base.partial(RapidContext.App.stopApp, instance));
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
        var elems = [].slice.call(document.body.childNodes);
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
            MochiKit.Signal.disconnectAll(ui.root, "onclose");
            var label = MochiKit.DOM.STRONG(null, "Error: ");
            MochiKit.DOM.appendChildNodes(ui.root, label, err.message);
            ui.overlay.hide();
        }
        return err;
    });
    d.addCallback(function () {
        return instance;
    });
    d.addBoth(function (res) {
        RapidContext.Log.context(null);
        return res;
    });
    RapidContext.App._addErrbackLogger(d);
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
    for (var n in app.ui) {
        delete app.ui[n];
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
    if (!(launcher.instances && launcher.instances.length)) {
        d = RapidContext.App.startApp(app);
    } else {
        var pos = MochiKit.Base.findIdentical(launcher.instances, app);
        var instance = (pos >= 0) ? app : launcher.instances[launcher.instances.length - 1];
        d = MochiKit.Async.wait(0, instance);
        RapidContext.App._addErrbackLogger(d);
    }
    d.addCallback(function (instance) {
        RapidContext.Log.context("RapidContext.App.callApp(" + launcher.id + "," + method + ")");
        var child = instance.ui.root;
        var parent = MochiKit.DOM.getFirstParentByTagAndClassName(child, null, "widget");
        if (parent != null && typeof(parent.selectChild) == "function") {
            parent.selectChild(child);
        }
        var methodName = launcher.className + "." + method;
        if (instance == null || instance[method] == null) {
            RapidContext.Log.error("No app method " + methodName + " found");
            throw new Error("No app method " + methodName + " found");
        }
        RapidContext.Log.log("Calling app method " + methodName, args);
        try {
            return instance[method].apply(instance, args);
        } catch (e) {
            RapidContext.Log.error("In call to " + methodName, e);
            throw new Error("In call to " + methodName + ": " + e.message);
        }
    });
    d.addBoth(function (res) {
        RapidContext.Log.context(null);
        return res;
    });
    return d;
};

/**
 * Performs an asynchronous procedure call. This function returns a deferred
 * object that will produce either a `callback` or an `errback` depending on
 * the server response.
 *
 * @param {String} name the procedure name
 * @param {Array} [args] the array of arguments, or `null`
 *
 * @return {Deferred} a `MochiKit.Async.Deferred` object that will
 *         callback with the response data on success
 */
RapidContext.App.callProc = function (name, args) {
    var params = {};
    var options = { timeout: 60 };

    // TODO: remove this legacy name conversion
    if (name.indexOf("RapidContext.") == 0) {
        name = "System" + name.substring(8);
    }
    RapidContext.Log.log("Call request " + name, args);
    for (var i = 0; args != null && i < args.length; i++) {
        if (args[i] == null) {
            params["arg" + i] = "null";
        } else {
            params["arg" + i] = JSON.stringify(args[i]);
        }
    }
    var logLevel = RapidContext.Log.level();
    if (logLevel == "log" || logLevel == "all") {
        params.trace = "true";
    }
    var d = RapidContext.App.loadJSON("rapidcontext/procedure/" + name, params, options);
    d.addCallback(function (res) {
        if (res.trace != null) {
            RapidContext.Log.log("Server trace " + name, res.trace);
        }
        if (res.error != null) {
            RapidContext.Log.error("Call error " + name, res.error);
            throw new Error(res.error);
        } else {
            RapidContext.Log.log("Call response " + name, res.data);
        }
        return res.data;
    });
    if (name.indexOf("System.") == 0) {
        d.addCallback(function (res) {
            if (res) {
                RapidContext.App._Cache.update(name, res);
            }
            return res;
        });
    }
    return d;
};

/**
 * Performs an asynchronous login. This function returns a deferred object
 * that will produce either a `callback` or an `errback` depending on the
 * success of the login attempt. If the current session is already bound to a
 * user, that session will be terminated and a new one will be created.
 *
 * @param {String} login the user login name or email address
 * @param {String} password the password to autheticate the user
 *
 * @return {Deferred} a `MochiKit.Async.Deferred` object that will
 *         callback with the response data on success
 */
RapidContext.App.login = function (login, password) {
    var d = MochiKit.Async.wait(0, false);
    var user = RapidContext.App.user();
    if (user && user.id) {
        d.addCallback(RapidContext.App.logout);
    }
    d.addCallback(function () {
        return RapidContext.App.callProc("System.Session.Current");
    });
    if (/@/.test(login)) {
        var s;
        d.addCallback(function (session) {
            s = session;
            return RapidContext.App.callProc("System.User.Search", [login]);
        });
        d.addCallback(function (user) {
            if (user && user.id) {
                login = user.id;
                return s;
            } else {
                throw new Error("no user with that email address");
            }
        });
    }
    d.addCallback(function (session) {
        var realm = RapidContext.App.status().realm;
        var hash = CryptoJS.MD5(login + ":" + realm + ":" + password);
        hash = CryptoJS.MD5(hash.toString() + ":" + session.nonce).toString();
        var args = [login, session.nonce, hash];
        return RapidContext.App.callProc("System.Session.Authenticate", args);
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
 * Performs an asynchronous HTTP request for a JSON data document and returns a
 * deferred response. If no request method has been specified, the `POST` or
 * `GET` methods are chosen depending on whether or not the params argument is
 * `null`. The request parameters are specified as an object that will be
 * encoded by the `MochiKit.Base.queryString` function. In addition to the
 * default options in `MochiKit.Async.doXHR`, this function also accepts a
 * timeout option for automatic request cancellation.
 *
 * Note that this function is unsuitable for loading JavaScript source code,
 * since using `eval()` will confuse some browser error messages and debuggers
 * regarding the actual source location.
 *
 * @param {String} url the URL to request
 * @param {Object} [params] the request parameters, or `null`
 * @param {Object} [options] the request options, or `null`
 * @config {String} [method] the HTTP method, "GET" or "POST"
 * @config {Number} [timeout] the timeout in seconds, default is no timeout
 * @config {Object} [headers] the specific HTTP headers to use
 * @config {String} [mimeType] the override MIME type, default is
 *             none
 *
 * @return {Deferred} a `MochiKit.Async.Deferred` object that will
 *         callback with the response text on success
 */
RapidContext.App.loadJSON = function (url, params, options) {
    var d = RapidContext.App.loadXHR(url, params, options);
    d.addCallback(function (res) {
        return JSON.parse(res.responseText);
    });
    return d;
};

/**
 * Performs an asynchronous HTTP request for a text document and returns a
 * deferred response. If no request method has been specified, the `POST` or
 * `GET` methods are chosen depending on whether or not the params argument is
 * `null`. The request parameters are specified as an object that will be
 * encoded by the `MochiKit.Base.queryString` function. In addition to the
 * default options in `MochiKit.Async.doXHR`, this function also accepts a
 * timeout option for automatic request cancellation.
 *
 * Note that this function is unsuitable for loading JavaScript source code,
 * since using `eval()` will confuse some browser error messages and debuggers
 * regarding the actual source location.
 *
 * @param {String} url the URL to request
 * @param {Object} [params] the request parameters, or `null`
 * @param {Object} [options] the request options, or `null`
 * @config {String} [method] the HTTP method, "GET" or "POST"
 * @config {Number} [timeout] the timeout in seconds, default is no timeout
 * @config {Object} [headers] the specific HTTP headers to use
 * @config {String} [mimeType] the override MIME type, default is
 *             none
 *
 * @return {Deferred} a `MochiKit.Async.Deferred` object that will
 *         callback with the response text on success
 */
RapidContext.App.loadText = function (url, params, options) {
    var d = RapidContext.App.loadXHR(url, params, options);
    d.addCallback(function (res) { return res.responseText; });
    return d;
};

/**
 * Performs an asynchronous HTTP request for an XML document and returns a
 * deferred response. If no request method has been specified, the `POST` or
 * `GET` methods are chosen depending on whether or not the params argument is
 * `null`. The request parameters are specified as an object that will be
 * encoded by the `MochiKit.Base.queryString` function. In addition to the
 * default options in `MochiKit.Async.doXHR`, this function also accepts a
 * timeout option for automatic request cancellation.
 *
 * @param {String} url the URL to request
 * @param {Object} [params] the request parameters, or `null`
 * @param {Object} [options] the request options, or `null`
 * @config {String} [method] the HTTP method, "GET" or "POST"
 * @config {Number} [timeout] the timeout in seconds, default is no timeout
 * @config {Object} [headers] the specific HTTP headers to use
 * @config {String} [mimeType] the override MIME type, default is
 *             none
 *
 * @return {Deferred} a `MochiKit.Async.Deferred` object that will
 *         callback with the parsed response XML document on success
 */
RapidContext.App.loadXML = function (url, params, options) {
    var d = RapidContext.App.loadXHR(url, params, options);
    d.addCallback(function (res) { return res.responseXML; });
    return d;
};

/**
 * Performs an asynchronous HTTP request and returns a deferred response. If no
 * request method has been specified, the `POST` or `GET` methods are chosen
 * depending on whether or not the `params` argument is `null`. The request
 * parameters are specified as an object that will be encoded by the
 * `MochiKit.Base.queryString` function. In addition to the default options in
 * `MochiKit.Async.doXHR`, this function also accepts a timeout option for
 * automatic request cancellation.
 *
 * Note that this function is unsuitable for loading JavaScript source code,
 * since using `eval()` will confuse some browser error messages and debuggers
 * regarding the actual source location.
 *
 * @param {String} url the URL to request
 * @param {Object} [params] the request parameters, or `null`
 * @param {Object} [options] the request options, or `null`
 * @config {String} [method] the HTTP method, "GET" or "POST"
 * @config {Number} [timeout] the timeout in seconds, default is no timeout
 * @config {Object} [headers] the specific HTTP headers to use
 * @config {String} [mimeType] the override MIME type, default is
 *             none
 *
 * @return {Deferred} a `MochiKit.Async.Deferred` object that will
 *         callback with the XMLHttpRequest instance on success
 */
RapidContext.App.loadXHR = function (url, params, options) {
    options = options || {};
    if (options.method == "GET" && params != null) {
        url += "?" + MochiKit.Base.queryString(params);
    } else if (params != null) {
        options.method = options.method || "POST";
        options.headers = options.headers || {};
        options.headers["Content-Type"] = "application/x-www-form-urlencoded";
        options.sendContent = MochiKit.Base.queryString(params);
    } else {
        options.method = options.method || "GET";
    }
    var nonCachedUrl = RapidContext.App._nonCachedUrl(url);
    var d = MochiKit.Async.doXHR(nonCachedUrl, options);
    if (options.timeout) {
        var canceller = function () {
            // TODO: Supply error to cancel() instead of this hack, when
            // supported in MochiKit (#323). This work-around is necessary
            // due to MochiKit internally using wait() in doXHR().
            d.results[0].canceller();
            d.results[0].errback("Timeout on request to " + url);
        };
        var timer = MochiKit.Async.callLater(options.timeout, canceller);
        d.addCallback(function (res) { timer.cancel(); return res; });
    }
    RapidContext.App._addErrbackLogger(d);
    return d;
};

/**
 * Loads a JavaScript to the the current page asynchronously and returns a
 * deferred response. This function is only suitable for loading JavaScript
 * source code, and not JSON data, since it loads the script by inserting a
 * `<script>` tag in the document `<head>` tag. All function definitions and
 * values must therefore be stored to global variables by the script to be
 * accessible after loading. The deferred callback function will therefore not
 * provide any data even on successful callback.
 *
 * This method of script loading has the advantage that JavaScript debuggers
 * (such as Firebug) will be able to handle the code properly (error messages,
 * breakpoints, etc). If the script fails to load due to errors however, the
 * returned deferred object may fail to `errback` in some cases.
 *
 * @param {String} url the URL to the script
 *
 * @return {Deferred} a `MochiKit.Async.Deferred` object that will
 *         callback when the script has been loaded
 */
RapidContext.App.loadScript = function (url) {
    var absoluteUrl = RapidContext.Util.resolveURI(url);
    var selector1 = 'script[src^="' + url + '"]';
    var selector2 = 'script[src^="' + absoluteUrl + '"]';
    var elems = MochiKit.Selector.findDocElements(selector1, selector2);
    if (elems.length > 0) {
        RapidContext.Log.log("Script already loaded, skipping", url);
        return MochiKit.Async.wait(0);
    }
    RapidContext.Log.log("Starting script loading", url);
    var d = MochiKit.Async.loadScript(RapidContext.App._nonCachedUrl(url));
    d.addCallback(function () {
        RapidContext.Log.log("Completed loading script", url);
    });
    d.addErrback(function (e) {
        RapidContext.Log.warning("Failed loading script", url + ": " + e.message);
        return e;
    });
    RapidContext.App._addErrbackLogger(d);
    return d;
};

/**
 * Loads a CSS stylesheet to the the current page asynchronously and returns a
 * deferred response. The stylesheet is loaded by inserting a `<link>` tag in
 * the document head tag, which means that the deferred callback function will
 * not be provided with any data.
 *
 * @param {String} url the URL to the stylesheet
 *
 * @return {Deferred} a `MochiKit.Async.Deferred` object that will
 *         callback when the stylesheet has been loaded
 */
RapidContext.App.loadStyles = function (url) {
    function findStylesheet(url) {
        var styles = document.styleSheets;
        for (var i = 0; i < styles.length; i++) {
            if (MochiKit.Text.startsWith(url, styles[i].href)) {
                return styles[i];
            }
        }
        return null;
    }
    var d = new MochiKit.Async.Deferred();
    var absoluteUrl = RapidContext.Util.resolveURI(url);
    if (findStylesheet(url) || findStylesheet(absoluteUrl)) {
        RapidContext.Log.log("Stylesheet already loaded, skipping", url);
        return MochiKit.Async.wait(0);
    }
    RapidContext.Log.log("Starting stylesheet loading", url);
    var loadUrl = RapidContext.App._nonCachedUrl(url);
    var link = MochiKit.DOM.LINK({ rel: "stylesheet", type: "text/css", href: loadUrl });
    document.getElementsByTagName("head")[0].appendChild(link);
    var img = MochiKit.DOM.IMG();
    img.onerror = function () {
        var sheet = findStylesheet(url) || findStylesheet(absoluteUrl);
        if (sheet && sheet.cssRules && sheet.cssRules.length) {
            RapidContext.Log.log("Completed loading stylesheet", url);
            d.callback();
        } else {
            RapidContext.Log.warning("Failed loading stylesheet", url);
            msg = "Failed loading stylesheet " + url;
            d.errback(new URIError(msg, url));
        }
    }
    img.src = loadUrl;
    RapidContext.App._addErrbackLogger(d);
    return d;
}

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
        var attrs = { src: url, border: "0", frameborder: "0",
                      height: "0", width: "0" };
        var iframe = MochiKit.DOM.createDOM("iframe", attrs);
        document.body.appendChild(iframe);
    } else {
        var name = MochiKit.DOM.INPUT({ name: "fileName", value: url });
        var file = MochiKit.DOM.INPUT({ name: "fileData", value: data });
        var flag = MochiKit.DOM.INPUT({ name: "download", value: "1" });
        var attrs = { action: "rapidcontext/download", method: "POST",
                      target: "_blank", style: { display: "none" } };
        var form = MochiKit.DOM.FORM(attrs, name, file, flag);
        document.body.appendChild(form);
        form.submit();
    }
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
 */
RapidContext.App._addErrbackLogger = function (d) {
    var logger = function (err) {
        if (!d.chained) {
            // TODO: Handle MochiKit.Async.CancelledError here?
            RapidContext.Log.warning("Unhandled error in deferred", err);
        }
        return err;
    };
    var adder = function () {
        if (!d.chained) {
            d.addErrback(logger);
        }
    };
    MochiKit.Async.callLater(0, adder);
};

RapidContext.App._Callback = {
    nextId: MochiKit.Base.counter(),
    create: function () {
        var id = "cb" + this.nextId();
        var d = new MochiKit.Async.Deferred();
        var func = MochiKit.Base.bind("handle", this, id, d);
        func.displayName = "RapidContext.App._Callback." + id;
        this[id] = d.func = func;
        return d;
    },
    handle: function (id, d, data) {
        delete this[id];
        if (data instanceof Error) {
            d.errback(data);
        } else {
            d.callback(data);
        }
    }
}

/**
 * The application data cache. Contains the most recently retrieved
 * data for some commonly used objects in the execution environment.
 */
RapidContext.App._Cache = {
    status: null,
    user: null,
    apps: [],
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
            var idxs = {};
            for (var i = 0; i < this.apps.length; i++) {
                idxs[this.apps[i].id] = i;
            }
            for (var i = 0; i < data.length; i++) {
                // TODO: use deep clone
                var launcher = MochiKit.Base.update(null, data[i]);
                launcher.launch = launcher.launch || "manual";
                if (!(launcher.resources instanceof Array)) {
                    launcher.resources = [];
                }
                for (var j = 0; j < launcher.resources.length; j++) {
                    if (launcher.resources[j].type == "icon") {
                        launcher.icon = launcher.resources[j].url;
                    }
                }
                if (launcher.className == null) {
                    RapidContext.Log.error("App missing 'className' property", launcher);
                }
                var pos = idxs[launcher.id];
                if (pos >= 0) {
                    delete idxs[launcher.id];
                    MochiKit.Base.update(this.apps[pos], launcher);
                } else {
                    this.apps.push(launcher);
                }
            }
            // TODO: use separate list of app instances...
            var arr = MochiKit.Base.values(idxs).reverse();
            for (var i = 0; i < arr.length; i++) {
                var pos = arr[i];
                if (this.apps[pos].instances == null) {
                    this.apps.splice(pos, 1);
                }
            }
            RapidContext.Log.log("Updated cached apps", this.apps);
            break;
        }
    }
};
