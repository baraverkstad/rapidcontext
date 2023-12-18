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
 * Provides functions for data conversion, filtering, mapping, etc.
 * @namespace RapidContext.Data
 */

import { isNil, isFunction, isObject, isArrayLike, isIterable, hasProperty, hasValue } from './fn.mjs';

const OFF = ['null', 'undefined', '0', 'f', 'false', 'off', 'n', 'no'];

/**
 * Converts a value to a boolean. This is identical to `!!val`, but also
 * converts `"null"`, `"0"`, `"false"`, `"off"` and similar values to `false`.
 *
 * @param {*} val the value to convert
 * @return {boolean} `true` or `false` depending on the value
 * @name bool
 * @memberof RapidContext.Data
 *
 * @example
 * bool(undefined) //==> false
 * bool('') //==> false
 * bool('FaLsE') //==> false
 */
export function bool(val) {
    return !!val && !OFF.includes(String(val).toLowerCase());
}

/**
 * Creates a new array from a collection of elements. This is similar to
 * `Array.from()`, but also supports iterating over object properties (using
 * `Object.values()`). When more than a single argument is provided, this
 * function is similar to `Array.of()`.
 *
 * @param {Object|Iterable|...*} coll the elements to include
 * @return {Array} a new `Array` with all the collection elements
 * @name array
 * @memberof RapidContext.Data
 *
 * @example
 * array(null) //==> []
 * array({ a: 1, b: 2, c: 3 }) //==> [1, 2, 3]
 * array(1, 2, 3) //==> [1, 2, 3]
 */
export function array(coll) {
    if (arguments.length === 1 && isNil(coll)) {
        return [];
    } else if (arguments.length === 1 && isObject(coll)) {
        return Object.values(coll);
    } else if (arguments.length === 1 && (isArrayLike(coll) || isIterable(coll))) {
        return Array.from(coll);
    } else {
        return Array.from(arguments);
    }
}

/**
 * Creates a new object from a collection of properties. The properties can be
 * specified in a number of ways, either as two separate arrays of keys and
 * values, as a single array of key-value pairs, as a object with properties to
 * copy, or as a lookup function (or query path). The values can be specified
 * as an array, a lookup object, a generator function, or a constant value.
 *
 * @param {Array|Object|function|string} keys the object keys or key-pairs
 * @param {Array|Object|function|*} [values] the values or generator function
 * @return {Object} a new `Object` with the specified properties
 * @name object
 * @memberof RapidContext.Data
 *
 * @example
 * object(['a', 'b'], true) //==> { a: true, b: true }
 * object(['a', 'b'], [1, 2]) //==> { a: 1, b: 2 }
 * object(['a', 'b'], { a: 1, b: 2, c: 3, d: 4 }) //==> { a: 1, b: 2 }
 * object({ a: 1, b: 2 }, true) //==> { a: true, b: true }
 * object('id', [{ id: 'a', val: 1 }, ...]) //==> { a: { id: 'a', val: 1 }, ... }
 */
export function object(keys, values) {
    function keyPair(key, idx) {
        if (Array.isArray(values)) {
            return [key, values[idx]];
        } else if (isObject(values)) {
            return [key, values[key]];
        } else if (isFunction(values)) {
            try {
                return [key, values(key)];
            } catch (ignore) {
                return [key];
            }
        } else {
            return [key, values];
        }
    }
    function merge(obj, value) {
        let k = Array.isArray(value) ? value[0] : value;
        let v = Array.isArray(value) ? value[1] : undefined;
        if (hasValue(k) && !(k in obj)) {
            obj[k] = v;
        }
        return obj;
    }
    if (Array.isArray(keys)) {
        if (arguments.length < 2) {
            return keys.reduce(merge, {});
        } else {
            return keys.map(keyPair).reduce(merge, {});
        }
    } else if (isObject(keys)) {
        if (arguments.length < 2) {
            return Object.assign({}, keys);
        } else {
            return Object.keys(keys).map(keyPair).reduce(merge, {});
        }
    } else if (keys) {
        keys = map(keys, values);
        return keys.map(keyPair).reduce(merge, {});
    } else {
        return {};
    }
}

/**
 * Creates a deep copy of an `Array` or an `Object`. Nested values will be
 * copied recursively. Only plain objects are be copied, others (including
 * primitive values) are returned as-is.
 *
 * @param {Array|Object|*} value the object or array to copy
 * @return {Array|Object|*} a new `Array` or `Object` with the same content
 * @name clone
 * @memberof RapidContext.Data
 */
export function clone(value) {
    if (Array.isArray(value) || isObject(value)) {
        return map(clone, value);
    } else {
        return value;
    }
}

/**
 * Retrieves one or more values from a data structure. The `key` provides a
 * dot-separated query path to traverse the data structure to any depth.
 * Wildcard property names are specified as `*`. Wildcard array elements are
 * specified as `[]`. The path may also be provided as an Array if needed.
 * Returns a bound function if only a single argument is specified.
 *
 * @param {string|Array} key the value query path
 * @param {object|Array} [val] the data structure to traverse
 * @return {number|function} the value found, or a bound function
 * @name get
 * @memberof RapidContext.Data
 *
 * @example
 * get('a.b', { a: { b: 42 } }) //==> 42
 * get('*.b', { a: { b: 42 }, c: { b: 13 } }) //==> [42, 13]
 * get('a.*', { a: { b: 42 }, c: { b: 13 } }) //==> [42]
 * get('[].b', [ { a: 42 }, { b: 13 }, { b: 1} }) //==> [13, 1]
 * get('1.b', [ { a: 42 }, { b: 13 }, { b: 1} }) //==> 13
 */
export function get(key, val) {
    if (arguments.length < 2) {
        return get.bind(null, ...arguments);
    } else {
        let path = Array.isArray(key) ? [].concat(key) : ('' + key).split(/(?=\[)|\./);
        let hasWildcard = path.some((el) => el === '*' || el === '[]');
        let ctx = [val];
        while (path.length > 0 && ctx.length > 0) {
            ctx = ctx.filter((o) => !isNil(o));
            let el = path.shift();
            if (el === '*' || el === '[]') {
                let arrs = (el === '*') ? ctx.map(Object.values) : ctx.filter(Array.isArray);
                ctx = Array.prototype.concat.apply([], arrs); // FIXME: arrs.flat()
            } else {
                let k = /^\[\d+\]$/.test(el) ? parseInt(el.substr(1), 10) : el;
                ctx = ctx.filter(hasProperty(k)).map((o) => o[k]);
            }
        }
        return (ctx.length > 1 || hasWildcard) ? ctx : ctx[0];
    }
}

/**
 * Filters a collection based on a predicate function. The input collection can
 * be either an Array-like object, or a plain object. The predicate function
 * `fn(val, idx/key, arr/obj)` is called for each array item or object
 * property. As an alternative to a function, a string query path can be
 * specified instead. Returns a new `Array` or `Object` depending on the input
 * collection. Returns a bound function if only a single argument is specified.
 *
 * @param {function|string} fn the predicate function or query path
 * @param {object|Array} [coll] the collection to filter
 * @return {object|Array|function} the new collection, or a bound function
 * @name filter
 * @memberof RapidContext.Data
 *
 * @example
 * filter(Boolean, [null, undefined, true, 0, '']) //==> [true]
 * filter(Boolean, { a: null, b: true, c: 0, d: 1 }) //==> { b: true, d: 1 }
 * filter('id', [null, { id: 3 }, {}, { id: false }]) //==> [3]
 */
export function filter(fn, coll) {
    function test(v, k, o) {
        try {
            return fn(v, k, o);
        } catch (e) {
            return false;
        }
    }
    fn = isFunction(fn) ? fn : get(fn);
    if (arguments.length < 2) {
        return filter.bind(null, ...arguments);
    } else if (Array.isArray(coll)) {
        return coll.filter(test);
    } else if (isObject(coll)) {
        let obj = {};
        for (let k in coll) {
            let v = coll[k];
            if (hasProperty(k, coll) && test(v, k, coll)) {
                obj[k] = v;
            }
        }
        return obj;
    } else {
        let arr = [];
        let len = +(coll && coll.length) || 0;
        for (let i = 0; i < len; i++) {
            let v = coll[i];
            if (test(v, i, coll)) {
                arr.push(v);
            }
        }
        return arr;
    }
}

/**
 * Concatenates nested `Array` elements into a parent `Array`. As an
 * alternative, the array elements may be specified as arguments. Only the
 * first level of `Array` elements are flattened by this method. In modern
 * environments, consider using `Array.prototype.flat()` instead.
 *
 * @param {Array|...*} arr the input array or sequence of elements
 * @return {Array} the new flattened `Array`
 * @name flatten
 * @memberof RapidContext.Data
 */
export function flatten(arr) {
    if (arguments.length === 1 && Array.isArray(arr)) {
        return Array.prototype.concat.apply([], arr);
    } else {
        return Array.prototype.concat.apply([], arguments);
    }
}

/**
 * Applies a function to each item or property in an input collection and
 * returns the results. The input collection can be either an Array-like
 * object, or a plain object. The mapping function `fn(val, idx/key, arr/obj)`
 * is called for each array item or object property. As an alternative to a
 * function, a string query path can be specified instead. Returns a new
 * `Array` or `Object` depending on the input collection. Returns a bound
 * function if only a single argument is specified.
 *
 * @param {function|string} fn the mapping function or query path
 * @param {object|Array} [coll] the collection to process
 * @return {object|Array|function} the new collection, or a bound function
 * @name map
 * @memberof RapidContext.Data
 *
 * @example
 * map(Boolean, [null, true, 0, 1, '']) //==> [false, true, false, true, false]
 * map(Boolean, { a: null, b: true, c: 0 }) //==> { a: false, b: true, c: false }
 * map('id', [{ id: 1 }, { id: 3 }) //==> [1, 3]
 */
export function map(fn, coll) {
    function inner(v, k, o) {
        try {
            return fn(v, k, o);
        } catch (e) {
            return undefined;
        }
    }
    fn = isFunction(fn) ? fn : get(fn);
    if (arguments.length < 2) {
        return map.bind(null, ...arguments);
    } else if (Array.isArray(coll)) {
        return coll.map(inner);
    } else if (isObject(coll)) {
        let obj = {};
        for (let k in coll) {
            let v = coll[k];
            if (hasProperty(k, coll)) {
                obj[k] = inner(v, k, coll);
            }
        }
        return obj;
    } else {
        let arr = [];
        let len = +(coll && coll.length) || 0;
        for (let i = 0; i < len; i++) {
            let v = coll[i];
            arr.push(inner(v, i, coll));
        }
        return arr;
    }
}

/**
 * Returns the unique values of a collection. An optional function may be
 * provided to extract the key to compare with the other elements. If no
 * function is specified, `JSON.stringify` is used to determine uniqueness.
 * Returns a bound function if only a single function argument is specified.
 *
 * @param {function} [fn] the comparison key extract function
 * @param {Array|Object|Iterable} coll the input collection
 * @return {Array|function} the new `Array`, or a bound function
 * @name uniq
 * @memberof RapidContext.Data
 *
 * @example
 * uniq([1, 2, 3, 3, 2, 1]) //==> [1, 2, 3]
 * uniq([{ a: 1, b: 2 }, { b: 2, a: 1 }]) //==> [{ a: 1, b: 2 }, { b: 2, a: 1 }]
 */
export function uniq(fn, coll) {
    if (arguments.length === 0) {
        return [];
    } else if (arguments.length === 1 && isFunction(fn)) {
        return uniq.bind(null, fn);
    } else if (arguments.length === 1) {
        coll = arguments[0];
        fn = JSON.stringify;
    }
    let arr = array(coll);
    let keys = map(fn, arr).map(String);
    return Object.values(object(keys, arr));
}

/**
 * Compares two values to determine relative order. The return value is a
 * number whose sign indicates the relative order: negative if `a` is less than
 * `b`, positive if `a` is greater than `b`, or zero if they are equal. If the
 * first argument is a function, it will be used to extract the values to
 * compare from the other arguments. Returns a bound function if only a single
 * argument is specified.
 *
 * @param {function} [valueOf] a function to extract the value
 * @param {*} a the first value
 * @param {*} [b] the second value
 * @return {number|function} the test result, or a bound function
 * @name compare
 * @memberof RapidContext.Data
 *
 * @example
 * compare(1, 1) //==> 0
 * compare(13, 42) //==> -1
 * compare('b', 'a') //==> +1
 *
 * @example
 * compare((s) => s.toLowerCase(), 'Abc', 'aBC') //==> 0
 */
export function compare(valueOf, a, b) {
    if (arguments.length < 2) {
        return compare.bind(null, ...arguments);
    } else if (arguments.length < 3 || !isFunction(valueOf)) {
        b = arguments[1];
        a = arguments[0];
    } else {
        a = valueOf(a);
        b = valueOf(b);
    }
    return a < b ? -1 : (a > b ? 1 : 0);
}

/**
 * Returns a sorted copy of a collection. An optional function may be provided
 * to extract the value to compare with the other elements. If no function is
 * specified, the comparison is made on the elements themselves. Returns a bound
 * function if only a single function argument is specified.
 *
 * @param {function} [fn] the value extract function
 * @param {Array|Object|Iterable} coll the input collection
 * @return {Array|function} the new sorted `Array`, or a bound function
 * @name sort
 * @memberof RapidContext.Data
 *
 * @example
 * sort([3, 2, 1]) //==> [1, 2, 3]
 * sort(get('pos'), [{ pos: 3 }, { pos: 1 }]) //==> [{ pos: 1 }, { pos: 3 }]
 *
 * @example
 * const toLower = (s) => s.toLowerCase();
 * const sortInsensitive = sort(toLower);
 * sortInsensitive(['b', 'B', 'a', 'aa', 'A']) //==> ['a', 'A', 'aa', 'b', 'B']
 */
export function sort(fn, coll) {
    if (arguments.length === 0) {
        return [];
    } else if (arguments.length === 1 && isFunction(fn)) {
        return sort.bind(null, fn);
    } else if (arguments.length === 1) {
        coll = arguments[0];
        fn = null;
    }
    let arr = array(coll);
    arr.sort(fn ? compare(fn) : compare);
    return arr;
}

export default { bool, array, object, clone, get, filter, flatten, map, uniq, compare, sort };
