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
 * Creates a new data table column widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {String} attrs.title the column title
 * @param {String} attrs.field the data property name
 * @param {String} [attrs.type] the data property type, one of
 *            "string", "number", "date", "time", "datetime",
 *            "boolean" or "object"
 * @param {String} [attrs.sort] the sort direction, one of "asc",
 *            "desc", "none" (disabled) or null (unsorted)
 * @param {Number} [attrs.maxLength] the maximum data length,
 *            overflow will be displayed as a tooltip, only used by
 *            the default renderer
 * @param {Boolean} [attrs.key] the unique key value flag, only to be
 *            set for a single column per table
 * @param {String} [attrs.tooltip] the tooltip text to display on the
 *            column header
 * @param {Function} [attrs.renderer] the function that renders the
 *            converted data value into a table cell, called with the
 *            TD DOM node, field value and data object as arguments
 *            (in that order)
 *
 * @return {Widget} the widget DOM node
 *
 * @class The table column widget class. Used to provide a sortable
 *     data table column, using a &lt;th&gt; HTML element for the
 *     header (and rendering data to &lt;td&gt; HTML elements).
 * @extends RapidContext.Widget
 */
RapidContext.Widget.TableColumn = function (attrs) {
    if (attrs.field == null) {
        throw new Error("The 'field' attribute cannot be null for a TableColumn");
    }
    var o = MochiKit.DOM.TH();
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetTableColumn");
    o.setAttrs(MochiKit.Base.update({ title: attrs.field, type: "string", key: false }, attrs));
    o.onclick = RapidContext.Widget._eventHandler(null, "_handleClick");
    return o;
};

// Register widget class
RapidContext.Widget.Classes.TableColumn = RapidContext.Widget.TableColumn;

/**
 * Updates the widget or HTML DOM node attributes. Note that some
 * updates will not take effect until the parent table is cleared
 * or data is reloaded.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {String} [attrs.title] the column title
 * @param {String} [attrs.field] the data property name
 * @param {String} [attrs.type] the data property type, one of
 *            "string", "number", "date", "time", "datetime",
 *            "boolean" or "object"
 * @param {String} [attrs.sort] the sort direction, one of "asc",
 *            "desc", "none" (disabled) or null (unsorted)
 * @param {Number} [attrs.maxLength] the maximum data length,
 *            overflow will be displayed as a tooltip, only used by
 *            the default renderer
 * @param {Boolean} [attrs.key] the unique key value flag, only to be
 *            set for a single column per table
 * @param {String} [attrs.tooltip] the tooltip text to display on the
 *            column header
 * @param {Function} [attrs.renderer] the function that renders the
 *            converted data value into a table cell, called with the
 *            TD DOM node, field value and data object as arguments
 *            (in that order)
 */
RapidContext.Widget.TableColumn.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
    var locals = RapidContext.Util.mask(attrs, ["title", "field", "type", "sort", "maxLength", "key", "tooltip", "renderer"]);
    if (typeof(locals.title) !== "undefined") {
        MochiKit.DOM.replaceChildNodes(this, locals.title);
    }
    if (typeof(locals.field) !== "undefined") {
        this.field = locals.field;
    }
    if (typeof(locals.type) !== "undefined") {
        this.type = locals.type;
    }
    if (typeof(locals.sort) !== "undefined") {
        this.sort = locals.sort;
        if (locals.sort == null || locals.sort == "none") {
            MochiKit.DOM.removeElementClass(this, "sortAsc");
            MochiKit.DOM.removeElementClass(this, "sortDesc");
        } else if (locals.sort == "desc") {
            MochiKit.DOM.removeElementClass(this, "sortAsc");
            MochiKit.DOM.addElementClass(this, "sortDesc");
        } else {
            MochiKit.DOM.removeElementClass(this, "sortDesc");
            MochiKit.DOM.addElementClass(this, "sortAsc");
        }
    }
    if (typeof(locals.maxLength) !== "undefined") {
        this.maxLength = parseInt(locals.maxLength);
    }
    if (typeof(locals.key) !== "undefined") {
        this.key = MochiKit.Base.bool(locals.key);
    }
    if (typeof(locals.tooltip) !== "undefined") {
        this.title = locals.tooltip;
    }
    if (typeof(locals.renderer) === "function") {
        this.renderer = locals.renderer;
    }
    this.__setAttrs(attrs);
};

/**
 * Maps the column field from one object onto another. This method
 * will also convert the data depending on the column data type.
 *
 * @param src                the source object (containing the field)
 * @param dst                the destination object
 */
RapidContext.Widget.TableColumn.prototype._map = function (src, dst) {
    var value = src[this.field];
    if (value != null) {
        if (this.key) {
            dst.$id = value;
        }
        switch (this.type) {
        case "number":
            if (value instanceof Number) {
                value = value.valueOf();
            } else if (typeof(value) != "number") {
                value = parseFloat(value);
            }
            break;
        case "date":
            if (/^@\d+$/.test(value)) {
                value = new Date(+value.substr(1));
            }
            if (value instanceof Date) {
                value = MochiKit.DateTime.toISODate(value);
            } else {
                value = MochiKit.Text.truncate(value, 10);
            }
            break;
        case "datetime":
            if (/^@\d+$/.test(value)) {
                value = new Date(+value.substr(1));
            }
            if (value instanceof Date) {
                value = MochiKit.DateTime.toISOTimestamp(value);
            } else {
                value = MochiKit.Text.truncate(value, 19);
            }
            break;
        case "time":
            if (/^@\d+$/.test(value)) {
                value = new Date(+value.substr(1));
            }
            if (value instanceof Date) {
                value = MochiKit.DateTime.toISOTime(value);
            } else {
                if (typeof(value) !== "string") {
                    value = value.toString();
                }
                if (value.length > 8) {
                    value = value.substring(value.length - 8);
                }
            }
            break;
        case "boolean":
            if (typeof(value) !== "boolean") {
                value = MochiKit.Base.bool(value);
            }
            break;
        case "string":
            if (typeof(value) !== "string") {
                value = value.toString();
            }
            break;
        }
    }
    dst[this.field] = value;
};

/**
 * Renders the column field value into a table cell.
 *
 * @param obj                the data object (containing the field)
 *
 * @return the table cell DOM node
 */
RapidContext.Widget.TableColumn.prototype._render = function (obj) {
    var td = MochiKit.DOM.TD();
    var value = obj[this.field];
    if (typeof(this.renderer) === "function") {
        try {
            this.renderer(td, value, obj.$data);
        } catch (e) {
            td.appendChild(RapidContext.Util.createTextNode(e));
        }
    } else if (typeof(value) == "boolean") {
        td.appendChild(RapidContext.Widget.Icon({ ref: value ? "YES" : "NO" }));
    } else {
        if (value == null || (typeof(value) == "number" && isNaN(value))) {
            value = "";
        } else if (typeof(value) != "string") {
            value = value.toString();
        }
        if (this.maxLength && this.maxLength < value.length) {
            td.title = value;
            value = MochiKit.Text.truncate(value, this.maxLength, "...");
        }
        td.appendChild(RapidContext.Util.createTextNode(value));
    }
    return td;
};

/**
 * Handles click events on the column header.
 */
RapidContext.Widget.TableColumn.prototype._handleClick = function () {
    if (this.parentNode != null) {
        var dir = (this.sort == "asc") ? "desc" : "asc";
        var tr = this.parentNode;
        var thead = tr.parentNode;
        var table = thead.parentNode;
        table.parentNode.sortData(this.field, dir);
    }
};
