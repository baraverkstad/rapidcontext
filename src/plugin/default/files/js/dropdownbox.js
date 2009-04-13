/**
 * Creates a new dropdown box.
 *
 * @constructor
 */
function DropDownBox() {
    this.domNode = null;
    this._init();
}

/**
 * Internal function to initialize the widget.
 *
 * @private
 */
DropDownBox.prototype._init = function() {
    this.domNode = document.createElement("select");
    this.domNode.onchange = FunctionUtil.bind("onChange", this);
}

/**
 * Adds a new option to the dropdown box.
 *
 * @param value              the option value
 * @param text               the option text
 */
DropDownBox.prototype.addOption = function(value, text) {
    var node = document.createElement("option");

    node.value = value;
    node.appendChild(document.createTextNode(text));
    this.domNode.appendChild(node);
}

/**
 * Removes all options in the dropdown box.
 */
DropDownBox.prototype.removeAllOptions = function() {
    this.domNode.innerHTML = "";
}

/**
 * Returns the currently selected option value.
 *
 * @return the currently selected option value, or
 *         null if no options are available
 */
DropDownBox.prototype.getOption = function() {
    if (this.domNode.selectedIndex >= 0) {
        return this.domNode.options[this.domNode.selectedIndex].value;
    } else {
        return null;
    }
}

/**
 * Changes the currently selected option value.
 *
 * @param value              the new option value
 */
DropDownBox.prototype.setOption = function(value) {
    this.domNode.value = value;
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
DropDownBox.prototype.setStyle = function(style) {
    CssUtil.setStyles(this.domNode, style);
}

/**
 * The dropdown value change signal.
 *
 * @signal Emitted when the dropdown value is changed
 */
DropDownBox.prototype.onChange = function() {
}

/**
 * Registers a value change handler.
 *
 * @param method             the callback method
 * @param obj                the callback object, or null
 *
 * @deprecated Use Signal.connect(widget, "onChange", obj, method) instead
 */
DropDownBox.prototype.registerOnChange = function(method, obj) {
    Signal.connect(this, "onChange", FunctionUtil.bind(method, obj, this));
}

// Register function names
ReTracer.Util.registerFunctionNames(DropDownBox.prototype, "DropDownBox");
