/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2009 Per Cederberg & Dynabyte AB.
 * All rights reserved.
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

// Create default RapidContext object
if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}

/**
 * @name RapidContext.UI
 * @namespace Provides functions for creating and handling the
 *     user interface of an app.
 */
if (typeof(RapidContext.UI) == "undefined") {
    RapidContext.UI = {};
}

/**
 * Displays an error message for the user. This operation may or may
 * not block the user interface, while the message is being
 * displayed (depending on implementation). This function can be
 * used as an errback function in deferred calls. All arguments to
 * this function will be concatenated and displayed.
 *
 * @param {String/Error} [args] the messages or errors to display
 */
RapidContext.UI.showError = function () {
    if (arguments.length == 1 &&
        arguments[0] instanceof MochiKit.Async.CancelledError) {
        // Ignore async cancellation errors
        return;
    }
    var msg = "";
    for (var i = 0; i < arguments.length; i++) {
        if (arguments[i] == null) {
            msg += "null";
        } else if (arguments[i] instanceof Error) {
            msg += arguments[i].message;
        } else {
            msg += arguments[i].toString();
        }
    }
    alert("Error: " + msg);
    return arguments[arguments.length - 1];
};

/**
 * Creates a tree of widgets from a parsed XML document. This
 * function will call createWidget() for any XML element node found,
 * performing some basic adjustments on the element attributes
 * before sending them as attributes to the widget constructor. Text
 * nodes with non-whitespace content will be mapped to HTML DOM text
 * nodes.
 *
 * @param {Object} node the XML document or node
 * @param {Object} [ids] the optional node id mappings
 *
 * @return {Array/Object} an array or an object with the root
 *         widget(s) created
 */
RapidContext.UI.buildUI = function (node, ids) {
    if (node.documentElement) {
        return RapidContext.UI.buildUI(node.documentElement.childNodes, ids);
    } else if (typeof(node.item) != "undefined" && typeof(node.length) == "number") {
        var iter = MochiKit.Iter.repeat(ids, node.length);
        iter = MochiKit.Iter.imap(RapidContext.UI.buildUI, node, iter);
        iter = MochiKit.Iter.ifilterfalse(MochiKit.Base.isUndefinedOrNull, iter);
        return MochiKit.Iter.list(iter);
    } else if (node.nodeType === 1) { // Node.ELEMENT_NODE
        try {
            return RapidContext.UI._buildUIElem(node, ids);
        } catch (e) {
            LOG.error("Failed to build UI element <" + node.nodeName + ">", e.message);
        }
    } else if (node.nodeType === 3) { // Node.TEXT_NODE
        var str = node.nodeValue;
        if (str != null && MochiKit.Format.strip(str) != "") {
            return RapidContext.Util.createTextNode(str.replace(/\s+/g, " "));
        }
    }
    // TODO: handling of CDATA nodes to escape text?
    return null;
};

/**
 * Creates a widget from a parsed XML element. This function will
 * call createWidget(), performing some basic adjustments on the
 * element attributes before sending them as attributes to the widget
 * constructor.
 *
 * @param {Object} node the XML document or node
 * @param {Object} [ids] the optional node id mappings
 *
 * @return {Object} an object with the widget created
 */
RapidContext.UI._buildUIElem = function (node, ids) {
    var name = node.nodeName;
    var attrs = RapidContext.Util.dict(RapidContext.Util.attributeArray(node));
    var locals = RapidContext.Util.mask(attrs, ["id", "w", "h", "a", "class", "style"]);
    var children = RapidContext.UI.buildUI(node.childNodes, ids);
    if (RapidContext.Widget.Classes[name]) {
        if (name == "Table" && attrs.multiple) {
            // TODO: remove deprecated code, eventually...
            LOG.warning("Table 'multiple' attribute is deprecated, use 'select'");
            attrs.select = MochiKit.Base.bool(attrs.multiple) ? "multiple" : "one";
            delete attrs.multiple;
        }
        var widget = RapidContext.Widget.createWidget(name, attrs, children);
    } else {
        var widget = MochiKit.DOM.createDOM(name, attrs, children);
    }
    if (locals.id) {
        if (ids) {
            ids[locals.id] = widget;
        } else {
            widget.id = locals.id;
        }
    }
    if (locals.w || locals.h || locals.a) {
        RapidContext.Util.registerSizeConstraints(widget, locals.w, locals.h, locals.a);
    }
    if (locals["class"]) {
        var classes = MochiKit.Format.strip(locals["class"]).split(" ");
        if (typeof(widget.addClass) == "function") {
            widget.addClass.apply(widget, classes);
        } else {
            for (var i = 0; i < classes.length; i++) {
                MochiKit.DOM.addElementClass(widget, classes[i]);
            }
        }
    }
    if (locals.style) {
        var styles = {};
        var parts = locals.style.split(";");
        for (var i = 0; i < parts.length; i++) {
            var a = parts[i].split(":");
            var k = MochiKit.Format.strip(a[0]);
            if (k != "" && a.length > 1) {
                styles[k] = MochiKit.Format.strip(a[1]);
            }
        }
        try {
            if (typeof(widget.setAttrs) == "function") {
                widget.setAttrs({ style: styles });
            } else {
                MochiKit.Style.setStyle(widget, styles);
            }
        } catch (e) {
            LOG.error("Failed to style UI element <" + name + ">", e.message);
        }
    }
    return widget;
};

/**
 * Connects the default UI signals for a procedure. This includes a default
 * error handler, a loading icon with cancellation handler and a reload icon
 * with the appropriate click handler.
 *
 * @param {Procedure} proc the RapidContext.Procedure instance
 * @param {Icon} [loadingIcon] the loading icon, or null
 * @param {Icon} [reloadIcon] the reload icon, or null
 */
RapidContext.UI.connectProc = function (proc, loadingIcon, reloadIcon) {
    // TODO: error signal not automatically cleaned up on stop()...
    MochiKit.Signal.connect(proc, "onerror", RapidContext.UI, "showError");
    if (loadingIcon) {
        MochiKit.Signal.connect(proc, "oncall", loadingIcon, "show");
        MochiKit.Signal.connect(proc, "onresponse", loadingIcon, "hide");
        MochiKit.Signal.connect(loadingIcon, "onclick", proc, "cancel");
    }
    if (reloadIcon) {
        MochiKit.Signal.connect(proc, "oncall", reloadIcon, "hide");
        MochiKit.Signal.connect(proc, "onresponse", reloadIcon, "show");
        MochiKit.Signal.connect(reloadIcon, "onclick", proc, "recall");
    }
};
