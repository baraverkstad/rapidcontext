/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2026 Per Cederberg. All rights reserved.
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

package org.rapidcontext.app;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.util.Date;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rapidcontext.app.model.RequestContext;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.JsonSerializer;
import org.rapidcontext.core.proc.CallContext;

@SuppressWarnings("javadoc")
public class JsProcedureIntegrationTest {

    private RequestContext cx;

    @BeforeClass
    public static void setup() throws Exception {
        String dir = Objects.requireNonNullElse(System.getenv("LOCAL_DIR"), "tmp/integration");
        ApplicationContext.init(new File("."), new File(dir), true);
    }

    @AfterClass
    public static void teardown() throws Exception {
        ApplicationContext.destroy();
    }

    @Before
    public void setupTest() throws Exception {
        cx = RequestContext.initLocal("test-admin");
    }

    @After
    public void teardownTest() throws Exception {
        if (cx != null) {
            cx.close();
        }
    }

    @Test
    public void testPrimitives() throws Exception {
        Dict res = (Dict) CallContext.execute("test/javascript/primitive", (Object) null);
        assertType(res.getDict("type"), "null [Null]", false, false);
        assertEquals(null, res.get("data"));
        assertEquals("null", res.get("str"));
        assertEquals("null", res.get("json"));

        res = (Dict) CallContext.execute("test/javascript/primitive", "Hello \uD83C\uDF0D!");
        assertType(res.getDict("type"), "string [String]", false, false, "String.prototype");
        assertEquals("Hello \uD83C\uDF0D!", res.get("data"));
        assertEquals("Hello \uD83C\uDF0D!", res.get("str"));
        assertEquals("\"Hello \uD83C\uDF0D!\"", res.get("json"));

        res = (Dict) CallContext.execute("test/javascript/primitive", 42);
        assertType(res.getDict("type"), "number [Number]", false, false, "Number.prototype");
        assertEquals(42, res.get("data"));
        assertEquals("42", res.get("str"));
        assertEquals("42", res.get("json"));

        res = (Dict) CallContext.execute("test/javascript/primitive", false);
        assertType(res.getDict("type"), "boolean [Boolean]", false, false, "Boolean.prototype");
        assertEquals(false, res.get("data"));
        assertEquals("false", res.get("str"));
        assertEquals("false", res.get("json"));
    }

    @Test
    public void testDict() throws Exception {
        Dict input = new Dict()
            .set("null", null)
            .set("name", "Test Name")
            .set("count", 42)
            .set("enabled", true)
            .set("obj", new Dict().set("deep", "value"))
            .set("arr", Array.of(0, "", false, null));
        Dict res = (Dict) CallContext.execute("test/javascript/dict", input);
        assertDictWrapper("dict", input, res);
        res = (Dict) CallContext.execute("system/procedure/call", "test/javascript/dict", Array.of(input));
        assertDictWrapper("pass-through dict", input, res);
        res = (Dict) CallContext.execute("test/javascript/helper/call", "test/javascript/dict", Array.of(input));
        assertDictWrapper("re-wrapped dict", input, res);
    }

    // Validate conformity of dict wrapper metadata
    private void assertDictWrapper(String context, Dict input, Dict res) throws Exception {
        assertType(res.getDict("type"), "object [DictWrapper]", true, false, "Object.prototype");
        assertEquals(context + " size", input.size(), res.get("size"));
        assertEquals(context + " data", input, res.get("data"));
        assertEquals(context + " str", asString(input), res.get("str"));
        String json = res.get("json", String.class);
        if (!input.equals(JsonSerializer.unserialize(json))) {
            assertEquals(context + " json", JsonSerializer.serialize(input, false), json);
        }
        assertValidProps(context + " keys", res.getDict("keys"));
        assertValidProps(context + " ownProperties", res.getDict("ownProperties"));
        assertValidProps(context + " prototypeProperties", res.getDict("prototypeProperties"));
    }

    @Test
    public void testArray() throws Exception {
        Array input = Array.of(
            null,
            "string",
            42,
            true,
            new Dict().set("key", "value"),
            Array.of(1, 2, 3)
        );
        Dict res = (Dict) CallContext.execute("test/javascript/array", input);
        assertArrayWrapper("array", input, res);
        res = (Dict) CallContext.execute("system/procedure/call", "test/javascript/array", Array.of(input));
        assertArrayWrapper("pass-through array", input, res);
        res = (Dict) CallContext.execute("test/javascript/helper/call", "test/javascript/array", Array.of(input));
        assertArrayWrapper("re-wrapped array", input, res);
    }

    // Validate conformity of array wrapper metadata
    private void assertArrayWrapper(String context, Array input, Dict res) throws Exception {
        assertType(res.getDict("type"), "object [Array]", true, true, "Array.prototype");
        assertEquals(context + " size", input.size(), res.get("size"));
        assertEquals(context + " data", input, res.get("data"));
        assertEquals(context + " str", asString(input), res.get("str"));
        String json = res.get("json", String.class);
        if (!input.equals(JsonSerializer.unserialize(json))) {
            assertEquals(context + " json", JsonSerializer.serialize(input, false), json);
        }
        assertValidProps(context + " keys", res.getDict("keys"));
        assertValidProps(context + " ownProperties", res.getDict("ownProperties"));
        assertValidProps(context + " prototypeProperties", res.getDict("prototypeProperties"));
    }

    @Test
    public void testNested() throws Exception {
        Array res = (Array) CallContext.execute("test/javascript/ad-hoc/nested-wrapper");
        Dict first = res.getDict(0);
        assertThat(first.get("nested"), instanceOf(Dict.class));
    }

    @Test
    public void testFunction() throws Exception {
        Dict res = (Dict) CallContext.execute("test/javascript/function", (Object) null);
        assertType(res.getDict("type"), "function [ProcedureWrapper]", true, false, "Function.prototype");
        assertThat("function name", res.get("name", String.class), containsString("wrapped "));
        assertValidProps("function keys", res.getDict("keys"));
        assertValidProps("function ownProperties", res.getDict("ownProperties"));
        assertValidProps("function prototypeProperties", res.getDict("prototypeProperties"));
    }

    @Test
    public void testException() throws Exception {
        Dict res = (Dict) CallContext.execute("test/javascript/exception");
        assertType(res.getDict("type"), "object [Error]", true, false, "Error.prototype");
        assertValidProps("exception keys", res.getDict("keys"));
        assertValidProps("exception ownProperties", res.getDict("ownProperties"));
        assertValidProps("exception prototypeProperties", res.getDict("prototypeProperties"));
    }

    @Test
    public void testConnection() throws Exception {
        Dict res = (Dict) CallContext.execute("test/javascript/connection");
        assertType(res.getDict("type"), "object [ConnectionWrapper]", true, false, "Object.prototype");
        assertType(res.getDict("methodType"), "function [Function]", true, false, "Function.prototype");
        assertValidProps("connection keys", res.getDict("keys"));
        assertValidProps("connection ownProperties", res.getDict("ownProperties"));
        assertValidProps("connection prototypeProperties", res.getDict("prototypeProperties"));
    }

    @Test
    public void testConsole() throws Exception {
        Dict res = (Dict) CallContext.execute("test/javascript/console");
        assertType(res.getDict("type"), "object [Console]", true, false, "Object.prototype");
        assertType(res.getDict("methodType"), "function [Function]", true, false, "Function.prototype");
        assertValidProps("console keys", res.getDict("keys"));
        assertValidProps("console ownProperties", res.getDict("ownProperties"));
        assertValidProps("console prototypeProperties", res.getDict("prototypeProperties"));
    }

    @Test
    public void testNativeJava() throws Exception {
        Array input = Array.of(42);
        Dict res = (Dict) CallContext.execute("test/javascript/helper/call", "test/native/values", input);
        assertEquals("int-std-val", 1234, res.get("int-std-val"));
        assertEquals("int-max-val", Integer.MAX_VALUE, res.get("int-max-val"));
        assertEquals("int-min-val", Integer.MIN_VALUE, res.get("int-min-val"));
        assertEquals("long-std-val", 1234L, res.get("long-std-val"));
        assertEquals("long-max-val", Long.MAX_VALUE, res.get("long-max-val"));
        assertEquals("long-min-val", Long.MIN_VALUE, res.get("long-min-val"));
        assertEquals("float-std-val", 12.34f, res.get("float-std-val"));
        assertEquals("float-max-val", Float.MAX_VALUE, res.get("float-max-val"));
        assertEquals("float-min-val", Float.MIN_VALUE, res.get("float-min-val"));
        assertEquals("double-std-val", 12.34d, res.get("double-std-val"));
        assertEquals("double-max-val", Double.MAX_VALUE, res.get("double-max-val"));
        assertEquals("double-min-val", Double.MIN_VALUE, res.get("double-min-val"));
        assertEquals("bool-true-val", true, res.get("bool-true-val"));
        assertEquals("bool-false-val", false, res.get("bool-false-val"));
        assertEquals("date", new Date(1234567890), res.get("date"));

        // NOTE: Numbers are floating-point double in JavaScript, so some loss of
        // precision, etc is unavoidable. See comments for comparisons below.
        res = (Dict) CallContext.execute("test/javascript/ad-hoc/native-values");
        assertEquals("int-std", 2468, res.get("int-std"));
        assertEquals("int-high", Integer.MAX_VALUE - 1, res.get("int-high"));
        assertEquals("int-low", Integer.MIN_VALUE + 1, res.get("int-low"));
        assertEquals("long-std", 2468, res.get("long-std")); // unwrapped to int
        assertEquals("long-high", Long.MAX_VALUE, res.get("long-high")); // precision loss
        assertEquals("long-low", Long.MIN_VALUE, res.get("long-low")); // precision loss
        assertEquals("float-std", 24.68d, res.get("float-std"));  // coerced to double
        assertEquals("float-high", Float.MAX_VALUE / 2.0d, res.get("float-high")); // coerced to double
        assertEquals("float-low", Float.MIN_VALUE * 2.0d, res.get("float-low")); // coerced to double
        assertEquals("double-std", 24.68d, res.get("double-std"));
        assertEquals("double-high", Double.MAX_VALUE / 2.0d, res.get("double-high"));
        assertEquals("double-low", Double.MIN_VALUE * 2.0d, res.get("double-low"));
        assertEquals("bool-true", true, res.get("bool-true"));
        assertEquals("bool-false", false, res.get("bool-false"));
    }

    // Validate conformity of type metadata
    private void assertType(Dict typeInfo, String name, boolean isObject, boolean isArray, String ...prototypes) {
        assertEquals("type name", name, typeInfo.get("name"));
        String chain = typeInfo.get("chain", String.class);
        for (String proto : prototypes) {
            assertThat("prototype chain", chain, containsString(proto));
        }
        assertEquals("is object", isObject, typeInfo.get("isObject"));
        assertEquals("is array", isArray, typeInfo.get("isArray"));
    }

    // Validate that no property values have a diff symbol
    private void assertValidProps(String context, Dict props) {
        for (String key : props.keys()) {
            String val = props.get(key, String.class);
            assertThat(context + " " + key, val, not(containsString("\uD83D\uDD34")));
        }
    }

    // Quick-and-dirty native JS toString
    private static String asString(Object val) {
        if (val == null) {
            return "";
        } else if (val instanceof Array arr) {
            return arr.stream()
                .map(JsProcedureIntegrationTest::asString)
                .collect(Collectors.joining(","));
        } else if (val instanceof Dict) {
            return "[object DictWrapper]";
        } else {
            return String.valueOf(val);
        }
    }
}
