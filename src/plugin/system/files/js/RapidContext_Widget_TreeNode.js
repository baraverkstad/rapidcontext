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
 * Creates a new tree node widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {string} attrs.name the tree node name
 * @param {boolean} [attrs.folder] the folder flag, defaults to `false` if no
 *            child nodes are provided in constructor call
 * @param {string} [attrs.icon] the icon reference to use, defaults
 *            to "FOLDER" for folders and "DOCUMENT" otherwise
 * @param {string} [attrs.tooltip] the tooltip text when hovering
 * @param {boolean} [attrs.hidden] the hidden widget flag, defaults to `false`
 * @param {...TreeNode} [child] the child tree node widgets
 *
 * @return {Widget} the widget DOM node
 *
 * @class The tree node widget class. Used to provide a tree node in a tree,
 *     using a number of `<div>` HTML elements. Note that events should
 *     normally not be listened for on individual tree nodes, but rather on
 *     the tree as a whole.
 * @extends RapidContext.Widget
 *
 * @example <caption>JavaScript</caption>
 * let parent = RapidContext.Widget.TreeNode({ folder: true, name: "Parent" });
 * let child = RapidContext.Widget.TreeNode({ name: "Child" });
 * parent.addAll(child);
 *
 * @example <caption>User Interface XML</caption>
 * <TreeNode name="Parent">
 *   <TreeNode name="Child" />
 * </TreeNode>
 */
RapidContext.Widget.TreeNode = function (attrs/*, ...*/) {
    const toggle = RapidContext.Widget.Icon("fa fa-fw");
    const icon = RapidContext.Widget.Icon("fa fa-fw fa-dot-circle-o");
    const label = RapidContext.UI.SPAN({ "class": "widgetTreeNodeText" });
    const cls = "widgetTreeNodeLabel overflow-ellipsis text-nowrap";
    const div = RapidContext.UI.DIV({ "class": cls }, toggle, icon, label);
    const o = RapidContext.UI.DIV({}, div);
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.TreeNode);
    o.classList.add("widgetTreeNode");
    const isFolder = (arguments.length > 1);
    attrs = { name: "Tree Node", folder: isFolder, ...attrs };
    o.setAttrs(attrs);
    o.addAll(Array.from(arguments).slice(1));
    return o;
};

// Register widget class
RapidContext.Widget.Classes.TreeNode = RapidContext.Widget.TreeNode;

/**
 * Returns and optionally creates the widget container DOM node. If a
 * child container is created, it will be hidden by default.
 *
 * @param {boolean} [create] the create flag, defaults to `false`
 *
 * @return {Node} the container DOM node, or
 *         null if this widget has no container (yet)
 */
RapidContext.Widget.TreeNode.prototype._containerNode = function (create) {
    let container = this.lastChild;
    if (container.classList.contains("widgetTreeNodeContainer")) {
        return container;
    } else if (create) {
        container = RapidContext.UI.DIV({ "class": "widgetTreeNodeContainer widgetHidden" });
        this.append(container);
        this.firstChild.childNodes[0].setAttrs({ "class": "fa fa-fw fa-plus-square-o" });
        if (!this.icon) {
            this.firstChild.childNodes[1].setAttrs({ "class": "fa fa-fw fa-folder" });
        }
        return container;
    } else {
        return null;
    }
};

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {string} [attrs.name] the tree node name
 * @param {boolean} [attrs.folder] the folder flag, cannot be
 *            reverted to `false` once set (implicitly or explicitly)
 * @param {Icon|Object|string} [attrs.icon] icon the icon to set, or
 *            null to remove
 * @param {string} [attrs.tooltip] the tooltip text when hovering
 * @param {boolean} [attrs.hidden] the hidden widget flag
 */
RapidContext.Widget.TreeNode.prototype.setAttrs = function (attrs) {
    this.marked = false;
    attrs = { ...attrs };
    if ("name" in attrs) {
        this.querySelector(".widgetTreeNodeText").innerText = attrs.name;
    }
    if ("folder" in attrs) {
        this._containerNode(RapidContext.Data.bool(attrs.folder));
        delete attrs.folder;
    }
    if ("icon" in attrs) {
        const icon = RapidContext.Widget.Icon(attrs.icon);
        this.firstChild.childNodes[1].replaceWith(icon);
    }
    if ("tooltip" in attrs) {
        this.firstChild.title = attrs.tooltip;
    }
    this.__setAttrs(attrs);
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
    this._containerNode(true).append(child);
};

/**
 * Removes a single child tree node widget from this widget.
 *
 * @param {Widget} child the tree node widget to remove
 */
RapidContext.Widget.TreeNode.prototype.removeChildNode = function (child) {
    const elem = this._containerNode();
    if (elem) {
        child && child.unselect();
        elem.removeChild(child);
    }
};

/**
 * Removes all marked tree nodes. When adding or updating tree nodes, any
 * node modified is automatically unmarked (e.g. by calling `setAttrs`). This
 * makes it easy to prune a tree after an update, by initially marking all
 * tree nodes with `markAll()`, inserting or touching all nodes to keep, and
 * finally calling this method to remove the remaining nodes.
 *
 * @example
 * parent.markAll();
 * parent.setAttrs();
 * child.setAttrs();
 * ...
 * parent.removeAllMarked();
 */
RapidContext.Widget.TreeNode.prototype.removeAllMarked = function () {
    const children = this.getChildNodes();
    for (let i = 0; i < children.length; i++) {
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
 * calling `setAttrs`). This makes it easy to prune a tree after an update, by
 * initially marking all tree nodes, inserting or touching all nodes to keep,
 * and finally calling `removeAllMarked()` to remove the remaining nodes.
 *
 * @example
 * parent.markAll();
 * parent.setAttrs();
 * child.setAttrs();
 * ...
 * parent.removeAllMarked();
 */
RapidContext.Widget.TreeNode.prototype.markAll = function () {
    this.marked = true;
    const children = this.getChildNodes();
    for (let i = 0; i < children.length; i++) {
        children[i].markAll();
    }
};

/**
 * Checks if this node is a folder.
 *
 * @return {boolean} `true` if this node is a folder, or
 *         `false` otherwise
 */
RapidContext.Widget.TreeNode.prototype.isFolder = function () {
    return this._containerNode() != null;
};

/**
 * Checks if this folder node is expanded.
 *
 * @return {boolean} `true` if this node is expanded, or
 *         `false` otherwise
 */
RapidContext.Widget.TreeNode.prototype.isExpanded = function () {
    const container = this._containerNode();
    return !!container && !container.classList.contains("widgetHidden");
};

/**
 * Checks if this node is selected.
 *
 * @return {boolean} `true` if the node is selected, or
 *         `false` otherwise
 */
RapidContext.Widget.TreeNode.prototype.isSelected = function () {
    return this.firstChild.classList.contains("selected");
};

/**
 * Returns the ancestor tree widget.
 *
 * @return {Widget} the ancestor tree widget, or
 *         null if none was found
 */
RapidContext.Widget.TreeNode.prototype.tree = function () {
    const parent = this.parent();
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
    const node = this.parentNode;
    if (node && node.classList.contains("widgetTreeNodeContainer")) {
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
    const parent = this.parent();
    if (parent == null) {
        return [this.name];
    } else {
        const path = parent.path();
        path.push(this.name);
        return path;
    }
};

/**
 * Finds a child tree node with the specified name.
 *
 * @param {string} name the child tree node name
 *
 * @return {Widget} the child tree node found, or
 *         null if not found
 */
RapidContext.Widget.TreeNode.prototype.findChild = function (name) {
    const children = this.getChildNodes();
    for (let i = 0; i < children.length; i++) {
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
    let node = this;
    if (path != null) {
        for (let i = 0; node != null && i < path.length; i++) {
            node = node.findChild(path[i]);
        }
    }
    return node;
};

/**
 * Selects this tree node.
 */
RapidContext.Widget.TreeNode.prototype.select = function () {
    this.firstChild.classList.add("selected");
    const tree = this.tree();
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
        this.firstChild.classList.remove("selected");
        const tree = this.tree();
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
    const parent = this.parent();
    if (parent != null && !parent.isExpanded()) {
        parent.expand();
    }
    const container = this._containerNode();
    if (container != null && !this.isExpanded()) {
        this.firstChild.childNodes[0].setAttrs({ "class": "fa fa-fw fa-minus-square-o" });
        if (!this.icon) {
            this.firstChild.childNodes[1].setAttrs({ "class": "fa fa-fw fa-folder-open" });
        }
        container.classList.remove("widgetHidden");
        const tree = this.tree();
        if (tree != null) {
            const detail = { tree: tree, node: this };
            tree.emit("expand", { detail: detail });
        }
    }
};

/**
 * Recursively expands this node and all its children. If a depth is
 * specified, expansions will not continue below that depth.
 *
 * @param {number} [depth] the optional maximum depth
 */
RapidContext.Widget.TreeNode.prototype.expandAll = function (depth) {
    if (typeof(depth) !== "number") {
        depth = 10;
    }
    this.expand();
    if (depth > 0) {
        const children = this.getChildNodes();
        for (let i = 0; i < children.length; i++) {
            children[i].expandAll(depth - 1);
        }
    }
};

/**
 * Collapses this node to hide any child nodes.
 */
RapidContext.Widget.TreeNode.prototype.collapse = function () {
    const container = this._containerNode();
    if (container != null && this.isExpanded()) {
        this.firstChild.childNodes[0].setAttrs({ "class": "fa fa-fw fa-plus-square-o" });
        if (!this.icon) {
            this.firstChild.childNodes[1].setAttrs({ "class": "fa fa-fw fa-folder" });
        }
        container.classList.add("widgetHidden");
        const tree = this.tree();
        if (tree != null) {
            const detail = { tree: tree, node: this };
            tree.emit("collapse", { detail: detail });
        }
    }
};

/**
 * Recursively collapses this node and all its children. If a depth
 * is specified, only children below that depth will be collapsed.
 *
 * @param {number} [depth] the optional minimum depth
 */
RapidContext.Widget.TreeNode.prototype.collapseAll = function (depth) {
    if (typeof(depth) !== "number") {
        depth = 0;
    }
    if (depth <= 0) {
        this.collapse();
    }
    const children = this.getChildNodes();
    for (let i = 0; i < children.length; i++) {
        children[i].collapseAll(depth - 1);
    }
};

/**
 * Toggles expand and collapse for this node.
 */
RapidContext.Widget.TreeNode.prototype.toggle = function () {
    if (this.isExpanded()) {
        this.collapse();
    } else {
        this.expand();
    }
};
