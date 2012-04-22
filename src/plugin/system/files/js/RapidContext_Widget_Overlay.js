/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2012 Per Cederberg. All rights reserved.
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
 * Creates a new overlay widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {Boolean} [attrs.loading] the display loading icon flag,
 *            defaults to true
 * @param {String} [attrs.message] the overlay message text, defaults
 *            to "Working..."
 *
 * @return {Widget} the widget DOM node
 *
 * @class The overlay widget class. Used to provide a layer on top
 *     of the parent node, using a &lt;div&gt; HTML element. This
 *     widget is useful for disabling the user interface during an
 *     operation.
 * @extends RapidContext.Widget
 */
RapidContext.Widget.Overlay = function (attrs) {
    var cover = MochiKit.DOM.DIV({ "class": "widgetOverlayCover" });
    var msg = MochiKit.DOM.DIV({ "class": "widgetOverlayMessage" });
    var o = MochiKit.DOM.DIV({}, cover, msg);
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetOverlay");
    o.setAttrs(MochiKit.Base.update({ loading: true, message: "Working..." }, attrs));
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Overlay = RapidContext.Widget.Overlay;

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {Boolean} [attrs.loading] the display loading icon flag
 * @param {String} [attrs.message] the overlay message text
 */
RapidContext.Widget.Overlay.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
    var locals = RapidContext.Util.mask(attrs, ["loading", "message"]);
    if (typeof(locals.loading) != "undefined") {
        this.showLoading = MochiKit.Base.bool(locals.loading);
    }
    if (typeof(locals.message) != "undefined") {
        this.message = locals.message || "";
    }
    if (this.showLoading) {
        var icon = RapidContext.Widget.Icon({ url: "images/icons/loading-overlay.gif", width: 32, height: 32 });
        icon.setStyle({ "margin-right": "20px" });
    }
    MochiKit.DOM.replaceChildNodes(this.lastChild, icon, this.message);
    if (!this.showLoading && !this.message) {
        MochiKit.DOM.addElementClass(this.lastChild, "widgetHidden");
    } else {
        MochiKit.DOM.removeElementClass(this.lastChild, "widgetHidden");
    }
    this.__setAttrs(attrs);
};
