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

package org.rapidcontext.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ZipUtilTest {

    private File tmpDir;

    @Before
    public void setUp() throws IOException {
        tmpDir = File.createTempFile("rapidcontext-test-zip-util", "");
        tmpDir.delete();
        tmpDir.mkdirs();
    }

    @After
    public void tearDown() throws IOException {
        if (tmpDir.exists()) {
            FileUtil.delete(tmpDir);
        }
    }

    @Test
    public void testUnpackZip() throws IOException {
        File zipFile = new File(tmpDir, "test.zip");
        byte[] data1 = "Content 1".getBytes(StandardCharsets.UTF_8);
        byte[] data2 = new byte[0];
        byte[] data3 = new byte[] { 0x00, 0x01, 0x7F, -0x7f };
        byte[] data4 = "Content 4".getBytes(StandardCharsets.UTF_8);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("file1.txt"));
            zos.write(data1);
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("/file-2.txt"));
            zos.write(data2);
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("subdir/.file_3.txt"));
            zos.write(data3);
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("/subdir//file 4.txt"));
            zos.write(data4);
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("///emptydir/"));
            zos.closeEntry();
        }

        File dstDir = new File(tmpDir, "dst");
        ZipUtil.unpackZip(zipFile, dstDir);
        assertTrue(dstDir.exists());
        assertTrue(dstDir.isDirectory());
        assertFile(new File(dstDir, "file1.txt"), data1);
        assertFile(new File(dstDir, "file-2.txt"), data2);
        assertFile(new File(dstDir, "subdir/.file_3.txt"), data3);
        assertFile(new File(dstDir, "subdir/file 4.txt"), data4);
        assertTrue(new File(dstDir, "emptydir").exists());
        assertTrue(new File(dstDir, "emptydir").isDirectory());
    }

    private void assertFile(File file, byte[] data) throws IOException {
        assertTrue(file.exists());
        try (FileInputStream is = new FileInputStream(file)) {
            assertArrayEquals(data, is.readAllBytes());
        }
    }

    @Test
    public void testUnpackZipEdgeCases() throws IOException {
        File zipFile = new File(tmpDir, "empty.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // No entries added
            zos.finish();
        }
        File dstDir = new File(tmpDir, "dst");
        ZipUtil.unpackZip(zipFile, dstDir);
        assertFalse(dstDir.exists());

        assertZipException(new File(tmpDir, "nonexistent.zip"));
        assertZipMalformedPath("../malicious.txt");
        assertZipMalformedPath("dir/../../malicious.txt");
        assertZipMalformedPath(".");
        assertZipMalformedPath("..");
        assertZipMalformedPath("dir/.");
        assertZipMalformedPath("dir/..");
        assertZipMalformedPath("dir/../malicious.txt");
    }

    private void assertZipException(File file) {
        assertThrows(IOException.class, () -> {
            ZipUtil.unpackZip(file, new File(tmpDir, "dst"));
        });
    }

    private void assertZipMalformedPath(String path) throws IOException {
        File zipFile = new File(tmpDir, "malicious.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry(path));
            zos.write("Malicious content".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        assertZipException(zipFile);
    }
}
