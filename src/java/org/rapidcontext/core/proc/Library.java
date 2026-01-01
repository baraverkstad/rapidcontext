/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2026 Per Cederberg. All rights reserved.
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

/**
 * A procedure library. The library handles procedure aliases, call
 * interceptors and other functions for all procedures.
 *
 * @author Per Cederberg
 *
 * @deprecated Procedures and interceptors are now initialized as normal
 *     storage objects instead. The Library API will be removed.
 */
@Deprecated(forRemoval = true)
public class Library {

    /**
     * The procedure call interceptor.
     */
    @SuppressWarnings("removal")
    private Interceptor interceptor = new DefaultInterceptor();

    /**
     * Creates a new procedure library.
     */
    public Library() {
        // Nothing to do here
    }

    /**
     * Returns the procedure call interceptor.
     *
     * @return the procedure call interceptor
     *
     * @deprecated Handled by CallInterceptor or ReserveInterceptor instead.
     * @see CallInterceptor
     * @see ReserveInterceptor
     */
    @Deprecated(forRemoval = true)
    @SuppressWarnings("removal")
    public Interceptor getInterceptor() {
        return interceptor;
    }

    /**
     * Sets the procedure call interceptor, overriding the default.
     *
     * @param i              the procedure call interceptor to use
     *
     * @deprecated Create a CallInterceptor or ReserveInterceptor instead.
     * @see CallInterceptor
     * @see ReserveInterceptor
     */
    @Deprecated(forRemoval = true)
    @SuppressWarnings("removal")
    public void setInterceptor(Interceptor i) {
        if (i == null) {
            this.interceptor = new DefaultInterceptor();
        } else {
            this.interceptor = i;
        }
    }
}
