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

package org.rapidcontext.core.data;

import java.util.Comparator;

/**
 * A dictionary value comparator. This comparator only compares the
 * values of two dictionary keys. All values returned which must
 * implement Comparable and be of compatible types.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class DictComparator implements Comparator<Object> {

    /**
     * The dictionary key name to compare.
     */
    private String key;

    /**
     * Creates a new dictionary comparator.
     *
     * @param key            the dictionary key name
     */
    public DictComparator(String key) {
        this.key = key;
    }

    /**
     * Compares two dictionaries by comparing their values for a
     * predefined property key.
     *
     * @param o1             the first object
     * @param o2             the second object
     *
     * @return a negative integer, zero, or a positive integer as the
     *         first argument is less than, equal to, or greater than
     *         the second
     *
     * @throws ClassCastException if the values were not comparable
     */
    public int compare(Object o1, Object o2) throws ClassCastException {
        Dict d1 = (Dict) o1;
        Dict d2 = (Dict) o2;
        if (d1 == null && d2 == null) {
            return 0;
        } else if (d1 == null) {
            return -1;
        } else if (d2 == null) {
            return 1;
        } else {
            return compareValues(d1.get(this.key), d2.get(this.key));
        }
    }

    /**
     * Compares two values according to generic comparison rules,
     * i.e. it checks for null values and otherwise uses the
     * Comparable interface.
     *
     * @param o1             the first object
     * @param o2             the second object
     *
     * @return a negative integer, zero, or a positive integer as the
     *         first argument is less than, equal to, or greater than
     *         the second
     *
     * @throws ClassCastException if the values were not comparable
     */
    @SuppressWarnings("unchecked")
    private int compareValues(Object o1, Object o2) throws ClassCastException {
        Comparable<Object> c1 = (Comparable<Object>) o1;
        Comparable<Object> c2 = (Comparable<Object>) o2;
        if (c1 == null && c2 == null) {
            return 0;
        } else if (c1 == null) {
            return -1;
        } else if (c2 == null) {
            return 1;
        } else {
            return c1.compareTo(c2);
        }
    }
}
