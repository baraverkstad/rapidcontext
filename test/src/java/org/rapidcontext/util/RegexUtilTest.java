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

import org.junit.Test;

public class RegexUtilTest {

    @Test
    public void testFromGlob() {
        assertThat("abc", matchesRegex(RegexUtil.fromGlob("a?c")));
        assertThat("abbc", not(matchesRegex(RegexUtil.fromGlob("a?c"))));
        assertThat("bbc", not(matchesRegex(RegexUtil.fromGlob("a?c"))));
        assertThat("ac", matchesRegex(RegexUtil.fromGlob("a*c")));
        assertThat("abc", matchesRegex(RegexUtil.fromGlob("a*c")));
        assertThat("abcc", matchesRegex(RegexUtil.fromGlob("a*c")));
        assertThat("ab/cc", not(matchesRegex(RegexUtil.fromGlob("a*c"))));
        assertThat("ab/cc", matchesRegex(RegexUtil.fromGlob("a**c")));
        assertThat("a/b/c/c", matchesRegex(RegexUtil.fromGlob("a**c")));
        assertThat("a/b/c/c/d", not(matchesRegex(RegexUtil.fromGlob("a**c"))));
    }
}
