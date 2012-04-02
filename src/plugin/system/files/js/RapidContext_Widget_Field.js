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
RapidContext.Widget = RapidContext.Widget || { Classes: {}};

/**
 * Creates a new field widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {String} attrs.name the field name
 * @param {String} [attrs.value] the initial field value, defaults
 *            to an empty string
 * @param {String} [attrs.format] the field format string, defaults
 *            to "{:s}"
 * @param {Function} [attrs.formatter] the value formatter function
 * @param {Number} [attrs.maxLength] the maximum data length,
 *            overflow will be displayed as a tooltip, defaults to
 *            -1 (unlimited)
 *
 * @return {Widget} the widget DOM node
 *
 * @class The field widget class. This widget is useful for providing
 *     visible display of form data, using a &lt;span&gt; HTML
 *     element.
 * @extends RapidContext.Widget
 *
 * @example
 * var field = RapidContext.Widget.Field({ name: "ratio", format: "Ratio: {:%}" });
 * form.addAll(field);
 * field.setAttrs({ value: 0.23 });
 */
RapidContext.Widget.Field = function (attrs) {
    var o = MochiKit.DOM.SPAN();
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetField");
    o.setAttrs(MochiKit.Base.update({ name: "", value: "", maxLength: -1 }, attrs));
    o.defaultValue = o.value;
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Field = RapidContext.Widget.Field;

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {String} [attrs.name] the field name
 * @param {String} [attrs.value] the field value
 * @param {String} [attrs.format] the field format string
 * @param {Function} [attrs.formatter] the value formatter function
 * @param {Number} [attrs.maxLength] the maximum data length,
 *            overflow will be displayed as a tooltip
 *
 * @example
 * field.setAttrs({ value: 0.23 });
 */
RapidContext.Widget.Field.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
    var locals = RapidContext.Util.mask(attrs, ["name", "value", "format", "formatter", "maxLength"]);
    if (typeof(locals.name) != "undefined") {
        this.name = locals.name;
    }
    if (typeof(locals.format) != "undefined") {
        this.format = locals.format;
    }
    if (typeof(locals.formatter) != "undefined") {
        this.formatter = locals.formatter;
    }
    if (typeof(locals.maxLength) != "undefined") {
        this.maxLength = parseInt(locals.maxLength);
    }
    if (typeof(locals.value) != "undefined") {
        var str = this.value = locals.value;
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
        var longStr = str;
        if (this.maxLength > 0) {
            str = MochiKit.Text.truncate(str, this.maxLength, "...");
        }
        MochiKit.DOM.replaceChildNodes(this, str);
        this.title = (str == longStr) ? null : longStr;
    }
    MochiKit.DOM.updateNodeAttributes(this, attrs);
};

/**
 * Resets the field value to the initial value.
 */
RapidContext.Widget.Field.prototype.reset = function () {
    this.setAttrs({ value: this.defaultValue });
};
