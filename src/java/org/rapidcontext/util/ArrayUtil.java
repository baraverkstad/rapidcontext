/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2017 Per Cederberg. All rights reserved.
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

import java.util.Iterator;
import java.util.Map;

/**
 * A set of utility methods for handling native arrays. This is an
 * add-on to the java.util.Arrays class.
 *
 * @author   Per Cederberg
 * @version  1.0
 *
 * @see java.util.Arrays
 */
public class ArrayUtil {

    /**
     * Searches for the first matching entry in an array.
     *
     * @param a              the array object
     * @param key            the search key
     *
     * @return the first matching index, or
     *         -1 if not found
     */
    public static int indexOf(Object[] a, Object key) {
        for (int i = 0; a != null && i < a.length; i++) {
            if (a[i] == null && key == null) {
                return i;
            } else if (a[i] != null && a[i].equals(key)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns an array of all the keys in a map.
     *
     * @param m              the map object
     *
     * @return the array of keys
     */
    public static Object[] keys(Map m) {
        return keys(m, new Object[m.size()]);
    }

    /**
     * Returns an array of all the keys in a map.
     *
     * @param m              the map object
     * @param keys           the array of keys
     *
     * @return the array of keys
     */
    public static Object[] keys(Map m, Object[] keys) {
        Iterator iter = m.keySet().iterator();
        for (int i = 0; iter.hasNext(); i++) {
            keys[i] = iter.next();
        }
        return keys;
    }

    /**
     * Returns a string array of the keys in a map.
     *
     * @param m              the map object
     *
     * @return the array of keys
     *
     * @throws ClassCastException if one of the keys wasn't a string
     */
    public static String[] stringKeys(Map m) {
        return (String[]) ArrayUtil.keys(m, new String[m.size()]);
    }
}
