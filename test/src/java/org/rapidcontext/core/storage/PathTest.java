/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2024 Per Cederberg. All rights reserved.
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
    public void testIsHidden() {
        assertFalse(Path.ROOT.isHidden());
        assertFalse(Path.from("/a/b/c/d").isHidden());
        assertTrue(Path.from("/.a/b/c/d").isHidden());
        assertTrue(Path.from("/a/.b/c/d").isHidden());
        assertTrue(Path.from("/a/b/.c/d").isHidden());
        assertTrue(Path.from("/a/b/c/.d").isHidden());
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
    public void testRemovePrefix() {
        assertEquals(Path.from("/a/b/c"), Path.from("/a/b/c").removePrefix(Path.ROOT));
        assertEquals(Path.from("/b/c"), Path.from("/a/b/c").removePrefix(Path.from("/a/")));
        assertEquals(Path.from("/c/"), Path.from("/a/b/c/").removePrefix(Path.from("/a/b/")));
        assertEquals(Path.from("/"), Path.from("/a/b/c").removePrefix(Path.from("/a/b/c")));
        assertEquals(Path.from("/a/b/c"), Path.from("/a/b/c").removePrefix(Path.from("/a/b/c/d")));
        assertEquals(Path.from("/a/b/c"), Path.from("/a/b/c").removePrefix(Path.from("/g/h/")));
    }

    @Test
    public void testResolveStr() {
        assertEquals(Path.from("/a"), Path.resolve(Path.ROOT, "/a"));
        assertEquals(Path.from("/a/"), Path.resolve(Path.ROOT, "/a/"));
        assertEquals(Path.from("/a/"), Path.resolve(Path.ROOT, "//a///"));
        assertEquals(Path.from("/a/b"), Path.resolve(Path.ROOT, "//a///b"));
        assertEquals(Path.from("/a/b/c/d"), Path.resolve(Path.from("/a/b/"), "/c/d"));
        assertEquals(Path.from("/a/c/d"), Path.resolve(Path.from("/a/b"), "c/d"));
        assertEquals(Path.from("/a/c/d"), Path.resolve(Path.from("/a/b/"), "../c/d"));
        assertEquals(Path.from("/c/d"), Path.resolve(Path.from("/a/b/"), "../../c/d"));
        assertEquals(Path.from("/../c/d"), Path.resolve(Path.from("/a/b/"), "../../../c/d"));
        assertEquals(Path.from("/../../c/d"), Path.resolve(Path.from("/a/"), "../../../c/d"));
    }

    @Test
    public void testResolvePath() {
        assertEquals(Path.ROOT, Path.resolve(Path.ROOT, (Path) null));
        assertEquals(Path.ROOT, Path.resolve(Path.ROOT, Path.ROOT));
        assertEquals(Path.from("/a"), Path.resolve(Path.from("/a"), Path.ROOT));
        assertEquals(Path.from("/b/c"), Path.resolve(Path.from("/a"), Path.from("/b/c")));
        assertEquals(Path.from("/a/b/c/"), Path.resolve(Path.from("/a/"), Path.from("/b/c/")));
        assertEquals(Path.from("/a/b/d"), Path.resolve(Path.from("/a/b/c/"), Path.from("../d")));
        assertEquals(Path.from("/a/e/"), Path.resolve(Path.from("/a/b/c"), Path.from("../d/../e/")));
    }
}
