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

// Create default RapidContext object
if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}

/**
 * @name RapidContext.Util
 * @namespace
 * Provides utility functions for basic objects, arrays, DOM nodes and CSS.
 * These functions are complementary to what is available in MochiKit and/or
 * jQuery.
 */
if (typeof(RapidContext.Util) == "undefined") {
    RapidContext.Util = {};
}


// General utility functions

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
RapidContext.Util.dict = function (itemsOrKeys, values) {
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
};

/**
 * Filters an object by removing a list of keys. A list of key names (or an
 * object whose property names will be used as keys) must be specified as an
 * argument. A new object containing the source object values for the specified
 * keys will be returned. The source object will be modified by removing all
 * the specified keys.
 *
 * @param {Object} src the source object to select and modify
 * @param {Array/Object} keys the list of keys to remove, or an
 *            object with the keys to remove
 *
 * @return {Object} a new object containing the matching keys and
 *             values found in the source object
 *
 * @deprecated This function will be removed in the future.
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
RapidContext.Util.mask = function (src, keys) {
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
};

/**
 * Converts a string to a title-cased string. All word boundaries are replaced
 * with a single space and the subsequent character is capitalized.
 *
 * All underscore ("_"), hyphen ("-") and lower-upper character pairs are
 * recognized as word boundaries. Note that this function does not change the
 * capitalization of other characters in the string.
 *
 * @param {String} str the string to convert
 *
 * @return {String} the converted string
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
    str = str.replace(/[_-]+/g, " ").trim();
    str = str.replace(/[a-z][A-Z]/g, function (match) {
        return match.charAt(0) + " " + match.charAt(1);
    });
    str = str.replace(/(^|\s)[a-z]/g, function (match) {
        return match.toUpperCase();
    });
    return str;
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
RapidContext.Util.functionName = function (func) {
    if (func == null) {
        return null;
    } else if (func.name != null && func.name != "") {
        return func.name;
    } else {
        return func.displayName;
    }
};

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
RapidContext.Util.registerFunctionNames = function (obj, name) {
    function worker(o, name, stack) {
        var isObj = (o != null && typeof(o) === "object");
        var isFunc = (typeof(o) === "function");
        var isAnon = isFunc && (o.name == null || o.name == "");
        var isProto = (o === Object.prototype || o === Function.prototype);
        var isNode = isObj && (typeof(o.nodeType) === "number");
        var isVisited = (MochiKit.Base.findIdentical(stack, o) >= 0);
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
};

/**
 * Resolves a relative URI to an absolute URI. This function will return
 * absolute URI:s directly and traverse any "../" directory paths in the
 * specified URI. The base URI provided must be absolute.
 *
 * @param {String} uri the relative URI to resolve
 * @param {String} [base] the absolute base URI, defaults to the
 *            the current document base URI
 *
 * @return {String} the resolved absolute URI
 *
 * @deprecated This function will be removed and/or renamed in the future.
 *     Better solutions for handling URL:s is to use a URL-parsing library such
 *     as URL.js.
 */
RapidContext.Util.resolveURI = function (uri, base) {
    var pos;
    base = base || document.baseURI || document.getElementsByTagName("base")[0].href;
    if (uri.indexOf(":") > 0) {
        return uri;
    } else if (uri.indexOf("#") == 0) {
        pos = base.lastIndexOf("#");
        if (pos >= 0) {
            base = base.substring(0, pos);
        }
        return base + uri;
    } else if (uri.indexOf("/") == 0) {
        pos = base.indexOf("/", base.indexOf("://") + 3);
        base = base.substring(0, pos);
        return base + uri;
    } else if (uri.indexOf("../") == 0) {
        pos = base.lastIndexOf("/");
        base = base.substring(0, pos);
        uri = uri.substring(3);
        return RapidContext.Util.resolveURI(uri, base);
    } else {
        pos = base.lastIndexOf("/");
        base = base.substring(0, pos + 1);
        return base + uri;
    }
};


// DOM utility functions

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
RapidContext.Util.isDOM = function (obj) {
    return obj != null &&
           typeof(obj.nodeType) === "number" &&
           obj.nodeType > 0;
};

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
RapidContext.Util.isHTML = function (obj) {
    var ns = RapidContext.Util.NS.HTML;
    return RapidContext.Util.isDOM(obj) &&
           MochiKit.Base.findIdentical(ns, obj.namespaceURI) >= 0;
};

/**
 * Returns an array with DOM node attribute name and value pairs. The name and
 * value pairs are also stored in arrays with two elements.
 *
 * @param {Object} node the HTML DOM node
 *
 * @return {Array} an array containing attribute name and value
 *             pairs (as arrays)
 */
RapidContext.Util.attributeArray = function (node) {
    var res = [];
    node = MochiKit.DOM.getElement(node);
    for (var i = 0; node != null && i < node.attributes.length; i++) {
        var a = node.attributes[i];
        res.push([a.name, a.value]);
    }
    return res;
};

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
RapidContext.Util.childNode = function (parent, indexOrNode) {
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
};

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
RapidContext.Util.createDOMExt = function (ns, tag, attrs/*, ...*/) {
    var doc = MochiKit.DOM.currentDocument();
    var node = (ns) ? doc.createElementNS(ns, tag) : doc.createElement(tag);
    MochiKit.DOM.updateNodeAttributes(node, attrs);
    var children = Array.prototype.slice.call(arguments, 3);
    MochiKit.DOM.appendChildNodes(node, children);
    return node;
};

/**
 * Creates a DOM text node from the specified text. This is a convenience
 * function for `currentDocument().createTextNode`, in order to be compatible
 * with the `withDocument()` function.
 *
 * @param {String} text the text content
 *
 * @return {Object} the DOM text node created
 */
RapidContext.Util.createTextNode = function (text) {
    return MochiKit.DOM.currentDocument().createTextNode(text);
};

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
RapidContext.Util.createDOMFuncExt = function (ns, tag, args, attrs/*, ...*/) {
    args = args || [];
    attrs = attrs || {};
    var children = Array.prototype.slice.call(arguments, 4);
    return function (/*arg1, ..., argN, attrs, ...*/) {
        var myArgs = Array.prototype.slice.call(arguments);
        var myAttrs = MochiKit.Base.update({}, attrs);
        args.forEach(function (key, idx) {
            if (myArgs[idx] == null) {
                throw new Error("Argument '" + key + "' cannot be null");
            }
            myAttrs[key] = myArgs[idx];
        });
        MochiKit.Base.update(myAttrs, myArgs[args.length]);
        var myChildren = [].concat(children, myArgs.slice(args.length + 1));
        return RapidContext.Util.createDOMExt(ns, tag, myAttrs, myChildren);
    };
};

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
 * @param {Number/String/Function} [width] the width constraint
 * @param {Number/String/Function} [height] the height constraint
 *
 * @see RapidContext.Util.resizeElements
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
 * @param {Object} [...] the HTML DOM nodes to resize
 *
 * @see RapidContext.Util.registerSizeConstraints
 *
 * @example
 * RapidContext.Util.resizeElements(node);
 * ==> Evaluates the size constraints for a node and all child nodes
 *
 * @example
 * elem.resizeContent = MochiKit.Base.noop;
 * ==> Assigns a no-op child resize handler to elem
 */
RapidContext.Util.resizeElements = function (/* ... */) {
    Array.prototype.slice.call(arguments).forEach(function (arg) {
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
            Array.prototype.slice.call(node.childNodes).forEach(function (child) {
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
