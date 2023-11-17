import test from 'node:test';
import assert from 'node:assert';
import text from 'rapidcontext/text.mjs';

test('text.str()', () => {
    assert.strictEqual(text.str(), '');
    assert.strictEqual(text.str(null), '');
    assert.strictEqual(text.str(undefined), '');
    assert.strictEqual(text.str(''), '');
    assert.strictEqual(text.str('abc'), 'abc');
    assert.strictEqual(text.str(123), '123');
    assert.strictEqual(text.str(false), 'false');
    assert.strictEqual(text.str([1, 2, 3]), '1,2,3');
});

test('text.lower()', () => {
    assert.strictEqual(text.lower(), '');
    assert.strictEqual(text.lower('ABC'), 'abc');
    assert.strictEqual(text.lower('AbC'), 'abc');
});

test('text.upper()', () => {
    assert.strictEqual(text.upper(), '');
    assert.strictEqual(text.upper('abc'), 'ABC');
    assert.strictEqual(text.upper('Abc'), 'ABC');
});

test('text.lowerFirst()', () => {
    assert.strictEqual(text.lowerFirst(), '');
    assert.strictEqual(text.lowerFirst('ABC'), 'aBC');
    assert.strictEqual(text.lowerFirst('Abc'), 'abc');
});

test('text.upperFirst()', () => {
    assert.strictEqual(text.upperFirst(), '');
    assert.strictEqual(text.upperFirst('abc'), 'Abc');
    assert.strictEqual(text.upperFirst('ABC'), 'ABC');
});

test('text.capitalize()', () => {
    assert.strictEqual(text.capitalize(), '');
    assert.strictEqual(text.capitalize('abc'), 'Abc');
    assert.strictEqual(text.capitalize('ABC'), 'Abc');
    assert.strictEqual(text.capitalize('aBC'), 'Abc');
});

test('text.words()', () => {
    assert.deepEqual(text.words(), []);
    assert.deepEqual(text.words(null), []);
    assert.deepEqual(text.words(''), []);
    assert.deepEqual(text.words('   '), []);
    assert.deepEqual(text.words('hello'), ['hello']);
    assert.deepEqual(text.words('  hello  '), ['hello']);
    assert.deepEqual(text.words('hello world'), ['hello', 'world']);
    assert.deepEqual(text.words('helloWorld'), ['hello', 'World']);
    assert.deepEqual(text.words('hello-world'), ['hello', 'world']);
    assert.deepEqual(text.words('hello_world'), ['hello', 'world']);
    assert.deepEqual(text.words('hello.world'), ['hello', 'world']);
    assert.deepEqual(text.words('lowerToUpper'), ['lower', 'To', 'Upper']);
    assert.deepEqual(text.words('MyClass'), ['My', 'Class']);
    assert.deepEqual(text.words('XMLHttpRequest'), ['XML', 'Http', 'Request']);
    assert.deepEqual(text.words('snake_case_mixedWithCamel'), ['snake', 'case', 'mixed', 'With', 'Camel']);
});

test('text.lowerCase()', () => {
    assert.strictEqual(text.lowerCase('Hello World'), 'hello world');
    assert.strictEqual(text.lowerCase('foo bar'), 'foo bar');
    assert.strictEqual(text.lowerCase('--foo-bar--'), 'foo bar');
    assert.strictEqual(text.lowerCase('__foo_bar__'), 'foo bar');
    assert.strictEqual(text.lowerCase('__FOO_BAR__'), 'foo bar');
});

test('text.upperCase()', () => {
    assert.strictEqual(text.upperCase('Hello World'), 'HELLO WORLD');
    assert.strictEqual(text.upperCase('foo bar'), 'FOO BAR');
    assert.strictEqual(text.upperCase('--foo-bar--'), 'FOO BAR');
    assert.strictEqual(text.upperCase('__foo_bar__'), 'FOO BAR');
});

test('text.camelCase()', () => {
    assert.strictEqual(text.camelCase('Hello World'), 'helloWorld');
    assert.strictEqual(text.camelCase('XMLHttpRequest'), 'xmlHttpRequest');
    assert.strictEqual(text.camelCase('Foo Bar'), 'fooBar');
    assert.strictEqual(text.camelCase('--foo-bar--'), 'fooBar');
    assert.strictEqual(text.camelCase('__FOO_BAR__'), 'fooBar');
});

test('text.kebabCase()', () => {
    assert.strictEqual(text.kebabCase('Hello World'), 'hello-world');
    assert.strictEqual(text.kebabCase('foo bar'), 'foo-bar');
    assert.strictEqual(text.kebabCase('--foo-bar--'), 'foo-bar');
    assert.strictEqual(text.kebabCase('__foo_bar__'), 'foo-bar');
    assert.strictEqual(text.kebabCase('__FOO_BAR__'), 'foo-bar');
});

test('text.snakeCase()', () => {
    assert.strictEqual(text.snakeCase('Hello World'), 'hello_world');
    assert.strictEqual(text.snakeCase('foo bar'), 'foo_bar');
    assert.strictEqual(text.snakeCase('--foo-bar--'), 'foo_bar');
    assert.strictEqual(text.snakeCase('__foo_bar__'), 'foo_bar');
    assert.strictEqual(text.snakeCase('__FOO_BAR__'), 'foo_bar');
});

test('text.startCase()', () => {
    assert.strictEqual(text.startCase('helloWorld'), 'Hello World');
    assert.strictEqual(text.startCase('foo bar'), 'Foo Bar');
    assert.strictEqual(text.startCase('--foo-bar--'), 'Foo Bar');
    assert.strictEqual(text.startCase('__foo_bar__'), 'Foo Bar');
    assert.strictEqual(text.startCase('__FOO_BAR__'), 'Foo Bar');
});

test('text.escape()', () => {
    assert.strictEqual(text.escape(), '');
    assert.strictEqual(text.escape(null), '');
    assert.strictEqual(text.escape(''), '');
    assert.strictEqual(text.escape('abc'), 'abc');
    assert.strictEqual(text.escape('<>&"\''), '&lt;&gt;&amp;&quot;&apos;');
    assert.strictEqual(text.escape(['<', '>']), '&lt;&gt;');
    assert.strictEqual(text.escape('foo & bar'), 'foo &amp; bar');
});

test('text.unescape()', () => {
    assert.strictEqual(text.unescape(), '');
    assert.strictEqual(text.unescape(null), '');
    assert.strictEqual(text.unescape(''), '');
    assert.strictEqual(text.unescape('abc'), 'abc');
    assert.strictEqual(text.unescape('&lt;&gt;&amp;&quot;&apos;'), '<>&"\'');
    assert.strictEqual(text.unescape('foo &amp; bar'), 'foo & bar');
    assert.strictEqual(text.unescape('&#39;'), '\'');
    assert.strictEqual(text.unescape('&#38;'), '&');
    assert.strictEqual(text.unescape('&#x26;'), '&');
    assert.strictEqual(text.unescape('&#x2F;'), '/');
});
