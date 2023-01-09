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
 * Creates a new data table widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {String} [attrs.select] the row selection mode ('none', 'one' or
 *            'multiple'), defaults to 'one'
 * @param {String} [attrs.key] the unique key identifier column field,
 *            defaults to null
 * @param {Boolean} [attrs.hidden] the hidden widget flag, defaults to false
 * @param {Widget} [...] the child table columns
 *
 * @return {Widget} the widget DOM node
 *
 * @class The table widget class. Used to provide a sortable and scrolling
 *     data table, using an outer `<div>` HTML element around a `<table>`. The
 *     `Table` widget can only have `TableColumn` child nodes, each providing a
 *     visible data column in the table.
 * @extends RapidContext.Widget
 *
 * @example {JavaScript}
 * var attrs1 = { field: "id", title: "Identifier", key: true, type: "number" };
 * var attrs2 = { field: "name", title: "Name", maxLength: 50, sort: "asc" };
 * var attrs3 = { field: "modified", title: "Last Modified", type: "datetime" };
 * var col1 = RapidContext.Widget.TableColumn(attrs1);
 * var col2 = RapidContext.Widget.TableColumn(attrs2);
 * var col3 = RapidContext.Widget.TableColumn(attrs3);
 * var exampleTable = RapidContext.Widget.Table({}, col1, col2, col3);
 * RapidContext.Util.registerSizeConstraints(exampleTable, "50%", "100%");
 *
 * @example {User Interface XML}
 * <Table id="exampleTable" w="50%" h="100%">
 *   <TableColumn field="id" title="Identifier" key="true" type="number" />
 *   <TableColumn field="name" title="Name" maxLength="50" sort="asc" />
 *   <TableColumn field="modified" title="Last Modified" type="datetime" />
 * </Table>
 */
RapidContext.Widget.Table = function (attrs/*, ...*/) {
    var thead = MochiKit.DOM.THEAD({}, MochiKit.DOM.TR());
    var tbody = MochiKit.DOM.TBODY();
    var table = MochiKit.DOM.TABLE({ "class": "widgetTable" }, thead, tbody);
    var o = MochiKit.DOM.DIV({}, table);
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.Table);
    MochiKit.DOM.addElementClass(o, "widgetTable");
    o.resizeContent = o._resizeContent;
    o._rows = [];
    o._data = null;
    o._keyField = null;
    o._selected = [];
    o._selectMode = "one";
    o._mouseX = 0;
    o._mouseY = 0;
    o.setAttrs(attrs);
    o.addAll(Array.from(arguments).slice(1));
    o.addEventListener("mousedown", o._handleMouseDown);
    o.addEventListener("click", o._handleClick);
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Table = RapidContext.Widget.Table;

/**
 * Emitted when the table data is cleared.
 *
 * @name RapidContext.Widget.Table#onclear
 * @event
 */

/**
 * Emitted when the table selection changes.
 *
 * @name RapidContext.Widget.Table#onselect
 * @event
 */

/**
 * Destroys this widget.
 */
RapidContext.Widget.Table.prototype.destroy = function () {
    // FIXME: Use AbortSignal instead to disconnect
    this.addEventListener("mousedown", this._handleMouseDown);
    this.addEventListener("click", this._handleClick);
};

/**
 * Returns the widget container DOM node.
 *
 * @return {Node} the container DOM node
 */
RapidContext.Widget.Table.prototype._containerNode = function () {
    var table = this.firstChild;
    var thead = table.firstChild;
    var tr = thead.firstChild;
    return tr;
};

/**
 * Handles the mouse down event to stop text selection in some cases.
 *
 * @param {Event} evt the DOM Event object
 */
RapidContext.Widget.Table.prototype._handleMouseDown = function (evt) {
    if (evt.ctrlKey || evt.metaKey || evt.shiftKey) {
        evt.preventDefault();
    }
};

/**
 * Handles the click event to change selected rows.
 *
 * @param {Event} evt the DOM Event object
 */
RapidContext.Widget.Table.prototype._handleClick = function (evt) {
    var tr = $(evt.target).closest("tbody > tr").get(0);
    var row = tr && (tr.rowIndex - 1);
    var isMulti = tr && this._selectMode === "multiple";
    var isSingle = tr && this._selectMode !== "none";
    if (isMulti && (evt.ctrlKey || evt.metaKey)) {
        evt.preventDefault();
        var pos = MochiKit.Base.findIdentical(this._selected, row);
        if (pos >= 0) {
            this._unmarkSelection(row);
            this._selected.splice(pos, 1);
        } else {
            this._selected.push(row);
            this._markSelection(row);
        }
        this._dispatch("select");
    } else if (isMulti && evt.shiftKey) {
        evt.preventDefault();
        this._unmarkSelection();
        this._selected.push(row);
        var start = this._selected[0];
        this._selected = [];
        var step = (row >= start) ? 1 : -1;
        for (var i = start; (step > 0) ? i <= row : i >= row; i += step) {
            this._selected.push(i);
        }
        this._markSelection();
        this._dispatch("select");
    } else if (isSingle) {
        this._unmarkSelection();
        this._selected = [row];
        this._markSelection();
        this._dispatch("select");
    }
};

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {String} [attrs.select] the row selection mode ('none', 'one' or
 *            'multiple')
 * @param {String} [attrs.key] the unique key identifier column field
 * @param {Boolean} [attrs.hidden] the hidden widget flag
 */
RapidContext.Widget.Table.prototype.setAttrs = function (attrs) {
    attrs = Object.assign({}, attrs);
    var locals = RapidContext.Util.mask(attrs, ["select", "key"]);
    if (typeof(locals.select) != "undefined") {
        this._selectMode = locals.select;
    }
    if (typeof(locals.key) != "undefined") {
        this.setIdKey(locals.key);
    }
    this.__setAttrs(attrs);
};

/**
 * Adds a single child table column widget to this widget.
 *
 * @param {Widget} child the table column widget to add
 */
RapidContext.Widget.Table.prototype.addChildNode = function (child) {
    if (!RapidContext.Widget.isWidget(child, "TableColumn")) {
        throw new Error("Table widget can only have TableColumn children");
    }
    this.clear();
    this._containerNode().appendChild(child);
};

/**
 * Removes a single child table column widget from this widget.
 * This will also clear all the data in the table.
 *
 * @param {Widget} child the table column widget to remove
 */
RapidContext.Widget.Table.prototype.removeChildNode = function (child) {
    this.clear();
    this._containerNode().removeChild(child);
};

/**
 * Returns the column index of a field.
 *
 * @param {String} field the field name
 *
 * @return {Number} the column index, or
 *         -1 if not found
 */
RapidContext.Widget.Table.prototype.getColumnIndex = function (field) {
    var cols = this.getChildNodes();
    for (var i = 0; i < cols.length; i++) {
        if (cols[i].field === field) {
            return i;
        }
    }
    return -1;
};

/**
 * Returns the unique key identifier column field, or null if none
 * was set.
 *
 * @return {String} the key column field name, or
 *         null for none
 */
RapidContext.Widget.Table.prototype.getIdKey = function () {
    if (this._keyField) {
        return this._keyField;
    }
    var cols = this.getChildNodes();
    for (var i = 0; i < cols.length; i++) {
        if (cols[i].key) {
            return cols[i].field;
        }
    }
    return null;
};

/**
 * Sets the unique key identifier column field. Note that this
 * method will regenerate all row identifiers if the table already
 * contains data.
 *
 * @param {String} key the new key column field name
 */
RapidContext.Widget.Table.prototype.setIdKey = function (key) {
    this._keyField = key;
    for (var i = 0; this._rows != null && i < this._rows.length; i++) {
        var row = this._rows[i];
        if (this._keyField != null && row.$data[this._keyField] != null) {
            row.$id = row.$data[this._keyField];
        }
    }
};

/**
 * Returns the current sort key for the table.
 *
 * @return {String} the current sort field, or
 *         null for none
 */
RapidContext.Widget.Table.prototype.getSortKey = function () {
    var cols = this.getChildNodes();
    for (var i = 0; i < cols.length; i++) {
        if (cols[i].sort != null && cols[i].sort != "none") {
            return cols[i].field;
        }
    }
    return null;
};

/**
 * Returns a table cell element.
 *
 * @param {Number} row the row index
 * @param {Number} col the column index
 *
 * @return {Node} the table cell element node, or
 *         null if not found
 */
RapidContext.Widget.Table.prototype.getCellElem = function (row, col) {
    try {
        var table = this.firstChild;
        var tbody = table.lastChild;
        return tbody.childNodes[row].childNodes[col];
    } catch (e) {
        return null;
    }
};

/**
 * Clears all the data in the table. The column headers will not be
 * affected by this method. Use `removeAll()` or `removeChildNode()` to
 * also remove columns.
 */
RapidContext.Widget.Table.prototype.clear = function () {
    this.setData([]);
};

/**
 * Returns an array with the data in the table. The array returned
 * should correspond exactly to the one previously set, i.e. it has
 * not been sorted or modified in other ways.
 *
 * @return {Array} an array with the data in the table
 */
RapidContext.Widget.Table.prototype.getData = function () {
    return this._data;
};

/**
 * Sets the table data. The table data is an array of objects, each
 * having properties corresponding to the table column fields. Any
 * object property not mapped to a table column will be ignored (i.e.
 * a hidden column). See the `TableColumn` class for data mapping
 * details. Note that automatically generated row ids will be reset
 * by this method and any selection on such tables is lost.
 *
 * @param {Array} data an array with data objects
 *
 * @example
 * var data = [
 *     { id: 1, name: "John Doe", modified: "@1300000000000" },
 *     { id: 2, name: "First Last", modified: new Date() },
 *     { id: 3, name: "Another Name", modified: "2004-11-30 13:33:20" }
 * ];
 * table.setData(data);
 */
RapidContext.Widget.Table.prototype.setData = function (data) {
    var cols = this.getChildNodes();
    var selectedIds = this.getSelectedIds();
    this._dispatch("clear");
    this._data = data;
    this._rows = [];
    this._selected = [];
    for (var i = 0; data != null && i < data.length; i++) {
        var row = { $id: "id" + i, $data: data[i] };
        for (var j = 0; j < cols.length; j++) {
            cols[j]._map(data[i], row);
        }
        if (this._keyField != null && data[i][this._keyField] != null) {
            row.$id = data[i][this._keyField];
        }
        this._rows.push(row);
    }
    var key = this.getSortKey();
    if (key) {
        this.sortData(key);
    } else {
        this._renderRows();
    }
    if (this.getIdKey() != null) {
        this._addSelectedIds(selectedIds);
    }
};

/**
 * Sorts the table data by field and direction.
 *
 * @param {String} field the sort field
 * @param {String} [direction] the sort direction, either "asc" or
 *            "desc"
 */
RapidContext.Widget.Table.prototype.sortData = function (field, direction) {
    var cols = this.getChildNodes();
    var selectedIds = this.getSelectedIds();
    this._selected = [];
    for (var i = 0; i < cols.length; i++) {
        if (cols[i].field === field) {
            if (cols[i].sort == "none") {
                // Skip sorting if not allowed
                return;
            } else if (direction == null) {
                direction = cols[i].sort || "asc";
            }
            cols[i].setAttrs({ sort: direction });
        } else if (cols[i].sort != "none") {
            cols[i].setAttrs({ sort: null });
        }
    }
    this._rows.sort(MochiKit.Base.keyComparator(field));
    if (direction == "desc") {
        this._rows.reverse();
    }
    this._renderRows();
    this._addSelectedIds(selectedIds);
};

/**
 * Redraws the table from updated source data. Note that this method
 * will not add or remove rows and keeps the current row order
 * intact. For a more complete redraw of the table, use `setData()`.
 */
RapidContext.Widget.Table.prototype.redraw = function () {
    var cols = this.getChildNodes();
    for (var i = 0; i < this._rows.length; i++) {
        var row = this._rows[i];
        for (var j = 0; j < cols.length; j++) {
            cols[j]._map(row.$data, row);
        }
    }
    this._renderRows();
    for (var k = 0; k < this._selected.length; k++) {
        this._markSelection(this._selected[k]);
    }
};

/**
 * Renders the table rows.
 */
RapidContext.Widget.Table.prototype._renderRows = function () {
    var cols = this.getChildNodes();
    var tbody = this.firstChild.lastChild;
    MochiKit.DOM.replaceChildNodes(tbody, this._rows.map(function (row) {
        return MochiKit.DOM.TR({}, cols.map(function (col) {
            return col._render(row);
        }));
    }));
    if (this._rows.length == 0) {
        // Add empty row to avoid browser bugs
        tbody.appendChild(MochiKit.DOM.TR());
    }
};

/**
 * Returns the number of rows in the table. This is a convenience
 * method for `getData().length`.
 *
 * @return {Number} the number of table rows
 */
RapidContext.Widget.Table.prototype.getRowCount = function () {
    return this._rows.length;
};

/**
 * Returns the row id for the specified row index. If the row index
 * is out of bounds, null will be returned. The row ids are the data
 * values from the key column, or automatically generated internal
 * values if no key column is set. Note that the row index uses the
 * current table sort order.
 *
 * @param {Number} index the row index, 0 <= index < row count
 *
 * @return {String} the unique row id, or null if not found
 */
RapidContext.Widget.Table.prototype.getRowId = function (index) {
    var row = this._rows[index];
    return row ? row.$id : null;
};

/**
 * Returns the currently selected row ids. If no rows are selected,
 * an empty array will be returned. The row ids are the data values
 * from the key column, or automatically generated internal values
 * if no key column is set.
 *
 * @return {Array} an array with the selected row ids
 */
RapidContext.Widget.Table.prototype.getSelectedIds = function () {
    var rows = this._rows;
    return this._selected.map(function (idx) {
        return rows[idx].$id;
    });
};

/**
 * Returns the currently selected row data.
 *
 * @return {Object/Array} the data row selected, or
 *         an array of selected data rows if multiple selection is enabled
 */
RapidContext.Widget.Table.prototype.getSelectedData = function () {
    var rows = this._rows;
    var data = this._selected.map(function (idx) {
        return rows[idx].$data;
    });
    return (this._selectMode === "multiple") ? data : data[0];
};

/**
 * Sets the selection to the specified row id values. If the current
 * selection is changed the select signal will be emitted.
 *
 * @param {String/Array} [...] the row ids or array with ids to select
 *
 * @return {Array} an array with the row ids actually modified
 */
RapidContext.Widget.Table.prototype.setSelectedIds = function () {
    var args = MochiKit.Base.flattenArguments(arguments);
    var ids = RapidContext.Util.dict(args, true);
    var oldIds = RapidContext.Util.dict(this.getSelectedIds(), true);
    var res = [];
    for (var i = 0; i < this._rows.length; i++) {
        var rowId = this._rows[i].$id;
        if (ids[rowId] && !oldIds[rowId]) {
            this._selected.push(i);
            this._markSelection(i);
            res.push(rowId);
        } else if (!ids[rowId] && oldIds[rowId]) {
            var pos = MochiKit.Base.findIdentical(this._selected, i);
            if (pos >= 0) {
                this._selected.splice(pos, 1);
                this._unmarkSelection(i);
                res.push(rowId);
            }
        }
    }
    if (res.length > 0) {
        this._dispatch("select");
    }
    return res;
};

/**
 * Adds the specified row id values to the selection. If the current
 * selection is changed the select signal will be emitted.
 *
 * @param {String/Array} [...] the row ids or array with ids to select
 *
 * @return {Array} an array with the new row ids actually selected
 */
RapidContext.Widget.Table.prototype.addSelectedIds = function () {
    var res = this._addSelectedIds(arguments);
    if (res.length > 0) {
        this._dispatch("select");
    }
    return res;
};

/**
 * Adds the specified row id values to the selection. Note that this
 * method does not emit any selection signal.
 *
 * @param {String/Array} [...] the row ids or array with ids to select
 *
 * @return {Array} an array with the new row ids actually selected
 */
RapidContext.Widget.Table.prototype._addSelectedIds = function () {
    var args = MochiKit.Base.flattenArguments(arguments);
    var ids = RapidContext.Util.dict(args, true);
    var res = [];
    Object.assign(ids, RapidContext.Util.dict(this.getSelectedIds(), false));
    for (var i = 0; i < this._rows.length; i++) {
        if (ids[this._rows[i].$id]) {
            this._selected.push(i);
            this._markSelection(i);
            res.push(this._rows[i].$id);
        }
    }
    return res;
};

/**
 * Removes the specified row id values from the selection. If the
 * current selection is changed the select signal will be emitted.
 *
 * @param {String/Array} [...] the row ids or array with ids to unselect
 *
 * @return {Array} an array with the row ids actually unselected
 */
RapidContext.Widget.Table.prototype.removeSelectedIds = function () {
    var args = MochiKit.Base.flattenArguments(arguments);
    var ids = RapidContext.Util.dict(args, true);
    var res = [];
    for (var i = 0; i < this._rows.length; i++) {
        if (ids[this._rows[i].$id]) {
            var pos = MochiKit.Base.findIdentical(this._selected, i);
            if (pos >= 0) {
                this._selected.splice(pos, 1);
                this._unmarkSelection(i);
                res.push(this._rows[i].$id);
            }
        }
    }
    if (res.length > 0) {
        this._dispatch("select");
    }
    return res;
};

/**
 * Marks selected rows.
 *
 * @param {Number} [index] the row index, or null to mark all
 */
RapidContext.Widget.Table.prototype._markSelection = function (index) {
    var tbody = this.firstChild.lastChild;
    var indices = (index == null) ? this._selected : [index];
    indices.forEach(function (idx) {
        MochiKit.DOM.addElementClass(tbody.childNodes[idx], "selected");
    });
};

/**
 * Unmarks selected rows.
 *
 * @param {Number} [index] the row index, or null to unmark all
 */
RapidContext.Widget.Table.prototype._unmarkSelection = function (index) {
    var tbody = this.firstChild.lastChild;
    var indices = (index == null) ? this._selected : [index];
    indices.forEach(function (idx) {
        MochiKit.DOM.removeElementClass(tbody.childNodes[idx], "selected");
    });
};

/**
 * Called when table content should be resized. This method is also called when
 * the widget is made visible in a container after being hidden.
 */
RapidContext.Widget.Table.prototype._resizeContent = function () {
    // Work-around to restore scrollTop for WebKit browsers
    if (this.scrollTop == 0 && this._selected.length > 0) {
        var index = this._selected[0];
        var tbody = this.firstChild.lastChild;
        var tr = tbody.childNodes[index];
        var h = this.clientHeight;
        var y = tr.offsetTop + tr.offsetHeight;
        this.scrollTop = Math.round(y - h / 2);
    }
    var thead = this.firstChild.firstChild;
    RapidContext.Util.resizeElements(thead);
};
