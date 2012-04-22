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
 * Creates a new tab container widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {Widget} [...] the child widgets or DOM nodes (should be
 *            Pane widgets)
 *
 * @return {Widget} the widget DOM node
 *
 * @class The tab container widget class. Used to provide a set of
 *     tabbed pages, where the user can switch page freely.
 *     Internally it uses a &lt;div&gt; HTML element containing Pane
 *     widgets that are hidden and shown according to the page
 *     transitions. If a child Pane widget is "pageCloseable", a
 *     close button will be available on the tab label and an
 *     "onclose" signal will be emitted for that node when removed
 *     from the container.
 * @extends RapidContext.Widget
 */
RapidContext.Widget.TabContainer = function (attrs/*, ... */) {
    var labels = MochiKit.DOM.DIV({ "class": "widgetTabContainerLabels" });
    var container = MochiKit.DOM.DIV({ "class": "widgetTabContainerContent" });
    var o = MochiKit.DOM.DIV(attrs, labels, container);
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetTabContainer");
    // TODO: possibly add MSIE size fix?
    RapidContext.Util.registerSizeConstraints(container, "100% - 22", "100% - 47");
    container.resizeContent = MochiKit.Base.noop;
    o._selectedIndex = -1;
    o.setAttrs(attrs);
    o.addAll(MochiKit.Base.extend(null, arguments, 1));
    return o;
};

// Register widget class
RapidContext.Widget.Classes.TabContainer = RapidContext.Widget.TabContainer;

/**
 * Returns an array with all child pane widgets. Note that the array
 * is a real JavaScript array, not a dynamic NodeList.
 *
 * @return {Array} the array of child DOM nodes
 */
RapidContext.Widget.TabContainer.prototype.getChildNodes = function () {
    return MochiKit.Base.extend([], this.lastChild.childNodes);
};

/**
 * Adds a single child page widget to this widget. The child widget
 * should be a RapidContext.Widget.Pane widget, or it will be added to a
 * new one.
 *
 * @param {Widget} child the page widget to add
 */
RapidContext.Widget.TabContainer.prototype.addChildNode = function (child) {
    if (!RapidContext.Widget.isWidget(child, "Pane")) {
        child = RapidContext.Widget.Pane(null, child);
    }
    RapidContext.Util.registerSizeConstraints(child, "100%", "100%");
    child.hide();
    var text = MochiKit.DOM.SPAN(null, child.pageTitle);
    if (child.pageCloseable) {
        var icon = RapidContext.Widget.Icon({ ref: "DEFAULT", tooltip: "Close" });
        // TODO: potential memory leak with stale child object references
        icon.onclick = RapidContext.Widget._eventHandler("TabContainer", "_handleClose", child);
    }
    var label = MochiKit.DOM.DIV({ "class": "widgetTabContainerLabel" },
                                 MochiKit.DOM.DIV({}, text, icon));
    // TODO: potential memory leak with stale child object references
    label.onclick = RapidContext.Widget._eventHandler("TabContainer", "selectChild", child);
    this.firstChild.appendChild(label);
    this.lastChild.appendChild(child);
    if (this._selectedIndex < 0) {
        this.selectChild(0);
    }
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
RapidContext.Widget.TabContainer.prototype.removeChildNode = function (child) {
    var children = this.getChildNodes();
    var index = MochiKit.Base.findIdentical(children, child);
    if (index < 0) {
        throw new Error("Cannot remove DOM node that is not a TabContainer child");
    }
    if (this._selectedIndex == index) {
        child._handleExit();
        this._selectedIndex = -1;
    }
    RapidContext.Widget.destroyWidget(this.firstChild.childNodes[index]);
    MochiKit.DOM.removeElement(child);
    RapidContext.Widget.emitSignal(child, "onclose");
    if (this._selectedIndex > index) {
        this._selectedIndex--;
    }
    if (this._selectedIndex < 0 && this.getChildNodes().length > 0) {
        this.selectChild((index == 0) ? 0 : index - 1);
    }
};

// TODO: add support for status updates in child pane widget

/**
 * Returns the index of the currently selected child in the tab
 * container.
 *
 * @return {Number} the index of the selected child, or
 *         -1 if no child is selected
 */
RapidContext.Widget.TabContainer.prototype.selectedIndex = function () {
    return this._selectedIndex;
};

/**
 * Returns the child widget currently selected in the tab container.
 *
 * @return {Node} the child widget selected, or
 *         null if no child is selected
 */
RapidContext.Widget.TabContainer.prototype.selectedChild = function () {
    var children = this.getChildNodes();
    return (this._selectedIndex < 0) ? null : children[this._selectedIndex];
};

/**
 * Selects a specified child in the tab container. This method can be
 * called without arguments to re-select the currently selected tab.
 *
 * @param {Number/Node} [indexOrChild] the child index or node
 */
RapidContext.Widget.TabContainer.prototype.selectChild = function (indexOrChild) {
    var children = this.getChildNodes();
    if (this._selectedIndex >= 0) {
        var label = this.firstChild.childNodes[this._selectedIndex];
        MochiKit.DOM.removeElementClass(label, "selected");
        children[this._selectedIndex]._handleExit();
    }
    var index = -1;
    if (indexOrChild == null) {
        index = this._selectedIndex;
    } else if (typeof(indexOrChild) == "number") {
        index = indexOrChild;
    } else {
        index = MochiKit.Base.findIdentical(children, indexOrChild);
    }
    this._selectedIndex = (index < 0 || index >= children.length) ? -1 : index;
    if (this._selectedIndex >= 0) {
        var label = this.firstChild.childNodes[this._selectedIndex];
        MochiKit.DOM.addElementClass(label, "selected");
        children[this._selectedIndex]._handleEnter();
    }
};

/**
 * Resizes the currently selected child. This method need not be called
 * directly, but is automatically called whenever a parent node is
 * resized. It optimizes the resize chain by only resizing those DOM
 * child nodes that are visible, i.e. the currently selected tab
 * container child.
 */
RapidContext.Widget.TabContainer.prototype.resizeContent = function () {
    RapidContext.Util.resizeElements(this.lastChild);
    var child = this.selectedChild();
    if (child != null) {
        RapidContext.Util.resizeElements(child);
    }
};

/**
 * Handles the tab close event.
 *
 * @param {Node} child the child DOM node
 * @param {Event} evt the MochiKit.Signal.Event object
 */
RapidContext.Widget.TabContainer.prototype._handleClose = function (child, evt) {
    evt.stop();
    this.removeChildNode(child);
};
