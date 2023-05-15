/**
 * Creates a new radio button group. By default horizontal alignment
 * is used.
 *
 * @constructor
 * @param name               the optional form name (null for any)
 * @param vertical           the vertical alignment flag
 */
function RadioGroup(name, vertical) {
    this.domNode = null;
    if (name == null) {
        name = "radiogroup." + RadioGroup._COUNTER++;
    }
    this._name = name;
    this._vertical = (vertical == true);
    this._init();
}

/**
 * Internal function to initialize the widget.
 *
 * @private
 */
RadioGroup.prototype._init = function() {
    this.domNode = document.createElement("div");
    if (this._vertical) {
        this.setStyle({ lineHeight: "1.8em" });
    }
}

/**
 * Adds a new option to the radio button group.
 *
 * @param value              the option value
 * @param text               the option text
 */
RadioGroup.prototype.addOption = function(value, text) {
    var input;
    var span;

    input = document.createElement("input");
    input.type = "radio";
    input.name = this._name;
    input.value = value;
    input.style.verticalAlign = "-25%";
    input.onclick = FunctionUtil.bind("onChange", this);
    this.domNode.appendChild(input);
    span = document.createElement("span");
    CssUtil.setStyles(span, { "class": "label", cursor: "default" });
    span.onclick = FunctionUtil.bind("setOption", this, value);
    span.appendChild(document.createTextNode(text));
    this.domNode.appendChild(span);
    if (this._vertical) {
        this.domNode.appendChild(document.createElement("br"));
    } else {
        this.domNode.appendChild(document.createTextNode(" "));
    }
}

/**
 * Removes all options in the radio button group.
 */
RadioGroup.prototype.removeAllOptions = function() {
    this.domNode.innerHTML = "";
}

/**
 * Returns the currently selected option value.
 *
 * @return the currently selected option value, or
 *         null for none
 */
RadioGroup.prototype.getOption = function() {
    var nodes = this.domNode.getElementsByTagName("INPUT");

    for (var i = 0; i < nodes.length; i++) {
        if (nodes[i].checked) {
            return nodes[i].value;
        }
    }
    return null;
}

/**
 * Changes the currently selected option value.
 *
 * @param value              the new option value
 */
RadioGroup.prototype.setOption = function(value) {
    var nodes = this.domNode.getElementsByTagName("INPUT");
    var changed = false;

    for (var i = 0; i < nodes.length; i++) {
        if (nodes[i].value == value) {
            changed = !nodes[i].checked;
            nodes[i].checked = true;
            nodes[i].defaultChecked = true;
        } else {
            nodes[i].checked = false;
            nodes[i].defaultChecked = false;
        }
    }
    if (changed) {
        this.onChange();
    }
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
RadioGroup.prototype.setStyle = function(style) {
    CssUtil.setStyles(this.domNode, style);
}

/**
 * The value change signal.
 *
 * @signal Emitted when the value is changed
 */
RadioGroup.prototype.onChange = function() {
}

/**
 * Registers a value change handler.
 *
 * @param method             the callback method
 * @param obj                the callback object, or null
 *
 * @deprecated Use Signal.connect(widget, "onChange", obj, method) instead
 */
RadioGroup.prototype.registerOnChange = function(method, obj) {
    Signal.connect(this, "onChange", FunctionUtil.bind(method, obj, this));
}

/**
 * The anonymous name radio group counter.
 *
 * @memberOf RadioGroup
 * @private
 */
RadioGroup._COUNTER = 0;
