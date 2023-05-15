/**
 * Creates a new code view widget.
 *
 * @constructor
 */
function CodeView() {
    this.domNode = null;
    this._init();
}

/**
 * Internal function to initialize the widget.
 *
 * @private
 */
CodeView.prototype._init = function() {
    this.domNode = document.createElement("pre");
}

/**
 * Sets the code view text.
 *
 * @param text               the text string
 */
CodeView.prototype.setText = function(text) {
    this.domNode.innerHTML = "";
    this.addText(text);
}

/**
 * Adds text to the code view.
 *
 * @param text               the text string
 * @param style              the optional CSS styling
 */
CodeView.prototype.addText = function(text, style) {
    if (text != null && text != "") {
        // TODO: Remove this IE bugfix for setting visible newlines...
        if (StringUtil.contains(navigator.userAgent, "MSIE")) {
            text = text.replace("\n", "\r");
        }
        if (style) {
            var span = document.createElement("span");
            for (var name in style) {
                span.style[name] = style[name];
            }
            span.appendChild(document.createTextNode(text));
            this.domNode.appendChild(span);
        } else {
            this.domNode.appendChild(document.createTextNode(text));
        }
    }
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
CodeView.prototype.setStyle = function(style) {
    CssUtil.setStyles(this.domNode, style);
}
