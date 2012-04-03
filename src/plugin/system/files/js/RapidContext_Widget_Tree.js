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
 * Creates a new tree widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {Widget} [...] the child tree node widgets
 *
 * @return {Widget} the widget DOM node
 *
 * @class The tree widget class. Used to provide a dynamic tree with
 *     expandable tree nodes, using a number of &lt;div&gt; HTML
 *     elements. The the "onexpand" and "onselect" event are emitted
 *     whenever a node is expanded, collapsed or selected.
 * @extends RapidContext.Widget
 */
RapidContext.Widget.Tree = function (attrs/*, ...*/) {
    var o = MochiKit.DOM.DIV(attrs);
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetTree");
    o.resizeContent = MochiKit.Base.noop;
    o.selectedPath = null;
    o.addAll(MochiKit.Base.extend(null, arguments, 1));
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Tree = RapidContext.Widget.Tree;

/**
 * Adds a single child tree node widget to this widget.
 *
 * @param {Widget} child the tree node widget to add
 */
RapidContext.Widget.Tree.prototype.addChildNode = function (child) {
    if (!RapidContext.Widget.isWidget(child, "TreeNode")) {
        throw new Error("Tree widget can only have TreeNode children");
    }
    this.appendChild(child);
};

/**
 * Removes all tree nodes that are marked as unmodified. When adding
 * or updating nodes, they (and their parent nodes) are automatically
 * marked as modified. This function makes tree pruning possible, by
 * initially marking all tree nodes as unmodified (clearing any
 * previous modified flag), touching all nodes to be kept, and
 * finally calling this method to remove the remaining nodes.
 */
RapidContext.Widget.Tree.prototype.removeAllMarked = function () {
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
RapidContext.Widget.Tree.prototype.markAll = function () {
    var children = this.getChildNodes();
    for (var i = 0; i < children.length; i++) {
        children[i].markAll();
    }
};

/**
 * Finds a root tree node with the specified name.
 *
 * @param {String} name the root tree node name
 *
 * @return {Widget} the root tree node found, or
 *         null if not found
 */
RapidContext.Widget.Tree.prototype.findRoot = function (name) {
    var children = this.getChildNodes();
    for (var i = 0; i < children.length; i++) {
        if (children[i].name == name) {
            return children[i];
        }
    }
    return null;
};

/**
 * Searches for a tree node from the specified path.
 *
 * @param {Array} path the tree node path (array of names)
 *
 * @return {Widget} the descendant tree node found, or
 *         null if not found
 */
RapidContext.Widget.Tree.prototype.findByPath = function (path) {
    if (path == null || path.length < 1) {
        return null;
    }
    var root = this.findRoot(path[0]);
    if (root != null) {
        return root.findByPath(path.slice(1));
    } else {
        return null;
    }
};

/**
 * Returns the currently selected tree node.
 *
 * @return {Widget} the currently selected tree node, or
 *         null if no node is selected
 */
RapidContext.Widget.Tree.prototype.selectedChild = function () {
    if (this.selectedPath == null) {
        return null;
    } else {
        return this.findByPath(this.selectedPath);
    }
};

/**
 * Sets the currently selected node in the tree. This method is only
 * called from the tree node select() and unselect() methods.
 *
 * @param {Widget} node the new selected tree node, or null for none
 */
RapidContext.Widget.Tree.prototype._handleSelect = function (node) {
    var prev = this.selectedChild();
    if (node == null) {
        this.selectedPath = null;
        RapidContext.Widget.emitSignal(this, "onselect", null);
    } else {
        if (prev != null && prev !== node) {
            prev.unselect();
        }
        this.selectedPath = node.path();
        RapidContext.Widget.emitSignal(this, "onselect", node);
    }
};

/**
 * Emits a signal when a node has been expanded or collapsed.
 *
 * @param {Widget} node the affected tree node
 */
RapidContext.Widget.Tree.prototype._emitExpand = function (node) {
    RapidContext.Widget.emitSignal(this, "onexpand", node);
};

/**
 * Recursively expands all nodes. If a depth is specified,
 * expansions will not continue below that depth.
 *
 * @param {Number} [depth] the optional maximum depth
 */
RapidContext.Widget.Tree.prototype.expandAll = function (depth) {
    if (typeof(depth) !== "number") {
        depth = 10;
    }
    var children = this.getChildNodes();
    for (var i = 0; depth > 0 && i < children.length; i++) {
        children[i].expandAll(depth - 1);
    }
};

/**
 * Recursively collapses all nodes. If a depth is specified, only
 * nodes below that depth will be collapsed.
 *
 * @param {Number} [depth] the optional minimum depth
 */
RapidContext.Widget.Tree.prototype.collapseAll = function (depth) {
    if (typeof(depth) !== "number") {
        depth = 0;
    }
    var children = this.getChildNodes();
    for (var i = 0; i < children.length; i++) {
        children[i].collapseAll(depth - 1);
    }
};

/**
 * Adds a path to the tree as a recursive list of child nodes. If
 * nodes in the specified path already exists, they will be used
 * instead of creating new nodes.
 *
 * @param {Array} path the tree node path (array of names)
 *
 * @return {Widget} the last node in the path
 */
RapidContext.Widget.Tree.prototype.addPath = function (path) {
    if (path == null || path.length < 1) {
        return null;
    }
    var node = this.findRoot(path[0]);
    if (node == null) {
        node = RapidContext.Widget.TreeNode({ name: path[0] });
        this.addChildNode(node);
    }
    node.marked = false;
    for (var i = 1; i < path.length; i++) {
        var child = node.findChild(path[i]);
        if (child == null) {
            child = RapidContext.Widget.TreeNode({ name: path[i] });
            node.addChildNode(child);
        }
        child.marked = false;
        node = child;
    }
    return node;
};
