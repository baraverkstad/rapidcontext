import test from 'node:test';
import assert from 'node:assert';
import data from 'rapidcontext/data.mjs';

function arrayLike() {
    return arguments;
}

function throwError() {
    throw new Error('test');
}

test('data.bool()', () => {
    assert.strictEqual(data.bool(), false);
    assert.strictEqual(data.bool(null), false);
    assert.strictEqual(data.bool(true), true);
    assert.strictEqual(data.bool(false), false);
    assert.strictEqual(data.bool(NaN), false);
    assert.strictEqual(data.bool(0), false);
    assert.strictEqual(data.bool(1), true);
    assert.strictEqual(data.bool(''), false);
    assert.strictEqual(data.bool('on'), true);
    assert.strictEqual(data.bool('OFF'), false);
    assert.strictEqual(data.bool('FaLsE'), false);
    assert.strictEqual(data.bool('null'), false);
    assert.strictEqual(data.bool('falsy'), true);
    assert.strictEqual(data.bool({}), true);
});

test('data.array()', () => {
    assert.deepEqual(data.array(), []);
    assert.deepEqual(data.array(null), []);
    assert.deepEqual(data.array(undefined), []);
    assert.deepEqual(data.array(null, undefined), [null, undefined]);
    assert.deepEqual(data.array(false), [false]);
    assert.deepEqual(data.array(0), [0]);
    assert.deepEqual(data.array(1, 'two', false), [1, 'two', false]);
    assert.deepEqual(data.array([]), []);
    assert.deepEqual(data.array([], []), [[], []]);
    assert.deepEqual(data.array([1, 'two', false]), [1, 'two', false]);
    assert.deepEqual(data.array({}), []);
    assert.deepEqual(data.array({}, {}), [{}, {}]);
    assert.deepEqual(data.array({ a: 1, b: 'two', c: false }), [1, 'two', false]);
    assert.deepEqual(data.array(new Set([1, 2, 3])), [1, 2, 3]);
    const arr = [1, 2, 3];
    assert.deepEqual(data.array(arr), arr, 'input array is copied');
    assert.notStrictEqual(data.array(arr), arr, 'input array is copied');
});

test('data.object()', () => {
    const dt = new Date();
    assert.deepEqual(data.object(), {});
    assert.deepEqual(data.object(null, 1), {});
    assert.deepEqual(data.object(undefined, 1), {});
    assert.deepEqual(data.object('a'), {});
    assert.deepEqual(data.object((o) => o.id), {});
    assert.deepEqual(data.object([]), {});
    assert.deepEqual(data.object([['a', 1], ['b', 2], ['c']]), { a: 1, b: 2, c: undefined });
    assert.deepEqual(data.object(['a', null, ['b']]), { 'a': undefined, 'b': undefined });
    assert.deepEqual(data.object(['a', 'a', 'a']), { a: undefined });
    assert.deepEqual(data.object(['a', 'b'], true), { a: true, b: true });
    assert.deepEqual(data.object(['a', 'b'], dt), { a: dt, b: dt });
    assert.deepEqual(data.object(['a', 'b'], [1, 2]), { a: 1, b: 2 });
    assert.deepEqual(data.object([1, null, 3, 1], [1, 2, 3, 4]), { '1': 1, '3': 3 });
    assert.deepEqual(data.object(['a', 'b', 'c'], [1, 2]), { a: 1, b: 2, c: undefined });
    assert.deepEqual(data.object(['a', 'a', 'a'], [1, 2, 3]), { a: 1 });
    assert.deepEqual(data.object(['a', 'b'], { a: 1, b: 2, c: 3, d: 4 }), { a: 1, b: 2 });
    assert.deepEqual(data.object(['a', 'b'], (k) => k), { a: 'a', b: 'b' });
    assert.deepEqual(data.object(['a', 'b'], throwError), { a: undefined, b: undefined });
    assert.deepEqual(data.object({}), {});
    assert.deepEqual(data.object({ a: 1, b: 2 }), { a: 1, b: 2 });
    assert.deepEqual(data.object({ a: 1, b: 2 }, true), { a: true, b: true });
    assert.deepEqual(data.object({ a: 1, b: 2 }, dt), { a: dt, b: dt });
    assert.deepEqual(data.object({ a: 1, b: 2 }, [0]), { a: 0, b: undefined });
    assert.deepEqual(data.object({ a: 1, b: 2 }, [4, 5, 6]), { a: 4, b: 5 });
    assert.deepEqual(data.object({ a: 1, b: 2 }, { a: 31, b: 32, c: 33, d: 34 }), { a: 31, b: 32 });
    assert.deepEqual(data.object({ a: 1, b: 2 }, (k) => k), { a: 'a', b: 'b' });
    const o1 = { k: 1, a: 1 };
    const o2 = { k: 'a', a: 2 };
    const o3 = { k: 'a', b: 3 };
    const o4 = { a: 4, b: 4 };
    assert.deepEqual(data.object(o1), o1, 'input object is copied');
    assert.notStrictEqual(data.object(o1), o1, 'input object is copied');
    assert.deepEqual(data.object('k', [o1, o2, o3, o4]), { '1': o1, 'a': o2 });
    assert.deepEqual(data.object((o) => o.k, [o1, o2, o3, o4]), { '1': o1, 'a': o2 });
});

test('data.clone()', () => {
    assert.strictEqual(data.clone(), undefined);
    assert.strictEqual(data.clone(null), null);
    assert.strictEqual(data.clone(1), 1);
    assert.strictEqual(data.clone(false), false);
    assert.strictEqual(data.clone('str'), 'str');
    const o = { a: 1, b: ['z', 'x', 'y'], c: { sub: false } };
    const res = data.clone(o);
    assert.deepEqual(res, o);
    assert.notStrictEqual(res, o);
    assert.notStrictEqual(res.b, o.b);
    assert.notStrictEqual(res.c, o.c);
    const dt = new Date();
    assert.strictEqual(data.clone(dt), dt);
    const re = /test/;
    assert.strictEqual(data.clone(re), re);
});

test('data.get()', () => {
    assert.strictEqual(data.get('', null), undefined);
    assert.strictEqual(data.get('a', null), undefined);
    assert.strictEqual(data.get('a', {}), undefined);
    assert.strictEqual(data.get('a', []), undefined);
    assert.strictEqual(data.get('a', { a: 13 }), 13);
    assert.strictEqual(data.get('a', { a: null }), null);
    assert.strictEqual(data.get(1, { '0': 'a', '1': 'b' }), 'b');
    assert.strictEqual(data.get(1, ['a', 'b']), 'b');
    assert.strictEqual(data.get('[1]', ['a', 'b']), 'b');
    assert.strictEqual(data.get('[1]', ['a']), undefined);
    assert.strictEqual(data.get('a.b', { a: {} }), undefined);
    assert.strictEqual(data.get('a.b', { a: { b: 42 } }), 42);
    assert.strictEqual(data.get('a.1', { a: [13, 4711] }), 4711);
    assert.strictEqual(data.get('a[1]', { a: [13, 4711] }), 4711);
    assert.strictEqual(data.get(['a', 'b'], { a: 13, c: 17 }), undefined);
    assert.strictEqual(data.get(['a', 'b'], { a: { b: 17 } }), 17);
    assert.strictEqual(data.get('a', []), undefined);
    assert.strictEqual(data.get('a', [[{ a: 13 }]]), undefined);
    assert.deepEqual(data.get('[].a', [null, { a: 13 }, { b: 42 }, { a: undefined }]), [13, undefined]);
    assert.deepEqual(data.get('a[].b', { a: [{ b: 42 }, { b: 19 }] }), [42, 19]);
    assert.deepEqual(data.get(['[]', 'b'], [null, { a: 13 }, { b: 42 }]), [42]);
    assert.deepEqual(data.get('*', { a: 1, b: null, c: undefined }), [1, null, undefined]);
    assert.deepEqual(data.get('*.a', { a: { b: 17 }, c: { a: 33 } }), [33]);
    assert.deepEqual(data.get('a.*', { a: { b: 17, c: 33 } }), [17, 33]);
    let getFolderId = data.get('folder.id');
    assert.strictEqual(typeof(getFolderId), 'function');
    assert.strictEqual(getFolderId(null), undefined);
    assert.strictEqual(getFolderId({ folder: 42 }), undefined);
    assert.strictEqual(getFolderId({ folder: { id: 42 } }), 42);
});

test('data.filter()', () => {
    assert.deepEqual(data.filter(Boolean, null), []);
    assert.deepEqual(data.filter(Boolean, []), []);
    assert.deepEqual(data.filter(Boolean, {}), {});
    assert.deepEqual(data.filter(Boolean, [null, undefined, true, 1, 0, '', {}, []]), [true, 1, {}, []]);
    assert.deepEqual(data.filter(Boolean, { a: null, b: true, c: 0 }), { b: true });
    assert.deepEqual(data.filter(Boolean, arrayLike(0, 1, true, 'test')), [1, true, 'test']);
    assert.deepEqual(data.filter(throwError, [1, 2, 3]), []);
    let isValid = data.filter('folder.id');
    assert.strictEqual(typeof(isValid), 'function');
    assert.deepEqual(isValid(null), []);
    assert.deepEqual(isValid([{ folder: 42 }, {}, null]), []);
    assert.deepEqual(isValid([{}, { folder: { id: 42 } }]), [{ folder: { id: 42 } }]);
});

test('data.flatten()', () => {
    assert.deepEqual(data.flatten(), []);
    assert.deepEqual(data.flatten(null), [null]);
    assert.deepEqual(data.flatten(undefined), [undefined]);
    assert.deepEqual(data.flatten(1), [1]);
    assert.deepEqual(data.flatten(1, [2, 3]), [1, 2, 3]);
    assert.deepEqual(data.flatten([]), []);
    assert.deepEqual(data.flatten([1, [2, 3], [1, 2, [3]]]), [1, 2, 3, 1, 2, [3]]);
});

test('data.map()', () => {
    assert.deepEqual(data.map(Boolean, null), []);
    assert.deepEqual(data.map(Boolean, []), []);
    assert.deepEqual(data.map(Boolean, {}), {});
    assert.deepEqual(data.map(Boolean, [null, true, 1, 0, '']), [false, true, true, false, false]);
    assert.deepEqual(data.map(Boolean, { a: null, b: true, c: 0 }), { a: false, b: true, c: false });
    assert.deepEqual(data.map(Boolean, arrayLike(null, 'test')), [false, true]);
    assert.deepEqual(data.map(throwError, [1, 2, 3]), [undefined, undefined, undefined]);
    let getFolderIds = data.map('folder.id');
    assert.strictEqual(typeof(getFolderIds), 'function');
    assert.deepEqual(getFolderIds(null), []);
    assert.deepEqual(getFolderIds([{ folder: { id: 42 } }, {}, null]), [42, undefined, undefined]);
    assert.deepEqual(
        getFolderIds({ a: { folder: { id: 42 } }, b: {}, c: null }),
        { a: 42, b: undefined, c: undefined }
    );
});

test('data.uniq()', () => {
    assert.deepEqual(data.uniq(), []);
    assert.deepEqual(data.uniq(null), []);
    assert.deepEqual(data.uniq([1, 2, 3, 3, 2, 1]), [1, 2, 3]);
    assert.deepEqual(data.uniq([{}, {}, null, null, undefined, 'test', 'test']), [{}, null, undefined, 'test']);
    assert.deepEqual(data.uniq([{ a: 1, b: 2 }, { a: 1, b: 2 }, { b: 2, a: 1 }]), [{ a: 1, b: 2 }, { b: 2, a: 1 }]);
    assert.deepEqual(data.uniq({ a: 1, b: 2, c: 1 }), [1, 2]);
    assert.deepEqual(data.uniq(String, [{}, { a: 1, b: 2 }, { a: 1, b: 2 }]), [{}]);
    assert.deepEqual(data.uniq((o) => o.a, [{ a: 1, b: 2 }, { a: 1, b: 2 }, { b: 2, a: 1 }]), [{ a: 1, b: 2 }]);
    assert.deepEqual(data.uniq(Boolean, [null, undefined, 0, 'test']), [null, 'test']);
    const uniq = data.uniq(Boolean);
    assert.strictEqual(typeof(uniq), 'function');
    assert.deepEqual(uniq([null, undefined, 0, 'test']), [null, 'test']);
});

test('data.compare()', () => {
    assert.strictEqual(data.compare(null, undefined), 0);
    assert.strictEqual(data.compare(null, NaN), 0);
    assert.strictEqual(data.compare(undefined, null), 0);
    assert.strictEqual(data.compare(undefined, NaN), 0);
    assert.strictEqual(data.compare(1, 1), 0);
    assert.strictEqual(data.compare(1, 42), -1);
    assert.strictEqual(data.compare(1, NaN), 1);
    assert.strictEqual(data.compare(null, 1), -1);
    assert.strictEqual(data.compare('', 'abc'), -1);
    assert.strictEqual(data.compare('abc', 'abc'), 0);
    assert.strictEqual(data.compare('abc', 'cde'), -1);
    assert.strictEqual(data.compare('cde', 'abc'), 1);
    let toLower = (o) => String.prototype.toLowerCase.call(String(o));
    let cmpIgnoreCase = data.compare(toLower);
    assert.strictEqual(typeof(cmpIgnoreCase), 'function');
    assert.strictEqual(cmpIgnoreCase('AbC', 'abc'), 0);
    assert.strictEqual(cmpIgnoreCase('DEF', 'abc'), 1);
    assert.strictEqual(cmpIgnoreCase(null, 'abc'), 1);
    assert.strictEqual(cmpIgnoreCase(13, 'abc'), -1);
    assert.strictEqual(cmpIgnoreCase(123, 123), 0);
    let getId = (o) => o.id;
    assert.throws(data.compare.bind(null, getId, { id: 13 }, undefined));
});

test('data.sort()', () => {
    assert.deepEqual(data.sort(), []);
    assert.deepEqual(data.sort([]), []);
    assert.deepEqual(data.sort([3, 2, 1, 11]), [1, 2, 3, 11]);
    assert.deepEqual(data.sort(['b', 'B', 'a', 'aa', 'A']), ['A', 'B', 'a', 'aa', 'b']);
    assert.deepEqual(data.sort({}), []);
    assert.deepEqual(data.sort({ a: 3, b: 2, c: 1, d: 11 }), [1, 2, 3, 11]);
    const toLower = (s) => s.toLowerCase();
    assert.deepEqual(data.sort(toLower, ['b', 'B', 'a', 'aa', 'A']), ['a', 'A', 'aa', 'b', 'B']);
    const sort = data.sort(toLower);
    assert.strictEqual(typeof(sort), 'function');
    assert.deepEqual(sort(['C', 'B', 'a']), ['a', 'B', 'C']);
});
