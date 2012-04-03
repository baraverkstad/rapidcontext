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
 * Creates a new pane widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {String} [attrs.pageTitle] the page title used when inside
 *            a page container, defaults to "Page"
 * @param {String/Object} [attrs.pageStatus] the page status used
 *            when inside a page container, use one of the predefined
 *            status constants in this class, defaults to "ANY"
 * @param {Boolean} [attrs.pageCloseable] the page closeable flag
 *            used when inside some page containers, defaults to
 *            false
 * @param {Object} [...] the child widgets or DOM nodes
 *
 * @return {Widget} the widget DOM node
 *
 * @class The pane widget class. Used to create the simplest form of
 *     element container. It is also used inside various types of
 *     paged containers, such as a TabContainer, a Wizard and
 *     similar. A pane only uses a &lt;div&gt; HTML element, and
 *     supports being hidden and shown according to any page
 *     transitions required by a parent container. In addition to
 *     standard HTML events, the "onenter" and "onexit" events are
 *     triggered whenever the pane is used in a parent container
 *     page transition.
 * @property {String} pageTitle [read-only] The current page title.
 * @property {Object} pageStatus [read-only] The current page status.
 * @property {Boolean} pageCloseable [read-only] The current page
 *               closeable flag value.
 * @extends RapidContext.Widget
 */
RapidContext.Widget.Pane = function (attrs/*, ... */) {
    var o = MochiKit.DOM.DIV();
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetPane");
    o.setAttrs(MochiKit.Base.update({ pageTitle: "Page", pageStatus: "ANY", pageCloseable: false }, attrs));
    o.addAll(MochiKit.Base.extend(null, arguments, 1));
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Pane = RapidContext.Widget.Pane;

/**
 * The default page status. Allows page transitions both to the
 * previous and the next page.
 *
 * @memberOf RapidContext.Widget.Pane
 * @name ANY
 * @static
 */
RapidContext.Widget.Pane.ANY = { previous: true, next: true };

/**
 * The forward-only page status. Allows transitions only to the next
 * page.
 *
 * @memberOf RapidContext.Widget.Pane
 * @name FORWARD
 * @static
 */
RapidContext.Widget.Pane.FORWARD = { previous: false, next: true };

/**
 * The backward-only page status. Allows transitions only to the
 * previous page.
 *
 * @memberOf RapidContext.Widget.Pane
 * @name BACKWARD
 * @static
 */
RapidContext.Widget.Pane.BACKWARD = { previous: true, next: false };

/**
 * The working page status. Will disable transitions both to the
 * previous and the next page. The page container may also display a
 * cancel button to allow user cancellation of the ongoing operation.
 *
 * @memberOf RapidContext.Widget.Pane
 * @name WORKING
 * @static
 */
RapidContext.Widget.Pane.WORKING = { previous: false, next: false };

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {String} [attrs.pageTitle] the page title used when inside
 *            a page container
 * @param {String/Object} [attrs.pageStatus] the page status used
 *            when inside a page container, use one of the predefined
 *            status constants in this class
 * @param {Boolean} [attrs.pageCloseable] the page closeable flag
 *            used when inside some page containers
 */
RapidContext.Widget.Pane.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
    var locals = RapidContext.Util.mask(attrs, ["pageTitle", "pageStatus", "pageCloseable"]);
    var modified = false;
    if (typeof(locals.pageTitle) != "undefined") {
        this.pageTitle = locals.pageTitle;
        modified = true;
    }
    if (typeof(locals.pageStatus) != "undefined") {
        if (typeof(locals.pageStatus) == "string") {
            locals.pageStatus = RapidContext.Widget.Pane[locals.pageStatus];
        }
        this.pageStatus = locals.pageStatus;
        modified = true;
    }
    if (typeof(locals.pageCloseable) != "undefined") {
        this.pageCloseable = MochiKit.Base.bool(locals.pageCloseable);
        modified = true;
    }
    if (modified && this.parentNode &&
        typeof(this.parentNode._updateStatus) == "function") {
        this.parentNode._updateStatus();
    }
    MochiKit.DOM.updateNodeAttributes(this, attrs);
};

/**
 * Handles the page enter event. This method is called by a parent
 * page container widget, such as a TabContainer or a Wizard. It will
 * reset the form validations (optional), show the pane (optional),
 * and finally trigger the "onenter" event.
 *
 * @param {Object} opts the page transition options
 * @param {Boolean} [opts.show] the show pane flag, defaults to true
 * @param {Boolean} [opts.validateReset] the form validation reset
 *            flag, used to clear all form validations in the pane
 */
RapidContext.Widget.Pane.prototype._handleEnter = function (opts) {
    opts = MochiKit.Base.update({ show: true, validateReset: false }, opts);
    if (MochiKit.Base.bool(opts.validateReset)) {
        var forms = this.getElementsByTagName("FORM");
        for (var i = 0; i < forms.length; i++) {
            if (typeof(forms[i].validateReset) == "function") {
                forms[i].validateReset();
            }
        }
    }
    if (MochiKit.Base.bool(opts.show)) {
        this.show();
        RapidContext.Util.resizeElements(this);
    }
    RapidContext.Widget.emitSignal(this, "onenter");
};

/**
 * Handles the page exit event. This method is called by a parent
 * page container widget, such as a TabContainer or a Wizard. It will
 * validate the form (optional), unfocus all form fields, hide the
 * pane (optional), and finally trigger the "onexit" event.
 *
 * @param {Object} opts the page transition options
 * @param {Boolean} [opts.hide] the hide pane flag, defaults to true
 * @param {Boolean} [opts.validate] the form validation flag, used to
 *            check all forms in the page for valid entries before
 *            proceeding, defaults to false
 *
 * @return {Boolean} true if the page exit event completed, or
 *         false if it was cancelled (due to validation errors)
 */
RapidContext.Widget.Pane.prototype._handleExit = function (opts) {
    opts = MochiKit.Base.update({ hide: true, validate: false }, opts);
    if (MochiKit.Base.bool(opts.validate)) {
        var forms = this.getElementsByTagName("FORM");
        for (var i = 0; i < forms.length; i++) {
            if (typeof(forms[i].validate) == "function") {
                var res = forms[i].validate();
                // TODO: handle MochiKit.Async.Deferred?
                if (!res) {
                    return false;
                }
            }
        }
    }
    this.blurAll();
    if (MochiKit.Base.bool(opts.hide)) {
        this.hide();
    }
    RapidContext.Widget.emitSignal(this, "onexit");
    return true;
};
