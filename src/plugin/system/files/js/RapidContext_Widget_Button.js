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
RapidContext.Widget = RapidContext.Widget || { Classes: {}};

/**
 * Creates a new button widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {Boolean} [attrs.highlight] the highlight option flag
 * @param {Object} [...] the child widgets or DOM nodes
 *
 * @return {Widget} the widget DOM node
 *
 * @class The button widget class. Used to provide a simple push
 *     button, using the &lt;button&gt; HTML element. In particular,
 *     the "onclick" event is usually of interest.
 * @property {Boolean} disabled The button disabled flag.
 * @extends RapidContext.Widget
 *
 * @example
 * var widget = RapidContext.Widget.Button({ highlight: true }, "Find");
 */
RapidContext.Widget.Button = function (attrs/*, ...*/) {
    var o = MochiKit.DOM.BUTTON();
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetButton");
    o.setAttrs(attrs);
    o.addAll(MochiKit.Base.extend(null, arguments, 1));
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Button = RapidContext.Widget.Button;

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {Boolean} [attrs.highlight] the highlight option flag
 */
RapidContext.Widget.Button.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
    var locals = RapidContext.Util.mask(attrs, ["highlight"]);
    if (typeof(locals.highlight) != "undefined") {
        if (MochiKit.Base.bool(locals.highlight)) {
            this.addClass("widgetButtonHighlight");
        } else {
            this.removeClass("widgetButtonHighlight");
        }
    }
    MochiKit.DOM.updateNodeAttributes(this, attrs);
};
