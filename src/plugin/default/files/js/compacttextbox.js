/**
 * Creates a new editable compact textbox.
 *
 * @constructor
 * @param {Object} props       the optional widget properties
 * @config {Object} style      (optional) the initial style object
 * @config {Widget} dialogParent  (optional) the dialog parent widget
 */
function CompactTextBox(props) {
    props = props || {};
    this.style = props.style || {};
    this.dialogParent = props.dialogParent || null;
    /** (read-only) The HTML DOM node. */
    this.domNode = null;
    this._inputNode = null;
    this._dialog = null;
    this._textbox = null;
    this._text = "";
    this._init();
}

/**
 * Internal function to initialize the widget.
 *
 * @private
 */
CompactTextBox.prototype._init = function() {
    var obj;

    this.domNode = document.createElement("span");
    this.domNode.style.whiteSpace = "nowrap";
    this._inputNode = document.createElement("input");
    this._inputNode.autocomplete = "off";
    this.domNode.appendChild(this._inputNode);
    obj = Icon.EXPAND.createElement();
    obj.onclick = FunctionUtil.bind("_showDialog", this);
    CssUtil.setStyles(obj, { cursor: "pointer" });
    this.domNode.appendChild(obj);
}

/**
 * Returns the widget text.
 *
 * @return the widget text
 */
CompactTextBox.prototype.getText = function() {
    if (this._inputNode.disabled) {
        return this._text;
    } else {
        return this._inputNode.value;
    }
}

/**
 * Sets the widget text.
 *
 * @param text               the widget text
 */
CompactTextBox.prototype.setText = function(text) {
    var lines;

    this._text = text || "";
    if (typeof(this._text) != "string") {
        this._text = "" + this._text;
    }
    lines = this._text.split("\n");
    if (lines.length <= 1) {
        this._inputNode.disabled = false;
        this._inputNode.className = "";
        this._inputNode.value = this._text;
    } else {
        this._inputNode.disabled = true;
        this._inputNode.className = "disabled";
        this._inputNode.value = lines.length + " lines, " +
                                this._text.length + " chars";
    }
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
CompactTextBox.prototype.setStyle = function(style) {
    CssUtil.setStyles(this.domNode, style);
}

/**
 * Callback function to show the text editing dialog.
 *
 * @private
 */
CompactTextBox.prototype._showDialog = function() {
    if (this._dialog == null) {
        this._dialog = new Dialog({ title: "Edit Text", modal: true, style: { w: "55%", h: "70%" } });
        this._textbox = new TextBox();
        this._textbox.setWrap(false);
        this._textbox.setStyle({ w: "100% - 6", h: "100% - 6" });
        this._dialog.addChild(this._textbox);
        Signal.connect(this._dialog, "onHide", this, "_hideDialog");
        if (this.dialogParent) {
            if (this.dialogParent.addAll) {
                this.dialogParent.addAll(this._dialog.domNode);
            } else if (this.dialogParent.addChild) {
                this.dialogParent.addChild(this._dialog);
            }
        }
    }
    this._textbox.setText(this.getText());
    this._dialog.show();
}

/**
 * Callback function when the editing dialog is hidden.
 *
 * @private
 */
CompactTextBox.prototype._hideDialog = function() {
    var text = MochiKit.Format.strip(this._textbox.getText());

    this.setText(text);
    this._inputNode.focus();
}

// Register function names
ReTracer.Util.registerFunctionNames(CompactTextBox.prototype, "CompactTextBox");
