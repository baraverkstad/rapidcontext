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

package org.rapidcontext.core.data.stat;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.stat.MovingSum.Interval;

/**
 * Unit tests for the RollingCount class.
 */
@SuppressWarnings("javadoc")
public class MovingSumTest {

    @Test
    public void testSerialize() {
        MovingSum sum1 = new MovingSum(0, Array.of(0, 10, 0, 20, 0, 30));
        assertEquals(0, sum1.time());
        assertEquals(10.0, sum1.valueFor(Interval.HOUR), 0.01);
        assertEquals(20.0, sum1.valueFor(Interval.DAY), 0.01);
        assertEquals(30.0, sum1.valueFor(Interval.MONTH), 0.01);
        assertEquals("[ 0, 10, 0, 20, 0, 30 ]", sum1.serialize().toString());
        MovingSum sum2 = new MovingSum(System.currentTimeMillis());
        sum2.add(123.0);
        assertEquals(123.0, sum2.valueFor(Interval.HOUR), 0.01);
        assertEquals(123.0, sum2.valueFor(Interval.DAY), 0.01);
        assertEquals(123.0, sum2.valueFor(Interval.MONTH), 0.01);
        assertEquals("[ 123, 0, 123, 0, 123, 0 ]", sum2.serialize().toString());
    }

    @Test
    public void testValueFor() {
        MovingSum sum1 = new MovingSum(0, Array.of(0, 10, 0, 100, 0, 1000));
        assertEquals(10.0, sum1.valueFor(Interval.HOUR), 0.1);
        assertEquals(100.0, sum1.valueFor(Interval.DAY), 0.1);
        assertEquals(1000.0, sum1.valueFor(Interval.MONTH), 0.1);
        sum1.moveTo((long) (0.25 * Interval.HOUR.millis));
        assertEquals(7.5, sum1.valueFor(Interval.HOUR), 0.1);
        sum1.moveTo((long) (0.5 * Interval.HOUR.millis));
        assertEquals(5.0, sum1.valueFor(Interval.HOUR), 0.1);
        sum1.moveTo(Interval.HOUR.millis - 1);
        assertEquals(0.0, sum1.valueFor(Interval.HOUR), 0.1);
        sum1.moveTo((long) (0.10 * Interval.DAY.millis));
        assertEquals(90.0, sum1.valueFor(Interval.DAY), 0.1);
        sum1.moveTo((long) (0.85 * Interval.DAY.millis));
        assertEquals(15.0, sum1.valueFor(Interval.DAY), 0.1);
        sum1.moveTo(Interval.DAY.millis - 1);
        assertEquals(0.0, sum1.valueFor(Interval.DAY), 0.1);
        sum1.moveTo(10 * Interval.DAY.millis);
        assertEquals(666.6, sum1.valueFor(Interval.MONTH), 0.1);
        sum1.moveTo(24 * Interval.DAY.millis);
        assertEquals(200.0, sum1.valueFor(Interval.MONTH), 0.1);
        sum1.moveTo(Interval.MONTH.millis - 1);
        assertEquals(0.0, sum1.valueFor(Interval.MONTH), 0.1);
    }

    @Test
    public void testAdd() {
        MovingSum sum = new MovingSum(System.currentTimeMillis());
        sum.add(13);
        assertEquals("[ 13, 0, 13, 0, 13, 0 ]", sum.serialize().toString());
        sum.add(7);
        assertEquals("[ 20, 0, 20, 0, 20, 0 ]", sum.serialize().toString());
    }
}
