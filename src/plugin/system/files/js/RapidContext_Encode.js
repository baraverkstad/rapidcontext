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

/**
 * @name RapidContext.Encode
 * @namespace Provides functions for encoding and decoding data.
 */
(function (window) {

    /**
     * Returns the unicode escape sequence for a character.
     *
     * @param {String} chr the single character string
     * @return {String} the unicode escape sequence
     * @private
     */
    function toCharEscape(chr) {
        return "\\u" + chr.charCodeAt(0).toString(16).padStart(4, "0");
    }

    /**
     * Serializes a value to JSON. The value is serialized by
     * `JSON.stringify`, but unicode escape sequences are inserted
     * for any non-printable ASCII characters.
     *
     * @param {Object} val the value to serialize
     * @return {String} the serialized JSON string
     * @memberof RapidContext.Encode
     */
    function toJSON(val) {
        return JSON.stringify(val).replace(/[\u007F-\uFFFF]/g, toCharEscape);
    }

    // Create namespaces
    var RapidContext = window.RapidContext || (window.RapidContext = {});
    var Encode = RapidContext.Encode || (RapidContext.Encode = {});

    // Export symbols
    Encode.toJSON = toJSON;

})(this);
