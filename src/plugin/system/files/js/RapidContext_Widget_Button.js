/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2022 Per Cederberg. All rights reserved.
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
RapidContext.Widget = RapidContext.Widget || { Classes: {} };

/**
 * Creates a new button widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {Boolean} [attrs.disabled] the disabled widget flag, defaults to
 *            false
 * @param {Boolean} [attrs.hidden] the hidden widget flag, defaults to false
 * @param {Boolean} [attrs.highlight] the highlight option flag,
 *            defaults to false
 * @param {String} [attrs.icon] the icon reference to use, defaults
 *            to null (no icon)
 * @param {Object} [...] the child widgets or DOM nodes
 *
 * @return {Widget} the widget DOM node
 *
 * @class The button widget class. Used to provide a simple push
 *     button, using the `<button>` HTML element.
 * @extends RapidContext.Widget
 *
 * @example {JavaScript}
 * var closeBtn = RapidContext.Widget.Button({ icon: "OK", highlight: true }, "Close");
 *
 * @example {User Interface XML}
 * <Button id="closeBtn" icon="OK" highlight="true">Close</Button>
 */
RapidContext.Widget.Button = function (attrs/*, ...*/) {
    function textNode(val) {
        var el = document.createElement("t");
        el.innerText = String(val && val.textContent || val).trim();
        return el;
    }
    var o = MochiKit.DOM.BUTTON({ type: attrs.type || "button" });
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.Button);
    o.addClass("widgetButton");
    o.setAttrs(attrs);
    var children = Array.prototype.slice.call(arguments, 1).filter(Boolean);
    children.forEach(function (item) {
        o.addChildNode((item.nodeType === 1) ? item : textNode(item));
    });
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Button = RapidContext.Widget.Button;

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {Boolean} [attrs.disabled] the disabled widget flag
 * @param {Boolean} [attrs.hidden] the hidden widget flag
 * @param {Boolean} [attrs.highlight] the highlight option flag
 * @param {Icon/Object/String} [attrs.icon] the icon reference to use
 */
RapidContext.Widget.Button.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
    var locals = RapidContext.Util.mask(attrs, ["highlight", "icon"]);
    this.__setAttrs(attrs);
    if ("highlight" in locals) {
        $(this).toggleClass("primary", MochiKit.Base.bool(locals.highlight));
    }
    if ("icon" in locals) {
        var child = this.querySelector("i");
        if (!locals.icon) {
            child && RapidContext.Widget.destroyWidget(child);
        } else if (!child) {
            this.insertBefore(RapidContext.Widget.Icon(locals.icon), this.firstChild);
        } else if (locals.icon.nodeType) {
            MochiKit.DOM.swapDOM(child, locals.icon);
        } else {
            child.setAttrs ? child.setAttrs(locals.icon) : $(child).attr(locals.icon);
        }
    }
};
