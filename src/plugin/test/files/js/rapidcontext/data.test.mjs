import tap from 'tap';
import data from '../../../../system/files/js/rapidcontext/data.mjs';

function arrayLike() {
    return arguments;
}

function throwError() {
    throw new Error('test');
}

tap.test('data.bool()', (t) => {
    t.equal(data.bool(), false);
    t.equal(data.bool(null), false);
    t.equal(data.bool(true), true);
    t.equal(data.bool(false), false);
    t.equal(data.bool(NaN), false);
    t.equal(data.bool(0), false);
    t.equal(data.bool(1), true);
    t.equal(data.bool(''), false);
    t.equal(data.bool('on'), true);
    t.equal(data.bool('OFF'), false);
    t.equal(data.bool('FaLsE'), false);
    t.equal(data.bool('null'), false);
    t.equal(data.bool('falsy'), true);
    t.equal(data.bool({}), true);
    t.end();
});

tap.test('data.array()', (t) => {
    t.same(data.array(), []);
    t.same(data.array(null), []);
    t.same(data.array(undefined), []);
    t.same(data.array(null, undefined), [null, undefined]);
    t.same(data.array(false), [false]);
    t.same(data.array(0), [0]);
    t.same(data.array(1, 'two', false), [1, 'two', false]);
    t.same(data.array([]), []);
    t.same(data.array([], []), [[], []]);
    t.same(data.array([1, 'two', false]), [1, 'two', false]);
    t.same(data.array({}), []);
    t.same(data.array({}, {}), [{}, {}]);
    t.same(data.array({ a: 1, b: 'two', c: false }), [1, 'two', false]);
    t.same(data.array(new Set([1, 2, 3])), [1, 2, 3]);
    const arr = [1, 2, 3];
    t.not(data.array(arr), arr, 'input array is copied');
    t.end();
});

tap.test('data.object()', (t) => {
    const dt = new Date();
    t.same(data.object(), {});
    t.same(data.object(null, 1), {});
    t.same(data.object(undefined, 1), {});
    t.same(data.object('a'), {});
    t.same(data.object((o) => o.id), {});
    t.same(data.object([]), {});
    t.same(data.object([['a', 1], ['b', 2], ['c']]), { a: 1, b: 2, c: undefined });
    t.same(data.object(['a', null, ['b']]), { 'a': undefined, 'b': undefined });
    t.same(data.object(['a', 'a', 'a']), { a: undefined });
    t.same(data.object(['a', 'b'], true), { a: true, b: true });
    t.same(data.object(['a', 'b'], dt), { a: dt, b: dt });
    t.same(data.object(['a', 'b'], [1, 2]), { a: 1, b: 2 });
    t.same(data.object([1, null, 3, 1], [1, 2, 3, 4]), { '1': 1, '3': 3 });
    t.same(data.object(['a', 'b', 'c'], [1, 2]), { a: 1, b: 2, c: undefined });
    t.same(data.object(['a', 'a', 'a'], [1, 2, 3]), { a: 1 });
    t.same(data.object(['a', 'b'], { a: 1, b: 2, c: 3, d: 4 }), { a: 1, b: 2 });
    t.same(data.object(['a', 'b'], (k) => k), { a: 'a', b: 'b' });
    t.same(data.object(['a', 'b'], throwError), { a: undefined, b: undefined });
    t.same(data.object({}), {});
    t.same(data.object({ a: 1, b: 2 }), { a: 1, b: 2 });
    t.same(data.object({ a: 1, b: 2 }, true), { a: true, b: true });
    t.same(data.object({ a: 1, b: 2 }, dt), { a: dt, b: dt });
    t.same(data.object({ a: 1, b: 2 }, [0]), { a: 0, b: undefined });
    t.same(data.object({ a: 1, b: 2 }, [4, 5, 6]), { a: 4, b: 5 });
    t.same(data.object({ a: 1, b: 2 }, { a: 31, b: 32, c: 33, d: 34 }), { a: 31, b: 32 });
    t.same(data.object({ a: 1, b: 2 }, (k) => k), { a: 'a', b: 'b' });
    const o1 = { k: 1, a: 1 };
    const o2 = { k: 'a', a: 2 };
    const o3 = { k: 'a', b: 3 };
    const o4 = { a: 4, b: 4 };
    t.not(data.object(o1), o1, 'input object is copied');
    t.same(data.object('k', [o1, o2, o3, o4]), { '1': o1, 'a': o2 });
    t.same(data.object((o) => o.k, [o1, o2, o3, o4]), { '1': o1, 'a': o2 });
    t.end();
});

tap.test('data.clone()', (t) => {
    t.same(data.clone(), undefined);
    t.same(data.clone(null), null);
    t.same(data.clone(1), 1);
    t.same(data.clone(false), false);
    t.same(data.clone('str'), 'str');
    const o = { a: 1, b: ['z', 'x', 'y'], c: { sub: false } };
    const res = data.clone(o);
    t.same(res, o);
    t.not(res, o);
    t.not(res.b, o.b);
    t.not(res.c, o.c);
    const dt = new Date();
    t.equal(data.clone(dt), dt);
    const re = /test/;
    t.equal(data.clone(re), re);
    t.end();
});

tap.test('data.get()', (t) => {
    t.equal(data.get('', null), undefined);
    t.equal(data.get('a', null), undefined);
    t.equal(data.get('a', {}), undefined);
    t.equal(data.get('a', []), undefined);
    t.equal(data.get('a', { a: 13 }), 13);
    t.equal(data.get('a', { a: null }), null);
    t.equal(data.get(1, { '0': 'a', '1': 'b' }), 'b');
    t.equal(data.get(1, ['a', 'b']), 'b');
    t.equal(data.get('[1]', ['a', 'b']), 'b');
    t.equal(data.get('[1]', ['a']), undefined);
    t.equal(data.get('a.b', { a: {} }), undefined);
    t.equal(data.get('a.b', { a: { b: 42 } }), 42);
    t.equal(data.get('a.1', { a: [13, 4711] }), 4711);
    t.equal(data.get('a[1]', { a: [13, 4711] }), 4711);
    t.same(data.get(['a', 'b'], { a: 13, c: 17 }), undefined);
    t.same(data.get(['a', 'b'], { a: { b: 17 } }), 17);
    t.same(data.get('a', []), undefined);
    t.same(data.get('a', [[{ a: 13 }]]), undefined);
    t.same(data.get('[].a', [null, { a: 13 }, { b: 42 }, { a: undefined }]), [13, undefined]);
    t.same(data.get('a[].b', { a: [{ b: 42 }, { b: 19 }] }), [42, 19]);
    t.same(data.get(['[]', 'b'], [null, { a: 13 }, { b: 42 }]), [42]);
    t.same(data.get('*', { a: 1, b: null, c: undefined }), [1, null, undefined]);
    t.same(data.get('*.a', { a: { b: 17 }, c: { a: 33 } }), [33]);
    t.same(data.get('a.*', { a: { b: 17, c: 33 } }), [17, 33]);
    let getFolderId = data.get('folder.id');
    t.equal(typeof(getFolderId), 'function');
    t.equal(getFolderId(null), undefined);
    t.equal(getFolderId({ folder: 42 }), undefined);
    t.equal(getFolderId({ folder: { id: 42 } }), 42);
    t.end();
});

tap.test('data.filter()', (t) => {
    t.same(data.filter(Boolean, null), []);
    t.same(data.filter(Boolean, []), []);
    t.same(data.filter(Boolean, {}), {});
    t.same(data.filter(Boolean, [null, undefined, true, 1, 0, '', {}, []]), [true, 1, {}, []]);
    t.same(data.filter(Boolean, { a: null, b: true, c: 0 }), { b: true });
    t.same(data.filter(Boolean, arrayLike(0, 1, true, 'test')), [1, true, 'test']);
    t.same(data.filter(throwError, [1, 2, 3]), []);
    let isValid = data.filter('folder.id');
    t.equal(typeof(isValid), 'function');
    t.same(isValid(null), []);
    t.same(isValid([{ folder: 42 }, {}, null]), []);
    t.same(isValid([{}, { folder: { id: 42 } }]), [{ folder: { id: 42 } }]);
    t.end();
});

tap.test('data.flatten()', (t) => {
    t.same(data.flatten(), []);
    t.same(data.flatten(null), [null]);
    t.same(data.flatten(undefined), [undefined]);
    t.same(data.flatten(1), [1]);
    t.same(data.flatten(1, [2, 3]), [1, 2, 3]);
    t.same(data.flatten([]), []);
    t.same(data.flatten([1, [2, 3], [1, 2, [3]]]), [1, 2, 3, 1, 2, [3]]);
    t.end();
});

tap.test('data.map()', (t) => {
    t.same(data.map(Boolean, null), []);
    t.same(data.map(Boolean, []), []);
    t.same(data.map(Boolean, {}), {});
    t.same(data.map(Boolean, [null, true, 1, 0, '']), [false, true, true, false, false]);
    t.same(data.map(Boolean, { a: null, b: true, c: 0 }), { a: false, b: true, c: false });
    t.same(data.map(Boolean, arrayLike(null, 'test')), [false, true]);
    t.same(data.map(throwError, [1, 2, 3]), [undefined, undefined, undefined]);
    let getFolderIds = data.map('folder.id');
    t.equal(typeof(getFolderIds), 'function');
    t.same(getFolderIds(null), []);
    t.same(getFolderIds([{ folder: { id: 42 } }, {}, null]), [42, undefined, undefined]);
    t.same(getFolderIds({ a: { folder: { id: 42 } }, b: {}, c: null }), { a: 42, b: undefined, c: undefined });
    t.end();
});

tap.test('data.uniq()', (t) => {
    t.same(data.uniq(), []);
    t.same(data.uniq(null), []);
    t.same(data.uniq([1, 2, 3, 3, 2, 1]), [1, 2, 3]);
    t.same(data.uniq([{}, {}, null, null, undefined, 'test', 'test']), [{}, null, undefined, 'test']);
    t.same(data.uniq([{ a: 1, b: 2 }, { a: 1, b: 2 }, { b: 2, a: 1 }]), [{ a: 1, b: 2 }, { b: 2, a: 1 }]);
    t.same(data.uniq({ a: 1, b: 2, c: 1 }), [1, 2]);
    t.same(data.uniq(String, [{}, { a: 1, b: 2 }, { a: 1, b: 2 }]), [{}]);
    t.same(data.uniq((o) => o.a, [{ a: 1, b: 2 }, { a: 1, b: 2 }, { b: 2, a: 1 }]), [{ a: 1, b: 2 }]);
    t.same(data.uniq(Boolean, [null, undefined, 0, 'test']), [null, 'test']);
    const uniq = data.uniq(Boolean);
    t.equal(typeof(uniq), 'function');
    t.same(uniq([null, undefined, 0, 'test']), [null, 'test']);
    t.end();
});

tap.test('data.compare()', (t) => {
    t.equal(data.compare(null, undefined), 0);
    t.equal(data.compare(undefined, null), 0);
    t.equal(data.compare(1, 1), 0);
    t.equal(data.compare(1, 42), -1);
    t.equal(data.compare(1, NaN), 0);
    t.equal(data.compare('abc', 'abc'), 0);
    t.equal(data.compare('abc', 'cde'), -1);
    t.equal(data.compare('cde', 'abc'), 1);
    let toLower = (o) => String.prototype.toLowerCase.call('' + o);
    let cmpIgnoreCase = data.compare(toLower);
    t.equal(typeof(cmpIgnoreCase), 'function');
    t.equal(cmpIgnoreCase('AbC', 'abc'), 0);
    t.equal(cmpIgnoreCase('DEF', 'abc'), 1);
    t.equal(cmpIgnoreCase(null, 'abc'), 1);
    t.equal(cmpIgnoreCase(13, 'abc'), -1);
    t.equal(cmpIgnoreCase(123, 123), 0);
    let getId = (o) => o.id;
    t.throws(data.compare.bind(null, getId, { id: 13 }, undefined));
    t.end();
});

tap.test('data.sort()', (t) => {
    t.same(data.sort(), []);
    t.same(data.sort([]), []);
    t.same(data.sort([3, 2, 1, 11]), [1, 2, 3, 11]);
    t.same(data.sort(['b', 'B', 'a', 'aa', 'A']), ['A', 'B', 'a', 'aa', 'b']);
    t.same(data.sort({}), []);
    t.same(data.sort({ a: 3, b: 2, c: 1, d: 11 }), [1, 2, 3, 11]);
    const toLower = (s) => s.toLowerCase();
    t.same(data.sort(toLower, ['b', 'B', 'a', 'aa', 'A']), ['a', 'A', 'aa', 'b', 'B']);
    const sort = data.sort(toLower);
    t.equal(typeof(sort), 'function');
    t.same(sort(['C', 'B', 'a']), ['a', 'B', 'C']);
    t.end();
});
