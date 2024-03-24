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

/**
 * Provides predicate functions for type checking and equality.
 * @namespace RapidContext.Fn
 */

/**
 * Checks if a value is either `null` or `undefined`.
 *
 * @param {*} val the value to check
 * @return {boolean} `true` on match, or `false` otherwise
 * @name isNil
 * @memberof RapidContext.Fn
 */
export function isNil(val) {
    return val === null || val === undefined;
}

/**
 * Checks if a value is `null` (but not `undefined`).
 *
 * @param {*} val the value to check
 * @return {boolean} `true` on match, or `false` otherwise
 * @name isNull
 * @memberof RapidContext.Fn
 */
export function isNull(val) {
    return val === null;
}

/**
 * Checks if `typeof(val)` matches a string or an array element. Returns a
 * bound function if only the first argument is specified.
 *
 * @param {string|Array} type the type name or array of type names
 * @param {*} [val] the value to check
 * @return {boolean|function} the match result, or a bound function
 * @name isTypeOf
 * @memberof RapidContext.Fn
 */
export function isTypeOf(type, val) {
    if (arguments.length < 2) {
        return isTypeOf.bind(null, ...arguments);
    } else {
        return Array.isArray(type) ? type.includes(typeof(val)) : typeof(val) === type;
    }
}

/**
 * Checks if a value is `undefined` (but not `null`).
 *
 * @param {*} val the value to check
 * @return {boolean} `true` on match, or `false` otherwise
 * @name isUndefined
 * @memberof RapidContext.Fn
 */
export function isUndefined(val) {
    return isTypeOf('undefined', val);
}

/**
 * Checks if a value is a `boolean`` (i.e. `typeof(..) = "boolean"`).
 *
 * @param {*} val the value to check
 * @return {boolean} `true` on match, or `false` otherwise
 * @name isBoolean
 * @memberof RapidContext.Fn
 */
export function isBoolean(val) {
    return isTypeOf('boolean', val);
}

/**
 * Checks if a value is a `function` (i.e. `typeof(..) = "function"`).
 *
 * @param {*} val the value to check
 * @return {boolean} `true` on match, or `false` otherwise
 * @name isFunction
 * @memberof RapidContext.Fn
 */
export function isFunction(val) {
    return isTypeOf('function', val);
}

/**
 * Checks if a value is a `number` (i.e. `typeof(..) = "number"`).
 *
 * @param {*} val the value to check
 * @return {boolean} `true` on match, or `false` otherwise
 * @name isNumber
 * @memberof RapidContext.Fn
 */
export function isNumber(val) {
    return isTypeOf('number', val);
}

/**
 * Checks if a value is a `bigint` (i.e. `typeof(..) = "bigint"`).
 *
 * @param {*} val the value to check
 * @return {boolean} `true` on match, or `false` otherwise
 * @name isBigInt
 * @memberof RapidContext.Fn
 */
export function isBigInt(val) {
    return isTypeOf('bigint', val);
}

/**
 * Checks if a value is a `string` (i.e. `typeof(..) = "string"`).
 *
 * @param {*} val the value to check
 * @return {boolean} `true` on match, or `false` otherwise
 * @name isString
 * @memberof RapidContext.Fn
 */
export function isString(val) {
    return isTypeOf('string', val);
}

/**
 * Checks if a value is a plain `Object` (and not based on another prototype).
 *
 * @param {*} val the value to check
 * @return {boolean} `true` on match, or `false` otherwise
 * @name isObject
 * @memberof RapidContext.Fn
 */
export function isObject(val) {
    // Note: Some built-in objects (e.g. Arguments) share prototype with Object
    // Note: Object.prototype.toString() gives weird result in some other cases
    let proto = Object.prototype;
    let str = proto.toString;
    return !!val && Object.getPrototypeOf(val) === proto && str.call(val) === '[object Object]';
}

/**
 * Checks if a value is `instanceof` a constructor function. Returns a bound
 * function if only the first argument is specified.
 *
 * @param {function} constructor the constructor function or class
 * @param {*} val the value to check
 * @return {boolean|function} the match result, or a bound function
 * @name isInstanceOf
 * @memberof RapidContext.Fn
 */
export function isInstanceOf(constructor, val) {
    if (arguments.length < 2) {
        return isInstanceOf.bind(null, ...arguments);
    } else {
        return val instanceof constructor;
    }
}

/**
 * Checks if a value is an instance of `Error`.
 *
 * @param {*} val the value to check
 * @return {boolean} `true` on match, or `false` otherwise
 * @name isError
 * @memberof RapidContext.Fn
 */
export function isError(val) {
    return isInstanceOf(Error, val);
}

/**
 * Checks if a value is an instance of `Date`.
 *
 * @param {*} val the value to check
 * @return {boolean} `true` on match, or `false` otherwise
 * @name isDate
 * @memberof RapidContext.Fn
 */
export function isDate(val) {
    return isInstanceOf(Date, val);
}

/**
 * Checks if a value is an instance of `RegExp`.
 *
 * @param {*} val the value to check
 * @return {boolean} `true` on match, or `false` otherwise
 * @name isRegExp
 * @memberof RapidContext.Fn
 */
export function isRegExp(val) {
    return isInstanceOf(RegExp, val);
}

/**
 * Checks if a value is an instance of `Promise`.
 *
 * @param {*} val the value to check
 * @return {boolean} `true` on match, or `false` otherwise
 * @name isPromise
 * @memberof RapidContext.Fn
 */
export function isPromise(val) {
    return isInstanceOf(Promise, val);
}

const isArray = Array.isArray;

/**
 * Checks if a value is Array-like. This is an object with a numeric `length`
 * property (but not a function).
 *
 * @param {*} val the value to check
 * @return {boolean} `true` on match, or `false` otherwise
 * @name isArrayLike
 * @memberof RapidContext.Fn
 */
export function isArrayLike(val) {
    return !!val && isNumber(val.length) && val.length >= 0 && !isFunction(val);
}

/**
 * Checks if a value is Set-like. This is an object with a numeric `size`
 * property, and `has()` and `keys()` methods.
 *
 * @param {*} val the value to check
 * @return {boolean} `true` on match, or `false` otherwise
 * @name isSetLike
 * @memberof RapidContext.Fn
 */
export function isSetLike(val) {
    return !!val && isNumber(val.size) && isFunction(val.has) && isFunction(val.keys);
}

/**
 * Checks if a value is iterable (i.e. follows the iterable protocol). This is
 * an object with a `Symbol.iterator` method returning the values.
 *
 * @param {*} val the value to check
 * @return {boolean} `true` on match, or `false` otherwise
 * @name isIterable
 * @memberof RapidContext.Fn
 */
export function isIterable(val) {
    return !!val && isFunction(val[Symbol.iterator]);
}

/**
 * Checks if an object has an own property with a specified key. Returns a
 * bound function if only the first argument is specified.
 *
 * @param {string} key the property key to check for
 * @param {Object} [obj] the object to check
 * @return {boolean|function} the match result, or a bound function
 * @name hasProperty
 * @memberof RapidContext.Fn
 */
export function hasProperty(key, obj) {
    if (arguments.length < 2) {
        return hasProperty.bind(null, ...arguments);
    } else {
        return !!obj && Object.prototype.hasOwnProperty.call(obj, key);
    }
}

/**
 * Checks if a value is set. Returns `false` for `null`, `undefined`, empty
 * arrays, empty objects or empty strings. Boolean `false` and `0` will return
 * `true`, as these are defined values.
 *
 * @param {*} val the value to check
 * @return {boolean} `true` if the value is set, or `false` otherwise
 * @name hasValue
 * @memberof RapidContext.Fn
 */
export function hasValue(val) {
    if (isNumber(val)) {
        return !isNaN(val);
    } else if (isArrayLike(val)) {
        return val.length > 0;
    } else if (isObject(val)) {
        return Object.keys(val).length > 0;
    } else if (isSetLike(val)) {
        return val.size > 0;
    } else {
        return isBoolean(val) || isBigInt(val) || !!val;
    }
}

/**
 * Checks if one or more values are equal. Supports recursively comparing both
 * arrays and objects, as well as standard values. Returns a bound function if
 * only the first argument is specified. Also returns a bound function if one
 * argument is a function.
 *
 * @param {...*} values the values to compare
 * @return {boolean|function} the test result, or a bound function
 * @name eq
 * @memberof RapidContext.Fn
 *
 * @example
 * eq(1, 1, 1, 3) //==> false
 * eq({ a: 1, b: 2 }, { b: 2, a: 1 }) //==> true
 *
 * @example
 * let isAbc = eq("abc");
 * isAbc("abc") //==> true
 *
 * @example
 * let hasValidA = eq(42, get("a"));
 * hasValidA({ a: 42 }) //==> true
 */
export function eq(...values) {
    function test() {
        let valid = [];
        for (let el of values) {
            if (isFunction(el)) {
                try {
                    el = el.apply(null, arguments);
                } catch (ignore) {
                    return false;
                }
            }
            if (valid.length > 0 && !isEq(valid[0], el)) {
                return false;
            }
            valid.push(el);
        }
        return true;
    }
    function isEq(a, b) {
        if (isArray(a) || isArray(b)) {
            const aLen = isArray(a) && a.length;
            const bLen = isArray(b) && b.length;
            return aLen === bLen && a.every((el, i) => isEq(el, b[i]));
        } else if (isObject(a) || isObject(b)) {
            const aKeys = isObject(a) && Object.keys(a).sort();
            const bKeys = isObject(b) && Object.keys(b).sort();
            return isEq(aKeys, bKeys) && aKeys.every((k) => isEq(a[k], b[k]));
        } else {
            return a === b;
        }
    }
    if (arguments.length < 2) {
        return eq.bind(null, ...arguments);
    } else if (values.some(isFunction)) {
        return test;
    } else {
        return test();
    }
}

export default {
    isNil, isNull, isUndefined, isBoolean, isFunction, isNumber, isBigInt,
    isString, isObject, isError, isDate, isRegExp, isPromise, isArrayLike,
    isSetLike, isIterable, isTypeOf, isInstanceOf, hasProperty, hasValue, eq
};
