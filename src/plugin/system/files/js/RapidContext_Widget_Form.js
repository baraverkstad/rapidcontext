/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2022 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE for more details.
 */

// Namespace initialization
if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}
RapidContext.Widget = RapidContext.Widget || { Classes: {}};

/**
 * Creates a new form widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {Boolean} [attrs.hidden] the hidden widget flag, defaults to false
 * @param {Object} [...] the child widgets or DOM nodes
 *
 * @return {Widget} the widget DOM node
 *
 * @class The form widget class. Provides a grouping for form fields, using the
 *     `<form>` HTML element. The form widget supports form reset, validation
 *     and data retrieval.
 * @extends RapidContext.Widget
 *
 * @example {JavaScript}
 * var field = RapidContext.Widget.TextField({ name: "name", helpText: "Your Name Here" });
 * var attrs = { name: "name", message: "Please enter your name to proceed." };
 * var valid = RapidContext.Widget.FormValidator(attrs);
 * var exampleForm = RapidContext.Widget.Form({}, field, valid);
 *
 * @example {User Interface XML}
 * <Form id="exampleForm">
 *   <TextField name="name" helpText="Your Name Here" />
 *   <FormValidator name="name" message="Please enter your name to proceed." />
 * </Form>
 */
RapidContext.Widget.Form = function (attrs/*, ...*/) {
    var o = MochiKit.DOM.FORM(attrs);
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetForm");
    o.setAttrs(attrs);
    o.addAll(MochiKit.Base.extend(null, arguments, 1));
    o.onsubmit = RapidContext.Widget._eventHandler(null, "_handleSubmit");
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Form = RapidContext.Widget.Form;

/**
 * Returns an array with all child DOM nodes containing form fields.
 * The child nodes will be returned based on the results of the
 * `RapidContext.Widget.isFormField()` function.
 *
 * @return {Array} the array of form field elements
 */
RapidContext.Widget.Form.prototype.fields = function () {
    var fields = [];
    MochiKit.Base.nodeWalk(this, function (elem) {
        if (elem.nodeType !== 1) { // !Node.ELEMENT_NODE
            return null;
        }
        if (RapidContext.Widget.isFormField(elem)) {
            fields.push(elem);
            return null;
        } else {
            return elem.childNodes;
        }
    });
    return fields;
};

/**
 * Returns a map with all child DOM nodes containing form fields with
 * a name attribute. If multiple fields have the same name, the
 * returned map will contain an array with all matching fields.
 *
 * @return {Object} the map of form field elements
 */
RapidContext.Widget.Form.prototype.fieldMap = function () {
    var fields = this.fields();
    var map = {};
    for (var i = 0; i < fields.length; i++) {
        var name = fields[i].name;
        if (typeof(name) == "string" && name != "*") {
            if (map[name] instanceof Array) {
                map[name].push(fields[i]);
            } else if (map[name] != null) {
                map[name] = [map[name], fields[i]];
            } else {
                map[name] = fields[i];
            }
        }
    }
    return map;
};

/**
 * Resets all fields in the form to their default values.
 */
// TODO: Consider renaming this method, since it collides with the reset()
//       method on the DOM element...
RapidContext.Widget.Form.prototype.reset = function () {
    this.validateReset();
    var fields = this.fields();
    for (var i = 0; i < fields.length; i++) {
        var elem = fields[i];
        // TODO: generic form field value setting
        if (typeof(elem.reset) == "function") {
            elem.reset();
        } else if (elem.type == "radio" && typeof(elem.defaultChecked) == "boolean") {
            elem.checked = elem.defaultChecked;
        } else if (elem.type == "checkbox" && typeof(elem.defaultChecked) == "boolean") {
            elem.checked = elem.defaultChecked;
        } else if (typeof(elem.defaultValue) == "string") {
            if (typeof(elem.setAttrs) == "function") {
                elem.setAttrs({ value: elem.defaultValue });
            } else {
                elem.value = elem.defaultValue;
            }
        } else if (elem.options != null) {
            for (var j = 0; j < elem.options.length; j++) {
                var opt = elem.options[j];
                opt.selected = opt.defaultSelected;
            }
        }
    }
};

/**
 * Returns a map with all form field values. If multiple fields have
 * the same name, the value will be set to an array of all values.
 * Any unchecked checkbox or radiobutton will be also be ignored.
 *
 * @return {Object} the map of form field values
 */
RapidContext.Widget.Form.prototype.valueMap = function () {
    var fields = this.fields();
    var map = {};
    for (var i = 0; i < fields.length; i++) {
        var name = fields[i].name;
        // TODO: use generic field value retrieval
        var value = "";
        if (typeof(fields[i].getValue) == "function") {
            value = fields[i].getValue();
        } else {
            value = fields[i].value;
        }
        if (fields[i].type === "radio" || fields[i].type === "checkbox") {
            if (fields[i].checked) {
                value = value || true;
            } else {
                value = null;
            }
        }
        if (typeof(name) == "string" && name != "*" && value != null) {
            if (map[name] instanceof Array) {
                map[name].push(value);
            } else if (map[name] != null) {
                map[name] = [map[name], value];
            } else {
                map[name] = value;
            }
        }
    }
    return map;
};

/**
 * Updates the fields in this form with a specified map of values.
 * If multiple fields have the same name, the value will be set to
 * all of them.
 *
 * @param {Object} values the map of form field values
 */
RapidContext.Widget.Form.prototype.update = function (values) {
    var fields = this.fields();
    for (var i = 0; i < fields.length; i++) {
        var elem = fields[i];
        if (elem.name == "*") {
            if (typeof(elem.setAttrs) == "function") {
                elem.setAttrs({ value: values });
            }
        } else if (elem.name in values) {
            var value = values[elem.name];
            // TODO: generic form field value setting
            if (elem.type === "radio" || elem.type === "checkbox") {
                if (value == null) {
                    elem.checked = false;
                } else if (MochiKit.Base.isArrayLike(value)) {
                    elem.checked = (MochiKit.Base.findValue(value, elem.value) >= 0);
                } else {
                    elem.checked = (elem.value === value || value === true);
                }
            } else {
                if (typeof(elem.setAttrs) == "function") {
                    elem.setAttrs({ value: value });
                } else {
                    if (MochiKit.Base.isArrayLike(value)) {
                        value = value.join(", ");
                    }
                    elem.value = value;
                }
            }
        }
    }
};

/**
 * Returns an array with all child DOM nodes containing form validator widgets.
 *
 * @return {Array} the array of form validator widgets
 */
RapidContext.Widget.Form.prototype.validators = function () {
    var res = [];
    var elems = this.getElementsByTagName("SPAN");
    for (var i = 0; i < elems.length; i++) {
        if (RapidContext.Widget.isWidget(elems[i], "FormValidator")) {
            res.push(elems[i]);
        }
    }
    return res;
};

/**
 * Validates this form using the form validators found.
 *
 * @return {Boolean/MochiKit.Async.Deferred} `true` if the form
 *         validated successfully, `false` if the validation failed,
 *         or a `MochiKit.Async.Deferred` instance if the validation
 *         was deferred
 */
RapidContext.Widget.Form.prototype.validate = function () {
    var validators = this.validators();
    var fields = this.fields();
    var values = this.valueMap();
    var success = true;
    var defers = [];
    for (var i = 0; i < validators.length; i++) {
        validators[i].reset();
    }
    for (var i = 0; i < validators.length; i++) {
        for (var j = 0; j < fields.length; j++) {
            if (validators[i].name == fields[j].name) {
                var name = fields[j].name;
                var res = validators[i].verify(fields[j], values[name] || "");
                if (res instanceof MochiKit.Async.Deferred) {
                    defers.push(res);
                } else if (res === false) {
                    success = false;
                }
            }
        }
    }
    if (!success) {
        return false;
    } else if (defers.length > 0) {
        return MochiKit.Async.gatherResults(defers);
    } else {
        return true;
    }
};

/**
 * Resets all form validators. This metod is automatically called upon form
 * reset.
 *
 * @see #reset
 */
RapidContext.Widget.Form.prototype.validateReset = function () {
    var validators = this.validators();
    for (var i = 0; i < validators.length; i++) {
        validators[i].reset();
    }
};

/**
 * Handles the form submit signal.
 *
 * @param {Event} evt the MochiKit.Signal.Event object
 */
RapidContext.Widget.Form.prototype._handleSubmit = function (evt) {
    evt.stop();
    return false;
};
