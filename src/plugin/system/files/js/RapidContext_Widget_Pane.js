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
RapidContext.Widget = RapidContext.Widget || { Classes: {} };

/**
 * Creates a new pane widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {string} [attrs.pageTitle] the page title used when inside
 *            a page container, defaults to "Page"
 * @param {string|Object} [attrs.pageStatus] the page status used
 *            when inside a page container, use one of the predefined
 *            status constants in this class, defaults to `ANY`
 * @param {boolean} [attrs.pageCloseable] the page closeable flag
 *            used when inside some page containers, defaults to
 *            `false`
 * @param {boolean} [attrs.hidden] the hidden widget flag, defaults to false
 * @param {...(string|Node|Array)} [child] the child widgets or DOM nodes
 *
 * @return {Widget} the widget DOM node
 *
 * @class The pane widget class. Used to create the simplest form of element
 *     container. It is also used inside various types of paged containers,
 *     such as a `TabContainer`, a `Wizard` or similar. A pane only uses a
 *     `<div>` HTML element, and supports being hidden and shown according to
 *     any page transitions required by a parent container.
 * @property {string} pageTitle [read-only] The current page title.
 * @property {Object} pageStatus [read-only] The current page status.
 * @property {boolean} pageCloseable [read-only] The current page
 *               closeable flag value.
 * @extends RapidContext.Widget
 *
 * @example <caption>JavaScript</caption>
 * let h1 = RapidContext.UI.H1({}, "Hello, world!");
 * let helloPane = RapidContext.Widget.Pane({}, h1);
 *
 * @example <caption>User Interface XML</caption>
 * <Pane id="helloPane">
 *   <h1>Hello, world!</h1>
 * </Pane>
 */
RapidContext.Widget.Pane = function (attrs/*, ... */) {
    let o = document.createElement("div");
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.Pane);
    o.addClass("widgetPane");
    o.setAttrs(Object.assign({ pageTitle: "Page", pageStatus: "ANY", pageCloseable: false }, attrs));
    o.addAll(Array.from(arguments).slice(1));
    return o;
};

// Register widget class
RapidContext.Widget.Classes.Pane = RapidContext.Widget.Pane;

/**
 * Emitted when the pane is shown for viewing in a container widget.
 *
 * @name RapidContext.Widget.Pane#onenter
 * @event
 */

/**
 * Emitted when the pane is hidden from view in a container widget.
 *
 * @name RapidContext.Widget.Pane#onexit
 * @event
 */

/**
 * Emitted when the pane is closed (removed) in a `TabContainer`.
 *
 * @name RapidContext.Widget.Pane#onclose
 * @event
 */

/**
 * The default page status. Allows page transitions both to the
 * previous and the next page.
 */
RapidContext.Widget.Pane.ANY = { previous: true, next: true };

/**
 * The forward-only page status. Allows transitions only to the next
 * page.
 */
RapidContext.Widget.Pane.FORWARD = { previous: false, next: true };

/**
 * The backward-only page status. Allows transitions only to the
 * previous page.
 */
RapidContext.Widget.Pane.BACKWARD = { previous: true, next: false };

/**
 * The working page status. Will disable transitions both to the
 * previous and the next page. The page container may also display a
 * cancel button to allow user cancellation of the ongoing operation.
 */
RapidContext.Widget.Pane.WORKING = { previous: false, next: false };

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {string} [attrs.pageTitle] the page title used when inside
 *            a page container
 * @param {string|Object} [attrs.pageStatus] the page status used
 *            when inside a page container, use one of the predefined
 *            status constants in this class
 * @param {boolean} [attrs.pageCloseable] the page closeable flag
 *            used when inside some page containers
 * @param {boolean} [attrs.hidden] the hidden widget flag
 */
RapidContext.Widget.Pane.prototype.setAttrs = function (attrs) {
    attrs = Object.assign({}, attrs);
    if ("pageStatus" in attrs && typeof(attrs.pageStatus) != "object") {
        attrs.pageStatus = RapidContext.Widget.Pane[attrs.pageStatus] || RapidContext.Widget.Pane.ANY;
    }
    if ("pageCloseable" in attrs) {
        attrs.pageCloseable = RapidContext.Data.bool(attrs.pageCloseable);
    }
    this.__setAttrs(attrs);
    if (this.parentNode && typeof(this.parentNode._updateStatus) == "function") {
        this.parentNode._updateStatus();
    }
};

/**
 * Handles the page enter event. This method is called by a parent
 * page container widget, such as a `TabContainer` or a `Wizard`. It will
 * reset the form validations (optional), show the pane (optional),
 * and finally trigger the "onenter" event.
 *
 * @param {Object} opts the page transition options
 * @param {boolean} [opts.show] the show pane flag, defaults to `true`
 * @param {boolean} [opts.validateReset] the form validation reset
 *            flag, used to clear all form validations in the pane
 */
RapidContext.Widget.Pane.prototype._handleEnter = function (opts) {
    opts = Object.assign({ show: true, validateReset: false }, opts);
    if (RapidContext.Data.bool(opts.validateReset)) {
        let forms = this.getElementsByTagName("FORM");
        for (let i = 0; i < forms.length; i++) {
            if (typeof(forms[i].validateReset) == "function") {
                forms[i].validateReset();
            }
        }
    }
    if (RapidContext.Data.bool(opts.show)) {
        this.show();
    }
    this.emit("enter");
};

/**
 * Handles the page exit event. This method is called by a parent
 * page container widget, such as a `TabContainer` or a `Wizard`. It will
 * validate the form (optional), unfocus all form fields, hide the
 * pane (optional), and finally trigger the "onexit" event.
 *
 * @param {Object} opts the page transition options
 * @param {boolean} [opts.hide] the hide pane flag, defaults to `true`
 * @param {boolean} [opts.validate] the form validation flag, used to
 *            check all forms in the page for valid entries before
 *            proceeding, defaults to `false`
 *
 * @return {boolean} `true` if the page exit event completed, or
 *         `false` if it was cancelled (due to validation errors)
 */
RapidContext.Widget.Pane.prototype._handleExit = function (opts) {
    function callFirst(obj, methods) {
        for (let i = 0; i < methods.length; i++) {
            let k = methods[i];
            if (typeof(obj[k]) === "function") {
                return obj[k]();
            }
        }
        return undefined;
    }
    opts = Object.assign({ hide: true, validate: false }, opts);
    if (RapidContext.Data.bool(opts.validate)) {
        let forms = this.getElementsByTagName("FORM");
        for (let i = 0; i < forms.length; i++) {
            if (callFirst(forms[i], ["validate", "checkValidity"]) === false) {
                return false;
            }
        }
    }
    this.blurAll();
    if (RapidContext.Data.bool(opts.hide)) {
        this.hide();
    }
    this.emit("exit");
    return true;
};
