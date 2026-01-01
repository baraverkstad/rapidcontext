/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2026 Per Cederberg. All rights reserved.
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
RapidContext.Widget.isFormField = RapidContext.deprecatedFunction(
    function (obj) {
        if (!RapidContext.Util.isHTML(obj) || typeof(obj.tagName) !== "string") {
            return false;
        }
        var tagName = obj.tagName.toUpperCase();
        return tagName == "INPUT" ||
               tagName == "TEXTAREA" ||
               tagName == "SELECT" ||
               RapidContext.Widget.isWidget(obj, "Field");
    },
    "RapidContext.Widget.isFormField() is deprecated"
);

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
 */
RapidContext.Widget.emitSignal = RapidContext.deprecatedFunction(
    function (node, sig/*, ...*/) {
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
    },
    "RapidContext.Widget.emitSignal() is deprecated, use emit() method instead"
);

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
RapidContext.Widget._eventHandler = RapidContext.deprecatedFunction(
    function (className, methodName/*, ...*/) {
        var baseArgs = Array.prototype.slice.call(arguments, 2);
        return function (evt) {
            var node = this;
            while (!RapidContext.Widget.isWidget(node, className)) {
                node = node.parentNode;
            }
            var e = new MochiKit.Signal.Event(this, evt);
            return node[methodName].apply(node, baseArgs.concat([e]));
        };
    },
    "RapidContext.Widget._eventHandler() is deprecated, use arrow functions instead"
);

/**
 * Dispatches a custom event from this DOM node. The event will be
 * created and emitted asynchronously (via setTimeout).
 *
 * @param {string} type the event type (e.g. `validate`)
 * @param {Object} [opts] the event options (e.g. `{ bubbles: true }`)
 */
RapidContext.Widget.prototype._dispatch = RapidContext.deprecatedFunction(
    function (type, opts) {
        this.emit(type, opts);
    },
    "RapidContext.Widget._dispatch() is deprecated, use emit() method instead"
);

/**
 * Performs a visual effect animation on this widget. This is
 * implemented using the `MochiKit.Visual` effect package. All options
 * sent to this function will be passed on to the appropriate
 * `MochiKit.Visual` function.
 *
 * @param {Object} opts the visual effect options
 * @param {string} opts.effect the MochiKit.Visual effect name
 * @param {string} opts.queue the MochiKit.Visual queue handling,
 *            defaults to "replace" and a unique scope for each widget
 *            (see `MochiKit.Visual` for full options)
 *
 * @example
 * widget.animate({ effect: "fade", duration: 0.5 });
 * widget.animate({ effect: "Move", transition: "spring", y: 300 });
 */
RapidContext.Widget.prototype.animate = RapidContext.deprecatedFunction(
    function (opts) {
        let queue = { scope: this.uid(), position: "replace" };
        opts = MochiKit.Base.updatetree({ queue: queue }, opts);
        if (typeof(opts.queue) == "string") {
            queue.position = opts.queue;
            opts.queue = queue;
        }
        let func = MochiKit.Visual[opts.effect];
        if (typeof(func) == "function") {
            func.call(null, this, opts);
        }
    },
    "RapidContext.Widget.animate() is deprecated, use CSS animations instead"
);
