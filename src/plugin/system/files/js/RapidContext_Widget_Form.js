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
RapidContext.Widget = RapidContext.Widget || { Classes: {} };

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
    o._originalReset = o.reset;
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.Form);
    o.addClass("widgetForm");
    o.setAttrs(attrs);
    o.addAll(Array.prototype.slice.call(arguments, 1));
    // FIXME: handle HTML validation events to also process custom validators
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Form = RapidContext.Widget.Form;

/**
 * Prevents the default onsubmit action.
 *
 * @param {Event} evt the DOM event object
 */
RapidContext.Widget.Form.prototype.onsubmit = function (evt) {
    evt.preventDefault();
};

/**
 * Returns an array with all child DOM nodes containing form fields.
 * The child nodes will be returned based on the results of the
 * `RapidContext.Widget.isFormField()` function.
 *
 * @return {Array} the array of form field elements
 *
 * @see https://developer.mozilla.org/en-US/docs/Web/API/HTMLFormElement/elements
 */
RapidContext.Widget.Form.prototype.fields = function () {
    var basics = Array.prototype.slice.call(this.elements);
    var extras = Array.prototype.slice.call(this.querySelectorAll(".widgetField"));
    return basics.concat(extras);
};

/**
 * Returns a map with all child DOM nodes containing form fields with
 * a name attribute. If multiple fields have the same name, the
 * returned map will contain an array with all matching fields.
 *
 * @return {Object} the map of form field elements
 */
RapidContext.Widget.Form.prototype.fieldMap = function () {
    function update(o, field) {
        var k = field.name;
        if (k && k != "*") {
            o[k] = (k in o) ? [].concat(o[k], field) : field;
        }
        return o;
    }
    return this.fields().reduce(update, {});
};

/**
 * Resets all fields and validations to their original state.
 */
RapidContext.Widget.Form.prototype.reset = function () {
    this._originalReset();
    var extras = Array.prototype.slice.call(this.querySelectorAll(".widgetField"));
    extras.forEach(function (field) {
        field.reset();
    });
    this.validateReset();
};

/**
 * Returns a map with all form field values. If multiple fields have
 * the same name, the value will be set to an array of all values.
 * Disabled fields and unchecked checkboxes or radiobuttons will be
 * ignored.
 *
 * @return {Object} the map of form field values
 */
RapidContext.Widget.Form.prototype.valueMap = function () {
    function getValue(field) {
        if (field.disabled) {
            return null;
        } else if (field.type === "radio" || field.type === "checkbox") {
            return field.checked ? (field.value || true) : null;
        } else if (typeof(field.getValue) == "function") {
            return field.getValue();
        } else {
            return field.value;
        }
    }
    function update(o, field) {
        var k = field.name;
        var v = getValue(field);
        if (k && k != "*" && v != null) {
            o[k] = (k in o) ? [].concat(o[k], v) : v;
        }
        return o;
    }
    return this.fields().reduce(update, {});
};

/**
 * Updates the fields in this form with a specified map of values.
 * If multiple fields have the same name, the value will be set to
 * all of them.
 *
 * @param {Object} values the map of form field values
 */
RapidContext.Widget.Form.prototype.update = function (values) {
    function setValue(field) {
        var v = values[field.name];
        if (field.name == "*" && typeof(field.setAttrs) == "function") {
            field.setAttrs({ value: values });
        } else if (!(field.name in values)) {
            // Don't change omitted fields
        } else if (field.type === "radio" || field.type === "checkbox") {
            var found = MochiKit.Base.isArrayLike(v) && MochiKit.Base.findValue(v, field.value) >= 0;
            field.checked = found || v === field.value || v === true;
        } else if (typeof(field.setAttrs) == "function") {
            field.setAttrs({ value: v });
        } else {
            field.value = MochiKit.Base.isArrayLike(v) ? v.join(", ") : v;
        }
    }
    this.fields().forEach(setValue);
};

/**
 * Returns an array with all child DOM nodes containing form validator widgets.
 *
 * @return {Array} the array of form validator widgets
 */
RapidContext.Widget.Form.prototype.validators = function () {
    var nodes = this.querySelectorAll(".widgetFormValidator");
    return Array.prototype.slice.call(nodes);
};

/**
 * Validates this form using the form validators found.
 *
 * @return {Boolean} `true` if the form validated successfully, or
 *         `false` if the validation failed
 */
RapidContext.Widget.Form.prototype.validate = function () {
    // FIXME: Validate using standard HTML validation as well
    var fields = this.fieldMap();
    var values = this.valueMap();
    var success = true;
    this.validateReset();
    this.validators().forEach(function (validator) {
        [].concat(fields[validator.name]).filter(Boolean).forEach(function (f) {
            var res = validator.verify(f, values[f.name] || "");
            if (res === false) {
                success = false;
            }
        });
    });
    return success;
};

/**
 * Resets all form validators. This method is automatically called upon form
 * reset.
 *
 * @see #reset
 */
RapidContext.Widget.Form.prototype.validateReset = function () {
    this.validators().forEach(function (validator) {
        validator.reset();
    });
};
