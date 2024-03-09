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

if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}
if (typeof(RapidContext.Util) == "undefined") {
    RapidContext.Util = {};
}

// Logs a deprecation warning on first call
RapidContext.deprecated = function (message) {
    var self = arguments.callee || {};
    var msgs = (self.msgs = self.msgs || {});
    msgs[message] || console.warn(message);
    msgs[message] = true;
};

// Creates a wrapped function that logs a deprecation warning
RapidContext.deprecatedFunction = function (func, message) {
    return Object.assign(
        function () {
            var self = arguments.callee || {};
            if (!self.caller || !self.caller.deprecated) {
                RapidContext.deprecated(message);
            }
            return func.apply(this, arguments);
        },
        { deprecated: true }
    );
}

// Changes all module functions to log deprecation warnings
RapidContext.deprecatedModule = function (module, message) {
    for (var k in module) {
        var v = module[k];
        if (typeof(v) === "function" && !v.deprecated) {
            module[k] = RapidContext.deprecatedFunction(v, message);
        }
    }
};

/**
 * Returns the name of a function. If the function is anonymous (i.e. the
 * `name` property is `undefined` or an empty string), the value of the
 * `displayName` property is returned instead.
 *
 * @param {Function} func the function to check
 *
 * @return {String} the function name, or `undefined` if not available
 *
 * @example
 * var o = { test: function () {} };
 * RapidContext.Util.registerFunctionNames(o, "o");
 * RapidContext.Util.functionName(o.test)
 * ==> "o.test"
 *
 * @see RapidContext.Util.registerFunctionNames
 */
RapidContext.Util.functionName = RapidContext.deprecatedFunction(
    function (func) {
        if (func == null) {
            return null;
        } else if (func.name != null && func.name != "") {
            return func.name;
        } else {
            return func.displayName;
        }
    },
    "RapidContext.Util.functionName() is deprecated"
);

/**
 * Registers function names for anonymous functions. This is useful when
 * debugging code in Firebug or similar tools, as readable stack traces can be
 * provided.
 *
 * This function will add a `displayName` property to all functions without a
 * `name` property. Both the `obj` properties and `obj.prototype` properties
 * are processed recursively, using the base name as a namespace (i.e.
 * `[name].[property]` or `[name].prototype.[property]`).
 *
 * @param {Object} obj the function or object to process
 * @param {String} [name] the function or object (class) name
 *
 * @example
 * var o = { name: "MyObject", test: function () {} };
 * RapidContext.Util.registerFunctionNames(o);
 * o.test.displayName
 * ==> "MyObject.test"
 *
 * @see RapidContext.Util.functionName
 */
RapidContext.Util.registerFunctionNames = RapidContext.deprecatedFunction(
    function (obj, name) {
        function worker(o, name, stack) {
            var isObj = (o != null && typeof(o) === "object");
            var isFunc = (typeof(o) === "function");
            var isAnon = isFunc && (o.name == null || o.name == "");
            var isProto = (o === Object.prototype || o === Function.prototype);
            var isNode = isObj && (typeof(o.nodeType) === "number");
            var isVisited = stack.includes(o);
            if (isFunc && isAnon && !o.displayName) {
                o.displayName = name;
            }
            if ((isObj || isFunc) && !isProto && !isNode && !isVisited) {
                stack.push(o);
                for (var prop in o) {
                    if (Object.prototype.hasOwnProperty.call(o, prop)) {
                        worker(o[prop], name + "." + prop, stack);
                    }
                }
                worker(o.prototype, name + ".prototype", stack);
                stack.pop();
            }
        }
        worker(obj, name || obj.name || obj.displayName || obj.NAME || "", []);
    },
    "RapidContext.Util.registerFunctionNames() is deprecated"
);

/**
 * Returns the current execution stack trace. The stack trace is an array of
 * function names with the innermost function at the lowest index (0). Due to
 * limitations in the JavaScript API:s, the stack trace will be cut if
 * recursion is detected. The stack trace will also be cut if the call depth
 * exceeds the maximum depth or if any function in the chain has an injected
 * stack trace.
 *
 * @param {Number} [maxDepth] the maximum call depth, defaults to 20
 *
 * @return {Array} the stack trace array of function names
 *
 * @see RapidContext.Util.functionName
 * @see RapidContext.Util.injectStackTrace
 */
RapidContext.Util.stackTrace = RapidContext.deprecatedFunction(
    function (maxDepth) {
        var func = arguments.callee.caller;
        var visited = [];
        var res = [];
        maxDepth = maxDepth || 20;
        while (func != null) {
            if (MochiKit.Base.findIdentical(visited, func) >= 0) {
                res.push("...recursion...");
                break;
            }
            if (func.$stackTrace != null) {
                res = res.concat(func.$stackTrace);
                break;
            }
            var name = RapidContext.Util.functionName(func);
            if (name === null) {
                // Skip stack trace when null (but not when undefined)
            } else {
                res.push(name || "<anonymous>");
            }
            visited.push(func);
            if (visited.length >= maxDepth) {
                res.push("...");
                break;
            }
            func = func.caller;
        }
        return res;
    },
    "RapidContext.Util.stackTrace() is deprecated"
);

/**
 * Injects a stack trace for a function. This method is useful for creating a
 * fake stack trace in anonymous or callback functions. A `null` value can be
 * used to clear any previously injected stack trace for the calling function.
 *
 * @param {Array} stackTrace the stack trace, or `null` to clear
 * @param {Function} [func] the function to modify, or `null` for the
 *            currently executing function (i.e. the caller)
 */
RapidContext.Util.injectStackTrace = RapidContext.deprecatedFunction(
    function (stackTrace, func) {
        func = func || arguments.callee.caller;
        if (func != null) {
            if (stackTrace) {
                func.$stackTrace = stackTrace;
            } else {
                delete func.$stackTrace;
            }
        }
    },
    "RapidContext.Util.injectStackTrace() is deprecated"
);

/**
 * Checks if the specified value corresponds to false. This function
 * will equate false, undefined, null, 0, "", [], "false" and "null"
 * with a boolean false value.
 *
 * @param {Object} value the value to check
 *
 * @return {Boolean} true if the value corresponds to false, or
 *         false otherwise
 */
RapidContext.Util.isFalse = RapidContext.deprecatedFunction(
    function (value) {
        return !MochiKit.Base.bool(value);
    },
    "RapidContext.Util.isFalse() is deprecated"
);

/**
 * Returns the first argument that is not undefined.
 *
 * @param {Object} [...] the values to check
 *
 * @return {Object} the first non-undefined argument, or
 *         undefined if all arguments were undefined
 *
 * @example
 * RapidContext.Util.defaultValue(undefined, window.noSuchProp, 1)
 *     --> 1
 *
 * @example
 * RapidContext.Util.defaultValue(0, 1)
 *     --> 0
 *
 * @example
 * RapidContext.Util.defaultValue(null, 1)
 *     --> null
 *
 * @example
 * RapidContext.Util.defaultValue()
 *     --> undefined
 */
RapidContext.Util.defaultValue = RapidContext.deprecatedFunction(
    function (/* ... */) {
        for (var i = 0; i < arguments.length; i++) {
            if (typeof(arguments[i]) != "undefined") {
                return arguments[i];
            }
        }
        return arguments[0];
    },
    "RapidContext.Util.defaultValue() is deprecated"
);

/**
 * Creates a new object by copying keys and values from another
 * object. A list of key names (or an object whose property names
 * will be used as keys) must be specified as an argument. The
 * returned object will only contain properties that were defined in
 * the source object, keeping the source object values. The source
 * object will be left unmodified.
 *
 * @param {Object} src the source object to select values from
 * @param {Array/Object} keys the list of keys to select, or an
 *            object with the keys to select
 *
 * @return {Object} a new object containing the matching keys and
 *             values found in the source object
 *
 * @example
 * RapidContext.Util.select({ a: 1, b: 2 }, ['a', 'c']);
 *     --> { a: 1 }
 *
 * @example
 * RapidContext.Util.select({ a: 1, b: 2 }, { a: true, c: true });
 *     --> { a: 1 }
 */
RapidContext.Util.select = RapidContext.deprecatedFunction(
    function (src, keys) {
        var res = {};
        if (!MochiKit.Base.isArrayLike(keys)) {
            keys = MochiKit.Base.keys(keys);
        }
        for (var i = 0; i < keys.length; i++) {
            var k = keys[i];
            if (k in src) {
                res[k] = src[k];
            }
        }
        return res;
    },
    "RapidContext.Util.select() is deprecated"
);

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
RapidContext.Util.findProperty = RapidContext.deprecatedFunction(
    function (lst, key, value, start, end) {
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
    },
    "RapidContext.Util.findProperty() is deprecated"
);

/**
 * Creates a dictionary object from a list of keys and values. Optionally a
 * list of key-value pairs can be provided instead. As a third option, a single
 * (non-array) value can be assigned to all the keys.
 *
 * If a key is specified twice, only the last value will be used. Note that
 * this function is the reverse of `MochiKit.Base.items()`,
 * `MochiKit.Base.keys()` and `MochiKit.Base.values()`.
 *
 * @param {Array} itemsOrKeys the list of keys or items
 * @param {Array} [values] the list of values (optional if key-value
 *            pairs are specified in first argument)
 *
 * @return {Object} an object with properties for each key-value pair
 *
 * @example
 * RapidContext.Util.dict(['a','b'], [1, 2])
 * ==> { a: 1, b: 2 }
 *
 * @example
 * RapidContext.Util.dict([['a', 1], ['b', 2]])
 * ==> { a: 1, b: 2 }
 *
 * @example
 * RapidContext.Util.dict(['a','b'], true)
 * ==> { a: true, b: true }
 */
RapidContext.Util.dict = RapidContext.deprecatedFunction(
    function (itemsOrKeys, values) {
        var o = {};
        if (!MochiKit.Base.isArrayLike(itemsOrKeys)) {
            throw new TypeError("First argument must be array-like");
        }
        if (MochiKit.Base.isArrayLike(values) && itemsOrKeys.length !== values.length) {
            throw new TypeError("Both arrays must be of same length");
        }
        for (var i = 0; i < itemsOrKeys.length; i++) {
            var k = itemsOrKeys[i];
            if (k === null || k === undefined) {
                throw new TypeError("Key at index " + i + " is null or undefined");
            } else if (MochiKit.Base.isArrayLike(k)) {
                o[k[0]] = k[1];
            } else if (MochiKit.Base.isArrayLike(values)) {
                o[k] = values[i];
            } else {
                o[k] = values;
            }
        }
        return o;
    },
    "RapidContext.Util.dict() is deprecated, use RapidContext.Data.object() instead"
);

/**
 * Filters an object by removing a list of keys. A list of key names (or an
 * object whose property names will be used as keys) must be specified as an
 * argument. A new object containing the source object values for the specified
 * keys will be returned. The source object will be modified by removing all
 * the specified keys.
 *
 * @param {Object} src the source object to select and modify
 * @param {Array|Object} keys the list of keys to remove, or an
 *            object with the keys to remove
 *
 * @return {Object} a new object containing the matching keys and
 *             values found in the source object
 *
 * @example
 * var o = { a: 1, b: 2 };
 * RapidContext.Util.mask(o, ['a', 'c']);
 * ==> { a: 1 } and modifies o to { b: 2 }
 *
 * @example
 * var o = { a: 1, b: 2 };
 * RapidContext.Util.mask(o, { a: null, c: null });
 * ==> { a: 1 } and modifies o to { b: 2 }
 */
RapidContext.Util.mask = RapidContext.deprecatedFunction(
    function (src, keys) {
        var res = {};
        if (!MochiKit.Base.isArrayLike(keys)) {
            keys = MochiKit.Base.keys(keys);
        }
        for (var i = 0; i < keys.length; i++) {
            var k = keys[i];
            if (k in src) {
                res[k] = src[k];
                delete src[k];
            }
        }
        return res;
    },
    "RapidContext.Util.mask() is deprecated, use object destructuring assignment instead"
);

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
 */
RapidContext.Util.truncate = RapidContext.deprecatedFunction(
    function (obj, maxLength, tail) {
        return MochiKit.Text.truncate(obj, maxLength, tail);
    },
    "RapidContext.Util.truncate() is deprecated"
);

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
RapidContext.Util.twoDigitNumber = RapidContext.deprecatedFunction(
    MochiKit.Format.numberFormatter("00"),
    "RapidContext.Util.twoDigitNumber() is deprecated"
);

RapidContext.Util._MILLIS_PER_SECOND = 1000;
RapidContext.Util._MILLIS_PER_MINUTE = 60 * 1000;
RapidContext.Util._MILLIS_PER_HOUR = 60 * 60 * 1000;
RapidContext.Util._MILLIS_PER_DAY = 24 * 60 * 60 * 1000;
RapidContext.Util._MILLIS_PER_WEEK = 7 * 24 * 60 * 60 * 1000;

/**
 * Creates a new time period object from a number of milliseconds.
 *
 * @param {Number} millis the number of milliseconds in the period
 *
 * @return {Object} new time duration object
 */
RapidContext.Util._toDuration = function (millis) {
    return {
        days: Math.floor(millis / RapidContext.Util._MILLIS_PER_DAY),
        hours: Math.floor(millis / RapidContext.Util._MILLIS_PER_HOUR) % 24,
        minutes: Math.floor(millis / RapidContext.Util._MILLIS_PER_MINUTE) % 60,
        seconds: Math.floor(millis / RapidContext.Util._MILLIS_PER_SECOND) % 60,
        millis: millis % 1000
    };
};

/**
 * Converts a number of milliseconds to an approximate time period.
 *
 * @param {Number} millis the number of milliseconds
 *
 * @return {String} the string representation of the period
 */
RapidContext.Util.toApproxPeriod = RapidContext.deprecatedFunction(
    function (millis) {
        var p = RapidContext.Util._toDuration(millis);
        if (p.days >= 10) {
            return p.days + " days";
        } else if (p.days >= 1) {
            return p.days + " days " + p.hours + " hours";
        } else if (p.hours >= 1) {
            return p.hours + ":" + MochiKit.Text.padLeft("" + p.minutes, 2, "0") + " hours";
        } else if (p.minutes >= 1) {
            return p.minutes + ":" + MochiKit.Text.padLeft("" + p.seconds, 2, "0") + " minutes";
        } else if (p.seconds >= 1) {
            return p.seconds + " seconds";
        } else {
            return p.millis + " milliseconds";
        }
    },
    "RapidContext.Util.toApproxPeriod() is deprecated"
);

/**
 * Resolves a relative URI to an absolute URI. This function will return
 * absolute URI:s directly and traverse any "../" directory paths in the
 * specified URI. The base URI provided must be absolute.
 *
 * @param {string} uri the relative URI to resolve
 * @param {string} [base] the absolute base URI, defaults to the
 *            the current document base URI
 *
 * @return {string} the resolved absolute URI
 */
RapidContext.Util.resolveURI = RapidContext.deprecatedFunction(
    function (uri, base) {
        var pos;
        base = base || document.baseURI || document.getElementsByTagName("base")[0].href;
        if (uri.includes(":")) {
            return uri;
        } else if (uri.startsWith("#")) {
            pos = base.lastIndexOf("#");
            if (pos >= 0) {
                base = base.substring(0, pos);
            }
            return base + uri;
        } else if (uri.startsWith("/")) {
            pos = base.indexOf("/", base.indexOf("://") + 3);
            base = base.substring(0, pos);
            return base + uri;
        } else if (uri.startsWith("../")) {
            pos = base.lastIndexOf("/");
            base = base.substring(0, pos);
            uri = uri.substring(3);
            return RapidContext.Util.resolveURI(uri, base);
        } else {
            pos = base.lastIndexOf("/");
            base = base.substring(0, pos + 1);
            return base + uri;
        }
    },
    "RapidContext.Util.resolveURI() is deprecated, use 'new URL(...)' instead"
);

/**
 * Creates a programmers debug representation of a DOM node. This method is
 * similar to `MochiKit.DOM.emitHtml`, except for that it does not recurse into
 * child nodes.
 *
 * @param {Object} node the HTML DOM node
 *
 * @return {String} a debug representation of the DOM node
 */
RapidContext.Util.reprDOM = RapidContext.deprecatedFunction(
    function (node) {
        if (node == null) {
            return "null";
        } else if (typeof(node) === 'string') {
            return node;
        } else if (node.nodeType === 1) { // Node.ELEMENT_NODE
            var res = "<" + node.tagName.toLowerCase();
            var attrs = MochiKit.Base.map(RapidContext.Util.reprDOM, node.attributes);
            res += attrs.join("");
            if (node.hasChildNodes()) {
                res += " ["  + node.childNodes.length + " child nodes]";
            }
            res += "/>";
            return res;
        } else if (node.nodeType === 2) { // Node.ATTRIBUTE_NODE
            return " " + node.name + '="' +
                   MochiKit.DOM.escapeHTML(node.value) + '"';
        } else if (node.nodeType === 3) { // Node.TEXT_NODE
            return MochiKit.DOM.escapeHTML(node.nodeValue);
        } else {
            return node.toString();
        }
    },
    "RapidContext.Util.reprDOM() is deprecated"
);

/**
 * Returns the margin sizes for an HTML DOM node. The margin sizes
 * for all four sides will be returned.
 *
 * @param {Object} node the HTML DOM node
 *
 * @return {Object} an object with "t", "b", "l" and "r" properties,
 *         each containing either an integer value or null
 */
RapidContext.Util.getMarginBox = RapidContext.deprecatedFunction(
    function (node) {
        var getStyle = MochiKit.Style.getStyle;
        var px = RapidContext.Util.toPixels;
        return { t: px(getStyle(node, "margin-top")),
                 b: px(getStyle(node, "margin-bottom")),
                 l: px(getStyle(node, "margin-left")),
                 r: px(getStyle(node, "margin-right")) };
    },
    "RapidContext.Util.getMarginBox() is deprecated"
);

/**
 * Returns the border widths for an HTML DOM node. The widths for
 * all four sides will be returned.
 *
 * @param {Object} node the HTML DOM node
 *
 * @return {Object} an object with "t", "b", "l" and "r" properties,
 *         each containing either an integer value or null
 */
RapidContext.Util.getBorderBox = RapidContext.deprecatedFunction(
    function (node) {
        var getStyle = MochiKit.Style.getStyle;
        var px = RapidContext.Util.toPixels;
        return { t: px(getStyle(node, "border-width-top")),
                 b: px(getStyle(node, "border-width-bottom")),
                 l: px(getStyle(node, "border-width-left")),
                 r: px(getStyle(node, "border-width-right")) };
    },
    "RapidContext.Util.getBorderBox() is deprecated"
);

/**
 * Returns the padding sizes for an HTML DOM node. The sizes for all
 * four sides will be returned.
 *
 * @param {Object} node the HTML DOM node
 *
 * @return {Object} an object with "t", "b", "l" and "r" properties,
 *         each containing either an integer value or null
 */
RapidContext.Util.getPaddingBox = RapidContext.deprecatedFunction(
    function (node) {
        var getStyle = MochiKit.Style.getStyle;
        var px = RapidContext.Util.toPixels;
        return { t: px(getStyle(node, "padding-top")),
                 b: px(getStyle(node, "padding-bottom")),
                 l: px(getStyle(node, "padding-left")),
                 r: px(getStyle(node, "padding-right")) };
    },
    "RapidContext.Util.getPaddingBox() is deprecated"
);

/**
 * Converts a style pixel value to the corresponding integer. If the
 * string ends with "px", those characters will be silently removed.
 *
 * @param {String} value the style string value to convert
 *
 * @return {Number} the numeric value, or
 *         null if the conversion failed
 */
RapidContext.Util.toPixels = RapidContext.deprecatedFunction(
    function (value) {
        value = parseInt(value);
        return isNaN(value) ? null : value;
    },
    "RapidContext.Util.toPixels() is deprecated"
);

/**
 * Returns the scroll offset for an HTML DOM node.
 *
 * @param {Object} node the HTML DOM node
 *
 * @return {Object} a MochiKit.Style.Coordinates object with "x" and
 *         "y" properties containing the element scroll offset
 */
RapidContext.Util.getScrollOffset = RapidContext.deprecatedFunction(
    function (node) {
        node = MochiKit.DOM.getElement(node);
        var x = node.scrollLeft || 0;
        var y = node.scrollTop || 0;
        return new MochiKit.Style.Coordinates(x, y);
    },
    "RapidContext.Util.getScrollOffset() is deprecated"
);

/**
 * Sets the scroll offset for an HTML DOM node.
 *
 * @param {Object} node the HTML DOM node
 * @param {Object} offset the MochiKit.Style.Coordinates containing
 *            the new scroll offset "x" and "y" values
 */
RapidContext.Util.setScrollOffset = RapidContext.deprecatedFunction(
    function (node, offset) {
        node = MochiKit.DOM.getElement(node);
        node.scrollLeft = offset.x;
        node.scrollTop = offset.y;
    },
    "RapidContext.Util.setScrollOffset() is deprecated"
);

/**
 * Resets the scroll offsets to zero for for an HTML DOM node.
 * Optionally all child node offsets can also be reset.
 *
 * @param {Object} node the HTML DOM node
 * @param {Boolean} [recursive] the recursive flag, defaults to
 *            false
 */
RapidContext.Util.resetScrollOffset = RapidContext.deprecatedFunction(
    function (node, recursive) {
        node = MochiKit.DOM.getElement(node);
        node.scrollLeft = 0;
        node.scrollTop = 0;
        if (recursive) {
            node = node.firstChild;
            while (node != null) {
                if (node.nodeType === 1) { // Node.ELEMENT_NODE
                    RapidContext.Util.resetScrollOffset(node, true);
                }
                node = node.nextSibling;
            }
        }
    },
    "RapidContext.Util.resetScrollOffset() is deprecated"
);

/**
 * Adjusts the scroll offsets for an HTML DOM node to ensure optimal
 * visibility for the specified coordinates box. This function will
 * scroll the node both vertially and horizontally to ensure that
 * the top left corner of the box is always visible and that as much
 * of the box extent as possible is visible.
 *
 * @param {Object} node the HTML DOM node
 * @param {Object} box the coordinates box with optional properties
 *            {l, t, r, b} or {x, y, w, h}
 */
RapidContext.Util.adjustScrollOffset = RapidContext.deprecatedFunction(
    function (node, box) {
        node = MochiKit.DOM.getElement(node);
        var dim = MochiKit.Style.getElementDimensions(node);
        var xMin = RapidContext.Util.defaultValue(box.l, box.x, NaN);
        var xMax = RapidContext.Util.defaultValue(box.r, xMin + box.w, NaN);
        var yMin = RapidContext.Util.defaultValue(box.t, box.y, NaN);
        var yMax = RapidContext.Util.defaultValue(box.b, yMin + box.h, NaN);
        if (!isNaN(xMax) && node.scrollLeft + dim.w < xMax) {
            node.scrollLeft = xMax - dim.h;
        }
        if (!isNaN(xMin) && node.scrollLeft > xMin) {
            node.scrollLeft = xMin;
        }
        if (!isNaN(yMax) && node.scrollTop + dim.h < yMax) {
            node.scrollTop = yMax - dim.h;
        }
        if (!isNaN(yMin) && node.scrollTop > yMin) {
            node.scrollTop = yMin;
        }
    },
    "RapidContext.Util.adjustScrollOffset() is deprecated"
);

RapidContext.Util.NS = {
    XHTML: "http://www.w3.org/1999/xhtml",
    XLINK: "http://www.w3.org/1999/xlink",
    SVG: "http://www.w3.org/2000/svg",
    XUL: "http://www.mozilla.org/keymaster/gatekeeper/there.is.only.xul"
};
RapidContext.Util.NS.HTML = [undefined, null, "", RapidContext.Util.NS.XHTML];

/**
 * Returns `true` if the specified object looks like a DOM node. Otherwise,
 * `false` will be returned. Any non-null object with a `nodeType` > 0 will be
 * considered a DOM node by this function.
 *
 * @param {Object} obj the object to check
 *
 * @return {Boolean} `true` if the object looks like a DOM node, or
 *         `false` otherwise
 */
RapidContext.Util.isDOM = RapidContext.deprecatedFunction(
    function (obj) {
        return obj != null &&
               typeof(obj.nodeType) === "number" &&
               obj.nodeType > 0;
    },
    "RapidContext.Util.isDOM() is deprecated"
);

/**
 * Returns `true` if the specified object looks like an HTML or XHTML DOM node.
 * Otherwise, `false` will be returned. Any non-null object with a
 * `nodeType` > 0 will be considered a DOM node, but only those with a matching
 * `namespaceURI` will be considered HTML DOM nodes.
 *
 * @param {Object} obj the object to check
 *
 * @return {Boolean} `true` if the object looks like an HTML DOM node,
 *         or `false` otherwise
 */
RapidContext.Util.isHTML = RapidContext.deprecatedFunction(
    function (obj) {
        return RapidContext.Util.isDOM(obj) &&
               RapidContext.Util.NS.HTML.includes(obj.namespaceURI);
    },
    "RapidContext.Util.isHTML() is deprecated"
);

/**
 * Creates a DOM node with a namespace.
 *
 * @param {String} ns the DOM namespace
 * @param {String} tag the DOM tag name
 * @param {Object} [attrs] the node attributes, or null for none
 * @param {Object} [...] the nodes or text to add as children
 *
 * @return {Object} the DOM node created
 *
 * @example
 * RapidContext.Util.createDOMExt("http://www.w3.org/2000/svg", "g");
 * ==> an SVG <g> element
 */
RapidContext.Util.createDOMExt = RapidContext.deprecatedFunction(
    function (ns, tag, attrs/*, ...*/) {
        var doc = MochiKit.DOM.currentDocument();
        var node = (ns) ? doc.createElementNS(ns, tag) : doc.createElement(tag);
        MochiKit.DOM.updateNodeAttributes(node, attrs);
        var children = Array.from(arguments).slice(3);
        node.append(...children);
        return node;
    },
    "RapidContext.Util.createDOMExt() is deprecated"
);

/**
 * Returns a function for creating a specific kind of DOM nodes. The returned
 * function will optionally require a sequence of non-null arguments that will
 * be added as attributes to the node creation. The returned function will
 * otherwise work similar to the `createDOMExt()` function, taking attributes
 * and child nodes.
 *
 * @param {String} ns the DOM namespace, or `null` for HTML
 * @param {String} tag the DOM tag name
 * @param {Array} [args] the array with required arguments, or `null` for no
 *            required arguments
 * @param {Object} [attrs] the default node attributes, or `null` for none
 * @param {Object} [...] the default nodes or text to add as children
 *
 * @return {Function} the function that creates the DOM nodes
 */
RapidContext.Util.createDOMFuncExt = RapidContext.deprecatedFunction(
    function (ns, tag, args, attrs/*, ...*/) {
        args = args || [];
        attrs = attrs || {};
        var children = Array.from(arguments).slice(4);
        return function (/*arg1, ..., argN, attrs, ...*/) {
            var myArgs = Array.from(arguments);
            var myAttrs = Object.assign({}, attrs);
            args.forEach(function (key, idx) {
                if (myArgs[idx] == null) {
                    throw new Error("Argument '" + key + "' cannot be null");
                }
                myAttrs[key] = myArgs[idx];
            });
            Object.assign(myAttrs, myArgs[args.length]);
            var myChildren = [].concat(children, myArgs.slice(args.length + 1));
            return RapidContext.Util.createDOMExt(ns, tag, myAttrs, myChildren);
        };
    },
    "RapidContext.Util.createDOMFuncExt() is deprecated"
);

/**
 * Creates a DOM text node from the specified text. This is a convenience
 * function for `currentDocument().createTextNode`, in order to be compatible
 * with the `withDocument()` function.
 *
 * @param {String} text the text content
 *
 * @return {Object} the DOM text node created
 */
RapidContext.Util.createTextNode = RapidContext.deprecatedFunction(
    function (text) {
        return MochiKit.DOM.currentDocument().createTextNode(text);
    },
    "RapidContext.Util.createTextNode() is deprecated"
);

/**
 * Returns an array with DOM node attribute name and value pairs. The name and
 * value pairs are also stored in arrays with two elements.
 *
 * @param {Object} node the HTML DOM node
 *
 * @return {Array} an array containing attribute name and value
 *             pairs (as arrays)
 */
RapidContext.Util.attributeArray = RapidContext.deprecatedFunction(
    function (node) {
        var res = [];
        node = MochiKit.DOM.getElement(node);
        for (var i = 0; node != null && i < node.attributes.length; i++) {
            var a = node.attributes[i];
            res.push([a.name, a.value]);
        }
        return res;
    },
    "RapidContext.Util.attributeArray() is deprecated"
);

/**
 * Returns an immediate child node from a parent DOM node. If a numeric index
 * is provided, the index will be range checked and any matching child DOM
 * node will be returned. Otherwise, the DOM tree is traversed to find the
 * immediate child that corresponds to the specified node.
 *
 * @param {Node} parent the parent HTML DOM node
 * @param {Number/Node} indexOrNode the child index or a descendant node
 *
 * @return {Node} the child HTML DOM node, or
 *         `null` if no matching node was found
 *
 * @example
 * var child = RapidContext.Util.childNode(parent, 2);
 * ==> parent.childNodes[2] or null
 *
 * @example
 * var child = RapidContext.Util.childNode(node, evt.target());
 * ==> child DOM node if descendant or null otherwise
 */
RapidContext.Util.childNode = RapidContext.deprecatedFunction(
    function (parent, indexOrNode) {
        parent = MochiKit.DOM.getElement(parent);
        if (typeof(indexOrNode) == "number") {
            if (indexOrNode < 0 || indexOrNode >= parent.childNodes.length) {
                return null;
            } else {
                return parent.childNodes[indexOrNode];
            }
        } else {
            var node = MochiKit.DOM.getElement(indexOrNode);
            while (node != null && node !== parent && node.parentNode !== parent) {
                node = node.parentNode;
            }
            return (node == null || node === parent) ? null : node;
        }
    },
    "RapidContext.Util.childNode() is deprecated"
);
