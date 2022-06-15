/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2022 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.storage;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for the Path class.
 */
@SuppressWarnings("javadoc")
public class PathTest {

    @Test
    public void testEquals() {
        assertNotEquals(null, new Path(""));
        assertNotEquals("/", new Path(""));
        assertEquals(Path.ROOT, new Path(""));
        assertEquals(Path.ROOT, new Path("/"));
        assertNotEquals(Path.ROOT, new Path("/a"));
        assertEquals(new Path("/a"), new Path("/a"));
        assertNotEquals(new Path("/a"), new Path("/a/"));
    }

    @Test
    public void testToIdent() {
        Path p = new Path("/a/b/c/d");
        assertEquals("", Path.ROOT.toIdent(0));
        assertEquals("", Path.ROOT.toIdent(1));
        assertEquals("a/b/c/d", p.toIdent(0));
        assertEquals("c/d", p.toIdent(-2));
        assertEquals("b/c/d", p.toIdent(1));
        assertEquals("", p.toIdent(10));
    }

    @Test
    public void testStartsWith() {
        Path p = new Path("/a/b/c/d");
        assertTrue(p.startsWith(Path.ROOT));
        assertFalse(p.startsWith(new Path("/a/b/c")));
        assertTrue(p.startsWith(new Path("/a/b/c/")));
        assertTrue(p.startsWith(new Path("/a/b/c/d")));
        assertFalse(p.startsWith(new Path("/a/b/c/d/")));
        assertFalse(p.startsWith(new Path("/b/c/")));
        p = new Path("/a/");
        assertTrue(p.startsWith(Path.ROOT));
        assertFalse(p.startsWith(new Path("/a")));
        assertTrue(p.startsWith(new Path("/a/")));
        assertFalse(p.startsWith(new Path("/a/b/c/")));
        assertFalse(p.startsWith(new Path("/b/c/")));
    }

    @Test
    public void testDepth() {
        assertEquals(0, Path.ROOT.depth());
        assertEquals(0, new Path("/a").depth());
        assertEquals(1, new Path("/a/").depth());
        assertEquals(3, new Path("/a/b/c/d").depth());
    }

    @Test
    public void testParent() {
        Path p = new Path("/a/b/c/d");
        assertEquals(Path.ROOT, Path.ROOT.parent());
        assertEquals(new Path("/a/b/c/"), p.parent());
        assertEquals(new Path("/a/b/"), p.parent().parent());
        assertEquals(Path.ROOT, p.parent().parent().parent().parent());
    }

    @Test
    public void testChild() {
        assertEquals(new Path("/a"), Path.ROOT.child("a", false));
        assertNotEquals(new Path("/a"), Path.ROOT.child("a", true));
        assertEquals(new Path("/a/"), Path.ROOT.child("a", true));
    }

    @Test
    public void testDescendant() {
        Path p = new Path("/a/b/c/");
        assertEquals(Path.ROOT, Path.ROOT.descendant(Path.ROOT));
        assertEquals(p, p.descendant(Path.ROOT));
        assertEquals(new Path("/a/b/c/d"), p.descendant(new Path("/d")));
        assertEquals(new Path("/a/b/c/e/"), p.descendant(new Path("/e/")));
    }

    @Test
    public void testRelativeTo() {
        Path p = new Path("/a/b/c/d");
        assertEquals(p, p.relativeTo(Path.ROOT));
        assertEquals(new Path("/b/c/d"), p.relativeTo(new Path("/a/")));
        assertEquals(new Path("/c/d"), p.relativeTo(new Path("/a/b/")));
        assertEquals(new Path("../../a/b/c/d"), p.relativeTo(new Path("/a/b/c")));
        assertEquals(new Path("../../a/b/c/d"), p.relativeTo(new Path("/g/h/")));
        assertEquals(new Path("../../a/b/c/d"), p.relativeTo(new Path("/g/h/i")));
    }
}
