import tap from 'tap';
import fn from '../../../../system/files/js/rapidcontext/fn.mjs';

tap.test('fn.isNil()', (t) => {
    t.equal(fn.isNil(undefined), true);
    t.equal(fn.isNil(null), true);
    t.equal(fn.isNil(true), false);
    t.equal(fn.isNil(''), false);
    t.equal(fn.isNil('undefined'), false);
    t.end();
});

tap.test('fn.isNull()', (t) => {
    t.equal(fn.isNull(undefined), false);
    t.equal(fn.isNull(null), true);
    t.equal(fn.isNull(true), false);
    t.end();
});

tap.test('fn.isUndefined()', (t) => {
    t.equal(fn.isUndefined(undefined), true);
    t.equal(fn.isUndefined(null), false);
    t.equal(fn.isUndefined(''), false);
    t.end();
});

tap.test('fn.isBoolean()', (t) => {
    t.equal(fn.isBoolean(undefined), false);
    t.equal(fn.isBoolean(null), false);
    t.equal(fn.isBoolean(0), false);
    t.equal(fn.isBoolean(true), true);
    t.equal(fn.isBoolean(false), true);
    t.equal(fn.isBoolean(Boolean(1)), true);
    t.equal(fn.isBoolean(''), false);
    t.equal(fn.isBoolean({}), false);
    t.end();
});

tap.test('fn.isFunction()', (t) => {
    t.equal(fn.isFunction(undefined), false);
    t.equal(fn.isFunction(null), false);
    t.equal(fn.isFunction(fn.isFunction), true);
    t.end();
});

tap.test('fn.isNumber()', (t) => {
    t.equal(fn.isNumber(undefined), false);
    t.equal(fn.isNumber(null), false);
    t.equal(fn.isNumber(0), true);
    t.equal(fn.isNumber(1.123), true);
    t.equal(fn.isNumber(Number('4711')), true);
    t.equal(fn.isNumber(Infinity), true);
    t.equal(fn.isNumber(-Infinity), true);
    t.equal(fn.isNumber(NaN), true);
    t.equal(fn.isNumber('1'), false);
    t.equal(fn.isNumber({}), false);
    t.end();
});

tap.test('fn.isBigInt()', (t) => {
    t.equal(fn.isBigInt(undefined), false);
    t.equal(fn.isBigInt(0), false);
    t.equal(fn.isBigInt(123n), true);
    t.equal(fn.isBigInt(Infinity), false);
    t.equal(fn.isBigInt(NaN), false);
    t.end();
});

tap.test('fn.isString()', (t) => {
    t.equal(fn.isString(undefined), false);
    t.equal(fn.isString(null), false);
    t.equal(fn.isString(''), true);
    t.equal(fn.isString({}), false);
    t.equal(fn.isString(['test']), false);
    t.end();
});

tap.test('fn.isObject()', (t) => {
    function TestObject() {
        this.value = 123;
    }
    t.equal(fn.isObject(undefined), false);
    t.equal(fn.isObject(null), false);
    t.equal(fn.isObject(''), false);
    t.equal(fn.isObject({}), true);
    t.equal(fn.isObject([]), false);
    t.equal(fn.isObject(/test/), false);
    t.equal(fn.isObject(new TestObject()), false);
    t.end();
});

tap.test('fn.isError()', (t) => {
    t.equal(fn.isError(undefined), false);
    t.equal(fn.isError(null), false);
    t.equal(fn.isError({}), false);
    t.equal(fn.isError(new Error()), true);
    t.equal(fn.isError(new TypeError()), true);
    t.end();
});

tap.test('fn.isDate()', (t) => {
    t.equal(fn.isDate(undefined), false);
    t.equal(fn.isDate(null), false);
    t.equal(fn.isDate({}), false);
    t.equal(fn.isDate(0), false);
    t.equal(fn.isDate(new Date()), true);
    t.end();
});

tap.test('fn.isRegExp()', (t) => {
    t.equal(fn.isRegExp(undefined), false);
    t.equal(fn.isRegExp(null), false);
    t.equal(fn.isRegExp({}), false);
    t.equal(fn.isRegExp(new RegExp('.')), true);
    t.equal(fn.isRegExp(/abc/), true);
    t.end();
});

tap.test('fn.isPromise()', (t) => {
    t.equal(fn.isPromise(undefined), false);
    t.equal(fn.isPromise(null), false);
    t.equal(fn.isPromise({}), false);
    t.equal(fn.isPromise(new Promise(() => null)), true);
    t.equal(fn.isPromise(Promise.all([])), true);
    t.end();
});

tap.test('fn.isArrayLike()', (t) => {
    t.equal(fn.isArrayLike(undefined), false);
    t.equal(fn.isArrayLike('123'), true);
    t.equal(fn.isArrayLike({}), false);
    t.equal(fn.isArrayLike([]), true);
    t.equal(fn.isArrayLike(new Int8Array([1, 2, 3])), true);
    t.end();
});

tap.test('fn.isSetLike()', (t) => {
    t.equal(fn.isSetLike(undefined), false);
    t.equal(fn.isSetLike('123'), false);
    t.equal(fn.isSetLike([]), false);
    t.equal(fn.isSetLike(new Set([])), true);
    t.equal(fn.isSetLike(new Set([1, 2, 3])), true);
    t.equal(fn.isSetLike(new Map([[1, 'one'], [2, 'two']])), true);
    t.end();
});

tap.test('fn.isIterable()', (t) => {
    t.equal(fn.isIterable(undefined), false);
    t.equal(fn.isIterable('123'), true);
    t.equal(fn.isIterable([]), true);
    t.equal(fn.isIterable(new Set([1, 2, 3])), true);
    t.equal(fn.isIterable(new Map([[1, 'one'], [2, 'two']])), true);
    t.end();
});

tap.test('fn.isTypeOf()', (t) => {
    let isFunction = fn.isTypeOf('function');
    let isFunctionOrObject = fn.isTypeOf(['function', 'object']);
    t.equal(typeof(isFunction), 'function');
    t.equal(isFunction(undefined), false);
    t.equal(isFunction(null), false);
    t.equal(isFunction({}), false);
    t.equal(isFunction(isFunction), true);
    t.equal(isFunction(Array), true);
    t.equal(isFunctionOrObject({}), true);
    t.equal(isFunctionOrObject(isFunction), true);
    t.end();
});

tap.test('fn.isInstanceOf()', (t) => {
    let isArray = fn.isInstanceOf(Array);
    t.equal(typeof(isArray), 'function');
    t.equal(isArray(undefined), false);
    t.equal(isArray(null), false);
    t.equal(isArray({}), false);
    t.equal(isArray([]), true);
    t.equal(isArray(new Array(1)), true);
    t.end();
});

tap.test('fn.hasProperty()', (t) => {
    let hasLength = fn.hasProperty('length');
    t.equal(typeof(hasLength), 'function');
    t.equal(hasLength(undefined), false);
    t.equal(hasLength(null), false);
    t.equal(hasLength({}), false);
    t.equal(hasLength([]), true);
    t.equal(hasLength({ length: 42 }), true);
    t.equal(fn.hasProperty(undefined, { 'undefined': 13 }), true);
    t.equal(fn.hasProperty(undefined, { 'null': 42 }), false);
    t.end();
});

tap.test('fn.hasValue()', (t) => {
    t.equal(fn.hasValue(undefined), false);
    t.equal(fn.hasValue(null), false);
    t.equal(fn.hasValue(false), true);
    t.equal(fn.hasValue(true), true);
    t.equal(fn.hasValue(NaN), false);
    t.equal(fn.hasValue(0), true);
    t.equal(fn.hasValue(1), true);
    t.equal(fn.hasValue(0n), true);
    t.equal(fn.hasValue(''), false);
    t.equal(fn.hasValue('abc'), true);
    t.equal(fn.hasValue(/regex/), true);
    t.equal(fn.hasValue(Symbol('foo')), true);
    t.equal(fn.hasValue({}), false);
    t.equal(fn.hasValue({ a: 1 }), true);
    t.equal(fn.hasValue([]), false);
    t.equal(fn.hasValue([0, 1, 2]), true);
    t.equal(fn.hasValue(new Int8Array(0)), false);
    t.equal(fn.hasValue(new Int8Array([1, 2, 3])), true);
    t.equal(fn.hasValue(new Set([])), false);
    t.equal(fn.hasValue(new Set([1, 2, 3])), true);
    t.end();
});

tap.test('fn.eq()', (t) => {
    t.equal(fn.eq(undefined, undefined), true);
    t.equal(fn.eq(null, undefined), false);
    t.equal(fn.eq(1, 1), true);
    t.equal(fn.eq(1, 42), false);
    t.equal(fn.eq(NaN, NaN), false);
    t.equal(fn.eq(9007199254740991n, 9007199254740991n), true);
    t.equal(fn.eq('abc', 'abc'), true);
    t.equal(fn.eq('abc', 'abc', '123'), false);
    t.equal(fn.eq([], []), true);
    t.equal(fn.eq([], {}), false);
    t.equal(fn.eq([1, 2, 3], [1, 2, 3]), true);
    t.equal(fn.eq([1, 2, 3], [1, 2, 9]), false);
    t.equal(fn.eq({}, {}), true);
    t.equal(fn.eq({}, []), false);
    t.equal(fn.eq({ a: 1 }, { a: 1 }), true);
    t.equal(fn.eq({ a: 1 }, { b: 1 }), false);
    t.equal(fn.eq({ a: 1, b: 13 }, { b: 13, a: 1 }), true);
    t.equal(fn.eq({ a: [1, 2, 3], b: { c: 1337 } }, { b: { c: 1337 }, a: [1, 2, 3] }), true);
    t.equal(fn.eq({ a: [1, 2, 3], b: { c: 1337 } }, { a: [1, 2, 3], b: { c: 1337, d: 9 } }), false);
    let dt = new Date();
    t.equal(fn.eq(dt, dt), true);
    t.equal(fn.eq(new Date(), new Date()), false);
    t.equal(fn.eq(Symbol('foo'), Symbol('foo')), false);
    t.equal(fn.eq(Symbol.for('foo'), Symbol.for('foo')), true);
    let is13 = fn.eq(13);
    t.equal(typeof(is13), 'function');
    t.equal(is13(12), false);
    t.equal(is13(13), true);
    t.equal(is13(13, 13, 11), false);
    let getFolderId = (o) => o.folder.id;
    let isValidFolderId = fn.eq(getFolderId, 13);
    t.equal(typeof(isValidFolderId), 'function');
    t.equal(isValidFolderId(), false);
    t.equal(isValidFolderId({ folder: { id: 1 } }), false);
    t.equal(isValidFolderId({ folder: { id: 13 } }), true);
    t.equal(isValidFolderId(123), false);
    t.end();
});
