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
 * Creates a new dialog widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {String} [attrs.title] the dialog title, defaults to "Dialog"
 * @param {Boolean} [attrs.modal] the modal dialog flag, defaults to false
 * @param {Boolean} [attrs.center] the center dialog flag, defaults to true
 * @param {Boolean} [attrs.closeable] the closeable dialog flag, defaults to true
 * @param {Boolean} [attrs.resizeable] the resize dialog flag, defaults to true
 * @param {Object} [...] the child widgets or DOM nodes
 *
 * @return {Widget} the widget DOM node
 *
 * @class The dialog widget class. Used to provide a resizeable and
 *     moveable window within the current page. Internally it uses a
 *     number of &lt;div&gt; HTML elements.
 * @extends RapidContext.Widget
 *
 * @example
 * var dialog = RapidContext.Widget.Dialog({ title: "My Dialog", modal: true },
 *                                         "...dialog content widgets here...");
 * parent.addAll(dialog);
 * dialog.show();
 */
RapidContext.Widget.Dialog = function (attrs/*, ... */) {
    var title = MochiKit.DOM.DIV({ "class": "widgetDialogTitle" }, "Dialog");
    var close = RapidContext.Widget.Icon({ ref: "DEFAULT", tooltip: "Close", "class": "widgetDialogClose" });
    var resize = RapidContext.Widget.Icon({ ref: "RESIZE", "class": "widgetDialogResize" });
    var content = MochiKit.DOM.DIV({ "class": "widgetDialogContent" });
    RapidContext.Util.registerSizeConstraints(content, "100% - 22", "100% - 43");
    var o = MochiKit.DOM.DIV({}, title, close, resize, content);
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetDialog");
    o._setHidden(true);
    o.setAttrs(MochiKit.Base.update({ modal: false, center: true }, attrs));
    o.addAll(MochiKit.Base.extend(null, arguments, 1));
    title.onmousedown = RapidContext.Widget._eventHandler("Dialog", "_handleMoveStart");
    close.onclick = RapidContext.Widget._eventHandler("Dialog", "hide");
    resize.onmousedown = RapidContext.Widget._eventHandler("Dialog", "_handleResizeStart");
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
 * Updates the dialog or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {String} [attrs.title] the dialog title
 * @param {Boolean} [attrs.modal] the modal dialog flag
 * @param {Boolean} [attrs.center] the center dialog flag
 * @param {Boolean} [attrs.closeable] the closeable dialog flag
 * @param {Boolean} [attrs.resizeable] the resize dialog flag
 */
RapidContext.Widget.Dialog.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
    var locals = RapidContext.Util.mask(attrs, ["title", "modal", "center", "resizeable", "closeable", "hidden", "class", "style"]);
    if (typeof(locals.title) != "undefined") {
        MochiKit.DOM.replaceChildNodes(this.firstChild, locals.title);
    }
    if (typeof(locals.modal) != "undefined") {
        this.modal = MochiKit.Base.bool(locals.modal);
    }
    if (typeof(locals.center) != "undefined") {
        this.center = MochiKit.Base.bool(locals.center);
    }
    if (typeof(locals.resizeable) != "undefined") {
        var resize = this.childNodes[2];
        resize.setAttrs({ hidden: !MochiKit.Base.bool(locals.resizeable) });
    }
    if (typeof(locals.closeable) != "undefined") {
        var close = this.childNodes[1];
        close.setAttrs({ hidden: !MochiKit.Base.bool(locals.closeable) });
    }
    if (typeof(locals.hidden) != "undefined") {
        this._setHiddenDialog(locals.hidden);
    }
    if (typeof(locals["class"]) != "undefined") {
        this.lastChild.className = "widgetDialogContent " + locals["class"];
    }
    if (typeof(locals.style) != "undefined") {
        MochiKit.Style.setStyle(this.lastChild, locals.style);
    }
    this.__setAttrs(attrs);
};

/**
 * Performs the changes corresponding to setting the "hidden"
 * widget attribute for the Dialog widget.
 *
 * @param {Boolean} value the new attribute value
 */
RapidContext.Widget.Dialog.prototype._setHiddenDialog = function (value) {
    if (value) {
        if (this._modalNode != null) {
            RapidContext.Widget.destroyWidget(this._modalNode);
            this._modalNode = null;
        }
        this.blurAll();
        this._setHidden(true);
        RapidContext.Widget.emitSignal(this, "onhide");
    } else {
        if (this.parentNode == null) {
            throw new Error("Cannot show Dialog widget without setting a parent DOM node");
        }
        if (this.modal) {
            var attrs = { loading: false, message: "", style: { "z-index": "99" } };
            this._modalNode = RapidContext.Widget.Overlay(attrs);
            this.parentNode.appendChild(this._modalNode);
        }
        this._setHidden(false);
        this.moveTo(0, 0);
        var dim = MochiKit.Style.getElementDimensions(this);
        this.resizeTo(dim.w, dim.h);
        if (this.center) {
            this.moveToCenter();
        }
        RapidContext.Util.resetScrollOffset(this, true);
        RapidContext.Widget.emitSignal(this, "onshow");
    }
};

/**
 * Returns an array with all child DOM nodes. Note that the array is
 * a real JavaScript array, not a dynamic NodeList.
 *
 * @return {Array} the array of child DOM nodes
 */
RapidContext.Widget.Dialog.prototype.getChildNodes = function () {
    return MochiKit.Base.extend([], this.lastChild.childNodes);
};

/**
 * Adds a single child DOM node to this widget.
 *
 * @param {Widget/Node} child the DOM node to add
 */
RapidContext.Widget.Dialog.prototype.addChildNode = function (child) {
    this.lastChild.appendChild(child);
};

/**
 * Removes a single child DOM node from this widget.
 *
 * @param {Widget/Node} child the DOM node to remove
 */
RapidContext.Widget.Dialog.prototype.removeChildNode = function (child) {
    this.lastChild.removeChild(child);
};

/**
 * Moves the dialog to the specified position (relative to the
 * parent DOM node). The position will be restrained by the parent
 * DOM node size.
 *
 * @param {Number} x the horizontal position (in pixels)
 * @param {Number} y the vertical position (in pixels)
 */
RapidContext.Widget.Dialog.prototype.moveTo = function (x, y) {
    var parentDim = MochiKit.Style.getElementDimensions(this.parentNode);
    var dim = MochiKit.Style.getElementDimensions(this);
    var pos = { x: Math.max(0, Math.min(x, parentDim.w - dim.w - 2)),
                y: Math.max(0, Math.min(y, parentDim.h - dim.h - 2)) };
    MochiKit.Style.setElementPosition(this, pos);
    RapidContext.Widget.emitSignal(this, "onmove", pos);
};

/**
 * Moves the dialog to the apparent center (relative to the parent DOM
 * node). The vertical position actually uses the golden ratio instead
 * of the geometric center for improved visual alignment.
 */
RapidContext.Widget.Dialog.prototype.moveToCenter = function () {
    var parentDim = MochiKit.Style.getElementDimensions(this.parentNode);
    var dim = MochiKit.Style.getElementDimensions(this);
    var pos = { x: Math.round(Math.max(0, (parentDim.w - dim.w) / 2)),
                y: Math.round(Math.max(0, (parentDim.h - dim.h) / 2.618)) };
    MochiKit.Style.setElementPosition(this, pos);
    RapidContext.Widget.emitSignal(this, "onmove", pos);
};

/**
 * Resizes the dialog to the specified size (in pixels). The size
 * will be restrained by the parent DOM node size.
 *
 * @param {Number} width the width (in pixels)
 * @param {Number} height the height (in pixels)
 *
 * @return {Dimensions} an object with "w" and "h" properties for the
 *         actual size used
 */
RapidContext.Widget.Dialog.prototype.resizeTo = function (width, height) {
    var parentDim = MochiKit.Style.getElementDimensions(this.parentNode);
    var pos = MochiKit.Style.getElementPosition(this, this.parentNode);
    var dim = { w: Math.max(150, Math.min(width, parentDim.w - pos.x - 2)),
                h: Math.max(100, Math.min(height, parentDim.h - pos.y - 2)) };
    MochiKit.Style.setElementDimensions(this, dim);
    RapidContext.Util.registerSizeConstraints(this, null, null);
    MochiKit.Base.update(this, dim);
    RapidContext.Util.resizeElements(this.lastChild);
    RapidContext.Widget.emitSignal(this, "onresize", dim);
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
RapidContext.Widget.Dialog.prototype.resizeToContent = function (mode) {
    var content = this.lastChild;
    MochiKit.Style.setStyle(content, { width: "auto", height: "auto", overflow: "hidden" });
    var x = Math.round(Math.max(content.scrollWidth, content.offsetWidth) + 4);
    var y = Math.round(Math.max(content.scrollHeight, content.offsetHeight) + 25);
    MochiKit.Style.setStyle(content, { overflow: "auto" });
    return this.resizeTo(x, y);
}

/**
 * Initiates a dialog move drag operation. This will install a mouse
 * event handler on the parent document.
 *
 * @param {Event} evt the MochiKit.Signal.Event object
 */
RapidContext.Widget.Dialog.prototype._handleMoveStart = function (evt) {
    var pos = MochiKit.Style.getElementPosition(this.parentNode);
    this._offsetPos = MochiKit.Style.getElementPosition(this, pos);
    this._startPos = evt.mouse().page;
    evt.stop();
    MochiKit.Signal.connect(document, "onmousemove", this, "_handleMove");
    MochiKit.Signal.connect(document, "onmouseup", this, "_stopDrag");
};

/**
 * Handles a dialog move drag operation.
 *
 * @param {Event} evt the MochiKit.Signal.Event object
 */
RapidContext.Widget.Dialog.prototype._handleMove = function (evt) {
    var pos = evt.mouse().page;
    this.moveTo(this._offsetPos.x + pos.x - this._startPos.x,
                this._offsetPos.y + pos.y - this._startPos.y);
};

/**
 * Initiates a dialog resize drag operation. This will install a
 * mouse event handler on the parent document.
 *
 * @param {Event} evt the MochiKit.Signal.Event object
 */
RapidContext.Widget.Dialog.prototype._handleResizeStart = function (evt) {
    this._offsetDim = MochiKit.Style.getElementDimensions(this);
    this._startPos = evt.mouse().page;
    evt.stop();
    // TODO: correct handling of drag event, since IE seems to get
    //       problems when mouse enters other HTML elements
    MochiKit.Signal.connect(document, "onmousemove", this, "_handleResize");
    MochiKit.Signal.connect(document, "onmousedown", function (evt) { evt.stop(); });
    MochiKit.Signal.connect(document, "onmouseup", this, "_stopDrag");
};

/**
 * Handles a dialog resize drag operation.
 *
 * @param {Event} evt the MochiKit.Signal.Event object
 */
RapidContext.Widget.Dialog.prototype._handleResize = function (evt) {
    var pos = evt.mouse().page;
    this.resizeTo(this._offsetDim.w + pos.x - this._startPos.x,
                  this._offsetDim.h + pos.y - this._startPos.y);
};

/**
 * Stops a dialog resize or move drag operation.
 *
 * @param {Event} evt the MochiKit.Signal.Event object
 */
RapidContext.Widget.Dialog.prototype._stopDrag = function (evt) {
    MochiKit.Signal.disconnectAll(document, "onmousemove");
    MochiKit.Signal.disconnectAll(document, "onmousedown");
    MochiKit.Signal.disconnectAll(document, "onmouseup");
};
