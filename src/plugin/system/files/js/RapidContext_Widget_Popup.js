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
RapidContext.Widget = RapidContext.Widget || { Classes: {} };

/**
 * Creates a new popup widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {number} [attrs.delay] the widget auto-hide delay in
 *            milliseconds, defaults to `5000`
 * @param {boolean} [attrs.hidden] the hidden widget flag, defaults to `true`
 * @param {...(Node|Array)} [child] the child widgets or DOM nodes
 *
 * @return {Widget} the widget DOM node
 *
 * @class The popup widget class. Used to provide a popup menu or information
 *     area, using a `<div>` HTML element. The `Popup` widget will automatically
 *     disappear after a configurable amount of time, unless the user performs
 *     keyboard or mouse actions related to the popup.
 * @extends RapidContext.Widget
 *
 * @example <caption>JavaScript</caption>
 * let hr = MochiKit.DOM.HR();
 * let third = MochiKit.DOM.LI({ "class": "disabled" }, "Third");
 * let popup = RapidContext.Widget.Popup({}, "First",  "Second", hr, third);
 *
 * @example <caption>User Interface XML</caption>
 * <Popup id="examplePopup">
 *   <li>&#187; First Item</div>
 *   <li>&#187; Second Item</div>
 *   <hr />
 *   <li class="disabled">&#187; Third Item</div>
 * </Popup>
 */
RapidContext.Widget.Popup = function (attrs/*, ...*/) {
    let o = document.createElement("menu");
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.Popup);
    o.addClass("widgetPopup");
    o._setHidden(true);
    o.tabIndex = -1;
    o.selectedIndex = -1;
    o._delayTimer = null;
    o.setAttrs(Object.assign({ delay: 5000 }, attrs));
    o.addAll(Array.from(arguments).slice(1));
    o.on("click mousemove", ".widgetPopup > *", o._handleMouseEvent);
    o.on("keydown", o._handleKeyDown);
    return o;
};

//Register widget class
RapidContext.Widget.Classes.Popup = RapidContext.Widget.Popup;

/**
 * Emitted when the popup is shown.
 *
 * @name RapidContext.Widget.Popup#onshow
 * @event
 */

/**
 * Emitted when the popup is hidden.
 *
 * @name RapidContext.Widget.Popup#onhide
 * @event
 */

/**
 * Emitted when a menu item is selected.
 *
 * @name RapidContext.Widget.Popup#menuselect
 * @event
 */

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {number} [attrs.delay] the widget auto-hide delay in
 *            milliseconds, defaults to 5000
 * @param {boolean} [attrs.hidden] the hidden widget flag
 */
RapidContext.Widget.Popup.prototype.setAttrs = function (attrs) {
    attrs = Object.assign({}, attrs);
    if ("delay" in attrs) {
        attrs.delay = parseInt(attrs.delay, 10) || 5000;
        this.resetDelay();
    }
    if ("showAnim" in attrs) {
        console.warn("deprecated: popup 'showAnim' attribute is ignored");
        delete attrs.showAnim;
    }
    if ("hideAnim" in attrs) {
        console.warn("deprecated: popup 'hideAnim' attribute is ignored");
        delete attrs.hideAnim;
    }
    if ("hidden" in attrs) {
        this._setHiddenPopup(RapidContext.Data.bool(attrs.hidden));
        delete attrs.hidden;
    }
    this.__setAttrs(attrs);
};

/**
 * Adds a single child node to this widget.
 *
 * @param {string|Node|Widget} child the item to add
 */
RapidContext.Widget.Popup.prototype.addChildNode = function (child) {
    if (!child.nodeType) {
        child = MochiKit.DOM.LI(null, child);
    }
    this._containerNode(true).append(child);
};

/**
 * Performs the changes corresponding to setting the `hidden`
 * widget attribute for the `Popup` widget.
 *
 * @param {boolean} value the new attribute value
 */
RapidContext.Widget.Popup.prototype._setHiddenPopup = function (value) {
    if (value && !this.isHidden()) {
        this._setHidden(true);
        this.style.maxHeight = 0;
        this.emit("hide");
        setTimeout(() => this.blur(), 100);
    } else if (!value && this.isHidden()) {
        this.selectChild(-1);
        this._setHidden(false);
        this.style.maxHeight = (this.scrollHeight + 10) + "px";
        this.scrollTop = 0;
        this.emit("show");
        setTimeout(() => this.focus(), 100);
    }
    this.resetDelay();
};

/**
 * Resets the popup auto-hide timer. Might be called manually when
 * receiving events on other widgets related to this one.
 */
RapidContext.Widget.Popup.prototype.resetDelay = function () {
    if (this._delayTimer) {
        clearTimeout(this._delayTimer);
        this._delayTimer = null;
    }
    if (!this.isHidden() && this.delay > 0) {
        this._delayTimer = setTimeout(() => this.hide(), this.delay);
    }
};

/**
 * Returns the currently selected child node.
 *
 * @return {Node} the currently selected child node, or
 *         null if no node is selected
 */
RapidContext.Widget.Popup.prototype.selectedChild = function () {
    return this.childNodes[this.selectedIndex] || null;
};

/**
 * Marks a popup child as selected. The currently selected child will
 * automatically be unselected by this method.
 *
 * @param {number|Node} indexOrNode the child node index or DOM node,
 *            use a negative value to unselect
 *
 * @return the index of the newly selected child, or
 *         -1 if none was selected
 */
RapidContext.Widget.Popup.prototype.selectChild = function (indexOrNode) {
    let index;
    let node = this.selectedChild();
    if (node != null) {
        node.classList.remove("selected");
    }
    let isNumber = typeof(indexOrNode) == "number";
    index = isNumber ? indexOrNode : Array.from(this.childNodes).indexOf(indexOrNode);
    node = this.childNodes[index];
    let selector = "li:not(.disabled), .widgetPopupItem:not(.disabled)";
    if (index >= 0 && node && node.matches(selector)) {
        this.selectedIndex = index;
        node.classList.add("selected");
        let top = node.offsetTop;
        let bottom = top + node.offsetHeight + 5;
        if (this.scrollTop + this.clientHeight < bottom) {
            this.scrollTop = bottom - this.clientHeight;
        }
        if (this.scrollTop > top) {
            this.scrollTop = top;
        }
    } else {
        this.selectedIndex = -1;
    }
    return this.selectedIndex;
};

/**
 * Moves the current selection by a numeric offset.
 *
 * @param {number} offset the selection offset (a positive or
 *            negative number)
 *
 * @return the index of the newly selected child, or
 *         -1 if none was selected
 */
RapidContext.Widget.Popup.prototype.selectMove = function (offset) {
    let active = this.selectedChild();
    let items = this.querySelectorAll("li:not(.disabled), .widgetPopupItem:not(.disabled)");
    let index = (offset < 0) ? offset : Math.max(0, offset - 1);
    if (active) {
        index = Array.from(items).indexOf(active) + offset;
    }
    index += (index < 0) ? items.length : 0;
    index -= (index >= items.length) ? items.length - 1 : 0;
    return this.selectChild(items[index]);
};

/**
 * Handles mouse events on the popup.
 *
 * @param {Event} evt the DOM Event object
 */
RapidContext.Widget.Popup.prototype._handleMouseEvent = function (evt) {
    this.show();
    let node = evt.delegateTarget;
    if (this.selectChild(node) >= 0 && evt.type == "click") {
        let detail = { menu: this, item: node };
        this.emit("menuselect", { detail });
    }
};

/**
 * Handles the key down event on the popup.
 *
 * @param {Event} evt the DOM Event object
 */
RapidContext.Widget.Popup.prototype._handleKeyDown = function (evt) {
    this.show();
    switch (evt.key) {
    case "ArrowUp":
    case "ArrowDown":
        evt.preventDefault();
        evt.stopImmediatePropagation();
        this.selectMove(evt.key == "ArrowUp" ? -1 : 1);
        break;
    case "Escape":
        evt.preventDefault();
        evt.stopImmediatePropagation();
        this.hide();
        break;
    case "Tab":
    case "Enter":
        evt.preventDefault();
        evt.stopImmediatePropagation();
        if (this.selectedChild()) {
            let detail = { menu: this, item: this.selectedChild() };
            this.emit("menuselect", { detail });
        }
        break;
    }
};
