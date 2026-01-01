/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2026 Per Cederberg. All rights reserved.
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

    // Export module API
    let RapidContext = window.RapidContext || (window.RapidContext = {});
    let module = RapidContext.UI || (RapidContext.UI = {});
    Object.assign(module, {
        buildUI: RapidContext.deprecatedFunction("RapidContext.UI.buildUI() is deprecated, use create() instead")
    });

})(this);
