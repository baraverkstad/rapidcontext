/**
 * Creates a new tab container.
 *
 * @constructor
 */
function TabContainer() {
    this.domNode = null;
    this._labelsNode = null;
    this._containerNode = null;
    this._children = [];
    this._selected = null;
    this._init();
}

/**
 * Internal function to initialize the widget.
 *
 * @private
 */
TabContainer.prototype._init = function() {
    this._labelsNode = MochiKit.DOM.DIV({ "class": "uiTabContainerLabels" });
    this._containerNode = MochiKit.DOM.DIV({ "class": "uiTabContainerContent" });
    this.domNode = MochiKit.DOM.DIV({ "class": "uiTabContainer" },
                                    this._labelsNode, this._containerNode);
    ReTracer.Util.registerSizeConstraints(this._containerNode, "100% - 22", "100% - 47");
    this._containerNode.resizeContent = MochiKit.Base.bind("_resizeSelectedTab", this);
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
TabContainer.prototype.setStyle = function(style) {
    CssUtil.setStyles(this.domNode, style);
    // TODO: Remove this IE bugfix used to adjust container
    //       top position by removing padding and border width
    if (/MSIE/.test(navigator.userAgent)) {
        var b = ReTracer.Util.getBorderBox(this.domNode);
        var p = ReTracer.Util.getPaddingBox(this.domNode);
        var value = b.t + p.t + 1;
        MochiKit.Style.setElementPosition(this._containerNode, { y: -value });
    }
}

/**
 * Adds a child widget to this container.
 *
 * @param child              the child widget to add
 */
TabContainer.prototype.addChild = function(child) {
    if (!(child instanceof TabPane)) {
        throw new Error("TabContainer widget can only have TabPane children");
    }
    child.parent = this;
    this._children.push(child);
    this._labelsNode.appendChild(child.labelNode);
    this._containerNode.appendChild(child.domNode);
    if (this._selected == null) {
        this.selectTab(child);
    }
}

/**
 * Removes a child widget from this container.
 *
 * @param child              the child widget to remove
 */
TabContainer.prototype.removeChild = function(child) {
    if (!(child instanceof TabPane)) {
        throw new Error("TabContainer widget can only have TabPane children");
    }
    if (this._selected == child) {
        this._selected.onBlur();
        this._selected = null;
    }
    child.onClose();
    child.parent = null;
    // TODO: replace ArrayUtil
    ArrayUtil.removeElem(this._children, child);
    MochiKit.DOM.removeElement(child.labelNode);
    MochiKit.DOM.removeElement(child.domNode);
    ReTracer.Widget.destroyWidget(child.domNode);
    if (this._selected == null && this._children.length > 0) {
        this.selectTab(this._children[this._children.length - 1]);
    }
}

/**
 * Selects a specified tab in the container. This method can be
 * called without arguments to re-select the currently selected
 * tab.
 *
 * @param tab                the tab pane widget to select
 */
TabContainer.prototype.selectTab = function(tab) {
    if (this._selected != null) {
        this._selected.onBlur();
    }
    if (tab != null) {
        this._selected = tab;
    }
    if (this._selected != null) {
        this._selected.onFocus();
    }
}

/**
 * Returns the currently selected tab in the container.
 *
 * @return the tab pane widget selected, or
 *         null if no tab is selected
 */
TabContainer.prototype.getSelectedTab = function() {
    return this._selected;
}

/**
 * Resizes the currently selected tab in the container.
 */
TabContainer.prototype._resizeSelectedTab = function() {
    var tab = this.getSelectedTab();
    if (tab != null) {
        ReTracer.Util.resizeElements(tab.domNode);
    }
}


/**
 * Creates a new tab container pane.
 *
 * @constructor
 * @param title              the tab title
 * @param closeable          the closeable flag, default is false
 */
function TabPane(title, closeable) {
    this.parent = null;
    this.labelNode = null;
    this.domNode = null;
    this._textNode = null;
    this._buttonNode = null;
    this._init(title, closeable);
}

/**
 * Internal function to initialize the widget.
 *
 * @param title              the tab title
 * @param closeable          the closeable flag, default is false
 *
 * @private
 */
TabPane.prototype._init = function(title, closeable) {
    this._textNode = MochiKit.DOM.SPAN(null, title);
    var div = MochiKit.DOM.DIV(null, this._textNode);
    if (closeable) {
        this._buttonNode = Icon.CLOSE.createElement();
        div.appendChild(this._buttonNode);
        MochiKit.Signal.connect(this._buttonNode, "onclick", this, "_handleClose");
    }
    this.labelNode = MochiKit.DOM.DIV({ "class": "uiTabContainerLabel" }, div);
    MochiKit.Signal.connect(this.labelNode, "onclick", this, "focus");
    this.domNode = MochiKit.DOM.DIV({ "class": "uiTabPane" });
    ReTracer.Util.registerSizeConstraints(this.domNode, "100%", "100%");
    MochiKit.Style.hideElement(this.domNode);
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
TabPane.prototype.setStyle = function(style) {
    CssUtil.setStyles(this.domNode, style);
}

/**
 * Checks if this tab is selected in the tab container.
 *
 * @return true if this tab is selected, or
 *         false otherwise
 */
TabPane.prototype.hasFocus = function() {
    return this.parent != null && this.parent.getSelectedTab() == this;
}

/**
 * Selects this tab in the tab container.
 */
TabPane.prototype.focus = function() {
    if (this.parent != null) {
        this.parent.selectTab(this);
    }
}

/**
 * Adds a child element to this pane.
 *
 * @param child              the child element to add
 */
TabPane.prototype.addChild = function(child) {
    if (child.domNode) {
        this.domNode.appendChild(child.domNode);
    } else if (child.nodeType) {
        this.domNode.appendChild(child);
    } else {
        this.domNode.innerHTML = child;
    }
    ReTracer.Util.resizeElements(this.domNode);
}

/**
 * Removes all child elements from this pane.
 */
TabPane.prototype.removeAllChildren = function() {
    this.domNode.innerHTML = "";
}

/**
 * The tab pane close (or remove) signal.
 *
 * @signal Emitted when the pane is being closed
 */
TabPane.prototype.onClose = function() {
    if (this._buttonNode) {
        MochiKit.Signal.disconnectAll(this._buttonNode);
    }
    MochiKit.Signal.disconnectAll(this.labelNode);
}

/**
 * The widget focus signal.
 *
 * @signal Emitted when the widget gained focus.
 */
TabPane.prototype.onFocus = function() {
    MochiKit.DOM.addElementClass(this.labelNode, "selected");
    MochiKit.Style.showElement(this.domNode);
    ReTracer.Util.resizeElements(this.domNode);
}

/**
 * The widget blur signal.
 *
 * @signal Emitted when the widget lost focus.
 */
TabPane.prototype.onBlur = function() {
    ReTracer.Util.blurAll(this.domNode);
    MochiKit.DOM.removeElementClass(this.labelNode, "selected");
    MochiKit.Style.hideElement(this.domNode);
}

/**
 * Handles mouse clicks on the tab close button.
 *
 * @param {Object} evt the MochiKit.Signal.Event object
 *
 * @private
 */
TabPane.prototype._handleClose = function(evt) {
    evt.stop();
    if (this.parent != null) {
        this.parent.removeChild(this);
    }
}

// Register function names
ReTracer.Util.registerFunctionNames(TabContainer.prototype, "TabContainer");
ReTracer.Util.registerFunctionNames(TabPane.prototype, "TabPane");
