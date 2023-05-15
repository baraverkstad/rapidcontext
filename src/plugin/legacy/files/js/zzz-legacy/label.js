/**
 * Creates a new text label.
 *
 * @constructor
 * @param text               the initial label text, or null
 * @param tagName            the optional tag name to use
 */
function Label(text, tagName) {
    this.domNode = null;
    this._init(tagName);
    this.addText(text);
}

/**
 * Internal function to initialize the widget.
 *
 * @param tagName            the optional tag name to use
 *
 * @private
 */
Label.prototype._init = function(tagName) {
    if (typeof tagName == "string" && tagName.length > 0) {
        this.domNode = document.createElement(tagName);
    } else {
        this.domNode = document.createElement("span");
    }
}

/**
 * Returns the label text.
 */
Label.prototype.getText = function() {
    return this.domNode.textContent;
}

/**
 * Sets the label text.
 *
 * @param text               the label text
 */
Label.prototype.setText = function(text) {
    this.domNode.innerHTML = "";
    this.addText(text);
}

/**
 * Sets the label tooltip text.
 *
 * @param tooltip            the label tooltip text
 */
Label.prototype.setTooltip = function(tooltip) {
    this.domNode.title = tooltip;
    this.domNode.className = "tooltip";
}

/**
 * Adds text to the label. If CSS styling or a tooltip text is
 * provided, the text will be added inside a "span" element.
 *
 * @param text               the text string
 * @param style              the optional CSS styling
 * @param tooltip            the optional tooltip text
 */
Label.prototype.addText = function(text, style, tooltip) {
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
 * Adds tagged text to the label. Only inline HTML text tags (such
 * as "span", "strong", "code", etc) should be used.
 *
 * @param tagName            the HTML tag name
 * @param text               the text string
 * @param style              the optional CSS styling
 * @param tooltip            the optional tooltip text
 */
Label.prototype.addTaggedText = function(tagName, text, style, tooltip) {
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
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
Label.prototype.setStyle = function(style) {
    CssUtil.setStyles(this.domNode, style);
}
