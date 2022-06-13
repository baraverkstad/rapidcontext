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
        assertNotEquals(new Path(""), null);
        assertNotEquals(new Path(""), "/");
        assertEquals(new Path(""), Path.ROOT);
        assertEquals(new Path("/"), Path.ROOT);
        assertNotEquals(new Path("/a"), Path.ROOT);
        assertEquals(new Path("/a"), new Path("/a"));
        assertNotEquals(new Path("/a"), new Path("/a/"));
    }

    @Test
    public void testToIdent() {
        Path p = new Path("/a/b/c/d");
        assertEquals(p.toIdent(0), "a/b/c/d");
        assertEquals(p.toIdent(-2), "c/d");
        assertEquals(p.toIdent(1), "b/c/d");
        assertEquals(p.toIdent(10), "");
    }

    @Test
    public void testStartWith() {
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
        assertEquals(Path.ROOT.depth(), 0);
        assertEquals(new Path("/a").depth(), 0);
        assertEquals(new Path("/a/").depth(), 1);
        assertEquals(new Path("/a/b/c/d").depth(), 3);
    }

    @Test
    public void testParent() {
        Path p = new Path("/a/b/c/d");
        assertEquals(Path.ROOT.parent(), Path.ROOT);
        assertEquals(p.parent(), new Path("/a/b/c/"));
        assertEquals(p.parent().parent(), new Path("/a/b/"));
        assertEquals(p.parent().parent().parent().parent(), Path.ROOT);
    }

    @Test
    public void testChild() {
        assertEquals(Path.ROOT.child("a", false), new Path("/a"));
        assertNotEquals(Path.ROOT.child("a", true), new Path("/a"));
        assertEquals(Path.ROOT.child("a", true), new Path("/a/"));
    }

    @Test
    public void testDescendant() {
        Path p = new Path("/a/b/c/");
        assertEquals(Path.ROOT.descendant(Path.ROOT), Path.ROOT);
        assertEquals(p.descendant(Path.ROOT), p);
        assertEquals(p.descendant(new Path("/d")), new Path("/a/b/c/d"));
        assertEquals(p.descendant(new Path("/e/")), new Path("/a/b/c/e/"));
    }

    @Test
    public void testSubPath() {
        Path p = new Path("/a/b/c/d");
        assertEquals(p.subPath(0), p);
        assertEquals(p.subPath(-3), p);
        assertEquals(p.subPath(1), new Path("/b/c/d"));
        assertEquals(p.subPath(4), new Path("/"));
        assertEquals(p.subPath(7), new Path("/"));
    }
}
