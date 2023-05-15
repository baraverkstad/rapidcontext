/**
 * Creates a new title pane.
 *
 * @constructor
 * @param title              the title to use
 */
function TitlePane(title) {
    this.title = (title == null) ? "Default Title" : title;
    this.domNode = null;
    this._textElem = null;
    this._iconImages = {};
    this._pane = null;
    this._init();
}

/**
 * Internal function to initialize the widget.
 *
 * @private
 */
TitlePane.prototype._init = function() {
    var h3;
    var img;

    this.domNode = document.createElement("div");
    h3 = document.createElement("h3");
    this._textElem = document.createElement("span");
    this._textElem.innerHTML = this.title.replace(" ", "&nbsp;");
    h3.appendChild(this._textElem);
    h3.appendChild(document.createTextNode(":"));
    img = Icon.BLANK.createElement();
    img.width = 0;
    h3.appendChild(img);
    this.domNode.appendChild(h3);
    this._pane = new Pane();
    this.domNode.appendChild(this._pane.domNode);
}

/**
 * Sets the title pane title.
 *
 * @param title              the new title
 */
TitlePane.prototype.setTitle = function(title) {
    this.title = title;
    this._textElem.innerHTML = title.replace(" ", "&nbsp;");
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
TitlePane.prototype.setStyle = function(style) {
    var paneStyle = {};

    if (style.overflow) {
        paneStyle.overflow = style.overflow;
        delete style.overflow;
    }
    if (style.w || style.width) {
        paneStyle.w = "100%";
    }
    if (style.h || style.height) {
        paneStyle.h = "100% - 22";
    }
    CssUtil.setStyles(this.domNode, style);
    this._pane.setStyle(paneStyle);
}

/**
 * Loads an icon to the title pane without displaying it. This
 * method is useful to ensure that the icons are placed in a
 * certain predetermined order. Use one of the predefined Icon
 * class constants as the icon value.
 *
 * @param icon               the icon to show (an Icon constant)
 * @param pos                the optional icon position
 */
TitlePane.prototype.loadIcon = function(icon, pos) {
    var img;

    if (this._iconImages[icon.src] == null) {
        this._iconImages[icon.src] = img = icon.createElement();
        img.style.marginLeft = "6px";
        img.style.cursor = "default";
        img.style.display = "none";
        if (pos != null && pos > 0) {
            pos += 3;
        }
        var n = this.domNode.firstChild.childNodes;
        if (pos != null && pos < 0 && -pos < n.length) {
            MochiKit.DOM.insertSiblingNodesBefore(n[n.length + pos], img);
        } else if (pos != null && pos > 0 && pos < n.length) {
            MochiKit.DOM.insertSiblingNodesBefore(n[pos], img);
        } else {
            this.domNode.firstChild.appendChild(img);
        }
    }
}

/**
 * Shows a title pane icon. The icon is loaded if not previously
 * used. Use one of the predefined Icon class constants as the icon
 * value.
 *
 * @param icon               the icon to show
 */
TitlePane.prototype.showIcon = function(icon) {
    this.loadIcon(icon);
    this._iconImages[icon.src].style.display = "inline";
}

/**
 * Hides a title pane icon. The icon is loaded if not previously
 * used. Use one of the predefined Icon class constants as the icon
 * value.
 *
 * @param icon               the icon to hide
 */
TitlePane.prototype.hideIcon = function(icon) {
    this.loadIcon(icon);
    this._iconImages[icon.src].style.display = "none";
}

/**
 * Adds a child element to this pane.
 *
 * @param child              the child element to add
 */
TitlePane.prototype.addChild = function(child) {
    this._pane.addChild(child);
}

/**
 * Removes all child elements from this pane.
 */
TitlePane.prototype.removeAllChildren = function() {
    this._pane.removeAllChildren();
}

/**
 * Registers an icon click handler. If the icon is unused, it will
 * be loaded to the title pane without displaying it. Use one of the
 * predefined Icon class constants as the icon value.
 *
 * @param icon               the icon to handle clicks for
 * @param method             the callback method
 * @param obj                the callback object, or null
 */
TitlePane.prototype.registerOnClick = function(icon, method, obj) {
    this.loadIcon(icon);
    Icon.registerOnClick(this._iconImages[icon.src], method, obj);
}
