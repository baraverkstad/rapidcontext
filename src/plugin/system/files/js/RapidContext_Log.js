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

    // The old console logging functions
    var consoleError = null;
    var consoleWarn = null;
    var consoleInfo = null;
    var consoleLog = null;

    // The current log level
    var logLevel = 3;

    // The current log message count
    var logCount = 0;

    // The current log history
    var logHistory = [];

    // The current log context
    var logContext = null;

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
                _setupConsole();
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
     * @memberof RapidContext.Log
     */
    function error(msg/**, ...*/) {
        if (logLevel >= 1) {
            _setupConsole();
            var args = _stringifyArgs.apply(null, arguments);
            if (consoleError && isMSIE) {
                var ctx = (logContext ? logContext + ": " : "");
                consoleError.call(window.console, ctx + args.join(", "));
            } else if (consoleError) {
                consoleError.apply(window.console, arguments);
            }
            _store("error", args);
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
     * @memberof RapidContext.Log
     */
    function warn(msg/**, ...*/) {
        if (logLevel >= 2) {
            _setupConsole();
            var args = _stringifyArgs.apply(null, arguments);
            if (consoleWarn && isMSIE) {
                var ctx = (logContext ? logContext + ": " : "");
                consoleWarn.call(window.console, ctx + args.join(", "));
            } else if (consoleWarn) {
                consoleWarn.apply(window.console, arguments);
            }
            _store("warn", args);
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
     * @memberof RapidContext.Log
     */
    function info(msg/**, ...*/) {
        if (logLevel >= 3) {
            _setupConsole();
            var args = _stringifyArgs.apply(null, arguments);
            if (consoleInfo && isMSIE) {
                var ctx = (logContext ? logContext + ": " : "");
                consoleInfo.call(window.console, ctx + args.join(", "));
            } else if (consoleInfo) {
                consoleInfo.apply(window.console, arguments);
            }
            _store("info", args);
        }
    }

    /**
     * Logs a log (debug or trace) message with optional data. Also available
     * as the global `console.log()` and `console.debug()` functions.
     *
     * @param {String} msg the log message
     * @param {Object} [...] the additional log data or messages
     *
     * @example
     * console.log('init AJAX call to URL:', url);
     * ...
     * console.log('done AJAX call to URL:', url, responseCode);
     *
     * @memberof RapidContext.Log
     */
    function log(msg/**, ...*/) {
        if (logLevel >= 4) {
            _setupConsole();
            var args = _stringifyArgs.apply(null, arguments);
            if (consoleLog && isMSIE) {
                var ctx = (logContext ? logContext + ": " : "");
                consoleLog.call(window.console, ctx + args.join(", "));
            } else if (consoleLog) {
                consoleLog.apply(window.console, arguments);
            }
            _store("log", args);
        }
    }

    /**
     * Modifies the `console` object for logging. This replaces the default
     * `console.error`, `console.warn`, `console.info`, `console.log` and
     * `console.debug` functions (if not previously modified). Also calls the
     * `console.group` and `console.groupEnd` functions to adjust for the
     * current log context.
     */
    function _setupConsole() {
        var isModified = (window.console.error !== error) ||
                         (window.console.warn !== warn) ||
                         (window.console.info !== info) ||
                         (window.console.log !== log);
        if (isModified) {
            consoleError || (consoleError = window.console.error);
            consoleWarn || (consoleWarn = window.console.warn);
            consoleInfo || (consoleInfo = window.console.info);
            consoleLog || (consoleLog = window.console.log);
            window.console.error = error;
            window.console.warn = warn;
            window.console.info = info;
            window.console.log = log;
            window.console.debug = log;
        }
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
    }

    /**
     * Creates a string representation (suitable for logging) for any value or
     * object. The returned string is similar to a JSON representation of the
     * value, but may also be simplified for increased readability.
     *
     * @param {Object} o the value or object to convert
     * @param {Number} [depth=0] the current object depth (max is 4)
     *
     * @return {String} the string representation of the value
     *
     * @memberof RapidContext.Log
     */
    function stringify(o, depth) {
        depth = depth || 0;
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
                arr.push(stringify(o[i], depth + 1));
            }
            return "[" + arr.join(", ") + "]";
        } else {
            var indent = "";
            for (var i = 0; i < depth; i++) {
                indent += "    ";
            }
            var newline = "\n    " + indent;
            var arr = [];
            for (var k in o) {
                if (arr.length > 0) {
                    arr.push(",");
                }
                if (arr.length > 99) {
                    arr.push(newline, "...");
                    break;
                }
                arr.push(newline, k, ": ", stringify(o[k], depth + 1));
            }
            if (arr.length > 0) {
                arr.push("\n", indent);
            }
            return "{" + arr.join("") + "}";
        }
    }

    /**
     * Creates a string representation (suitable for logging) for all the
     * arguments to this function.
     *
     * @param {Object} [...] the values or objects to convert
     *
     * @return {Array} the array of string representations
     *
     * @private
     */
    function _stringifyArgs(/*...*/) {
        var res = [];
        for (var i = 0; i < arguments.length; i++) {
            res.push(stringify(arguments[i]));
        }
        return res;
    }

    // Create namespaces
    var RapidContext = window.RapidContext || (window.RapidContext = {});
    var module = RapidContext.Log || (RapidContext.Log = {});

    // Export namespace symbols
    module.history = history;
    module.clear = clear;
    module.level = level;
    module.context = context;
    module.error = error;
    module.warn = warn;
    module.info = info;
    module.log = module.debug = module.trace = log;
    module.stringify = stringify;

    // Modify global console object
    window.console || (window.console = {});
    _setupConsole();

})(this);
