/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2017 Per Cederberg. All rights reserved.
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
 * @name RapidContext.Log
 * @namespace Provides a logging service for debugging apps and server calls.
 *
 * All log messages are filtered by log level and either discarded or
 * stored to an internal array. This makes it possible to either show these
 * in a developer console or to send them to a server for remote storage.
 *
 * This module also replaces the built-in `console.error()`, `console.warn()`,
 * `console.info()` and `console.log()` functions with its own versions,
 * passing through the filtered log messages to the original functions. For
 * browsers without a `console` object, a minimal version is provided.
 */
(function (window, undefined) {

    // Function-level strict mode
    "use strict";

    // Detect MSIE browser to force simplified console object logging
    var isMSIE = /MSIE/.test(window.navigator.userAgent);

    // The original console log functions
    var consoleBackup = {};

    // The current log level
    var logLevel = 3;

    // The current log message count
    var logCount = 0;

    // The current log history
    var logHistory = [];

    // The current log context
    var logContext = null;

    // The last published log event
    var publishedId = -1;

    // The log levels filterer out for publishing
    var publishLevels = /error|warn/;

    // The currently running publisher timer
    var publisherTimer = null;

    /**
     * Clears the log console and the array of stored messages.
     *
     * @see RapidContext.Log.history
     *
     * @memberof RapidContext.Log
     */
    function clear() {
        logHistory = [];
        if (window.console && window.console.clear) {
            window.console.clear();
        }
    }

    /**
     * Returns the history of filtered log entries. Each log entry is a plain
     * object with properties -- `id`, `time`, `level`, `context`, `message`
     * and `data`.
     *
     * @return {Array} the array of log entries
     *
     * @memberof RapidContext.Log
     */
    function history() {
        return logHistory.slice(0);
    }

    /**
     * Returns and optionally sets the current log level. The supported log
     * level values are -- "none", "error", "warn", "info", "log" and "all".
     *
     * @param {String} [value] the new log level
     * @return {String} the current log level
     *
     * @memberof RapidContext.Log
     */
    function level(value) {
        if (typeof(value) !== "undefined") {
            if (value === 0 || /^none/i.test(value)) {
                logLevel = 0;
            } else if (value === 1 || /^err/i.test(value)) {
                logLevel = 1;
            } else if (value === 2 || /^warn/i.test(value)) {
                logLevel = 2;
            } else if (value === 3 || /^info/i.test(value)) {
                logLevel = 3;
            } else if (value === 4 || /^(log|debug|trace)/i.test(value)) {
                logLevel = 4;
            } else {
                logLevel = 5;
            }
        }
        if (logLevel <= 0) {
            return "none";
        } else if (logLevel <= 1) {
            return "error";
        } else if (logLevel <= 2) {
            return "warn";
        } else if (logLevel <= 3) {
            return "info";
        } else if (logLevel <= 4) {
            return "log";
        } else {
            return "all";
        }
    }

    /**
     * Returns and optionally sets the current log context. The log context is
     * used to tag all subsequent log messages until the context is removed or
     * modified.
     *
     * @param {String} [value] the new log context, or null to clear
     *
     * @return {String} the current log context, or null for none
     *
     * @example
     * RapidContext.Log.context('mybutton.onclick');
     * ...
     * console.warn('unsupported xyz value:', value);
     * ...
     * RapidContext.Log.context(null);
     *
     * @memberof RapidContext.Log
     */
    function context(value) {
        if (typeof(value) !== "undefined") {
            logContext = value;
            if (value == null) {
                _consoleContext();
            }
        }
        return logContext;
    }

    /**
     * Logs an error message with optional data. Also available as the global
     * `console.error()` function.
     *
     * @param {String} msg the log message
     * @param {Object} [...] the additional log data or messages
     *
     * @example
     * console.error('failed to initialize module');
     *
     * @name error
     * @function
     * @memberof RapidContext.Log
     */
    function logError(msg/**, ...*/) {
        if (logLevel >= 1) {
            var args = Array.prototype.slice.apply(arguments);
            var strs = args.map(stringify);
            _consoleLog("error", args, strs);
            _store("error", strs);
        }
    }

    /**
     * Logs a warning message with optional data. Also available as the global
     * `console.warn()` function.
     *
     * @param {String} msg the log message
     * @param {Object} [...] the additional log data or messages
     *
     * @example
     * console.warn('missing "data" attribute on document root:', document.body);
     *
     * @name warn
     * @function
     * @memberof RapidContext.Log
     */
    function logWarn(msg/**, ...*/) {
        if (logLevel >= 2) {
            var args = Array.prototype.slice.apply(arguments);
            var strs = args.map(stringify);
            _consoleLog("warn", args, strs);
            _store("warn", strs);
        }
    }

    /**
     * Logs an information message with optional data. Also available as the
     * global `console.info()` function.
     *
     * @param {String} msg the log message
     * @param {Object} [...] the additional log data or messages
     *
     * @example
     * console.info('authorization failed, user not logged in');
     *
     * @name info
     * @function
     * @memberof RapidContext.Log
     */
    function logInfo(msg/**, ...*/) {
        if (logLevel >= 3) {
            var args = Array.prototype.slice.apply(arguments);
            var strs = args.map(stringify);
            _consoleLog("info", args, strs);
            _store("info", strs);
        }
    }

    /**
     * Logs a debug message with optional data. Also available as the global
     * `console.log()` and `console.debug()` functions.
     *
     * @param {String} msg the log message
     * @param {Object} [...] the additional log data or messages
     *
     * @example
     * console.log('init AJAX call to URL:', url);
     * ...
     * console.log('done AJAX call to URL:', url, responseCode);
     *
     * @name debug
     * @function
     * @memberof RapidContext.Log
     */
    function logDebug(msg/**, ...*/) {
        if (logLevel >= 4) {
            var args = Array.prototype.slice.apply(arguments);
            var strs = args.map(stringify);
            _consoleLog("log", args, strs);
            _store("log", strs);
        }
    }

    /**
     * Logs a number of browser meta-data parameters (INFO level).
     *
     * @memberof RapidContext.Log
     */
    function logBrowserInfo() {
        function copy(obj, keys) {
            var res = {};
            for (var i = 0; i < keys.length; i++) {
                var k = keys[i];
                res[k] = obj[k];
            }
            return res;
        }
        context('Browser')
        logInfo("location.href", location.href);
        logInfo("navigator.userAgent", navigator.userAgent);
        logInfo("navigator.language", navigator.language);
        logInfo("screen", copy(screen, ["width", "height", "colorDepth"]));
        logInfo("window", copy(window, ["innerWidth", "innerHeight", "devicePixelRatio"]));
        logInfo("document.cookie", document.cookie);
        context(null);
    }

    /**
     * Handles window.onerror events (global uncaught errors).
     */
    function _windowErrorHandler(msg, src, line, col, error) {
        var location = "unknown source";
        if (error && error.stack) {
            location = error.stack;
        } else if (src && src !== "undefined") {
            location = [src, line || 0, col || 0].join(':');
        }
        logError(msg, location);
        return true;
    }

    /**
     * Modifies the `console` object for logging. This replaces the default
     * `console.error`, `console.warn`, `console.info`, `console.log` and
     * `console.debug` functions (if not previously modified).
     */
    function _consoleSetup() {
        function overwrite(obj, key, value, backup) {
            if (obj[key] !== value) {
                backup[key] = obj[key];
                obj[key] = value;
            }
        }
        window.console || (window.console = {});
        overwrite(window.console, 'error', logError, consoleBackup);
        overwrite(window.console, 'warn', logWarn, consoleBackup);
        overwrite(window.console, 'info', logInfo, consoleBackup);
        overwrite(window.console, 'log', logDebug, consoleBackup);
        overwrite(window.console, 'debug', logDebug, consoleBackup);
    }

    /**
     * Calls the `console.group` and `console.groupEnd` functions (if
     * they exist) to adjust for the current log context.
     */
    function _consoleContext() {
        if (window.console._group && window.console._group !== logContext) {
            window.console._group = null;
            window.console.groupEnd && window.console.groupEnd();
        }
        if (logContext && window.console._group !== logContext) {
            window.console._group = logContext;
            window.console.group && window.console.group(logContext + ":");
        }
    }

    /**
     * Logs a message to one of the console loggers.
     *
     * @param {String} loggerName the console logger name ('error', 'warn'...)
     * @param {Array} args the log message & data (as raw objects)
     * @param {Array} strs the log message & data (as strings)
     */
    function _consoleLog(loggerName, args, strs) {
        _consoleSetup();
        _consoleContext();
        var logger = consoleBackup[loggerName];
        if (typeof(logger) === 'function' && typeof(logger.apply) === 'function') {
            logger.apply(window.console, args);
        } else if (logger && isMSIE) {
            var ctx = (logContext ? logContext + ": " : "");
            logger(ctx + strs.join(", "));
        }
    }

    /**
     * Stores a log message to the history.
     *
     * @param {String} level the message log level
     * @param {Array} args the log message arguments (as strings)
     *
     * @private
     */
    function _store(level, args) {
        var data = (args.length > 1) ? args.slice(1).join(", ") : null;
        logHistory.push({ id: ++logCount, time: new Date(), level: level,
                          context: logContext, message: args[0], data: data });
        while (logHistory.length > 100) {
            logHistory.shift();
        }
        if (publisherTimer == null) {
            publisherTimer = setTimeout(_publishEvents, 100);
        }
    }

    /**
     * Publishes stored events (above a threshold level) to the server.
     *
     * @private
     */
    function _publishEvents() {
        var events = [];
        var lastId = 0;
        for (var i = 0; i < logHistory.length; i++) {
            var evt = logHistory[i];
            lastId = evt.id;
            if (evt.id > publishedId && publishLevels.test(evt.level)) {
                events.push(evt);
            }
        }
        if (events.length <= 0) {
            publishedId = lastId;
            publisherTimer = null;
            return;
        }
        var opts = {
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify(events)
        };
        var d = $.ajax("rapidcontext/log", opts);
        d.then(function (res) {
            publishedId = lastId;
            publisherTimer = setTimeout(_publishEvents, 10000);
        });
        d.fail(function (err) {
            logError("failed to publish eventlog", err);
            publisherTimer = setTimeout(_publishEvents, 30000);
        });
    }

    /**
     * Creates a string representation (suitable for logging) for any value or
     * object. The returned string is similar to a JSON representation of the
     * value, but may be simplified for increased readability.
     *
     * @param {Object} o the value or object to convert
     *
     * @return {String} the string representation of the value
     *
     * @memberof RapidContext.Log
     */
    function stringify(o) {
        return _stringify(o, 0);
    }

    /**
     * Internal stringify implementation that tracks of object graph
     * depth.
     *
     * @param {Object} o the value or object to convert
     * @param {Number} depth the current object depth (max is 4)
     *
     * @return {Array} the array of string representations
     *
     * @private
     */
    function _stringify(o, depth) {
        var type = typeof(o);
        if (o == null || type == "boolean" || type == "number") {
            return "" + o;
        } else if (type == "string" && depth > 0) {
            return '"' + o.replace(/"/g, '\\"').replace(/\n/g, "\\n") + '"';
        } else if (type == "string") {
            return o;
        } else if (type == "function") {
            var name = o.name || o.displayName || "";
            var signature = o.toString().replace(/\)[\s\S]*/, "") + ")";
            if (/function\s+\(/.test(signature) && name) {
                signature = signature.replace(/function\s+/, "function " + name);
            } else if (!(/function/.test(signature))) {
                signature = "function ()";
            }
            return signature + " {...}";
        } else if (o.nodeType === 1 || o.nodeType === 9) {
            o = o.documentElement || o.document || o;
            var xml = o.outerHTML || o.outerXML || o.xml || "" + o;
            if (o.childNodes && o.childNodes.length) {
                return xml.replace(/>[\s\S]*</, ">...<");
            } else {
                return xml.replace(/>[\s\S]*/, " />");
            }
        } else if (depth > 3) {
            return "...";
        } else if (typeof(o.length) == "number") {
            var arr = [];
            for (var i = 0; i < o.length; i++) {
                if (i > 20) {
                    arr.push("...");
                    break;
                }
                arr.push(_stringify(o[i], depth + 1));
            }
            return "[" + arr.join(", ") + "]";
        } else {
            var arr = [];
            for (var k in o) {
                if (arr.length > 20) {
                    arr.push("...");
                    break;
                }
                arr.push(k + ": " + _stringify(o[k], depth + 1));
            }
            return "{" + arr.join(", ") + "}";
        }
    }

    // Create namespaces
    var RapidContext = window.RapidContext || (window.RapidContext = {});
    var module = RapidContext.Log || (RapidContext.Log = {});

    // Export namespace symbols
    module.history = history;
    module.clear = clear;
    module.level = level;
    module.context = context;
    module.error = logError;
    module.warn = logWarn;
    module.info = logInfo;
    module.log = module.debug = module.trace = logDebug;
    module.logBrowserInfo = logBrowserInfo;
    module.stringify = stringify;

    // Modify global console object
    _consoleSetup();

    // Install global error handler
    window.onerror = _windowErrorHandler;

})(this);
