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
 * Creates a new overlay widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {Boolean} [attrs.loading] the display loading icon flag, defaults to
 *            `true`
 * @param {String} [attrs.message] the overlay message text, defaults to
 *            "Working..."
 * @param {Boolean} [attrs.dark] the dark overlay flag, defaults to `false`
 * @param {Boolean} [attrs.hidden] the hidden widget flag, defaults to `true`
 *
 * @return {Widget} the widget DOM node
 *
 * @class The overlay widget class. Used to provide a layer on top of the
 *     parent node, using a `<div>` HTML element. This widget is useful for
 *     disabling the user interface during an operation.
 * @extends RapidContext.Widget
 *
 * @example {JavaScript}
 * var workOverlay = RapidContext.WidgetOverlay({ message: "Doing Stuff..." });
 *
 * @example {User Interface XML}
 * <Overlay id="workOverlay" message="Doing Stuff..." />
 */
RapidContext.Widget.Overlay = function (attrs) {
    var cover = MochiKit.DOM.DIV({ "class": "widgetOverlayCover" });
    var msg = MochiKit.DOM.DIV({ "class": "widgetOverlayMessage" });
    var o = MochiKit.DOM.DIV({}, cover, msg);
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.Overlay);
    o.addClass("widgetOverlay");
    o.setAttrs(MochiKit.Base.update({ loading: true, message: "Working..." }, attrs));
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
 * @param {Boolean} [attrs.loading] the display loading icon flag
 * @param {String} [attrs.message] the overlay message text
 * @param {Boolean} [attrs.hidden] the hidden widget flag
 */
RapidContext.Widget.Overlay.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
    var locals = RapidContext.Util.mask(attrs, ["loading", "message", "dark"]);
    if (typeof(locals.loading) != "undefined") {
        this.showLoading = MochiKit.Base.bool(locals.loading);
    }
    if (typeof(locals.message) != "undefined") {
        this.message = locals.message || "";
    }
    if (typeof(locals.dark) != "undefined") {
        if (locals.dark) {
            this.addClass("widgetOverlayDark");
        } else {
            this.removeClass("widgetOverlayDark");
        }
    }
    if (this.showLoading) {
        var icon = RapidContext.Widget.Icon({ ref: "LOADING", "class": "m-1" });
    }
    MochiKit.DOM.replaceChildNodes(this.lastChild, icon, this.message);
    if (!this.showLoading && !this.message) {
        MochiKit.DOM.addElementClass(this.lastChild, "widgetHidden");
    } else {
        MochiKit.DOM.removeElementClass(this.lastChild, "widgetHidden");
    }
    this.__setAttrs(attrs);
};
