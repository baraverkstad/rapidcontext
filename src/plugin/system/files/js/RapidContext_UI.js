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
 * displayed (depending on implementation). All arguments will be
 * concatenated and displayed.
 *
 * @param {String/Error} [args] the messages or errors to display
 */
RapidContext.UI.showError = function () {
    var msg = Array.prototype.slice.call(arguments).map(function (arg) {
        var isError = arg instanceof Error && arg.message;
        return isError ? arg.message : arg;
    }).join(", ");
    alert("Error: " + msg);
};

/**
 * Creates a tree of widgets from a parsed XML document. This
 * function will call `createWidget()` for any XML element node found,
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
        var res = [];
        var elems = Array.prototype.slice.call(node);
        for (var i = 0; i < elems.length; i++) {
            var elem = RapidContext.UI.buildUI(elems[i], ids);
            if (elem) {
                res.push(elem);
            }
        }
        return res;
    } else if (node.nodeType === 1) { // Node.ELEMENT_NODE
        try {
            return RapidContext.UI._buildUIElem(node, ids);
        } catch (e) {
            RapidContext.Log.error("Failed to build UI element", node, e);
        }
    } else if (node.nodeType === 3 || node.nodeType === 4) {
        // Node.TEXT_NODE or Node.CDATA_SECTION_NODE
        var str = node.nodeValue;
        if (str && MochiKit.Format.strip(str) && node.nodeType === 3) {
            return RapidContext.Util.createTextNode(str.replace(/\s+/g, " "));
        } else if (str && node.nodeType === 4) {
            return RapidContext.Util.createTextNode(str);
        }
    }
    return null;
};

/**
 * Creates a widget from a parsed XML element. This function will
 * call `createWidget()`, performing some basic adjustments on the
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
    if (name == "style") {
        RapidContext.UI._buildUIStylesheet(MochiKit.DOM.scrapeText(node));
        node.parentNode.removeChild(node);
        return null;
    }
    var attrs = RapidContext.Util.dict(RapidContext.Util.attributeArray(node));
    var locals = RapidContext.Util.mask(attrs, ["id", "w", "h", "a"]);
    var children = RapidContext.UI.buildUI(node.childNodes, ids);
    var widget;
    if (RapidContext.Widget.Classes[name]) {
        if (name == "Table" && attrs.multiple) {
            // TODO: remove deprecated code, eventually...
            RapidContext.Log.warn("Table 'multiple' attribute is deprecated, use 'select'", node);
            attrs.select = MochiKit.Base.bool(attrs.multiple) ? "multiple" : "one";
            delete attrs.multiple;
        }
        widget = RapidContext.Widget.createWidget(name, attrs, children);
    } else {
        widget = MochiKit.DOM.createDOM(name, attrs, children);
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
    return widget;
};

/**
 * Creates and injects a stylesheet element from a set of CSS rules.
 *
 * @param {String} css the CSS rules to inject
 */
RapidContext.UI._buildUIStylesheet = function (css) {
    var style = document.createElement("style");
    style.setAttribute("type", "text/css");
    document.getElementsByTagName("head")[0].appendChild(style);
    try {
        style.innerHTML = css;
    } catch (e) {
        var parts = css.split(/\s*[{}]\s*/);
        for (var i = 0; i < parts.length; i += 2) {
            var rules = parts[i].split(/\s*,\s*/);
            var styles = parts[i + 1];
            for (var j = 0; j < rules.length; j++) {
                var rule = MochiKit.Format.strip(rules[j].replace(/\s+/, " "));
                style.styleSheet.addRule(rule, styles);
            }
        }
    }
};

/**
 * Connects the default UI signals for a procedure. This includes a default
 * error handler, a loading icon with cancellation handler and a reload icon
 * with the appropriate click handler.
 *
 * @param {Procedure} proc the `RapidContext.Procedure` instance
 * @param {Icon} [loadingIcon] the loading icon, or `null`
 * @param {Icon} [reloadIcon] the reload icon, or `null`
 *
 * @see RapidContext.Procedure
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
