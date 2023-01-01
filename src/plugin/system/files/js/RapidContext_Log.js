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

/**
 * @name RapidContext.Log
 * @namespace Provides a logging service for debugging apps and server calls.
 *
 * All log messages are filtered by log level and either discarded or
 * stored to an internal array. Log messages on the error or warning levels
 * are also sent to the server for remote logging.
 *
 * This module replaces the built-in `console.error()`, `console.warn()`,
 * `console.info()`, `console.log()` and `console.debug()` functions with its
 * own versions, passing through the log messages if not filtered.
 */
(function (window) {

    // The original console logger functions
    var backup = {};

    // The current log state
    var state = {
        count: 0,
        level: 3,
        context: null,
        history: [],
        publish: {
            last: 0,
            timer: null
        }
    };

    // The configuration settings
    var config = {
        interval: 10000,
        url: "rapidcontext/log",
        filter: null,
        publisher: null
    };

    /**
     * Initializes and configures the logging module. Will modify the `console`
     * object for logging. This replaces the default `console.error`,
     * `console.warn`, `console.info`, `console.log` and `console.debug`
     * functions. Safe to call multiple times to change or update config.
     *
     * @param {Object} [opts] the log configuration options, or null
     * @config {Number} [interval] the publish delay in millis, default is 10s
     * @config {String} [url] the publish URL endpoint
     * @config {Function} [filter] the event filter, returns a boolean
     * @config {Function} [publisher] the event publisher, returns a Promise
     *
     * @memberof RapidContext.Log
     */
    function init(opts) {
        function overwrite(obj, key, fn) {
            if (obj[key] !== fn) {
                backup[key] = obj[key] || function () {};
                obj[key] = fn;
            }
        }
        if (typeof(window.console) !== "object") {
            window.console = {};
        }
        overwrite(window.console, "error", error);
        overwrite(window.console, "warn", warn);
        overwrite(window.console, "info", info);
        overwrite(window.console, "log", debug);
        overwrite(window.console, "debug", debug);
        overwrite(window, "onerror", _onerror);
        opts = opts || {};
        config.interval = opts.interval || config.interval;
        config.url = opts.url || config.url;
        config.filter = opts.filter || _isErrorOrWarning;
        config.publisher = opts.publisher || _publish;
    }

    /**
     * Clears the log console and the array of stored messages.
     *
     * @see RapidContext.Log.history
     *
     * @memberof RapidContext.Log
     */
    function clear() {
        state.history = [];
        if (window.console && typeof(window.console.clear) === "function") {
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
        return state.history.slice(0);
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
                state.level = 0;
            } else if (value === 1 || /^err/i.test(value)) {
                state.level = 1;
            } else if (value === 2 || /^warn/i.test(value)) {
                state.level = 2;
            } else if (value === 3 || /^info/i.test(value)) {
                state.level = 3;
            } else if (value === 4 || /^(log|debug|trace)/i.test(value)) {
                state.level = 4;
            } else {
                state.level = 5;
            }
        }
        if (state.level <= 0) {
            return "none";
        } else if (state.level <= 1) {
            return "error";
        } else if (state.level <= 2) {
            return "warn";
        } else if (state.level <= 3) {
            return "info";
        } else if (state.level <= 4) {
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
            state.context = value;
            if (!value) {
                // Clear group immediately, but create new group on first log
                _group();
            }
        }
        return state.context;
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
        if (state.level >= 1) {
            var args = Array.prototype.slice.call(arguments);
            _log("error", args);
            _store("error", args.map(stringify));
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
        if (state.level >= 2) {
            var args = Array.prototype.slice.call(arguments);
            _log("warn", args);
            _store("warn", args.map(stringify));
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
        if (state.level >= 3) {
            var args = Array.prototype.slice.call(arguments);
            _log("info", args);
            _store("info", args.map(stringify));
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
     * @memberof RapidContext.Log
     */
    function debug(msg/**, ...*/) {
        if (state.level >= 4) {
            var args = Array.prototype.slice.call(arguments);
            _log("log", args);
            _store("log", args.map(stringify));
        }
    }

    /**
     * Handles window.onerror events (global uncaught errors).
     */
    function _onerror(msg, url, line, col, err) {
        if (err instanceof Error && err.stack) {
            error(msg || "Uncaught error", err);
        } else {
            var location = [url, line, col].filter(Boolean).join(":");
            error(msg || "Uncaught error", location, err);
        }
        return true;
    }

    /**
     * Logs a message to one of the console loggers.
     *
     * @param {String} level the log level (i.e. function 'error', 'warn'...)
     * @param {Array} args the log message & data (as raw objects)
     */
    function _log(level, args) {
        var logger = backup[level];
        if (typeof(logger) === "function") {
            _group(state.context);
            logger.apply(window.console, args);
        }
    }

    /**
     * Calls the `console.group` and `console.groupEnd` functions (if
     * they exist) to change the group label (if needed).
     *
     * @param {String} label the log context label
     */
    function _group(label) {
        var console = window.console;
        if (console._group !== label) {
            if (console._group) {
                delete console._group;
                if (typeof(console.groupEnd) === "function") {
                    console.groupEnd();
                }
            }
            if (label) {
                console._group = label;
                if (typeof(console.group) === "function") {
                    console.group(label + ":");
                }
            }
        }
    }

    /**
     * Stores a log message to the history.
     *
     * @param {String} level the message log level
     * @param {Array} args the log message arguments (as strings)
     */
    function _store(level, args) {
        var m = 1;
        for (; m < args.length; m++) {
            if (args[m] && args[m].indexOf("\n") >= 0) {
                break;
            }
        }
        state.history.push({
            id: ++state.count,
            time: new Date(),
            level: level,
            context: state.context,
            message: args.slice(0, m).join(" "),
            data: (m < args.length) ? args.slice(m).join("\n") : null
        });
        state.history.splice(0, Math.max(0, state.history.length - 100));
        if (!state.publish.timer) {
            state.publish.timer = setTimeout(_publishLoop, 100);
        }
    }

    /**
     * Handles stored events publishing in a timer loop.
     */
    function _publishLoop() {
        function isValid(evt) {
            lastId = evt.id;
            return evt.id > state.publish.last && config.filter(evt);
        }
        function onSuccess() {
            state.publish.last = lastId;
            state.publish.timer = setTimeout(_publishLoop, config.interval);
        }
        function onError(err) {
            info("error publishing log events", err);
            state.publish.timer = setTimeout(_publishLoop, config.interval);
        }
        var lastId = 0;
        var events = state.history.filter(isValid);
        if (events.length <= 0) {
            state.publish.last = lastId;
            state.publish.timer = null;
        } else {
            try {
                config.publisher(events).then(onSuccess, onError);
            } catch (e) {
                onError(e);
            }
        }
    }

    /**
     * Publishes events to the server.
     */
    function _publish(events) {
        return $.ajax(config.url, {
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify(events)
        });
    }

    /**
     * Checks if an event is an error or a warning.
     */
    function _isErrorOrWarning(evt) {
        return !!evt && /error|warn/.test(evt.level);
    }

    /**
     * Creates a string representation (suitable for logging) for any value or
     * object. The returned string is similar to a JSON representation of the
     * value, but may be simplified for increased readability.
     *
     * @param {Object} val the value or object to convert
     *
     * @return {String} the string representation of the value
     *
     * @memberof RapidContext.Log
     */
    function stringify(val) {
        var isObject = Object.prototype.toString.call(val) === "[object Object]";
        var isArray = val instanceof Array;
        var isSerializable = val && typeof(val.toJSON) === "function";
        if (val && (isObject || isArray || isSerializable)) {
            try {
                return JSON.stringify(val, null, 2);
            } catch (e) {
                return String(val);
            }
        } else if (val instanceof Error) {
            var parts = [val.toString()];
            if (val.stack) {
                var stack = String(val.stack).trim()
                    .replace(val.toString() + "\n", "")
                    .replace(/https?:.*\//g, "")
                    .replace(/\?[^:\s]+:/g, ":")
                    .replace(/@(.*)/mg, " ($1)")
                    .replace(/^(\s+at\s+)?/mg, "    at ");
                parts.push("\n", stack);
            }
            return parts.join("");
        } else if (val && (val.nodeType === 1 || val.nodeType === 9)) {
            var el = val.documentElement || val.document || val;
            var xml = el.outerHTML || el.outerXML || el.xml || String(el);
            var children = el.childNodes && el.childNodes.length;
            return xml.replace(/>[^<]*/, children ? ">..." : "/>");
        } else {
            return String(val);
        }
    }

    // Create namespaces
    var RapidContext = window.RapidContext || (window.RapidContext = {});
    var module = RapidContext.Log || (RapidContext.Log = {});

    // Export namespace symbols
    module.init = init;
    module.history = history;
    module.clear = clear;
    module.level = level;
    module.context = context;
    module.error = error;
    module.warn = warn;
    module.info = info;
    module.log = module.debug = module.trace = debug;
    module.stringify = stringify;

    // Install console loggers and global error handler
    init();

})(this);
