/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2026 Per Cederberg. All rights reserved.
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
 * @param {boolean} [attrs.hidden] the hidden widget flag, defaults to false
 * @param {...(string|Node|Array)} [child] the child widgets or DOM nodes
 *
 * @return {Widget} the widget DOM node
 *
 * @class The form widget class. Provides a grouping for form fields, using the
 *     `<form>` HTML element. The form widget supports form reset, validation
 *     and data retrieval.
 * @extends RapidContext.Widget
 *
 * @example <caption>JavaScript</caption>
 * var field = RapidContext.Widget.TextField({ name: "name", placeholder: "Your Name Here" });
 * var attrs = { name: "name", message: "Please enter your name to proceed." };
 * var valid = RapidContext.Widget.FormValidator(attrs);
 * var exampleForm = RapidContext.Widget.Form({}, field, valid);
 *
 * @example <caption>User Interface XML</caption>
 * <Form id="exampleForm">
 *   <TextField name="name" placeholder="Your Name Here" />
 *   <FormValidator name="name" message="Please enter your name to proceed." />
 * </Form>
 */
RapidContext.Widget.Form = function (attrs/*, ...*/) {
    const o = RapidContext.UI.FORM(attrs);
    o._validators = {};
    o._originalReset = o.reset;
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.Form);
    o.addClass("widgetForm");
    o.setAttrs(attrs);
    o.addAll(Array.from(arguments).slice(1));
    o.on("input", o._handleInput);
    o.on("invalid", o._handleInvalid, { capture: true });
    o.on("submit", o._handleSubmit);
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Form = RapidContext.Widget.Form;

/**
 * Applies custom validators on field input.
 *
 * @param {Event} evt the DOM event object
 */
RapidContext.Widget.Form.prototype._handleInput = function (evt) {
    this._callValidators(evt.target);
};

/**
 * Debounces the input invalid events and validates the form.
 */
RapidContext.Widget.Form.prototype._handleInvalid = function () {
    if (this._validationTimer !== false) {
        this._validationTimer && clearTimeout(this._validationTimer);
        this._validationTimer = setTimeout(() => this.validate(), 10);
    }
};

/**
 * Prevents the default submit action and validates the form.
 *
 * @param {Event} evt the DOM event object
 */
RapidContext.Widget.Form.prototype._handleSubmit = function (evt) {
    evt.preventDefault();
    if (!this.validate()) {
        evt.stopImmediatePropagation();
    }
};

RapidContext.Widget.Form.prototype._fieldValue = function (field) {
    if (field.disabled) {
        return null;
    } else if (field.type === "radio" || field.type === "checkbox") {
        return field.checked ? (field.value || true) : null;
    } else if (typeof(field.getValue) == "function") {
        return field.getValue();
    } else {
        return field.value;
    }
};

/**
 * Returns an array with all child DOM nodes containing form fields.
 *
 * @return {Array} the array of form field elements
 *
 * @see https://developer.mozilla.org/en-US/docs/Web/API/HTMLFormElement/elements
 */
RapidContext.Widget.Form.prototype.fields = function () {
    const basics = Array.from(this.elements);
    const extras = Array.from(this.querySelectorAll(".widgetField"));
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
        const k = field.name;
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
    Array.from(this.querySelectorAll(".widgetField")).forEach((f) => f.reset());
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
    const update = (o, field) => {
        const k = field.name;
        const v = this._fieldValue(field);
        if (k && k != "*" && v != null) {
            o[k] = (k in o) ? [].concat(o[k], v) : v;
        }
        return o;
    };
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
        const v = values[field.name];
        if (field.name == "*" && typeof(field.setAttrs) == "function") {
            field.setAttrs({ value: values });
        } else if (!(field.name in values)) {
            // Don't change omitted fields
        } else if (field.type === "radio" || field.type === "checkbox") {
            const found = Array.isArray(v) && v.includes(field.value);
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
 * Adds a custom form validator for a named form field. The function will be
 * called as `[field].validator([value], [field], [form])` and should return
 * `true`, `false` or a validation error message. The validator will be called
 * on each `input` event and before form submission for enabled fields.
 *
 * Note: Checkbox validators will be called once for each `<input>` element,
 * regardless of checked state. Radio validators will only be called with
 * either the first or the checked `<input>` element.
 *
 * @param {string|Element} field the form field or name
 * @param {function} validator the validator function
 */
RapidContext.Widget.Form.prototype.addValidator = function (field, validator) {
    const name = String(field.name ?? field);
    const arr = [].concat(this._validators[name], validator).filter(Boolean);
    this._validators[name] = arr;
};

/**
 * Removes all custom form validators for a named form field.
 *
 * @param {string|Element} [field] the form field, name, or null for all
 */
RapidContext.Widget.Form.prototype.removeValidators = function (field) {
    if (field) {
        const name = String(field.name ?? field);
        delete this._validators[name];
    } else {
        this._validators = {};
    }
};

/**
 * Calls all custom validators for a form field. The validation result will
 * update the `setCustomValidity()` on the field.
 *
 * @param {Element} field the form field
 */
RapidContext.Widget.Form.prototype._callValidators = function (field) {
    const validators = this._validators[field.name];
    if (!field.disabled && validators) {
        let res = true;
        validators.forEach((validator) => {
            if (res === true) {
                res = validator.call(field, this._fieldValue(field), field, this);
            }
        });
        field.setCustomValidity((res === true) ? "" : (res || "Validation failed"));
    }
};

/**
 * Returns an array with all child DOM nodes containing form validator widgets.
 *
 * @return {Array} the array of form validator widgets
 */
RapidContext.Widget.Form.prototype.validators = function () {
    return Array.from(this.querySelectorAll(".widgetFormValidator"));
};

/**
 * Validates this form using the form validators found.
 *
 * @return {boolean} `true` if the form validated successfully, or
 *         `false` if the validation failed
 */
RapidContext.Widget.Form.prototype.validate = function () {
    this._validationTimer && clearTimeout(this._validationTimer);
    this._validationTimer = false;
    const fields = this.fieldMap();
    const values = this.valueMap();
    let success = true;
    this.validateReset();
    Object.keys(this._validators).forEach((name) => {
        [].concat(fields[name]).filter(Boolean).forEach((f) => {
            if (f.type !== "radio" || f.checked) {
                this._callValidators(f);
            }
        });
    });
    this.validators().forEach((validator) => {
        [].concat(fields[validator.name]).filter(Boolean).forEach((f) => {
            success = validator.verify(f, values[f.name] ?? "") && success;
        });
    });
    success = this.checkValidity() && success;
    success || this.addClass("invalid");
    delete this._validationTimer;
    this.emit("validate", { detail: success, bubbles: true });
    return success;
};

/**
 * Resets all form validations. This method is automatically called when form
 * is reset.
 *
 * @see #reset
 */
RapidContext.Widget.Form.prototype.validateReset = function () {
    const fields = this.fieldMap();
    Object.keys(fields).forEach((name) => {
        [].concat(fields[name]).filter(Boolean).forEach((f) => {
            f.setCustomValidity && f.setCustomValidity("");
        });
    });
    [this, ...this.querySelectorAll(".invalid")].forEach((el) => {
        el.classList.remove("invalid");
    });
    this.validators().forEach((validator) => validator.reset());
};
