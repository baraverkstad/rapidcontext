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
 * Creates a new data table column widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {string} attrs.title the column title
 * @param {string} attrs.field the data property name
 * @param {string} [attrs.type] the data property type, one of
 *            "string", "number", "date", "time", "datetime",
 *            "boolean" or "object" (defaults to "string")
 * @param {string} [attrs.sort] the sort direction, one of "asc",
 *            "desc", "none" (disabled) or null (unsorted)
 * @param {number} [attrs.maxLength] the maximum data length,
 *            overflow will be displayed as a tooltip, only used by
 *            the default renderer
 * @param {boolean} [attrs.key] the unique key value flag, only to be
 *            set for a single column per table
 * @param {string} [attrs.tooltip] the tooltip text to display on the
 *            column header
 * @param {string} [attrs.cellStyle] the CSS styles or class names to set on
 *            the rendered cells
 * @param {function} [attrs.renderer] the function that renders the converted
 *            data value into a table cell, called as
 *            `renderer(<td>, value, data)` with the DOM node, field value and
 *            data object as arguments
 *
 * @return {Widget} the widget DOM node
 *
 * @class The table column widget class. Used to provide a sortable data table
 *     column, using a `<th>` HTML element for the header (and rendering data
 *     to `<td>` HTML elements).
 * @extends RapidContext.Widget
 *
 * @example <caption>JavaScript</caption>
 * var attrs1 = { field: "id", title: "Identifier", key: true, type: "number" };
 * var attrs2 = { field: "name", title: "Name", maxLength: 50, sort: "asc" };
 * var attrs3 = { field: "modified", title: "Last Modified", type: "datetime" };
 * var col1 = RapidContext.Widget.TableColumn(attrs1);
 * var col2 = RapidContext.Widget.TableColumn(attrs2);
 * var col3 = RapidContext.Widget.TableColumn(attrs3);
 * var exampleTable = RapidContext.Widget.Table({}, col1, col2, col3);
 * RapidContext.Util.registerSizeConstraints(exampleTable, "50%", "100%");
 *
 * @example <caption>User Interface XML</caption>
 * <Table id="exampleTable" w="50%" h="100%">
 *   <TableColumn field="id" title="Identifier" key="true" type="number" />
 *   <TableColumn field="name" title="Name" maxLength="50" sort="asc" />
 *   <TableColumn field="modified" title="Last Modified" type="datetime" />
 * </Table>
 */
RapidContext.Widget.TableColumn = function (attrs) {
    if (attrs.field == null) {
        throw new Error("The 'field' attribute cannot be null for a TableColumn");
    }
    var o = document.createElement("th");
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.TableColumn);
    o.addClass("widgetTableColumn");
    o.setAttrs(Object.assign({ title: attrs.field, type: "string", key: false }, attrs));
    o.on("click", o._handleClick);
    return o;
};

// Register widget class
RapidContext.Widget.Classes.TableColumn = RapidContext.Widget.TableColumn;

/**
 * Returns the widget container DOM node.
 *
 * @return {Node} returns null, since child nodes are not supported
 */
RapidContext.Widget.TableColumn.prototype._containerNode = function () {
    return null;
};

/**
 * Updates the widget or HTML DOM node attributes. Note that some
 * updates will not take effect until the parent table is cleared
 * or data is reloaded.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {string} [attrs.title] the column title
 * @param {string} [attrs.field] the data property name
 * @param {string} [attrs.type] the data property type, one of
 *            "string", "number", "date", "time", "datetime",
 *            "boolean" or "object"
 * @param {string} [attrs.sort] the sort direction, one of "asc",
 *            "desc", "none" (disabled) or null (unsorted)
 * @param {number} [attrs.maxLength] the maximum data length,
 *            overflow will be displayed as a tooltip, only used by
 *            the default renderer
 * @param {boolean} [attrs.key] the unique key value flag, only to be
 *            set for a single column per table
 * @param {string} [attrs.tooltip] the tooltip text to display on the
 *            column header
 * @param {string} [attrs.cellStyle] the CSS styles or class names to set on
 *            the rendered cells
 * @param {function} [attrs.renderer] the function that renders the converted
 *            data value into a table cell, called as
 *            `renderer(<td>, value, data)` with the DOM node, field value and
 *            data object as arguments
 */
RapidContext.Widget.TableColumn.prototype.setAttrs = function (attrs) {
    attrs = Object.assign({}, attrs);
    if ("title" in attrs) {
        this.innerText = attrs.title;
        delete attrs.title;
    }
    if ("sort" in attrs) {
        this.classList.toggle("sortNone", attrs.sort == "none");
        this.classList.toggle("sortDesc", attrs.sort == "desc");
        this.classList.toggle("sortAsc", attrs.sort == "asc");
    }
    if ("maxLength" in attrs) {
        attrs.maxLength = parseInt(attrs.maxLength, 10) || null;
    }
    if ("key" in attrs) {
        attrs.key = MochiKit.Base.bool(attrs.key);
    }
    if ("tooltip" in attrs) {
        attrs.title = attrs.tooltip;
        delete attrs.tooltip;
    }
    if ("renderer" in attrs) {
        let valid = typeof(attrs.renderer) === "function";
        attrs.renderer = valid ? attrs.renderer : null;
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
    var td = document.createElement("td");
    var value = obj[this.field];
    if (typeof(this.cellStyle) === "string" && this.cellStyle.includes(":")) {
        td.style = this.cellStyle;
    } else if (typeof(this.cellStyle) === "string") {
        td.className = this.cellStyle;
    }
    if (typeof(this.renderer) === "function") {
        try {
            this.renderer(td, value, obj.$data);
        } catch (e) {
            td.append(e.toString());
        }
    } else if (typeof(value) == "boolean") {
        var css = value ? "fa fa-check-square" : "fa fa-square-o";
        td.append(RapidContext.Widget.Icon(css));
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
        td.append(value);
    }
    return td;
};

/**
 * Handles click events on the column header.
 */
RapidContext.Widget.TableColumn.prototype._handleClick = function () {
    let table = this.closest(".widget.widgetTable");
    let dir = (this.sort == "asc") ? "desc" : "asc";
    table && table.sortData(this.field, dir);
};
