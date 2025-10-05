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

import java.util.List;

import org.rapidcontext.core.type.Procedure;

/**
 * A procedure call stack. The stack contains an ordered list of the
 * procedures currently being called.
 *
 * @author Per Cederberg
 *
 * @deprecated The CallContext class now encapsulates a call stack.
 */
@Deprecated(forRemoval = true)
public class CallStack {

    private CallContext cx;

    /**
     * Creates a new procedure call stack.
     *
     * @param cx             the call context
     */
    protected CallStack(CallContext cx) {
        this.cx = cx;
    }

    /**
     * Checks if the specified procedure exists in the call stack.
     *
     * @param proc           the procedure definition
     *
     * @return true if the procedure exists in the call stack, or
     *         false otherwise
     *
     * @deprecated Use {@link CallContext#isCalledBy(Procedure)} instead.
     */
    @Deprecated(forRemoval = true)
    public boolean contains(Procedure proc) {
        return cx.isCalledBy(proc);
    }

    /**
     * Returns the current height of the call stack.
     *
     * @return the current height of the call stack
     *
     * @deprecated Use {@link CallContext#depth()} instead.
     */
    @Deprecated(forRemoval = true)
    public int height() {
        return cx.depth();
    }

    /**
     * Returns the procedure calling the currently executing procedure.
     *
     * @return the caller procedure, or
     *         null if top-level
     *
     * @deprecated Use {@link CallContext#caller()} instead.
     */
    @Deprecated(forRemoval = true)
    public Procedure caller() {
        return cx.caller();
    }

    /**
     * Returns a printable stack trace for debugging purposes.
     *
     * @param maxSize        the maximum stack trace length
     *
     * @return an array with all the procedures on the stack
     *
     * @deprecated Use {@link CallContext#stackTrace()} instead.
     */
    @Deprecated(forRemoval = true)
    public List<String> toStackTrace(int maxSize) {
        List<String> res = cx.stackTrace();
        return (res.size() > maxSize) ? res.subList(0, maxSize) : res;
    }
}
