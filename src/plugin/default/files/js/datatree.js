/**
 * Creates a new data tree. This widget is a special type of tree
 * that allows simplified visualization of generic data objects.
 *
 * @constructor
 */
function DataTree() {
    this.domNode = null;
    this._tree = null;
    this._init();
}

/**
 * Internal function to initialize the widget.
 *
 * @private
 */
DataTree.prototype._init = function() {
    // TODO: Replace tree widget with custom code in this class,
    //       since we will normally span several lines
    this._tree = new Tree();
    this.domNode = this._tree.domNode;
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
DataTree.prototype.setStyle = function(style) {
    this._tree.setStyle(style);
}

/**
 * Clears the data from the tree.
 */
DataTree.prototype.clear = function() {
    this._tree.clear();
}

/**
 * Displays the specified data.
 *
 * @param data               the data to display
 */
DataTree.prototype.setData = function(data) {
    this._tree.clear();
    if (data instanceof Array) {
        for (var i = 0; i < data.length; i++) {
            this._addData(null, "" + i, data[i]);
        }
    } else if (typeof(data) == "number" ||
               typeof(data) == "boolean" ||
               typeof(data) == "string") {
        label = this._createLabel(null, data);
        this._tree.addNode(new TreeNode(label, false));
    } else if (data == null) {
        label = this._createLabel(null, data);
        this._tree.addNode(new TreeNode(label, false));
    } else {
        for (var name in data) {
            this._addData(null, name, data[name]);
        }
    }
}

DataTree.prototype._addData = function(parent, name, data) {
    var node;
    var label;

    // TODO: aggregate simple string fields to a single label with
    //       formatting for labels
    if (data instanceof Array) {
        label = this._createLabel(name, "[Array]");
        node = new TreeNode(label, true);
        for (var i = 0; i < data.length; i++) {
            this._addData(node, "" + i, data[i]);
        }
    } else if (typeof(data) == "number" ||
               typeof(data) == "boolean" ||
               typeof(data) == "string") {
        label = this._createLabel(name, data);
        node = new TreeNode(label, false);
    } else if (data == null) {
        label = this._createLabel(name, data);
        node = new TreeNode(label, false);
    } else {
        label = this._createLabel(name, "[Object]");
        node = new TreeNode(label, true);
        for (var name in data) {
            this._addData(node, name, data[name]);
        }
    }
    if (parent == null) {
        this._tree.addNode(node);
    } else {
        parent.addNode(node);
    }
}

DataTree.prototype._createLabel = function(name, value) {
    var str = "";

    if (name != null) {
        str += name + ": ";
    }
    if (value == null) {
        str += "<null>";
    } else {
        // TODO: provide dialog for long responses
        str += ReTracer.Util.truncate(value, 30, "...");
    }
    return str;
}

// Register function names
ReTracer.Util.registerFunctionNames(DataTree.prototype, "DataTree");
