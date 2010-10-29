/**
 * Creates a new table widget.
 *
 * @constructor
 * @param {Object} props         the optional widget properties
 * @config {Boolean} multiple    the multiple row flag, default is false
 */
function Table(props) {
    props = props || {};
    this.multiple = props.multiple || false;
    var thead = MochiKit.DOM.THEAD(null, MochiKit.DOM.TR());
    var tbody = MochiKit.DOM.TBODY();
    var table = MochiKit.DOM.TABLE({ "class": "uiTable" }, thead, tbody);
    this.domNode = MochiKit.DOM.DIV({ "class": "uiTable" }, table);
    tbody.resizeContent = MochiKit.Base.noop;
    MochiKit.Signal.connect(tbody, "onmousedown", this, "_handleSelect");
    this._columns = [];
    this._rows = [];
    this._data = null;
    this._keyField = null;
    this._selected = [];
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
Table.prototype.setStyle = function(style) {
    var table = this.domNode.firstChild;
    if (style.overflow) {
        delete style.overflow;
    }
    if (style.w) {
        CssUtil.setStyle(table, "w", style.w);
    }
    if (style.width) {
        CssUtil.setStyle(table, "width", style.width);
    }
    if (style.h) {
        if (StringUtil.contains(navigator.userAgent, "Gecko/")) {
            CssUtil.setStyle(table, "h", "100%");
            CssUtil.setStyle(table.lastChild, "h", "100% - 20");
        }
    }
    if (style.height) {
        if (StringUtil.contains(navigator.userAgent, "Gecko/")) {
            CssUtil.setStyle(table, "height", style.height);
            var value = "" + style.height;
            value = parseInt(value.substring(0, value.length - 2));
            CssUtil.setStyle(table.lastChild, "height", (value - 20) + "px");
        }
    }
    CssUtil.setStyles(this.domNode, style);
}

/**
 * Adds a column to this table.
 *
 * @param child              the table column to add
 */
Table.prototype.addChild = function(child) {
    if (!(child instanceof TableColumn)) {
        throw new Error("Table widget can only have TableColumn children");
    }
    this.clear();
    child.parent = this;
    var thead = this.domNode.firstChild.firstChild;
    thead.firstChild.appendChild(child.domNode);
    this._columns.push(child);
}

/**
 * Removes a column from this table.
 *
 * @param child              the table column to remove
 */
Table.prototype.removeChild = function(child) {
    if (!(child instanceof TableColumn)) {
        throw new Error("Table widget can only have TableColumn children");
    }
    this.clear();
    ArrayUtil.removeElem(this._columns, child);
    MochiKit.DOM.removeElement(child.domNode);
    child._destroy();
}

/**
 * Removes all columns from this table.
 */
Table.prototype.removeAllChildren = function() {
    this.clear();
    for (var i = 0; i < this._columns.length; i++) {
        MochiKit.DOM.removeElement(this._columns[i].domNode);
        this._columns[i]._destroy();
    }
    this._columns =[];
}

/**
 * Returns the column index of a field.
 *
 * @param field              the field name
 *
 * @return the column index, or
 *         -1 if not found
 */
Table.prototype.getColumnIndex = function(field) {
    for (var i = 0; i < this._columns.length; i++) {
        if (this._columns[i].field == field) {
            return i;
        }
    }
    return -1;
}

/**
 * Returns the unique key identifier column field, or null if none
 * was set.
 *
 * @return the key column field name, or
 *         null for none
 */
Table.prototype.getIdKey = function() {
    if (this._keyField) {
        return this._keyField;
    }
    for (var i = 0; i < this._columns.length; i++) {
        if (this._columns[i].key) {
            return this._columns[i].field;
        }
    }
    return null;
}

/**
 * Sets the unique key identifier column field. Note that this
 * method will regenerate all row identifiers if the table already
 * contains data.
 *
 * @param {String} key the new key column field name 
 */
Table.prototype.setIdKey = function(key) {
    this._keyField = key;
    for (var i = 0; i < this._rows.length; i++) {
        var row = this._rows[i];
        if (this._keyField != null && row.$data[this._keyField] != null) {
            row.$id = row.$data[this._keyField];
        }
    }
}

/**
 * Returns the current sort key for the table.
 *
 * @return the current sort field, or
 *         null for none
 */
Table.prototype.getSortKey = function() {
    for (var i = 0; i < this._columns.length; i++) {
        if (this._columns[i].sort != null && this._columns[i].sort != "none") {
            return this._columns[i].field;
        }
    }
    return null;
}

/**
 * Returns a table cell element.
 *
 * @param row                the row index
 * @param col                the column index
 *
 * @return the table cell element node, or
 *         null if not found
 */
Table.prototype.getCellElem = function(row, col) {
    try {
        var tbody = this.domNode.firstChild.lastChild;
        return tbody.childNodes[row].childNodes[col];
    } catch (e) {
        return null;
    }
}

/**
 * Clears all the data in the table.
 */
Table.prototype.clear = function() {
    this.setData([]);
}

/**
 * Returns an array with the data in the table.
 *
 * @return an array with the data in the table
 */
Table.prototype.getData = function() {
    return this._data;
}

/**
 * Sets the table data.
 *
 * @param data               an array with data objects
 */
Table.prototype.setData = function(data) {
    this.onClear();
    this._data = data;
    this._rows = [];
    this._selected = [];
    for (var i = 0; data != null && i < data.length; i++) {
        var row = { $id: "id" + i, $data: data[i] };
        for (var j = 0; j < this._columns.length; j++) {
            this._columns[j]._map(data[i], row);
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
}

/**
 * Sorts the table data by field and direction.
 *
 * @param field              the sort field
 * @param direction          the optional direction
 */
Table.prototype.sortData = function(field, direction) {
    var selectedIds = this.getSelectedIds();
    this._selected = [];
    for (var i = 0; i < this._columns.length; i++) {
        if (field == this._columns[i].field) {
            if (this._columns[i].sort == "none") {
                // Skip sorting if not allowed
                return;
            } else if (direction == null) {
                direction = this._columns[i].sort || "asc";
            }
            this._columns[i]._setSort(direction);
        } else if (this._columns[i].sort != "none") {
            this._columns[i]._setSort(null);
        }
    }
    this._rows.sort(MochiKit.Base.keyComparator(field));
    if (direction == "desc") {
        this._rows.reverse();
    }
    this._renderRows();
    this._addSelectedIds(selectedIds);
}

/**
 * Redraws the table from updated source data. Note that this method
 * will not add or remove rows and keeps the current row order
 * intact. For a more complete redraw of the table, use setData().
 */
Table.prototype.redraw = function() {
    for (var i = 0; i < this._rows.length; i++) {
        var row = this._rows[i];
        for (var j = 0; j < this._columns.length; j++) {
            this._columns[j]._map(row.$data, row);
        }
    }
    this._renderRows();
    for (var i = 0; i < this._selected.length; i++) {
        this._markSelection(this._selected[i]);
    }
}

/**
 * Renders the table rows.
 *
 * @private
 */
Table.prototype._renderRows = function() {
    var tbody = this.domNode.firstChild.lastChild;
    MochiKit.DOM.replaceChildNodes(tbody);
    for (var i = 0; i < this._rows.length; i++) {
        var tr = MochiKit.DOM.TR();
        if (i % 2 == 1) {
            MochiKit.DOM.addElementClass(tr, "alt");
        }
        for (var j = 0; j < this._columns.length; j++) {
            tr.appendChild(this._columns[j]._render(this._rows[i]));
        }
        tr.rowNo = i;
        tbody.appendChild(tr);
    }
    if (this._rows.length == 0) {
        // Add empty row to avoid browser bugs
        tbody.appendChild(MochiKit.DOM.TR());
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
Table.prototype.getSelectedIds = function() {
    var res = [];
    for (var i = 0; i < this._selected.length; i++) {
        res.push(this._rows[this._selected[i]].$id);
    }
    return res;
}

/**
 * Returns the currently selected row data.
 *
 * @return the data row selected, or
 *         an array of selected data rows if multiple selection is enabled
 */
Table.prototype.getSelectedData = function() {
    if (this.multiple) {
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
}

/**
 * Adds the specified row id values to the selection. If the current
 * selection is changed the select signal will be emitted.
 *
 * @param {String/Array} [...] the row ids or array with ids to select
 *
 * @return {Array} an array with the new row ids actually selected
 */
Table.prototype.addSelectedIds = function() {
    var res = this._addSelectedIds(arguments);
    if (res.length > 0) {
        this.onSelect();
    }
    return res;
}

/**
 * Adds the specified row id values to the selection. Note that this
 * method does not emit any selection signal.
 *
 * @param {String/Array} [...] the row ids or array with ids to select
 *
 * @return {Array} an array with the new row ids actually selected
 *
 * @private
 */
Table.prototype._addSelectedIds = function() {
    var args = MochiKit.Base.flattenArguments(arguments);
    var ids = ReTracer.Util.dict(args, true);
    var res = [];
    MochiKit.Base.update(ids, ReTracer.Util.dict(this.getSelectedIds(), false));
    for (var i = 0; i < this._rows.length; i++) {
        if (ids[this._rows[i].$id]) {
            this._selected.push(i);
            this._markSelection(i);
            res.push(this._rows[i].$id);
        }
    }
    return res;
}

/**
 * Removes the specified row id values from the selection. If the
 * current selection is changed the select signal will be emitted.
 *
 * @param {String/Array} [...] the row ids or array with ids to unselect
 *
 * @return {Array} an array with the row ids actually unselected
 */
Table.prototype.removeSelectedIds = function() {
    var args = MochiKit.Base.flattenArguments(arguments);
    var ids = ReTracer.Util.dict(args, true);
    var res = [];
    for (var i = 0; i < this._rows.length; i++) {
        if (ids[this._rows[i].$id]) {
            var pos = MochiKit.Base.findIdentical(this._selected, i);
            if (pos >= 0) {
                this._selected.splice(i, 1);
                this._unmarkSelection(i);
                res.push(this._rows[i].$id);
            }
        }
    }
    if (res.length > 0) {
        this.onSelect();
    }
    return res;
}

/**
 * The table clear signal.
 *
 * @signal Emitted when the table is cleared
 */
Table.prototype.onClear = function() {
}

/**
 * The table selection change signal.
 *
 * @signal Emitted when the table selection is changed
 */
Table.prototype.onSelect = function() {
}

/**
 * Handles the mouse selection events.
 *
 * @param row                the table row
 * @param evt                the event object (in some browsers)
 *
 * @private
 */
Table.prototype._handleSelect = function(evt) {
    // TODO: change to not allow iteration beyond <table> tag
    var tr = MochiKit.DOM.getFirstParentByTagAndClassName(evt.target(), "TR");
    if (tr == null || tr.rowNo == null) {
        evt.stop();
        return false;
    }
    var row = tr.rowNo;
    if (this.multiple) {
        if (evt.modifier().ctrl || evt.modifier().meta) {
            if (MochiKit.Base.findIdentical(this._selected, row) >= 0) {
                this._unmarkSelection(row);
                ArrayUtil.removeElem(this._selected, row);
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
    } else {
        this._unmarkSelection();
        this._selected = [row];
        this._markSelection(row);
    }
    evt.stop();
    this.onSelect();
    return false;
}

/**
 * Marks selected rows.
 *
 * @param indexOrNull        the row index, or null for the array
 *
 * @private
 */
Table.prototype._markSelection = function(indexOrNull) {
    if (indexOrNull == null) {
        for (var i = 0; i < this._selected.length; i++) {
            this._markSelection(this._selected[i]);
        }
    } else {
        var tbody = this.domNode.firstChild.lastChild;
        var tr = tbody.childNodes[indexOrNull];
        MochiKit.DOM.addElementClass(tr, "selected");
    }
}

/**
 * Unmarks selected rows.
 *
 * @param indexOrNull        the row index, or null for the array
 *
 * @private
 */
Table.prototype._unmarkSelection = function(indexOrNull) {
    if (indexOrNull == null) {
        for (var i = 0; i < this._selected.length; i++) {
            this._unmarkSelection(this._selected[i]);
        }
    } else {
        var tbody = this.domNode.firstChild.lastChild;
        var tr = tbody.childNodes[indexOrNull];
        MochiKit.DOM.removeElementClass(tr, "selected"); 
    }
}


/**
 * Creates a new table column.
 *
 * @constructor
 * @param {Object} props         the optional column properties
 * @config {String} title        the column title
 * @config {String} field        the data property name
 * @config {String} type         the data property type, defaults to "string"
 * @config {String} sort         the initial sort direction, use "none" to disable 
 * @config {String} maxLength    the maximum data length, overflow will be 
 *                               displayed as a tooltip
 * @config {Boolean} key         the unique key identifier column flag,
 *                               only to be set for one column
 */
function TableColumn(props) {
    props = props || {};
    this.parent = null;
    this.title = props.title || props.field;
    this.field = props.field || null;
    this.type = props.type || "string";
    this.sort = props.sort || null;
    this.maxLength = props.maxLength || null;
    this.key = props.key || false;
    if (this.field == null) {
        throw new Error("The 'field' property cannot be null for a TableColumn");
    }
    this.type = this.type.toLowerCase();
    this.domNode = MochiKit.DOM.TH(null, this.title);
    MochiKit.Signal.connect(this.domNode, "onclick", this, "_handleClick");
    this._setSort(this.sort);
}

/**
 * Destructor of this object.
 *
 * @private
 */
TableColumn.prototype._destroy = function() {
    this.parent = null;
    MochiKit.Signal.disconnectAll(this.domNode);
}

/**
 * Changes the column sort direction. This method also updates the
 * column header display.
 *
 * @param sort               the sort direction, or null for none
 *
 * @private
 */
TableColumn.prototype._setSort = function(sort) {
    this.sort = sort;
    if (sort == null || sort == "none") {
        MochiKit.DOM.removeElementClass(this.domNode, "sortAsc"); 
        MochiKit.DOM.removeElementClass(this.domNode, "sortDesc"); 
    } else if (sort == "desc") {
        MochiKit.DOM.removeElementClass(this.domNode, "sortAsc"); 
        MochiKit.DOM.addElementClass(this.domNode, "sortDesc");
    } else {
        MochiKit.DOM.removeElementClass(this.domNode, "sortDesc"); 
        MochiKit.DOM.addElementClass(this.domNode, "sortAsc");
    }
}

/**
 * Maps the column field from one object onto another. This method
 * will also convert the data depending on the column data type.
 *
 * @param src                the source object (containing the field)
 * @param dst                the destination object
 *
 * @private
 */
TableColumn.prototype._map = function(src, dst) {
    var value = src[this.field];

    if (value != null) {
        if (this._key) {
            dst.$id = value;
        }
        switch (this.type) {
        case "number":
            if (value instanceof Number) {
                value = value.valueOf();
            } else if (typeof(value) != "number") {
                try {
                    value = parseFloat(value);
                } catch (ignore) {
                    value = NaN;
                }
            }
            break;
        case "date":
            if (value instanceof Date) {
                value = MochiKit.DateTime.toISODate(value);
            } else {
                value = ReTracer.Util.truncate(value, 10);
            }
            break;
        case "datetime":
            if (value instanceof Date) {
                value = MochiKit.DateTime.toISOTimestamp(value);
            } else {
                value = ReTracer.Util.truncate(value, 19);
            }
            break;
        case "time":
            if (value instanceof Date) {
                value = MochiKit.DateTime.toISOTime(value);
            } else {
                if (typeof(value) != "string") {
                    value = value.toString();
                }
                if (value.length > 8) {
                    value = value.substring(value.length - 8);
                }
            }
            break;
        default:
            if (typeof(value) != "string") {
                value = value.toString();
            }
        }
    }
    dst[this.field] = value;
}

/**
 * Renders the column field value into a table cell.
 *
 * @param obj                the data object (containing the field)
 *
 * @return the table cell DOM node
 *
 * @private
 */
TableColumn.prototype._render = function(obj) {
    var td = MochiKit.DOM.TD();
    var value = obj[this.field] || "";

    if (typeof(value) != "string") {
        value = value.toString();
    }
    if (this.maxLength && this.maxLength < value.length) {
        td.title = value;
        value = ReTracer.Util.truncate(value, this.maxLength, "...");
    }
    if (this.type == "html") {
        td.innerHTML = value;
    } else {
        td.appendChild(ReTracer.Util.createTextNode(value));
    }
    return td;
}

/**
 * Handles click events on the column header.
 *
 * @private
 */
TableColumn.prototype._handleClick = function() {
    if (this.parent != null) {
        var dir = (this.sort == "asc") ? "desc" : "asc";
        this.parent.sortData(this.field, dir);
    }
}

// Register function names
ReTracer.Util.registerFunctionNames(Table.prototype, "Table");
ReTracer.Util.registerFunctionNames(TableColumn.prototype, "TableColumn");
