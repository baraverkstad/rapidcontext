/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2023 Per Cederberg. All rights reserved.
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

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.core.data.Dict;

/**
 * A combined usage metric with moving counters, average durations
 * and recent errors.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class MovingUsage {

    /**
     * The usage counters.
     */
    private MovingSum counts;

    /**
     * The average durations (in millis).
     */
    private MovingAverage durations = null;

    /**
     * The error counters.
     */
    private MovingSum errors = null;

    /**
     * The the most recent error message.
     */
    private String errorMsg = null;

    /**
     * Initializes a new set of moving usage metrics.
     *
     * @param now            the current time (in millis)
     */
    public MovingUsage(long now) {
        counts = new MovingSum(now);
    }

    /**
     * Initializes a set of moving usage metrics from serialized data.
     *
     * @param now            the current time (in millis)
     * @param dict           the serialized dictionary
     */
    public MovingUsage(long now, Dict dict) {
        counts = new MovingSum(now, dict.getArray("c"));
        if (dict.containsKey("d")) {
            durations = new MovingAverage(dict.getArray("d"));
        }
        if (dict.containsKey("e")) {
            errors = new MovingSum(now, dict.getArray("e"));
        }
        if (dict.containsKey("msg")) {
            errorMsg = dict.get("msg", String.class);
        }
    }

    /**
     * Returns a dictionary with the serialized metric values.
     *
     * @return a dictionary with serialized values
     */
    public Dict serialize() {
        Dict dict = new Dict();
        dict.set("c", counts.serialize());
        if (durations != null) {
            dict.set("d", durations.serialize());
        }
        if (errors != null) {
            dict.set("e", errors.serialize());
        }
        if (errorMsg != null) {
            dict.set("msg", errorMsg);
        }
        return dict;
    }

    /**
     * Returns the timestamp (in millis) for the most recent move.
     *
     * @return the current interval timestamp (in millis)
     */
    public long time() {
        return counts.time();
    }

    /**
     * Returns a dictionary with the current counters and other
     * metrics.
     *
     * @return a dictionary with metric values
     */
    public Dict values() {
        Dict dict = new Dict();
        dict.set("count", counts.values());
        if (durations != null) {
            dict.set("avg", durations.values());
        }
        if (errors != null) {
            dict.set("error", errors.values());
            if (errorMsg != null) {
                dict.getDict("error").set("msg", errorMsg);
            }
        }
        return dict;
    }

    /**
     * Increases the usage counters and average durations.
     *
     * @param value      the usage count to add, or zero (0) for none
     * @param duration   the duration (in millis), or zero (0) to skip
     * @param success    the success flag
     * @param error      the optional error message
     */
    public void add(int value, long duration, boolean success, String error) {
        counts.add(value);
        if (duration > 0) {
            if (durations == null) {
                durations = new MovingAverage(duration);
            } else {
                durations.add(duration);
            }
        }
        if (!success) {
            if (errors == null) {
                errors = new MovingSum(counts.time());
            }
            errors.add(value);
        }
        if (error != null) {
            errorMsg = StringUtils.abbreviate(error, 1000);
        }
    }

    /**
     * Move the current time and possibly roll-over one or more of
     * the moving sums.
     *
     * @param now            the current time (in millis)
     */
    public void move(long now) {
        counts.moveTo(now);
        if (errors != null) {
            errors.moveTo(now);
            if (errors.valueFor(MovingSum.Interval.MONTH) <= 0.0) {
                errors = null;
                errorMsg = null;
            } else if (errorMsg != null && errors.valueFor(MovingSum.Interval.DAY) <= 0.0) {
                errorMsg = null;
            }
        }
    }
}
