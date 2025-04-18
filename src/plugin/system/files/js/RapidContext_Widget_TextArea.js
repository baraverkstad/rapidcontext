/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2025 Per Cederberg. All rights reserved.
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
 * @param {string} [attrs.name] the form field name
 * @param {string} [attrs.value] the field value, defaults to ""
 * @param {string} [attrs.helpText] the help text when empty (deprecated)
 * @param {boolean} [attrs.autosize] the auto-resize flag, defaults to false
 * @param {boolean} [attrs.disabled] the disabled widget flag, defaults to
 *            false
 * @param {boolean} [attrs.hidden] the hidden widget flag, defaults to false
 * @param {...(string|Node)} [value] the initial text content
 *
 * @return {Widget} the widget DOM node
 *
 * @class The text area widget class. Used to provide a text input field
 *     spanning multiple rows, using the `<textarea>` HTML element.
 * @property {boolean} disabled The read-only widget disabled flag.
 * @property {string} defaultValue The value to use on form reset.
 * @extends RapidContext.Widget
 *
 * @example <caption>JavaScript</caption>
 * var attrs = { name="description", placeholder: "Description Text" };
 * var field = RapidContext.Widget.TextArea(attrs);
 *
 * @example <caption>User Interface XML</caption>
 * <TextArea name="description" placeholder="Description Text" />
 */
RapidContext.Widget.TextArea = function (attrs/*, ...*/) {
    function scrape(val) {
        return String(val && val.textContent || val || "");
    }
    let text = (attrs && attrs.value) || "";
    text += Array.from(arguments).slice(1).map(scrape).join("");
    const o = RapidContext.UI.TEXTAREA({
        autocapitalize: "off",
        autocomplete: "off",
        autocorrect: "off",
        spellcheck: "off",
        value: text,
    });
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.TextArea);
    o.addClass("widgetTextArea");
    o.setAttrs({ ...attrs, value: text });
    o.on("input", o._handleChange);
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
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {string} [attrs.name] the form field name
 * @param {string} [attrs.value] the field value
 * @param {string} [attrs.helpText] the help text when empty (deprecated)
 * @param {boolean} [attrs.autosize] the auto-resize flag
 * @param {boolean} [attrs.disabled] the disabled widget flag
 * @param {boolean} [attrs.hidden] the hidden widget flag
 */
RapidContext.Widget.TextArea.prototype.setAttrs = function (attrs) {
    attrs = { ...attrs };
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
    if (this.autosize && this.scrollHeight == 0) {
        this.rows = this.value.split("\n").length;
    }
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
 * @return {string} the field value
 *
 * @example
 * var str = field.getValue();
 * var lines = str.split("\n").map((s) => s.trim());
 * field.setAttrs({ "value": lines.join("\n") });
 */
RapidContext.Widget.TextArea.prototype.getValue = function () {
    let str = this.value;
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
    const cause = (evt && evt.inputType) || "set";
    const detail = { before: this.storedValue || "", after: this.value, cause: cause };
    this.emit("change", { detail: detail, bubbles: true });
    this.storedValue = this.value;
    if (this.autosize) {
        this.style.height = "auto";
        if (this.scrollHeight > 10) {
            this.style.height = `${this.scrollHeight}px`;
        }
    }
};
