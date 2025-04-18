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
 * Creates a new field widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {string} attrs.name the form field name
 * @param {string} [attrs.value] the initial field value, defaults
 *            to an empty string
 * @param {string} [attrs.tag] the HTML tag to use, defaults to "span"
 * @param {string} [attrs.format] the field format string, defaults
 *            to "{:s}"
 * @param {function} [attrs.formatter] the value formatter function
 * @param {number} [attrs.maxLength] the maximum data length,
 *            overflow will be displayed as a tooltip, defaults to
 *            unlimited
 * @param {boolean} [attrs.mask] the masked display flag, when set
 *            the field value is only displayed after the user has
 *            clicked the field, defaults to false
 * @param {boolean} [attrs.hidden] the hidden widget flag, defaults to false
 *
 * @return {Widget} the widget DOM node
 *
 * @class The field widget class. This widget is useful for providing
 *     visible display of form data, using a `<span>` HTML element.
 * @extends RapidContext.Widget
 *
 * @example <caption>JavaScript</caption>
 * let attrs = { name: "ratio", value: 0.23, format: "Ratio: {:%}" };
 * let field = RapidContext.Widget.Field(attrs);
 *
 * @example <caption>User Interface XML</caption>
 * <Field name="ratio" value="0.23" format="Ratio: {:%}" />
 */
RapidContext.Widget.Field = function (attrs) {
    const o = document.createElement(attrs.tag || "span");
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.Field);
    o.addClass("widgetField");
    o.setAttrs({ name: "", value: "", ...attrs });
    o.defaultValue = o.value;
    o.defaultMask = !!o.mask;
    o.on("click", o._handleClick);
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Field = RapidContext.Widget.Field;

/**
 * Returns the widget container DOM node.
 *
 * @return {Node} returns null, since child nodes are not supported
 */
RapidContext.Widget.Field.prototype._containerNode = function () {
    return null;
};

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {string} [attrs.name] the form field name
 * @param {string} [attrs.value] the field value
 * @param {string} [attrs.format] the field format string
 * @param {function} [attrs.formatter] the value formatter function
 * @param {number} [attrs.maxLength] the maximum data length,
 *            overflow will be displayed as a tooltip
 * @param {boolean} [attrs.mask] the masked display flag, when set
 *            the field value is only displayed after the user has
 *            clicked the field
 * @param {boolean} [attrs.hidden] the hidden widget flag
 *
 * @example
 * field.setAttrs({ value: 0.23 });
 */
RapidContext.Widget.Field.prototype.setAttrs = function (attrs) {
    attrs = { ...attrs };
    if ("formatter" in attrs) {
        const valid = typeof(attrs.formatter) == "function";
        attrs.formatter = valid ? attrs.formatter : null;
    }
    if ("maxLength" in attrs) {
        const val = parseInt(attrs.maxLength, 10);
        attrs.maxLength = isNaN(val) ? null : val;
    }
    if ("mask" in attrs) {
        attrs.mask = RapidContext.Data.bool(attrs.mask);
    }
    this.__setAttrs(attrs);
    this.redraw();
};

/**
 * Redraws the field from updated values or status. Note that this
 * method is called automatically whenever the `setAttrs()` method is
 * called.
 */
RapidContext.Widget.Field.prototype.redraw = function () {
    let str = this.value;
    if (this.formatter) {
        try {
            str = this.formatter(str);
        } catch (e) {
            str = e.message;
        }
    } else if (str == null || str === "") {
        str = "";
    } else if (this.format) {
        str = MochiKit.Text.format(this.format, str);
    } else if (typeof(str) != "string") {
        str = str.toString();
    }
    const longStr = str;
    if (this.maxLength > 0) {
        str = MochiKit.Text.truncate(str, this.maxLength, "...");
    }
    if (this.mask) {
        this.addClass("widgetFieldMask");
        this.title = "Click to show";
        this.innerText = "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022";
        this.append(RapidContext.Widget.Icon({ ref: "LOCK", tooltip: "Click to show" }));
    } else {
        if (str == longStr) {
            delete this.title;
        } else {
            this.title = longStr;
        }
        this.innerText = str;
    }
};

/**
 * Resets the field value to the initial value.
 */
RapidContext.Widget.Field.prototype.reset = function () {
    this.setAttrs({ value: this.defaultValue, mask: this.defaultMask });
};

/**
 * Handles click events on the field (if masked).
 */
RapidContext.Widget.Field.prototype._handleClick = function () {
    if (this.mask) {
        this.mask = false;
        this.removeClass("widgetFieldMask");
        this.redraw();
    }
};
