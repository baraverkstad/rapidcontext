/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2025 Per Cederberg. All rights reserved.
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
RapidContext.Widget = RapidContext.Widget ?? { Classes: {} };

/**
 * Creates a new overlay widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {boolean} [attrs.loading] the display loading icon flag, defaults to
 *            `true`
 * @param {string} [attrs.message] the overlay message text, defaults to
 *            "Working..."
 * @param {boolean} [attrs.dark] the dark overlay flag, defaults to `false`
 * @param {boolean} [attrs.hidden] the hidden widget flag, defaults to `true`
 *
 * @return {Widget} the widget DOM node
 *
 * @class The overlay widget class. Used to provide a layer on top of the
 *     parent node, using a `<div>` HTML element. This widget is useful for
 *     disabling the user interface during an operation.
 * @extends RapidContext.Widget
 *
 * @example <caption>JavaScript</caption>
 * let workOverlay = RapidContext.WidgetOverlay({ message: "Doing Stuff..." });
 *
 * @example <caption>User Interface XML</caption>
 * <Overlay id="workOverlay" message="Doing Stuff..." />
 */
RapidContext.Widget.Overlay = function (attrs) {
    const cover = RapidContext.UI.DIV({ "class": "widgetOverlayCover" });
    const msg = RapidContext.UI.DIV({ "class": "widgetOverlayMessage" });
    const o = RapidContext.UI.DIV({}, cover, msg);
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.Overlay);
    o.addClass("widgetOverlay");
    o.setAttrs({ loading: true, message: "Working...", ...attrs });
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Overlay = RapidContext.Widget.Overlay;

/**
 * Returns the widget container DOM node.
 *
 * @return {Node} returns null, since child nodes are not supported
 */
RapidContext.Widget.Overlay.prototype._containerNode = function () {
    return null;
};

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {boolean} [attrs.loading] the display loading icon flag
 * @param {string} [attrs.message] the overlay message text
 * @param {boolean} [attrs.hidden] the hidden widget flag
 */
RapidContext.Widget.Overlay.prototype.setAttrs = function (attrs) {
    attrs = { ...attrs };
    if ("loading" in attrs) {
        attrs.loading = RapidContext.Data.bool(attrs.loading);
    }
    if ("dark" in attrs) {
        attrs.dark = RapidContext.Data.bool(attrs.dark);
        this.classList.toggle("widgetOverlayDark", attrs.dark);
    }
    this.__setAttrs(attrs);
    this.lastChild.innerHTML = "";
    const icon = this.loading && RapidContext.Widget.Icon("fa fa-refresh fa-spin m-1");
    this.lastChild.append(icon ?? "", this.message ?? "");
    this.lastChild.classList.toggle("widgetHidden", !this.loading && !this.message);
};
