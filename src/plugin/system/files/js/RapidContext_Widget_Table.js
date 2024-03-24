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
 * Creates a new data table widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {string} [attrs.select] the row selection mode ('none', 'one',
 *            'multiple' or 'auto'), defaults to 'one'
 * @param {string} [attrs.key] the unique key identifier column field,
 *            defaults to null
 * @param {boolean} [attrs.hidden] the hidden widget flag, defaults to false
 * @param {...TableColumn} [child] the child table columns
 *
 * @return {Widget} the widget DOM node
 *
 * @class The table widget class. Used to provide a sortable and scrolling
 *     data table, using an outer `<div>` HTML element around a `<table>`. The
 *     `Table` widget can only have `TableColumn` child nodes, each providing a
 *     visible data column in the table.
 * @extends RapidContext.Widget
 *
 * @example <caption>JavaScript</caption>
 * let attrs1 = { field: "id", title: "Identifier", key: true, type: "number" };
 * let attrs2 = { field: "name", title: "Name", maxLength: 50, sort: "asc" };
 * let attrs3 = { field: "modified", title: "Last Modified", type: "datetime" };
 * let col1 = RapidContext.Widget.TableColumn(attrs1);
 * let col2 = RapidContext.Widget.TableColumn(attrs2);
 * let col3 = RapidContext.Widget.TableColumn(attrs3);
 * let exampleTable = RapidContext.Widget.Table({}, col1, col2, col3);
 *
 * @example <caption>User Interface XML</caption>
 * <Table id="exampleTable" w="50%" h="100%">
 *   <TableColumn field="id" title="Identifier" key="true" type="number" />
 *   <TableColumn field="name" title="Name" maxLength="50" sort="asc" />
 *   <TableColumn field="modified" title="Last Modified" type="datetime" />
 * </Table>
 */
RapidContext.Widget.Table = function (attrs/*, ...*/) {
    let thead = RapidContext.UI.THEAD({}, document.createElement("tr"));
    let tbody = RapidContext.UI.TBODY();
    let table = RapidContext.UI.TABLE({ "class": "widgetTable" }, thead, tbody);
    let o = RapidContext.UI.DIV({}, table);
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.Table);
    o.classList.add("widgetTable");
    o._data = [];
    o._rows = [];
    o._keyField = null;
    o._selected = [];
    o._selectMode = "one";
    o._mouseX = 0;
    o._mouseY = 0;
    o.setAttrs(attrs);
    o.addAll(Array.from(arguments).slice(1));
    o.on("mousedown", o._handleMouseDown);
    o.on("click", o._handleClick);
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
 * Returns the widget container DOM node.
 *
 * @return {Node} the container DOM node
 */
RapidContext.Widget.Table.prototype._containerNode = function () {
    let table = this.firstChild;
    let thead = table.firstChild;
    let tr = thead.firstChild;
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
    if (evt.target.closest("a[href]")) {
        return;
    }
    let tr = evt.target.closest(".widgetTable > tbody > tr");
    let row = tr && (tr.rowIndex - 1);
    let isMulti = tr && this._selectMode === "multiple";
    let isSingle = tr && this._selectMode !== "none";
    if (isMulti && (evt.ctrlKey || evt.metaKey)) {
        evt.preventDefault();
        let pos = this._selected.indexOf(row);
        if (pos >= 0) {
            this._unmarkSelection(row);
            this._selected.splice(pos, 1);
        } else {
            this._selected.push(row);
            this._markSelection(row);
        }
        this.emit("select");
    } else if (isMulti && evt.shiftKey) {
        evt.preventDefault();
        this._unmarkSelection();
        this._selected.push(row);
        let start = this._selected[0];
        this._selected = [];
        let step = (row >= start) ? 1 : -1;
        for (let i = start; (step > 0) ? i <= row : i >= row; i += step) {
            this._selected.push(i);
        }
        this._markSelection();
        this.emit("select");
    } else if (isSingle) {
        this._unmarkSelection();
        this._selected = [row];
        this._markSelection();
        this.emit("select");
    }
};

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {string} [attrs.select] the row selection mode ('none', 'one' or
 *            'multiple')
 * @param {string} [attrs.key] the unique key identifier column field
 * @param {boolean} [attrs.hidden] the hidden widget flag
 */
RapidContext.Widget.Table.prototype.setAttrs = function (attrs) {
    attrs = Object.assign({}, attrs);
    if ("select" in attrs) {
        this._selectMode = attrs.select;
    }
    if ("key" in attrs) {
        this.setIdKey(attrs.key);
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
    this._containerNode().append(child);
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
 * @param {string} field the field name
 *
 * @return {number} the column index, or
 *         -1 if not found
 */
RapidContext.Widget.Table.prototype.getColumnIndex = function (field) {
    let cols = this.getChildNodes();
    return cols.findIndex((col) => col.field === field);
};

/**
 * Returns the unique key identifier column field, or null if none
 * was set.
 *
 * @return {string} the key column field name, or
 *         null for none
 */
RapidContext.Widget.Table.prototype.getIdKey = function () {
    if (this._keyField) {
        return this._keyField;
    }
    for (let col of this.getChildNodes()) {
        if (col.key) {
            return col.field;
        }
    }
    return null;
};

/**
 * Sets the unique key identifier column field. Note that this
 * method will regenerate all row identifiers if the table already
 * contains data.
 *
 * @param {string} key the new key column field name
 */
RapidContext.Widget.Table.prototype.setIdKey = function (key) {
    this._keyField = key;
    for (let row of this._rows) {
        if (this._keyField && row.$data[this._keyField] != null) {
            row.$id = row.$data[this._keyField];
        }
    }
};

/**
 * Returns the current sort key for the table.
 *
 * @return {string} the current sort field, or
 *         null for none
 */
RapidContext.Widget.Table.prototype.getSortKey = function () {
    for (let col of this.getChildNodes()) {
        if (col.sort && col.sort != "none") {
            return col.field;
        }
    }
    return null;
};

/**
 * Returns a table cell element.
 *
 * @param {number} row the row index
 * @param {number} col the column index
 *
 * @return {Node} the table cell element node, or
 *         null if not found
 */
RapidContext.Widget.Table.prototype.getCellElem = function (row, col) {
    try {
        let table = this.firstChild;
        let tbody = table.lastChild;
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
 * normally correspond exactly to the one previously set, i.e. it
 * has not been sorted or modified in other ways. If `updateData()`
 * is called however, a new data array is created to match current
 * rows.
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
 * let data = [
 *     { id: 1, name: "John Doe", modified: "@1300000000000" },
 *     { id: 2, name: "First Last", modified: new Date() },
 *     { id: 3, name: "Another Name", modified: "2004-11-30 13:33:20" }
 * ];
 * table.setData(data);
 */
RapidContext.Widget.Table.prototype.setData = function (data) {
    let columns = this.getChildNodes();
    let key = this.getIdKey() || "$id";
    let selectedIds = key ? this.getSelectedIds() : [];
    this.emit("clear");
    this._data = data || [];
    this._rows = this._data.map((obj, idx) => this._mapRow(columns, key, obj, idx));
    this._selected = [];
    let sort = this.getSortKey();
    if (sort) {
        this.sortData(sort);
    } else {
        this._renderRows();
    }
    if (this._selectMode !== "none") {
        let isAuto = this._selectMode === "auto" && this._rows.length === 1;
        if (isAuto && !selectedIds.includes(this._rows[0].$id)) {
            this.addSelectedIds(this._rows[0].$id);
        } else {
            this._addSelectedIds(selectedIds);
        }
    }
};

/**
 * Updates one or more rows of table data. Data is matched to
 * existing rows either via the key identifier field or object
 * identity. Any matching rows will be mapped and re-rendered
 * accordingly. Non-matching data will be be ignored.
 *
 * @param {Array|Object} data an array with data or a single data object
 *
 * @example
 * table.updateData({ id: 2, name: "New Name", modified: new Date() });
 */
RapidContext.Widget.Table.prototype.updateData = function (data) {
    data = Array.isArray(data) ? data : [data];
    let columns = this.getChildNodes();
    let key = this.getIdKey() || "$id";
    for (let obj of data) {
        let idx = this._rows.findIndex((o) => o.$id === obj[key] || o.$data === obj);
        if (idx >= 0) {
            let row = this._rows[idx] = this._mapRow(columns, key, obj, idx);
            let tr = document.createElement("tr");
            tr.append(...columns.map((col) => col._render(row)));
            let tbody = this.firstChild.lastChild;
            tbody.children[idx].replaceWith(tr);
        }
    }
    this._data = this._rows.map((o) => o.$data);
    for (let sel of this._selected) {
        this._markSelection(sel);
    }
};

/**
 * Creates a data row by mapping an object according to specified
 * columns. Also extracts or creates an '$id' property and maps the
 * source data to '$data'.
 *
 * @param {Array} columns the array of columns to map
 * @param {String} key the id field, or null if not set
 * @param {Object} obj the object with data to map
 * @param {number} idx the row index for automatic id creation
 *
 * @return {Object} the data row object created
 */
RapidContext.Widget.Table.prototype._mapRow = function (columns, key, obj, idx) {
    let id = (key && obj[key] != null) ? obj[key] : "id" + idx;
    let row = { $id: id, $data: obj };
    for (let col of columns) {
        row[col.field] = col._map(obj);
    }
    return row;
};

/**
 * Sorts the table data by field and direction.
 *
 * @param {string} field the sort field
 * @param {string} [direction] the sort direction, either "asc" or
 *            "desc"
 */
RapidContext.Widget.Table.prototype.sortData = function (field, direction) {
    let selectedIds = this.getSelectedIds();
    this._selected = [];
    for (let col of this.getChildNodes()) {
        if (col.sort != "none") {
            if (col.field === field) {
                direction = direction || col.sort || "asc";
                col.setAttrs({ sort: direction });
            } else {
                col.setAttrs({ sort: null });
            }
        }
    }
    this._rows.sort(RapidContext.Data.compare((o) => o[field]));
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
    let cols = this.getChildNodes();
    for (let row of this._rows) {
        for (let col of cols) {
            row[col.field] = col._map(row.$data);
        }
    }
    this._renderRows();
    for (let sel of this._selected) {
        this._markSelection(sel);
    }
};

/**
 * Renders the table rows.
 */
RapidContext.Widget.Table.prototype._renderRows = function () {
    let cols = this.getChildNodes();
    let tbody = this.firstChild.lastChild;
    tbody.innerHTML = "";
    for (let row of this._rows) {
        let tr = document.createElement("tr");
        tr.append(...cols.map((col) => col._render(row)));
        tbody.append(tr);
    }
    if (this._rows.length == 0) {
        // Add empty row to avoid browser bugs
        tbody.append(document.createElement("tr"));
    }
};

/**
 * Returns the number of rows in the table. This is a convenience
 * method for `getData().length`.
 *
 * @return {number} the number of table rows
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
 * @param {number} index the row index, 0 <= index < row count
 *
 * @return {string} the unique row id, or null if not found
 */
RapidContext.Widget.Table.prototype.getRowId = function (index) {
    let row = this._rows[index];
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
    return this._selected.map((idx) => this._rows[idx].$id);
};

/**
 * Returns the currently selected row data.
 *
 * @return {Object|Array} the data row selected, or
 *         an array of selected data rows if multiple selection is enabled
 */
RapidContext.Widget.Table.prototype.getSelectedData = function () {
    let data = this._selected.map((idx) => this._rows[idx].$data);
    return (this._selectMode === "multiple") ? data : data[0];
};

/**
 * Sets the selection to the specified row id values. If the current
 * selection is changed the select signal will be emitted.
 *
 * @param {...(string|Array)} id the row ids or array with ids to select
 *
 * @return {Array} an array with the row ids actually modified
 */
RapidContext.Widget.Table.prototype.setSelectedIds = function (...ids) {
    let $ids = RapidContext.Data.object([].concat(...ids), true);
    let oldIds = RapidContext.Data.object(this.getSelectedIds(), true);
    let res = [];
    for (let i = 0; i < this._rows.length; i++) {
        let rowId = this._rows[i].$id;
        if ($ids[rowId] && !oldIds[rowId]) {
            this._selected.push(i);
            this._markSelection(i);
            res.push(rowId);
        } else if (!$ids[rowId] && oldIds[rowId]) {
            let pos = this._selected.indexOf(i);
            if (pos >= 0) {
                this._selected.splice(pos, 1);
                this._unmarkSelection(i);
                res.push(rowId);
            }
        }
    }
    if (res.length > 0) {
        this.emit("select");
    }
    return res;
};

/**
 * Adds the specified row id values to the selection. If the current
 * selection is changed the select signal will be emitted.
 *
 * @param {...(string|Array)} id the row ids or array with ids to select
 *
 * @return {Array} an array with the new row ids actually selected
 */
RapidContext.Widget.Table.prototype.addSelectedIds = function (...ids) {
    let res = this._addSelectedIds(...ids);
    if (res.length > 0) {
        this.emit("select");
    }
    return res;
};

/**
 * Adds the specified row id values to the selection. Note that this
 * method does not emit any selection signal.
 *
 * @param {...(string|Array)} id the row ids or array with ids to select
 *
 * @return {Array} an array with the new row ids actually selected
 */
RapidContext.Widget.Table.prototype._addSelectedIds = function (...ids) {
    let $ids = RapidContext.Data.object([].concat(...ids), true);
    Object.assign($ids, RapidContext.Data.object(this.getSelectedIds(), false));
    let res = [];
    for (let i = 0; i < this._rows.length; i++) {
        if ($ids[this._rows[i].$id] === true) {
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
 * @param {...(string|Array)} id the row ids or array with ids to unselect
 *
 * @return {Array} an array with the row ids actually unselected
 */
RapidContext.Widget.Table.prototype.removeSelectedIds = function (...ids) {
    let $ids = RapidContext.Data.object([].concat(...ids), true);
    let res = [];
    for (let i = 0; i < this._rows.length; i++) {
        if ($ids[this._rows[i].$id] === true) {
            let pos = this._selected.indexOf(i);
            if (pos >= 0) {
                this._selected.splice(pos, 1);
                this._unmarkSelection(i);
                res.push(this._rows[i].$id);
            }
        }
    }
    if (res.length > 0) {
        this.emit("select");
    }
    return res;
};

/**
 * Marks selected rows.
 *
 * @param {number} [index] the row index, or null to mark all
 */
RapidContext.Widget.Table.prototype._markSelection = function (index) {
    let tbody = this.firstChild.lastChild;
    let indices = (index == null) ? this._selected : [index];
    for (let idx of indices) {
        tbody.childNodes[idx].classList.add("selected");
    }
};

/**
 * Unmarks selected rows.
 *
 * @param {number} [index] the row index, or null to unmark all
 */
RapidContext.Widget.Table.prototype._unmarkSelection = function (index) {
    let tbody = this.firstChild.lastChild;
    let indices = (index == null) ? this._selected : [index];
    for (let idx of indices) {
        tbody.childNodes[idx].classList.remove("selected");
    }
};
