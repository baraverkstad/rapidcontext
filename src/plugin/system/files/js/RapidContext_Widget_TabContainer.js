/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2025 Per Cederberg. All rights reserved.
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
 * Creates a new tab container widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {boolean} [attrs.hidden] the hidden widget flag, defaults to false
 * @param {...Pane} [child] the child Pane widgets
 *
 * @return {Widget} the widget DOM node
 *
 * @class The tab container widget class. Used to provide a set of tabbed
 *     pages, where the user can switch page freely. Internally it uses a
 *     `<div>` HTML element containing `Pane` widgets that are hidden and shown
 *     according to the page transitions. If a child `Pane` widget is
 *     `pageCloseable`, a close button will be available on the tab label.
 * @extends RapidContext.Widget
 *
 * @example <caption>JavaScript</caption>
 * let page1 = RapidContext.Widget.Pane({ pageTitle: "One" });
 * ...
 * let page2 = RapidContext.Widget.Pane({ pageTitle: "Two", pageCloseable: true });
 * ...
 * let attrs = { style: { width: "100%", height: "100%" } };
 * let tabs = RapidContext.Widget.TabContainer(attrs, page1, page2);
 *
 * @example <caption>User Interface XML</caption>
 * <TabContainer style="width: 100%; height: 100%;">
 *   <Pane pageTitle="One">
 *     ...
 *   </Pane>
 *   <Pane pageTitle="Two" pageCloseable="true">
 *     ...
 *   </Pane>
 * <TabContainer>
 */
RapidContext.Widget.TabContainer = function (attrs/*, ... */) {
    const labels = RapidContext.UI.DIV({ "class": "widgetTabContainerLabels" });
    const container = RapidContext.UI.DIV({ "class": "widgetTabContainerContent" });
    const o = RapidContext.UI.DIV(attrs, labels, container);
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.TabContainer);
    o.classList.add("widgetTabContainer");
    o._selectedIndex = -1;
    o.setAttrs(attrs);
    o.addAll(Array.from(arguments).slice(1));
    o.on("click", ".widgetTabContainerLabel", o._handleLabelClick);
    return o;
};

// Register widget class
RapidContext.Widget.Classes.TabContainer = RapidContext.Widget.TabContainer;

/**
 * Handles tab label click events.
 *
 * @param {Event} evt the DOM Event object
 */
RapidContext.Widget.TabContainer.prototype._handleLabelClick = function (evt) {
    const label = evt.delegateTarget;
    const pos = label ? Array.from(label.parentNode.children).indexOf(label) : -1;
    const child = this.getChildNodes()[pos];
    if (child) {
        evt.preventDefault();
        evt.stopImmediatePropagation();
        if (evt.target.dataset.close) {
            this.removeChildNode(child);
        } else {
            this.selectChild(child);
        }
    }
};

/**
 * Returns an array with all child pane widgets. Note that the array
 * is a real JavaScript array, not a dynamic `NodeList`.
 *
 * @return {Array} the array of child DOM nodes
 */
RapidContext.Widget.TabContainer.prototype.getChildNodes = function () {
    return Array.from(this.lastChild.childNodes);
};

/**
 * Adds a single child page widget to this widget. The child widget
 * should be a `RapidContext.Widget.Pane` widget, or it will be added to a
 * new one.
 *
 * @param {Widget} child the page widget to add
 *
 * @see RapidContext.Widget.Pane
 */
RapidContext.Widget.TabContainer.prototype.addChildNode = function (child) {
    if (!RapidContext.Widget.isWidget(child, "Pane")) {
        child = RapidContext.Widget.Pane(null, child);
    }
    child.style.width = child.style.height = "100%";
    child.hide();
    const text = RapidContext.UI.SPAN({}, child.pageTitle);
    let icon = null;
    if (child.pageCloseable) {
        icon = RapidContext.Widget.Icon({ "class": "fa fa-close", tooltip: "Close" });
        icon.dataset.close = true;
    }
    const labelAttrs = { "class": "widgetTabContainerLabel" };
    const label = RapidContext.UI.DIV(labelAttrs, RapidContext.UI.DIV({}, text, icon));
    this.firstChild.append(label);
    this.lastChild.append(child);
    if (this._selectedIndex < 0) {
        this.selectChild(0);
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
RapidContext.Widget.TabContainer.prototype.removeChildNode = function (child) {
    const index = this.getChildNodes().indexOf(child);
    if (index < 0) {
        throw new Error("Cannot remove DOM node that is not a TabContainer child");
    }
    if (this._selectedIndex == index) {
        child._handleExit();
        this._selectedIndex = -1;
    }
    RapidContext.Widget.destroyWidget(this.firstChild.childNodes[index]);
    child.remove();
    child.emit && child.emit("close");
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
 * @return {number} the index of the selected child, or
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
    const children = this.getChildNodes();
    return (this._selectedIndex < 0) ? null : children[this._selectedIndex];
};

/**
 * Selects a specified child in the tab container. This method can be
 * called without arguments to re-select the currently selected tab.
 *
 * @param {number|Node} [indexOrChild] the child index or node
 */
RapidContext.Widget.TabContainer.prototype.selectChild = function (indexOrChild) {
    const children = this.getChildNodes();
    let label;
    if (this._selectedIndex >= 0) {
        label = this.firstChild.childNodes[this._selectedIndex];
        label.classList.remove("selected");
        children[this._selectedIndex]._handleExit();
    }
    let index;
    if (indexOrChild == null) {
        index = this._selectedIndex;
    } else if (typeof(indexOrChild) == "number") {
        index = indexOrChild;
    } else {
        index = children.indexOf(indexOrChild);
    }
    this._selectedIndex = (index < 0 || index >= children.length) ? -1 : index;
    if (this._selectedIndex >= 0) {
        label = this.firstChild.childNodes[this._selectedIndex];
        label.classList.add("selected");
        children[this._selectedIndex]._handleEnter();
    }
};
