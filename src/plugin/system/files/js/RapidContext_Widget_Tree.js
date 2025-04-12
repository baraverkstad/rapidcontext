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
 * Creates a new tree widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {boolean} [attrs.hidden] the hidden widget flag, defaults to false
 * @param {...TreeNode} [child] the child tree node widgets
 *
 * @return {Widget} the widget DOM node
 *
 * @class The tree widget class. Used to provide a dynamic tree with expandable
 *     tree nodes, using a number of `<div>` HTML elements.
 * @extends RapidContext.Widget
 *
 * @example <caption>JavaScript</caption>
 * var tree = RapidContext.Widget.Tree({ style: { width: "200px", height: "400px" } });
 * var root = RapidContext.Widget.TreeNode({ folder: true, name: "Root" });
 * var child = RapidContext.Widget.TreeNode({ name: "Child" });
 * root.addAll(child);
 * tree.addAll(root);
 *
 * @example <caption>User Interface XML</caption>
 * <Tree style="width: 200px; height: 400px;">
 *   <TreeNode name="Root">
 *     <TreeNode name="Child" />
 *   </TreeNode>
 * </Tree>
 */
RapidContext.Widget.Tree = function (attrs/*, ...*/) {
    var o = RapidContext.UI.DIV(attrs);
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.Tree);
    o.classList.add("widgetTree");
    o.selectedPath = null;
    o.setAttrs(attrs);
    o.addAll(Array.from(arguments).slice(1));
    o.on("click", ".widgetTreeNodeLabel", o._handleNodeLabelClick);
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Tree = RapidContext.Widget.Tree;

/**
 * Emitted when a tree node is expanded.
 *
 * @name RapidContext.Widget.Tree#onexpand
 * @event
 */

/**
 * Emitted when a tree node is collapsed.
 *
 * @name RapidContext.Widget.Tree#oncollapse
 * @event
 */

/**
 * Emitted when a tree node is selected. It will be emitted after
 * "onunselect" if another node was previously selected.
 *
 * @name RapidContext.Widget.Tree#onselect
 * @event
 */

/**
 * Emitted when a tree node selection is removed. It will be emitted
 * before "onselect" if another node was previously selected.
 *
 * @name RapidContext.Widget.Tree#onunselect
 * @event
 */

/**
 * Handles node label click events.
 *
 * @param {Event} evt the DOM Event object
 */
RapidContext.Widget.Tree.prototype._handleNodeLabelClick = function (evt) {
    var node = evt.target.closest("div.widgetTreeNode");
    if (RapidContext.Widget.isWidget(evt.target, "Icon")) {
        node.toggle();
    } else {
        node.select();
    }
};

/**
 * Adds a single child tree node widget to this widget.
 *
 * @param {Widget} child the tree node widget to add
 */
RapidContext.Widget.Tree.prototype.addChildNode = function (child) {
    if (!RapidContext.Widget.isWidget(child, "TreeNode")) {
        throw new Error("Tree widget can only have TreeNode children");
    }
    this.append(child);
};

/**
 * Removes all marked tree nodes. When adding or updating tree nodes, any
 * node modified is automatically unmarked (e.g. by calling `setAttrs` on the
 * tree nodes or `addPath` on the tree). This makes it easy to prune a tree
 * after an update, by initially marking all tree nodes with `markAll()`,
 * inserting or touching all nodes to keep, and finally calling this method to
 * remove the remaining nodes.
 *
 * @example
 * tree.markAll();
 * tree.addPath(["Root", "Child"]);
 * ...
 * tree.removeAllMarked();
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
 * Marks this tree node and all child nodes recursively. When adding or
 * updating tree nodes, any node modified is automatically unmarked (e.g. by
 * calling `setAttrs` on the tree nodes or `addPath` on the tree). This makes
 * it easy to prune a tree after an update, by initially marking all tree
 * nodes, inserting or touching all nodes to keep, and finally calling
 * `removeAllMarked()` to remove the remaining nodes.
 *
 * @example
 * tree.markAll();
 * tree.addPath(["Root", "Child"]);
 * ...
 * tree.removeAllMarked();
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
 * @param {string} name the root tree node name
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
    return this.findByPath(this.selectedPath);
};

/**
 * Sets the currently selected node in the tree. This method is only
 * called from the tree node `select()` and `unselect()` methods.
 *
 * @param {Widget} node the new selected tree node, or null for none
 */
RapidContext.Widget.Tree.prototype._handleSelect = function (node) {
    var prev = this.selectedChild();
    if (node == null) {
        this.selectedPath = null;
        this.emit("unselect");
    } else {
        if (prev != null && prev !== node) {
            prev.unselect();
        }
        this.selectedPath = node.path();
        this.emit("select");
    }
};

/**
 * Recursively expands all nodes. If a depth is specified,
 * expansions will not continue below that depth.
 *
 * @param {number} [depth] the optional maximum depth
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
 * @param {number} [depth] the optional minimum depth
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
        node = RapidContext.Widget.TreeNode({ name: path[0], folder: (path.length > 1) });
        this.addChildNode(node);
    }
    node.marked = false;
    for (var i = 1; i < path.length; i++) {
        var child = node.findChild(path[i]);
        if (child == null) {
            var attrs = { name: path[i], folder: (path.length > i + 1) };
            child = RapidContext.Widget.TreeNode(attrs);
            node.addChildNode(child);
        }
        child.marked = false;
        node = child;
    }
    return node;
};
