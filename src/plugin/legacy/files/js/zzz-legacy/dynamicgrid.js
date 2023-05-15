/**
 * Creates a new dynamic grid. This widget is composed from a
 * title pane and a grid pane, and simplifies much of the logic
 * for loading data into the grid. If no procedure name is
 * specified, data must be loaded manually into the grid.
 *
 * @constructor
 * @param title              the title to use
 * @param procedure          the procedure for loading data, or null
 */
function DynamicGrid(title, procedure) {
    this.domNode = null;
    this._pane = null;
    this._grid = null;
    this._columns = 2;
    this._maxLength = 25;
    this._autoAdd = true;
    this._procedure = procedure;
    this._arguments = [];
    this._mappers = [];
    this._fields = {};
    this._pos = { x: 0, y: 0 };
    this._data = null;
    this._onLoad = null;
    this._init(title);
}

/**
 * Internal function to initialize the widget.
 *
 * @param title              the title to use
 *
 * @private
 */
DynamicGrid.prototype._init = function(title) {
    this._pane = new TitlePane(title);
    this._pane.loadIcon(Icon.SEARCH);
    this._pane.loadIcon(Icon.RELOAD);
    this._pane.loadIcon(Icon.LOADING);
    this._pane.registerOnClick(Icon.RELOAD, this.reload, this);
    this._grid = new GridPane();
    this._pane.addChild(this._grid);
    this.domNode = this._pane.domNode;
}

/**
 * Sets the widget title.
 *
 * @param title              the new title
 */
DynamicGrid.prototype.setTitle = function(title) {
    this._pane.setTitle(title);
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
DynamicGrid.prototype.setStyle = function(style) {
    this._pane.setStyle(style);
}

/**
 * Shows an icon and optionally sets a click handler for the icon.
 *
 * @param icon               the icon to show
 * @param method             the callback method, or null for unchanged
 * @param obj                the callback object, or null for none
 */
DynamicGrid.prototype.showIcon = function(icon, method, obj) {
    this._pane.loadIcon(icon, -3);
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
DynamicGrid.prototype.hideIcon = function(icon, method, obj) {
    this._pane.loadIcon(icon, -3);
    this._pane.hideIcon(icon);
    if (method != null) {
        this._pane.registerOnClick(icon, method, obj);
    }
}

/**
 * Sets the display limits of the grid.
 *
 * @param columns            the number of columns to display
 * @param maxLength          the maximum field value length
 * @param autoAdd            the automatic add field flag,
 *                           defaults to true
 */
DynamicGrid.prototype.setLimits = function(columns, maxLength, autoAdd) {
    this._columns = columns;
    this._maxLength = maxLength;
    if (autoAdd != null) {
        this._autoAdd = autoAdd;
    }
}

/**
 * Enables or disables the search function.
 *
 * @param text               the prompt text, or null to disable
 */
DynamicGrid.prototype.enableSearch = function(text) {
    var self = this;

    if (text == null) {
        this._pane.hideIcon(Icon.SEARCH);
        this._pane.registerOnClick(Icon.SEARCH, null);
    } else {
        this._pane.showIcon(Icon.SEARCH);
        this._pane.registerOnClick(Icon.SEARCH, function() {
            var value = (self._arguments.length <= 0) ? "" : self._arguments[0];
            value = MochiKit.Format.strip(prompt(text, value));
            if (value != "") {
                self.load(value);
            }
        });
    }
}

/**
 * Removes all field display specifications and clears the grid data.
 */
DynamicGrid.prototype.clearFields = function() {
    this._fields = {};
    this.clear();
}

/**
 * Marks the specified field names as hidden. Any number of field
 * names may be supplied as arguments to this function.
 */
DynamicGrid.prototype.hideField = function() {
    for (var i = 0; i < arguments.length; i++) {
        this._fields[arguments[i]] = { name: arguments[i], pos: -1 };
    }
}

/**
 * Marks the specified field for display at a fixed position with
 * the specified label.
 *
 * @param name               the field name
 * @param position           the field position
 * @param label              the field label, or null for default
 * @param width              the field width, or null for 1
 */
DynamicGrid.prototype.showField = function(name, position, label, width) {
    if (position == null) {
        position = 100;
    }
    if (label == null) {
        label = this._createLabel(name);
    }
    if (width == null || isNaN(width)) {
        width = 1;
    } else {
        width = 1 + (width - 1) * 2;
    }
    this._fields[name] = { name: name, pos: position, label: label,
                           width: width };
}

/**
 * Returns the grid cell for the specified field. If the grid has not
 * yet been drawn, null will be returned.
 *
 * @param name               the field name
 *
 * @return the grid cell for the field value, or
 *         null if not available
 */
DynamicGrid.prototype.getFieldCell = function(name) {
    var field = this._fields[name];

    return (field == null) ? null : field.cell;
}

/**
 * Adds a data mapper function.
 *
 * @param mapperFunction     the data mapper function
 */
DynamicGrid.prototype.addMapper = function(mapperFunction) {
    this._mappers.push(mapperFunction);
}

/**
 * Removes all data mapper functions.
 */
DynamicGrid.prototype.removeAllMappers = function() {
    this._mappers = [];
}

/**
 * Clears the data from the grid.
 */
DynamicGrid.prototype.clear = function() {
    var fieldList = [];
    var field;

    this._data = null;
    this._pos = { x: 0, y: 0 };
    this._pane.hideIcon(Icon.RELOAD);
    this._pane.hideIcon(Icon.LOADING);
    while (this._grid.getHeight() > 0) {
        this._grid.removeRow(this._grid.getHeight() - 1);
    }
    for (var name in this._fields) {
        field = this._fields[name];
        field.cell = null;
        if (field.pos >= 0) {
            fieldList.push(field);
        }
    }
    fieldList.sort(function (f1, f2) {
        return f1.pos - f2.pos;
    });
    for (var i = 0; i < fieldList.length; i++) {
        field = fieldList[i];
        field.cell = this._createFieldCell(field.label, field.width);
    }
}

/**
 * Loads data into the grid. The arguments sent to this method will
 * be passed directly on to the call to the load procedure.
 */
DynamicGrid.prototype.load = function() {
    this._arguments = arguments;
    this.reload();
}

/**
 * Reloads the data in the grid. The arguments used in the last call
 * to load() will be used when calling the load procedure.
 */
DynamicGrid.prototype.reload = function() {
    if (this._procedure == null) {
        alert("Error: no procedure set for loading data into grid");
        return;
    }
    this._pane.hideIcon(Icon.RELOAD);
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
DynamicGrid.prototype._callbackLoad = function(data, error) {
    this._pane.showIcon(Icon.RELOAD);
    this._pane.hideIcon(Icon.LOADING);
    if (error != null) {
        alert("Error: " + error);
    } else {
        this.setData(data);
        this._pane.showIcon(Icon.RELOAD);
        if (this._onLoad != null) {
            this._onLoad.method.call(this._onLoad.object, this, data);
        }
    }
}

/**
 * Returns the last data object loaded or set to the grid. The data
 * object will be returned in its post-transformed shape, before data
 * normalization.
 *
 * @return the data object loaded, or
 *         null if empty
 */
DynamicGrid.prototype.getData = function() {
    return this._data;
}

/**
 * Displays the specified data.
 *
 * @param data               the data to display
 */
DynamicGrid.prototype.setData = function(data) {
    this.clear();
    if (data != null && data.rows != null) {
        data = data.rows;
    }
    this._transformData(data);
    this._data = data;
    data = this._normalizeData(data);
    this._displayData(data);
}

/**
 * Transforms the returned data with the data mappers.
 *
 * @private
 * @param data               the data loaded
 */
DynamicGrid.prototype._transformData = function(data) {
    if (data != null && this._mappers.length > 0) {
        if (data.length == null) {
            data = [data];
        }
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
 * Normalizes a data object, string or array.
 *
 * @param data               the data object, string or array
 *
 * @return the normalized data object
 *
 * @private
 */
DynamicGrid.prototype._normalizeData = function(data) {
    var res;
    var obj;

    if (data == null) {
        res = null;
    } else if (data instanceof String || typeof data == "string") {
        res = MochiKit.Format.strip(data);
    } else if (data instanceof Number || typeof data == "number") {
        res = "" + data;
    } else if (data instanceof Array) {
        if (data.length > 0) {
            res = this._normalizeData(data[0]);
        } else {
            res = {};
        }
        for (var i = 1; i < data.length; i++) {
            data[i] = this._normalizeData(data[i]);
            for (var name in res) {
                if (res[name] != data[i][name]) {
                    res[name] += " / " + data[i][name];
                }
            }
            for (var name in data[i]) {
                if (res[name] == null && data[i][name] != null) {
                    res[name] = "null / " + data[i][name];
                }
            }
        }
    } else if (typeof data == "object") {
        res = {};
        for (var name in data) {
            obj = this._normalizeData(data[name]);
            if (obj == null || obj instanceof String || typeof obj == "string") {
                res[name] = obj;
            } else {
                // Skip nested objects
            }
        }
    } else {
        res = "" + data;
    }
    return res;
}

/**
 * Displays all fields in the specified data object. This method
 * relies on that any defined fields have already been assigned to a
 * grid cell.
 *
 * @param data               the data object
 *
 * @private
 */
DynamicGrid.prototype._displayData = function(data) {
    var cell;
    var str;

    for (var name in data) {
        cell = null;
        if (this._fields[name] != null) {
            cell = this._fields[name].cell;
        } else if (this._autoAdd) {
            cell = this._createFieldCell(this._createLabel(name), 1);
        }
        str = data[name];
        if (cell != null && str != null) {
            if (str.length > this._maxLength) {
                cell.addChild(this._createPopupLink(str));
            } else {
                cell.setText(str);
            }
        }
    }
}

/**
 * Creates a suitable label from a field name.
 *
 * @param name               the field name
 *
 * @return the field label
 *
 * @private
 */
DynamicGrid.prototype._createLabel = function(name) {
    var str = name.substring(0, 1).toUpperCase() +
              name.substring(1).toLowerCase();
    var fun = function(match) {
        return " " + match.substring(1).toUpperCase();
    };
    return str.replace(/[ _-][a-zA-Z]/g, fun);
}

/**
 * Creates a data popup link.
 *
 * @param data               the data string
 *
 * @return the link widget
 *
 * @private
 */
DynamicGrid.prototype._createPopupLink = function(data) {
    var label = data.substring(0, this._maxLength) + "...";
    var link = new Link("#", label);

    Signal.connect(link, "onClick", function() {
        alert(data);
    });
    return link;
}

/**
 * Creates a field cell in the data grid.
 *
 * @param label              the field label
 * @param width              the column span value
 *
 * @return the field text cell created
 *
 * @private
 */
DynamicGrid.prototype._createFieldCell = function(label, width) {
    var cell;

    cell = this._grid.getCell(this._pos.x, this._pos.y, { th: true });
    cell.setStyle({ "class": "label", paddingRight: "4px" });
    cell.setText(label.replace(" ", "\u00A0") + ":");
    cell = this._grid.getCell(this._pos.x + 1, this._pos.y, { colspan: "" + width });
    cell.setStyle({ "class": "value", paddingRight: "10px" });
    this._pos.x += 1 + width;
    if (this._pos.x + 1 >= this._columns * 2) {
        this._pos.y++;
        this._pos.x = 0;
    }
    return cell;
}

/**
 * Registers a data loaded handler.
 *
 * @param method             the callback method
 * @param obj                the callback object, or null
 */
DynamicGrid.prototype.registerOnLoad = function(method, obj) {
    if (method == null) {
        this._onLoad = null;
    } else {
        this._onLoad = { method: method, object: obj };
    }
}
