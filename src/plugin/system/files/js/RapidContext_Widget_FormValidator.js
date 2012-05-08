/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2012 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE.txt file for more details.
 */

// Namespace initialization
if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}
RapidContext.Widget = RapidContext.Widget || { Classes: {}};

/**
 * Creates a new form validator widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {String} attrs.name the form field name to validate
 * @param {Boolean} [attrs.mandatory] the mandatory field flag,
 *            defaults to true
 * @param {String/RegExp} [attrs.regex] the regular expression to
 *            match the field value against, defaults to null
 * @param {String} [attrs.display] the validator display setting
 *            (either "none", "icon", "text" or "both"), defaults
 *            to "both"
 * @param {String} [attrs.message] the message to display, defaults
 *            to the validator function error message
 * @param {Function} [attrs.validator] the validator function
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
 */
RapidContext.Widget.FormValidator = function (attrs) {
    var o = MochiKit.DOM.SPAN();
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetFormValidator");
    o.setAttrs(MochiKit.Base.update({ name: "", mandatory: true, display: "both", message: null, validator: null }, attrs));
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
 * Resets this form validator. This will hide any error messages and
 * mark all invalidated fields as valid.
 */
RapidContext.Widget.FormValidator.prototype.reset = function () {
    for (var i = 0; i < this.fields.length; i++) {
        MochiKit.DOM.removeElementClass(this.fields[i], "widgetInvalid");
    }
    this.fields = [];
    this.hide();
    this.removeAll();
};

/**
 * Verifies a form field with this validator. If the form field
 * value doesn't match this validator, the field will be invalidated
 * until this validator is reset.
 *
 * @param {Widget/Node} field the form field DOM node
 *
 * @return {Boolean/MochiKit.Async.Deferred} true if the form
 *         validated successfully, false if the validation failed,
 *         or a MochiKit.Async.Deferred instance if the validation
 *         was deferred
 */
RapidContext.Widget.FormValidator.prototype.verify = function (field) {
    if (!field.disabled) {
        // TODO: use generic field value retrieval
        var value = "";
        if (typeof(field.getValue) == "function") {
            value = field.getValue();
        } else {
            value = field.value;
        }
        var stripped = MochiKit.Format.strip(value);
        if (MochiKit.Format.strip(value) == "") {
            if (this.mandatory) {
                var msg = "This field is mandatory and cannot be left blank";
                this.addError(field, msg);
                return false;
            }
        } else if (this.regex != null && !this.regex.test(stripped)) {
            var msg = "The field format is incorrect";
            this.addError(field, msg);
            return false;
        } else if (typeof(this.validator) == "function") {
            var res = this.validator(value);
            if (res instanceof MochiKit.Async.Deferred) {
                var self = this;
                res.addErrback(function (e) {
                    self.addError(field, e.message);
                    return e;
                });
                return res;
            } else if (typeof(res) == "string") {
                this.addError(field, res);
                return false;
            } else if (res === false) {
                this.addError(field, "Field validation failed");
                return false;
            }
        }
    }
    return true;
};

/**
 * Adds a validation error message for the specified field. If the
 * field is already invalid, this method will not do anything.
 *
 * @param {Widget/Node} field the field DOM node
 * @param {String} message the validation error message
 */
RapidContext.Widget.FormValidator.prototype.addError = function (field, message) {
    if (!MochiKit.DOM.hasElementClass(field, "widgetInvalid")) {
        this.fields.push(field);
        MochiKit.DOM.addElementClass(field, "widgetInvalid");
        if (this.display !== "none") {
            message = this.message || message;
            var span = null;
            var icon = null;
            if (this.display !== "icon") {
                span = MochiKit.DOM.SPAN({}, message);
            }
            if (this.display !== "text") {
                icon = RapidContext.Widget.Icon({ ref: "ERROR", tooltip: message });
            }
            this.addAll(icon, span);
            this.show();
        }
    }
};
