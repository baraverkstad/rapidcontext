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
 * Creates a new dialog widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {string} [attrs.title] the dialog title, defaults to "Dialog"
 * @param {boolean} [attrs.modal] the modal dialog flag, defaults to `false`
 * @param {boolean} [attrs.system] the system dialog flag, implies modal,
 *            defaults to `false`
 * @param {boolean} [attrs.center] the center dialog flag, defaults to `true`
 * @param {boolean} [attrs.closeable] the closeable dialog flag, defaults to
 *            `true`
 * @param {boolean} [attrs.resizeable] the resize dialog flag, defaults to
 *            `true`
 * @param {boolean} [attrs.hidden] the hidden widget flag, defaults to `true`
 * @param {...(Node|Widget|string)} [child] the child widgets or DOM nodes
 *
 * @return {Widget} the widget DOM node
 *
 * @class The dialog widget class. Used to provide a resizeable and
 *     moveable window within the current page. Internally it uses a
 *     number of `<div>` HTML elements.
 * @extends RapidContext.Widget
 *
 * @example <caption>JavaScript</caption>
 * var h1 = MochiKit.DOM.H1({}, "Hello, world!");
 * var attrs = { title: "Hello", modal: true };
 * var helloDialog = RapidContext.Widget.Dialog(attrs, h1);
 * RapidContext.Util.registerSizeConstraints(helloDialog, "200", "75");
 *
 * @example <caption>User Interface XML</caption>
 * <Dialog id="helloDialog" title="Hello" modal="true" w="200" h="75">
 *   <h1>Hello, world!</h1>
 * </Dialog>
 */
RapidContext.Widget.Dialog = function (attrs/*, ... */) {
    var DIV = MochiKit.DOM.DIV;
    var title = DIV({ "class": "widgetDialogTitle", "data-dialog": "move" }, "Dialog");
    var close = RapidContext.Widget.Icon({
        "class": "widgetDialogClose fa fa-times",
        "title": "Close",
        "data-dialog": "close"
    });
    var resize = DIV({ "class": "widgetDialogResize", "data-dialog": "resize" });
    var content = DIV({ "class": "widgetDialogContent" });
    var o = DIV({}, title, close, resize, content);
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.Dialog);
    o.classList.add("widgetDialog");
    o.resizeContent = o._resizeContent;
    o._setHidden(true);
    o.setAttrs(Object.assign({ modal: false, system: false, center: true }, attrs));
    o.addAll(Array.from(arguments).slice(1));
    o.addEventListener("click", o._handleClick);
    o.addEventListener("mousedown", o._handleMouseDown);
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Dialog = RapidContext.Widget.Dialog;

/**
 * Emitted when the dialog is shown.
 *
 * @name RapidContext.Widget.Dialog#onshow
 * @event
 */

/**
 * Emitted when the dialog is hidden.
 *
 * @name RapidContext.Widget.Dialog#onhide
 * @event
 */

/**
 * Emitted when the dialog is moved. The event will be sent
 * repeatedly when moving with a mouse drag operation.
 *
 * @name RapidContext.Widget.Dialog#onmove
 * @event
 */

/**
 * Emitted when the dialog is resized. The event will be sent
 * repeatedly when resizing with a mouse drag operation.
 *
 * @name RapidContext.Widget.Dialog#onresize
 * @event
 */

/**
 * Destroys this widget.
 */
RapidContext.Widget.Dialog.prototype.destroy = function () {
    // FIXME: Use AbortSignal instead to disconnect
    this.removeEventListener("click", this._handleClick);
    this.removeEventListener("mousedown", this._handleMouseDown);
};

/**
 * Returns the widget container DOM node.
 *
 * @return {Node} the container DOM node
 */
RapidContext.Widget.Dialog.prototype._containerNode = function () {
    return this.lastChild;
};

/**
 * Returns the widget style DOM node.
 *
 * @return {Node} the style DOM node
 */
RapidContext.Widget.Dialog.prototype._styleNode = function () {
    return this.lastChild;
};

/**
 * Handles click events in the dialog. Will close the dialog if an element
 * with `data-dialog="close"` attribute was clicked.
 *
 * @param {Event} evt the DOM Event object
 */
RapidContext.Widget.Dialog.prototype._handleClick = function (evt) {
    let el = evt.target.closest("[data-dialog]");
    if (el && el.dataset.dialog == "close") {
        this.hide();
    }
};

/**
 * Handles mouse down events in the dialog. Will start move or resize actions
 * if an element with a `data-dialog="move"` or `data-dialog="resize"`
 * attribute was clicked.
 *
 * @param {Event} evt the DOM Event object
 */
RapidContext.Widget.Dialog.prototype._handleMouseDown = function (evt) {
    let el = evt.target.closest("[data-dialog]");
    let action = el && el.dataset.dialog;
    if (action == "move" || action == "resize") {
        var isDim = action == "resize";
        var x = (isDim ? this.offsetWidth : this.offsetLeft) - evt.pageX;
        var y = (isDim ? this.offsetHeight : this.offsetTop) - evt.pageY;
        document._drag = { target: this, action: action, x: x, y: y };
        evt.preventDefault();
        document.addEventListener("mouseup", this._handleMouseUp);
        document.addEventListener("mousemove", this._handleMouseMove);
    }
};

/**
 * Stops a dialog resize or move drag operation and removes event listeners.
 * Note that this event handler is attached to the root `document`.
 *
 * @param {Event} evt the DOM Event object
 */
RapidContext.Widget.Dialog.prototype._handleMouseUp = function (evt) {
    var o = document._drag;
    if (o && o.target) {
        // FIXME: Use AbortSignal instead to disconnect
        document.removeEventListener("mouseup", o.target._handleMouseUp);
        document.removeEventListener("mousemove", o.target._handleMouseMove);
    }
    delete document._drag;
};

/**
 * Handles a dialog move drag operation.
 *
 * @param {Event} evt the DOM Event object
 */
RapidContext.Widget.Dialog.prototype._handleMouseMove = function (evt) {
    var o = document._drag;
    if (o && o.action == "move") {
        o.target.moveTo(o.x + evt.pageX, o.y + evt.pageY);
    } else if (o && o.action == "resize") {
        o.target.resizeTo(o.x + evt.pageX, o.y + evt.pageY);
    }
};

/**
 * Updates the dialog or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {string} [attrs.title] the dialog title
 * @param {boolean} [attrs.modal] the modal dialog flag
 * @param {boolean} [attrs.system] the system dialog flag, implies modal
 * @param {boolean} [attrs.center] the center dialog flag
 * @param {boolean} [attrs.closeable] the closeable dialog flag
 * @param {boolean} [attrs.resizeable] the resize dialog flag
 * @param {boolean} [attrs.hidden] the hidden widget flag
 */
RapidContext.Widget.Dialog.prototype.setAttrs = function (attrs) {
    attrs = Object.assign({}, attrs);
    var locals = RapidContext.Util.mask(attrs, [
        "title", "modal", "system", "center", "resizeable", "closeable", "hidden"
    ]);
    if (typeof(locals.title) != "undefined") {
        this.firstChild.innerText = locals.title;
    }
    if (typeof(locals.modal) != "undefined") {
        this.modal = MochiKit.Base.bool(locals.modal);
    }
    if (typeof(locals.system) != "undefined") {
        this.system = MochiKit.Base.bool(locals.system);
    }
    if (typeof(locals.center) != "undefined") {
        this.center = MochiKit.Base.bool(locals.center);
    }
    if (typeof(locals.resizeable) != "undefined") {
        let resize = this.childNodes[2];
        resize.classList.toggle("hidden", !MochiKit.Base.bool(locals.resizeable));
    }
    if (typeof(locals.closeable) != "undefined") {
        var close = this.childNodes[1];
        close.setAttrs({ hidden: !MochiKit.Base.bool(locals.closeable) });
    }
    if (typeof(locals.hidden) != "undefined") {
        this._setHiddenDialog(locals.hidden);
    }
    this.__setAttrs(attrs);
};

/**
 * Performs the changes corresponding to setting the `hidden`
 * widget attribute for the Dialog widget.
 *
 * @param {boolean} value the new attribute value
 */
RapidContext.Widget.Dialog.prototype._setHiddenDialog = function (value) {
    if (!!value === this.isHidden()) {
        // Avoid repetitive show/hide calls
    } else if (value) {
        if (this._modalNode != null) {
            RapidContext.Widget.destroyWidget(this._modalNode);
            this._modalNode = null;
        }
        this.blurAll();
        this._setHidden(true);
        this._dispatch("hide");
    } else {
        if (this.parentNode == null) {
            throw new Error("Cannot show Dialog widget without setting a parent DOM node");
        }
        if (this.modal || this.system) {
            var attrs = { loading: false, message: "", style: { "z-index": "99" } };
            if (this.system) {
                attrs.dark = true;
            }
            this._modalNode = RapidContext.Widget.Overlay(attrs);
            this.parentNode.append(this._modalNode);
        }
        this._setHidden(false);
        this.resetScroll();
        this._resizeContent();
        this._dispatch("show");
    }
};

/**
 * Moves the dialog to the specified position (relative to the
 * parent DOM node). The position will be restrained by the parent
 * DOM node size.
 *
 * @param {number} x the horizontal position (in pixels)
 * @param {number} y the vertical position (in pixels)
 */
RapidContext.Widget.Dialog.prototype.moveTo = function (x, y) {
    var max = {
        x: this.parentNode.offsetWidth - this.offsetWidth - 2,
        y: this.parentNode.offsetHeight - this.offsetHeight - 2
    };
    var pos = {
        x: Math.round(Math.max(0, Math.min(x, max.x))),
        y: Math.round(Math.max(0, Math.min(y, max.y)))
    };
    this.style.left = pos.x + "px";
    this.style.top = pos.y + "px";
    var el = this.lastChild;
    el.style.maxWidth = (this.parentNode.offsetWidth - pos.x - el.offsetLeft - 5) + "px";
    el.style.maxHeight = (this.parentNode.offsetHeight - pos.y - el.offsetTop - 5) + "px";
    this.center = false;
    this._dispatch("move", { detail: pos });
};

/**
 * Moves the dialog to the apparent center (relative to the parent DOM
 * node). The vertical position actually uses the golden ratio instead
 * of the geometric center for improved visual alignment.
 */
RapidContext.Widget.Dialog.prototype.moveToCenter = function () {
    this.style.left = "0px";
    this.style.top = "0px";
    this.lastChild.style.maxWidth = "";
    this.lastChild.style.maxHeight = "";
    var x = (this.parentNode.offsetWidth - this.offsetWidth) / 2;
    var y = (this.parentNode.offsetHeight - this.offsetHeight) / 2.618;
    this.moveTo(x, y);
    this.center = true;
};

/**
 * Resizes the dialog to the specified size (in pixels). The size
 * will be restrained by the parent DOM node size.
 *
 * @param {number} w the width (in pixels)
 * @param {number} h the height (in pixels)
 *
 * @return {Dimensions} an object with "w" and "h" properties for the
 *         actual size used
 */
RapidContext.Widget.Dialog.prototype.resizeTo = function (w, h) {
    var max = {
        w: this.parentNode.offsetWidth - this.offsetLeft - 2,
        h: this.parentNode.offsetHeight - this.offsetTop - 2
    };
    var dim = {
        w: Math.round(Math.max(150, Math.min(w, max.w))),
        h: Math.round(Math.max(100, Math.min(h, max.h)))
    };
    this.style.width = dim.w + "px";
    this.style.height = dim.h + "px";
    delete this.sizeConstraints; // FIXME: Remove with RapidContext.Util.registerSizeConstraints
    this.center = false;
    this._resizeContent();
    this._dispatch("resize", { detail: dim });
    return dim;
};

/**
 * Resizes the dialog to the optimal size for the current content.
 * Note that the size reported by the content may vary depending on
 * if it has already been displayed, is absolutely positioned, etc.
 * The size will be restrained by the parent DOM node size.
 *
 * @return {Dimensions} an object with "w" and "h" properties for the
 *         actual size used
 */
RapidContext.Widget.Dialog.prototype.resizeToContent = function () {
    var el = this.lastChild;
    var w = Math.max(el.scrollWidth, el.offsetWidth) + 2;
    var h = Math.max(el.scrollHeight, el.offsetHeight) + el.offsetTop + 2;
    return this.resizeTo(w, h);
};

/**
 * Called when dialog content should be resized.
 */
RapidContext.Widget.Dialog.prototype._resizeContent = function () {
    if (!this.isHidden()) {
        RapidContext.Util.resizeElements(this.lastChild);
        if (this.center) {
            this.moveToCenter();
        }
    }
};

/**
 * Resets the scroll offsets for all child elements in the dialog.
 */
RapidContext.Widget.Dialog.prototype.resetScroll = function () {
    function scrollReset(el) {
        el.scrollTop = 0;
        el.scrollLeft = 0;
    }
    Array.from(this.querySelectorAll("*")).forEach(scrollReset);
};
