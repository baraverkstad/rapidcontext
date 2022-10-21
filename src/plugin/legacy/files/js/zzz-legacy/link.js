/**
 * Creates a new text link.
 *
 * @constructor
 * @param url                the destination URL
 * @param text               the link text
 */
function Link(url, text) {
    this.domNode = null;
    this._validUrl = StringUtil.contains(url, ":");
    this._init();
    this.setUrl(url);
    this.setText(text);
}

/**
 * Internal function to initialize the widget.
 *
 * @private
 */
Link.prototype._init = function() {
    this.domNode = document.createElement("a");
    this.domNode.target = "_blank";
    this.domNode.onclick = FunctionUtil.bind("onClick", this);
}

/**
 * Sets the link destination URL.
 *
 * @param url                the destination URL
 */
Link.prototype.setUrl = function(url) {
    this._validUrl = StringUtil.contains(url, ":");
    this.domNode.href = url;
}

/**
 * Returns the link text.
 */
Link.prototype.getText = function() {
    return this.domNode.textContent;
}

/**
 * Sets the link text.
 *
 * @param text               the link text
 */
Link.prototype.setText = function(text) {
    this.domNode.innerHTML = "";
    this.addText(text);
}

/**
 * Sets the link tooltip text.
 *
 * @param tooltip            the tooltip text
 */
Link.prototype.setTooltip = function(tooltip) {
    this.domNode.title = tooltip;
    this.domNode.className = "tooltip";
}

/**
 * Sets the link image.
 *
 * @param url                the link image
 */
Link.prototype.setImage = function(url) {
    this.domNode.innerHTML = "";
    if (url != null) {
        var img = document.createElement("img");
        img.src = url;
        this.domNode.appendChild(img);
    }
}

/**
 * Adds text to the link. If CSS styling or a tooltip text is
 * provided, the text will be added inside a "span" element.
 *
 * @param text               the text string
 * @param style              the optional CSS styling
 * @param tooltip            the optional tooltip text
 */
Link.prototype.addText = function(text, style, tooltip) {
    if (text != null && text != "") {
        if (style != null || tooltip != null) {
            var elem = document.createElement("span");
            CssUtil.setStyles(elem, style);
            if (tooltip != null) {
                elem.title = tooltip;
                elem.className = "tooltip";
            }
            elem.appendChild(document.createTextNode(text));
            this.domNode.appendChild(elem);
        } else {
            this.domNode.appendChild(document.createTextNode(text));
        }
    }
}

/**
 * Adds tagged text to the link. Only inline HTML text tags (such
 * as "span", "strong", "code", etc) should be used.
 *
 * @param tagName            the HTML tag name
 * @param text               the text string
 * @param style              the optional CSS styling
 * @param tooltip            the optional tooltip text
 */
Link.prototype.addTaggedText = function(tagName, text, style, tooltip) {
    if (text != null && text != "") {
        var elem = document.createElement(tagName);
        if (style != null) {
            CssUtil.setStyles(elem, style);
        }
        if (tooltip != null) {
            elem.title = tooltip;
            elem.className = "tooltip";
        }
        elem.appendChild(document.createTextNode(text));
        this.domNode.appendChild(elem);
    }
}

/**
 * Adds a image or an icon to the link.
 *
 * @param iconOrUrl          the icon reference or image URL
 */
Link.prototype.addImage = function(iconOrUrl) {
    var img;

    if (iconOrUrl instanceof Icon) {
        img = iconOrUrl.createElement();
    } else if (iconOrUrl.nodeType > 0) {
        img = iconOrUrl;
    } else {
        img = document.createElement("img");
        img.src = iconOrUrl.toString();
    }
    if (this.domNode.childNodes.length > 0) {
        this.addText(" ");
    }
    this.domNode.appendChild(img);
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
Link.prototype.setStyle = function(style) {
    CssUtil.setStyles(this.domNode, style);
}

/**
 * The link click signal.
 *
 * @signal Emitted when the link is clicked
 */
Link.prototype.onClick = function() {
    return this._validUrl;
}

// Register function names
ReTracer.Util.registerFunctionNames(Link.prototype, "Link");
