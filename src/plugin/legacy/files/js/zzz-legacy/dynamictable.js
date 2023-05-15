/**
 * Creates a new dynamic table. This widget is composed from a
 * title pane and a data table, and simplifies much of the logic
 * for loading data into the table. If no procedure name is
 * specified, data must be loaded manually into the table.
 *
 * @constructor
 * @param title              the title to use
 * @param procedure          the procedure for loading data, or null
 */
function DynamicTable(title, procedure) {
    this.domNode = null;
    this._pane = null;
    this._table = null;
    this._procedure = procedure;
    this._arguments = [];
    this._mappers = [];
    this._reloadIds = null;
    this._init(title);
}

/**
 * Internal function to initialize the widget.
 *
 * @param title              the title to use
 *
 * @private
 */
DynamicTable.prototype._init = function(title) {
    this._pane = new TitlePane(title);
    this._pane.setStyle({ overflow: "hidden" });
    this._pane.loadIcon(Icon.RELOAD);
    this._pane.loadIcon(Icon.LOADING);
    this._pane.registerOnClick(Icon.RELOAD, this.reload, this);
    this.domNode = this._pane.domNode;
    this._table = new Table();
    this._table.setStyle({ w: "100%", h: "100%" });
    Signal.connect(this._table, "onSelect", this, "onSelect");
    this._pane.addChild(this._table);
}

/**
 * Sets the widget title.
 *
 * @param title              the new title
 */
DynamicTable.prototype.setTitle = function(title) {
    this._pane.setTitle(title);
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
DynamicTable.prototype.setStyle = function(style) {
    this._pane.setStyle(style);
}

/**
 * Shows an icon and optionally sets a click handler for the icon.
 *
 * @param icon               the icon to show
 * @param method             the callback method, or null for unchanged
 * @param obj                the callback object, or null for none
 */
DynamicTable.prototype.showIcon = function(icon, method, obj) {
    this._pane.loadIcon(icon, -2);
    this._pane.showIcon(icon);
    if (method != null) {
        this._pane.registerOnClick(icon, method, obj);
    }
}

/**
 * Hides an icon and optionally sets a click handler for the icon.
 * This method can also be called to load an icon.
 *
 * @param icon               the icon to hide
 * @param method             the callback method, or null for unchanged
 * @param obj                the callback object, or null for none
 */
DynamicTable.prototype.hideIcon = function(icon, method, obj) {
    this._pane.loadIcon(icon, -2);
    this._pane.hideIcon(icon);
    if (method != null) {
        this._pane.registerOnClick(icon, method, obj);
    }
}

/**
 * Returns the multiple selection flag. If multiple selection is
 * enabled, several rows in the table may be selected at once.
 *
 * @return the multiple selection flag
 */
DynamicTable.prototype.getSelectMultiple = function() {
    return this._table.multiple;
}

/**
 * Sets the multiple selection flag. If multiple selection is enabled,
 * several rows in the table may be selected at once.
 *
 * @param multiple           the multiple selection flag
 */
DynamicTable.prototype.setSelectMultiple = function(multiple) {
    this._table.multiple = multiple;
}

/**
 * Adds a column to the table.
 *
 * @param {String} label the column title or label
 * @param {String} field the data field name mapped to the column
 * @param {String} dataType the data type ("String" or "Number")
 * @param {String} [sort] the optional sort type ("asc", "desc" or "none")
 * @param {Number} [maxLength] the optional maximum field length
 * @param {String} [padding] the optional padding to use after truncation
 * @param {Boolean} [key] the unique key identifier column flag
 */
DynamicTable.prototype.addColumn = function(label,
                                            field,
                                            dataType,
                                            sort,
                                            maxLength,
                                            padding,
                                            key) {
    this._table.addChild(new TableColumn({ title: label, field: field,
                                           type: dataType, sort: sort,
                                           maxLength: maxLength, key: key }));
}

/**
 * Removes all columns from the table.
 */
DynamicTable.prototype.removeAllColumns = function() {
    this._table.removeAllChildren();
    this._mappers = [];
}

/**
 * Adds a data mapper function.
 *
 * @param mapperFunction     the data mapper function
 */
DynamicTable.prototype.addMapper = function(mapperFunction) {
    this._mappers.push(mapperFunction);
}

/**
 * Removes all data mapper functions.
 */
DynamicTable.prototype.removeAllMappers = function() {
    this._mappers = [];
}

/**
 * Clears the data from the table.
 */
DynamicTable.prototype.clear = function() {
    this._pane.hideIcon(Icon.RELOAD);
    this._pane.hideIcon(Icon.LOADING);
    this._table.clear();
    this.onClear();
}

/**
 * Loads data into the table. The arguments sent to this method will
 * be passed directly on to the call to the load procedure.
 */
DynamicTable.prototype.load = function() {
    this._arguments = arguments;
    this.reload();
}

/**
 * Reloads the data in the table. The arguments used in the last call
 * to load() will be used when calling the load procedure.
 */
DynamicTable.prototype.reload = function() {
    if (this._procedure == null) {
        alert("Error: no procedure set for loading data into table");
        return;
    }
    if (this._table.getIdKey() != null) {
        this._reloadIds = this._table.getSelectedIds();
    } else {
        this._reloadIds = null;
    }
    this.clear();
    this._pane.showIcon(Icon.LOADING);
    retracer.call(this._procedure, this._arguments, this._callbackLoad, this);
}

/**
 * Callback function for the load method.
 *
 * @param data               the data loaded
 * @param error              the optional error, or null
 *
 * @private
 */
DynamicTable.prototype._callbackLoad = function(data, error) {
    this._pane.showIcon(Icon.RELOAD);
    this._pane.hideIcon(Icon.LOADING);
    if (error != null) {
        alert("Error: " + error);
    } else {
        if (data != null && data.rows != null && data.rows.length != null) {
            data = data.rows;
        }
        this.setData(data);
    }
}

/**
 * Returns an array with all the data in the table.
 *
 * @return an array with all the data objects
 */
DynamicTable.prototype.getData = function() {
    return this._table.getData();
}

/**
 * Sets the array of data in the table.
 *
 * @param data               the array of row data objects
 */
DynamicTable.prototype.setData = function(data) {
    this._transformData(data);
    this._table.setData(data);
    if (this._reloadIds != null) {
        this._table.addSelectedIds(this._reloadIds);
        this._reloadIds = null;
    }
}

/**
 * Transforms the returned data with the data mappers.
 *
 * @param data               the data loaded
 *
 * @private
 */
DynamicTable.prototype._transformData = function(data) {
    if (data != null && data.length != null && this._mappers.length > 0) {
        for (var i = 0; i < data.length; i++) {
            for (var j = 0; j < this._mappers.length; j++) {
                try {
                    this._mappers[j](data[i]);
                } catch (ignore) {
                    // Ignore mapping errors
                }
            }
        }
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
DynamicTable.prototype.getSelectedIds = function() {
    return this._table.getSelectedIds();
}

/**
 * Returns the currently selected row data.
 *
 * @return the data row selected, or
 *         an array of selected data rows if multiple selection is enabled
 */
DynamicTable.prototype.getSelectedData = function() {
    return this._table.getSelectedData();
}

/**
 * Adds the specified row id values to the selection. If the current
 * selection is changed the select signal will be emitted.
 *
 * @param {String/Array} [...] the row ids or array with ids to select
 *
 * @return {Array} an array with the new row ids actually selected
 */
DynamicTable.prototype.addSelectedIds = function() {
    return this._table.addSelectedIds(arguments);
}

/**
 * Removes the specified row id values from the selection. If the
 * current selection is changed the select signal will be emitted.
 *
 * @param {String/Array} [...] the row ids or array with ids to unselect
 *
 * @return {Array} an array with the row ids actually unselected
 */
DynamicTable.prototype.removeSelectedIds = function() {
    return this._table.removeSelectedIds(arguments);
}

/**
 * The table clear signal.
 *
 * @signal Emitted when the table is cleared
 */
DynamicTable.prototype.onClear = function() {
}

/**
 * The table selection change signal.
 *
 * @signal Emitted when the table selection is changed
 */
DynamicTable.prototype.onSelect = function() {
}
