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
import static org.rapidcontext.util.FileUtil.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class FileUtilTest {

    private File tmpDir;

    @Before
    public void setUp() throws IOException {
        tmpDir = File.createTempFile("rapidcontext-test-file-util", "");
        tmpDir.delete();
        tmpDir.mkdirs();
    }

    @After
    public void tearDown() throws IOException {
        if (tmpDir.exists()) {
            delete(tmpDir);
        }
    }

    @Test
    public void testResolve() {
        File srcDir = new File("test/src/java/org/rapidcontext/util/");
        assertEquals(new File(srcDir, "FileUtilTest.java"), resolve(srcDir, "fileutiltest.JAVA"));
        assertEquals(new File(srcDir, "nonexistent.txt"), resolve(srcDir, "nonexistent.txt"));
    }

    @Test
    public void testReadText() throws IOException {
        File file = new File(tmpDir, "test.txt");
        String content = "Hello, World!";
        writeText(file, content);
        assertEquals(content + "\n", readText(file));
        try (FileInputStream is = new FileInputStream(file)) {
            assertEquals(content + "\n", readText(is));
        }
        assertThrows(IOException.class, () -> {
            readText(new File(tmpDir, "nonexistent.txt"));
        });
    }

    @Test
    public void testCopyFile() throws IOException {
        File srcFile = new File(tmpDir, "test.txt");
        File dstFile = new File(tmpDir, "copy.txt");
        String content = "Hello, World!\n";
        writeText(srcFile, content);
        copy(srcFile, dstFile);
        assertTrue(dstFile.exists());
        assertEquals(content, readText(dstFile));
    }

    @Test
    public void testCopyDirectory() throws IOException {
        File srcDir = new File(tmpDir, "src");
        srcDir.mkdirs();
        File subDir = new File(srcDir, "subdir");
        subDir.mkdirs();
        File file1 = new File(srcDir, "file1.txt");
        File file2 = new File(srcDir, "file2.txt");
        File file3 = new File(subDir, "file3.txt");
        writeText(file1, "Content 1");
        writeText(file2, "Content 2");
        writeText(file3, "Content 3");

        File dstDir = new File(tmpDir, "dst");
        copy(srcDir, dstDir);
        assertTrue(dstDir.exists());
        assertTrue(dstDir.isDirectory());
        assertTrue(new File(dstDir, "file1.txt").exists());
        assertTrue(new File(dstDir, "file2.txt").exists());
        assertTrue(new File(dstDir, "subdir").exists());
        assertTrue(new File(dstDir, "subdir/file3.txt").exists());
        assertEquals("Content 1\n", readText(new File(dstDir, "file1.txt")));
        assertEquals("Content 2\n", readText(new File(dstDir, "file2.txt")));
        assertEquals("Content 3\n", readText(new File(dstDir, "subdir/file3.txt")));
    }

    @Test
    public void testMove() throws IOException {
        File srcFile = new File(tmpDir, "src-file.txt");
        File dstFile = new File(tmpDir, "dst-file.txt");
        String content = "Content\n";
        writeText(srcFile, content);

        move(srcFile, dstFile);
        assertFalse(srcFile.exists());
        assertTrue(dstFile.exists());
        assertEquals(content, readText(dstFile));

        // Test overwrite
        content = "New content\n";
        writeText(srcFile, content);
        move(srcFile, dstFile);
        assertFalse(srcFile.exists());
        assertTrue(dstFile.exists());
        assertEquals(content, readText(dstFile));
    }

    @Test
    public void testCopyInputToFile() throws IOException {
        String content = "Hello, World!";
        File dstFile = new File(tmpDir, "stream-copy.txt");
        try (InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            copy(is, dstFile);
        }
        assertTrue(dstFile.exists());
        assertEquals(content + "\n", readText(dstFile));
    }

    @Test
    public void testCopyInputToOutput() throws IOException {
        byte[] bytes = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            copy(is, os);
        }
        assertArrayEquals(bytes, os.toByteArray());
    }

    @Test
    public void testDelete() throws IOException {
        File testDir = new File(tmpDir, "test-delete");
        testDir.mkdirs();
        File subDir = new File(testDir, "subdir");
        subDir.mkdirs();
        File file1 = new File(testDir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        file1.createNewFile();
        file2.createNewFile();

        delete(null);
        assertThrows(IOException.class, () -> {
            delete(new File(testDir, "nonexistent.txt"));
        });
        assertTrue(testDir.exists());
        assertTrue(subDir.exists());
        assertTrue(file1.exists());
        assertTrue(file2.exists());

        delete(file1);
        assertTrue(testDir.exists());
        assertTrue(subDir.exists());
        assertFalse(file1.exists());
        assertTrue(file2.exists());

        delete(subDir);
        assertTrue(testDir.exists());
        assertFalse(subDir.exists());
        assertFalse(file1.exists());
        assertFalse(file2.exists());
    }

    @Test
    public void testDeleteFiles() throws IOException {
        File testDir = new File(tmpDir, "test-delete-files");
        testDir.mkdirs();
        File subDir = new File(testDir, "subdir");
        subDir.mkdirs();
        File file1 = new File(testDir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        file1.createNewFile();
        file2.createNewFile();

        deleteFiles(null);
        deleteFiles(new File(testDir, "nonexistent"));
        assertTrue(testDir.exists());
        assertTrue(subDir.exists());
        assertTrue(file1.exists());
        assertTrue(file2.exists());

        deleteFiles(subDir);
        assertTrue(testDir.exists());
        assertTrue(subDir.exists());
        assertTrue(file1.exists());
        assertFalse(file2.exists());

        deleteFiles(testDir);
        assertTrue(testDir.exists());
        assertFalse(subDir.exists());
        assertFalse(file1.exists());
        assertFalse(file2.exists());
    }

    @Test
    public void testDeleteEmptyDirs() throws IOException {
        File testDir = new File(tmpDir, "test-delete-empty-dirs");
        testDir.mkdirs();
        File subDir = new File(testDir, "subdir");
        subDir.mkdirs();
        File empty = new File(testDir, "empty1");
        empty.mkdirs();
        File file1 = new File(testDir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        file1.createNewFile();
        file2.createNewFile();

        deleteEmptyDirs(subDir);
        assertTrue(testDir.exists());
        assertTrue(subDir.exists());
        assertTrue(empty.exists());

        deleteEmptyDirs(testDir);
        assertTrue(testDir.exists());
        assertTrue(subDir.exists());
        assertFalse(empty.exists());

        file1.delete();
        file2.delete();
        deleteEmptyDirs(testDir);
        assertTrue(testDir.exists());
        assertFalse(subDir.exists());
        assertFalse(empty.exists());
    }

    @Test
    public void testTempFile() throws IOException {
        File tmp = tempFile("test.txt");
        assertTrue(tmp.getName().startsWith("test-"));
        assertTrue(tmp.getName().endsWith(".txt"));
        tmp = tempFile("a.b.c.d.tmp");
        assertTrue(tmp.getName().startsWith("a.b.c.d-"));
        assertTrue(tmp.getName().endsWith(".tmp"));
        tmp = tempFile("test");
        assertTrue(tmp.getName().startsWith("test-"));
        assertFalse(tmp.getName().contains("."));
        assertFalse(tmp.getParent().contains(tmpDir.toString()));
        setTempDir(tmpDir);
        tmp = tempFile("test");
        assertEquals(canonical(tmpDir), tmp.getParentFile());
    }

    private void writeText(File dst, String content) throws IOException {
        try (FileOutputStream os = new FileOutputStream(dst)) {
            os.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
