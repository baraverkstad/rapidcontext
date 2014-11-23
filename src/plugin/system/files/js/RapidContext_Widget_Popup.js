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
RapidContext.Widget = RapidContext.Widget || { Classes: {}};

/**
 * Creates a new popup widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {Number} [attrs.delay] the widget auto-hide delay in
 *            milliseconds, defaults to `5000`
 * @param {Object} [attrs.showAnim] the optional animation options
              when showing the popup, defaults to none
 * @param {Object} [attrs.hideAnim] the optional animation options
 *            when hiding the popup, defaults to none
 * @param {Boolean} [attrs.hidden] the hidden widget flag, defaults to `true`
 * @param {Widget} [...] the child widgets or DOM nodes
 *
 * @return {Widget} the widget DOM node
 *
 * @class The popup widget class. Used to provide a popup menu or information
 *     area, using a `<div>` HTML element. The `Popup` widget will automatically
 *     disappear after a configurable amount of time, unless the user performs
 *     keyboard or mouse actions related to the popup.
 * @extends RapidContext.Widget
 *
 * @example {JavaScript}
 * var attrs1 = { value: 1, "class": "widgetPopupItem" };
 * var attrs2 = { value: 2, "class": "widgetPopupItem" };
 * var attrs3 = { value: 3, "class": "widgetPopupItem widgetPopupDisabled" };
 * var item1 = MochiKit.DOM.DIV(attrs1, "First Item");
 * var item2 = MochiKit.DOM.DIV(attrs2, "Second Item");
 * var item3 = MochiKit.DOM.DIV(attrs3, "Third Item");
 * var hr = MochiKit.DOM.HR();
 * var examplePopup = RapidContext.Widget.Popup({}, item1, item2, hr, item3);
 *
 * @example {User Interface XML}
 * <Popup id="examplePopup">
 *   <div value="1" class="widgetPopupItem">&#187; First Item</div>
 *   <div value="2" class="widgetPopupItem">&#187; Second Item</div>
 *   <hr />
 *   <div value="3" class="widgetPopupItem widgetPopupDisabled">&#187; Third Item</div>
 * </Popup>
 */
RapidContext.Widget.Popup = function (attrs/*, ...*/) {
    var o = MochiKit.DOM.DIV();
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetPopup");
    o._setHidden(true);
    o.selectedIndex = -1;
    o._delayTimer = null;
    o.setAttrs(MochiKit.Base.update({ delay: 5000 }, attrs));
    o.addAll(MochiKit.Base.extend(null, arguments, 1));
    MochiKit.Signal.connect(o, "onmousemove", o, "_handleMouseMove");
    MochiKit.Signal.connect(o, "onclick", o, "_handleMouseClick");
    return o;
};

//Register widget class
RapidContext.Widget.Classes.Popup = RapidContext.Widget.Popup;

/**
 * Emitted when the popup is shown. This event signal carries no
 * event information.
 *
 * @name RapidContext.Widget.Popup#onshow
 * @event
 */

/**
 * Emitted when the popup is hidden. This event signal carries no
 * event information.
 *
 * @name RapidContext.Widget.Popup#onhide
 * @event
 */

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {Number} [attrs.delay] the widget auto-hide delay in
 *            milliseconds, defaults to 5000
 * @param {Object} [attrs.showAnim] the optional animation options
              when showing the popup, defaults to none
 * @param {Object} [attrs.hideAnim] the optional animation options
 *            when hiding the popup, defaults to none
 * @param {Boolean} [attrs.hidden] the hidden widget flag
 */
RapidContext.Widget.Popup.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
    var locals = RapidContext.Util.mask(attrs, ["delay", "showAnim", "hideAnim", "hidden"]);
    if (typeof(locals.delay) != "undefined") {
        this.delay = parseInt(locals.delay);
        this.resetDelay();
    }
    if (typeof(locals.showAnim) != "undefined") {
        this.showAnim = locals.showAnim;
    }
    if (typeof(locals.hideAnim) != "undefined") {
        this.hideAnim = locals.hideAnim;
    }
    if (typeof(locals.hidden) != "undefined") {
        this._setHiddenPopup(locals.hidden);
    }
    this.__setAttrs(attrs);
};

/**
 * Performs the changes corresponding to setting the `hidden`
 * widget attribute for the `Popup` widget.
 *
 * @param {Boolean} value the new attribute value
 */
RapidContext.Widget.Popup.prototype._setHiddenPopup = function (value) {
    if (value && !this.isHidden()) {
        this._setHidden(true);
        if (this.hideAnim) {
            this.animate(this.hideAnim);
        }
        RapidContext.Widget.emitSignal(this, "onhide");
    } else if (!value && this.isHidden()) {
        this.selectChild(-1);
        this._setHidden(false);
        if (this.showAnim) {
            this.animate(this.showAnim);
        }
        this.scrollTop = 0;
        RapidContext.Widget.emitSignal(this, "onshow");
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
        this._delayTimer = setTimeout(MochiKit.Base.bind("hide", this), this.delay);
    }
};

/**
 * Returns the currently selected child node.
 *
 * @return {Node} the currently selected child node, or
 *         null if no node is selected
 */
RapidContext.Widget.Popup.prototype.selectedChild = function () {
    return RapidContext.Util.childNode(this, this.selectedIndex);
};

/**
 * Marks a popup child as selected. The currently selected child will
 * automatically be unselected by this method.
 *
 * @param {Number/Node} indexOrNode the child node index or DOM node,
 *            use a negative value to unselect
 *
 * @return the index of the newly selected child, or
 *         -1 if none was selected
 */
RapidContext.Widget.Popup.prototype.selectChild = function (indexOrNode) {
    var node = this.selectedChild();
    if (node != null) {
        MochiKit.DOM.removeElementClass(node, "widgetPopupSelected");
    }
    var node = RapidContext.Util.childNode(this, indexOrNode);
    if (typeof(indexOrNode) == "number") {
        var index = indexOrNode;
    } else {
        var index = MochiKit.Base.findIdentical(this.childNodes, node);
    }
    if (index >= 0 && node != null) {
        this.selectedIndex = index;
        MochiKit.DOM.addElementClass(node, "widgetPopupSelected");
        var top = node.offsetTop;
        var bottom = top + node.offsetHeight + 5;
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
 * @param {Number} offset the selection offset (a positive or
 *            negative number)
 *
 * @return the index of the newly selected child, or
 *         -1 if none was selected
 */
RapidContext.Widget.Popup.prototype.selectMove = function (offset) {
    var index = this.selectedIndex + offset;
    if (index >= this.childNodes.length) {
        index = 0;
    }
    if (index < 0) {
        index = this.childNodes.length - 1;
    }
    return this.selectChild(index);
};

/**
 * Handles mouse move events over the popup.
 *
 * @param {Event} evt the MochiKit.Signal.Event object
 */
RapidContext.Widget.Popup.prototype._handleMouseMove = function (evt) {
    this.show();
    var node = RapidContext.Util.childNode(this, evt.target());
    if (node != null && MochiKit.DOM.hasElementClass(node, "widgetPopupItem")) {
        this.selectChild(node);
    } else {
        this.selectChild(-1);
    }
};

/**
 * Handles mouse click events on the popup.
 *
 * @param {Event} evt the `MochiKit.Signal.Event` object
 */
RapidContext.Widget.Popup.prototype._handleMouseClick = function (evt) {
    var node = RapidContext.Util.childNode(this, evt.target());
    if (node != null && MochiKit.DOM.hasElementClass(node, "widgetPopupItem")) {
        this.selectChild(node);
    } else {
        this.selectChild(-1);
    }
};
