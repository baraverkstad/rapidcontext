/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2013 Per Cederberg. All rights reserved.
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
 * Creates a new field widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {String} attrs.name the form field name
 * @param {String} [attrs.value] the initial field value, defaults
 *            to an empty string
 * @param {String} [attrs.format] the field format string, defaults
 *            to "{:s}"
 * @param {Function} [attrs.formatter] the value formatter function
 * @param {Number} [attrs.maxLength] the maximum data length,
 *            overflow will be displayed as a tooltip, defaults to
 *            -1 (unlimited)
 * @param {Boolean} [attrs.mask] the masked display flag, when set
 *            the field value is only displayed after the user has
 *            clicked the field, defaults to false
 * @param {Boolean} [attrs.hidden] the hidden widget flag, defaults to false
 *
 * @return {Widget} the widget DOM node
 *
 * @class The field widget class. This widget is useful for providing
 *     visible display of form data, using a `<span>` HTML element.
 * @extends RapidContext.Widget
 *
 * @example {JavaScript}
 * var attrs = { name: "ratio", value: 0.23, format: "Ratio: {:%}" };
 * var field = RapidContext.Widget.Field(attrs);
 *
 * @example {User Interface XML}
 * <Field name="ratio" value="0.23" format="Ratio: {:%}" />
 */
RapidContext.Widget.Field = function (attrs) {
    var o = MochiKit.DOM.SPAN();
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetField");
    o.setAttrs(MochiKit.Base.update({ name: "", value: "", maxLength: -1, mask: false }, attrs));
    o.defaultValue = o.value;
    o.defaultMask = o.mask;
    o.onclick = RapidContext.Widget._eventHandler(null, "_handleClick");
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
 * @param {String} [attrs.name] the form field name
 * @param {String} [attrs.value] the field value
 * @param {String} [attrs.format] the field format string
 * @param {Function} [attrs.formatter] the value formatter function
 * @param {Number} [attrs.maxLength] the maximum data length,
 *            overflow will be displayed as a tooltip
 * @param {Boolean} [attrs.mask] the masked display flag, when set
 *            the field value is only displayed after the user has
 *            clicked the field
 * @param {Boolean} [attrs.hidden] the hidden widget flag
 *
 * @example
 * field.setAttrs({ value: 0.23 });
 */
RapidContext.Widget.Field.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
    var locals = RapidContext.Util.mask(attrs, ["name", "value", "format", "formatter", "maxLength", "mask"]);
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
    if (typeof(locals.mask) != "undefined") {
        this.mask = locals.mask;
    }
    if (typeof(locals.value) != "undefined") {
        this.value = locals.value;
    }
    this.__setAttrs(attrs);
    this.redraw();
}

/**
 * Redraws the field from updated values or status. Note that this
 * method is called automatically whenever the `setAttrs()` method is
 * called.
 */
RapidContext.Widget.Field.prototype.redraw = function () {
    var str = this.value;
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
    if (this.mask) {
        var tooltip = "Click to Show Value";
        var mask = "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022";
        var icon = RapidContext.Widget.Icon({ ref: "LOCK", tooltip: tooltip });
        this.addClass("widgetFieldMask");
        this.title = tooltip;
        MochiKit.DOM.replaceChildNodes(this, mask, icon);
    } else {
        this.title = (str == longStr) ? null : longStr;
        MochiKit.DOM.replaceChildNodes(this, str);
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
