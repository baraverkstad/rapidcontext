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
 * Creates a new form validator widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {String} attrs.name the form field name to validate
 * @param {Boolean} [attrs.mandatory] the mandatory field flag,
 *            defaults to `true`
 * @param {String/RegExp} [attrs.regex] the regular expression to
 *            match the field value against, defaults to `null`
 * @param {String} [attrs.display] the validator display setting
 *            (either "none", "icon", "text" or "both"), defaults
 *            to "both"
 * @param {String} [attrs.message] the message to display, defaults
 *            to the validator function error message
 * @param {Function} [attrs.validator] the validator function
 * @param {Boolean} [attrs.hidden] the hidden widget flag, defaults to `false`
 *
 * @return {Widget} the widget DOM node
 *
 * @class The form validator widget class. Provides visual feedback on form
 *     validation failures, using a `<span>` HTML element. It is normally
 *     hidden by default and may be configured to only modify its related form
 *     field.
 * @property {String} name The form field name to validate.
 * @property {String} message The default validation message.
 * @property {Function} validator The validator function in use.
 * @extends RapidContext.Widget
 *
 * @example {JavaScript}
 * var field = RapidContext.Widget.TextField({ name: "name", placeholder: "Your Name Here" });
 * var attrs = { name: "name", message: "Please enter your name to proceed." };
 * var valid = RapidContext.Widget.FormValidator(attrs);
 * var exampleForm = RapidContext.Widget.Form({}, field, valid);
 *
 * @example {User Interface XML}
 * <Form id="exampleForm">
 *   <TextField name="name" placeholder="Your Name Here" />
 *   <FormValidator name="name" message="Please enter your name to proceed." />
 * </Form>
 */
RapidContext.Widget.FormValidator = function (attrs) {
    var o = MochiKit.DOM.SPAN();
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.FormValidator);
    o.addClass("widgetFormValidator");
    var defaults = { name: "", mandatory: true, display: "both", message: null, validator: null };
    o.setAttrs(MochiKit.Base.update(defaults, attrs));
    o.fields = [];
    o.hide();
    return o;
};

// Register widget class
RapidContext.Widget.Classes.FormValidator = RapidContext.Widget.FormValidator;

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {String} [attrs.name] the form field name to validate
 * @param {Boolean} [attrs.mandatory] the mandatory field flag
 * @param {String/RegExp} [attrs.regex] the regular expression to
 *            match the field value against
 * @param {String} [attrs.display] the validator display setting
 *            (either "none", "icon", "text" or "both")
 * @param {String} [attrs.message] the message to display
 * @param {Function} [attrs.validator] the validator function
 * @param {Boolean} [attrs.hidden] the hidden widget flag
 */
RapidContext.Widget.FormValidator.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
    var locals = RapidContext.Util.mask(attrs, ["name", "mandatory", "regex", "display", "message", "validator"]);
    if (typeof(locals.name) != "undefined") {
        this.name = locals.name;
    }
    if (typeof(locals.mandatory) != "undefined") {
        this.mandatory = MochiKit.Base.bool(locals.mandatory);
    }
    if (typeof(locals.regex) != "undefined") {
        if (locals.regex instanceof RegExp) {
            this.regex = locals.regex;
        } else {
            if (locals.regex.indexOf("^") != 0) {
                locals.regex = "^" + locals.regex;
            }
            if (locals.regex.indexOf("$") != locals.regex.length - 1) {
                locals.regex += "$";
            }
            this.regex = new RegExp(locals.regex);
        }
    }
    if (typeof(locals.display) != "undefined") {
        this.display = locals.display;
    }
    if (typeof(locals.message) != "undefined") {
        this.message = locals.message;
    }
    if (typeof(locals.validator) != "undefined") {
        this.validator = locals.validator;
    }
    this.__setAttrs(attrs);
};

/**
 * Resets this form validator. This will hide any error messages and mark all
 * invalidated fields as valid.
 *
 * Note that this method is normally not called directly, instead the
 * validation is reset by the `RapidContext.Widget.Form` widget.
 *
 * @see RapidContext.Widget.Form#validateReset
 */
RapidContext.Widget.FormValidator.prototype.reset = function () {
    this.fields.forEach(function (field) {
        MochiKit.DOM.removeElementClass(field, "invalid");
    });
    this.fields = [];
    this.hide();
    this.removeAll();
};

/**
 * Verifies a form field with this validator. If the form field value doesn't
 * match this validator, the field will be invalidated until this validator is
 * reset.
 *
 * Note that this method is normally not called directly, instead the
 * validation is performed by the `RapidContext.Widget.Form` widget.
 *
 * @param {Widget/Node} field the form field DOM node
 * @param {String} [value] the form field value to check
 *
 * @return {Boolean} `true` if the form validated successfully, or
 *         `false` if the validation failed
 *
 * @see RapidContext.Widget.Form#validate
 */
RapidContext.Widget.FormValidator.prototype.verify = function (field, value) {
    if (!field.disabled) {
        if (arguments.length == 1 && typeof(field.getValue) == "function") {
            value = field.getValue();
        } else if (arguments.length == 1) {
            value = field.value;
        }
        var str = String(value).trim();
        if (field.validationMessage) {
            this.addError(field, field.validationMessage);
            return false;
        } else if (this.mandatory && str == "") {
            this.addError(field, "This field is required");
            return false;
        } else if (this.regex && str && !this.regex.test(str)) {
            this.addError(field, "The field format is incorrect");
            return false;
        } else if (typeof(this.validator) == "function") {
            var res = this.validator(value);
            if (res !== true) {
                this.addError(field, res || "Field validation failed");
                return false;
            }
        }
    }
    return true;
};

/**
 * Adds a validation error message for the specified field. If the field is
 * already invalid, this method will not do anything.
 *
 * Note that this method is normally not called directly, instead the
 * validation is performed by the `RapidContext.Widget.Form` widget.
 *
 * @param {Widget/Node} field the field DOM node
 * @param {String} message the validation error message
 *
 * @see RapidContext.Widget.Form#validate
 */
RapidContext.Widget.FormValidator.prototype.addError = function (field, message) {
    if (!MochiKit.DOM.hasElementClass(field, "invalid")) {
        this.fields.push(field);
        MochiKit.DOM.addElementClass(field, "invalid");
        if (this.display !== "none") {
            message = this.message || message;
            var span = null;
            var icon = null;
            if (!this.display || this.display === "both") {
                this.addClass("block");
            }
            if (this.display !== "icon") {
                span = MochiKit.DOM.SPAN({}, message);
            }
            if (this.display !== "text") {
                icon = RapidContext.Widget.Icon({ ref: "ERROR", tooltip: message });
            }
            if (!this.childNodes.length) {
                this.addAll(icon, span);
            }
            this.show();
        }
    }
};
