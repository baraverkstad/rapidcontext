/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2024 Per Cederberg. All rights reserved.
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
 * Provides functions for managing the app user interface.
 * @namespace RapidContext.UI
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
     * @param {...(String|Error)} [arg] the messages or errors to display
     *
     * @memberof RapidContext.UI
     */
    function showError() {
        var msg = Array.from(arguments).map(function (arg) {
            var isError = arg instanceof Error && arg.message;
            return isError ? arg.message : arg;
        }).join(", ");
        console.warn(msg, ...arguments);
        if (!errorDialog) {
            var xml = [
                "<Dialog title='Error' system='true' style='width: 25rem;'>",
                "  <i class='fa fa-exclamation-circle fa-3x color-danger mr-3'></i>",
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
            errorDialog = RapidContext.UI.create(xml);
            window.document.body.append(errorDialog);
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
     * @return {Array|Object} an array or an object with the root
     *         widget(s) created
     *
     * @deprecated Use RapidContext.UI.create() instead.
     *
     * @memberof RapidContext.UI
     */
    function buildUI(node, ids) {
        console.warn("deprecated: call to RapidContext.UI.buildUI(), use create() instead");
        if (node.documentElement) {
            return buildUI(node.documentElement.childNodes, ids);
        } else if (node && node.item && typeof(node.length) == "number") {
            return Array.from(node).map((el) => buildUI(el, ids)).filter(Boolean);
        } else {
            try {
                let el = RapidContext.UI.create(node);
                if (el) {
                    [el.matches("[id]") && el, ...el.querySelectorAll("[id]")]
                        .filter(Boolean)
                        .forEach((el) => ids[el.attributes.id.value] = el);
                }
                return el;
            } catch (e) {
                console.error("Failed to build UI element", node, e);
                return null;
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

    // Export module API
    let RapidContext = window.RapidContext || (window.RapidContext = {});
    let module = RapidContext.UI || (RapidContext.UI = {});
    Object.assign(module, { showError, buildUI, connectProc });

})(this);
