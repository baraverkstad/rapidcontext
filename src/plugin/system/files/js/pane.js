/**
 * Creates a new pane widget. The pane widget is a basic container
 * for other widgets, optionally providing scrollbars if both width
 * and height is constrained.
 *
 * @constructor
 * @param {Object} props         the optional widget properties
 * @config {String} tagName      the HTML tag name, defaults to 'div'
 * @config {Object} style        the initial style object
 */
function Pane(props) {
    props = props || {};
    this.tagName = props.tagName || "div";
    this.style = props.style || {};
    /** (read-only) The HTML DOM node. */
    this.domNode = document.createElement(this.tagName);
    this.setStyle(this.style);
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
Pane.prototype.setStyle = function(style) {
    // TODO: move this check to generic UI function
    if ((style.w || style.width) && (style.h || style.height)) {
        if (style.overflow == null && this.style.overflow == null) {
            style.overflow = "auto";
        }
    }
    CssUtil.setStyles(this.domNode, style);
}

/**
 * Adds a child element to this pane.
 *
 * @param child              the child element to add
 */
Pane.prototype.addChild = function(child) {
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
 * Removes all child elements from this pane.
 */
Pane.prototype.removeAllChildren = function() {
    this.domNode.innerHTML = "";
}

// Register function names
ReTracer.Util.registerFunctionNames(Pane.prototype, "Pane");
