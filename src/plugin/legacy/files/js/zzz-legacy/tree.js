/**
 * Creates a new tree widget.
 *
 * @constructor
 */
function Tree() {
    this.domNode = null;
    this._root = null;
    this._selected = null;
    this._init();
}

/**
 * Internal function to initialize the widget.
 *
 * @private
 */
Tree.prototype._init = function() {
    this._root = new TreeNode("", true);
    this._root.tree = this;
    this.domNode = this._root.childNode;
    this.domNode.className = "tree";
    this.domNode.style.display = "block";
    this.domNode.resizeContent = MochiKit.Base.noop;
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
Tree.prototype.setStyle = function(style) {
    // TODO: move this check to generic UI function
    if ((style.w || style.width) && (style.h || style.height)) {
        if (style.overflow == null) {
            style.overflow = "auto";
        }
    }
    // TODO: move parts of this to generic UI functions
    if (style.w) {
        style.w = style.w + " - 2";
    }
    if (style.width) {
        var value = "" + style.width;
        value = parseInt(value.substring(0, value.length - 2));
        style.width = (value - 2) + "px";
    }
    if (style.h) {
        style.h = style.h + " - 2";
    }
    if (style.height) {
        var value = "" + style.height;
        value = parseInt(value.substring(0, value.length - 2));
        style.height = (value - 2) + "px";
    }
    CssUtil.setStyles(this.domNode, style);
}

/**
 * Searches for a tree node from the specified path.
 *
 * @param path               the tree node path (array of names)
 *
 * @return the descendant tree node found, or
 *         null if not found
 */
Tree.prototype.findByPath = function(path) {
    return this._root.findByPath(path);
}

/**
 * Returns the currently selected node in the tree.
 *
 * @return the currently selected node, or
 *         null if no node is selected
 */
Tree.prototype.getSelectedNode = function() {
    return this._selected;
}

/**
 * Sets the currently selected node in the tree. This can also be
 * done by calling the select() and unselect() methods on the tree
 * nodes themselves.
 *
 * @param node               the selected node, or null for none
 */
Tree.prototype.setSelectedNode = function(node) {
    if (this._selected != node) {
        if (node == null) {
            if (this._selected.isSelected()) {
                this._selected.unselect();
            } else {
                this._selected = null;
                this.onSelect(null);
            }
        } else {
            if (this._selected != null) {
                this._selected.unselect();
            }
            if (!node.isSelected()) {
                node.select();
            } else {
                this._selected = node;
                this.onSelect(node);
            }
        }
    }
}

/**
 * Recursively expands all nodes. If a depth is specified,
 * expansions will not continue below that depth.
 *
 * @param depth              the optional maximum depth
 */
Tree.prototype.expandAll = function(depth) {
    this._root.expandAll(depth);
}

/**
 * Recursively collapses all nodes. If a depth is specified, only
 * nodes below that depth will be collapsed.
 *
 * @param depth              the optional minimum depth
 */
Tree.prototype.collapseAll = function(depth) {
    this._root.collapseAll(depth);
}

/**
 * Adds a node to the root tree node.
 *
 * @param node               the node to add
 */
Tree.prototype.addNode = function(node) {
    return this._root.addNode(node);
}

/**
 * Adds a path to the tree as a recursive list of child nodes. If
 * nodes in the specified path already exists, they will be used
 * instead of creating new nodes.
 *
 * @param path               the tree node path (array of names)
 *
 * @return the last node in the path
 */
Tree.prototype.addPath = function(path) {
    return this._root.addPath(path);
}

/**
 * Removes a node from the tree.
 *
 * @param node               the node to remove
 */
Tree.prototype.removeNode = function(node) {
    if (node.parent != null) {
        node.parent.removeNode(node);
    }
    node.tree = null;
}

/**
 * Clears all nodes in the tree.
 */
Tree.prototype.clear = function() {
    this._selected = null;
    this._root.removeAllChildren();
}

/**
 * The tree selection change signal.
 *
 * @param node               the new node selected, or null for none
 *
 * @signal Emitted when the tree node selection is changed
 */
Tree.prototype.onSelect = function(node) {
}

/**
 * The tree expand or collapse signal.
 *
 * @param node               the node expanded or collapsed
 *
 * @signal Emitted when a tree node is expanded or collapsed
 */
Tree.prototype.onExpand = function(node) {
}


/**
 * Creates a new tree node.
 *
 * @constructor
 * @param name               the tree node name
 * @param folder             the folder flag
 */
function TreeNode(name, folder) {
    this.domNode = null;
    this.imgNode = null;
    this.iconNode = null;
    this.textNode = null;
    this.childNode = null;
    this.name = name;
    this.tree = null;
    this.parent = null;
    this.children = (folder) ? [] : null;
    this.icon = null;
    this._init();
}

/**
 * Internal function to initialize the widget.
 *
 * @private
 */
TreeNode.prototype._init = function() {
    var self = this;

    this.domNode = document.createElement("p");
    this.domNode.onclick = function() {
        self.select();
    }
    if (this.children == null) {
        this.imgNode = Icon.BLANK.createElement();
    } else {
        this.imgNode = Icon.PLUS.createElement();
    }
    this.imgNode.onclick = function(e) {
        self.toggle();
        if (e) {
            e.stopPropagation();
        } else if (window.event) {
            window.event.cancelBubble = true;
        }
    }
    this.domNode.appendChild(this.imgNode);
    this.textNode = document.createElement("span");
    this.textNode.appendChild(document.createTextNode(this.name));
    this.textNode.style.cursor = "pointer";
    this.domNode.appendChild(this.textNode);
    if (this.children != null) {
        this.childNode = document.createElement("div");
        this.childNode.style.display = "none";
    }
}

/**
 * Internal function to destroy the widget and its children.
 *
 * @private
 */
TreeNode.prototype._destroy = function() {
    this.domNode.innerHTML = "";
    if (this.children != null) {
        for (var i = 0; i < this.children.length; i++) {
            this.children[i]._destroy();
        }
        this.childNode.innerHTML = "";
    }
    this.domNode = null;
    this.imgNode = null;
    this.iconNode = null;
    this.textNode = null;
    this.childNode = null;
    this.name = null;
    this.tree = null;
    this.parent = null;
    this.children = null;
    this.icon = null;
}

/**
 * Checks if this node is a folder.
 *
 * @return true if this node is a folder, or
 *         false otherwise
 */
TreeNode.prototype.isFolder = function() {
    return this.children != null;
}

/**
 * Checks if this folder node is expanded.
 *
 * @return true if this node is expanded, or
 *         false otherwise
 */
TreeNode.prototype.isExpanded = function() {
    return this.childNode != null &&
           this.childNode.style.display == "block";
}

/**
 * Checks if this node is selected.
 *
 * @return true if the node is selected, or
 *         false otherwise
 */
TreeNode.prototype.isSelected = function() {
    return this.domNode.className == "selected";
}

/**
 * Changes the name of this node.
 *
 * @param name               the new node name
 */
TreeNode.prototype.setName = function(name) {
    this.name = name;
    this.textNode.innerHTML = "";
    this.textNode.appendChild(document.createTextNode(name));
}

/**
 * Sets the icon for this node.
 *
 * @param icon               the icon to set, or null to remove
 */
TreeNode.prototype.setIcon = function(icon) {
    if (this.iconNode == null && icon != null) {
        this.iconNode = icon.createElement();
        MochiKit.DOM.insertSiblingNodesAfter(this.imgNode, this.iconNode);
    } else if (this.iconNode != null && icon != null) {
        this.iconNode.src = icon.src;
    } else if (this.iconNode != null && icon == null) {
        MochiKit.DOM.removeElement(this.iconNode);
        this.iconNode = null;
    }
}

/**
 * Sets the tree for this node and all child nodes.
 *
 * @param tree               the tree to set, or null to clear
 *
 * @private
 */
TreeNode.prototype._setTree = function(tree) {
    this.tree = tree;
    for (var i = 0; this.children != null && i < this.children.length; i++) {
        this.children[i]._setTree(tree);
    }
}

/**
 * Returns the path to this tree node.
 *
 * @return the tree node path, i.e an array of node names
 */
TreeNode.prototype.getPath = function() {
    var path = (this.parent == null) ? [] : this.parent.getPath();
    if (this.parent != null) {
        path.push(this.name);
    }
    return path;
}

/**
 * Finds a child node with the specified name.
 *
 * @param name               the child node name
 *
 * @return the child tree node found, or
 *         null if not found
 */
TreeNode.prototype.findChild = function(name) {
    for (var i = 0; this.children != null && i < this.children.length; i++) {
        if (this.children[i].name == name) {
            return this.children[i];
        }
    }
    return null;
}

/**
 * Searches for a descendant tree node from the specified path.
 *
 * @param path               the tree node path (array of node names)
 *
 * @return the descendant tree node found, or
 *         null if not found
 */
TreeNode.prototype.findByPath = function(path) {
    var node = this;

    for (var i = 0; node != null && path != null && i < path.length; i++) {
        node = node.findChild(path[i]);
    }
    return node;
}

/**
 * Selects this tree node.
 */
TreeNode.prototype.select = function() {
    if (!this.isSelected()) {
        this.domNode.className = "selected";
        if (this.tree != null) {
            this.tree.setSelectedNode(this);
        }
        this.expand();
    }
}

/**
 * Unselects this tree node.
 */
TreeNode.prototype.unselect = function() {
    if (this.isSelected()) {
        this.domNode.className = "";
        if (this.tree != null) {
            this.tree.setSelectedNode(null);
        }
    }
}

/**
 * Expands this node to display any child nodes. If the parent node
 * is not expanded, it will be expanded as well.
 */
TreeNode.prototype.expand = function() {
    if (this.parent != null && !this.parent.isExpanded()) {
        this.parent.expand();
    }
    if (this.childNode != null && !this.isExpanded()) {
        this.imgNode.src = Icon.MINUS.src;
        this.childNode.style.display = "block";
        if (this.tree != null) {
            this.tree.onExpand(this);
        }
    }
}

/**
 * Recursively expands this node and all its children. If a depth is
 * specified, expansions will not continue below that depth.
 *
 * @param depth              the optional maximum depth
 */
TreeNode.prototype.expandAll = function(depth) {
    if (typeof(depth) !== "number") {
        depth = 10;
    }
    this.expand();
    if (this.children != null && depth > 0) {
        for (var i = 0; i < this.children.length; i++) {
            this.children[i].expandAll(depth - 1);
        }
    }
}

/**
 * Collapses this node to hide any child nodes.
 */
TreeNode.prototype.collapse = function() {
    if (this.childNode != null && this.isExpanded()) {
        this.imgNode.src = Icon.PLUS.src;
        this.childNode.style.display = "none";
        if (this.tree != null) {
            this.tree.onExpand(this);
        }
    }
}

/**
 * Recursively collapses this node and all its children. If a depth
 * is specified, only children below that depth will be collapsed.
 *
 * @param depth              the optional minimum depth
 */
TreeNode.prototype.collapseAll = function(depth) {
    if (typeof(depth) !== "number") {
        depth = 0;
    }
    if (depth <= 0) {
        this.collapse();
    }
    if (this.children != null) {
        for (var i = 0; i < this.children.length; i++) {
            this.children[i].collapseAll(depth - 1);
        }
    }
}

/**
 * Toggles expand and collapse for this node.
 */
TreeNode.prototype.toggle = function() {
    if (this.isExpanded()) {
        this.collapse();
    } else {
        this.expand();
    }
}

/**
 * Adds a node as a child node.
 *
 * @param node               the node to add
 */
TreeNode.prototype.addNode = function(node) {
    if (this.children == null) {
        this.children = [];
        this.childNode = document.createElement("div");
        this.childNode.style.display = "none";
        MochiKit.DOM.insertSiblingNodesAfter(this.domNode, this.childNode);
        this.imgNode.src = Icon.PLUS.src;
    }
    if (node.parent != null) {
        node.parent.removeChild(node);
    }
    this.children.push(node);
    node._setTree(this.tree);
    node.parent = this;
    this.childNode.appendChild(node.domNode);
    if (node.childNode != null) {
        this.childNode.appendChild(node.childNode);
    }
}

/**
 * Adds a path as a recursive list of child nodes. If nodes in the
 * specified path already exists, they will be used instead of
 * creating new nodes.
 *
 * @param path               the tree node path (array of node names)
 *
 * @return the last node in the path
 */
TreeNode.prototype.addPath = function(path) {
    var node = this;
    var child;

    for (var i = 0; path != null && i < path.length; i++) {
        child = node.findChild(path[i]);
        if (child == null) {
            child = new TreeNode(path[i], false);
            node.addNode(child);
        }
        node = child;
    }
    return node;
}

/**
 * Removes a node as a child node.
 *
 * @param node               the node to remove
 */
TreeNode.prototype.removeChild = function(node) {
    ArrayUtil.removeElem(this.children, node);
    MochiKit.DOM.removeElement(node.domNode);
    MochiKit.DOM.removeElement(node.childNode);
    node._setTree(null);
    node.parent = null;
}

/**
 * Removes all children and destroys them.
 */
TreeNode.prototype.removeAllChildren = function() {
    if (this.children != null) {
        for (var i = 0; i < this.children.length; i++) {
            this.children[i]._destroy();
        }
        this.children = [];
        this.childNode.innerHTML = "";
    }
}
