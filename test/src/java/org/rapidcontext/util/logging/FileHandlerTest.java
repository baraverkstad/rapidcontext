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

package org.rapidcontext.util.logging;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rapidcontext.util.FileUtil;

@SuppressWarnings("javadoc")
public class FileHandlerTest {

    private File tempDir;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("rapidcontext-test-").toFile();
    }

    @After
    public void tearDown() throws Exception {
        FileUtil.delete(tempDir);
    }

    @Test
    public void testDefaultConstructor() throws Exception {
        FileHandler handler = new FileHandler();
        try {
            assertEquals(Level.ALL, handler.getLevel());
            assertThat(handler.getFormatter(), instanceOf(LogFormatter.class));
        } finally {
            handler.close();
        }
    }

    @Test
    public void testLoggingToFile() throws Exception {
        File logFile = new File(tempDir, "test.log");
        FileHandler handler = new FileHandler(logFile.getAbsolutePath(), true);
        try {
            handler.publish(new LogRecord(Level.INFO, "test-msg-1"));
            handler.close();
            assertTrue("Log file should exist", logFile.exists());
            assertThat(FileUtil.readText(logFile), containsString("test-msg-1"));
        } finally {
            handler.close();
        }
    }

    @Test
    public void testFileRotation() throws Exception {
        String pattern = tempDir.getAbsolutePath() + "/rotate-%d{yyyyMMdd-HHmm}.log";
        FileHandler handler = new FileHandler(pattern, true);
        try {
            long now = System.currentTimeMillis();
            handler.open(now);
            handler.publish(new LogRecord(Level.INFO, "msg-1"));
            handler.flush();

            // Simulate rotation by jumping 2 minutes ahead
            handler.open(now + 2 * DateUtils.MILLIS_PER_MINUTE);
            handler.publish(new LogRecord(Level.INFO, "msg-2"));
            handler.flush();

            File[] logFiles = tempDir.listFiles();
            assertTrue("Should have at least 2 log files", logFiles != null && logFiles.length >= 2);
        } finally {
            handler.close();
        }
    }
}
