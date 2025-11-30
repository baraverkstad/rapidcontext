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
 * Provides functions for basic text processing.
 * @namespace RapidContext.Text
 */

/**
 * Converts a value to a string, similar to `String()`, but handles `null`
 * and `undefined` values.
 *
 * @param {*} val the value to convert
 * @return {string} the string representation
 * @name str
 * @memberof RapidContext.Text
 *
 * @example
 * str(null) //==> ''
 * str(undefined) //==> ''
 * str(123) //==> '123'
 */
export function str(val) {
    return (val === null || val === undefined) ? '' : String(val);
}

/**
 * Converts a string to lower case, similar to `String.toLowerCase()`, but
 * handles `null` and `undefined` values.
 *
 * @param {*} val the value to convert
 * @return {string} the lower case string
 * @name lower
 * @memberof RapidContext.Text
 *
 * @example
 * lower('AbC') //==> 'abc'
 */
export function lower(val) {
    return str(val).toLowerCase();
}

/**
 * Converts a string to upper case, similar to `String.toUpperCase()`, but
 * handles `null` and `undefined` values.
 *
 * @param {*} val the value to convert
 * @return {string} the upper case string
 * @name upper
 * @memberof RapidContext.Text
 *
 * @example
 * upper('Abc') //==> 'ABC'
 */
export function upper(val) {
    return str(val).toUpperCase();
}

/**
 * Converts the first character of a string to lower case.
 *
 * @param {*} val the value to convert
 * @return {string} the converted string
 * @name lowerFirst
 * @memberof RapidContext.Text
 *
 * @example
 * lowerFirst('ABC') //==> 'aBC'
 */
export function lowerFirst(val) {
    const s = str(val);
    return s.charAt(0).toLowerCase() + s.slice(1);
}

/**
 * Converts the first character of a string to upper case.
 *
 * @param {*} val the value to convert
 * @return {string} the converted string
 * @name upperFirst
 * @memberof RapidContext.Text
 *
 * @example
 * upperFirst('abc') //==> 'Abc'
 */
export function upperFirst(val) {
    const s = str(val);
    return s.charAt(0).toUpperCase() + s.slice(1);
}

/**
 * Capitalizes the first character of a string and converts the rest to lower
 * case.
 *
 * @param {*} val the value to convert
 * @return {string} the capitalized string
 * @name capitalize
 * @memberof RapidContext.Text
 *
 * @example
 * capitalize('aBC') //==> 'Abc'
 */
export function capitalize(val) {
    const s = str(val);
    return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase();
}

/**
 * Splits a string into an array of words. It handles whitespace, punctuation,
 * and CamelCase boundaries.
 *
 * @param {*} val the value to split
 * @return {Array} the array of words
 * @name words
 * @memberof RapidContext.Text
 *
 * @example
 * words('hello world') //==> ['hello', 'world']
 * words('helloWorld') //==> ['hello', 'World']
 * words('XMLHttpRequest') //==> ['XML', 'Http', 'Request']
 */
export function words(val) {
    return str(val)
        .trim()
        .replaceAll(/(\p{Ll})(\p{Lu})/gu, '$1 $2')
        .replaceAll(/(\p{Lu})(\p{Lu}\p{Ll})/gu, '$1 $2')
        .split(/[\s\p{P}]+/gu)
        .filter(Boolean);
}

/**
 * Converts a string to space separated lower case words.
 *
 * @param {*} val the value to convert
 * @return {string} the lower case string
 * @name lowerCase
 * @memberof RapidContext.Text
 *
 * @example
 * lowerCase('foo bar') //==> 'foo bar'
 * lowerCase('--foo-bar--') //==> 'foo bar'
 * lowerCase('__foo_bar__') //==> 'foo bar'
 */
export function lowerCase(val) {
    return words(val).map(lower).join(' ');
}

/**
 * Converts a string to space separated upper case words.
 *
 * @param {*} val the value to convert
 * @return {string} the upper case string
 * @name upperCase
 * @memberof RapidContext.Text
 *
 * @example
 * upperCase('foo bar') //==> 'FOO BAR'
 * upperCase('--foo-bar--') //==> 'FOO BAR'
 * upperCase('__foo_bar__') //==> 'FOO BAR'
 */
export function upperCase(val) {
    return words(val).map(upper).join(' ');
}

/**
 * Converts a string to camel-case, i.e. each word (except the first) is
 * capitalized and connected without spaces.
 *
 * @param {*} val the value to convert
 * @return {string} the camel-case string
 * @name camelCase
 * @memberof RapidContext.Text
 *
 * @example
 * camelCase('Foo Bar') //==> 'fooBar'
 * camelCase('--foo-bar--') //==> 'fooBar'
 * camelCase('__FOO_BAR__') //==> 'fooBar'
 */
export function camelCase(val) {
    return words(val).map((s, idx) => (idx === 0) ? lower(s) : capitalize(s)).join('');
}

/**
 * Converts a string to kebab-case, i.e. each word is connected with a
 * hyphen.
 *
 * @param {*} val the value to convert
 * @return {string} the kebab-case string
 * @name kebabCase
 * @memberof RapidContext.Text
 *
 * @example
 * kebabCase('foo bar') //==> 'foo-bar'
 * kebabCase('--foo-bar--') //==> 'foo-bar'
 * kebabCase('__foo_bar__') //==> 'foo-bar'
 */
export function kebabCase(val) {
    return words(val).map(lower).join('-');
}

/**
 * Converts a string to snake_case, i.e. each word is connected with an
 * underscore.
 *
 * @param {*} val the value to convert
 * @return {string} the snake_case string
 * @name snakeCase
 * @memberof RapidContext.Text
 *
 * @example
 * snakeCase('foo bar') //==> 'foo_bar'
 * snakeCase('--foo-bar--') //==> 'foo_bar'
 * snakeCase('__foo_bar__') //==> 'foo_bar'
 */
export function snakeCase(val) {
    return words(val).map(lower).join('_');
}

/**
 * Converts a string to Start Case, i.e. each word is capitalized and
 * connected with a single space.
 *
 * @param {*} val the value to convert
 * @return {string} the Start Case string
 * @name startCase
 * @memberof RapidContext.Text
 *
 * @example
 * startCase('foo bar') //==> 'Foo Bar'
 * startCase('--foo-bar--') //==> 'Foo Bar'
 * startCase('__foo_bar__') //==> 'Foo Bar'
 */
export function startCase(val) {
    return words(val).map(capitalize).join(' ');
}

const ESCAPE_MAP = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    '\'': '&apos;'
};

const UNESCAPE_MAP = {
    '&amp;': '&',
    '&lt;': '<',
    '&gt;': '>',
    '&quot;': '"',
    '&apos;': '\''
};

/**
 * Escapes HTML/XML special characters to their entities. Only encodes the
 * '&', '<', '>', '"' and "'" characters.
 *
 * @param {*} val the value to escape
 * @return {string} the escaped string
 * @name escape
 * @memberof RapidContext.Text
 *
 * @example
 * escape('foo & bar') //==> 'foo &amp; bar'
 */
export function escape(val) {
    const s = (val == null) ? '' : [].concat(val).join('');
    return s.replaceAll(/[&<>"']/g, (m) => ESCAPE_MAP[m]);
}

/**
 * Unescapes some HTML/XML entities to their original characters. Only decodes
 * the '&', '<', '>', '"' and "'" characters, as well as numerical entities.
 *
 * @param {*} val the value to unescape
 * @return {string} the unescaped string
 * @name unescape
 * @memberof RapidContext.Text
 *
 * @example
 * unescape('foo &amp; bar') //==> 'foo & bar'
 * unescape('&#39;') //==> "'"
 */
export function unescape(val) {
    return str(val).replaceAll(/&(?:amp|lt|gt|quot|apos|#\d+|#x[0-9a-fA-F]+);/g, (m) => {
        if (m.startsWith('&#x')) {
            return String.fromCharCode(parseInt(m.substring(3, m.length - 1), 16));
        } else if (m.startsWith('&#')) {
            return String.fromCharCode(parseInt(m.substring(2, m.length - 1), 10));
        } else {
            return UNESCAPE_MAP[m];
        }
    });
}

export default {
    str, lower, upper, lowerFirst, upperFirst, capitalize, words, lowerCase,
    upperCase, camelCase, kebabCase, snakeCase, startCase, escape, unescape
};
