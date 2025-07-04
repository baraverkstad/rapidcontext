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

import org.rapidcontext.core.data.Array;

/**
 * A set of approximated moving averages. The updated averages are
 * calculated using only the previous average (for each window) and
 * the new value. Three constant window sizes are used to smooth the
 * output average to a different degree. Calculations use double
 * internally, but serialize to long values multiplied by the
 * corresponding window size (to preserve limited precision).
 *
 * @author Per Cederberg
 */
public class MovingAverage {

    /**
     * The predefined window sizes.
     */
    public enum Window {

        /** The short (~10 data points) window size. */
        SHORT(10.0),

        /** The medium (~100 data points) window size. */
        MEDIUM(100.0),

        /** The long (~1000 data points) window size. */
        LONG(1000.0);

        /** The window size. */
        public final double size;

        /** The window size ratio (inverted size). */
        public final double ratio;

        // Creates a new predefined window.
        private Window(double size) {
            this.size = size;
            this.ratio = 1.0f / size;
        }
    };

    // The current averages for each window
    private double s, m, l;

    /**
     * Initializes a new set of moving averages with an initial value.
     *
     * @param initial        the initial value to use
     */
    public MovingAverage(double initial) {
        s = m = l = initial;
    }

    /**
     * Initializes a new set of moving averages from a serialized array.
     *
     * @param arr            the serialized array of long values
     */
    public MovingAverage(Array arr) {
        s = arr.get(0, Long.class, 0L) / Window.SHORT.size;
        m = arr.get(1, Long.class, 0L) / Window.MEDIUM.size;
        l = arr.get(2, Long.class, 0L) / Window.LONG.size;
    }

    /**
     * Returns an array with the averages for each of the window
     * sizes (small to large). The returned averages are multiplied
     * by each window size (to preserve some precision) and rounded
     * to long values.
     *
     * @return the serialized array of long values
     */
    public Array serialize() {
        return Array.of(
            Math.round(s * Window.SHORT.size),
            Math.round(m * Window.MEDIUM.size),
            Math.round(l * Window.LONG.size)
        );
    }

    /**
     * Returns an array with the averages for each of the window
     * sizes (small to large). The returned averages will be a
     * rounded to long values.
     *
     * @return an array of long values
     */
    public Array values() {
        return Array.of(
            Math.round(s),
            Math.round(m),
            Math.round(l)
        );
    }

    /**
     * Returns the approximate average for the specified window.
     *
     * @param window     the window to fetch
     *
     * @return the approximate average for the specified window
     */
    public double valueFor(Window window) {
        return switch (window) {
            case SHORT -> s;
            case MEDIUM -> m;
            case LONG -> l;
            default -> 0.0;
        };
    }

    /**
     * Adds a value to the moving average (for all windows).
     *
     * @param value          the value to add
     */
    public void add(double value) {
        s += Window.SHORT.ratio * value - Window.SHORT.ratio * s;
        m += Window.MEDIUM.ratio * value - Window.MEDIUM.ratio * m;
        l += Window.LONG.ratio * value - Window.LONG.ratio * l;
    }
}
