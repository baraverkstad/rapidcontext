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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWithIgnoringCase;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.startsWithIgnoringCase;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.rapidcontext.core.data.JsonSerializer.serialize;

import java.io.File;
import java.util.Date;
import java.util.Objects;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rapidcontext.app.model.RequestContext;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.Binary;
import org.rapidcontext.core.web.Mime;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.storage.Storage;

@SuppressWarnings("javadoc")
public class StorageProcedureIntegrationTest {

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
        CallContext.execute("system/storage/delete", "testdata/");
        CallContext.execute("system/storage/delete", "testdata-copy/");
        if (cx != null) {
            cx.close();
        }
    }

    @Test
    public void testQuerySingle() throws Exception {
        // Query validations
        assertThrows(ProcedureException.class, () -> checkQuerySingle(null, null, false));
        checkQuerySingle("/missing-object", null, false);

        // Query serialized objects
        checkQuerySingle("/testdata/jsondata", Mime.JSON[0], false);
        checkQuerySingle("/testdata/propsdata", Mime.PROPERTIES[0], false);
        checkQuerySingle("/testdata/xmldata", Mime.XML[0], false);
        checkQuerySingle("/testdata/yamldata", Mime.YAML[0], false);
        checkQuerySingle("/testdata/ad-hoc/.hidden", Mime.YAML[0], false);

        // Verify no access to actual files
        checkQuerySingle("/testdata/propsdata.properties", null, true);
        checkQuerySingle("/testdata/jsondata.json", null, true);
        checkQuerySingle("/testdata/xmldata.xml", null, true);
        checkQuerySingle("/testdata/yamldata.yaml", null, true);
        checkQuerySingle("/testdata/ad-hoc/.hidden.yaml", null, true);

        // Query serialized objects via /files and storage paths
        checkQuerySingle("/files/testdata/yamldata", Mime.YAML[0], false);
        checkQuerySingle("/files/testdata/yamldata.yaml", Mime.YAML[0], true);
        checkQuerySingle("/files/testdata/yamldata.json", null, false);
        checkQuerySingle("/.storage/plugin/test/testdata/yamldata", Mime.YAML[0], false);
        checkQuerySingle("/.storage/plugin/test/testdata/yamldata.yaml", Mime.YAML[0], true);
        checkQuerySingle("/.storage/plugin/test/testdata/yamldata.xml", null, false);

        // Query binary file
        checkQuerySingle("/testdata/ad-hoc/binary/plain.txt", Mime.TEXT[0], true);

        // Query Java object
        checkQuerySingle("/connection/test/httpbin", Mime.YAML[0], false);
    }

    private void checkQuerySingle(String path, String mimeType, boolean binary) throws Exception {
        Dict opts = new Dict().set("path", path).set("computed", true);
        Object basic = CallContext.execute("system/storage/query", path);
        Object extended = CallContext.execute("system/storage/query", opts);
        if (mimeType == null) {
            assertEquals(path, null, basic);
            assertEquals(path, null, extended);
        } else {
            assertMetadata((Dict) basic, path, binary, mimeType);
            assertMetadata((Dict) extended, path, binary, mimeType);
            if (extended instanceof Dict dict) {
                assertEquals(path + " _plugin", "test", dict.get("_plugin"));
                Array arr = new Array().add("/.storage/plugin/test/");
                assertEquals(path + " _storages", arr, dict.get("_storages"));
            }
        }
    }

    @Test
    public void testQueryMultiple() throws Exception {
        // Query validations
        checkQueryMultiple("missing-index/", "ignored", "value", 0);
        checkQueryMultiple("testdata/", "ignored", "value", 5);

        // Query result limits
        checkQueryMultiple("testdata/", "limit", 3, 3);
        checkQueryMultiple("testdata/", "limit", 0, 5);
        checkQueryMultiple("testdata/", "limit", -1, 5);
        checkQueryMultiple("testdata/", "limit", null, 5);
        assertThrows(ProcedureException.class, () -> checkQueryMultiple("testdata/", "limit", "abc", -1));

        // Query hidden files
        checkQueryMultiple("testdata/", "hidden", true, 6);
        checkQueryMultiple("testdata/", "hidden", "yes", 6);
        checkQueryMultiple("testdata/", "hidden", 1, 6);
        checkQueryMultiple("testdata/", "hidden", false, 5);
        checkQueryMultiple("testdata/", "hidden", null, 5);

        // Query folder depth
        checkQueryMultiple("testdata/", "depth", 0, 4);
        checkQueryMultiple("testdata/", "depth", 0L, 4);
        checkQueryMultiple("testdata/", "depth", "0", 4);
        checkQueryMultiple("testdata/", "depth", 1, 4);
        checkQueryMultiple("testdata/", "depth", -1, 5);
        checkQueryMultiple("testdata/", "depth", null, 5);

        // Query file type
        checkQueryMultiple("testdata/", "fileType", "yaml", 0);
        checkQueryMultiple("files/testdata/", "fileType", "yaml", 1);
        checkQueryMultiple("testdata/", "fileType", "txt", 1);
        checkQueryMultiple("testdata/", "fileType", "TXT", 1);
        checkQueryMultiple("testdata/", "fileType", null, 5);
        checkQueryMultiple("testdata/", "fileType", " ", 5);

        // Query MIME type
        checkQueryMultiple("testdata/", "mimeType", "text/yaml", 1);
        checkQueryMultiple("testdata/", "mimeType", "TEXT/YAML", 1);
        checkQueryMultiple("testdata/", "mimeType", "application/", 1);
        checkQueryMultiple("testdata/", "mimeType", "text/", 4);
        checkQueryMultiple("testdata/", "mimeType", "text/x", 2);
        checkQueryMultiple("testdata/", "mimeType", "unknown", 0);
        checkQueryMultiple("testdata/", "mimeType", " ", 5);
        checkQueryMultiple("testdata/", "mimeType", null, 5);

        // Query object category
        checkQueryMultiple("testdata/", "category", "binary", 1);
        checkQueryMultiple("testdata/", "category", "object", 4);
        checkQueryMultiple("testdata/", "category", "unknown", 0);
        checkQueryMultiple("testdata/", "category", null, 5);
    }

    private void checkQueryMultiple(String path, String key, Object val, int count) throws Exception {
        Dict opts = new Dict().set("path", path).set(key, val);
        Array res = (Array) CallContext.execute("system/storage/query", opts);
        String context = "query " + key + " " + val;
        assertEquals(context + " count", count, res.size());
        for (Object o : res) {
            Dict d = (Dict) o;
            boolean hasVal = val != null && !val.toString().isBlank();
            if (key.equals("fileType") && hasVal) {
                assertThat(context + " path", d.get("path", String.class), endsWithIgnoringCase("." + val));
            } else if (key.equals("mimeType") && hasVal && val instanceof String s) {
                assertThat(context + " mimeType", d.get("mimeType", String.class), startsWithIgnoringCase(s));
            } else if (key.equals("category") && hasVal) {
                assertEquals(context + " category", val, d.get("category", String.class));
            }
        }
    }

    @Test
    public void testList() throws Exception {
        assertThrows(ProcedureException.class, () -> CallContext.execute("system/storage/list", ""));
        checkList("testdata/", 1, 4);
        assertThrows(ProcedureException.class, () -> CallContext.execute("system/storage/list", "testdata"));
        checkList("files/testdata/", 5, 0);
        checkList(".storage/plugin/test/testdata/", 5, 0);
        checkList("missing-index/", 0, 0);
    }

    private void checkList(String path, int binaryCount, int objectCount) throws Exception {
        Array res = (Array) CallContext.execute("system/storage/list", path);
        assertDataList(res, path, binaryCount, objectCount);
    }

    @Test
    public void testReadSingle() throws Exception {
        // Read serialized objects
        checkReadSingle("testdata/yamldata", false, Mime.YAML[0]);
        checkReadSingle("testdata/propsdata", false, Mime.PROPERTIES[0]);
        checkReadSingle("testdata/jsondata", false, Mime.JSON[0]);
        checkReadSingle("testdata/xmldata", false, Mime.XML[0]);
        checkReadSingle("testdata/ad-hoc/.hidden", false, Mime.YAML[0]);

        // Verify no access to actual files
        checkReadSingle("testdata/yamldata.yaml", true, null);
        checkReadSingle("testdata/propsdata.properties", true, null);
        checkReadSingle("testdata/jsondata.json", true, null);
        checkReadSingle("testdata/xmldata.xml", true, null);
        checkReadSingle("testdata/ad-hoc/.hidden.yaml", true, null);

        // Read serialized objects via /files and storage paths
        checkReadSingle("files/testdata/yamldata", false, Mime.YAML[0]);
        checkReadSingle("files/testdata/yamldata.yaml", true, Mime.YAML[0]);
        checkReadSingle("files/testdata/yamldata.json", true, null);
        checkReadSingle(".storage/plugin/test/testdata/yamldata", false, Mime.YAML[0]);
        checkReadSingle(".storage/plugin/test/testdata/yamldata.yaml", true, Mime.YAML[0]);
        checkReadSingle(".storage/plugin/test/testdata/yamldata.json", true, null);

        // Read binary file
        checkReadSingle("testdata/ad-hoc/binary/plain.txt", true, Mime.TEXT[0]);
        Dict res = (Dict) storageRead("testdata/ad-hoc/binary/plain.txt", false, true, null);
        assertThat("computed _text", res.get("_text", String.class), containsString("storage testing"));

        // Read Java object
        String path = "connection/test/httpbin";
        res = storageReadDict(path);
        assertEquals(path + " id", "test/httpbin", res.get("id"));
        assertEquals(path + " type", "connection/http", res.get("type"));
        assertNull(path + " _maxOpen", res.get("_maxOpen"));
        res = (Dict) storageRead(path, false, true, null);
        assertEquals(path + " _maxOpen", 4, (int) res.get("_maxOpen", Integer.class));
    }

    private void checkReadSingle(String path, boolean binary, String mimeType)
    throws Exception {
        Dict rawData = storageReadDict(path);
        Dict withMeta = storageReadMeta(path);
        Dict meta = (Dict) CallContext.execute("system/storage/query", path);
        if (mimeType == null) {
            assertEquals("read " + path, null, rawData);
            assertEquals("read " + path, null, withMeta);
        } else {
            String id = path.substring(path.lastIndexOf('/') + 1);
            Dict expect = binary ? meta : buildDict(id, !id.equals("propsdata"));
            assertData(rawData, "/" + path, binary, expect);
            assertData(withMeta.getDict("data"), "/" + path, binary, expect);
            assertMetadata(withMeta.getDict("metadata"), "/" + path, binary, mimeType);
            assertEquals("read " + path + " metadata", meta, withMeta.getDict("metadata"));
        }
    }

    @Test
    public void testReadMultiple() throws Exception {
        // Read validations
        checkReadMultiple("missing-index/", "ignored", "value", 0, 0);
        checkReadMultiple("testdata/", "ignored", "value", 1, 4);

        // Read result limits
        checkReadMultiple("testdata/", "limit", 3, 1, 2);
        checkReadMultiple("testdata/", "limit", 0, 1, 4);
        checkReadMultiple("testdata/", "limit", -1, 1, 4);
        checkReadMultiple("testdata/", "limit", null, 1, 4);
        assertThrows(ProcedureException.class, () -> checkReadMultiple("testdata/", "limit", "abc", -1, -1));

        // Read hidden files
        checkReadMultiple("testdata/", "hidden", true, 1, 5);
        checkReadMultiple("testdata/", "hidden", "yes", 1, 5);
        checkReadMultiple("testdata/", "hidden", 1, 1, 5);
        checkReadMultiple("testdata/", "hidden", false, 1, 4);
        checkReadMultiple("testdata/", "hidden", null, 1, 4);

        // Read folder depth
        checkReadMultiple("testdata/", "depth", 0, 0, 4);
        checkReadMultiple("testdata/", "depth", 0L, 0, 4);
        checkReadMultiple("testdata/", "depth", "0", 0, 4);
        checkReadMultiple("testdata/", "depth", 1, 0, 4);
        checkReadMultiple("testdata/", "depth", -1, 1, 4);
        checkReadMultiple("testdata/", "depth", null, 1, 4);

        // Read file type
        checkReadMultiple("testdata/", "fileType", "yaml", 0, 0);
        checkReadMultiple("files/testdata/", "fileType", "yaml", 1, 0);
        checkReadMultiple("testdata/", "fileType", "txt", 1, 0);
        checkReadMultiple("testdata/", "fileType", "TXT", 1, 0);
        checkReadMultiple("testdata/", "fileType", null, 1, 4);
        checkReadMultiple("testdata/", "fileType", " ", 1, 4);

        // Read MIME type
        checkReadMultiple("testdata/", "mimeType", "text/yaml", 0, 1);
        checkReadMultiple("testdata/", "mimeType", "TEXT/YAML", 0, 1);
        checkReadMultiple("testdata/", "mimeType", "application/", 0, 1);
        checkReadMultiple("testdata/", "mimeType", "text/", 1, 3);
        checkReadMultiple("testdata/", "mimeType", "text/x", 0, 2);
        checkReadMultiple("testdata/", "mimeType", "unknown", 0, 0);
        checkReadMultiple("testdata/", "mimeType", " ", 1, 4);
        checkReadMultiple("testdata/", "mimeType", null, 1, 4);

        // Read object category
        checkReadMultiple("testdata/", "category", "binary", 1, 0);
        checkReadMultiple("testdata/", "category", "object", 0, 4);
        checkReadMultiple("testdata/", "category", "unknown", 0, 0);
        checkReadMultiple("testdata/", "category", null, 1, 4);
    }

    private void checkReadMultiple(String path, String key, Object val, int binaryCount, int objectCount) throws Exception {
        Array res = (Array) storageRead(path, false, false, new Dict().set(key, val));
        assertDataList(res, path, binaryCount, objectCount);
    }

    @Test
    public void testWriteSimple() throws Exception {
        // Write validations
        assertThrows(ProcedureException.class, () -> checkWriteSimple("", "", Mime.YAML[0], new Dict()));
        checkWriteSimple(".storage/plugin/test/test", "", null, new Dict());
        assertThrows(ProcedureException.class, () -> checkWriteSimple("testdata/", "", Mime.YAML[0], new Dict()));
        assertThrows(ProcedureException.class, () -> storageWrite(
            "testdata/write-both.json", null, new Dict(), "yaml"
        ));
        assertTrue(storageWrite("testdata/write-both.json", null, new Dict(), "json"));

        // Write data objects
        Dict data = buildDict("write-test", false);
        checkWriteSimple("testdata/write-default", "", Mime.PROPERTIES[0], data);
        checkWriteSimple("testdata/write-props", "properties", Mime.PROPERTIES[0], data);
        checkWriteSimple("testdata/write-yaml", "yaml", Mime.YAML[0], data);
        checkWriteSimple("testdata/write-json", "json", Mime.JSON[0], data);
        checkWriteSimple("testdata/write-xml", "xml", Mime.XML[0], data);

        // Overwrite data objects
        data.set("id", "write-updated");
        data.remove("arr");
        checkWriteSimple("testdata/write-default", "", Mime.PROPERTIES[0], data);
        checkWriteSimple("testdata/write-props", "properties", Mime.PROPERTIES[0], data);
        checkWriteSimple("testdata/write-yaml", "yaml", Mime.YAML[0], data);
        checkWriteSimple("testdata/write-json", "json", Mime.JSON[0], data);
        checkWriteSimple("testdata/write-xml", "xml", Mime.XML[0], data);

        // Overwrite with format change
        data.set("id", "write-format-change");
        checkWriteSimple("testdata/write-default", "yaml", Mime.YAML[0], data);
        checkWriteSimple("testdata/write-default", "json", Mime.JSON[0], data);
        checkWriteSimple("testdata/write-default", "xml", Mime.XML[0], data);
        checkWriteSimple("testdata/write-default", "properties", Mime.PROPERTIES[0], data);

        // Overwrite plugin data objects
        data.set("id", "write-override");
        checkWriteSimple("testdata/jsondata", "yaml", Mime.YAML[0], data);
        checkWriteSimple("testdata/jsondata", "", null, null); // Delete by writing null data
        Dict res = storageReadDict("testdata/jsondata");
        assertEquals("original restored", "jsondata", res.get("id"));

        // Write binary (plain text)
        String text = "storage testing write";
        boolean success = storageWrite("testdata/write-plain.txt", null, text, "binary");
        assertTrue("binary write plain.txt", success);
        checkReadSingle("testdata/write-plain.txt", true, Mime.TEXT[0]);
        res = (Dict) storageRead("testdata/write-plain.txt", false, true, null);
        assertThat("binary _text", res.get("_text", String.class), containsString(text));

        // Write binary (.bin)
        Binary.BinaryString bytes = new Binary.BinaryString("XYZ-01");
        success = storageWrite("testdata/write-data.bin", null, bytes, "binary");
        assertTrue("binary write data.bin", success);
        checkReadSingle("testdata/write-data.bin", true, Mime.BIN[0]);
        Dict meta = (Dict) CallContext.execute("system/storage/query", "/testdata/write-data.bin");
        assertEquals("binary size", bytes.size(), (long) meta.get("size", Long.class));
    }

    private void checkWriteSimple(String path, String format, String mimeType, Dict data) throws Exception {
        // Write using path + format extension
        String formatPath = path + (format.isBlank() ? "" : "." + format);
        boolean expect = Objects.isNull(mimeType) == Objects.isNull(data);
        boolean success = storageWrite(formatPath, null, data, "");
        assertEquals("write success", expect, success);
        Dict res = storageReadMeta(path);
        if (!success) {
            assertNull("read failed", res);
        } else if (data != null) {
            assertData(res.getDict("data"), "/" + path, false, data);
            assertMetadata(res.getDict("metadata"), "/" + path, false, mimeType);
        }

        // Write using format argument
        String plainPath = path + "-plain";
        success = storageWrite(plainPath, null, data, format);
        assertEquals("write success", expect, success);
        res = storageReadMeta(plainPath);
        if (mimeType == null) {
            assertNull("read failed", res);
        } else {
            assertData(res.getDict("data"), "/" + plainPath, false, data);
            assertMetadata(res.getDict("metadata"), "/" + plainPath, false, mimeType);
        }
    }

    @Test
    public void testWriteUpdate() throws Exception {
        // Write initial data
        storageWrite("testdata/update.json", null, buildDict("update", false));
        Dict expect = storageReadMeta("testdata/update");
        Date created = expect.getDict("metadata").get("modified", Date.class);

        // Write patch data
        Dict patch = new Dict().set("id", "update-patch").set("object", null);
        boolean success = storageWrite("testdata/update", new Dict().set("update", true), patch);
        assertTrue("write update", success);
        Dict res = storageReadMeta("testdata/update");
        Date modified = res.getDict("metadata").get("modified", Date.class);
        assertData(res.getDict("data"), "/testdata/update", false, expect.getDict("data").merge(patch));
        assertTrue("write modified", modified.after(created));

        // Write patch and move
        patch.set("id", "update-moved").set("object", new Dict().set("key", "moved"));
        success = storageWrite("testdata/update", new Dict().set("updateTo", "testdata/update-moved"), patch);
        assertTrue("write updateTo", success);
        res = storageReadMeta("testdata/update-moved");
        Date modifiedAgain = res.getDict("metadata").get("modified", Date.class);
        assertData(res.getDict("data"), "/testdata/update-moved", false, expect.getDict("data").merge(patch));
        assertTrue("write modified", modifiedAgain.after(modified));
        assertNull("write updateTo", storageReadMeta("testdata/update"));
    }

    @Test
    public void testDelete() throws Exception {
        // Delete validations
        assertThrows(ProcedureException.class, () -> CallContext.execute("system/storage/delete", ""));

        // Single object delete
        Dict data = buildDict("delete-test", true);
        storageWrite("testdata/delete-test.json", null, data);
        checkReadSingle("testdata/delete-test", false, Mime.JSON[0]);
        boolean success = (Boolean) CallContext.execute("system/storage/delete", "testdata/delete-test");
        assertTrue("delete success", success);
        checkReadSingle("testdata/delete-test", false, null);

        // Recursive delete (directory)
        storageWrite("testdata/delete-dir/one", null, data);
        storageWrite("testdata/delete-dir/two", null, data);
        assertNotNull(storageReadMeta("testdata/delete-dir/one"));
        assertNotNull(storageReadMeta("testdata/delete-dir/two"));
        success = (Boolean) CallContext.execute("system/storage/delete", "testdata/delete-dir/");
        assertTrue("delete success", success);
        checkReadSingle("testdata/delete-dir/one", false, null);
        checkReadSingle("testdata/delete-dir/two", false, null);

        // Read-only delete
        success = (Boolean) CallContext.execute("system/storage/delete", ".storage/plugin/test/testdata/yamldata");
        assertFalse("delete success", success);
        checkReadSingle("testdata/yamldata", false, Mime.YAML[0]);
    }

    @Test
    public void testCopy() throws Exception {
        // Copy validations
        assertThrows(ProcedureException.class, () -> storageCopy("", "x", ""));
        assertThrows(ProcedureException.class, () -> storageCopy("x", "", ""));
        assertFalse(storageCopy("missing-data", "testdata/", ""));
        assertThrows(ProcedureException.class, () -> storageCopy("testdata", "testdata", ""));
        assertThrows(ProcedureException.class, () -> storageCopy("testdata/", "testdata/sub/", ""));
        assertThrows(ProcedureException.class, () -> storageCopy("testdata/", "testdata/", "recursive"));
        assertThrows(ProcedureException.class, () -> storageCopy("testdata/", "testdata/sub/", "recursive"));
        assertThrows(ProcedureException.class, () -> storageCopy("testdata/jsondata", "copy.yaml", "properties"));

        // Copy same format
        checkCopy("testdata/jsondata", "testdata/jsondata-copy", null, Mime.JSON[0]);
        checkCopy("testdata/propsdata", "testdata/propsdata-copy", null, Mime.PROPERTIES[0]);
        checkCopy("testdata/xmldata", "testdata/xmldata-copy", "", Mime.XML[0]);
        checkCopy("testdata/yamldata", "testdata/yamldata-copy", "", Mime.YAML[0]);

        // Copy with format extension
        checkCopy("testdata/xmldata", "testdata/xmldata-copy-ext.json", "", Mime.JSON[0]);
        checkCopy("testdata/xmldata", "testdata/xmldata-copy-ext.properties", "", Mime.PROPERTIES[0]);
        checkCopy("testdata/xmldata", "testdata/xmldata-copy-ext.yaml", "", Mime.YAML[0]);
        checkCopy("testdata/xmldata", "testdata/xmldata-copy-ext.xml", "", Mime.XML[0]);

        // Copy with format argument
        checkCopy("testdata/jsondata", "testdata/jsondata-copy-props", "properties", Mime.PROPERTIES[0]);
        checkCopy("testdata/jsondata", "testdata/jsondata-copy-json", "json", Mime.JSON[0]);
        checkCopy("testdata/jsondata", "testdata/jsondata-copy-yaml", "yaml", Mime.YAML[0]);
        checkCopy("testdata/jsondata", "testdata/jsondata-copy-xml", "xml", Mime.XML[0]);

        // Copy special files
        checkCopy("testdata/ad-hoc/.hidden", "testdata/ad-hoc/.copy-hidden", null, Mime.YAML[0]);
        checkCopy("testdata/ad-hoc/binary/plain.txt", "testdata/", "binary", Mime.TEXT[0]);
    }

    private void checkCopy(String from, String to, String format, String mimeType)
    throws Exception {
        Dict expect = storageReadMeta(from);
        Date created = expect.getDict("metadata").get("modified", Date.class);
        if (to.endsWith(".properties") || "properties".equals(format)) {
            expect.getDict("data").remove("empty"); // Not serialized to properties
        }
        boolean success = storageCopy(from, to, (format == null) ? "" : format);
        assertEquals("copy success", true, success);
        if (!from.endsWith("/") && to.endsWith("/")) {
            to += from.substring(from.lastIndexOf('/') + 1);
        }
        to = "/" + Storage.objectName(to);
        Dict res = storageReadMeta(to);
        Date modified = res.getDict("metadata").get("modified", Date.class);
        boolean binary = "binary".equals(format);
        if (binary) {
            expect.getDict("data").set("modified", modified); // Avoid assert timestamp mismatch
        }
        assertData(res.getDict("data"), to, binary, expect.getDict("data"));
        assertMetadata(res.getDict("metadata"), to, binary, mimeType);
        assertTrue(to + "modified", modified.after(created));
    }

    @Test
    public void testCopyRecursive() throws Exception {
        Array srcItems = (Array) CallContext.execute("system/storage/query", new Dict().set("path", "testdata/").set("hidden", true));
        boolean success = storageCopy("testdata/", "testdata-copy/", "recursive");
        assertTrue("recursive copy", success);
        for (Object o : srcItems) {
            Dict meta = (Dict) o;
            String src = meta.get("path", String.class);
            String dst = src.replaceFirst("^/testdata/", "/testdata-copy/");
            Dict expect = storageReadDict(src);
            Dict res = storageReadMeta(dst);
            assertNotNull("dst exists: " + dst, res);
            if ("binary".equals(meta.get("category"))) {
                expect.set("modified", res.getDict("data").get("modified"));
                assertData(res.getDict("data"), dst, true, expect);
            } else {
                assertData(res.getDict("data"), dst, false, expect);
                String mimeType = res.getDict("metadata").get("mimeType", String.class);
                assertEquals(dst + " mimeType", meta.get("mimeType", String.class), mimeType);
            }
        }
    }

    @Test
    public void testCopyUpdate() throws Exception {
        boolean success = storageCopy("testdata/jsondata", "testdata/copy-update", "update");
        assertTrue("copy is missing", success);
        success = storageCopy("testdata/jsondata", "testdata/copy-update", "update");
        assertFalse("copy is newer", success);
        Dict patch = new Dict().set("value", "modified");
        storageWrite("testdata/jsondata", new Dict().set("update", true), patch);
        success = storageCopy("testdata/jsondata", "testdata/copy-update", "update");
        assertTrue("copy is older", success);
    }

    private Dict buildDict(String id, boolean withEmpty) {
        Dict d = new Dict()
            .set("id", id)
            .set("a", "abc\u00E5\u00E4\u00F6")
            .set("b", 2)
            .set("c", false)
            .set("d", new Date(0))
            .set("object", new Dict().set("key", "value"))
            .set("array", Array.of("item 1", "item 2"));
        if (withEmpty) {
            d.set("empty", new Dict().add("obj", new Dict()).add("arr", new Array()));
        }
        return d;
    }

    private Object storageRead(String path, boolean metadata, boolean computed, Dict filter) throws Exception {
        if (metadata || computed || filter != null) {
            Dict opts = new Dict().set("path", path).set("metadata", metadata).set("computed", computed);
            opts.merge(filter);
            return CallContext.execute("system/storage/read", opts);
        } else {
            return CallContext.execute("system/storage/read", path);
        }
    }

    private Dict storageReadDict(String path) throws Exception {
        return (Dict) storageRead(path, false, false, null);
    }

    private Dict storageReadMeta(String path) throws Exception {
        return (Dict) storageRead(path, true, false, null);
    }

    private boolean storageWrite(String path, Dict opts, Object ...args) throws Exception {
        Object[] allArgs = new Object[args.length + 1];
        allArgs[0] = (opts != null) ? opts.set("path", path) : path;
        System.arraycopy(args, 0, allArgs, 1, args.length);
        return (Boolean) CallContext.execute("system/storage/write", allArgs);
    }

    private boolean storageCopy(String src, String dst, String format) throws Exception {
        return (Boolean) CallContext.execute("system/storage/copy", src, dst, format);
    }

    private void assertMetadata(Dict res, String path, boolean binary, String mimeType) throws Exception {
        if (binary) {
            assertEquals(path + " category", "binary", res.get("category"));
            assertThat(path + " class", res.get("class", String.class), containsString("core.data.Binary"));
        } else {
            assertEquals(path + " category", "object", res.get("category"));
            assertThat(path + " class", res.get("class", String.class), containsString("core.data.Dict"));
        }
        assertEquals(path + " path", path, res.get("path"));
        assertEquals(path + " mimeType", mimeType, res.get("mimeType"));
        assertThat(path + " modified", res.get("modified", String.class), matchesRegex("^@\\d+$"));
        assertTrue(path + " size", res.get("size", Long.class) > 0L);
    }

    private void assertData(Dict res, String path, boolean binary, Dict expect) throws Exception {
        if (binary) {
            assertEquals(path + " type", "file", res.get("type"));
            assertEquals(path + " path", path, res.get("path").toString());
            assertTrue(path + " name", path.endsWith(res.get("name", String.class)));
            assertEquals(path + " mimeType", expect.get("mimeType"), res.get("mimeType"));
            assertEquals(path + " modified", expect.get("modified"), res.get("modified"));
            assertEquals(path + " size", expect.get("size"), res.get("size"));
        } else {
            assertEquals(path, serialize(expect, false), serialize(res, false));
        }
    }

    private void assertDataList(Array res, String path, int binaryCount, int objectCount) throws Exception {
        int binaries = 0;
        int objects = 0;
        for (Object o : res) {
            assertThat(path + " item", o, instanceOf(Dict.class));
            Dict dict = (Dict) o;
            if ("file".equals(dict.get("type", String.class))) {
                binaries++;
                String itemPath = dict.get("path").toString();
                Dict meta = (Dict) CallContext.execute("system/storage/query", itemPath);
                assertData(dict, itemPath, true, meta);
            } else {
                objects++;
                String id = dict.get("id", String.class);
                Dict data = buildDict(id, !"propsdata".equals(id));
                assertData(dict, "/" + path + id, false, data);
            }
        }
        assertEquals(path + " binaries", binaryCount, binaries);
        assertEquals(path + " objects", objectCount, objects);
    }
}
