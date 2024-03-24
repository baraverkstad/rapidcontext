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

package org.rapidcontext.core.data.stat;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.rapidcontext.core.data.Array;

/**
 * Unit tests for the MovingAverage class.
 */
@SuppressWarnings("javadoc")
public class MovingAverageTest {

    @Test
    public void testSerialize() {
        MovingAverage avg1 = new MovingAverage(3.0);
        assertEquals("[ 30, 300, 3000 ]", avg1.serialize().toString());
        MovingAverage avg2 = new MovingAverage(avg1.serialize());
        assertEquals("[ 30, 300, 3000 ]", avg2.serialize().toString());
        MovingAverage avg3 = new MovingAverage(Array.of(10, 200, 3000));
        assertEquals(1.0, avg3.valueFor(MovingAverage.Window.SHORT), 0.1);
        assertEquals(2.0, avg3.valueFor(MovingAverage.Window.MEDIUM), 0.1);
        assertEquals(3.0, avg3.valueFor(MovingAverage.Window.LONG), 0.1);
    }

    @Test
    public void testAdd() {
        MovingAverage avg = new MovingAverage(10.0);
        assertEquals(10.0, avg.valueFor(MovingAverage.Window.SHORT), 0.1);
        assertEquals(10.0, avg.valueFor(MovingAverage.Window.MEDIUM), 0.1);
        assertEquals(10.0, avg.valueFor(MovingAverage.Window.LONG), 0.1);
        avg.add(0.0);
        assertEquals(9.0, avg.valueFor(MovingAverage.Window.SHORT), 0.1);
        assertEquals(10.0, avg.valueFor(MovingAverage.Window.MEDIUM), 0.1);
        assertEquals(10.0, avg.valueFor(MovingAverage.Window.LONG), 0.1);
        for (int i = 0; i < 10; i++) {
            avg.add(20.0);
        }
        assertEquals(16.2, avg.valueFor(MovingAverage.Window.SHORT), 0.1);
        assertEquals(10.8, avg.valueFor(MovingAverage.Window.MEDIUM), 0.1);
        assertEquals(10.0, avg.valueFor(MovingAverage.Window.LONG), 0.1);
        for (int i = 0; i < 100; i++) {
            avg.add(30.0);
        }
        assertEquals(30.0, avg.valueFor(MovingAverage.Window.SHORT), 0.1);
        assertEquals(23.0, avg.valueFor(MovingAverage.Window.MEDIUM), 0.1);
        assertEquals(12.0, avg.valueFor(MovingAverage.Window.LONG), 0.1);
        for (int i = 0; i < 2000; i++) {
            avg.add(40.0);
        }
        assertEquals(40.0, avg.valueFor(MovingAverage.Window.SHORT), 0.1);
        assertEquals(40.0, avg.valueFor(MovingAverage.Window.MEDIUM), 0.1);
        assertEquals(36.2, avg.valueFor(MovingAverage.Window.LONG), 0.1);
        avg.add(10000.0);
        assertEquals(1036.0, avg.valueFor(MovingAverage.Window.SHORT), 0.1);
        assertEquals(139.6, avg.valueFor(MovingAverage.Window.MEDIUM), 0.1);
        assertEquals(46.1, avg.valueFor(MovingAverage.Window.LONG), 0.1);
    }
}
