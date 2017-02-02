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

// Namespace initialization
if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}
RapidContext.Widget = RapidContext.Widget || { Classes: {}};

/**
 * Creates a new navigation bar widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {Boolean} [attrs.hidden] the hidden widget flag, defaults to false
 *
 * @return {Widget} the widget DOM node
 *
 * @class The navigation bar widget class. Used to show step-based progress or
 *     location in a tree structure. In both cases, it allows the user to go
 *     back to a previous step. The widget uses a `<table>` HTML element.
 * @extends RapidContext.Widget
 * @property {Number} position The current position in the path (from 0)
 * @property {Number} maxPosition The maximum position reached when
 *     navigating.
 * @property {Array} path The path being navigated.
 *
 * @example {JavaScript}
 * var navBar = RapidContext.NavigationBar();
 * navBar.moveTo(0, ["Step 1", "Step 2", "Step 3"]);
 *
 * @example {User Interface XML}
 * <NavigationBar id="navBar" />
 * <!-- No support for menu items in XML yet -->
 */
RapidContext.Widget.NavigationBar = function (attrs) {
    var tr = MochiKit.DOM.TR();
    var tbody = MochiKit.DOM.TBODY({}, tr);
    var o = MochiKit.DOM.TABLE({}, tbody);
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetNavigationBar");
    o.setAttrs(attrs);
    o.moveTo(0, ["Start"]);
    o.onclick = RapidContext.Widget._eventHandler(null, "_handleClick");
    return o;
};

// Register widget class
RapidContext.Widget.Classes.NavigationBar = RapidContext.Widget.NavigationBar;

/**
 * Returns the widget container DOM node.
 *
 * @return {Node} returns null, since child nodes are not supported
 */
RapidContext.Widget.NavigationBar.prototype._containerNode = function () {
    return null;
};

/**
 * Emitted when the navigation position changes. This is triggered
 * either by the user moving forward or backward, or by a call to the
 * `moveTo()` method. This event signal carries a reference to the
 * widget itself.
 *
 * @name RapidContext.Widget.NavigationBar#onchange
 * @event
 */

/**
 * Resets the position and maximum position counters and moves to
 * the first step in the path.
 */
RapidContext.Widget.NavigationBar.prototype.reset = function () {
    this.position = 0;
    this.maxPosition = 0;
    this.moveTo(0);
};

/**
 * Moves the navigation bar to the specified position in the path.
 * Optionally a new path may also be provided.
 *
 * @param {Number} pos the new position to move to (from 0)
 * @param {Array} [path] the new path to navigate
 */
RapidContext.Widget.NavigationBar.prototype.moveTo = function (pos, path) {
    this.position = pos;
    this.maxPosition = Math.max(this.maxPosition, pos);
    if (path != null) {
        this.path = path.slice(0);
    }
    var tr = this.firstChild.firstChild;
    MochiKit.DOM.replaceChildNodes(tr);
    for (var i = 0; i < this.path.length; i++) {
        var className = this._posClass(i);
        tr.appendChild(MochiKit.DOM.TD({ "class": className }, this.path[i]));
        className = className + "-" + this._posClass(i + 1);
        tr.appendChild(MochiKit.DOM.TD({ "class": className }, "\xa0"));
    }
    RapidContext.Widget.emitSignal(this, "onchange", this);
};

/**
 * Returns the class to use for a specified path position.
 *
 * @param {Number} pos the position to render
 *
 * @return {String} the CSS class corresponding to the position
 */
RapidContext.Widget.NavigationBar.prototype._posClass = function (pos) {
    if (pos >= this.path.length) {
        return "end";
    } else if (pos == this.position) {
        return "active";
    } else if (pos < this.maxPosition) {
        return "prev";
    } else {
        return "prev";
    }
};

/**
 * Handles click events on the navigation bar.
 *
 * @param {Event} evt the MochiKit.Signal.Event object
 */
RapidContext.Widget.NavigationBar.prototype._handleClick = function (evt) {
    var node = evt.target();
    if (node.className === "prev") {
        var step = 0;
        while (node = node.previousSibling) {
            step++;
        }
        this.moveTo(step / 2);
    }
};
