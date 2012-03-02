/**
 * Creates a new button.
 *
 * @constructor
 */
function Button(text) {
    this.domNode = null;
    this._init(text);
    this.setText(text);
}

/**
 * Internal function to initialize the widget.
 *
 * @private
 */
Button.prototype._init = function() {
    this.domNode = document.createElement("button");
    this.domNode.onclick = FunctionUtil.bind("onClick", this);
}

/**
 * Sets the button text.
 *
 * @param text               the new button text
 */
Button.prototype.setText = function(text) {
    this.domNode.innerHTML = "";
    this.domNode.appendChild(document.createTextNode(text));
}

/**
 * Enables or disables the button.
 *
 * @param disabled           the disabled boolean flag
 */
Button.prototype.setDisabled = function(disabled) {
    this.domNode.disabled = disabled;
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
Button.prototype.setStyle = function(style) {
    CssUtil.setStyles(this.domNode, style);
}

/**
 * The button click signal.
 *
 * @signal Emitted when the button is clicked
 */
Button.prototype.onClick = function() {
    return false;
}

// Register function names
ReTracer.Util.registerFunctionNames(Button.prototype, "Button");
