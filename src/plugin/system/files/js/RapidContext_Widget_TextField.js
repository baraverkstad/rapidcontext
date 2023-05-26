/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2023 Per Cederberg. All rights reserved.
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
 * @param {String} [attrs.name] the form field name
 * @param {String} [attrs.value] the field value, defaults to ""
 * @param {String} [attrs.helpText] the help text when empty (deprecated)
 * @param {Boolean} [attrs.disabled] the disabled widget flag, defaults to
 *            false
 * @param {Boolean} [attrs.hidden] the hidden widget flag, defaults to false
 * @param {Object} [...] the initial text content
 *
 * @return {Widget} the widget DOM node
 *
 * @class The text field widget class. Used to provide a text input field for a
 *     single line, using the `<input>` HTML element.
 * @property {Boolean} disabled The read-only widget disabled flag.
 * @property {String} defaultValue The value to use on form reset.
 * @extends RapidContext.Widget
 *
 * @example {JavaScript}
 * var attrs = { name: "name", placeholder: "Your Name Here" };
 * var field = RapidContext.Widget.TextField(attrs);
 *
 * @example {User Interface XML}
 * <TextField name="name" placeholder="Your Name Here" />
 */
RapidContext.Widget.TextField = function (attrs/*, ...*/) {
    function scrape(val) {
        return String(val && val.textContent || val || "");
    }
    var type = (attrs && attrs.type) || "text";
    var text = (attrs && attrs.value) || "";
    text += Array.from(arguments).slice(1).map(scrape).join("");
    var o = MochiKit.DOM.INPUT({ type: type, value: text });
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.TextField);
    o.addClass("widgetTextField");
    o.setAttrs(Object.assign({}, attrs, { value: text }));
    o.addEventListener("input", o._handleChange);
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
 * Destroys this widget.
 */
RapidContext.Widget.TextField.prototype.destroy = function () {
    // FIXME: Use AbortSignal instead to disconnect
    this.removeEventListener("input", this._handleChange);
};

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {String} [attrs.name] the form field name
 * @param {String} [attrs.value] the field value
 * @param {String} [attrs.helpText] the help text when empty (deprecated)
 * @param {Boolean} [attrs.disabled] the disabled widget flag
 * @param {Boolean} [attrs.hidden] the hidden widget flag
 */
RapidContext.Widget.TextField.prototype.setAttrs = function (attrs) {
    attrs = Object.assign({}, attrs);
    var locals = RapidContext.Util.mask(attrs, ["helpText", "value"]);
    if ("helpText" in locals) {
        attrs.placeholder = attrs.placeholder || locals.helpText;
    }
    if ("value" in locals) {
        this.value = locals.value || "";
        this._handleChange(null);
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
 * @return {String} the field value
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
    this._dispatch("change", { detail: detail, bubbles: true });
    this.storedValue = this.value;
};
