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

// Namespace initialization
if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}

/**
 * @namespace The base class for the HTML user interface widgets.
 *     The Widget class shouldn't be instantiated directly, instead
 *     one of the subclasses should be instantiated.
 */
RapidContext.Widget = function () {
    throw new ReferenceError("cannot call Widget constructor");
};

/**
 * The global widget registry. This is a widget lookup table where
 * all widgets should have an entry. The entries should be added as
 * the JavaScript file are loaded. Each widget is indexed by the
 * widget name (class name) and point to the constructor function.
 */
RapidContext.Widget.Classes = {};

/**
 * Function to return unique identifiers.
 *
 * @return {Number} the next number in the sequence
 */
RapidContext.Widget._nextId = MochiKit.Base.counter();

/**
 * Checks if the specified object is a widget. Any non-null object
 * that looks like a DOM node and has the element class "widget"
 * will cause this function to return `true`. Otherwise, `false` will
 * be returned. As an option, this function can also check if the
 * widget has a certain class by checking for an additional CSS
 * class "widget<className>" (which is a standard followed by all
 * widgets).
 *
 * @param {Object} obj the object to check
 * @param {String} [className] the optional widget class name
 *
 * @return {Boolean} `true` if the object looks like a widget, or
 *         `false` otherwise
 *
 * @static
 */
RapidContext.Widget.isWidget = function (obj, className) {
    if (className != null) {
        return RapidContext.Util.isHTML(obj) &&
               MochiKit.DOM.hasElementClass(obj, "widget") &&
               MochiKit.DOM.hasElementClass(obj, "widget" + className);
    } else {
        return RapidContext.Util.isHTML(obj) &&
               MochiKit.DOM.hasElementClass(obj, "widget");
    }
};

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
 * Adds all functions from a widget class to a DOM node. This will also convert
 * the DOM node into a widget by adding the "widget" CSS class and all the
 * default widget functions from `Widget.prototype`.
 *
 * The default widget functions are added non-destructively, using the prefix
 * "__" if also defined in the widget class.
 *
 * @param {Node} node the DOM node to modify
 * @param {Object/Function} [...] the widget class or constructor
 *
 * @return {Widget} the widget DOM node
 */
RapidContext.Widget._widgetMixin = function (node/*, objOrClass, ...*/) {
    MochiKit.DOM.addElementClass(node, "widget");
    var protos = MochiKit.Base.extend([], arguments, 1);
    protos.push(RapidContext.Widget);
    while (protos.length > 0) {
        var obj = protos.pop();
        if (typeof(obj) === "function") {
            obj = obj.prototype;
        }
        for (var key in obj) {
            var prevKey = "__" + key;
            if (key in node) {
                node[prevKey] = node[key];
            }
            try {
                node[key] = obj[key];
            } catch (e) {
                var msg = "failed to overwrite '" + key + "' in DOM node";
                RapidContext.Log.error(msg, node, e);
            }
        }
    }
    return node;
};

/**
 * Creates a new widget with the specified name, attributes and
 * child widgets or DOM nodes. The widget class name must have been
 * registered in the `RapidContext.Widget.Classes` lookup table, or an
 * exception will be thrown. This function is identical to calling
 * the constructor function directly.
 *
 * @param {String} name the widget class name
 * @param {Object} attrs the widget and node attributes
 * @param {Object} [...] the child widgets or DOM nodes
 *
 * @return {Widget} the widget DOM node
 *
 * @throws {ReferenceError} if the widget class name couldn't be
 *             found in `RapidContext.Widget.Classes`
 *
 * @static
 */
RapidContext.Widget.createWidget = function (name, attrs/*, ...*/) {
    var cls = RapidContext.Widget.Classes[name];
    if (cls == null) {
        throw new ReferenceError("failed to find widget '" + name +
                                 "' in RapidContext.Widget.Classes");
    }
    return cls.apply(this, MochiKit.Base.extend([], arguments, 1));
};

/**
 * Destroys a widget or a DOM node. This function will remove the DOM
 * node from the tree, disconnect all signals and call all widget
 * destructor functions. The same procedure will also be applied
 * recursively to all child nodes. Once destroyed, all references to
 * the widget object should be cleared in order for the browser to
 * be able to reclaim the memory used.
 *
 * @param {Widget/Node/Array} node the (widget) DOM node or list
 *
 * @static
 */
RapidContext.Widget.destroyWidget = function (node) {
    if (node.nodeType != null) {
        if (typeof(node.destroy) == "function") {
            node.destroy();
        }
        if (node.parentNode != null) {
            MochiKit.DOM.removeElement(node);
        }
        MochiKit.Signal.disconnectAll(node);
        MochiKit.Signal.disconnectAllTo(node);
        while (node.firstChild != null) {
            RapidContext.Widget.destroyWidget(node.firstChild);
        }
    } else if (MochiKit.Base.isArrayLike(node)) {
        for (var i = node.length - 1; i >= 0; i--) {
            RapidContext.Widget.destroyWidget(node[i]);
        }
    }
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
    var baseArgs = MochiKit.Base.extend([], arguments, 2);
    return function (evt) {
        var node = this;
        while (!RapidContext.Widget.isWidget(node, className)) {
            node = node.parentNode;
        }
        var e = new MochiKit.Signal.Event(this, evt);
        return node[methodName].apply(node, baseArgs.concat([e]));
    };
};

/**
 * Emits an asynchronous signal to any listeners connected with
 * MochiKit.Signal. This function will log any errors to the default
 * error log in `MochiKit.Logging`.
 *
 * Note that this function is an internal helper function for the
 * widgets and shouldn't be called by external code.
 *
 * @param {Widget} node the widget DOM node
 * @param {String} sig the signal name ("onclick" or similar)
 * @param {Object} [...] the optional signal arguments
 *
 * @deprecated Use _fireEvent() instead to emit proper Event object.
 */
RapidContext.Widget.emitSignal = function (node, sig/*, ...*/) {
    var args = $.makeArray(arguments);
    function later() {
        try {
            MochiKit.Signal.signal.apply(MochiKit.Signal, args);
        } catch (e) {
            var msg = "Exception in signal '" + sig + "' handler";
            MochiKit.Logging.logError(msg, e);
        }
    }
    setTimeout(later);
};

/**
 * Creates a DOM event object.
 *
 * @param {String} evtType the event type name ("click" or similar)
 * @param {Object} [detail] the optional event "detail" value
 *
 * @return {Event} a new Event object of the specified type
 */
RapidContext.Widget._createEvent = function (evtType, detail) {
    var evt;
    try {
        evt = new Event(evtType);
    } catch (msieFailsOfCourse) {
        if (document.createEvent) {
            evt = document.createEvent("Event");
            evt.initEvent(evtType, true, true);
        } else {
            evt = document.createEventObject();
            evt.type = evtType;
        }
    }
    if (detail) {
        evt.detail = detail;
    }
    return evt;
};

/**
 * Fires an event (asynchronously) to all listeners. This function uses
 * native DOM APIs, so all type of event listeners should be reached
 * by the event.
 *
 * @param {Widget/Node} node the (widget) DOM node emitting the event
 * @param {Event/String} evt the event object or type
 * @param {Object} [detail] the optional event "detail" value
 */
RapidContext.Widget._fireEvent = function (node, evt, detail) {
    if (typeof(evt) == "string") {
        evt = RapidContext.Widget._createEvent(evt, detail);
    }
    function later() {
        try {
            if (node.dispatchEvent) {
                return node.dispatchEvent(evt);
            } else if (node.parentNode && node.fireEvent) {
                // MSIE 6-8
                return node.fireEvent("on" + evt.type, evt);
            }
        } catch (e) {
            RapidContext.Log.error("Failed to fire event", evt, node);
        }
    }
    setTimeout(later);
};

/**
 * Returns the unique identifier for this DOM node. If a node id has
 * already been set, that id will be returned. Otherwise a new id
 * will be generated and assigned to the widget DOM node.
 *
 * @return {String} the the unique DOM node identifier
 */
RapidContext.Widget.prototype.uid = function () {
    if (!this.id) {
        this.id = "widget" + RapidContext.Widget._nextId();
    }
    return this.id;
};

/**
 * The internal widget destructor function. This method should only
 * be called by `destroyWidget()` and may be overridden by subclasses.
 * By default this method does nothing.
 */
RapidContext.Widget.prototype.destroy = function () {
    // Nothing to do by default
};

/**
 * Returns the widget container DOM node. By default this method
 * returns the widget itself, but subclasses may override it to place
 * child DOM nodes in a different container.
 *
 * @return {Node} the container DOM node, or
 *         null if this widget has no container
 */
RapidContext.Widget.prototype._containerNode = function () {
    return this;
};

/**
 * Returns the widget style DOM node. By default this method returns
 * the widget itself, but subclasses may override it to move widget
 * styling (but not sizing or positioning) to a subnode.
 *
 * @return {Node} the style DOM node
 */
RapidContext.Widget.prototype._styleNode = function () {
    return this;
};

/**
 * Updates the widget or HTML DOM node attributes. This method is
 * sometimes overridden by individual widgets to allow modification
 * of additional widget attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {Boolean} [attrs.disabled] the disabled widget flag
 * @param {Boolean} [attrs.hidden] the hidden widget flag
 * @param {String} [attrs.class] the CSS class names
 */
RapidContext.Widget.prototype.setAttrs = function (attrs) {
    for (var name in attrs) {
        var value = attrs[name];
        if (name == "disabled") {
            this._setDisabled(value);
        } else if (name == "hidden") {
            this._setHidden(value);
        } else if (name == "class") {
            var elem = this._styleNode();
            this.removeClass.apply(this, elem.className.split(/\s+/));
            this.addClass.apply(this, value.split(/\s+/));
        } else if (name == "style") {
            if (typeof(value) == "string") {
                var styles = {};
                var parts = value.split(";");
                for (var i = 0; i < parts.length; i++) {
                    var a = parts[i].split(":");
                    var k = MochiKit.Format.strip(a[0]);
                    if (k != "" && a.length > 1) {
                        styles[k] = MochiKit.Format.strip(a[1]);
                    }
                }
                value = styles;
            }
            this.setStyle(value);
        } else if (value != null) {
            MochiKit.DOM.setNodeAttribute(this, name, value);
            if (typeof(value) != "object") {
                try {
                    this[name] = value;
                } catch (ignore) {
                    // IE8: breaks on setting button.type property
                }
            }
        } else {
            this.removeAttribute(name);
            delete this[name];
        }
    }
};

/**
 * Updates the CSS styles of this HTML DOM node. This method is
 * identical to `MochiKit.Style.setStyle`, but uses "this" as the
 * first argument.
 *
 * @param {Object} styles an object with the styles to set
 *
 * @example
 * widget.setStyle({ "font-size": "bold", "color": "red" });
 */
RapidContext.Widget.prototype.setStyle = function (styles) {
    styles = MochiKit.Base.update({}, styles);
    var posDimNames = ["width", "height", "zIndex", "z-index",
                       "position", "top", "bottom", "left", "right"];
    var posDimStyles = RapidContext.Util.mask(styles, posDimNames);
    MochiKit.Style.setStyle(this, posDimStyles);
    MochiKit.Style.setStyle(this._styleNode(), styles);
};

/**
 * Checks if this HTML DOM node has the specified CSS class names.
 * Note that more than one CSS class name may be checked, in which
 * case all must be present.
 *
 * @param {String} [...] the CSS class names to check
 *
 * @return {Boolean} `true` if all CSS classes were present, or
 *         `false` otherwise
 */
RapidContext.Widget.prototype.hasClass = function (/* ... */) {
    var elem = this._styleNode();
    for (var i = 0; i < arguments.length; i++) {
        if (!MochiKit.DOM.hasElementClass(elem, arguments[i])) {
            return false;
        }
    }
    return true;
};

/**
 * Adds the specified CSS class names to this HTML DOM node.
 *
 * @param {String} [...] the CSS class names to add
 */
RapidContext.Widget.prototype.addClass = function (/* ... */) {
    var elem = this._styleNode();
    for (var i = 0; i < arguments.length; i++) {
        MochiKit.DOM.addElementClass(elem, arguments[i]);
    }
};

/**
 * Removes the specified CSS class names from this HTML DOM node.
 * Note that this method will not remove any class starting with
 * "widget".
 *
 * @param {String} [...] the CSS class names to remove
 */
RapidContext.Widget.prototype.removeClass = function (/* ... */) {
    var elem = this._styleNode();
    for (var i = 0; i < arguments.length; i++) {
        var name = "" + arguments[i];
        if (name.indexOf("widget") != 0) {
            MochiKit.DOM.removeElementClass(elem, name);
        }
    }
};

/**
 * Toggles adding and removing the specified CSS class names to and
 * from this HTML DOM node. If all the CSS classes are already set,
 * they will be removed. Otherwise they will be added.
 *
 * @param {String} [...] the CSS class names to remove
 *
 * @return {Boolean} `true` if the CSS classes were added, or
 *         `false` otherwise
 */
RapidContext.Widget.prototype.toggleClass = function (/* ... */) {
    if (this.hasClass.apply(this, arguments)) {
        this.removeClass.apply(this, arguments);
        return false;
    } else {
        this.addClass.apply(this, arguments);
        return true;
    }
};

/**
 * Checks if this widget is disabled. This method checks both the
 * "widgetDisabled" CSS class and the `disabled` property. Changes
 * to the disabled status can be made with `enable()`, `disable()` or
 * `setAttrs()`.
 *
 * @return {Boolean} `true` if the widget is disabled, or
 *         `false` otherwise
 */
// FIXME: This function is unreachable in MSIE, due to a dynamic attribute
//        with the same name (on all DOM nodes).
RapidContext.Widget.prototype.isDisabled = function () {
    return this.disabled === true &&
           MochiKit.DOM.hasElementClass(this, "widgetDisabled");
};

/**
 * Performs the changes corresponding to setting the `disabled`
 * widget attribute.
 *
 * @param {Boolean} value the new attribute value
 */
RapidContext.Widget.prototype._setDisabled = function (value) {
    value = MochiKit.Base.bool(value);
    if (value) {
        MochiKit.DOM.addElementClass(this, "widgetDisabled");
    } else {
        MochiKit.DOM.removeElementClass(this, "widgetDisabled");
    }
    MochiKit.DOM.setNodeAttribute(this, "disabled", value);
    this.disabled = value;
};

/**
 * Enables this widget if it was previously disabled. This is
 * equivalent to calling `setAttrs({ disabled: false })`.
 */
RapidContext.Widget.prototype.enable = function () {
    this.setAttrs({ disabled: false });
};

/**
 * Disables this widget if it was previously enabled. This method is
 * equivalent to calling `setAttrs({ disabled: true })`.
 */
RapidContext.Widget.prototype.disable = function () {
    this.setAttrs({ disabled: true });
};

/**
 * Checks if this widget node is hidden. This method checks for the
 * existence of the `widgetHidden` CSS class. It does NOT check the
 * actual widget visibility (the `display` style property set by
 * animations for example).
 *
 * @return {Boolean} `true` if the widget is hidden, or
 *         `false` otherwise
 */
RapidContext.Widget.prototype.isHidden = function () {
    return MochiKit.DOM.hasElementClass(this, "widgetHidden");
};

/**
 * Performs the changes corresponding to setting the `hidden`
 * widget attribute.
 *
 * @param {Boolean} value the new attribute value
 */
RapidContext.Widget.prototype._setHidden = function (value) {
    value = MochiKit.Base.bool(value);
    if (value) {
        MochiKit.DOM.addElementClass(this, "widgetHidden");
    } else {
        MochiKit.DOM.removeElementClass(this, "widgetHidden");
    }
    MochiKit.DOM.setNodeAttribute(this, "hidden", value);
    this.hidden = value;
};

/**
 * Shows this widget node if it was previously hidden. This method is
 * equivalent to calling `setAttrs({ hidden: false })`. It is safe
 * for all types of widgets, since it only removes the `widgetHidden`
 * CSS class instead of setting the `display` style property.
 */
RapidContext.Widget.prototype.show = function () {
    this.setAttrs({ hidden: false });
};

/**
 * Hides this widget node if it was previously visible. This method
 * is equivalent to calling `setAttrs({ hidden: true })`. It is safe
 * for all types of widgets, since it only adds the `widgetHidden`
 * CSS class instead of setting the `display` style property.
 */
RapidContext.Widget.prototype.hide = function () {
    this.setAttrs({ hidden: true });
};

/**
 * Performs a visual effect animation on this widget. This is
 * implemented using the `MochiKit.Visual` effect package. All options
 * sent to this function will be passed on to the appropriate
 * `MochiKit.Visual` function.
 *
 * @param {Object} opts the visual effect options
 * @param {String} opts.effect the MochiKit.Visual effect name
 * @param {String} opts.queue the MochiKit.Visual queue handling,
 *            defaults to "replace" and a unique scope for each widget
 *            (see `MochiKit.Visual` for full options)
 *
 * @example
 * widget.animate({ effect: "fade", duration: 0.5 });
 * widget.animate({ effect: "Move", transition: "spring", y: 300 });
 */
RapidContext.Widget.prototype.animate = function (opts) {
    var queue = { scope: this.uid(), position: "replace" };
    opts = MochiKit.Base.updatetree({ queue: queue }, opts);
    if (typeof(opts.queue) == "string") {
        queue.position = opts.queue;
        opts.queue = queue;
    }
    var func = MochiKit.Visual[opts.effect];
    if (typeof(func) == "function") {
        func.call(null, this, opts);
    }
};

/**
 * Blurs (unfocuses) this DOM node and all relevant child nodes. This function
 * will recursively blur all `<a>`, `<button>`, `<input>`, `<textarea>` and
 * `<select>` child nodes found.
 */
RapidContext.Widget.prototype.blurAll = function () {
    RapidContext.Util.blurAll(this);
};

/**
 * Returns an array with all child DOM nodes. Note that the array is
 * a real JavaScript array, not a dynamic `NodeList`. This method is
 * sometimes overridden by child widgets in order to hide
 * intermediate DOM nodes required by the widget.
 *
 * @return {Array} the array of child DOM nodes
 */
RapidContext.Widget.prototype.getChildNodes = function () {
    var elem = this._containerNode();
    return elem ? MochiKit.Base.extend([], elem.childNodes) : [];
};

/**
 * Adds a single child DOM node to this widget. This method is
 * sometimes overridden by child widgets in order to hide or control
 * intermediate DOM nodes required by the widget.
 *
 * @param {Widget/Node} child the DOM node to add
 */
RapidContext.Widget.prototype.addChildNode = function (child) {
    var elem = this._containerNode();
    if (elem) {
        elem.appendChild(child);
    } else {
        throw new Error("cannot add child node, widget is not a container");
    }
};

/**
 * Removes a single child DOM node from this widget. This method is
 * sometimes overridden by child widgets in order to hide or control
 * intermediate DOM nodes required by the widget.
 *
 * Note that this method will NOT destroy the removed child widget,
 * so care must be taken to ensure proper child widget destruction.
 *
 * @param {Widget/Node} child the DOM node to remove
 */
RapidContext.Widget.prototype.removeChildNode = function (child) {
    var elem = this._containerNode();
    if (elem) {
        elem.removeChild(child);
    }
};

/**
 * Adds one or more children to this widget. This method will flatten any
 * arrays among the arguments and ignores any `null` or `undefined` arguments.
 * Any DOM nodes or widgets will be added to the end, and other objects will be
 * converted to a text node first. Subclasses should normally override the
 * `addChildNode()` method instead of this one, since that is the basis for
 * DOM node insertion.
 *
 * @param {Object} [...] the children to add
 */
RapidContext.Widget.prototype.addAll = function (/* ... */) {
    var args = MochiKit.Base.flattenArray(arguments);
    for (var i = 0; i < args.length; i++) {
        if (args[i] == null) {
            // Ignore null values
        } else if (RapidContext.Util.isDOM(args[i])) {
            this.addChildNode(args[i]);
            // TODO: remove this call for performance
            RapidContext.Util.resizeElements(args[i]);
        } else {
            this.addChildNode(RapidContext.Util.createTextNode(args[i]));
        }
    }
};

/**
 * Removes all children to this widget. This method will also destroy and child
 * widgets and disconnect all signal listeners. This method uses the
 * `getChildNodes()` and `removeChildNode()` methods to find and remove the
 * individual child nodes.
 */
RapidContext.Widget.prototype.removeAll = function () {
    var children = this.getChildNodes();
    for (var i = children.length - 1; i >= 0; i--) {
        this.removeChildNode(children[i]);
        RapidContext.Widget.destroyWidget(children[i]);
    }
};
