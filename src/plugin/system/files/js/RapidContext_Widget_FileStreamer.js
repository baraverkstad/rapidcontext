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
 * Creates a new file streamer widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {String} [attrs.url] the URL to post the data to, defaults
 *            to window.location.href
 * @param {String} [attrs.name] the file input field name,
 *            defaults to 'file'
 * @param {String} [attrs.size] the file input size, defaults to '30'
 * @param {Boolean} [attrs.hidden] the hidden widget flag, defaults to false
 *
 * @return {Widget} the widget DOM node
 *
 * @class The file streamer widget class. This widget is used to
 *     provide a file upload (file input) control that can stream the
 *     selected file to the server. The file data is always sent
 *     asynchronously (i.e. in the background) to allow the rest of
 *     the web page to remain active also during the potentially long
 *     delays caused by sending large amounts of data. The widget
 *     creates its own `<iframe>` HTML element inside which the actual
 *     `<form>` and `<input>` elements are created automatically.
 * @extends RapidContext.Widget
 *
 * @example {JavaScript}
 * var file = RapidContext.Widget.FileStreamer({ url: "rapidcontext/upload/my_id" });
 * form.addAll(file);
 * MochiKit.Signal.connect(file, "onselect", function () {
 *     file.hide();
 *     // add code to show progress bar
 * });
 */
RapidContext.Widget.FileStreamer = function (attrs) {
    var defs = {
        src: "about:blank",
        scrolling: "no",
        border: "0",
        frameborder: "0"
    };
    var o = MochiKit.DOM.createDOM("iframe", defs);
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.FileStreamer);
    o.addClass("widgetFileStreamer");
    o.setAttrs(MochiKit.Base.update({ url: "", name: "file", size: "30" }, attrs));
    // TODO: create some kind of utility function for these idioms
    var links = MochiKit.Selector.findDocElements("link[href*=widget.css]");
    if (links.length > 0) {
        o.cssUrl = links[0].href;
    }
    o.onload = o._handleLoad;
    return o;
};

// Register widget class
RapidContext.Widget.Classes.FileStreamer = RapidContext.Widget.FileStreamer;

/**
 * Returns the widget container DOM node.
 *
 * @return {Node} returns null, since child nodes are not supported
 */
RapidContext.Widget.FileStreamer.prototype._containerNode = function () {
    return null;
};

/**
 * Emitted when the file upload begins. This event is triggered by
 * the user selecting a file to upload, which automatically starts
 * the upload. This event signal carries no event information.
 *
 * @name RapidContext.Widget.FileStreamer#onselect
 * @event
 */

/**
 * Emitted when the file upload has completed. This event signal
 * carries no event information.
 *
 * @name RapidContext.Widget.FileStreamer#onupload
 * @event
 */

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {String} [attrs.url] the URL to post the data to
 * @param {String} [attrs.name] the file input field name
 * @param {String} [attrs.size] the file input size
 * @param {Boolean} [attrs.hidden] the hidden widget flag
 */
RapidContext.Widget.FileStreamer.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
    var locals = RapidContext.Util.mask(attrs, ["url", "name", "size"]);
    if (typeof(locals.url) != "undefined") {
        this.formUrl = RapidContext.Util.resolveURI(locals.url);
    }
    if (typeof(locals.name) != "undefined") {
        this.inputName = locals.param;
    }
    if (typeof(locals.size) != "undefined") {
        this.inputSize = locals.size;
    }
    // TODO: update form if already created, or recreate?
    this.__setAttrs(attrs);
};

/**
 * Handles the iframe onload event.
 */
RapidContext.Widget.FileStreamer.prototype._handleLoad = function () {
    var doc = this.contentDocument;
    if (doc.location.href == this.formUrl) {
        RapidContext.Widget.emitSignal(this, "onupload");
    }
    MochiKit.DOM.withDocument(doc, MochiKit.Base.bind("_initDocument", this));
};

/**
 * Handles the file input onchange event.
 */
RapidContext.Widget.FileStreamer.prototype._handleChange = function () {
    RapidContext.Widget.emitSignal(this, "onselect");
    var form = this.contentDocument.getElementsByTagName("form")[0];
    form.submit();
    form.appendChild(RapidContext.Widget.Overlay());
};

/**
 * Creates the document form and file input element.
 */
RapidContext.Widget.FileStreamer.prototype._initDocument = function () {
    var doc = this.contentDocument;
    var head = doc.getElementsByTagName("head")[0];
    var body = doc.body;
    if (head == null) {
        head = doc.createElement("head");
        body.parentElement.insertBefore(head, body);
    }
    var linkAttrs = { rel: "stylesheet", href: this.cssUrl, type: "text/css" };
    var link = MochiKit.DOM.createDOM("link", linkAttrs);
    head.appendChild(link);
    var inputAttrs = { type: "file", name: this.inputName, size: this.inputSize };
    var input = MochiKit.DOM.INPUT(inputAttrs);
    var formAttrs = { method: "POST", action: this.formUrl, enctype: "multipart/form-data" };
    var form = MochiKit.DOM.FORM(formAttrs, input);
    input.onchange = MochiKit.Base.bind("_handleChange", this);
    body.className = "widgetFileStreamer";
    MochiKit.DOM.replaceChildNodes(body, form);
};

/**
 * Handles widget resize calls, so that the `<iframe>` can be adjusted
 * to the file input field.
 *
 * @private
 */
RapidContext.Widget.FileStreamer.prototype.resizeContent = function () {
    var doc = this.contentDocument;
    if (doc != null && typeof(doc.getElementsByTagName) === "function") {
        var form = doc.getElementsByTagName("form")[0];
        if (form != null) {
            var input = form.firstChild;
            this.width = input.clientWidth + 2;
            this.height = Math.max(24, input.clientHeight);
        }
    }
};
