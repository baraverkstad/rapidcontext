/**
 * Creates a new dynamic tree. This widget is composed from a
 * title pane and a tree, and simplifies much of the logic
 * for loading data into the tree.
 *
 * @constructor
 * @param title              the title to use
 * @param field              the path field name
 * @param separator          the path field separator
 * @param procedure          the procedure for loading data, or null
 */
function DynamicTree(title, field, separator, procedure) {
    this.domNode = null;
    this._pane = null;
    this._tree = null;
    this._field = field;
    this._separator = separator;
    this._procedure = procedure;
    this._arguments = [];
    this._mappers = [];
    this._reloadPath = null;
    this._init(title);
}

/**
 * Internal function to initialize the widget.
 *
 * @param title              the title to use
 *
 * @private
 */
DynamicTree.prototype._init = function(title) {
    this._pane = new TitlePane(title);
    this._pane.setStyle({ overflow: "hidden" })
    this._pane.loadIcon(Icon.RELOAD);
    this._pane.loadIcon(Icon.LOADING);
    this._pane.registerOnClick(Icon.RELOAD, this.reload, this);
    this.domNode = this._pane.domNode;
    this._tree = new Tree();
    this._tree.setStyle({ w: "100%", h: "100%" });
    Signal.connect(this._tree, "onSelect", this, "onSelect");
    this._pane.addChild(this._tree);
}

/**
 * Sets the widget title.
 *
 * @param title              the new title
 */
DynamicTree.prototype.setTitle = function(title) {
    this._pane.setTitle(title);
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
DynamicTree.prototype.setStyle = function(style) {
    this._pane.setStyle(style);
}

/**
 * Shows an icon and optionally sets a click handler for the icon.
 *
 * @param icon               the icon to show
 * @param method             the callback method, or null for unchanged
 * @param obj                the callback object, or null for none
 */
DynamicTree.prototype.showIcon = function(icon, method, obj) {
    this._pane.loadIcon(icon, -2);
    this._pane.showIcon(icon);
    if (method != null) {
        this._pane.registerOnClick(icon, method, obj);
    }
}

/**
 * Hides an icon and optionally sets a click handler for the icon.
 * This method can also be called to load an icon.
 *
 * @param icon               the icon to hide
 * @param method             the callback method, or null for unchanged
 * @param obj                the callback object, or null for none
 */
DynamicTree.prototype.hideIcon = function(icon, method, obj) {
    this._pane.loadIcon(icon, -2);
    this._pane.hideIcon(icon);
    if (method != null) {
        this._pane.registerOnClick(icon, method, obj);
    }
}

/**
 * Clears the data from the tree.
 */
DynamicTree.prototype.clear = function() {
    this._pane.hideIcon(Icon.RELOAD);
    this._pane.hideIcon(Icon.LOADING);
    this._tree.clear();
    this.onClear();
}

/**
 * Loads data into the table. The arguments sent to this method will
 * be passed directly on to the call to the load procedure.
 */
DynamicTree.prototype.load = function() {
    this._arguments = arguments;
    this.reload();
}

/**
 * Reloads the data in the table. The arguments used in the last call
 * to load() will be used when calling the load procedure.
 */
DynamicTree.prototype.reload = function() {
    if (this._procedure == null) {
        alert("Error: no procedure set for loading data into table");
        return;
    }
    var node = this.getSelectedNode();
    this._reloadPath = (node == null) ? null : node.getPath();
    this.clear();
    this._pane.showIcon(Icon.LOADING);
    retracer.call(this._procedure, this._arguments, this._callbackLoad, this);
}

/**
 * Callback function for the load method.
 *
 * @param data               the data loaded
 * @param error              the optional error, or null
 *
 * @private
 */
DynamicTree.prototype._callbackLoad = function(data, error) {
    this._pane.showIcon(Icon.RELOAD);
    this._pane.hideIcon(Icon.LOADING);
    if (error != null) {
        alert("Error: " + error);
    } else {
        this.setData(data);
        if (this._reloadPath != null) {
            var node = this._tree.findByPath(this._reloadPath);
            if (node != null) {
                node.expand();
                node.select();
            }
        }
    }
}

/**
 * Displays the specified data.
 *
 * @param data               the data to display
 */
DynamicTree.prototype.setData = function(data) {
    var node;

    if (data != null && data.rows != null && data.rows.length != null) {
        data = data.rows;
    }
    this._transformData(data);
    for (var i = 0; data != null && i < data.length; i++) {
        node = this._tree.addPath(data[i][this._field].split(this._separator));
        node.data = data[i];
        node.setIcon(Icon.DOCUMENT);
        while ((node = node.parent) != null) {
            node.setIcon(Icon.FOLDER);
        }
    }
}

/**
 * Transforms the returned data with the data mappers. Also sort the
 * list by the path field.
 *
 * @param data               the data loaded
 *
 * @private
 */
DynamicTree.prototype._transformData = function(data) {
    if (data != null && data.length != null) {
        for (var i = 0; i < data.length; i++) {
            for (var j = 0; j < this._mappers.length; j++) {
                try {
                    this._mappers[j](data[i]);
                } catch (e) {
                    // Ignore mapping errors
                }
            }
        }
        ArrayUtil.sort(data, this._field);
    }
}

/**
 * Searches for a tree node from the specified path string.
 *
 * @param path               the tree node path string
 *
 * @return the descendant tree node found, or
 *         null if not found
 */
DynamicTree.prototype.findByPath = function(path) {
    return this._tree.findByPath(path.split(this._separator));
}

/**
 * Returns the currently selected node in the tree. Note that the
 * tree node may be an intermediate folder node not containing any
 * data.
 *
 * @return the currently selected node, or
 *         null if no node is selected
 */
DynamicTree.prototype.getSelectedNode = function() {
    return this._tree.getSelectedNode();
}

/**
 * Returns the currently selected row data.
 *
 * @return the data row selected, or
 *         an array of selected data rows if multiple selection is enabled
 */
DynamicTree.prototype.getSelectedData = function() {
    var node = this._tree.getSelectedNode();
    return (node == null || node.data == null) ? null : node.data;
}

/**
 * Recursively expands all nodes. If a depth is specified,
 * expansions will not continue below that depth.
 *
 * @param depth              the optional maximum depth
 */
DynamicTree.prototype.expandAll = function(depth) {
    this._tree.expandAll(depth);
}

/**
 * Recursively collapses all nodes. If a depth is specified, only
 * nodes below that depth will be collapsed.
 *
 * @param depth              the optional minimum depth
 */
DynamicTree.prototype.collapseAll = function(depth) {
    this._tree.collapseAll(depth);
}

/**
 * The tree clear signal.
 *
 * @signal Emitted when the tree is cleared
 */
DynamicTree.prototype.onClear = function() {
}

/**
 * The tree selection change signal.
 *
 * @param node               the new node selected, or null for none
 *
 * @signal Emitted when the tree selection is changed
 */
DynamicTree.prototype.onSelect = function() {
}

// Register function names
ReTracer.Util.registerFunctionNames(DynamicTree.prototype, "DynamicTree");
