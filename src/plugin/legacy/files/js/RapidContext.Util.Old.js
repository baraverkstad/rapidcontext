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

if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}
if (typeof(RapidContext.Util) == "undefined") {
    RapidContext.Util = {};
}

RapidContext.Util._logOnce = function(level, message) {
    var func = arguments.callee.caller;
    if (func.loggedMsg == null) {
        func.loggedMsg = {};
    }
    if (func.loggedMsg[message] !== true) {
        func.loggedMsg[message] = true
        MochiKit.Logging.logger.baseLog(level, message);
    }
};

/**
 * Checks if the specified value corresponds to false. This function
 * will equate false, undefined, null, 0, "", [], "false" and "null"
 * with a boolean false value.
 *
 * @param {Object} value the value to check
 *
 * @return {Boolean} true if the value corresponds to false, or
 *         false otherwise
 *
 * @deprecated Use MochiKit.Base.bool instead.
 */
RapidContext.Util.isFalse = function (value) {
    var msg = "RapidContext.Util.isFalse is deprecated. Use " +
              "MochiKit.Base.bool instead.";
    RapidContext.Util._logOnce('DEBUG', msg);
    return !MochiKit.Base.bool(value);
};

/**
 * Finds the index of an object in a list having a specific property
 * value. The value will be compared using MochiKit.Base.compare.
 *
 * @param {Array} lst the Array-like object to search
 * @param {String} key the property key name
 * @param {Object} value the property value to search for
 * @param {Number} [start] the search start index, defaults to 0
 * @param {Number} [end] the search end index, defaults to lst.length
 *
 * @return {Number} the array index found, or
 *         -1 if not found
 */
RapidContext.Util.findProperty = function (lst, key, value, start, end) {
    if (typeof(end) == "undefined" || end === null) {
        end = (lst == null) ? 0 : lst.length;
    }
    if (typeof(start) == "undefined" || start === null) {
        start = 0;
    }
    var cmp = MochiKit.Base.compare;
    for (var i = start; lst != null && i < end; i++) {
        if (lst[i] != null && cmp(lst[i][key], value) === 0) {
            return i;
        }
    }
    return -1;
};

/**
 * Returns a truncated copy of a string or an array. If the string
 * or array is shorter than the specified maximum length, the object
 * will be returned unmodified. If an optional tail string or array
 * is specified, additional elements will be removed from the object
 * to append it to the end.
 *
 * @param {String/Array} obj the string or array to truncate
 * @param {Number} maxLength the maximum length
 * @param {String/Array} tail the optional tail to use on truncation
 *
 * @return {String/Array} the truncated string or array
 *
 * @deprecated Use MochiKit.Text.truncate instead.
 */
RapidContext.Util.truncate = function (obj, maxLength, tail) {
    var msg = "RapidContext.Util.truncate is deprecated. Use " +
              "MochiKit.Text.truncate instead.";
    RapidContext.Util._logOnce('DEBUG', msg);
    return MochiKit.Text.truncate(obj, maxLength, tail);
};

/**
 * Formats a number using two digits, i.e. pads with a leading zero
 * character if the number is only one digit.
 *
 * @param {Number} value the number to format
 *
 * @return {String} the formatted number string
 *
 * @function
 */
RapidContext.Util.twoDigitNumber = MochiKit.Format.numberFormatter("00");

/**
 * Returns the margin sizes for an HTML DOM node. The margin sizes
 * for all four sides will be returned.
 *
 * @param {Object} node the HTML DOM node
 *
 * @return {Object} an object with "t", "b", "l" and "r" properties,
 *         each containing either an integer value or null
 */
RapidContext.Util.getMarginBox = function (node) {
    var getStyle = MochiKit.Style.getStyle;
    var px = RapidContext.Util.toPixels;
    return { t: px(getStyle(node, "margin-top")),
             b: px(getStyle(node, "margin-bottom")),
             l: px(getStyle(node, "margin-left")),
             r: px(getStyle(node, "margin-right")) };
};

/**
 * Returns the border widths for an HTML DOM node. The widths for
 * all four sides will be returned.
 *
 * @param {Object} node the HTML DOM node
 *
 * @return {Object} an object with "t", "b", "l" and "r" properties,
 *         each containing either an integer value or null
 */
RapidContext.Util.getBorderBox = function (node) {
    var getStyle = MochiKit.Style.getStyle;
    var px = RapidContext.Util.toPixels;
    return { t: px(getStyle(node, "border-width-top")),
             b: px(getStyle(node, "border-width-bottom")),
             l: px(getStyle(node, "border-width-left")),
             r: px(getStyle(node, "border-width-right")) };
};

/**
 * Returns the padding sizes for an HTML DOM node. The sizes for all
 * four sides will be returned.
 *
 * @param {Object} node the HTML DOM node
 *
 * @return {Object} an object with "t", "b", "l" and "r" properties,
 *         each containing either an integer value or null
 */
RapidContext.Util.getPaddingBox = function (node) {
    var getStyle = MochiKit.Style.getStyle;
    var px = RapidContext.Util.toPixels;
    return { t: px(getStyle(node, "padding-top")),
             b: px(getStyle(node, "padding-bottom")),
             l: px(getStyle(node, "padding-left")),
             r: px(getStyle(node, "padding-right")) };
};

/**
 * Converts a style pixel value to the corresponding integer. If the
 * string ends with "px", those characters will be silently removed.
 *
 * @param {String} value the style string value to convert
 *
 * @return {Number} the numeric value, or
 *         null if the conversion failed
 */
RapidContext.Util.toPixels = function (value) {
    value = parseInt(value);
    return isNaN(value) ? null : value;
};

/**
 * Returns the scroll offset for an HTML DOM node.
 *
 * @param {Object} node the HTML DOM node
 *
 * @return {Object} a MochiKit.Style.Coordinates object with "x" and
 *         "y" properties containing the element scroll offset
 */
RapidContext.Util.getScrollOffset = function (node) {
    node = MochiKit.DOM.getElement(node);
    var x = node.scrollLeft || 0;
    var y = node.scrollTop || 0;
    return new MochiKit.Style.Coordinates(x, y);
};

/**
 * Sets the scroll offset for an HTML DOM node.
 *
 * @param {Object} node the HTML DOM node
 * @param {Object} offset the MochiKit.Style.Coordinates containing
 *            the new scroll offset "x" and "y" values
 */
RapidContext.Util.setScrollOffset = function (node, offset) {
    node = MochiKit.DOM.getElement(node);
    node.scrollLeft = offset.x;
    node.scrollTop = offset.y;
};

