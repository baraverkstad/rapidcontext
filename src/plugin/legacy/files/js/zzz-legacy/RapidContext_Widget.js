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

// Namespace initialization
if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}
RapidContext.Widget = RapidContext.Widget || {};

/**
 * Checks if the specified object is a form field. Any non-null object that
 * looks like a DOM node and is either an standard HTML form field (`<input>`,
 * `<textarea>` or `<select>`) or one with a "value" property will cause this
 * function to return `true`. Otherwise, `false` will be returned.
 *
 * @param {Object} obj the object to check
 *
 * @return {Boolean} `true` if the object looks like a form field, or
 *         `false` otherwise
 *
 * @static
 */
RapidContext.Widget.isFormField = function (obj) {
    RapidContext.deprecated("RapidContext.Widget.isFormField is deprecated.");
    if (!RapidContext.Util.isHTML(obj) || typeof(obj.tagName) !== "string") {
        return false;
    }
    var tagName = obj.tagName.toUpperCase();
    return tagName == "INPUT" ||
           tagName == "TEXTAREA" ||
           tagName == "SELECT" ||
           RapidContext.Widget.isWidget(obj, "Field");
};

/**
 * Emits an asynchronous signal to any listeners connected with
 * MochiKit.Signal.
 *
 * Note that this function is an internal helper function for the
 * widgets and shouldn't be called by external code.
 *
 * @param {Widget} node the widget DOM node
 * @param {String} sig the signal name ("onclick" or similar)
 * @param {Object} [...] the optional signal arguments
 *
 * @deprecated Use _dispatch() instead to emit a proper Event object.
 */
RapidContext.Widget.emitSignal = function (node, sig/*, ...*/) {
    RapidContext.deprecated("RapidContext.Widget.emitSignal is deprecated.");
    var args = $.makeArray(arguments);
    function later() {
        try {
            MochiKit.Signal.signal.apply(MochiKit.Signal, args);
        } catch (e) {
            var msg = "exception in signal '" + sig + "' handler";
            RapidContext.Log.error(msg, node, e);
        }
    }
    setTimeout(later);
};

/**
 * Creates an event handler function that will forward any calls to
 * another function. The other function must exist as a property in
 * a parent widget of the specified class.
 *
 * @param {String} className the parent widget class name, or null
 *                     to use the same node
 * @param {String} methodName the name of the method to call
 * @param {Object} [...] the additional method arguments
 *
 * @return {Function} a function that forwards calls as specified
 */
RapidContext.Widget._eventHandler = function (className, methodName/*, ...*/) {
    var baseArgs = Array.prototype.slice.call(arguments, 2);
    return function (evt) {
        var node = this;
        while (!RapidContext.Widget.isWidget(node, className)) {
            node = node.parentNode;
        }
        var e = new MochiKit.Signal.Event(this, evt);
        return node[methodName].apply(node, baseArgs.concat([e]));
    };
};
