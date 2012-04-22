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
 * Creates a new data table widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {String} [attrs.select] the row selection mode ('none', 'one' or
 *            'multiple'), defaults to 'one'
 * @param {String} [attrs.key] the unique key identifier column field,
 *            defaults to null
 * @param {Widget} [...] the child table columns
 *
 * @return {Widget} the widget DOM node
 *
 * @class The table widget class. Used to provide a sortable and
 *     scrolling data table, using an outer &lt;div&gt; HTML
 *     element around a &lt;table&gt;. The Table widget can only
 *     have TableColumn child nodes, each providing a visible data
 *     column in the table. In addition to standard HTML events, the
 *     "onclear" and "onselect" events are triggered when data is
 *     cleared or selected in the table.
 * @extends RapidContext.Widget
 *
 * @example
 * &lt;Table id="exTable" w="50%" h="100%"&gt;
 *   &lt;TableColumn title="Id" field="id" key="true" type="number" /&gt;
 *   &lt;TableColumn title="Name" field="name" sort="asc" /&gt;
 *   &lt;TableColumn title="Creation Date" field="created" type="date" /&gt;
 * &lt;/Table&gt;
 */
RapidContext.Widget.Table = function (attrs/*, ...*/) {
    var thead = MochiKit.DOM.THEAD({}, MochiKit.DOM.TR());
    var tbody = MochiKit.DOM.TBODY();
    tbody.resizeContent = MochiKit.Base.noop;
    var table = MochiKit.DOM.TABLE({ "class": "widgetTable" }, thead, tbody);
    var o = MochiKit.DOM.DIV({}, table);
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetTable");
    o._rows = [];
    o._data = null;
    o._keyField = null;
    o._selected = [];
    o._selectMode = "one";
    o._mouseX = 0;
    o._mouseY = 0;
    o.setAttrs(attrs);
    o.addAll(MochiKit.Base.extend(null, arguments, 1));
    tbody.onmousedown = RapidContext.Widget._eventHandler("Table", "_handleMouseDown");
    tbody.onmouseup = RapidContext.Widget._eventHandler("Table", "_handleMouseUp");
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Table = RapidContext.Widget.Table;

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {String} [attrs.select] the row selection mode ('none', 'one' or
 *            'multiple')
 * @param {String} [attrs.key] the unique key identifier column field
 */
RapidContext.Widget.Table.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
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
 * Returns an array with all child table column widgets. Note that
 * the array is a real JavaScript array, not a dynamic NodeList.
 *
 * @return {Array} the array of child table column widgets
 */
RapidContext.Widget.Table.prototype.getChildNodes = function () {
    var table = this.firstChild;
    var thead = table.firstChild;
    var tr = thead.firstChild;
    return MochiKit.Base.extend([], tr.childNodes);
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
    var table = this.firstChild;
    var thead = table.firstChild;
    var tr = thead.firstChild;
    tr.appendChild(child);
};

/**
 * Removes a single child table column widget from this widget.
 * This will also clear all the data in the table.
 *
 * @param {Widget} child the table column widget to remove
 */
RapidContext.Widget.Table.prototype.removeChildNode = function (child) {
    this.clear();
    var table = this.firstChild;
    var thead = table.firstChild;
    var tr = thead.firstChild;
    tr.removeChild(child);
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
 * affected by this method. Use removeAll() or removeChildNode() to
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
 * a hidden column). See the TableColumn class for data mapping
 * details. Note that automatically generated row ids will be reset
 * by this method and any selection on such tables is lost.
 *
 * @param {Array} data an array with data objects
 *
 * @example
 * var data = [
 *     { id: 1, name: "John Doe", created: "2007-12-31" },
 *     { id: 2, name: "First Last", created: "2008-03-01" },
 *     { id: 3, name: "Another Name", created: "2009-01-12" }
 * ];
 * table.setData(data);
 */
RapidContext.Widget.Table.prototype.setData = function (data) {
    var cols = this.getChildNodes();
    var selectedIds = this.getSelectedIds();
    RapidContext.Widget.emitSignal(this, "onclear");
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
 * intact. For a more complete redraw of the table, use setData().
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
    for (var i = 0; i < this._selected.length; i++) {
        this._markSelection(this._selected[i]);
    }
};

/**
 * Renders the table rows.
 */
RapidContext.Widget.Table.prototype._renderRows = function () {
    var cols = this.getChildNodes();
    var tbody = this.firstChild.lastChild;
    MochiKit.DOM.replaceChildNodes(tbody);
    for (var i = 0; i < this._rows.length; i++) {
        var tr = MochiKit.DOM.TR();
        if (i % 2 == 1) {
            MochiKit.DOM.addElementClass(tr, "widgetTableAlt");
        }
        for (var j = 0; j < cols.length; j++) {
            tr.appendChild(cols[j]._render(this._rows[i]));
        }
        tr.rowNo = i;
        tbody.appendChild(tr);
    }
    if (this._rows.length == 0) {
        // Add empty row to avoid browser bugs
        tbody.appendChild(MochiKit.DOM.TR());
    }
};

/**
 * Returns the number of rows in the table. This is a convenience
 * method for "getData().length" and always returns the same row
 * count.
 *
 * @return {Number} the number of table rows
 */
RapidContext.Widget.Table.prototype.getRowCount = function () {
    return this._rows.length;
}

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
    if (index >= 0 && index < this._rows.length) {
        return this._rows[index].$id;
    } else {
        return null;
    }
}

/**
 * Returns the currently selected row ids. If no rows are selected,
 * an empty array will be returned. The row ids are the data values
 * from the key column, or automatically generated internal values
 * if no key column is set.
 *
 * @return {Array} an array with the selected row ids
 */
RapidContext.Widget.Table.prototype.getSelectedIds = function () {
    var res = [];
    for (var i = 0; i < this._selected.length; i++) {
        res.push(this._rows[this._selected[i]].$id);
    }
    return res;
};

/**
 * Returns the currently selected row data.
 *
 * @return {Object/Array} the data row selected, or
 *         an array of selected data rows if multiple selection is enabled
 */
RapidContext.Widget.Table.prototype.getSelectedData = function () {
    if (this._selectMode === "multiple") {
        var res = [];
        for (var i = 0; i < this._selected.length; i++) {
            res.push(this._rows[this._selected[i]].$data);
        }
        return res;
    } else if (this._selected.length > 0) {
        return this._rows[this._selected[0]].$data;
    } else {
        return null;
    }
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
        RapidContext.Widget.emitSignal(this, "onselect");
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
        RapidContext.Widget.emitSignal(this, "onselect");
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
    MochiKit.Base.update(ids, RapidContext.Util.dict(this.getSelectedIds(), false));
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
        RapidContext.Widget.emitSignal(this, "onselect");
    }
    return res;
};

/**
 * Handles the mouse up event by stopping text selection in some cases.
 *
 * @param {Event} evt the MochiKit.Signal.Event object
 */
RapidContext.Widget.Table.prototype._handleMouseDown = function (evt) {
    this._mouseX = evt.mouse().page.x;
    this._mouseY = evt.mouse().page.y;
    if (evt.modifier().ctrl || evt.modifier().meta || evt.modifier().shift) {
        evt.stop();
        return false;
    } else {
        return true;
    }
}

/**
 * Handles the mouse up event by changing the selection if appropriate.
 *
 * @param {Event} evt the MochiKit.Signal.Event object
 */
RapidContext.Widget.Table.prototype._handleMouseUp = function (evt) {
    var moveX = Math.abs(evt.mouse().page.x - this._mouseX);
    var moveY = Math.abs(evt.mouse().page.y - this._mouseY);
    var moveXY = Math.sqrt(moveX * moveX + moveY * moveY);
    var tr = MochiKit.DOM.getFirstParentByTagAndClassName(evt.target(), "TR");
    if (tr == null || tr.rowNo == null || !MochiKit.DOM.isChildNode(tr, this)) {
        return true;
    } else if (moveXY > 5.0) {
        return true;
    }
    var row = tr.rowNo;
    if (this._selectMode === "multiple") {
        if (evt.modifier().ctrl || evt.modifier().meta) {
            var pos = MochiKit.Base.findIdentical(this._selected, row);
            if (pos >= 0) {
                this._unmarkSelection(row);
                this._selected.splice(pos, 1);
            } else {
                this._selected.push(row);
                this._markSelection(row);
            }
        } else if (evt.modifier().shift) {
            var start = row;
            if (this._selected.length > 0) {
                start = this._selected[0];
            }
            this._unmarkSelection();
            this._selected = [];
            if (row >= start) {
                for (var i = start; i <= row; i++) {
                    this._selected.push(i);
                }
            } else {
                for (var i = start; i >= row; i--) {
                    this._selected.push(i);
                }
            }
            this._markSelection();
        } else {
            this._unmarkSelection();
            this._selected = [row];
            this._markSelection(row);
        }
    } else if (this._selectMode !== "none") {
        this._unmarkSelection();
        this._selected = [row];
        this._markSelection(row);
    }
    RapidContext.Widget.emitSignal(this, "onselect");
    if (evt.modifier().ctrl || evt.modifier().meta || evt.modifier().shift) {
        evt.stop();
        return false;
    } else {
        return true;
    }
};

/**
 * Marks selected rows.
 *
 * @param {Number} indexOrNull the row index, or null for the array
 */
RapidContext.Widget.Table.prototype._markSelection = function (indexOrNull) {
    if (indexOrNull == null) {
        for (var i = 0; i < this._selected.length; i++) {
            this._markSelection(this._selected[i]);
        }
    } else {
        var tbody = this.firstChild.lastChild;
        var tr = tbody.childNodes[indexOrNull];
        MochiKit.DOM.addElementClass(tr, "selected");
    }
};

/**
 * Unmarks selected rows.
 *
 * @param {Number} indexOrNull the row index, or null for the array
 */
RapidContext.Widget.Table.prototype._unmarkSelection = function (indexOrNull) {
    if (indexOrNull == null) {
        for (var i = 0; i < this._selected.length; i++) {
            this._unmarkSelection(this._selected[i]);
        }
    } else {
        var tbody = this.firstChild.lastChild;
        var tr = tbody.childNodes[indexOrNull];
        MochiKit.DOM.removeElementClass(tr, "selected");
    }
};
