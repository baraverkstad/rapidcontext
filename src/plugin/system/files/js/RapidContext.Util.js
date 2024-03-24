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

// Create default RapidContext object
if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}

/**
 * Provides utility functions for basic objects, arrays, DOM nodes and CSS.
 * These functions are complementary to what is available in MochiKit and/or
 * jQuery.
 * @namespace RapidContext.Util
 */
if (typeof(RapidContext.Util) == "undefined") {
    RapidContext.Util = {};
}


// General utility functions

/**
 * Converts a string to a title-cased string. All word boundaries are replaced
 * with a single space and the subsequent character is capitalized.
 *
 * All underscore ("_"), hyphen ("-") and lower-upper character pairs are
 * recognized as word boundaries. Note that this function does not change the
 * capitalization of other characters in the string.
 *
 * @param {string} str the string to convert
 *
 * @return {string} the converted string
 *
 * @example
 * RapidContext.Util.toTitleCase("a short heading")
 * ==> "A Short Heading"
 *
 * @example
 * RapidContext.Util.toTitleCase("camelCase")
 * ==> "Camel Case"
 *
 * @example
 * RapidContext.Util.toTitleCase("bounding-box")
 * ==> "Bounding Box"
 *
 * @example
 * RapidContext.Util.toTitleCase("UPPER_CASE_VALUE")
 * ==> "UPPER CASE VALUE"
 */
RapidContext.Util.toTitleCase = function (str) {
    str = str.replace(/[._-]+/g, " ").trim();
    str = str.replace(/[a-z][A-Z]/g, function (match) {
        return match.charAt(0) + " " + match.charAt(1);
    });
    str = str.replace(/(^|\s)[a-z]/g, function (match) {
        return match.toUpperCase();
    });
    return str;
};


// DOM utility functions

/**
 * Blurs (unfocuses) a specified DOM node and all relevant child nodes. This
 * function will recursively blur all `<a>`, `<button>`, `<input>`,
 * `<textarea>` and `<select>` child nodes found.
 *
 * @param {Object} node the HTML DOM node
 */
RapidContext.Util.blurAll = function (node) {
    node.blur();
    var tags = ["A", "BUTTON", "INPUT", "TEXTAREA", "SELECT"];
    for (var i = 0; i < tags.length; i++) {
        var nodes = node.getElementsByTagName(tags[i]);
        for (var j = 0; j < nodes.length; j++) {
            nodes[j].blur();
        }
    }
};

/**
 * Registers size constraints for the element width and/or height. The
 * constraints may either be fixed numeric values or simple arithmetic (in a
 * string). The formulas will be converted to CSS calc() expressions.
 *
 * Legacy constraint functions are still supported and must take two arguments
 * (parent width and height) and should return a number. The returned number is
 * set as the new element width or height (in pixels). Any returned value will
 * also be bounded by the parent element size to avoid calculation errors.
 *
 * @param {Object} node the HTML DOM node
 * @param {number|string|function} [width] the width constraint
 * @param {number|string|function} [height] the height constraint
 *
 * @deprecated Use CSS width and height with calc() instead.
 *
 * @example
 * RapidContext.Util.registerSizeConstraints(node, "50%-20", "100%");
 * ==> Sets width to 50%-20 px and height to 100% of parent dimension
 *
 * @example
 * RapidContext.Util.resizeElements(node, otherNode);
 * ==> Evaluates the size constraints for both nodes
 */
RapidContext.Util.registerSizeConstraints = function (node, width, height) {
    function toCSS(val) {
        if (/[+-]/.test(val)) {
            val = "calc( " + val.replace(/[+-]/g, " $& ") + " )";
        }
        val = val.replace(/(\d)( |$)/g, "$1px$2");
        return val;
    }
    console.warn("deprecated: call to RapidContext.Util.registerSizeConstraints(), use CSS calc() instead");
    node = MochiKit.DOM.getElement(node);
    if (typeof(width) == "number" || typeof(width) == "string") {
        node.style.width = toCSS(String(width));
    } else if (typeof(width) == "function") {
        console.info("registerSizeConstraints: width function support will be removed", node);
        node.sizeConstraints = node.sizeConstraints || { w: null, h: null };
        node.sizeConstraints.w = width;
    }
    if (typeof(height) == "number" || typeof(height) == "string") {
        node.style.height = toCSS(String(height));
    } else if (typeof(height) == "function") {
        console.info("registerSizeConstraints: height function support will be removed", node);
        node.sizeConstraints = node.sizeConstraints || { w: null, h: null };
        node.sizeConstraints.h = height;
    }
};

/**
 * Resizes one or more DOM nodes using their registered size constraints and
 * their parent element sizes. The resize operation will only modify those
 * elements that have constraints, but will perform a depth-first recursion
 * over all element child nodes as well.
 *
 * Partial constraints are accepted, in which case only the width or the height
 * is modified. Aspect ratio constraints are applied after the width and height
 * constraints. The result will always be bounded by the parent element width
 * or height.
 *
 * The recursive descent of this function can be limited by adding a
 * `resizeContent` function to a DOM node. Such a function will be called to
 * handle all subnode resizing, making it possible to limit or omitting the
 * DOM tree traversal.
 *
 * @param {...Node} node the HTML DOM nodes to resize
 *
 * @deprecated Use CSS width and height with calc() instead.
 *
 * @example
 * RapidContext.Util.resizeElements(node);
 * ==> Evaluates the size constraints for a node and all child nodes
 *
 * @example
 * elem.resizeContent = () => {};
 * ==> Assigns a no-op child resize handler to elem
 */
RapidContext.Util.resizeElements = function (/* ... */) {
    console.warn("deprecated: call to RapidContext.Util.resizeElements(), use CSS calc() instead");
    Array.from(arguments).forEach(function (arg) {
        var node = MochiKit.DOM.getElement(arg);
        if (node && node.nodeType === 1 && node.parentNode && node.sizeConstraints) {
            var ref = { w: node.parentNode.w, h: node.parentNode.h };
            if (ref.w == null && ref.h == null) {
                ref = MochiKit.Style.getElementDimensions(node.parentNode, true);
            }
            var dim = RapidContext.Util._evalConstraints(node.sizeConstraints, ref);
            MochiKit.Style.setElementDimensions(node, dim);
            node.w = dim.w;
            node.h = dim.h;
        }
        if (node && typeof(node.resizeContent) == "function") {
            try {
                node.resizeContent();
            } catch (e) {
                console.error("Error in resizeContent()", node, e);
            }
        } else if (node && node.childNodes) {
            Array.from(node.childNodes).forEach(function (child) {
                if (child.nodeType === 1) {
                    RapidContext.Util.resizeElements(child);
                }
            });
        }
    });
};

/**
 * Evaluates the size constraint functions with a refeence dimension
 * object. This is an internal function used to encapsulate the
 * function calls and provide logging on errors.
 *
 * @param {Object} sc the size constraints object
 * @param {Object} ref the MochiKit.Style.Dimensions maximum
 *            reference values
 *
 * @return {Object} the MochiKit.Style.Dimensions with evaluated size
 *         constraint values (some may be null)
 */
RapidContext.Util._evalConstraints = function (sc, ref) {
    var w, h;
    if (typeof(sc.w) == "function") {
        try {
            w = Math.max(0, Math.min(ref.w, sc.w(ref.w, ref.h)));
        } catch (e) {
            console.error("Error evaluating width size constraint; " +
                          "w: " + ref.w + ", h: " + ref.h, e);
        }
    }
    if (typeof(sc.h) == "function") {
        try {
            h = Math.max(0, Math.min(ref.h, sc.h(ref.w, ref.h)));
        } catch (e) {
            console.error("Error evaluating height size constraint; " +
                          "w: " + ref.w + ", h: " + ref.h, e);
        }
    }
    if (w != null) {
        w = Math.floor(w);
    }
    if (h != null) {
        h = Math.floor(h);
    }
    return new MochiKit.Style.Dimensions(w, h);
};
