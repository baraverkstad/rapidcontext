/**
 * Creates a new checkbox widget.
 *
 * @constructor
 * @param props              the optional widget properties
 */
function CheckBox(props) {
    props = props || {};
    this.domNode = document.createElement("div");
    this._inputNode = document.createElement("input");
    this._inputNode.type = "checkbox";
    this._inputNode.style.verticalAlign = "-40%";
    this._inputNode.onclick = FunctionUtil.bind("onChange", this);
    this.domNode.appendChild(this._inputNode);
    this._labelNode = document.createElement("span");
    CssUtil.setStyles(this._labelNode, { "class": "label", cursor: "default" });
    this._labelNode.onclick = FunctionUtil.bind("toggleChecked", this);
    this.setLabel(props.label || "");
    this.domNode.appendChild(this._labelNode);
    this.setChecked(props.checked || false);
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
CheckBox.prototype.setStyle = function(style) {
    CssUtil.setStyles(this.domNode, style);
}

/**
 * Returns the label text.
 *
 * @return the label text
 */
CheckBox.prototype.getLabel = function() {
    return this._labelNode.textContent;
}

/**
 * Sets the label text.
 *
 * @param text               the label text
 */
CheckBox.prototype.setLabel = function(text) {
    this._labelNode.innerHTML = "";
    this._labelNode.appendChild(document.createTextNode(text));
}

/**
 * Checks if the checkbox is checked.
 *
 * @return true if the checkbox is checked, or
 *         false otherwise
 */
CheckBox.prototype.isChecked = function() {
    return this._inputNode.checked;
}

/**
 * Sets the checkbox value.
 *
 * @param checked            the checked flag
 */
CheckBox.prototype.setChecked = function(checked) {
    var old = this.isChecked();
    this._inputNode.checked = checked;
    if (old != checked) {
        this.onChange();
    }
    return checked;
}

/**
 * Toggles the checkbox value.
 *
 * @return true if the checkbox is checked, or
 *         false otherwise
 */
CheckBox.prototype.toggleChecked = function() {
    return this.setChecked(!this.isChecked());
}

/**
 * The value change signal.
 *
 * @signal Emitted when the value is changed
 */
CheckBox.prototype.onChange = function() {
}
