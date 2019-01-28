/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2019 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE for more details.
 */

// Namespace initialization
if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}
RapidContext.Widget = RapidContext.Widget || { Classes: {}};

/**
 * Creates a new text area (or text box) widget.
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
 * @class The text area widget class. Used to provide a text input field
 *     spanning multiple rows, using the `<textarea>` HTML element.
 * @property {Boolean} disabled The read-only widget disabled flag.
 * @property {Boolean} focused The read-only widget focused flag.
 * @property {String} defaultValue The value to use on form reset.
 * @extends RapidContext.Widget
 *
 * @example {JavaScript}
 * var attrs = { name="description", helpText: "Description Text" };
 * var field = RapidContext.Widget.TextArea(attrs);
 *
 * @example {User Interface XML}
 * <TextArea name="description" helpText="Description Text" />
 */
RapidContext.Widget.TextArea = function (attrs/*, ...*/) {
    var text = "";
    if (attrs != null && attrs.value != null) {
        text = attrs.value;
    }
    for (var i = 1; i < arguments.length; i++) {
        var o = arguments[i];
        if (RapidContext.Util.isDOM(o)) {
            text += MochiKit.DOM.scrapeText(o);
        } else if (o != null) {
            text += o.toString();
        }
    }
    var o = MochiKit.DOM.TEXTAREA({ value: text });
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetTextArea");
    o.focused = false;
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
RapidContext.Widget.Classes.TextArea = RapidContext.Widget.TextArea;

/**
 * Emitted when the text is modified. This event is triggered by both
 * keypress and paste events if the text content is modified. The DOM
 * standard onchange event may also be trigged (on blur).
 *
 * @name RapidContext.Widget.TextArea#onchange
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
RapidContext.Widget.TextArea.prototype.setAttrs = function (attrs) {
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
        this._handleChange();
    }
    this.__setAttrs(attrs);
    this._render();
};

/**
 * Resets the text area form value to the initial value.
 */
RapidContext.Widget.TextArea.prototype.reset = function () {
    this.setAttrs({ value: this.defaultValue });
};

/**
 * Returns the text area value. This function is slightly different from using
 * the `value` property directly, since it will always return the actual value
 * instead of the temporary help text displayed when the text area is empty and
 * unfocused.
 *
 * @return {String} the field value
 *
 * @example
 * var value = field.getValue();
 * var lines = value.split("\n");
 * lines = MochiKit.Base.map(MochiKit.Format.strip, lines);
 * value = lines.join("\n");
 * field.setAttrs({ "value": value });
 */
RapidContext.Widget.TextArea.prototype.getValue = function () {
    var str = (this.focused) ? this.value : this.storedValue;
    // This is a hack to remove multiple newlines caused by
    // platforms inserting or failing to normalize newlines
    // within the HTML textarea control.
    if (/\r\n\n/.test(str)) {
        str = str.replace(/\r\n\n/g, "\n");
    }
    if (this.focused && this.value != str) {
        this.value = str;
    }
    return str;
};

/**
 * Handles keypress and paste events for this this widget.
 *
 * @param evt the MochiKit.Signal.Event object
 */
RapidContext.Widget.TextArea.prototype._handleChange = function (evt) {
    if (evt) {
        setTimeout(MochiKit.Base.bind("_handleChange", this));
    } else if (this.storedValue != this.value) {
        this.storedValue = this.value;
        RapidContext.Widget.emitSignal(this, "onchange", this.value);
    }
}

/**
 * Handles focus and blur events for this widget.
 *
 * @param evt the MochiKit.Signal.Event object
 */
RapidContext.Widget.TextArea.prototype._handleFocus = function (evt) {
    var value = this.getValue();
    if (evt.type() == "focus") {
        this.focused = true;
        if (this.value != value) {
            this.value = value
        }
    } else if (evt.type() == "blur") {
        this.focused = false;
        this.storedValue = value;
    }
    this._render();
};

/**
 * Updates the display of the widget content.
 */
RapidContext.Widget.TextArea.prototype._render = function () {
    var value = this.getValue();
    var str = MochiKit.Format.strip(value);
    if (!this.focused && str == "" && this.helpText) {
        this.value = this.helpText;
        this.addClass("widgetTextAreaHelp");
    } else {
        if (this.value != value) {
            this.value = value;
        }
        this.removeClass("widgetTextAreaHelp");
    }
};
