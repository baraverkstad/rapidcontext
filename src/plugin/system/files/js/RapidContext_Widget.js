/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2012 Per Cederberg. All rights reserved.
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
 * will cause this function to return true. Otherwise, false will
 * be returned. As an option, this function can also check if the
 * widget has a certain class by checking for an additional CSS
 * class "widget<className>" (which is a standard followed by all
 * widgets).
 *
 * @param {Object} obj the object to check
 * @param {String} [className] the optional widget class name
 *
 * @return {Boolean} true if the object looks like a widget, or
 *         false otherwise
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
 * Checks if the specified object is a form field. Any non-null
 * object that looks like a DOM node and is either an standard HTML
 * form field (&lt;input&gt;, &lt;textarea&gt; or &lt;select&gt;) or
 * one with a "value" property will cause this function to return
 * true. Otherwise, false will be returned.
 *
 * @param {Object} obj the object to check
 *
 * @return {Boolean} true if the object looks like a form field, or
 *         false otherwise
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
 * Adds all functions from a widget class to a DOM node. This will
 * also convert the DOM node into a widget by adding the "widget"
 * CSS class and add all the default widget functions from the
 * standard Widget prototype. Functions are added non-destructively,
 * using the prefix "__" on the function name if is was already
 * defined. This means that existing functions will not be
 * overwritten and parent object functions will be available under a
 * different name.
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
        var obj = protos.shift();
        if (typeof(obj) === "function") {
            obj = obj.prototype;
        }
        for (var key in obj) {
            var parentKey = "__" + key;
            if (!(key in node)) {
                node[key] = obj[key];
            } else if (!(parentKey in node)) {
                node[parentKey] = obj[key];
            }
        }
    }
    return node;
};

/**
 * Creates a new widget with the specified name, attributes and
 * child widgets or DOM nodes. The widget class name must have been
 * registered in the RapidContext.Widget.Classes lookup table, or an
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
 *             found in RapidContext.Widget.Classes
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
 * Emits a signal to any listeners connected with MochiKit.Signal.
 * This function handles errors by logging them to the default error
 * log in MochiKit.Logging.<p>
 *
 * Note that this function is an internal helper function for the
 * widgets and shouldn't be called by external code.
 *
 * @param {Widget} node the widget DOM node
 * @param {String} sig the signal name ("onclick" or similar)
 * @param {Object} [...] the optional signal arguments
 *
 * @return {Boolean} true if the signal was processed correctly, or
 *         false if an exception was thrown
 */
RapidContext.Widget.emitSignal = function (node, sig/*, ...*/) {
    try {
        MochiKit.Signal.signal.apply(MochiKit.Signal, arguments);
        return true;
    } catch (e) {
        var msg = "Exception in signal '" + sig + "' handler";
        MochiKit.Logging.logError(msg, e);
        return false;
    }
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
 * be called by destroyWidget() and may be overridden by subclasses.
 * By default this method does nothing.
 */
RapidContext.Widget.prototype.destroy = function () {
    // Nothing to do by default
};

/**
 * Updates the widget or HTML DOM node attributes. This method is
 * sometimes overridden by individual widgets to allow modification
 * of widget attributes also available in the constructor.
 *
 * @param {Object} attrs the widget and node attributes to set
 */
RapidContext.Widget.prototype.setAttrs = function (attrs) {
    MochiKit.DOM.updateNodeAttributes(this, attrs);
};

/**
 * Updates the CSS styles of this HTML DOM node. This method is
 * identical to MochiKit.Style.setStyle, but uses "this" as the
 * first argument.
 *
 * @param {Object} styles an object with the styles to set
 *
 * @example
 * widget.setStyle({ "font-size": "bold", "color": "red" });
 */
RapidContext.Widget.prototype.setStyle = function (styles) {
    MochiKit.Style.setStyle(this, styles);
};

/**
 * Checks if this HTML DOM node has the specified CSS class names.
 * Note that more than one CSS class name may be checked, in which
 * case all must be present.
 *
 * @param {String} [...] the CSS class names to check
 *
 * @return {Boolean} true if all CSS classes were present, or
 *         false otherwise
 */
RapidContext.Widget.prototype.hasClass = function (/* ... */) {
    for (var i = 0; i < arguments.length; i++) {
        if (!MochiKit.DOM.hasElementClass(this, arguments[i])) {
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
    for (var i = 0; i < arguments.length; i++) {
        MochiKit.DOM.addElementClass(this, arguments[i]);
    }
};

/**
 * Removes the specified CSS class names from this HTML DOM node.
 *
 * @param {String} [...] the CSS class names to remove
 */
RapidContext.Widget.prototype.removeClass = function (/* ... */) {
    for (var i = 0; i < arguments.length; i++) {
        MochiKit.DOM.removeElementClass(this, arguments[i]);
    }
};

/**
 * Toggles adding and removing the specified CSS class names to and
 * from this HTML DOM node. If all the CSS classes are already set,
 * they will be removed. Otherwise they will be added.
 *
 * @param {String} [...] the CSS class names to remove
 *
 * @return {Boolean} true if the CSS classes were added, or
 *         false otherwise
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
 * Checks if this HTML DOM node is hidden (with the hide() method).
 * This method does NOT check the actual widget visibility (which
 * will be affected by animations for example), but only checks for
 * the "widgetHidden" CSS class.
 *
 * @return {Boolean} true if the widget is hidden, or
 *         false otherwise
 */
RapidContext.Widget.prototype.isHidden = function () {
    return this.hasClass("widgetHidden");
};

/**
 * Shows this HTML DOM node if it was previously hidden with the
 * hide() method. This mechanism is safe for all types of HTML
 * elements, since it uses a "widgetHidden" CSS class to hide nodes
 * instead of explicitly setting the CSS display property.
 */
RapidContext.Widget.prototype.show = function () {
    this.removeClass("widgetHidden");
};

/**
 * Hides this HTML DOM node if it doesn't have an explicit "display"
 * CSS value. This mechanism is safe for all types of HTML elements,
 * since it uses a "widgetHidden" CSS class to hide nodes instead of
 * explicitly setting the CSS display property.
 */
RapidContext.Widget.prototype.hide = function () {
    this.addClass("widgetHidden");
};

/**
 * Performs a visual effect animation on this widget. This is
 * implemented using the MochiKit.Visual effect package. All options
 * sent to this function will be passed on to the appropriate
 * MochiKit.Visual function.
 *
 * @param {Object} opts the visual effect options
 * @param {String} opts.effect the MochiKit.Visual effect name
 * @param {String} opts.queue the MochiKit.Visual queue handling,
 *            defaults to "replace" and a unique scope for each widget
 *            (see MochiKit.Visual for full options)
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
 * Blurs (unfocuses) this DOM node and all relevant child nodes.
 * This function will recursively blur all A, BUTTON, INPUT,
 * TEXTAREA and SELECT child nodes found.
 */
RapidContext.Widget.prototype.blurAll = function () {
    RapidContext.Util.blurAll(this);
};

/**
 * Returns an array with all child DOM nodes. Note that the array is
 * a real JavaScript array, not a dynamic NodeList. This method is
 * sometimes overridden by child widgets in order to hide
 * intermediate DOM nodes required by the widget.
 *
 * @return {Array} the array of child DOM nodes
 */
RapidContext.Widget.prototype.getChildNodes = function () {
    return MochiKit.Base.extend([], this.childNodes);
};

/**
 * Adds a single child DOM node to this widget. This method is
 * sometimes overridden by child widgets in order to hide or control
 * intermediate DOM nodes required by the widget.
 *
 * @param {Widget/Node} child the DOM node to add
 */
RapidContext.Widget.prototype.addChildNode = function (child) {
    this.appendChild(child);
};

/**
 * Removes a single child DOM node from this widget. This method is
 * sometimes overridden by child widgets in order to hide or control
 * intermediate DOM nodes required by the widget.<p>
 *
 * Note that this method will NOT destroy the removed child widget,
 * so care must be taken to ensure proper child widget destruction.
 *
 * @param {Widget/Node} child the DOM node to remove
 */
RapidContext.Widget.prototype.removeChildNode = function (child) {
    this.removeChild(child);
};

/**
 * Adds one or more children to this widget. This method will
 * flatten any arrays among the arguments and ignores any null or
 * undefined argument. Any DOM nodes or widgets will be added to the
 * end, and other objects will be converted to a text node first.
 * Subclasses should normally override the addChildNode() method instead
 * of this one, since that is the basis for DOM node insertion.
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
 * Removes all children to this widget. This method will also destroy
 * and child widgets and disconnect all signal listeners. This method
 * uses the getChildNodes() and removeChildNode() methods to find and
 * remove the individual child nodes.
 */
RapidContext.Widget.prototype.removeAll = function () {
    var children = this.getChildNodes();
    for (var i = children.length - 1; i >= 0; i--) {
        this.removeChildNode(children[i]);
        RapidContext.Widget.destroyWidget(children[i]);
    }
};
