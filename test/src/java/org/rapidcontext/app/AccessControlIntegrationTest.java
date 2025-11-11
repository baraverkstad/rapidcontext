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

package org.rapidcontext.app;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matcher;

import java.io.File;
import java.util.Objects;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rapidcontext.app.model.RequestContext;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.ctx.ThreadContext;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;

@SuppressWarnings("javadoc")
public class AccessControlIntegrationTest {

    private static final String[] ANYONE = { "anonymous", "test-viewer", "test-editor", "test-admin" };
    private static final String[] USERS = { "test-viewer", "test-editor", "test-admin" };

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
    public void testAnonymousAccess() throws Exception {
        // Check system built-in access
        assertAccess(true, "app/", "search", ANYONE);
        assertAccess(true, "app/login", "read", ANYONE);
        assertAccess(false, "app/start", "read", "anonymous");
        assertAccess(true, "files/", "read", ANYONE);
        assertAccess(true, "files/missing.txt", "read", ANYONE);
        assertAccess(true, "procedure/system/reset", "read", ANYONE);
        assertAccess(true, "type/", "search", ANYONE);
        assertAccess(true, "type/user", "read", ANYONE);

        // Check testdata access
        assertAccess(false, "testdata/", "search", "anonymous");
        assertAccess(true, "testdata/propsdata", "read", "anonymous");
        assertAccess(false, "testdata/yamldata", "read", "anonymous");
        assertAccess(false, "testdata/jsondata", "read", "anonymous");
        assertAccess(false, "testdata/xmldata", "read", "anonymous");
    }

    @Test
    public void testUserAccess() throws Exception {
        // Check per-role access
        assertAccess(true, "test-access/anonymous", "read", ANYONE);
        assertAccess(false, "test-access/standard", "read", "anonymous");
        assertAccess(true, "test-access/standard", "read", USERS);
        assertAccess(false, "test-access/extended", "read", "anonymous", "test-viewer");
        assertAccess(true, "test-access/extended", "read", "test-editor", "test-admin");
        assertAccess(false, "test-access/custom", "read", "anonymous", "test-viewer");
        assertAccess(true, "test-access/custom", "read", "test-editor", "test-admin");

        // Check testdata access
        assertAccess(true, "testdata/", "search", USERS);
        assertAccess(true, "testdata/jsondata", "read", USERS);
        assertAccess(true, "testdata/propsdata", "read", USERS);
        assertAccess(true, "testdata/yamldata", "read", USERS);
        assertAccess(true, "testdata/xmldata", "read", USERS);
        assertAccess(true, "testdata/nested/but/missing/file.txt", "read", USERS);
        assertAccess(false, "testdata/jsondata", "write", "anonymous", "test-viewer");
        assertAccess(true, "testdata/jsondata", "write", "test-editor", "test-admin");
        assertAccess(false, "testdata/nested/but/missing/file.txt", "write", "anonymous", "test-viewer");
        assertAccess(true, "testdata/nested/but/missing/file.txt", "write", "test-editor", "test-admin");

        // Test procedure access
        assertAccess(false, "procedure/test123/anything", "read", "test-viewer", "test-editor");
        assertAccess(false, "procedure/test456/something", "read", "test-viewer", "test-editor");
        assertAccess(true, "procedure/test/", "search", USERS);
        assertAccess(true, "procedure/test/native/values", "read", USERS);
        assertAccess(true, "procedure/test/native/values", "internal", USERS);
        assertAccess(true, "procedure/TEST/native/values", "internal", USERS);
        assertAccess(false, "procedure/test/cmd/ls", "read", "test-viewer");
        assertAccess(true, "procedure/test/cmd/ls", "read", "test-editor", "test-admin");
        assertAccess(true, "/procedure/TeSt/cmd/ls", "read", "test-editor", "test-admin");
        assertAccess(false, "procedure/test/http/test-get", "read", "test-viewer");
        assertAccess(true, "procedure/test/http/test-get", "read", "test-editor", "test-admin");
        assertAccess(true, "procedure/test/htp/something-else", "read", USERS);
    }

    @Test
    public void testIndirectAccess() throws Exception {
        // Test direct access
        assertAccess(false, "connection/test/http", "internal", "test-viewer", "test-editor");
        assertAccess(false, "connection/test/httpbin", "read", "test-viewer", "test-editor");
        assertAccess(false, "connection/test/http/nested", "internal", "test-viewer", "test-editor");
        assertAccess(false, "connection/test/other", "internal", "test-viewer", "test-editor");

        // Test call chain access
        CallContext cx = CallContext.init("test/http/test-get");
        try {
            assertAccess(true, "connection/test/http", "internal", USERS);
            assertAccess(true, "connection/test/httpbin", "internal", USERS);
            assertAccess(true, "/CONNECTION/TEST/HTTPBIN", "internal", USERS);
        } finally {
            cx.close();
        }

        // Test explicit via access
        assertViaAccess(true, "connection/test/httpbin", "read", "procedure/test/http/test-get", USERS);
        assertViaAccess(true, "connection/test/httpbin", "read", "procedure/test/HTTP/TEST-GET", USERS);
        assertViaAccess(false, "connection/test/httpbin", "read", "procedure/system/procedure/call", "test-viewer", "test-editor");
        assertViaAccess(true, "connection/test/httpbin", "read", "procedure/system/procedure/call", "test-admin");
        assertViaAccess(false, "connection/test/httpbin", "read", "-", "test-viewer", "test-editor");
    }

    @Test
    public void testCustomPermissions() throws Exception {
        // Check custom-one permission
        assertAccess(false, "test-access/", "custom-one", "anonymous", "test-viewer");
        assertAccess(true, "test-access/", "custom-one", "test-editor", "test-admin");
        assertAccess(false, "test-access/specific/only", "custom-one", "anonymous", "test-viewer");
        assertAccess(true, "test-access/specific/only", "custom-one", "test-editor", "test-admin");

        // Check custom-two permission
        assertAccess(false, "test-access/", "custom-two", "anonymous", "test-viewer", "test-editor");
        assertAccess(true, "test-access/", "custom-two", "test-admin");
        assertAccess(false, "test-access/specific/only", "custom-two", "anonymous", "test-viewer");
        assertAccess(true, "test-access/specific/only", "custom-two", "test-editor", "test-admin");

        // Check standard permissions
        assertAccess(false, "test-access/specific/only", "read", "test-editor");
        assertAccess(false, "test-access/specific/only", "write", "test-editor");
        assertAccess(false, "test-access/specific/only", "search", "test-editor");
    }

    @Test
    public void testContextAccess() throws Exception {
        // Check anonymous access
        RequestContext localCx = RequestContext.initLocal(null);
        try {
            assertCtxAccess(false, "testdata/", "search", "read");
            assertCtxAccess(true, "testdata/propsdata", "read");
            assertCtxAccess(false, "testdata/propsdata.properties", "read");
            assertCtxAccess(false, "testdata/propsdata", "write");
        } finally {
            localCx.close();
        }

        // Check test-viewer access
        localCx = RequestContext.initLocal("test-viewer");
        try {
            assertCtxAccess(true, "testdata/", "search", "read");
            assertCtxAccess(true, "testdata/propsdata", "read");
            assertCtxAccess(true, "testdata/propsdata.properties", "read");
            assertCtxAccess(false, "testdata/propsdata", "write");
        } finally {
            localCx.close();
        }

        // Check test-editor access
        localCx = RequestContext.initLocal("test-editor");
        try {
            assertCtxAccess(true, "testdata/", "search", "read");
            assertCtxAccess(true, "testdata/propsdata", "read");
            assertCtxAccess(true, "testdata/propsdata.properties", "read");
            assertCtxAccess(true, "testdata/propsdata", "write");
        } finally {
            localCx.close();
        }
    }

    @Test
    public void testStorageQuery() throws Exception {
        // Check anonymous access
        RequestContext localCx = RequestContext.initLocal(null);
        try {
            assertThrows(ProcedureException.class, () -> storageQuery("files/testdata/"));
            assertThrows(ProcedureException.class, () -> storageQuery("testdata/"));
            assertThrows(ProcedureException.class, () -> storageQuery("procedure/test/"));
            Array res = storageQuery("app/");
            assertEquals("anonymous app/ results", 1, res.size());
            assertPaths(res, containsString("login"));
        } finally {
            localCx.close();
        }

        // Check test-viewer access
        localCx = RequestContext.initLocal("test-viewer");
        try {
            assertThrows(ProcedureException.class, () -> storageQuery("files/testdata/"));
            Array res = storageQuery("testdata/");
            assertEquals("test-viewer testdata/ results", 5, res.size());
            res = storageQuery("procedure/test/");
            assertPaths(res, allOf(
                not(containsString("/test/cmd/")),
                not(containsString("/test/http/"))
            ));
            res = storageQuery("app/");
            assertEquals("anonymous app/ results", 2, res.size());
            assertPaths(res, anyOf(
                containsString("app/login"),
                containsString("app/example")
            ));
        } finally {
            localCx.close();
        }

        // Check test-editor access
        localCx = RequestContext.initLocal("test-editor");
        try {
            assertThrows(ProcedureException.class, () -> storageQuery("files/testdata/"));
            Array res = storageQuery("procedure/test/");
            assertPaths(res, anyOf(
                containsString("test/cmd/"),
                containsString("test/http/"),
                containsString("test/javascript/"),
                containsString("test/native/")
            ));
            res = storageQuery("app/");
            assertEquals("test-editor app/ results", 2, res.size());
        } finally {
            localCx.close();
        }
    }

    @Test
    public void testStorageRead() throws Exception {
        // Check anonymous access
        RequestContext localCx = RequestContext.initLocal(null);
        try {
            assertThat(storageRead("testdata/propsdata"), instanceOf(Dict.class));
            assertThrows(ProcedureException.class, () -> storageRead("testdata/yamldata"));
            assertThrows(ProcedureException.class, () -> storageRead("procedure/test/native/values"));
            assertThrows(ProcedureException.class, () -> storageRead("app/example"));
        } finally {
            localCx.close();
        }

        // Check test-viewer access
        localCx = RequestContext.initLocal("test-viewer");
        try {
            assertThat(storageRead("testdata/yamldata"), instanceOf(Dict.class));
            assertThat(storageRead("procedure/test/native/values"), instanceOf(Dict.class));
            assertThat(storageRead("app/example"), instanceOf(Dict.class));
        } finally {
            localCx.close();
        }

        // Check test-editor access
        localCx = RequestContext.initLocal("test-editor");
        try {
            assertThat(storageRead("procedure/test/cmd/ls"), instanceOf(Dict.class));
            assertThat(storageRead("procedure/test/http/test-get"), instanceOf(Dict.class));
        } finally {
            localCx.close();
        }
    }

    @Test
    public void testStorageWrite() throws Exception {
        // Check anonymous access
        RequestContext localCx = RequestContext.initLocal(null);
        try {
            assertThrows(ProcedureException.class, () -> storageWrite("testdata/test-write", new Dict()));
            assertThrows(ProcedureException.class, () -> storageDelete("testdata/yamldata"));
        } finally {
            localCx.close();
        }

        // Check test-viewer access
        localCx = RequestContext.initLocal("test-viewer");
        try {
            assertThrows(ProcedureException.class, () -> storageWrite("testdata/test-write", new Dict()));
            assertThrows(ProcedureException.class, () -> storageDelete("testdata/yamldata"));
        } finally {
            localCx.close();
        }

        // Check test-editor access
        localCx = RequestContext.initLocal("test-editor");
        try {
            boolean success = storageWrite("testdata/test-write", new Dict());
            assertTrue("test-editor write", success);
            success = storageDelete("testdata/test-write");
            assertTrue("test-editor delete", success);
            assertThrows(ProcedureException.class, () -> storageWrite("test-access/extended", new Dict()));
            assertThrows(ProcedureException.class, () -> storageDelete("test-access/extended"));
        } finally {
            localCx.close();
        }
    }

    @Test
    public void testCalls() throws Exception {
        // Check anonymous access
        RequestContext localCx = RequestContext.initLocal(null);
        try {
            assertThrows(ProcedureException.class, () -> call("test/native/values", 1));
            assertThrows(ProcedureException.class, () -> callIndirect("test/native/values", 1));
            assertThat(call("system/storage/read", "testdata/propsdata"), instanceOf(Dict.class));
            assertThat(callIndirect("system/storage/read", "testdata/propsdata"), instanceOf(Dict.class));
            assertThrows(ProcedureException.class, () -> call("system/storage/read", "testdata/yamldata"));
            assertThrows(ProcedureException.class, () -> callIndirect("system/storage/read", "testdata/yamldata"));
        } finally {
            localCx.close();
        }

        // Check test-viewer access
        localCx = RequestContext.initLocal("test-viewer");
        try {
            assertThat(call("test/native/values", 1), instanceOf(Dict.class));
            assertThat(callIndirect("test/native/values", 1), instanceOf(Dict.class));
            assertThrows(ProcedureException.class, () -> call("test/http/test-get"));
            assertThrows(ProcedureException.class, () -> callIndirect("test/http/test-get"));
            assertThat(call("system/storage/read", "testdata/yamldata"), instanceOf(Dict.class));
            assertThat(callIndirect("system/storage/read", "testdata/yamldata"), instanceOf(Dict.class));

            // Check connection/test/httpbin access (indirect)
            assertThrows(ProcedureException.class, () -> call("system/storage/read", "connection/test/httpbin"));
            assertThrows(ProcedureException.class, () -> callIndirect("system/storage/read", "connection/test/httpbin"));
            assertThat(call("test/javascript/connection"), instanceOf(Dict.class));
            assertThat(callIndirect("test/javascript/connection"), instanceOf(Dict.class));
        } finally {
            localCx.close();
        }

        // Check test-editor access
        localCx = RequestContext.initLocal("test-editor");
        try {
            assertThat(call("test/native/values", 1), instanceOf(Dict.class));
            assertThat(callIndirect("test/native/values", 1), instanceOf(Dict.class));
            assertThat(call("system/storage/read", "testdata/yamldata"), instanceOf(Dict.class));
            assertThat(callIndirect("system/storage/read", "testdata/yamldata"), instanceOf(Dict.class));

            // Check connection/test/httpbin access (indirect)
            assertThat(call("test/javascript/connection"), instanceOf(Dict.class));
            assertThat(callIndirect("test/javascript/connection"), instanceOf(Dict.class));
            assertThrows(ProcedureException.class, () -> call("system/storage/read", "connection/test/httpbin"));
            assertThrows(ProcedureException.class, () -> callIndirect("system/storage/read", "connection/test/httpbin"));

            // Check double indirect call protection
            String callProc = "system/procedure/call";
            Array args = Array.of("test/javascript/connection");
            assertThrows(ProcedureException.class, () -> CallContext.execute(callProc, callProc, args));
        } finally {
            localCx.close();
        }
    }

    private void assertAccess(boolean expect, String path, String perm, String... users) throws Exception {
        for (String user : users) {
            boolean res = (Boolean) CallContext.execute("system/user/access", path, perm, user);
            assertEquals(user + " " + perm + " " + path + " access", expect, res);
        }
    }

    private void assertViaAccess(boolean expect, String path, String perm, String via, String... users) throws Exception {
        Dict opts = new Dict().set("permission", perm).set("via", via);
        for (String user : users) {
            boolean res = (Boolean) CallContext.execute("system/user/access", path, opts, user);
            assertEquals(user + " " + perm + " " + path + " via " + via + " access", expect, res);
        }
    }

    private void assertCtxAccess(boolean expect, String path, String... perms) throws Exception {
        ThreadContext ctx = ThreadContext.active();
        for (String perm : perms) {
            boolean res = ctx.hasAccess(path, perm);
            assertEquals(ctx.user() + " " + path + " " + perm + " access", expect, res);
        }
    }

    private void assertPaths(Array res, Matcher<String> matcher) {
        for (Object o : res) {
            assertThat(o, instanceOf(Dict.class));
            String path = ((Dict) o).get("path", String.class);
            assertThat(path, matcher);
        }
    }

    private Array storageQuery(String path) throws Exception {
        return (Array) CallContext.execute("system/storage/query", path);
    }

    private Object storageRead(String path) throws Exception {
        return CallContext.execute("system/storage/read", path);
    }

    private boolean storageWrite(String path, Dict data) throws Exception {
        return (Boolean) CallContext.execute("system/storage/write", path, data);
    }

    private boolean storageDelete(String path) throws Exception {
        return (Boolean) CallContext.execute("system/storage/delete", path);
    }

    private Object call(String proc, Object... args) throws Exception {
        return CallContext.execute(proc, args);
    }

    private Object callIndirect(String proc, Object... args) throws Exception {
        return CallContext.execute("system/procedure/call", proc, Array.of(args));
    }
}
