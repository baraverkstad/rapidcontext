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

package org.rapidcontext.core.proc;

import java.util.ArrayList;

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
     *
     * @deprecated Replaced with org.rapidcontext.core.type.Procedure signature.
     */
    @Deprecated(forRemoval=true)
    @SuppressWarnings({"deprecation", "removal"})
    public boolean contains(org.rapidcontext.core.proc.Procedure proc) {
        return stack.contains(proc);
    }

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
     * Returns the bottom procedure in the stack, i.e. the first
     * procedure in the call chain.
     *
     * @return the bottom procedure in the stack, or
     *         null if the stack is empty
     */
    public Procedure bottom() {
        return stack.isEmpty() ? null : stack.get(0);
    }

    /**
     * Returns the top procedure in the stack, i.e. the last
     * procedure in the call chain.
     *
     * @return the top procedure in the stack, or
     *         null if the stack is empty
     */
    public Procedure top() {
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    /**
     * Returns all procedures on the stack in an array.
     *
     * @return an array with all the procedures on the stack
     */
    public Procedure[] toArray() {
        Procedure[] res = new Procedure[stack.size()];
        stack.toArray(res);
        return res;
    }

    /**
     * Adds a new last entry to the call stack.
     *
     * @param proc           the procedure being called
     * @param bindings       the bindings used
     */
    void push(Procedure proc, Bindings bindings) {
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
