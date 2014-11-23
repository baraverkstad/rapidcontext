/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2013 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE.txt file for more details.
 */

// Namespace initialization
if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}
RapidContext.Widget = RapidContext.Widget || { Classes: {}};

/**
 * Creates a new text field widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {String} [attrs.name] the form field name
 * @param {String} [attrs.value] the field value, defaults to ""
 * @param {String} [attrs.helpText] the help text shown on empty input,
 *            defaults to ""
 * @param {Boolean} [attrs.disabled] the disabled widget flag, defaults to
 *            false
 * @param {Boolean} [attrs.hidden] the hidden widget flag, defaults to false
 * @param {Object} [...] the initial text content
 *
 * @return {Widget} the widget DOM node
 *
 * @class The text field widget class. Used to provide a text input field for a
 *     single line, using the `<input>` HTML element. The text field may also
 *     be connected to a popup (for auto-complete or similar).
 * @property {Boolean} disabled The read-only widget disabled flag.
 * @property {Boolean} focused The read-only widget focused flag.
 * @property {String} defaultValue The value to use on form reset.
 * @extends RapidContext.Widget
 *
 * @example {JavaScript}
 * var attrs = { name: "name", helpText: "Your Name Here" };
 * var field = RapidContext.Widget.TextField(attrs);
 *
 * @example {User Interface XML}
 * <TextField name="name" helpText="Your Name Here" />
 */
RapidContext.Widget.TextField = function (attrs/*, ...*/) {
    var type = (attrs && attrs.type) || "text";
    var text = (attrs && attrs.value) || "";
    for (var i = 1; i < arguments.length; i++) {
        var o = arguments[i];
        if (RapidContext.Util.isDOM(o)) {
            text += MochiKit.DOM.scrapeText(o);
        } else if (o != null) {
            text += o.toString();
        }
    }
    var o = MochiKit.DOM.INPUT({ type: type, value: text });
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetTextField");
    o.focused = false;
    o._popupCreated = false;
    o.setAttrs(MochiKit.Base.update({ helpText: "", value: text }, attrs));
    var changeHandler = RapidContext.Widget._eventHandler(null, "_handleChange");
    o.onkeyup = changeHandler;
    o.oncut = changeHandler;
    o.onpaste = changeHandler;
    var focusHandler = RapidContext.Widget._eventHandler(null, "_handleFocus");
    o.onfocus = focusHandler;
    o.onblur = focusHandler;
    return o;
};

// Register widget class
RapidContext.Widget.Classes.TextField = RapidContext.Widget.TextField;

/**
 * Emitted when the text is modified. This event is triggered by either
 * user events (keypress, paste, cut, blur) or by setting the value via
 * setAttrs(). The DOM standard onchange event has no 'event.detail'
 * data and is triggered on blur. The synthetic onchange events all
 * contain an 'event.detail' object with 'before', 'after' and 'cause'
 * properties.
 *
 * @name RapidContext.Widget.TextField#onchange
 * @event
 */

/**
 * Emitted when an item has been selected in the connected popup.
 * This event signal carries no 'event.detail' information.
 *
 * @name RapidContext.Widget.TextField#ondataavailable
 * @event
 */

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {String} [attrs.name] the form field name
 * @param {String} [attrs.value] the field value
 * @param {String} [attrs.helpText] the help text shown on empty input
 * @param {Boolean} [attrs.disabled] the disabled widget flag
 * @param {Boolean} [attrs.hidden] the hidden widget flag
 */
RapidContext.Widget.TextField.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
    var locals = RapidContext.Util.mask(attrs, ["helpText", "value"]);
    if (typeof(locals.helpText) != "undefined") {
        var str = MochiKit.Format.strip(locals.helpText);
        if ("placeholder" in this) {
            attrs.placeholder = str;
        } else {
            this.helpText = str;
        }
    }
    if (typeof(locals.value) != "undefined") {
        this.value = locals.value;
        this._handleChange(false, "set");
    }
    this.__setAttrs(attrs);
    this._render();
};

/**
 * Resets the text area form value to the initial value.
 */
RapidContext.Widget.TextField.prototype.reset = function () {
    this.setAttrs({ value: this.defaultValue });
};

/**
 * Returns the text field value. This function is slightly different
 * from using the `value` property directly, since it will always
 * return the actual value string instead of the temporary help text
 * displayed when the text field is empty and unfocused.
 *
 * @return {String} the field value
 *
 * @example
 * var value = field.getValue();
 * value = MochiKit.Format.strip(value);
 * field.setAttrs({ "value": value });
 */
RapidContext.Widget.TextField.prototype.getValue = function () {
    return (this.focused) ? this.value : this.storedValue;
};

/**
 * Returns (or creates) a popup for this text field. The popup will
 * not be shown by this method, only returned as-is. If the create
 * flag is specified, a new popup will be created if none has been
 * created previously.
 *
 * @param {Boolean} create the create popup flag
 *
 * @return {Widget} the popup widget, or
 *         null if none existed or was created
 */
RapidContext.Widget.TextField.prototype.popup = function (create) {
    if (!this._popupCreated && create) {
        this.autocomplete = "off";
        this._popupCreated = true;
        var style = { "max-height": "300px", "width": "300px" };
        var popup = RapidContext.Widget.Popup({ style: style });
        MochiKit.DOM.insertSiblingNodesAfter(this, popup);
        MochiKit.Style.makePositioned(this.parentNode);
        MochiKit.Signal.connect(this, "onkeydown", this, "_handleKeyDown");
        MochiKit.Signal.connect(popup, "onclick", this, "_handleClick");
    }
    return (this._popupCreated) ? this.nextSibling : null;
};

/**
 * Shows a popup for the text field containing the specified items.
 * The items specified may be either a list of HTML DOM nodes or
 * text strings.
 *
 * @param {Object} [attrs] the popup attributes to set
 * @param {Number} [attrs.delay] the popup auto-hide delay, defaults
 *            to 30 seconds
 * @param {Array} [items] the items to show, or null to keep the
 *            previous popup content
 *
 * @example
 * var items = ["Cat", "Dog", "Elephant", "Zebra"];
 * field.showPopup({}, items);
 */
RapidContext.Widget.TextField.prototype.showPopup = function (attrs, items) {
    var popup = this.popup(true);
    if (items) {
        popup.hide();
        MochiKit.DOM.replaceChildNodes(popup);
        for (var i = 0; i < items.length; i++) {
            if (typeof(items[i]) == "string") {
                var node = MochiKit.DOM.DIV({ "class": "widgetPopupItem" }, items[i]);
                node.value = items[i];
                popup.appendChild(node);
            } else {
                MochiKit.DOM.appendChildNodes(popup, items[i]);
            }
        }
    }
    if (popup.childNodes.length > 0) {
        var pos = { x: this.offsetLeft + 1,
                    y: this.offsetTop + this.offsetHeight + 1 };
        MochiKit.Style.setElementPosition(popup, pos);
        var style = { width: Math.max(300, this.width) + "px" };
        popup.setAttrs(MochiKit.Base.update({ delay: 30000, style: style }, attrs));
        popup.show();
        if (items && items.length == 1) {
            popup.selectChild(0);
        }
    }
};

/**
 * Handles keypress and paste events for this this widget.
 *
 * @param evt the MochiKit.Signal.Event object
 */
RapidContext.Widget.TextField.prototype._handleChange = function (evt, cause) {
    if (evt) {
        cause = "on" + evt.type();
        setTimeout(MochiKit.Base.bind("_handleChange", this, false, cause));
    } else if (this.storedValue != this.value) {
        var detail = { before: this.storedValue, after: this.value, cause: cause };
        this.storedValue = this.value;
        RapidContext.Widget._fireEvent(this, "change", detail);
    }
}

/**
 * Handles focus and blur events for this widget.
 *
 * @param evt the `MochiKit.Signal.Event` object
 */
RapidContext.Widget.TextField.prototype._handleFocus = function (evt) {
    var value = this.getValue();
    if (evt.type() == "focus") {
        this.focused = true;
        if (this.value != value) {
            this.value = value
        }
    } else if (evt.type() == "blur") {
        this.focused = false;
        this.storedValue = value;
        var popup = this.popup();
        if (popup && !popup.isHidden()) {
            popup.setAttrs({ delay: 250 });
        }
    }
    this._render();
};

/**
 * Handles the key down event for the text field.
 *
 * @param {Event} evt the `MochiKit.Signal.Event` object
 */
RapidContext.Widget.TextField.prototype._handleKeyDown = function (evt) {
    var popup = this.popup(false);
    if (popup != null) {
        popup.resetDelay();
        if (popup.isHidden()) {
            switch (evt.key().string) {
            case "KEY_ESCAPE":
                evt.stop();
                break;
            case "KEY_ARROW_UP":
            case "KEY_ARROW_DOWN":
                this.showPopup();
                popup.selectChild(0);
                evt.stop();
                break;
            }
        } else {
            switch (evt.key().string) {
            case "KEY_TAB":
            case "KEY_ENTER":
                popup.hide();
                evt.stop();
                if (popup.selectedChild() != null) {
                    RapidContext.Widget._fireEvent(this, "dataavailable");
                }
                break;
            case "KEY_ESCAPE":
                popup.hide();
                evt.stop();
                break;
            case "KEY_ARROW_UP":
            case "KEY_ARROW_DOWN":
                popup.selectMove(evt.key().string == "KEY_ARROW_UP" ? -1 : 1);
                evt.stop();
                break;
            }
        }
    }
};

/**
 * Handles the mouse click event on the popup.
 *
 * @param evt the `MochiKit.Signal.Event` object
 */
RapidContext.Widget.TextField.prototype._handleClick = function (evt) {
    this.blur();
    this.focus();
    RapidContext.Widget._fireEvent(this, "dataavailable")
};

/**
 * Updates the display of the widget content.
 */
RapidContext.Widget.TextField.prototype._render = function () {
    var value = this.getValue();
    var str = MochiKit.Format.strip(value);
    if (!this.focused && str == "" && this.helpText) {
        this.value = this.helpText;
        this.addClass("widgetTextFieldHelp");
    } else {
        if (this.value != value) {
            this.value = value;
        }
        this.removeClass("widgetTextFieldHelp");
    }
};
