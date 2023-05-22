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

/**
 * @name RapidContext.Browser
 * @namespace Provides a browser compatibility and diagnostics information.
 */
(function (window) {

    /**
     * List of all required browser features.
     *
     * @name REQUIRED
     * @memberof RapidContext.Browser
     * @constant
     */
    var REQUIRED = [
        "Array.isArray",
        "Array.from",
        "Array.prototype.every",
        "Array.prototype.filter",
        "Array.prototype.find",
        "Array.prototype.findIndex",
        "Array.prototype.forEach",
        "Array.prototype.includes",
        "Array.prototype.map",
        "Array.prototype.reduce",
        "Array.prototype.some",
        "Blob",
        "CSS.supports",
        "CustomEvent",
        "Date.now",
        "Date.prototype.toISOString",
        "Date.prototype.toLocaleString",
        "DOMTokenList.prototype.add",
        "DOMTokenList.prototype.remove",
        "DOMTokenList.prototype.toggle",
        "FileReader",
        "FormData",
        "Function.prototype.bind",
        "JSON.parse",
        "JSON.stringify",
        "Math.imul",
        "Number.prototype.toLocaleString",
        "Object.assign",
        "Object.create",
        "Object.defineProperty",
        "Object.getPrototypeOf",
        "Object.is",
        "Object.keys",
        "Object.setPrototypeOf",
        "Promise",
        "Promise.all",
        "Promise.race",
        "Promise.reject",
        "Promise.resolve",
        "Promise.prototype.finally",
        "String.prototype.endsWith",
        "String.prototype.includes",
        "String.prototype.startsWith",
        "String.prototype.trim",
        "String.raw", // proxy for template literals
        "URL",
        "URL.createObjectURL",
        "URL.revokeObjectURL",
        "console.assert",
        "console.debug",
        "console.error",
        "console.info",
        "console.log",
        "console.warn",
        "document.querySelectorAll",
        "localStorage.getItem",
        "sessionStorage.setItem",
        { test: "'head' in document", name: "document.head shortcut" },
        { test: "'classList' in Element.prototype", name: "Element.prototype.classList" },
        { test: "'dataset' in HTMLElement.prototype", name: "HTMLElement.prototype.dataset" },
        { test: "'onload' in HTMLLinkElement.prototype", name: "<link> element onload" },
        {
            test: "var el = document.createElement('div');el.style.cssText='width:calc(10px);';!!el.style.length",
            name: "CSS calc()"
        },
        { test: "for (var el of []) el; true", name: "loops with for...of" },
        { test: "var a = 42; ({ a }).a === 42", name: "shorthand property names" },
        { test: "({ f(arg) { return arg; } }).f(true)", name: "shorthand method names" },
        { test: "var k = 'a'; ({ [k]: 42 })['a'] === 42", name: "computed property names" },
        { test: "class Test {}; true", name: "Class declarations" },
        "display: flex"
    ];

    /**
     * List of all optional (or recommended) browser features. These are
     * not used in built-in libraries and apps, but will be in the future.
     *
     * @name OPTIONAL
     * @memberof RapidContext.Browser
     * @constant
     */
    var OPTIONAL = [
        "AbortController",
        "AbortSignal",
        // "Array.prototype.at",
        "Array.prototype.flat",
        "Array.prototype.flatMap",
        "Element.prototype.after",
        "Element.prototype.append",
        "Element.prototype.before",
        "Element.prototype.closest",
        "Element.prototype.matches",
        "Element.prototype.prepend",
        "Element.prototype.replaceWith",
        "fetch",
        // "navigator.credentials.create",
        // "navigator.credentials.get",
        "NodeList.prototype.forEach",
        "Object.entries",
        // "Object.fromEntries",
        "Object.values",
        // "Promise.allSettled",
        // "Promise.any",
        "Set",
        "String.prototype.padEnd",
        "String.prototype.padStart",
        // "SubtleCrypto.prototype.digest",
        // "Temporal",
        // "ResizeObserver",
        "URLSearchParams",
        // "globalThis",
        { test: "let a = 2; a === 2", name: "Let statements" },
        { test: "const a = 3; a === 3", name: "Const statements" },
        { test: "(() => true)()", name: "Arrow functions" },
        { test: "/^\\p{L}+$/u.test('a\u00E5\u00C4')", name: "regexp Unicode property escapes" },
        { test: "((a,  b, ...args) => true)()", name: "rest parameters" },
        { test: "Math.max(...[1, 2, 3]) === 3", name: "spread parameters" },
        { test: "[...[1, 2, 3]].length === 3", name: "spread array literals" },
        { test: "var [a, ...b] = [1, 2, 3]; a == 1 && b.length == 2", name: "array destructuring assignment" },
        { test: "var {a, b} = { a: 1, b: 2, c: 3 }; a == 1 && b == 2", name: "object destructuring assignment" },
        // { test: "var a = null; a?.dummy; true", name: "optional chaining operator (?.)" },
        // { test: "null ?? true", name: "nullish coalescing operator (??)" },
        { test: "'noModule' in HTMLScriptElement.prototype", name: "ES6 modules (import/export)" },
        { test: "async () => await Promise.resolve(true)", name: "async/await functions" }
    ];

    /**
     * Checks if the browser supports all required APIs.
     *
     * @return {Boolean} true if the browser is supported, or false otherwise
     *
     * @memberof RapidContext.Browser
     */
    function isSupported() {
        var hasRequired = supports(REQUIRED);
        var hasOptional = supports(OPTIONAL);
        if (!hasRequired) {
            console.error("browser: required features are missing", info());
        } else if (!hasOptional) {
            console.warn("browser: some recommended features are missing", info());
        }
        return hasRequired;
    }

    /**
     * Checks for browser support for one or more specified APIs. Supports
     * checking JavaScript APIs, JavaScript syntax and CSS support.
     *
     * @param {String/Object/Array} feature one or more features to check
     * @param {String/Object} [...] the additional features to check
     * @return {Boolean} true if supported, or false otherwise
     *
     * @example
     * RapidContext.Browser.supports("Array.isArray") ==> true;
     * RapidContext.Browser.supports({ test: "let a = 2; a === 2", name: "Let statements" }) ==> true;
     * RapidContext.Browser.supports("display: flex") ==> false;
     *
     * @memberof RapidContext.Browser
     */
    function supports(feature/*, ...*/) {
        function checkCSS(code) {
            try {
                return CSS.supports("(" + code + ")");
            } catch (ignore) {
                return false;
            }
        }
        function checkPath(base, path) {
            while (base && path.length > 0) {
                base = base[path.shift()];
            }
            return typeof(base) === "function";
        }
        function checkEval(code) {
            try {
                var val = eval(code);
                return val !== undefined && val !== null;
            } catch (ignore) {
                return false;
            }
        }
        function check(test) {
            var isCSS = /^[a-z-]+:/i.test(test);
            var isPath = /^[a-z]+(\.[a-z]+)*$/i.test(test);
            var isValid = (
                (isCSS && checkCSS(test)) ||
                (isPath && checkPath(window, test.split("."))) ||
                (!isCSS && !isPath && checkEval(test))
            );
            return isValid;
        }
        // NOTE: Code below uses old school JavaScript for compatibility
        var res = true;
        var features = (feature instanceof Array) ? feature : Array.prototype.slice.call(arguments);
        for (var i = 0; i < features.length; i++) {
            var def = features[i].test ? features[i] : { test: features[i] };
            if (!check(def.test)) {
                var explain = [def.name, def.test].filter(Boolean).join(": ");
                console.warn("browser: missing support for " + explain);
                res = false;
            }
        }
        return res;
    }

    /**
     * Returns browser information version and platform information.
     *
     * @param {String} [userAgent] the agent string, or undefined for this browser
     * @return {Object} a browser meta-data object
     *
     * @memberof RapidContext.Browser
     */
    function info(userAgent) {
        function firstMatch(patterns, text) {
            for (var k in patterns) {
                var m = patterns[k].exec(text);
                if (m) {
                    var extra = m[1] ? " " + m[1] : "";
                    if (/^[ \d_.]+$/.test(extra)) {
                        extra = extra.replace(/_/g, ".");
                    }
                    return k + extra;
                }
            }
            return null;
        }
        function clone(obj, keys) {
            var res = {};
            for (var i = 0; i < keys.length; i++) {
                var k = keys[i];
                res[k] = obj[k];
            }
            return res;
        }
        var BROWSERS = {
            "Edge": /Edg(?:e|A|iOS|)\/([^\s;]+)/,
            "Chrome": /Chrome\/([^\s;]+)/,
            "Chrome iOS": /CriOS\/([^\s;]+)/,
            "Firefox": /Firefox\/([^\s;]+)/,
            "Firefox iOS": /FxiOS\/([^\s;]+)/,
            "Safari": /Version\/(\S+).* Safari\/[^\s;]+/,
            "MSIE": /(?:MSIE |Trident\/.*rv:)([^\s;)]+)/
        };
        var PLATFORMS = {
            "Android": /Android ([^;)]+)/,
            "Chrome OS": /CrOS [^\s]+ ([^;)]+)/,
            "iOS": /(?:iPhone|CPU) OS ([\d_]+)/,
            "macOS": /Mac OS X ([^;)]+)/,
            "Linux": /Linux ([^;)]+)/,
            "Windows": /Windows ([^;)]+)/
        };
        var DEVICES = {
            "iPad": /iPad/,
            "iPhone": /iPhone/,
            "Tablet": /Tablet/,
            "Mobile": /Mobile|Android/
        };
        var ua = userAgent || window.navigator.userAgent;
        var browser = firstMatch(BROWSERS, ua);
        var platform = firstMatch(PLATFORMS, ua);
        var device = firstMatch(DEVICES, ua);
        var res = {};
        if (browser && platform) {
            res["browser"] = browser;
            res["platform"] = platform;
            res["device"] = device || "Desktop/Other";
        } else {
            res["userAgent"] = String(ua);
        }
        if (!userAgent) {
            res["language"] = window.navigator.language;
            res["screen"] = clone(window.screen, ["width", "height", "colorDepth"]);
            res["window"] = clone(window, ["innerWidth", "innerHeight", "devicePixelRatio"]);
            res["cookies"] = _cookies();
        }
        return res;
    }

    function _cookies() {
        var cookies = {};
        var pairs = window.document.cookie.split(/\s*;\s*/g);
        pairs.forEach(function (pair) {
            var name = pair.split("=")[0];
            var value = pair.substr(name.length + 1);
            if (name && value) {
                cookies[decodeURIComponent(name)] = decodeURIComponent(value);
            }
        });
        return cookies;
    }

    /**
     * Gets, sets or removes browser cookies.
     *
     * @param {String} [name] the cookie name to get/set
     * @param {String} [value] the cookie value to set, or null to remove
     * @return {Object/String} all cookie values or a single value
     *
      * @memberof RapidContext.Browser
     */
    function cookie(name, value) {
        if (name === undefined || name === null) {
            return _cookies();
        } else if (value === undefined) {
            return _cookies()[name];
        } else if (value === null) {
            var prefix = encodeURIComponent(name) + "=";
            var suffix = ";path=/;expires=" + new Date(0).toUTCString();
            var domain = window.location.hostname.split(".");
            while (domain.length > 1) {
                var domainPart = ";domain=" + domain.join(".");
                window.document.cookie = prefix + domainPart + suffix;
                domain.shift();
            }
            window.document.cookie = prefix + suffix;
            return null;
        } else {
            window.document.cookie = encodeURIComponent(name) + "=" + encodeURIComponent(value) + ";path=/";
            return value;
        }
    }

    // Create namespaces
    var RapidContext = window.RapidContext || (window.RapidContext = {});
    var module = RapidContext.Browser || (RapidContext.Browser = {});

    // Export namespace symbols
    module.REQUIRED = REQUIRED;
    module.OPTIONAL = OPTIONAL;
    module.isSupported = isSupported;
    module.supports = supports;
    module.info = info;
    module.cookie = cookie;

})(this);
