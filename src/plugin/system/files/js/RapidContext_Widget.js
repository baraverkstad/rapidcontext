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

/**
 * The base class for the HTML user interface widgets. The Widget
 * class shouldn't be instantiated directly, instead one of the
 * subclasses should be instantiated.
 *
 * @class
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
 * @return {number} the next number in the sequence
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
 * @param {string} [className] the optional widget class name
 *
 * @return {boolean} `true` if the object looks like a widget, or
 *         `false` otherwise
 */
RapidContext.Widget.isWidget = function (obj, className) {
    return obj &&
           obj.nodeType > 0 &&
           obj.classList.contains("widget") &&
           (!className || obj.classList.contains("widget" + className));
};

/**
 * Splits a string of CSS class names into an array.
 *
 * @param {Array|string} val the CSS class names
 *
 * @return {Array} nested arrays with single CSS class names
 */
RapidContext.Widget._toCssClass = function (val) {
    if (Array.isArray(val)) {
        // FIXME: Use Array.prototype.flatMap(...) here
        return val.map(RapidContext.Widget._toCssClass);
    } else if (val) {
        return String(val).split(/\s+/g).filter(Boolean);
    } else {
        return [];
    }
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
 * @param {...(Object|function)} cls the widget class or constructor
 *
 * @return {Widget} the widget DOM node
 */
RapidContext.Widget._widgetMixin = function (node/*, objOrClass, ...*/) {
    node.classList.add("widget");
    var protos = Array.from(arguments).slice(1);
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
                console.warn(msg, e, node);
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
 * @param {string} name the widget class name
 * @param {Object} attrs the widget and node attributes
 * @param {...(Node|Widget)} [child] the child widgets or DOM nodes
 *
 * @return {Widget} the widget DOM node
 *
 * @throws {ReferenceError} if the widget class name couldn't be
 *             found in `RapidContext.Widget.Classes`
 */
RapidContext.Widget.createWidget = function (name, attrs/*, ...*/) {
    var cls = RapidContext.Widget.Classes[name];
    if (cls == null) {
        throw new ReferenceError("failed to find widget '" + name +
                                 "' in RapidContext.Widget.Classes");
    }
    var args = Array.from(arguments).slice(1);
    return cls.apply(this, MochiKit.Base.flattenArray(args));
};

/**
 * Destroys a widget or a DOM node. This function will remove the DOM
 * node from its parent, disconnect any signals and call destructor
 * functions. It is also applied recursively to to all child nodes.
 * Once destroyed, all references to the widget object should be
 * cleared to reclaim browser memory.
 *
 * @param {Widget|Node|NodeList|Array} node the DOM node or list
 */
RapidContext.Widget.destroyWidget = function (node) {
    if (node && node.nodeType === 1) {
        if (typeof(node.destroy) == "function") {
            node.destroy();
        }
        if (node.parentNode != null) {
            node.remove();
        }
        MochiKit.Signal.disconnectAll(node);
        MochiKit.Signal.disconnectAllTo(node);
        RapidContext.Widget.destroyWidget(node.childNodes);
    } else if (node && typeof(node.length) === "number") {
        Array.from(node).forEach(RapidContext.Widget.destroyWidget);
    }
};

/**
 * Returns the unique identifier for this DOM node. If a node id has
 * already been set, that id will be returned. Otherwise a new id
 * will be generated and assigned to the widget DOM node.
 *
 * @return {string} the the unique DOM node identifier
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
 * Dispatches a custom event from this DOM node. The event will be
 * created and emitted asynchronously (via setTimeout).
 *
 * @param {string} type the event type (e.g. `validate`)
 * @param {Object} [opts] the event options (e.g. `{ bubbles: true }`)
 */
RapidContext.Widget.prototype._dispatch = function (type, opts) {
    var self = this;
    function later() {
        self.dispatchEvent(new CustomEvent(type, opts));
    }
    setTimeout(later);
};

/**
 * Updates the widget or HTML DOM node attributes. This method is
 * sometimes overridden by individual widgets to allow modification
 * of additional widget attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {boolean} [attrs.disabled] the disabled widget flag
 * @param {boolean} [attrs.hidden] the hidden widget flag
 * @param {string} [attrs.class] the CSS class names
 */
RapidContext.Widget.prototype.setAttrs = function (attrs) {
    /* eslint max-depth: "off" */
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
                    var k = a[0].trim();
                    if (k != "" && a.length > 1) {
                        styles[k] = a[1].trim();
                    }
                }
                value = styles;
            }
            this.setStyle(value);
        } else if (value != null) {
            this.setAttribute(name, value);
            if (typeof(value) != "object") {
                this[name] = value;
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
    styles = Object.assign({}, styles);
    var posDimNames = [
        "width", "height", "zIndex", "z-index",
        "position", "top", "bottom", "left", "right"
    ];
    var posDimStyles = RapidContext.Util.mask(styles, posDimNames);
    MochiKit.Style.setStyle(this, posDimStyles);
    MochiKit.Style.setStyle(this._styleNode(), styles);
};

/**
 * Checks if this HTML DOM node has the specified CSS class names.
 * Note that more than one CSS class name may be checked, in which
 * case all must be present.
 *
 * @param {...(string|Array)} cls the CSS class names to check
 *
 * @return {boolean} `true` if all CSS classes were present, or
 *         `false` otherwise
 */
RapidContext.Widget.prototype.hasClass = function (/* ... */) {
    function isMatch(val) {
        if (Array.isArray(val)) {
            return val.every(isMatch);
        } else {
            return elem.classList.contains(val);
        }
    }
    var elem = this._styleNode();
    // FIXME: Use Array.prototype.flatMap(...) here
    return Array.from(arguments).map(RapidContext.Widget._toCssClass).every(isMatch);
};

/**
 * Adds the specified CSS class names to this HTML DOM node.
 *
 * @param {...(string|Array)} cls the CSS class names to add
 */
RapidContext.Widget.prototype.addClass = function (/* ... */) {
    function add(val) {
        if (Array.isArray(val)) {
            val.forEach(add);
        } else {
            elem.classList.add(val);
        }
    }
    var elem = this._styleNode();
    // FIXME: Use Array.prototype.flatMap(...) here
    Array.from(arguments).map(RapidContext.Widget._toCssClass).forEach(add);
};

/**
 * Removes the specified CSS class names from this HTML DOM node.
 * Note that this method will not remove any class starting with
 * "widget".
 *
 * @param {...(string|Array)} cls the CSS class names to remove
 */
RapidContext.Widget.prototype.removeClass = function (/* ... */) {
    function remove(val) {
        if (Array.isArray(val)) {
            val.filter(Boolean).forEach(remove);
        } else if (!val.startsWith("widget")) {
            elem.classList.remove(val);
        }
    }
    var elem = this._styleNode();
    // FIXME: Use Array.prototype.flatMap(...) here
    Array.from(arguments).map(RapidContext.Widget._toCssClass).forEach(remove);
};

/**
 * Toggles adding and removing the specified CSS class names to and
 * from this HTML DOM node. If all the CSS classes are already set,
 * they will be removed. Otherwise they will be added.
 *
 * @param {...(string|Array)} cls the CSS class names to remove
 *
 * @return {boolean} `true` if the CSS classes were added, or
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
 * @return {boolean} `true` if the widget is disabled, or
 *         `false` otherwise
 */
RapidContext.Widget.prototype.isDisabled = function () {
    return this.disabled === true && this.classList.contains("widgetDisabled");
};

/**
 * Performs the changes corresponding to setting the `disabled`
 * widget attribute.
 *
 * @param {boolean} value the new attribute value
 */
RapidContext.Widget.prototype._setDisabled = function (value) {
    value = MochiKit.Base.bool(value);
    this.classList.toggle("widgetDisabled", value);
    this.setAttribute("disabled", value);
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
 * @return {boolean} `true` if the widget is hidden, or
 *         `false` otherwise
 */
RapidContext.Widget.prototype.isHidden = function () {
    return this.classList.contains("widgetHidden");
};

/**
 * Performs the changes corresponding to setting the `hidden`
 * widget attribute.
 *
 * @param {boolean} value the new attribute value
 */
RapidContext.Widget.prototype._setHidden = function (value) {
    value = MochiKit.Base.bool(value);
    this.classList.toggle("widgetHidden", value);
    this.setAttribute("hidden", value);
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
 * @param {string} opts.effect the MochiKit.Visual effect name
 * @param {string} opts.queue the MochiKit.Visual queue handling,
 *            defaults to "replace" and a unique scope for each widget
 *            (see `MochiKit.Visual` for full options)
 *
 * @example
 * widget.animate({ effect: "fade", duration: 0.5 });
 * widget.animate({ effect: "Move", transition: "spring", y: 300 });
 *
 * @deprecated Use CSS animations instead.
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
    return elem ? Array.from(elem.childNodes) : [];
};

/**
 * Adds a single child DOM node to this widget. This method is
 * sometimes overridden by child widgets in order to hide or control
 * intermediate DOM nodes required by the widget.
 *
 * @param {Widget|Node} child the DOM node to add
 */
RapidContext.Widget.prototype.addChildNode = function (child) {
    var elem = this._containerNode();
    if (elem) {
        elem.append(child);
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
 * @param {Widget|Node} child the DOM node to remove
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
 * @param {...(string|Node|Array)} child the children to add
 */
RapidContext.Widget.prototype.addAll = function (...children) {
    [].concat(...children).filter((o) => o != null).forEach((child) => {
        this.addChildNode(child);
    });
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
