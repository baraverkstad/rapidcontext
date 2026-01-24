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

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class ConsoleHandlerTest {

    private PrintStream originalOut;
    private PrintStream originalErr;
    private ClosableByteArrayOutputStream capturedOut;
    private ClosableByteArrayOutputStream capturedErr;

    private static class ClosableByteArrayOutputStream extends ByteArrayOutputStream {
        boolean closed = false;
        @Override
        public void close() throws IOException {
            super.close();
            closed = true;
        }
    }

    @Before
    public void setUp() {
        originalOut = System.out;
        originalErr = System.err;
        capturedOut = new ClosableByteArrayOutputStream();
        capturedErr = new ClosableByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut));
        System.setErr(new PrintStream(capturedErr));
    }

    @After
    public void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void testDefaultConstructor() throws Exception {
        ConsoleHandler handler = new ConsoleHandler();
        assertEquals(handler.getLevel(), Level.ALL);
        assertThat(handler.getFormatter(), instanceOf(LogFormatter.class));
    }

    @Test
    public void testStreamSeparation() throws Exception {
        ConsoleHandler handler = new ConsoleHandler();
        handler.publish(new LogRecord(Level.SEVERE, "severe-msg"));
        handler.publish(new LogRecord(Level.WARNING, "warning-msg"));
        handler.publish(new LogRecord(Level.INFO, "info-msg"));
        handler.publish(new LogRecord(Level.FINE, "fine-msg"));
        handler.flush();

        String stdout = capturedOut.toString();
        String stderr = capturedErr.toString();

        assertThat(stdout, containsString("info-msg"));
        assertThat(stdout, containsString("fine-msg"));
        assertThat(stdout, not(containsString("warning-msg")));
        assertThat(stdout, not(containsString("severe-msg")));

        assertThat(stderr, containsString("warning-msg"));
        assertThat(stderr, containsString("severe-msg"));
        assertThat(stderr, not(containsString("info-msg")));
        assertThat(stderr, not(containsString("fine-msg")));
    }

    @Test
    public void testCustomErrorLevel() throws Exception {
        ConsoleHandler handler = new ConsoleHandler(Level.SEVERE);
        handler.publish(new LogRecord(Level.WARNING, "warning-on-stdout"));
        handler.publish(new LogRecord(Level.SEVERE, "severe-on-stderr"));
        handler.flush();

        assertThat(capturedOut.toString(), containsString("warning-on-stdout"));
        assertThat(capturedErr.toString(), containsString("severe-on-stderr"));
        assertThat(capturedErr.toString(), not(containsString("warning-on-stdout")));
    }

    @Test
    public void testClose() throws Exception {
        ConsoleHandler handler = new ConsoleHandler();
        handler.publish(new LogRecord(Level.SEVERE, "severe-msg"));
        handler.publish(new LogRecord(Level.INFO, "info-msg"));
        handler.close();
        assertFalse("System.out should not be closed", capturedOut.closed);
        assertFalse("System.err should not be closed", capturedErr.closed);
    }
}