/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg & Dynabyte AB.
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

package org.rapidcontext.core.env;

/**
 * An external adapter connection. A connection provides connectivity
 * to an external system, such as a database, a message bus or
 * similar. The adapter connection is created by the adapter, but
 * managed by a connection pool for improved resource utilization.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public interface AdapterConnection {

    /**
     * Activates the connection. This method is called just before a
     * connection is to be used, i.e. when a new connection has been
     * created or when fetched from a resource pool.
     *
     * @throws AdapterException if the connection couldn't be
     *             activated (connection will be closed)
     */
    public void activate() throws AdapterException;

    /**
     * Passivates the connection. This method is called just after a
     * connection has been used and will be returned to the pool.
     * This operation should clear or reset the connection, so that
     * it can safely be used again at a later time without affecting
     * previous results or operations.
     *
     * @throws AdapterException if the connection couldn't be
     *             passivated (connection will be closed)
     */
    public void passivate() throws AdapterException;

    /**
     * Validates the connection. This method is called before using
     * a connection and regularly when it is idle in the pool. It can
     * be used to trigger a "ping" of a connection, if implemented by
     * the adapter. An empty implementation is acceptable.
     *
     * @throws AdapterException if the connection didn't validate
     *             correctly
     */
    public void validate() throws AdapterException;

    /**
     * Closes the connection. This method is used to free any
     * resources used by the connection.  After this method has been
     * called, no further calls will be made to this connection.
     *
     * @throws AdapterException if the connection couldn't be closed
     *             properly (connection discarded anyway)
     */
    public void close() throws AdapterException;

    /**
     * Commits any pending changes. This method is called after each
     * successful procedure tree execution that included this
     * connection. This method may be implemented as a no-op, if
     * the adapter does not support commit and rollback semantics.
     *
     * @throws AdapterException if the pending changes couldn't be
     *             committed to permanent storage (connection will be
     *             closed)
     */
    public void commit() throws AdapterException;

    /**
     * Rolls any pending changes back. This method is called after an
     * unsuccessful procedure tree execution that included this
     * connection. This method may be implemented as a no-op, if the
     * adapter does not support commit and rollback semantics.
     *
     * @throws AdapterException if the pending changes couldn't be
     *             rolled back (connection will be closed)
     */
    public void rollback() throws AdapterException;
}
