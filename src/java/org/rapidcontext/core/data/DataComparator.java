/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2009 Per Cederberg & Dynabyte AB.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE.txt file for more details.
 */

package org.rapidcontext.core.data;

import java.util.Comparator;

/**
 * A data object comparator. This comparator only compares two key
 * values, which both must implement Comparable and be of the same
 * type.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class DataComparator implements Comparator {

    /**
     * The data property key name to compare.
     */
    private String key;

    /**
     * Creates a new data object comparator.
     *
     * @param key            the data property key name
     */
    public DataComparator(String key) {
        this.key = key;
    }

    /**
     * Compares two data objects by comparing their values for a
     * predefined property key.
     *
     * @param o1             the first object
     * @param o2             the second object
     *
     * @return a negative integer, zero, or a positive integer as the
     *         first argument is less than, equal to, or greater than
     *         the second
     *
     * @throws ClassCastException if the values are not comparable
     *             (for example, strings and integers)
     */
    public int compare(Object o1, Object o2) throws ClassCastException {
        Data  d1 = (Data) o1;
        Data  d2 = (Data) o2;

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
     * @throws ClassCastException if the values are not comparable
     *             (for example, strings and integers)
     */
    private int compareValues(Object o1, Object o2) throws ClassCastException {
        Comparable  c1 = (Comparable) o1;
        Comparable  c2 = (Comparable) o2;

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
