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
 * Creates a new tree node widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {String} attrs.name the tree node name
 * @param {Boolean} [attrs.folder] the folder flag, defaults to false
 * @param {String} [attrs.icon] the icon reference to use, defaults
 *            to "FOLDER" for folders and "DOCUMENT" otherwise
 * @param {String} [attrs.tooltip] the tooltip text when hovering
 * @param {Widget} [...] the child tree node widgets
 *
 * @return {Widget} the widget DOM node
 *
 * @class The tree node widget class. Used to provide a tree node in
 *     a tree, using a number of &lt;div&gt; HTML elements. Note that
 *     events should normally not be listened for on individual tree
 *     nodes, but rather on the tree as a whole.
 * @extends RapidContext.Widget
 */
RapidContext.Widget.TreeNode = function (attrs/*, ...*/) {
    var icon = RapidContext.Widget.Icon({ ref: "BLANK" });
    var label = MochiKit.DOM.SPAN({ "class": "widgetTreeNodeText" });
    var div = MochiKit.DOM.DIV({ "class": "widgetTreeNodeLabel" }, icon, label);
    var o = MochiKit.DOM.DIV({}, div);
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetTreeNode");
    attrs = MochiKit.Base.update({ name: "Tree Node", folder: false }, attrs);
    if (typeof(attrs.icon) == "undefined") {
        attrs.icon = attrs.folder ? "FOLDER" : "DOCUMENT";
    }
    o.setAttrs(attrs);
    o.addAll(MochiKit.Base.extend(null, arguments, 1));
    icon.onclick = RapidContext.Widget._eventHandler("TreeNode", "toggle");
    div.onclick = RapidContext.Widget._eventHandler("TreeNode", "select");
    return o;
};

// Register widget class
RapidContext.Widget.Classes.TreeNode = RapidContext.Widget.TreeNode;

/**
 * Returns and/or creates the child container DOM node. If a child
 * container is created, it will be hidden by default.
 *
 * @param {Boolean} create the optional create flag, defaults to false
 *
 * @return {Node} the child container DOM node found or created
 */
RapidContext.Widget.TreeNode.prototype._container = function (create) {
    var container = this.lastChild;
    if (MochiKit.DOM.hasElementClass(container, "widgetTreeNodeContainer")) {
        return container;
    } else if (create) {
        container = MochiKit.DOM.DIV({ "class": "widgetTreeNodeContainer widgetHidden" });
        this.appendChild(container);
        var imgNode = this.firstChild.firstChild;
        imgNode.setAttrs({ ref: "PLUS" });
        this.setAttrs({ icon: "FOLDER" });
        return container;
    } else {
        return null;
    }
};

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {String} [attrs.name] the tree node name
 * @param {Boolean} [attrs.folder] the folder flag, cannot be
 *            reverted to false once set (implicitly or explicitly)
 * @param {Icon/Object/String} [attrs.icon] icon the icon to set, or
 *            null to remove
 * @param {String} [attrs.tooltip] the tooltip text when hovering
 */
RapidContext.Widget.TreeNode.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
    this.marked = false;
    var locals = RapidContext.Util.mask(attrs, ["name", "folder", "icon", "tooltip"]);
    if (typeof(locals.name) != "undefined") {
        this.name = locals.name;
        var node = this.firstChild.firstChild;
        while (!MochiKit.DOM.hasElementClass(node, "widgetTreeNodeText")) {
            node = node.nextSibling;
        }
        MochiKit.DOM.replaceChildNodes(node, locals.name);
    }
    if (MochiKit.Base.bool(locals.folder)) {
        this._container(true);
    }
    if (typeof(locals.icon) != "undefined") {
        var imgNode = this.firstChild.firstChild;
        var iconNode = imgNode.nextSibling;
        if (!RapidContext.Widget.isWidget(iconNode, "Icon")) {
            iconNode = null;
        }
        if (iconNode == null && locals.icon != null) {
            if (typeof(locals.icon) === "string") {
                locals.icon = RapidContext.Widget.Icon({ ref: locals.icon });
            } else if (!RapidContext.Widget.isWidget(locals.icon, "Icon")) {
                locals.icon = RapidContext.Widget.Icon(locals.icon);
            }
            MochiKit.DOM.insertSiblingNodesAfter(imgNode, locals.icon);
        } else if (iconNode != null && locals.icon != null) {
            if (RapidContext.Widget.isWidget(locals.icon, "Icon")) {
                MochiKit.DOM.swapDOM(iconNode, locals.icon);
            } else if (typeof(locals.icon) === "string") {
                iconNode.setAttrs({ ref: locals.icon });
            } else {
                iconNode.setAttrs(locals.icon);
            }
        } else if (iconNode != null && locals.icon == null) {
            RapidContext.Widget.destroyWidget(iconNode);
        }
    }
    if (typeof(locals.tooltip) != "undefined") {
        this.firstChild.title = locals.tooltip;
    }
    this.__setAttrs(attrs);
};

/**
 * Returns an array with all child tree node widgets. Note that the
 * array is a real JavaScript array, not a dynamic NodeList.
 *
 * @return {Array} the array of child tree node widgets
 */
RapidContext.Widget.TreeNode.prototype.getChildNodes = function () {
    var container = this._container();
    if (container == null) {
        return [];
    } else {
        return MochiKit.Base.extend([], container.childNodes);
    }
};

/**
 * Adds a single child tree node widget to this widget.
 *
 * @param {Widget} child the tree node widget to add
 */
RapidContext.Widget.TreeNode.prototype.addChildNode = function (child) {
    if (!RapidContext.Widget.isWidget(child, "TreeNode")) {
        throw new Error("TreeNode widget can only have TreeNode children");
    }
    this._container(true).appendChild(child);
};

/**
 * Removes a single child tree node widget from this widget.
 *
 * @param {Widget} child the tree node widget to remove
 */
RapidContext.Widget.TreeNode.prototype.removeChildNode = function (child) {
    var container = this._container();
    if (container != null) {
        container.removeChild(child);
    }
};

/**
 * Removes all tree nodes that are marked as unmodified. When adding
 * or updating nodes, they (and their parent nodes) are automatically
 * marked as modified. This function makes tree pruning possible, by
 * initially marking all tree nodes as unmodified (clearing any
 * previous modified flag), touching all nodes to be kept, and
 * finally calling this method to remove the remaining nodes.
 */
RapidContext.Widget.TreeNode.prototype.removeAllMarked = function () {
    var children = this.getChildNodes();
    for (var i = 0; i < children.length; i++) {
        if (children[i].marked === true) {
            this.removeChildNode(children[i]);
        } else {
            children[i].removeAllMarked();
        }
    }
};

/**
 * Marks all tree nodes as unmodified. When adding or updating nodes,
 * they (and their parent nodes) are automatically marked as
 * modified. This function makes tree pruning possible, by initially
 * marking all tree nodes (clearing any previous modified flag),
 * touching all nodes to be kept, and finally calling the
 * removeAllMarked() method to remove the remaining nodes.
 */
RapidContext.Widget.TreeNode.prototype.markAll = function () {
    this.marked = true;
    var children = this.getChildNodes();
    for (var i = 0; i < children.length; i++) {
        children[i].markAll();
    }
};

/**
 * Checks if this node is a folder.
 *
 * @return {Boolean} true if this node is a folder, or
 *         false otherwise
 */
RapidContext.Widget.TreeNode.prototype.isFolder = function () {
    return this._container() != null;
};

/**
 * Checks if this folder node is expanded.
 *
 * @return {Boolean} true if this node is expanded, or
 *         false otherwise
 */
RapidContext.Widget.TreeNode.prototype.isExpanded = function () {
    var container = this._container();
    return container != null &&
           !MochiKit.DOM.hasElementClass(container, "widgetHidden");
};

/**
 * Checks if this node is selected.
 *
 * @return {Boolean} true if the node is selected, or
 *         false otherwise
 */
RapidContext.Widget.TreeNode.prototype.isSelected = function () {
    return MochiKit.DOM.hasElementClass(this.firstChild, "selected");
};

/**
 * Returns the ancestor tree widget.
 *
 * @return {Widget} the ancestor tree widget, or
 *         null if none was found
 */
RapidContext.Widget.TreeNode.prototype.tree = function () {
    var parent = this.parent();
    if (parent != null) {
        return parent.tree();
    }
    if (RapidContext.Widget.isWidget(this.parentNode, "Tree")) {
        return this.parentNode;
    } else {
        return null;
    }
};

/**
 * Returns the parent tree node widget.
 *
 * @return {Widget} the parent tree node widget, or
 *         null if this is a root node
 */
RapidContext.Widget.TreeNode.prototype.parent = function () {
    var node = this.parentNode;
    if (MochiKit.DOM.hasElementClass(node, "widgetTreeNodeContainer")) {
        return node.parentNode;
    } else {
        return null;
    }
};

/**
 * Returns the path to this tree node.
 *
 * @return {Array} the tree node path, i.e an array of node names
 */
RapidContext.Widget.TreeNode.prototype.path = function () {
    var parent = this.parent();
    if (parent == null) {
        return [this.name];
    } else {
        var path = parent.path();
        path.push(this.name);
        return path;
    }
};

/**
 * Finds a child tree node with the specified name.
 *
 * @param {String} name the child tree node name
 *
 * @return {Widget} the child tree node found, or
 *         null if not found
 */
RapidContext.Widget.TreeNode.prototype.findChild = function (name) {
    var children = this.getChildNodes();
    for (var i = 0; i < children.length; i++) {
        if (children[i].name == name) {
            return children[i];
        }
    }
    return null;
};

/**
 * Searches for a descendant tree node from the specified path.
 *
 * @param {Array} path the tree node path (array of node names)
 *
 * @return {Widget} the descendant tree node found, or
 *         null if not found
 */
RapidContext.Widget.TreeNode.prototype.findByPath = function (path) {
    var node = this;

    for (var i = 0; node != null && path != null && i < path.length; i++) {
        node = node.findChild(path[i]);
    }
    return node;
};

/**
 * Selects this tree node.
 */
RapidContext.Widget.TreeNode.prototype.select = function () {
    MochiKit.DOM.addElementClass(this.firstChild, "selected");
    var tree = this.tree();
    if (tree != null) {
        tree._handleSelect(this);
    }
    this.expand();
};

/**
 * Unselects this tree node.
 */
RapidContext.Widget.TreeNode.prototype.unselect = function () {
    if (this.isSelected()) {
        MochiKit.DOM.removeElementClass(this.firstChild, "selected");
        var tree = this.tree();
        if (tree != null) {
            tree._handleSelect(null);
        }
    }
};

/**
 * Expands this node to display any child nodes. If the parent node
 * is not expanded, it will be expanded as well.
 */
RapidContext.Widget.TreeNode.prototype.expand = function () {
    var parent = this.parent();
    if (parent != null && !parent.isExpanded()) {
        parent.expand();
    }
    var container = this._container();
    if (container != null && !this.isExpanded()) {
        var imgNode = this.firstChild.firstChild;
        imgNode.setAttrs({ ref: "MINUS" });
        MochiKit.DOM.removeElementClass(container, "widgetHidden");
        var tree = this.tree();
        if (tree != null) {
            tree._emitExpand(this);
        }
    }
};

/**
 * Recursively expands this node and all its children. If a depth is
 * specified, expansions will not continue below that depth.
 *
 * @param {Number} [depth] the optional maximum depth
 */
RapidContext.Widget.TreeNode.prototype.expandAll = function (depth) {
    if (typeof(depth) !== "number") {
        depth = 10;
    }
    this.expand();
    var children = this.getChildNodes();
    for (var i = 0; depth > 0 && i < children.length; i++) {
        children[i].expandAll(depth - 1);
    }
};

/**
 * Collapses this node to hide any child nodes.
 */
RapidContext.Widget.TreeNode.prototype.collapse = function () {
    var container = this._container();
    if (container != null && this.isExpanded()) {
        var imgNode = this.firstChild.firstChild;
        imgNode.setAttrs({ ref: "PLUS" });
        MochiKit.DOM.addElementClass(container, "widgetHidden");
        var tree = this.tree();
        if (tree != null) {
            tree._emitExpand(this);
        }
    }
};

/**
 * Recursively collapses this node and all its children. If a depth
 * is specified, only children below that depth will be collapsed.
 *
 * @param {Number} [depth] the optional minimum depth
 */
RapidContext.Widget.TreeNode.prototype.collapseAll = function (depth) {
    if (typeof(depth) !== "number") {
        depth = 0;
    }
    if (depth <= 0) {
        this.collapse();
    }
    var children = this.getChildNodes();
    for (var i = 0; i < children.length; i++) {
        children[i].collapseAll(depth - 1);
    }
};

/**
 * Toggles expand and collapse for this node.
 */
RapidContext.Widget.TreeNode.prototype.toggle = function (evt) {
    if (evt) {
        evt.stop();
    }
    if (this.isExpanded()) {
        this.collapse();
    } else {
        this.expand();
    }
};
