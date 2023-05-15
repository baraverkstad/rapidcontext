/**
 * Creates a new dialog widget. The dialog widget is a container
 * for other widgets, floating above the parent element. If no
 * parent element is specified, the document root element is used.
 *
 * @constructor
 * @param {Object} props         the optional widget properties
 * @config {String} title        the dialog title
 * @config {Boolean} modal       the modal dialog flag
 * @config {Object} style        the initial style
 */
function Dialog(props) {
    props = props || {};
    this.title = props.title || "Dialog";
    this.modal = props.modal || false;
    this.style = props.style || {};
    /** (read-only) The HTML DOM node. */
    this.domNode = document.createElement("div");
    this._modalNode = null;
    this._closeImage = Icon.CLOSE.createElement();
    this._resizeImage = Icon.RESIZE.createElement();
    this._titleNode = document.createElement("div");
    this._pane = new Pane();
    this._offsetPos = { x: 0, y: 0 };
    this._parentPos = null;
    CssUtil.setStyle(this.domNode, "class", "uiDialog");
    CssUtil.setStyle(this.domNode, "display", "none");
    CssUtil.setStyle(this.domNode, "w", "60%");
    CssUtil.setStyle(this.domNode, "h", "70%");
    CssUtil.setStyle(this._closeImage, "class", "uiDialogClose");
    MochiKit.Signal.connect(this._titleNode, "onmousedown", this, "_handleMoveStart");
    MochiKit.Signal.connect(this._closeImage, "onclick", this, "hide");
    this.domNode.appendChild(this._closeImage);
    CssUtil.setStyle(this._resizeImage, "class", "uiDialogResize");
    MochiKit.Signal.connect(this._resizeImage, "onmousedown", this, "_handleResizeStart");
    this.domNode.appendChild(this._resizeImage);
    CssUtil.setStyle(this._titleNode, "class", "uiDialogTitle");
    this._titleNode.appendChild(document.createTextNode(this.title));
    this.domNode.appendChild(this._titleNode);
    this._pane.setStyle({ "class": "uiDialogContent", w: "100% - 22", h: "100% - 44" });
    this.domNode.appendChild(this._pane.domNode);
    this.setStyle(this.style);
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
Dialog.prototype.setStyle = function(style) {
    CssUtil.setStyles(this.domNode, style);
}

/**
 * Shows the dialog. The dialog will be centered on the window.
 */
Dialog.prototype.show = function() {
    if (this.domNode.parentNode == null) {
        document.body.appendChild(this.domNode);
    }
    if (this.modal) {
        this._modalNode = document.createElement("div");
        CssUtil.setStyle(this._modalNode, "class", "uiDialogModal");
        this.domNode.parentNode.appendChild(this._modalNode);
    }
    CssUtil.setStyle(this.domNode, "display", "block");
    CssUtil.resize(this.domNode);
    ReTracer.Util.resetScrollOffset(this.domNode, true);
    var parentDim = MochiKit.Style.getElementDimensions(this.domNode.parentNode);
    var dim = MochiKit.Style.getElementDimensions(this.domNode);
    var pos = { x: Math.round(Math.max(0, (parentDim.w - dim.w) / 2)),
                y: Math.round(Math.max(0, (parentDim.h - dim.h) / 2)) };
    MochiKit.Style.setElementPosition(this.domNode, pos);
    this.onShow();
}

/**
 * Hides the dialog.
 */
Dialog.prototype.hide = function() {
    if (this._modalNode != null) {
        MochiKit.DOM.removeElement(this._modalNode);
        this._modalNode = null;
    }
    ReTracer.Util.blurAll(this.domNode);
    CssUtil.setStyle(this.domNode, "display", "none");
    this.onHide();
}

/**
 * Adds a child element to this dialog.
 *
 * @param child              the child element to add
 */
Dialog.prototype.addChild = function(child) {
    this._pane.addChild(child);
}

/**
 * Removes all child elements from this dialog.
 */
Dialog.prototype.removeAllChildren = function() {
    this._pane.removeAllChildren();
}

/**
 * The dialog show signal.
 *
 * @signal Emitted when the dialog has been shown
 */
Dialog.prototype.onShow = function() {
}

/**
 * The dialog hide signal.
 *
 * @signal Emitted when the dialog has been hidden
 */
Dialog.prototype.onHide = function() {
}

/**
 * Moves the dialog to the specified position (relative to the
 * parent DOM node). The position will be restrained by the parent
 * DOM node size.
 *
 * @param {Number} x the horizontal position (in pixels)
 * @param {Number} y the vertical position (in pixels)
 */
Dialog.prototype.moveTo = function(x, y) {
    var parentDim = MochiKit.Style.getElementDimensions(this.domNode.parentNode);
    var dim = MochiKit.Style.getElementDimensions(this.domNode);
    var pos = { x: Math.max(0, Math.min(x, parentDim.w - dim.w - 2)),
                y: Math.max(0, Math.min(y, parentDim.h - dim.h - 2)) };
    MochiKit.Style.setElementPosition(this.domNode, pos);
}

/**
 * Resizes the dialog to the specified size (in pixels). The size
 * will be restrained by the parent DOM node size.
 *
 * @param {Number} width the width (in pixels)
 * @param {Number} height the height (in pixels)
 */
Dialog.prototype.resizeTo = function(width, height) {
    var parentDim = MochiKit.Style.getElementDimensions(this.domNode.parentNode);
    var pos = MochiKit.Style.getElementPosition(this.domNode.parentNode);
    pos = MochiKit.Style.getElementPosition(this.domNode, pos);
    var dim = { w: Math.max(150, Math.min(width, parentDim.w - pos.x - 2)),
                h: Math.max(100, Math.min(height, parentDim.h - pos.y - 2)) };
    MochiKit.Style.setElementDimensions(this.domNode, dim);
    ReTracer.Util.registerSizeConstraints(this.domNode, null, null);
    MochiKit.Base.update(this.domNode, dim);
    ReTracer.Util.resizeElements(this._pane.domNode);
}

/**
 * Handles the start of a move operation.
 *
 * @param {Object} evt the MochiKit.Signal.Event object
 *
 * @private
 */
Dialog.prototype._handleMoveStart = function(evt) {
    var pos = MochiKit.Style.getElementPosition(this.domNode.parentNode);
    this._offsetPos = MochiKit.Style.getElementPosition(this.domNode, pos);
    this._startPos = evt.mouse().page;
    evt.stop();
    MochiKit.Signal.connect(document, "onmousemove", this, "_handleMove");
    MochiKit.Signal.connect(document, "onmouseup", this, "_stopDrag");
}

/**
 * Handles the move operation.
 *
 * @param {Object} evt the MochiKit.Signal.Event object
 *
 * @private
 */
Dialog.prototype._handleMove = function(evt) {
    var pos = evt.mouse().page;
    this.moveTo(this._offsetPos.x + pos.x - this._startPos.x,
                this._offsetPos.y + pos.y - this._startPos.y);
}

/**
 * Handles the start of a resize operation.
 *
 * @param {Object} evt the MochiKit.Signal.Event object
 *
 * @private
 */
Dialog.prototype._handleResizeStart = function(evt) {
    this._offsetDim = MochiKit.Style.getElementDimensions(this.domNode);
    this._startPos = evt.mouse().page;
    evt.stop();
    // TODO: correct handling of drag event, since IE seems to get
    //       problems when mouse enters other HTML elements
    MochiKit.Signal.connect(document, "onmousemove", this, "_handleResize");
    MochiKit.Signal.connect(document, "onmousedown", function(evt) { evt.stop(); });
    MochiKit.Signal.connect(document, "onmouseup", this, "_stopDrag");
}

/**
 * Handles the resize operation.
 *
 * @param {Object} evt the MochiKit.Signal.Event object
 *
 * @private
 */
Dialog.prototype._handleResize = function(evt) {
    var pos = evt.mouse().page;
    this.resizeTo(this._offsetDim.w + pos.x - this._startPos.x,
                  this._offsetDim.h + pos.y - this._startPos.y);
}

/**
 * Handles the stop of a drag operation.
 *
 * @private
 */
Dialog.prototype._stopDrag = function() {
    MochiKit.Signal.disconnectAll(document, "onmousemove");
    MochiKit.Signal.disconnectAll(document, "onmousedown");
    MochiKit.Signal.disconnectAll(document, "onmouseup");
}
