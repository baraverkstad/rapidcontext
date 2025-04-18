import test from 'node:test';
import assert from 'node:assert';
import fn from 'rapidcontext/fn.mjs';

test('fn.isNil()', () => {
    assert.strictEqual(fn.isNil(undefined), true);
    assert.strictEqual(fn.isNil(null), true);
    assert.strictEqual(fn.isNil(true), false);
    assert.strictEqual(fn.isNil(''), false);
    assert.strictEqual(fn.isNil('undefined'), false);
});

test('fn.isNull()', () => {
    assert.strictEqual(fn.isNull(undefined), false);
    assert.strictEqual(fn.isNull(null), true);
    assert.strictEqual(fn.isNull(true), false);
});

test('fn.isUndefined()', () => {
    assert.strictEqual(fn.isUndefined(undefined), true);
    assert.strictEqual(fn.isUndefined(null), false);
    assert.strictEqual(fn.isUndefined(''), false);
});

test('fn.isBoolean()', () => {
    assert.strictEqual(fn.isBoolean(undefined), false);
    assert.strictEqual(fn.isBoolean(null), false);
    assert.strictEqual(fn.isBoolean(0), false);
    assert.strictEqual(fn.isBoolean(true), true);
    assert.strictEqual(fn.isBoolean(false), true);
    assert.strictEqual(fn.isBoolean(Boolean(1)), true);
    assert.strictEqual(fn.isBoolean(''), false);
    assert.strictEqual(fn.isBoolean({}), false);
});

test('fn.isFunction()', () => {
    assert.strictEqual(fn.isFunction(undefined), false);
    assert.strictEqual(fn.isFunction(null), false);
    assert.strictEqual(fn.isFunction(fn.isFunction), true);
});

test('fn.isNumber()', () => {
    assert.strictEqual(fn.isNumber(undefined), false);
    assert.strictEqual(fn.isNumber(null), false);
    assert.strictEqual(fn.isNumber(0), true);
    assert.strictEqual(fn.isNumber(1.123), true);
    assert.strictEqual(fn.isNumber(Number('4711')), true);
    assert.strictEqual(fn.isNumber(Infinity), true);
    assert.strictEqual(fn.isNumber(-Infinity), true);
    assert.strictEqual(fn.isNumber(NaN), true);
    assert.strictEqual(fn.isNumber('1'), false);
    assert.strictEqual(fn.isNumber({}), false);
});

test('fn.isBigInt()', () => {
    assert.strictEqual(fn.isBigInt(undefined), false);
    assert.strictEqual(fn.isBigInt(0), false);
    assert.strictEqual(fn.isBigInt(123n), true);
    assert.strictEqual(fn.isBigInt(Infinity), false);
    assert.strictEqual(fn.isBigInt(NaN), false);
});

test('fn.isString()', () => {
    assert.strictEqual(fn.isString(undefined), false);
    assert.strictEqual(fn.isString(null), false);
    assert.strictEqual(fn.isString(''), true);
    assert.strictEqual(fn.isString({}), false);
    assert.strictEqual(fn.isString(['test']), false);
});

test('fn.isObject()', () => {
    function TestObject() {
        this.value = 123;
    }
    assert.strictEqual(fn.isObject(undefined), false);
    assert.strictEqual(fn.isObject(null), false);
    assert.strictEqual(fn.isObject(''), false);
    assert.strictEqual(fn.isObject({}), true);
    assert.strictEqual(fn.isObject([]), false);
    assert.strictEqual(fn.isObject(/test/), false);
    assert.strictEqual(fn.isObject(new TestObject()), false);
});

test('fn.isError()', () => {
    assert.strictEqual(fn.isError(undefined), false);
    assert.strictEqual(fn.isError(null), false);
    assert.strictEqual(fn.isError({}), false);
    assert.strictEqual(fn.isError(new Error()), true);
    assert.strictEqual(fn.isError(new TypeError()), true);
});

test('fn.isDate()', () => {
    assert.strictEqual(fn.isDate(undefined), false);
    assert.strictEqual(fn.isDate(null), false);
    assert.strictEqual(fn.isDate({}), false);
    assert.strictEqual(fn.isDate(0), false);
    assert.strictEqual(fn.isDate(new Date()), true);
});

test('fn.isRegExp()', () => {
    assert.strictEqual(fn.isRegExp(undefined), false);
    assert.strictEqual(fn.isRegExp(null), false);
    assert.strictEqual(fn.isRegExp({}), false);
    assert.strictEqual(fn.isRegExp(/abc/), true);
});

test('fn.isPromise()', () => {
    assert.strictEqual(fn.isPromise(undefined), false);
    assert.strictEqual(fn.isPromise(null), false);
    assert.strictEqual(fn.isPromise({}), false);
    assert.strictEqual(fn.isPromise(Promise.resolve(null)), true);
    assert.strictEqual(fn.isPromise(Promise.all([])), true);
});

test('fn.isArrayLike()', () => {
    assert.strictEqual(fn.isArrayLike(undefined), false);
    assert.strictEqual(fn.isArrayLike('123'), true);
    assert.strictEqual(fn.isArrayLike({}), false);
    assert.strictEqual(fn.isArrayLike([]), true);
    assert.strictEqual(fn.isArrayLike(new Int8Array([1, 2, 3])), true);
});

test('fn.isSetLike()', () => {
    assert.strictEqual(fn.isSetLike(undefined), false);
    assert.strictEqual(fn.isSetLike('123'), false);
    assert.strictEqual(fn.isSetLike([]), false);
    assert.strictEqual(fn.isSetLike(new Set([])), true);
    assert.strictEqual(fn.isSetLike(new Set([1, 2, 3])), true);
    assert.strictEqual(fn.isSetLike(new Map([[1, 'one'], [2, 'two']])), true);
});

test('fn.isIterable()', () => {
    assert.strictEqual(fn.isIterable(undefined), false);
    assert.strictEqual(fn.isIterable('123'), true);
    assert.strictEqual(fn.isIterable([]), true);
    assert.strictEqual(fn.isIterable(new Set([1, 2, 3])), true);
    assert.strictEqual(fn.isIterable(new Map([[1, 'one'], [2, 'two']])), true);
});

test('fn.isTypeOf()', () => {
    const isFunction = fn.isTypeOf('function');
    const isFunctionOrObject = fn.isTypeOf(['function', 'object']);
    assert.strictEqual(typeof(isFunction), 'function');
    assert.strictEqual(isFunction(undefined), false);
    assert.strictEqual(isFunction(null), false);
    assert.strictEqual(isFunction({}), false);
    assert.strictEqual(isFunction(isFunction), true);
    assert.strictEqual(isFunction(Array), true);
    assert.strictEqual(isFunctionOrObject({}), true);
    assert.strictEqual(isFunctionOrObject(isFunction), true);
});

test('fn.isInstanceOf()', () => {
    const isArray = fn.isInstanceOf(Array);
    assert.strictEqual(typeof(isArray), 'function');
    assert.strictEqual(isArray(undefined), false);
    assert.strictEqual(isArray(null), false);
    assert.strictEqual(isArray({}), false);
    assert.strictEqual(isArray([]), true);
    assert.strictEqual(isArray(new Array(1)), true);
});

test('fn.hasProperty()', () => {
    const hasLength = fn.hasProperty('length');
    assert.strictEqual(typeof(hasLength), 'function');
    assert.strictEqual(hasLength(undefined), false);
    assert.strictEqual(hasLength(null), false);
    assert.strictEqual(hasLength({}), false);
    assert.strictEqual(hasLength([]), true);
    assert.strictEqual(hasLength({ length: 42 }), true);
    assert.strictEqual(fn.hasProperty(undefined, { 'undefined': 13 }), true);
    assert.strictEqual(fn.hasProperty(undefined, { 'null': 42 }), false);
});

test('fn.hasValue()', () => {
    assert.strictEqual(fn.hasValue(undefined), false);
    assert.strictEqual(fn.hasValue(null), false);
    assert.strictEqual(fn.hasValue(false), true);
    assert.strictEqual(fn.hasValue(true), true);
    assert.strictEqual(fn.hasValue(NaN), false);
    assert.strictEqual(fn.hasValue(0), true);
    assert.strictEqual(fn.hasValue(1), true);
    assert.strictEqual(fn.hasValue(0n), true);
    assert.strictEqual(fn.hasValue(''), false);
    assert.strictEqual(fn.hasValue('abc'), true);
    assert.strictEqual(fn.hasValue(/regex/), true);
    assert.strictEqual(fn.hasValue(Symbol('foo')), true);
    assert.strictEqual(fn.hasValue({}), false);
    assert.strictEqual(fn.hasValue({ a: 1 }), true);
    assert.strictEqual(fn.hasValue([]), false);
    assert.strictEqual(fn.hasValue([0, 1, 2]), true);
    assert.strictEqual(fn.hasValue(new Int8Array(0)), false);
    assert.strictEqual(fn.hasValue(new Int8Array([1, 2, 3])), true);
    assert.strictEqual(fn.hasValue(new Set([])), false);
    assert.strictEqual(fn.hasValue(new Set([1, 2, 3])), true);
});

test('fn.eq()', () => {
    assert.strictEqual(fn.eq(undefined, undefined), true);
    assert.strictEqual(fn.eq(null, undefined), false);
    assert.strictEqual(fn.eq(1, 1), true);
    assert.strictEqual(fn.eq(1, 42), false);
    assert.strictEqual(fn.eq(NaN, NaN), false);
    assert.strictEqual(fn.eq(9007199254740991n, 9007199254740991n), true);
    assert.strictEqual(fn.eq('abc', 'abc'), true);
    assert.strictEqual(fn.eq('abc', 'abc', '123'), false);
    assert.strictEqual(fn.eq([], []), true);
    assert.strictEqual(fn.eq([], {}), false);
    assert.strictEqual(fn.eq([1, 2, 3], [1, 2, 3]), true);
    assert.strictEqual(fn.eq([1, 2, 3], [1, 2, 9]), false);
    assert.strictEqual(fn.eq({}, {}), true);
    assert.strictEqual(fn.eq({}, []), false);
    assert.strictEqual(fn.eq({ a: 1 }, { a: 1 }), true);
    assert.strictEqual(fn.eq({ a: 1 }, { b: 1 }), false);
    assert.strictEqual(fn.eq({ a: 1, b: 13 }, { b: 13, a: 1 }), true);
    assert.strictEqual(fn.eq({ a: [1, 2, 3], b: { c: 1337 } }, { b: { c: 1337 }, a: [1, 2, 3] }), true);
    assert.strictEqual(fn.eq({ a: [1, 2, 3], b: { c: 1337 } }, { a: [1, 2, 3], b: { c: 1337, d: 9 } }), false);
    const dt = new Date();
    assert.strictEqual(fn.eq(dt, dt), true);
    assert.strictEqual(fn.eq(new Date(), new Date()), false);
    assert.strictEqual(fn.eq(Symbol('foo'), Symbol('foo')), false);
    assert.strictEqual(fn.eq(Symbol.for('foo'), Symbol.for('foo')), true);
    const is13 = fn.eq(13);
    assert.strictEqual(typeof(is13), 'function');
    assert.strictEqual(is13(12), false);
    assert.strictEqual(is13(13), true);
    assert.strictEqual(is13(13, 13, 11), false);
    const getFolderId = (o) => o.folder.id;
    const isValidFolderId = fn.eq(getFolderId, 13);
    assert.strictEqual(typeof(isValidFolderId), 'function');
    assert.strictEqual(isValidFolderId(), false);
    assert.strictEqual(isValidFolderId({ folder: { id: 1 } }), false);
    assert.strictEqual(isValidFolderId({ folder: { id: 13 } }), true);
    assert.strictEqual(isValidFolderId(123), false);
});
