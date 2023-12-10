/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2023 Per Cederberg. All rights reserved.
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
 * @param {boolean} [attrs.disabled] the disabled widget flag, defaults to
 *            false
 * @param {boolean} [attrs.hidden] the hidden widget flag, defaults to false
 * @param {boolean} [attrs.highlight] the highlight option flag,
 *            defaults to false
 * @param {string} [attrs.icon] the icon reference to use, defaults
 *            to null (no icon)
 * @param {...(string|Node|Array)} [child] the child widgets or DOM nodes
 *
 * @return {Widget} the widget DOM node
 *
 * @class The button widget class. Used to provide a simple push
 *     button, using the `<button>` HTML element.
 * @extends RapidContext.Widget
 *
 * @example <caption>JavaScript</caption>
 * var closeBtn = RapidContext.Widget.Button({ icon: "OK", highlight: true }, "Close");
 *
 * @example <caption>User Interface XML</caption>
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
    var children = Array.from(arguments).slice(1).filter(Boolean);
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
 * @param {boolean} [attrs.disabled] the disabled widget flag
 * @param {boolean} [attrs.hidden] the hidden widget flag
 * @param {boolean} [attrs.highlight] the highlight option flag
 * @param {Icon|Object|string} [attrs.icon] the icon reference to use
 */
RapidContext.Widget.Button.prototype.setAttrs = function (attrs) {
    attrs = Object.assign({}, attrs);
    if ("highlight" in attrs) {
        this.classList.toggle("primary", RapidContext.Data.bool(attrs.highlight));
        delete attrs.highlight;
    }
    if ("icon" in attrs) {
        let child = this.querySelector("i");
        if (!attrs.icon) {
            child && RapidContext.Widget.destroyWidget(child);
        } else if (!child) {
            this.insertBefore(RapidContext.Widget.Icon(attrs.icon), this.firstChild);
        } else if (attrs.icon.nodeType) {
            child.replaceWith(attrs.icon);
        } else if (child.setAttrs) {
            child.setAttrs(attrs.icon);
        } else if (typeof(attrs.icon) === "string") {
            child.className = attrs.icon;
        } else {
            Object.keys(attrs.icon).forEach((k) => child.setAttribute(k, attrs.icon[k]));
        }
        delete attrs.icon;
    }
    this.__setAttrs(attrs);
};
