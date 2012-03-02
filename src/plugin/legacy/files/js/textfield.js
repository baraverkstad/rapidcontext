/**
 * Creates a new editable textfield.
 *
 * @constructor
 */
function TextField() {
    this._text = "";
    this._prevText = "";
    this._displayText = null;
    this._autoNode = null;
    this._autoSelected = null;
    this._autoClearTimer = false;
    this._autoClearFunc = MochiKit.Base.bind("_handleAutoClear", this);
    this.domNode = MochiKit.DOM.INPUT();
    this.domNode.autocomplete = "off";
    MochiKit.Signal.connect(this.domNode, "onfocus", this, "_handleOnFocus");
    MochiKit.Signal.connect(this.domNode, "onblur", this, "_handleOnBlur");
    MochiKit.Signal.connect(this.domNode, "onchange", this, "_handleOnChange");
    MochiKit.Signal.connect(this.domNode, "onkeydown", this, "_handleKeyDown");
    MochiKit.Signal.connect(this.domNode, "onkeyup", this, "_handleKeyUp");
    MochiKit.Signal.connect(this.domNode, "onkeypress", this, "_handleKeyPress");
}

/**
 * Returns the textfield text.
 *
 * @return the textfield text
 */
TextField.prototype.getText = function() {
    return (this._text == null) ? this.domNode.value : this._text;
}

/**
 * Sets the textfield text.
 *
 * @param text               the textfield text
 */
TextField.prototype.setText = function(text) {
    text = text || "";
    if (typeof(text) != "string") {
        text = "" + text;
    }
    if (this._text != null) {
        this._text = text;
    }
    if (this._text == null || this._displayText == null) {
        this.domNode.value = text;
    }
    this._handleOnChange();
}

/**
 * Returns the textfield display text when not in focus.
 *
 * @return the textfield display text, or
 *         null if not set
 */
TextField.prototype.getDisplayText = function() {
    return this._displayText;
}

/**
 * Sets the textfield display text when not in focus.
 *
 * @param text               the display text, or null to clear
 */
TextField.prototype.setDisplayText = function(text) {
    this._displayText = text;
    if (this._text != null && text == null) {
        this.domNode.value = this._text;
    } else if (this._text != null) {
        this.domNode.value = text;
    }
}

/**
 * Clears the auto complete list.
 */
TextField.prototype.clearAutoComplete = function() {
    if (this._autoNode != null) {
        ReTracer.Widget.destroyWidget(this._autoNode);
        this._autoNode = null;
        this._autoSelected = null;
    }
}

/**
 * Populates the auto complete list with a new alternative.
 *
 * @param value              the item value
 * @param label              the optional item label
 * @param styles             the optional additional styles
 */
TextField.prototype.addAutoComplete = function(value, label, styles) {
    label = label || value;
    if (this._autoNode == null) {
        var pos = MochiKit.Style.getElementPosition(this.domNode);
        var dim = MochiKit.Style.getElementDimensions(this.domNode);
        var attrs = { "overflow": "auto", "max-height": "150px" };
        this._autoNode = MochiKit.DOM.DIV({ "class": "dropdown", "style": attrs });
        pos.x += 3;
        pos.y += dim.h + 3;
        MochiKit.Style.setElementPosition(this._autoNode, pos);
        MochiKit.Signal.connect(this._autoNode, "onmousedown", this, "_handleMouseDown");
        document.body.appendChild(this._autoNode);
    }
    var elem = MochiKit.DOM.DIV({ "style": styles }, label);
    elem.data = { value: value };
    MochiKit.Signal.connect(elem, "onmouseover", this, "_handleMouseOver");
    MochiKit.Signal.connect(elem, "onclick", this, "_handleMouseClick");
    this._autoNode.appendChild(elem);
}

/**
 * Sets the textfield length in characters. Note that this does not
 * actually limit the text field content length, but only controls
 * the field appearence.
 *
 * @param length             the number of characters to display
 */
TextField.prototype.setLength = function(length) {
    this.domNode.size = length;
}

/**
 * Enables or disables the text field.
 *
 * @param disabled           the disabled boolean flag
 */
TextField.prototype.setDisabled = function(disabled) {
    this.domNode.disabled = disabled;
}

/**
 * Updates the CSS styles for this widget. This function takes an
 * object containing CSS property names and values.
 *
 * @param style              the style object
 */
TextField.prototype.setStyle = function(style) {
    CssUtil.setStyles(this.domNode, style);
}

/**
 * The widget focus signal.
 *
 * @signal Emitted when the widget gained focus.
 */
TextField.prototype.onFocus = function() {
}

/**
 * The widget blur signal.
 *
 * @signal Emitted when the widget lost focus.
 */
TextField.prototype.onBlur = function() {
}

/**
 * The text field change signal. Note that this signal is also
 * emitted on programmatic change of the text field value.
 *
 * @signal Emitted when the text field content is changed.
 */
TextField.prototype.onChange = function() {
}

/**
 * Handles the focus event.
 *
 * @param {Event} evt the MochiKit.Signal.Event object 
 *
 * @private
 */
TextField.prototype._handleOnFocus = function(evt) {
    this.domNode.value = this._text || "";
    this._text = null;
    this.onFocus();
}

/**
 * Handles the blur event and attempts to check for events caused by
 * clicks inside the auto-completion box.
 *
 * @param {Event} evt the MochiKit.Signal.Event object 
 *
 * @private
 */
TextField.prototype._handleOnBlur = function(evt) {
    this._text = this.domNode.value;
    if (this._displayText != null) {
        this.domNode.value = this._displayText;
    }
    if (this._autoNode && evt.mouse()) {
        var pos = MochiKit.Style.getElementPosition(this._autoNode);
        var dim = MochiKit.Style.getElementDimensions(this._autoNode);
        var evtPos = evt.mouse().client;
    }
    var inside = pos != null && dim != null && evtPos != null &&
                 pos.x <= evtPos.x && evtPos.x <= pos.x + dim.w &&
                 pos.y <= evtPos.y && evtPos.y <= pos.y + dim.h; 
    if (inside) {
        // Only IE provides mouse coordinates for blur events.
        // If the position was inside auto-complete we move the
        // focus back to the input box.
        this.domNode.focus();
    } else {
        // Other browsers must reset the auto-complete timer if
        // a mouse event is caught.
        if (this._autoNode != null) {
            this._autoClearTimer = true;
            setTimeout(this._autoClearFunc, 250);
        }
        this.onBlur();
    }
}

/**
 * Handles the change event and triggers a change signal if the field
 * value has really changed since the last call.
 *
 * @private
 */
TextField.prototype._handleOnChange = function() {
    var text = this.getText();

    if (text != this._prevText) {
        this._prevText = text;
        this.onChange();
    }
}

/**
 * Handles the key down event.
 *
 * @param {Event} evt the MochiKit.Signal.Event object 
 *
 * @private
 */
TextField.prototype._handleKeyDown = function(evt) {
    if (this._autoNode != null) {
        switch (evt.key().string) {
        case "KEY_TAB":
        case "KEY_ENTER":
            if (this._autoSelected != null) {
                evt.stop();
                // TODO: prevent renewed auto-complete search here...
                this.setText(this._autoSelected.data.value);
                this.clearAutoComplete();
                // Ugly blur + focus to fix problem with ESC key
                this.domNode.blur();
                this.domNode.focus();
            }
            break;
        case "KEY_ESCAPE":
            this.clearAutoComplete();
            break;
        case "KEY_ARROW_UP":
            if (this._autoSelected) {
                this._autoSelected.className = "";
                this._autoSelected = this._autoSelected.previousSibling;
            }
            if (this._autoSelected == null) {
                this._autoSelected = this._autoNode.lastChild;
            }
            this._autoSelected.className = "selected";
            this._autoSelected.blockMouseOver = true;
            this._scrollVisible(this._autoSelected);
            break;
        case "KEY_ARROW_DOWN":
            if (this._autoSelected) {
                this._autoSelected.className = "";
                this._autoSelected = this._autoSelected.nextSibling;
            }
            if (this._autoSelected == null) {
                this._autoSelected = this._autoNode.firstChild;
            }
            this._autoSelected.className = "selected";
            this._autoSelected.blockMouseOver = true;
            this._scrollVisible(this._autoSelected);
            break;
        }
    }
}

/**
 * Handles the key up event.
 *
 * @param {Event} evt the MochiKit.Signal.Event object 
 *
 * @private
 */
TextField.prototype._handleKeyUp = function(evt) {
    this._handleOnChange();
}

/**
 * Scrolls an element into horizontal visibility.
 *
 * @param node               the HTML DOM node
 */
TextField.prototype._scrollVisible = function(node) {
    var box = { y: node.offsetTop, h: node.offsetHeight + 5 }
    ReTracer.Util.adjustScrollOffset(node.parentNode, box);
}

/**
 * Handles the key press event.
 *
 * @param {Event} evt the MochiKit.Signal.Event object 
 *
 * @private
 */
TextField.prototype._handleKeyPress = function(evt) {
    switch (evt.key().string) {
    case "KEY_TAB":
        if (this._autoSelected != null) {
            evt.stop();
        }
        break;
    case "KEY_ENTER":
        evt.stop();
        break;
    }
}

/**
 * Handles the mouse over event for the dropdown list.
 *
 * @param {Event} evt the MochiKit.Signal.Event object 
 *
 * @private
 */
TextField.prototype._handleMouseOver = function(evt) {
    var elem = evt.src();
    if (this._autoSelected && this._autoSelected.blockMouseOver) {
        this._autoSelected.blockMouseOver = false;
        return;
    }
    if (this._autoSelected) {
        this._autoSelected.className = "";
    }
    this._autoSelected = elem;
    this._autoSelected.className = "selected";
}

/**
 * Handles the mouse click event for the dropdown list.
 *
 * @param {Event} evt the MochiKit.Signal.Event object 
 *
 * @private
 */
TextField.prototype._handleMouseClick = function(evt) {
    var elem = evt.src();
    evt.stop();
    this.setText(elem.data.value);
    this.clearAutoComplete();
    // Ugly blur + focus to fix problem with ESC key
    this.domNode.blur();
    this.domNode.focus();
}

/**
 * Handles the mouse down event for the dropdown list. This method
 * clears the dropdown clear timer to avoid accidentally closing
 * the dropdown when clicked.
 *
 * @private
 */
TextField.prototype._handleMouseDown = function() {
    this._autoClearTimer = false;
}

/**
 * Handles the dropdown clear timeout. This method will clear the
 * dropdown, unless the timer has been reset.
 *
 * @private
 */
TextField.prototype._handleAutoClear = function() {
    if (this._autoClearTimer) {
        this._autoClearTimer = false;
        this.clearAutoComplete();
    }
}

// Register function names
ReTracer.Util.registerFunctionNames(TextField.prototype, "TextField");
