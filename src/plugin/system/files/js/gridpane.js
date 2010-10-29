/**
 * Creates a new grid pane.
 *
 * @constructor
 */
function GridPane() {
    this.domNode = null;
    this._tbodyElem = null;
    this._init();
}

/**
 * Internal function to initialize the widget.
 *
 * @private
 */
GridPane.prototype._init = function() {
    this.domNode = document.createElement("table");
    this._tbodyElem = document.createElement("tbody");
    this.domNode.appendChild(this._tbodyElem);
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
GridPane.prototype.setStyle = function(style) {
    CssUtil.setStyles(this.domNode, style);
}

/**
 * Returns the grid height in number of cells.
 *
 * @return the grid height in number of cells
 */
GridPane.prototype.getHeight = function() {
    return this._tbodyElem.childNodes.length;
}

/**
 * Returns the grid width in number of cells. An optional row number
 * may be specified, otherwise the width of the first row is
 * returned.
 *
 * @param row                the row position (from 0)
 *
 * @return the grid width in number of cells
 */
GridPane.prototype.getWidth = function(row) {
    if (row == null || row < 0) {
        row = 0;
    }
    if (row >= this._tbodyElem.childNodes.length) {
        return 0;
    } else {
        return this._tbodyElem.childNodes[row].childNodes.length;
    }
}

/**
 * Finds or creates a grid cell component.
 *
 * @param x                  the horizontal position (from 0)
 * @param y                  the vertical position (from 0)
 * @param props              the grid cell properties, or null
 *
 * @return the grid cell element component
 */
GridPane.prototype.getCell = function(x, y, props) {
    var tr;
    var td;

    while (this._tbodyElem.childNodes.length <= y) {
        tr = document.createElement("tr");
        this._tbodyElem.appendChild(tr);
    }
    tr = this._tbodyElem.childNodes[y];
    while (tr.childNodes.length <= x) {
        if (tr.childNodes.length == x && props && props.th) {
            td = document.createElement("th");
        } else {
            td = document.createElement("td");
        }
        if (tr.childNodes.length == x && props) {
            for (var name in props) {
                td.setAttribute(name, props[name]);
                if (name == "colspan") {
                    td.colSpan = props[name];
                }
            }
        }
        tr.appendChild(td);
    }
    td = tr.childNodes[x];
    return new GridCell(td);
}

/**
 * Removes a row in the grid. Note that all rows below will be moved
 * upwards one step.
 *
 * @param y                  the vertical position (from 0)
 */
GridPane.prototype.removeRow = function(y) {
    if (y >= 0 && y < this._tbodyElem.childNodes.length) {
        var tr = this._tbodyElem.childNodes[y];
        this._tbodyElem.removeChild(tr);
    }
}

/**
 * Removes all rows in the grid.
 */
GridPane.prototype.removeAllRows = function() {
    while (this._tbodyElem.childNodes.length > 0) {
        this.removeRow(this._tbodyElem.childNodes.length - 1);
    }
}


/**
 * Creates a new grid cell.
 *
 * @constructor
 */
function GridCell(cellElem) {
    this.domNode = cellElem;
}

/**
 * Updates the CSS styles for this cell. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
GridCell.prototype.setStyle = function(style) {
    CssUtil.setStyles(this.domNode, style);
}

/**
 * Adds a child element to the this cell.
 *
 * @param child              the child element to add
 */
GridCell.prototype.addChild = function(child) {
    if (child.domNode) {
        this.domNode.appendChild(child.domNode);
    } else if (child.nodeType) {
        this.domNode.appendChild(child);
    } else {
        this.domNode.innerHTML = child;
    }
    CssUtil.resize(this.domNode);
}

/**
 * Removes all child elements from this cell.
 */
GridCell.prototype.removeAllChildren = function() {
    this.domNode.innerHTML = "";
}

/**
 * Replaces the cell content with the specified text.
 *
 * @param text               the cell text content
 */
GridCell.prototype.setText = function(text) {
    this.domNode.innerHTML = "";
    this.addChild(document.createTextNode(text));
}

/**
 * Replaces the cell content with the specified HTML code.
 *
 * @param html               the cell HTML content
 */
GridCell.prototype.setHtml = function(html) {
    this.domNode.innerHTML = html;
}

// Register function names
ReTracer.Util.registerFunctionNames(GridPane.prototype, "GridPane");
ReTracer.Util.registerFunctionNames(GridCell.prototype, "GridCell");
