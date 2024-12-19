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
