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

package org.rapidcontext.core.proc;

import java.util.ArrayList;
import java.util.List;

import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.type.Procedure;

/**
 * A procedure call stack. The stack contains an ordered list of the
 * procedures currently being called.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class CallStack {

    /**
     * The call stack.
     */
    private ArrayList<Procedure> stack = new ArrayList<>();

    /**
     * Creates a new empty procedure call stack.
     */
    public CallStack() {}

    /**
     * Checks if the specified procedure exists in the call stack.
     *
     * @param proc           the procedure definition
     *
     * @return true if the procedure exists in the call stack, or
     *         false otherwise
     */
    public boolean contains(Procedure proc) {
        return stack.contains(proc);
    }

    /**
     * Returns the current height of the call stack.
     *
     * @return the current height of the call stack
     */
    public int height() {
        return stack.size();
    }

    /**
     * Returns the most recent caller from the stack.
     *
     * @param offset         the offset from the top (0 for top)
     *
     * @return the top caller in the stack, or
     *         null if the stack is empty
     */
    public Path top(int offset) {
        int idx = stack.size() - 1 - offset;
        return (0 <= idx && idx < stack.size()) ? stack.get(idx).path() : null;
    }

    /**
     * Returns a printable stack trace for debugging purposes.
     *
     * @param maxSize        the maximum stack trace length
     *
     * @return an array with all the procedures on the stack
     */
    public List<String> toStackTrace(int maxSize) {
        ArrayList<String> res = new ArrayList<>(maxSize + 1);
        int size = stack.size();
        int start = Math.max(0, size - maxSize);
        for (int i = size - 1; i >= start; i--) {
            res.add(stack.get(i).id());
        }
        if (size > maxSize) {
            res.add("...");
        }
        return res;
    }

    /**
     * Adds a new last entry to the call stack.
     *
     * @param proc           the procedure being called
     */
    void push(Procedure proc) {
        stack.add(proc);
    }

    /**
     * Removes the last entry in the call stack.
     */
    void pop() {
        if (!stack.isEmpty()) {
            stack.remove(stack.size() - 1);
        }
    }
}
