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
 * Creates a new text area (or text box) widget.
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
 * @class The text area widget class. Used to provide a text input field
 *     spanning multiple rows, using the `<textarea>` HTML element.
 * @property {Boolean} disabled The read-only widget disabled flag.
 * @property {String} defaultValue The value to use on form reset.
 * @extends RapidContext.Widget
 *
 * @example {JavaScript}
 * var attrs = { name="description", placeholder: "Description Text" };
 * var field = RapidContext.Widget.TextArea(attrs);
 *
 * @example {User Interface XML}
 * <TextArea name="description" placeholder="Description Text" />
 */
RapidContext.Widget.TextArea = function (attrs/*, ...*/) {
    function scrape(val) {
        return String(val && val.textContent || val || "");
    }
    var text = (attrs && attrs.value) || "";
    text += Array.prototype.slice.call(arguments, 1).map(scrape).join("");
    var o = MochiKit.DOM.TEXTAREA({ value: text });
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.TextArea);
    o.addClass("widgetTextArea");
    o.setAttrs(MochiKit.Base.update({}, attrs, { value: text }));
    o.addEventListener("input", o._handleChange);
    return o;
};

// Register widget class
RapidContext.Widget.Classes.TextArea = RapidContext.Widget.TextArea;

/**
 * Emitted when the text is modified. This event is triggered by either
 * user events (keypress, paste, cut, blur) or by setting the value via
 * setAttrs(). The DOM standard onchange event has no 'event.detail'
 * data and is triggered on blur. The synthetic onchange events all
 * contain an 'event.detail' object with 'before', 'after' and 'cause'
 * properties.
 *
 * @name RapidContext.Widget.TextArea#onchange
 * @event
 */

/**
 * Destroys this widget.
 */
RapidContext.Widget.TextArea.prototype.destroy = function () {
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
RapidContext.Widget.TextArea.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
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
RapidContext.Widget.TextArea.prototype.reset = function () {
    this.setAttrs({ value: this.defaultValue });
};

/**
 * Returns the text area value. This function is slightly different from using
 * the `value` property directly, since it will attempt to normalize newlines
 * in the value.
 *
 * @return {String} the field value
 *
 * @example
 * var str = field.getValue();
 * var lines = str.split("\n").map((s) => s.trim());
 * field.setAttrs({ "value": lines.join("\n") });
 */
RapidContext.Widget.TextArea.prototype.getValue = function () {
    var str = this.value;
    // This is a hack to remove multiple newlines caused by
    // platforms inserting or failing to normalize newlines
    // within the HTML textarea control.
    str = str.replace(/\r\n\n/g, "\n");
    if (this.value != str) {
        this.value = str;
    }
    return str;
};

/**
 * Handles input events for this this widget.
 *
 * @param {Event} [evt] the DOM Event object or null for manual
 */
RapidContext.Widget.TextArea.prototype._handleChange = function (evt) {
    var cause = (evt && evt.inputType) || "set";
    var detail = { before: this.storedValue || "", after: this.value, cause: cause };
    this._dispatch("change", { detail: detail, bubbles: true });
    this.storedValue = this.value;
};
