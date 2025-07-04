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

package org.rapidcontext.core.stats;

import org.apache.commons.lang3.time.DateUtils;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;

/**
 * A set of approximate moving sums for predefined time intervals.
 * For each interval, both the current (running) and previous sums
 * are stored, allowing for calculating an approximate sum for the
 * whole interval. This uses a percentage of the previous sum
 * depending on current time. Three predefined time intervals (hour,
 * day, month) are provided.
 *
 * @author Per Cederberg
 */
public class MovingSum {

    /**
     * The predefined time intervals.
     */
    public static enum Interval {

        /** The hour interval. */
        HOUR(DateUtils.MILLIS_PER_HOUR),

        /** The day (24 hour) interval. */
        DAY(DateUtils.MILLIS_PER_DAY),

        /** The month (30 day) interval. */
        MONTH(30 * DateUtils.MILLIS_PER_DAY);

        /** The interval length in milliseconds. */
        public final long millis;

        // Creates a new predefined interval
        private Interval(long millis) {
            this.millis = millis;
        }
    }

    // The timestamp for the last update
    private long time;

    // The current sums for each interval
    private double h0, h1, d0, d1, m0, m1;

    /**
     * Initializes a new set of moving sums.
     *
     * @param now            the current time (in millis)
     */
    public MovingSum(long now) {
        time = now;
        h0 = h1 = d0 = d1 = m0 = m1 = 0.0;
    }

    /**
     * Initializes a new set of moving sums from a serialized array.
     *
     * @param now            the serialization time (in millis)
     * @param arr            the serialized array of long values
     */
    public MovingSum(long now, Array arr) {
        time = now;
        h0 = arr.get(0, Long.class, 0L);
        h1 = arr.get(1, Long.class, 0L);
        d0 = arr.get(2, Long.class, 0L);
        d1 = arr.get(3, Long.class, 0L);
        m0 = arr.get(4, Long.class, 0L);
        m1 = arr.get(5, Long.class, 0L);
    }

    /**
     * Returns an array with the sums for each of the intervals (hour
     * to month). Two long values are provided for each interval.
     *
     * @return the serialized array of long values
     */
    public Array serialize() {
        return Array.of(
            Math.round(h0),
            Math.round(h1),
            Math.round(d0),
            Math.round(d1),
            Math.round(m0),
            Math.round(m1)
        );
    }

    /**
     * Returns the timestamp (in millis) for the most recent move.
     *
     * @return the current interval timestamp (in millis)
     */
    public long time() {
        return time;
    }

    /**
     * Returns a dictionary with the approximate sums for each of the
     * window sizes. The returned sums will be a rounded to long
     * values.
     *
     * @return a dictionary with sums for each interval
     */
    public Dict values() {
        return new Dict()
            .set("hour", Math.round(valueFor(Interval.HOUR)))
            .set("day", Math.round(valueFor(Interval.DAY)))
            .set("month", Math.round(valueFor(Interval.MONTH)));
    }

    /**
     * Returns the approximate sum for the specified interval. The
     * current time is used to determine what percentage of the
     * previous sum to add to the current (running) sum.
     *
     * @param interval   the interval to fetch
     *
     * @return the approximate sum for the specified interval
     */
    public double valueFor(Interval interval) {
        double ratio = (time % interval.millis) / (double) interval.millis;
        return switch (interval) {
            case HOUR -> h0 + h1 * (1 - ratio);
            case DAY -> d0 + d1 * (1 - ratio);
            case MONTH -> m0 + m1 * (1 - ratio);
            default -> 0.0;
        };
    }

    /**
     * Adds a value to the moving sum (for all intervals).
     *
     * @param value          the value to add
     */
    public void add(double value) {
        h0 += value;
        d0 += value;
        m0 += value;
    }

    /**
     * Moves the current time forward. If needed, one or more of the
     * sums will be rolled over.
     *
     * @param now            the current time (in millis)
     */
    public void moveTo(long now) {
        long prev = time;
        time = Math.max(time, now);
        long diff = time / Interval.HOUR.millis - prev / Interval.HOUR.millis;
        if (diff > 0) {
            h1 = (diff == 1) ? h0 : 0.0;
            h0 = 0.0;
            diff = time / Interval.DAY.millis - prev / Interval.DAY.millis;
            if (diff > 0) {
                d1 = (diff == 1) ? d0 : 0.0;
                d0 = 0.0;
                diff = time / Interval.MONTH.millis - prev / Interval.MONTH.millis;
                if (diff > 0) {
                    m1 = (diff == 1) ? m0 : 0.0;
                    m0 = 0.0;
                }
            }
        }
    }
}
