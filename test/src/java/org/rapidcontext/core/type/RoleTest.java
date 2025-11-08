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

package org.rapidcontext.core.type;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.rapidcontext.core.type.Role.ACCESS_PATH;
import static org.rapidcontext.core.type.Role.ACCESS_PERMISSION;
import static org.rapidcontext.core.type.Role.ACCESS_REGEX;
import static org.rapidcontext.core.type.Role.ACCESS_VIA;
import static org.rapidcontext.core.type.Role.KEY_ACCESS;
import static org.rapidcontext.core.type.Role.KEY_AUTO;
import static org.rapidcontext.core.type.Role.KEY_ID;
import static org.rapidcontext.core.type.Role.KEY_TYPE;
import static org.rapidcontext.core.type.Role.PERM_ALL;
import static org.rapidcontext.core.type.Role.PERM_READ;
import static org.rapidcontext.core.type.Role.PERM_WRITE;
import static org.rapidcontext.core.storage.StorableObject.PREFIX_COMPUTED;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Test;
import org.rapidcontext.core.ctx.Context;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;

@SuppressWarnings("javadoc")
public class RoleTest {

    @Test
    public void testNormalize() {
        // Check type and id normalization
        Dict data = new Dict();
        assertSame(data, Role.normalize("test/role", data));
        assertEquals("role", data.get(KEY_TYPE, String.class));
        assertEquals("test/role", data.get(KEY_ID, String.class));

        // Check legacy access path normalization
        Dict entry = new Dict().set("type", "app").set("name", "start");
        data.set(KEY_ACCESS, Array.of(entry));
        Role.normalize("test/role", data);
        assertFalse(entry.containsKey("type"));
        assertFalse(entry.containsKey("name"));
        assertEquals("app/start", entry.get(ACCESS_PATH));
        assertEquals(PERM_READ, entry.get(ACCESS_PERMISSION));

        // Check legacy access regex normalization
        entry = new Dict().set("type", "procedure").set("regexp", "test/.*");
        data.set(KEY_ACCESS, Array.of(entry));
        Role.normalize("test/role", data);
        assertFalse(entry.containsKey("type"));
        assertFalse(entry.containsKey("regexp"));
        assertEquals("procedure/test/.*", entry.get(ACCESS_REGEX));
        assertEquals(PERM_READ, entry.get(ACCESS_PERMISSION));

        // Check regexp typo normalization
        entry = new Dict().set("regexp", "foo/.*");
        data.set(KEY_ACCESS, Array.of(entry));
        Role.normalize("test/role", data);
        assertFalse(entry.containsKey("regexp"));
        assertEquals("foo/.*", entry.get(ACCESS_REGEX));

        // Check legacy caller normalization
        entry = new Dict().set("caller", "system");
        data.set(KEY_ACCESS, Array.of(entry));
        Role.normalize("test/role", data);
        assertFalse(entry.containsKey("caller"));
        assertEquals(PERM_READ, entry.get(ACCESS_PERMISSION));
        assertEquals(Role.VIA_NON_INTERNAL, entry.get(ACCESS_VIA));

        // Check legacy internal permission normalization
        entry = pathAccess("data/internal/**", "internal");
        data.set(KEY_ACCESS, Array.of(entry));
        Role.normalize("test/role", data);
        assertEquals(PERM_READ, entry.get(ACCESS_PERMISSION));
        assertEquals(Role.VIA_NON_INTERNAL, entry.get(ACCESS_VIA));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInit() {
        Dict none = pathAccess("/data/confidential/**", "read none");
        Dict all = regexAccess("^/admin/.*$", "ALL");
        Dict blank = pathAccess("data/*/foo", "   ");
        Dict standard = pathAccess("data/**", "read, write;  custom-1\tCUSTOM-2");
        Dict internal = pathAccess("data/internal/**", "internal");
        Dict via = pathAccess("data/**", "read").set(ACCESS_VIA, "procedure/reports/**");
        Role role = buildRole("test/role", none, all, blank, standard, internal, via);
        role.init();

        assertThat(none.get(PREFIX_COMPUTED + ACCESS_REGEX), instanceOf(Pattern.class));
        Pattern pattern = none.get(PREFIX_COMPUTED + ACCESS_REGEX, Pattern.class);
        assertTrue(pattern.matcher("data/CONFIDENTIAL/report").matches());
        assertFalse(pattern.matcher("data/confidential-info").matches());
        assertThat(none.get(PREFIX_COMPUTED + ACCESS_PERMISSION), instanceOf(Set.class));
        Set<String> perms = none.get(PREFIX_COMPUTED + ACCESS_PERMISSION, HashSet.class);
        assertTrue(perms.isEmpty());
        assertNull(none.get(PREFIX_COMPUTED + ACCESS_VIA));

        assertThat(all.get(PREFIX_COMPUTED + ACCESS_REGEX), instanceOf(Pattern.class));
        pattern = all.get(PREFIX_COMPUTED + ACCESS_REGEX, Pattern.class);
        assertEquals("^admin/.*$", pattern.pattern());
        assertThat(all.get(PREFIX_COMPUTED + ACCESS_PERMISSION), instanceOf(Set.class));
        perms = all.get(PREFIX_COMPUTED + ACCESS_PERMISSION, HashSet.class);
        assertEquals(Set.of(PERM_ALL), perms);
        assertNull(all.get(PREFIX_COMPUTED + ACCESS_VIA));

        assertThat(blank.get(PREFIX_COMPUTED + ACCESS_REGEX), instanceOf(Pattern.class));
        assertThat(blank.get(PREFIX_COMPUTED + ACCESS_PERMISSION), instanceOf(Set.class));
        perms = blank.get(PREFIX_COMPUTED + ACCESS_PERMISSION, HashSet.class);
        assertTrue(perms.isEmpty());
        assertNull(blank.get(PREFIX_COMPUTED + ACCESS_VIA));

        assertThat(standard.get(PREFIX_COMPUTED + ACCESS_REGEX), instanceOf(Pattern.class));
        assertThat(standard.get(PREFIX_COMPUTED + ACCESS_PERMISSION), instanceOf(Set.class));
        perms = standard.get(PREFIX_COMPUTED + ACCESS_PERMISSION, HashSet.class);
        assertEquals(Set.of(PERM_READ, PERM_WRITE, "custom-1", "custom-2"), perms);
        assertNull(standard.get(PREFIX_COMPUTED + ACCESS_VIA));

        assertThat(internal.get(PREFIX_COMPUTED + ACCESS_REGEX), instanceOf(Pattern.class));
        assertThat(internal.get(PREFIX_COMPUTED + ACCESS_PERMISSION), instanceOf(Set.class));
        perms = internal.get(PREFIX_COMPUTED + ACCESS_PERMISSION, HashSet.class);
        assertEquals(Set.of(PERM_READ), perms);
        assertThat(internal.get(PREFIX_COMPUTED + ACCESS_VIA), instanceOf(Pattern.class));
        pattern = internal.get(PREFIX_COMPUTED + ACCESS_VIA, Pattern.class);
        assertTrue(pattern.matcher("procedure/reports/list").matches());
        assertFalse(pattern.matcher("procedure/system/procedure/call").matches());

        assertThat(via.get(PREFIX_COMPUTED + ACCESS_REGEX), instanceOf(Pattern.class));
        assertThat(via.get(PREFIX_COMPUTED + ACCESS_PERMISSION), instanceOf(Set.class));
        perms = via.get(PREFIX_COMPUTED + ACCESS_PERMISSION, HashSet.class);
        assertEquals(Set.of(PERM_READ), perms);
        assertThat(via.get(PREFIX_COMPUTED + ACCESS_VIA), instanceOf(Pattern.class));
        pattern = via.get(PREFIX_COMPUTED + ACCESS_VIA, Pattern.class);
        assertTrue(pattern.matcher("procedure/reports/list").matches());
        assertFalse(pattern.matcher("procedure/other/test").matches());
    }

    @Test
    public void testHasUser() {
        User user = new User("test-user");
        Role role = new Role("one", "role", new Dict().set(KEY_AUTO, "all"));
        assertTrue(role.hasUser(null));

        role = new Role("two", "role", new Dict().set(KEY_AUTO, "auth"));
        assertFalse(role.hasUser(null));
        assertTrue(role.hasUser(user));

        role = new Role("test-role", "role", new Dict());
        assertFalse(role.hasUser(user));
        user.setRoles(new String[] { "other", "test-role" });
        assertTrue(role.hasUser(user));
        user.setRoles(new String[] { "other" });
        assertFalse(role.hasUser(user));
    }

    @Test
    public void testHasAccess() {
        Role role = buildRole("via-role",
            pathAccess("/data/confidential/**", "none"),
            pathAccess("data/restricted/**", "read").set(ACCESS_VIA, "procedure/reports/**"),
            pathAccess("data/visible/**", "read"),
            pathAccess("data/visible/**", "write"),
            regexAccess("^admin/.*$", "all")
        );
        role.init();
        assertFalse(role.hasAccess("data/confidential/report", PERM_READ));
        assertFalse(role.hasAccess("data/restricted/overview", PERM_READ));
        TestContext.withContext(
            () -> assertTrue(role.hasAccess("data/restricted/overview", PERM_READ)),
            "procedure/reports/list"
        );
        TestContext.withContext(
            () -> assertFalse(role.hasAccess("data/restricted/overview", PERM_READ)),
            "procedure/system/procedure/call"
        );
        TestContext.withContext(
            () -> assertTrue(role.hasAccess("data/restricted/overview", PERM_READ)),
            "procedure/system/procedure/call",
            "procedure/reports/list"
        );
        assertFalse(role.hasAccess("data/restricted/overview", PERM_WRITE));
        assertTrue(role.hasAccess("data/visible/info", PERM_READ));
        assertTrue(role.hasAccess("data/visible/info", PERM_WRITE));
        assertTrue(role.hasAccess("admin/user", PERM_WRITE));
        assertTrue(role.hasAccess("admin/user", "custom-permission"));
        assertFalse(role.hasAccess("data/other/info", PERM_READ));
        assertFalse(role.hasAccess("unknown/path", PERM_READ));
    }

    private Role buildRole(String id, Dict... access) {
        return new Role(id, "role", new Dict().set(KEY_ACCESS, Array.of((Object[]) access)));
    }

    private Dict pathAccess(String path, String perm) {
        return new Dict().set(ACCESS_PATH, path).set(ACCESS_PERMISSION, perm);
    }

    private Dict regexAccess(String regex, String perm) {
        return new Dict().set(ACCESS_REGEX, regex).set(ACCESS_PERMISSION, perm);
    }

    private static final class TestContext extends Context {

        TestContext(String id) {
            super(id);
        }

        static void withContext(Runnable action, String... ids) {
            try {
                for (String id : ids) {
                    new TestContext(id).open();
                }
                action.run();
            } finally {
                while (Context.active() instanceof TestContext cx) {
                    cx.close();
                    if (cx == root) {
                        root = null;
                        break;
                    }
                }
            }
        }
    }
}
