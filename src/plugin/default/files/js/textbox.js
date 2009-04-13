/**
 * Creates a new editable textbox.
 *
 * @constructor
 */
function TextBox() {
    this.domNode = null;
    this._init();
}

/**
 * Internal function to initialize the widget.
 *
 * @private
 */
TextBox.prototype._init = function() {
    this.domNode = document.createElement("textarea");
}

/**
 * Returns the textbox text.
 *
 * @return the textbox text
 */
TextBox.prototype.getText = function() {
    return this.domNode.value;
}

/**
 * Sets the textbox text.
 *
 * @param text               the textbox text
 */
TextBox.prototype.setText = function(text) {
    text = text || "";
    if (typeof(text) != "string") {
        text = "" + text;
    }
    this.domNode.value = text;
}

/**
 * Sets the text wrapping in the text box.
 *
 * @param wrap               the wrap flag
 */
TextBox.prototype.setWrap = function(wrap) {
    this.domNode.setAttribute("wrap", wrap ? "SOFT" : "OFF");
}

/**
 * Enables or disables the text box.
 *
 * @param disabled           the disabled boolean flag
 */
TextBox.prototype.setDisabled = function(disabled) {
    this.domNode.disabled = disabled;
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
TextBox.prototype.setStyle = function(style) {
    CssUtil.setStyles(this.domNode, style);
}

/**
 * Returns the current textbox size. The size object returned will
 * contain the "rows" and "cols" properties for the number of
 * characters in the textbox size.
 *
 * @return the current textbox size object
 */
TextBox.prototype.getSize = function() {
    return { rows: this.domNode.rows, cols: this.domNode.cols };
}

/**
 * Resizes the textbox to the specified size.
 *
 * @param rows            the number of rows
 * @param cols            the number of columns
 */
TextBox.prototype.resizeTo = function(rows, cols) {
    this.domNode.rows = Math.max(rows, 1);
    this.domNode.cols = Math.max(cols, 1);
}

/**
 * Resizes the textbox to fit the current text content.
 *
 * @param minRows            the minimum number of rows
 * @param minCols            the minimum number of columns
 */
TextBox.prototype.resizeFromText = function(minRows, minCols) {
    var  text = this.getText();
    var  rows = 1;
    var  cols = 1;
    var  pos;

    while ((pos = text.indexOf("\n")) >= 0) {
        rows++;
        cols = Math.max(cols, pos + 1);
        text = text.substring(pos + 1);
    }
    cols = Math.max(cols, text.length);
    if (minRows) {
        rows = Math.max(rows, minRows);
    }
    if (minCols) {
        cols = Math.max(cols, minCols);
    }
    this.resizeTo(rows, cols);
}

// Register function names
ReTracer.Util.registerFunctionNames(TextBox.prototype, "TextBox");
