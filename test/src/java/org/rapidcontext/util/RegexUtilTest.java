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

import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.rapidcontext.util.RegexUtil.*;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class RegexUtilTest {

    @Test
    public void testFromGlob() {
        assertThat("abc", matchesRegex(fromGlob("a?c")));
        assertThat("abbc", not(matchesRegex(fromGlob("a?c"))));
        assertThat("bbc", not(matchesRegex(fromGlob("a?c"))));
        assertThat("ac", matchesRegex(fromGlob("a*c")));
        assertThat("abc", matchesRegex(fromGlob("a*c")));
        assertThat("abcc", matchesRegex(fromGlob("a*c")));
        assertThat("ab/cc", not(matchesRegex(fromGlob("a*c"))));
        assertThat("ab/cc", matchesRegex(fromGlob("a**c")));
        assertThat("a/b/c/c", matchesRegex(fromGlob("a**c")));
        assertThat("a/b/c/c/d", not(matchesRegex(fromGlob("a**c"))));
    }
}
