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
import static org.rapidcontext.core.type.Role.KEY_ACCESS;
import static org.rapidcontext.core.type.Role.KEY_AUTO;
import static org.rapidcontext.core.type.Role.KEY_ID;
import static org.rapidcontext.core.type.Role.KEY_TYPE;
import static org.rapidcontext.core.type.Role.PERM_ALL;
import static org.rapidcontext.core.type.Role.PERM_INTERNAL;
import static org.rapidcontext.core.type.Role.PERM_READ;
import static org.rapidcontext.core.type.Role.PERM_WRITE;
import static org.rapidcontext.core.storage.StorableObject.PREFIX_COMPUTED;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Test;
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
        assertEquals(PERM_INTERNAL, entry.get(ACCESS_PERMISSION));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInit() {
        Dict none = pathAccess("/data/confidential/**", "read none");
        Dict all = regexAccess("^/admin/.*$", "ALL");
        Dict blank = pathAccess("data/*/foo", "   ");
        Dict custom = pathAccess("data/**", "read, write;  custom-1\tCUSTOM-2");
        Role role = buildRole("test/role", none, all, blank, custom);
        role.init();

        assertThat(none.get(PREFIX_COMPUTED + ACCESS_REGEX), instanceOf(Pattern.class));
        Pattern pattern = none.get(PREFIX_COMPUTED + ACCESS_REGEX, Pattern.class);
        assertTrue(pattern.matcher("data/CONFIDENTIAL/report").matches());
        assertFalse(pattern.matcher("data/confidential-info").matches());
        assertThat(none.get(PREFIX_COMPUTED + ACCESS_PERMISSION), instanceOf(Set.class));
        Set<String> perms = none.get(PREFIX_COMPUTED + ACCESS_PERMISSION, HashSet.class);
        assertTrue(perms.isEmpty());

        assertThat(all.get(PREFIX_COMPUTED + ACCESS_REGEX), instanceOf(Pattern.class));
        pattern = all.get(PREFIX_COMPUTED + ACCESS_REGEX, Pattern.class);
        assertEquals("^admin/.*$", pattern.pattern());
        assertThat(all.get(PREFIX_COMPUTED + ACCESS_PERMISSION), instanceOf(Set.class));
        perms = all.get(PREFIX_COMPUTED + ACCESS_PERMISSION, HashSet.class);
        assertEquals(Set.of(PERM_ALL), perms);

        assertThat(blank.get(PREFIX_COMPUTED + ACCESS_REGEX), instanceOf(Pattern.class));
        assertThat(blank.get(PREFIX_COMPUTED + ACCESS_PERMISSION), instanceOf(Set.class));
        perms = blank.get(PREFIX_COMPUTED + ACCESS_PERMISSION, HashSet.class);
        assertTrue(perms.isEmpty());

        assertThat(custom.get(PREFIX_COMPUTED + ACCESS_REGEX), instanceOf(Pattern.class));
        assertThat(custom.get(PREFIX_COMPUTED + ACCESS_PERMISSION), instanceOf(Set.class));
        perms = custom.get(PREFIX_COMPUTED + ACCESS_PERMISSION, HashSet.class);
        assertEquals(Set.of(PERM_INTERNAL, PERM_READ, PERM_WRITE, "custom-1", "custom-2"), perms);
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
        Role role = buildRole("test-role",
            pathAccess("/data/confidential/**", "none"),
            pathAccess("data/visible/info", "internal"),
            pathAccess("data/**", "read"),
            regexAccess("^admin/.*$", "all")
        );
        role.init();
        assertFalse(role.hasAccess("data/confidential/report", PERM_READ));
        assertTrue(role.hasAccess("data/visible/info", PERM_READ));
        assertTrue(role.hasAccess("data/visible/info", PERM_INTERNAL));
        assertFalse(role.hasAccess("data/visible/info", PERM_WRITE));
        assertTrue(role.hasAccess("data/other", PERM_INTERNAL));
        assertTrue(role.hasAccess("admin/user", PERM_WRITE));
        assertTrue(role.hasAccess("admin/user", "custom-permission"));
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
}
