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

package org.rapidcontext.core.proc;

import java.util.ArrayList;

/**
 * A procedure call stack. The stack contains an ordered list of the
 * procedures currently being called.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class CallStack {

    /**
     * The call stack.
     */
    private ArrayList stack = new ArrayList();

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
        return (stack.size() > 0) ? (Procedure) stack.get(0) : null;
    }

    /**
     * Returns the top procedure in the stack, i.e. the last
     * procedure in the call chain.
     *
     * @return the top procedure in the stack, or
     *         null if the stack is empty
     */
    public Procedure top() {
        return (stack.size() > 0) ? (Procedure) stack.get(stack.size() - 1) : null;
    }

    /**
     * Returns all procedures on the stack in an array.
     *
     * @return an array with all the procedures on the stack
     */
    public Procedure[] toArray() {
        Procedure[]  res = new Procedure[stack.size()];

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
        if (stack.size() > 0) {
            stack.remove(stack.size() - 1);
        }
    }
}
