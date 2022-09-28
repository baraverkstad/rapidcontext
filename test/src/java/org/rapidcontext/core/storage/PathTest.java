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
    public void testFrom() {
        assertTrue(Path.from("").isRoot());
        assertTrue(Path.from("/").isRoot());
        assertTrue(Path.from("//").isRoot());
        assertFalse(Path.from("/a").isIndex());
        assertTrue(Path.from("/a/").isIndex());
    }

    @Test
    public void testEquals() {
        assertNotEquals(null, Path.from(""));
        assertNotEquals("/", Path.from(""));
        assertEquals(Path.ROOT, Path.from(""));
        assertEquals(Path.ROOT, Path.from("/"));
        assertNotEquals(Path.ROOT, Path.from("/a"));
        assertEquals(Path.from("/a"), Path.from("/a"));
        assertNotEquals(Path.from("/a"), Path.from("/a/"));
    }

    @Test
    public void testToIdent() {
        Path p = Path.from("/a/b/c/d");
        assertEquals("", Path.ROOT.toIdent(0));
        assertEquals("", Path.ROOT.toIdent(1));
        assertEquals("a/b/c/d", p.toIdent(0));
        assertEquals("c/d", p.toIdent(-2));
        assertEquals("b/c/d", p.toIdent(1));
        assertEquals("", p.toIdent(10));
    }

    @Test
    public void testStartsWith() {
        Path p = Path.from("/a/b/c/d");
        assertTrue(p.startsWith(Path.ROOT));
        assertFalse(p.startsWith(Path.from("/a/b/c")));
        assertTrue(p.startsWith(Path.from("/a/b/c/")));
        assertTrue(p.startsWith(Path.from("/a/b/c/d")));
        assertFalse(p.startsWith(Path.from("/a/b/c/d/")));
        assertFalse(p.startsWith(Path.from("/b/c/")));
        p = Path.from("/a/");
        assertTrue(p.startsWith(Path.ROOT));
        assertFalse(p.startsWith(Path.from("/a")));
        assertTrue(p.startsWith(Path.from("/a/")));
        assertFalse(p.startsWith(Path.from("/a/b/c/")));
        assertFalse(p.startsWith(Path.from("/b/c/")));
    }

    @Test
    public void testDepth() {
        assertEquals(0, Path.ROOT.depth());
        assertEquals(0, Path.from("/a").depth());
        assertEquals(1, Path.from("/a/").depth());
        assertEquals(3, Path.from("/a/b/c/d").depth());
    }

    @Test
    public void testParent() {
        Path p = Path.from("/a/b/c/d");
        assertEquals(Path.ROOT, Path.ROOT.parent());
        assertEquals(Path.from("/a/b/c/"), p.parent());
        assertEquals(Path.from("/a/b/"), p.parent().parent());
        assertEquals(Path.ROOT, p.parent().parent().parent().parent());
    }

    @Test
    public void testChild() {
        assertEquals(Path.from("/a"), Path.ROOT.child("a", false));
        assertNotEquals(Path.from("/a"), Path.ROOT.child("a", true));
        assertEquals(Path.from("/a/"), Path.ROOT.child("a", true));
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
        Path p = Path.from("/a/b/c/d");
        assertEquals(p, p.relativeTo(Path.ROOT));
        assertEquals(Path.from("/b/c/d"), p.relativeTo(Path.from("/a/")));
        assertEquals(Path.from("/c/d"), p.relativeTo(Path.from("/a/b/")));
        assertEquals(Path.from("../../a/b/c/d"), p.relativeTo(Path.from("/a/b/c")));
        assertEquals(Path.from("../../a/b/c/d"), p.relativeTo(Path.from("/g/h/")));
        assertEquals(Path.from("../../a/b/c/d"), p.relativeTo(Path.from("/g/h/i")));
    }
}
