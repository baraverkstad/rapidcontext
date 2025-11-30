/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2025 Per Cederberg. All rights reserved.
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
 * Provides functions for encoding and decoding data.
 * @namespace RapidContext.Encode
 */
(function (window) {

    /**
     * Returns the unicode escape sequence for a character.
     *
     * @param {string} chr the single character string
     * @return {string} the unicode escape sequence
     * @private
     */
    function toCharEscape(chr) {
        return `\\u${chr.charCodeAt(0).toString(16).padStart(4, "0")}`;
    }

    /**
     * Serializes a value to JSON. The value is serialized by
     * `JSON.stringify`, but unicode escape sequences are inserted
     * for any non-printable ASCII characters.
     *
     * @param {Object} val the value to serialize
     * @return {string} the JSON string
     * @memberof RapidContext.Encode
     */
    function toJSON(val) {
        val = (val == null) ? null : val;
        return JSON.stringify(val).replaceAll(/[\u007F-\uFFFF]/g, toCharEscape);
    }

    /**
     * Serializes a value to a URL component. The value is serialized
     * by `encodeURIComponent`, but any non-String values are first
     * converted to strings.
     *
     * @param {Object} val the value to serialize
     * @param {Object} isForm the flag for using `+` instead of `%20`
     * @return {string} the URL-encoded string
     * @memberof RapidContext.Encode
     */
    function toUrlPart(val, isForm) {
        val = (val == null) ? "" : val;
        const isObject = typeof(val) === "object";
        const res = encodeURIComponent(isObject ? toJSON(val) : String(val));
        return isForm ? res.replaceAll(/%20/g, "+") : res;
    }

    /**
     * Serializes an object to a URL query string. If an object value
     * is an array, each entry will be added to the query separately.
     *
     * @param {Object} val the key-value pairs to serialize
     * @param {Object} isForm the flag for using `+` instead of `%20`
     * @return {string} the URL-encoded query string
     * @memberof RapidContext.Encode
     */
    function toUrlQuery(data, isForm) {
        data = data || {};
        const parts = [];
        for (const key in data) {
            const val = data[key];
            const arr = Array.isArray(val) ? val : [val];
            arr.length || arr.push("");
            arr.forEach(function (v) {
                parts.push(`${toUrlPart(key, isForm)}=${toUrlPart(v, isForm)}`);
            });
        }
        return parts.join("&");
    }

    // Create namespaces & export symbols
    const RapidContext = window.RapidContext || (window.RapidContext = {});
    const Encode = RapidContext.Encode || (RapidContext.Encode = {});
    Object.assign(Encode, { toJSON, toUrlPart, toUrlQuery });

})(globalThis);
