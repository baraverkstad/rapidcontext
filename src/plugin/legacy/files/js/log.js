/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2012 Per Cederberg & Dynabyte AB.
 * All rights reserved.
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
 * @namespace Provides functions for application and debug logging.
 */
var LOG = { nextId: 1, entries: [], max: 100, enabled: {} }

/**
 * The error log level constant.
 */
LOG.ERROR = "Error";
LOG.enabled[LOG.ERROR] = true;

/**
 * The warning log level constant.
 */
LOG.WARNING = "Warning";
LOG.enabled[LOG.WARNING] = true;

/**
 * The info log level constant.
 */
LOG.INFO = "Info"
LOG.enabled[LOG.INFO] = true;

/**
 * The trace log level constant.
 */
LOG.TRACE = "Trace";
LOG.enabled[LOG.TRACE] = false;

/**
 * Clears all stored log messages.
 */
LOG.clear = function() {
    this.entries = [];
}

/**
 * Adds an error log message.
 *
 * @param message            the log message
 * @param data               the log data, or null for none
 */
LOG.error = function(message, data) {
    this._log(LOG.ERROR, message, data);
}

/**
 * Adds a warning log message.
 *
 * @param message            the log message
 * @param data               the log data, or null for none
 */
LOG.warning = function(message, data) {
    this._log(LOG.WARNING, message, data);
}

/**
 * Adds an information log message.
 *
 * @param message            the log message
 * @param data               the log data, or null for none
 */
LOG.info = function(message, data) {
    this._log(LOG.INFO, message, data);
}

/**
 * Adds a trace log message. Note that trace messages are normally
 * not enabled.
 *
 * @param message            the log message
 * @param data               the log data, or null for none
 */
LOG.trace = function(message, data) {
    this._log(LOG.TRACE, message, data);
}

/**
 * Adds a log message.
 *
 * @param level              the log level (see level constants)
 * @param message            the log message
 * @param data               the log data, or null for none
 *
 * @private
 */
LOG._log = function(level, message, data) {
    if (this.enabled[level]) {
        var trace = RapidContext.Util.stackTrace().slice(2);
        var caller = (trace.length > 0) ? trace[0] : null;
        var context = (trace.length > 0) ? trace[trace.length - 1] : null;
        this.entries.push({ id: this.nextId++, time: new Date(), level: level,
                            context: context, caller: caller, stackTrace: trace,
                            message: message, data: this._toJSON(data) });
        if (this.entries.length > this.maxEntries + 10) {
            this.entries.splice(0, 10);
        }
    }
}

/**
 * Creates a JSON data structure from a generic one. Functions will
 * be replaced by their names, and DOM nodes and widgets will be
 * converted into strings. The function is recursive with a maximum
 * call depth of 3, to be able to handle circular data structures.
 *
 * @param data               the data to convert
 *
 * @return the JSON data structure suitable for logging
 *
 * @private
 */
LOG._toJSON = function(data) {
    var self = arguments.callee;
    var res;

    if (self.depth == null) {
        self.depth = 0;
    }
    if (data == null) {
        res = null;
    } else if (typeof(data) === "boolean") {
        res = data;
    } else if (typeof(data) === "number") {
        res = data;
    } else if (typeof(data) === "string") {
        res = data;
    } else if (typeof(data) === "function") {
        res = RapidContext.Util.functionName(data);
    } else if (data.nodeType != null) {
        res = "<node nodeType:" + data.nodeType + ",tagName:" + data.tagName +
              ",id:" + data.id + ",className:" + data.className + ">";
    } else if (data.domNode != null) {
        res = "<widget class:" + RapidContext.Util.functionName(data.constructor) +
              ",id:" + data.id + ">";
    } else if (self.depth > 3) {
        res = "...";
    } else if (data instanceof Array) {
        self.depth++;
        var res = [];
        for (var i = 0; i < data.length; i++) {
            res[i] = self.call(null, data[i]);
        }
        self.depth--;
    } else {
        self.depth++;
        var res = {};
        for (var name in data) {
            res[name] = self.call(null, data[name]);
        }
        self.depth--;
    }
    return res;
}
