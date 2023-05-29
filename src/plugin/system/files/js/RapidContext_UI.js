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

/**
 * @name RapidContext.UI
 * @namespace Provides functions for managing the app user interface.
 */
(function (window) {

    // The global error dialog
    var errorDialog = null;

    /**
     * Displays an error message for the user. This operation may or may
     * not block the user interface, while the message is being
     * displayed (depending on implementation). All arguments will be
     * concatenated and displayed.
     *
     * @param {String/Error} [args] the messages or errors to display
     *
     * @memberof RapidContext.UI
     */
    function showError() {
        console.warn.apply(console, arguments);
        var msg = Array.from(arguments).map(function (arg) {
            var isError = arg instanceof Error && arg.message;
            return isError ? arg.message : arg;
        }).join(", ");
        if (!errorDialog) {
            var xml = [
                "<Dialog title='Error' system='true' style='width: 25rem;'>",
                "  <i class='fa fa-exclamation-circle fa-3x widget-red mr-3'></i>",
                "  <div class='inline-block vertical-top' style='width: calc(100% - 4em);'>",
                "    <h4>Error message:</h4>",
                "    <div class='text-pre-wrap' data-message='error'></div>",
                "  </div>",
                "  <div class='text-right mt-1'>",
                "    <Button icon='fa fa-lg fa-times' data-dialog='close'>",
                "      Close",
                "    </Button>",
                "  </div>",
                "</Dialog>"
            ].join("");
            errorDialog = buildUI(xml);
            window.document.body.appendChild(errorDialog);
        }
        if (errorDialog.isHidden()) {
            errorDialog.querySelector("[data-message]").innerText = msg;
            errorDialog.show();
        } else {
            var txt = errorDialog.querySelector("[data-message]").innerText;
            if (!txt.includes(msg)) {
                txt += "\n\n" + msg;
            }
            errorDialog.querySelector("[data-message]").innerText = txt;
        }
    }

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
     *
     * @memberof RapidContext.UI
     */
    function buildUI(node, ids) {
        if (typeof(node) === "string") {
            node = new DOMParser().parseFromString(node, "text/xml");
            return buildUI(node.documentElement, ids);
        } else if (node.documentElement) {
            return buildUI(node.documentElement.childNodes, ids);
        } else if (typeof(node.item) != "undefined" && typeof(node.length) == "number") {
            var res = [];
            var elems = Array.from(node);
            for (var i = 0; i < elems.length; i++) {
                var elem = buildUI(elems[i], ids);
                if (elem) {
                    res.push(elem);
                }
            }
            return res;
        } else if (node.nodeType === 1) { // Node.ELEMENT_NODE
            try {
                return _buildUIElem(node, ids);
            } catch (e) {
                RapidContext.Log.error("Failed to build UI element", node, e);
            }
        } else if (node.nodeType === 3 || node.nodeType === 4) {
            // Node.TEXT_NODE or Node.CDATA_SECTION_NODE
            var str = node.nodeValue;
            if (str && str.trim() && node.nodeType === 3) {
                return RapidContext.Util.createTextNode(str.replace(/\s+/g, " "));
            } else if (str && node.nodeType === 4) {
                return RapidContext.Util.createTextNode(str);
            }
        }
        return null;
    }

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
    function _buildUIElem(node, ids) {
        var name = node.nodeName;
        if (name == "style") {
            _buildUIStylesheet(MochiKit.DOM.scrapeText(node));
            node.parentNode.removeChild(node);
            return null;
        }
        var attrs = RapidContext.Util.dict(RapidContext.Util.attributeArray(node));
        var locals = RapidContext.Util.mask(attrs, ["id", "w", "h"]);
        var children = buildUI(node.childNodes, ids);
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
        if (locals.w || locals.h) {
            RapidContext.Util.registerSizeConstraints(widget, locals.w, locals.h);
        }
        return widget;
    }

    /**
     * Creates and injects a stylesheet element from a set of CSS rules.
     *
     * @param {String} css the CSS rules to inject
     */
    function _buildUIStylesheet(css) {
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
                    var rule = rules[j].replace(/\s+/, " ").trim();
                    style.styleSheet.addRule(rule, styles);
                }
            }
        }
    }

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
     *
     * @memberof RapidContext.UI
     */
    function connectProc(proc, loadingIcon, reloadIcon) {
        // TODO: error signal not automatically cleaned up on stop()...
        MochiKit.Signal.connect(proc, "onerror", showError);
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
    }

    // Create namespaces
    var RapidContext = window.RapidContext || (window.RapidContext = {});
    var module = RapidContext.UI || (RapidContext.UI = {});

    // Export namespace symbols
    Object.assign(module, { showError, buildUI, connectProc });

})(this);
