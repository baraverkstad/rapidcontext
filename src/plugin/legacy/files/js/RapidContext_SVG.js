/*
 * RapidContext <https://www.rapidcontext.com/>
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
 * See the RapidContext LICENSE for more details.
 */

// Create default RapidContext object
if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}

/**
 * @name RapidContext.SVG
 * @namespace Provides functions for creating embedded SVG images.
 */
if (typeof(RapidContext.SVG) == "undefined") {
    RapidContext.SVG = {};
}

/**
 * Creates an SVG document node.
 *
 * @function
 * @param {Object} [attrs] the optional node attributes
 * @param {Object} [...] the nodes or text to add as children
 *
 * @return {Node} the SVG DOM document node created
 */
RapidContext.SVG.SVG =
    RapidContext.Util.createDOMFuncExt(RapidContext.Util.NS.SVG, "svg",
                                   [],
                                   { version: "1.1", baseProfile: "full" });

/**
 * Creates an SVG definitions node.
 *
 * @function
 * @param {Object} [attrs] the optional node attributes
 * @param {Object} [...] the nodes or text to add as children
 *
 * @return {Node} the SVG DOM document node created
 */
RapidContext.SVG.DEFS =
    RapidContext.Util.createDOMFuncExt(RapidContext.Util.NS.SVG, "defs");

/**
 * Creates an SVG group node.
 *
 * @function
 * @param {Object} [attrs] the optional node attributes
 * @param {Object} [...] the nodes or text to add as children
 *
 * @return {Node} the SVG DOM document node created
 */
RapidContext.SVG.G =
    RapidContext.Util.createDOMFuncExt(RapidContext.Util.NS.SVG, "g");

/**
 * Creates an SVG line node.
 *
 * @function
 * @param {String} x1 the x1 coordinate value
 * @param {String} y1 the y1 coordinate value
 * @param {String} x2 the x2 coordinate value
 * @param {String} y2 the y2 coordinate value
 * @param {Object} [attrs] the optional node attributes
 *
 * @return {Node} the SVG DOM document node created
 */
RapidContext.SVG.LINE =
    RapidContext.Util.createDOMFuncExt(RapidContext.Util.NS.SVG, "line",
                                   ["x1", "y1", "x2", "y2"]);

/**
 * Creates an SVG rectangle node.
 *
 * @function
 * @param {String} x the x coordinate value
 * @param {String} y the y coordinate value
 * @param {String} width the width value
 * @param {String} height the height value
 * @param {Object} [attrs] the optional node attributes
 *
 * @return {Node} the SVG DOM document node created
 */
RapidContext.SVG.RECT =
    RapidContext.Util.createDOMFuncExt(RapidContext.Util.NS.SVG, "rect",
                                   ["x", "y", "width", "height"]);

/**
 * Creates an SVG circle node.
 *
 * @function
 * @param {String} cx the center x coordinate value
 * @param {String} cy the center y coordinate value
 * @param {String} r the radius value
 * @param {Object} [attrs] the optional node attributes
 *
 * @return {Node} the SVG DOM document node created
 */
RapidContext.SVG.CIRCLE =
    RapidContext.Util.createDOMFuncExt(RapidContext.Util.NS.SVG, "circle",
                                   ["cx", "cy", "r"]);

/**
 * Creates an SVG path node.
 *
 * @function
 * @param {String} d the path data value
 * @param {Object} [attrs] the optional node attributes
 *
 * @return {Node} the SVG DOM document node created
 */
RapidContext.SVG.PATH =
    RapidContext.Util.createDOMFuncExt(RapidContext.Util.NS.SVG, "path",
                                   ["d"]);

/**
 * Creates an SVG text node.
 *
 * @function
 * @param {String} x the x coordinate value
 * @param {String} y the y coordinate value
 * @param {Object} [attrs] the optional node attributes
 * @param {Object} [...] the text to add as children
 *
 * @return {Node} the SVG DOM document node created
 */
RapidContext.SVG.TEXT =
    RapidContext.Util.createDOMFuncExt(RapidContext.Util.NS.SVG, "text",
                                   ["x", "y"]);

/**
 * Creates an SVG radial gradient node.
 *
 * @function
 * @param {String} id the id of the node
 * @param {Object} [attrs] the optional node attributes
 * @param {Object} [...] the stop nodes to add as children
 *
 * @return {Node} the SVG DOM document node created
 */
RapidContext.SVG.RADIALGRADIENT =
    RapidContext.Util.createDOMFuncExt(RapidContext.Util.NS.SVG, "radialGradient",
                                   ["id"],
                                   { gradientUnits: "objectBoundingBox",
                                     cx: "0.5", cy: "0.5", r: "0.5" });

/**
 * Creates an SVG gradient stop node.
 *
 * @function
 * @param {String} offset the stop offset
 * @param {String} color the stop color
 * @param {Object} [attrs] the optional node attributes
 *
 * @return {Node} the SVG DOM document node created
 */
RapidContext.SVG.STOP =
    RapidContext.Util.createDOMFuncExt(RapidContext.Util.NS.SVG, "stop",
                                   ["offset", "stop-color"]);

/**
 * Moves a node to the top of the SVG drawing order (i.e. z-index).
 * Note that this will only have effect on other SVG DOM nodes with
 * the same parent node. Otherwise, the node must be moved down in
 * the SVG DOM tree by changing parent node.
 *
 * @param {Node/String} node the SVG DOM node or unique id
 */
RapidContext.SVG.moveToTop = function (node) {
    node = MochiKit.DOM.getElement(node);
    if (node != null) {
        var parent = node.parentNode;
        if (parent && parent.lastChild !== node) {
            parent.appendChild(node);
        }
    }
};

/**
 * Moves a node to the bottom of the SVG drawing order (i.e. z-index).
 * Note that this will only have effect on other SVG DOM nodes with
 * the same parent node. Otherwise, the node must be moved up in
 * the SVG DOM tree by changing parent node.
 *
 * @param {Node/String} node the SVG DOM node or unique id
 */
RapidContext.SVG.moveToBottom = function (node) {
    node = MochiKit.DOM.getElement(node);
    if (node != null) {
        var parent = node.parentNode;
        if (parent && parent.firstChild !== node) {
            parent.insertBefore(node, parent.firstChild);
        }
    }
};

/**
 * Adds a rotation transform to a SVG DOM node. Any previous
 * rotation transform will be kept intact.
 *
 * @param {Node/String} node the SVG DOM node or unique id
 * @param {String/Number} angle the numeric angle
 * @param {String/Number} [x] the x coordinate value
 * @param {String/Number} [y] the y coordinate value
 */
RapidContext.SVG.rotate = function (node, angle, x, y) {
    var str = MochiKit.DOM.getNodeAttribute(node, "transform");
    x = x || 0;
    y = y || 0;
    if (str == null || str == "") {
        str = "";
    } else {
        str += " ";
    }
    str += "rotate(" + angle + "," + x + "," + y + ")";
    MochiKit.DOM.setNodeAttribute(node, "transform", str);
};
