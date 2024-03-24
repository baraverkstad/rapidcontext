/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2024 Per Cederberg. All rights reserved.
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
 * Creates a new text field widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {string} [attrs.name] the form field name
 * @param {string} [attrs.value] the field value, defaults to ""
 * @param {string} [attrs.helpText] the help text when empty (deprecated)
 * @param {boolean} [attrs.disabled] the disabled widget flag, defaults to
 *            false
 * @param {boolean} [attrs.hidden] the hidden widget flag, defaults to false
 * @param {...(string|Node)} [value] the initial text content
 *
 * @return {Widget} the widget DOM node
 *
 * @class The text field widget class. Used to provide a text input field for a
 *     single line, using the `<input>` HTML element.
 * @property {boolean} disabled The read-only widget disabled flag.
 * @property {string} defaultValue The value to use on form reset.
 * @extends RapidContext.Widget
 *
 * @example <caption>JavaScript</caption>
 * var attrs = { name: "name", placeholder: "Your Name Here" };
 * var field = RapidContext.Widget.TextField(attrs);
 *
 * @example <caption>User Interface XML</caption>
 * <TextField name="name" placeholder="Your Name Here" />
 */
RapidContext.Widget.TextField = function (attrs/*, ...*/) {
    function scrape(val) {
        return String(val && val.textContent || val || "");
    }
    var type = (attrs && attrs.type) || "text";
    var text = (attrs && attrs.value) || "";
    text += Array.from(arguments).slice(1).map(scrape).join("");
    var o = RapidContext.UI.INPUT({ type: type, value: text });
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.TextField);
    o.addClass("widgetTextField");
    o.setAttrs(Object.assign({}, attrs, { value: text }));
    o.on("input", o._handleChange);
    return o;
};

// Register widget class
RapidContext.Widget.Classes.TextField = RapidContext.Widget.TextField;

/**
 * Emitted when the text is modified. This event is triggered by either
 * user events (keypress, paste, cut, blur) or by setting the value via
 * setAttrs(). The DOM standard onchange event has no 'event.detail'
 * data and is triggered on blur. The synthetic onchange events all
 * contain an 'event.detail' object with 'before', 'after' and 'cause'
 * properties.
 *
 * @name RapidContext.Widget.TextField#onchange
 * @event
 */

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {string} [attrs.name] the form field name
 * @param {string} [attrs.value] the field value
 * @param {string} [attrs.helpText] the help text when empty (deprecated)
 * @param {boolean} [attrs.disabled] the disabled widget flag
 * @param {boolean} [attrs.hidden] the hidden widget flag
 */
RapidContext.Widget.TextField.prototype.setAttrs = function (attrs) {
    attrs = Object.assign({}, attrs);
    if ("helpText" in attrs) {
        console.warn("deprecated: setting 'helpText' attribute, use 'placeholder' instead");
        attrs.placeholder = attrs.placeholder || attrs.helpText;
        delete attrs.helpText;
    }
    if ("value" in attrs) {
        // FIXME: This is wrong, since we're setting an attribute here.
        // But until Form.update() has some other way to set a field
        // value and trigger changes, this will remain.
        this.value = attrs.value || "";
        this._handleChange(null);
        delete attrs.value;
    }
    this.__setAttrs(attrs);
};

/**
 * Resets the text area form value to the initial value.
 */
RapidContext.Widget.TextField.prototype.reset = function () {
    this.setAttrs({ value: this.defaultValue });
};

/**
 * Returns the text field value.
 *
 * @return {string} the field value
 */
RapidContext.Widget.TextField.prototype.getValue = function () {
    return this.value;
};

/**
 * Handles input events for this this widget.
 *
 * @param {Event} [evt] the DOM Event object or null for manual
 */
RapidContext.Widget.TextField.prototype._handleChange = function (evt) {
    var cause = (evt && evt.inputType) || "set";
    var detail = { before: this.storedValue || "", after: this.value, cause: cause };
    this.emit("change", { detail: detail, bubbles: true });
    this.storedValue = this.value;
};
