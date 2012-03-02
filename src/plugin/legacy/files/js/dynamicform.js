/**
 * Creates a new dynamic form. This widget is composed from a
 * title pane and several form widgets, and simplifies much of the
 * logic for handling forms.
 *
 * @constructor
 * @param title              the title to use
 */
function DynamicForm(title) {
    this.domNode = null;
    this._pane = null;
    this._grid = null;
    this._pos = { x: 0, y: 0 };
    this._fields = {};
    this._init(title);
}

/**
 * Internal function to initialize the widget.
 *
 * @param title              the title to use
 *
 * @private
 */
DynamicForm.prototype._init = function(title) {
    this._pane = new TitlePane(title);
    this._grid = new GridPane();
    this._pane.addChild(this._grid);
    this.domNode = this._pane.domNode;
}

/**
 * Sets the widget title.
 *
 * @param title              the new title
 */
DynamicForm.prototype.setTitle = function(title) {
    this._pane.setTitle(title);
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
DynamicForm.prototype.setStyle = function(style) {
    this._pane.setStyle(style);
}

/**
 * Shows an icon and optionally sets a click handler for the icon.
 *
 * @param icon               the icon to show
 * @param method             the callback method, or null for unchanged
 * @param obj                the callback object, or null for none
 */
DynamicForm.prototype.showIcon = function(icon, method, obj) {
    this._pane.loadIcon(icon);
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
DynamicForm.prototype.hideIcon = function(icon, method, obj) {
    this._pane.loadIcon(icon);
    this._pane.hideIcon(icon);
    if (method != null) {
        this._pane.registerOnClick(icon, method, obj);
    }
}

/**
 * Adds a line break to the form.
 */
DynamicForm.prototype.addBreak = function() {
    if (this._pos.x > 0) {
        this._pos.x = 0;
        this._pos.y++;
    }
}

/**
 * Adds a label to the form.
 *
 * @param text               the label text
 *
 * @return the label widget created
 */
DynamicForm.prototype.addLabel = function(text) {
    var label = new Label(text.replace(" ", "\u00A0") + ":");
    label.setStyle({ "class": "label" });
    this.addField(null, label, { paddingRight: "4px", verticalAlign: "middle" });
    return label;
}

/**
 * Adds a text label to the form.
 *
 * @param name               the unique field name
 * @param text               the default text, or null for blank
 *
 * @return the text label widget created
 */
DynamicForm.prototype.addTextLabel = function(name, text) {
    var label = new Label(text);
    label.setStyle({ "class": "value" });
    this.addField(name, label, { paddingRight: "10px", verticalAlign: "middle" });
    return label;
}

/**
 * Adds a text field to the form.
 *
 * @param name               the unique field name
 * @param text               the default text, or null for blank
 *
 * @return the text field widget created
 */
DynamicForm.prototype.addTextField = function(name, text) {
    var field = new TextField();
    if (text != null) {
        field.setText(text);
    }
    this.addField(name, field, { paddingRight: "10px" });
    return field;
}

/**
 * Adds a text box to the form.
 *
 * @param name               the unique field name
 * @param text               the default text, or null for blank
 *
 * @return the text box widget created
 */
DynamicForm.prototype.addTextBox = function(name, text) {
    var field = new TextBox();
    if (text != null) {
        field.setText(text);
    }
    this.addField(name, field, { paddingRight: "10px" });
    return field;
}

/**
 * Adds a compact text box to the form.
 *
 * @param name               the unique field name
 * @param text               the default text, or null for blank
 *
 * @return the compact text box widget created
 */
DynamicForm.prototype.addCompactTextBox = function(name, text) {
    var field = new CompactTextBox();
    if (text != null) {
        field.setText(text);
    }
    this.addField(name, field, { paddingRight: "10px" });
    return field;
}

/**
 * Adds a drop down to the form.
 *
 * @param name               the unique field name
 *
 * @return the drop down widget created
 */
DynamicForm.prototype.addDropDown = function(name) {
    var box = new DropDownBox();
    this.addField(name, box, { paddingRight: "10px" });
    return box;
}

/**
 * Adds a button to the form.
 *
 * @param text               the button text
 *
 * @return the button widget created
 */
DynamicForm.prototype.addButton = function(text) {
    var button = new Button(text);
    this.addField(null, button, { paddingRight: "10px" });
    return button;
}

/**
 * Adds an icon or image to the form.
 *
 * @param icon               the icon or the image URL
 *
 * @return the button widget created
 */
DynamicForm.prototype.addIcon = function(icon) {
    var elem;
    if (icon instanceof Icon) {
        elem = icon.createElement()
    } else {
        elem = document.createElement("img");
        elem.src = icon;
    }
    this.addField(null, elem, { paddingRight: "10px", verticalAlign: "middle" });
    return elem;
}

/**
 * Adds a generic widget to the form.
 *
 * @param name               the unique field name
 * @param widget             the widget to add
 * @param style              the optional CSS style object, or null
 */
DynamicForm.prototype.addField = function(name, widget, style) {
    var cell = this._grid.getCell(this._pos.x++, this._pos.y)
    cell.setStyle(style);
    cell.addChild(widget);
    if (name != null) {
        this._fields[name] = widget;
    }
    return cell;
}

/**
 * Removes all the form fields.
 */
DynamicForm.prototype.removeAll = function() {
    this._fields = {};
    this._pos = { x: 0, y: 0 };
    while (this._grid.getHeight() > 0) {
        this._grid.removeRow(this._grid.getHeight() - 1);
    }
}

/**
 * Adds a field validator function. The validator function is called
 * with the field (widget) object whenever the field value is
 * visited or upon form validation. The function should return null
 * if validation was ok, or a short error message on failure.
 * 
 * @param name               the field name
 * @param validator          the validator function
 */
DynamicForm.prototype.addValidator = function(name, validator) {
    var field;

    if (this._fields[name] != null) {
        field = this._fields[name];
        if (field.data == null) {
            field.data = {};
        }
        field.data.validator = validator;
        if (field.onFocus != null) {
            Signal.connect(field, "onFocus",
                           FunctionUtil.bind("_validateReset", this, field));
            Signal.connect(field, "onBlur",
                           FunctionUtil.bind("_validateField", this, field));
        }
    }
}

/**
 * Clears all the form fields.
 */
DynamicForm.prototype.clear = function() {
    var values = this.getValues();
    for (var name in values) {
        values[name] = (StringUtil.endsWith(name, ".data")) ? null : "";
    }
    this.setValues(values);
    for (var name in this._fields) {
        this._validateReset(this._fields[name]);
    }
}

/**
 * Returns the currently form field values. The returned object
 * contains the field names associated with the values found.
 *
 * @return the field value object
 */
DynamicForm.prototype.getValues = function() {
    var res = {};
    var field;

    for (var name in this._fields) {
        field = this._fields[name];
        if (field.getText) {
            res[name] = MochiKit.Format.strip(field.getText());
            field.setText(res[name]);
        } else if (field.getOption) {
            res[name] = field.getOption();
        } else if (field.isChecked) {
            res[name] = field.isChecked();
        } else {
            res[name] = null;
        }
        if (field.data != null) {
            res[name + ".data"] = field.data;
        }
    }
    return res;
}

/**
 * Updates the form field values.
 *
 * @param values             the field value object
 */
DynamicForm.prototype.setValues = function(values) {
    var field;
    var value;

    for (var name in values) {
        field = this._fields[name];
        value = values[name] || "";
        if (typeof(value) != "string") {
            value = "" + value;
        }
        if (field != null && field.setText) {
            field.setText(value);
        } else if (field != null && field.setOption) {
            field.setOption(value);
        } else if (field != null && field.setChecked) {
            field.setChecked(value != "" && value != "false" &&
                             value != "0" && value != "null");
        }
        if (field != null && (name + ".data") in values) {
            value = values[name + ".data"] || {};
            if (field.data && field.data.validator) {
                value.validator = field.data.validator;
            }
            field.data = value;
        }
    }
}

/**
 * Returns a field widget.
 *
 * @param name               the field name
 *
 * @return the field widget, or
 *         null if not found
 */
DynamicForm.prototype.getField = function(name) {
    return this._fields[name];
}

/**
 * Updates the CSS styles for a field widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param name               the field name
 * @param style              the style object
 */
DynamicForm.prototype.setFieldStyle = function(name, style) {
    var widget = this._fields[name];
    if (widget != null && widget.domNode != null) {
        CssUtil.setStyles(widget.domNode.parentNode, style);
    }
}

/**
 * Validates the all fields in the form.
 *
 * @return true if all fields validated correctly, or
 *         false otherwise
 */
DynamicForm.prototype.validate = function() {
    var res = true;
    var field;

    for (var name in this._fields) {
        field = this._fields[name];
        if (field.data != null && field.data.validator != null) {
            res = this._validateField(field) && res;
        }
    }
    return res;
}

/**
 * Resets a field validation. This method is called when a field
 * regains focus.
 *
 * @param widget             the field widget
 *
 * @private
 */
DynamicForm.prototype._validateReset = function(widget) {
    widget.setStyle({ background: "white" });
    if (widget.setDisplayText != null) {
        widget.setDisplayText(null);
    }
}

/**
 * Validates a field. This method is called when a field loses
 * focus.
 *
 * @param widget             the field widget
 *
 * @return true if the field validated correctly, or
 *         false otherwise
 *
 * @private
 */
DynamicForm.prototype._validateField = function(widget) {
    var msg = widget.data.validator(widget);

    if (msg == "") {
        widget.setStyle({ background: "#ffffcc" });
        return false;
    } else if (msg != null) {
        widget.setStyle({ background: "#ffcccc" });
        if (widget.setDisplayText != null) {
            widget.setDisplayText(msg);
        }
        return false;
    }
    return true;
}

// Register function names
ReTracer.Util.registerFunctionNames(DynamicForm.prototype, "DynamicForm");
