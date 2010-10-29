/**
 * Creates a new editable password field.
 *
 * @constructor
 * @param props              the optional widget properties
 */
function PasswordField(props) {
    this.domNode = null;
    this.domNode = document.createElement("input");
    this.domNode.type = "password";
    this.domNode.autocomplete = "off";
}

/**
 * Returns the password field text.
 *
 * @return the password field text
 */
PasswordField.prototype.getText = function() {
    return this.domNode.value;
}

/**
 * Sets the password field text.
 *
 * @param text               the password text
 */
PasswordField.prototype.setText = function(text) {
    this.domNode.value = text;
}

/**
 * Sets the password field length in characters. Note that this does
 * not actually limit the field content length, but only controls
 * the field appearence.
 *
 * @param length             the number of positions to display
 */
PasswordField.prototype.setLength = function(length) {
    this.domNode.size = length;
}

/**
 * Enables or disables the password field.
 *
 * @param disabled           the disabled boolean flag
 */
PasswordField.prototype.setDisabled = function(disabled) {
    this.domNode.disabled = disabled;
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
PasswordField.prototype.setStyle = function(style) {
    CssUtil.setStyles(this.domNode, style);
}

// Register function names
ReTracer.Util.registerFunctionNames(PasswordField.prototype, "PasswordField");
