/**
 * RapidContext HTTP plug-in <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2009 Per Cederberg & Dynabyte AB.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE.txt file for more details.
 */

package org.rapidcontext.app.plugin.http;

import java.net.MalformedURLException;
import java.net.URL;

import org.rapidcontext.core.data.Data;
import org.rapidcontext.core.env.AdapterConnection;
import org.rapidcontext.core.env.AdapterException;
import org.rapidcontext.core.proc.ProcedureException;

/**
 * A JDBC adapter connection. This class encapsulates a JDBC
 * connection and allows execution of arbitrary SQL queries or
 * statements.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class HttpConnection implements AdapterConnection {

    /**
     * The HTTP connection parameters. Indexed with the configuration
     * keys specified in the HTTP adapter.
     */
    private Data params = null;

    /**
     * Creates a new HTTP connection.
     *
     * @param params            the HTTP connection parameters
     */
    HttpConnection(Data params) {
        this.params = params;
    }

    /**
     * Activates the connection. This method is called just before a
     * connection is to be used, i.e. when a new connection has been
     * created or when fetched from a resource pool. It can also be
     * called to trigger a "ping" of a connection, if such
     * functionality is implemented by the adapter.
     *
     * @throws AdapterException if the connection couldn't be
     *             activated (connection will be closed)
     */
    public void activate() throws AdapterException {
        // Nothing to do here
    }

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
    public void passivate() throws AdapterException {
        // Nothing to do here
    }

    /**
     * Closes the connection. This method is used to free any
     * resources used by the connection.  After this method has been
     * called, no further calls will be made to this connection.
     *
     * @throws AdapterException if the connection couldn't be closed
     *             properly (connection discarded anyway)
     */
    public void close() throws AdapterException {
        // Nothing to do here
    }

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
    public void commit() throws AdapterException {
        // Nothing to do here
    }

    /**
     * Rolls any pending changes back. This method is called after an
     * unsuccessful procedure tree execution that included this
     * connection. This method may be implemented as a no-op, if the
     * adapter does not support commit and rollback semantics.
     *
     * @throws AdapterException if the pending changes couldn't be
     *             rolled back (connection will be closed)
     */
    public void rollback() throws AdapterException {
        // Nothing to do here
    }

    /**
     * Returns the base URL for the connection.
     *
     * @return the base URL for the connection
     *
     * @throws ProcedureException if the base URL was malformed
     */
    public URL getUrl() throws ProcedureException {
        String  str = this.params.getString(HttpAdapter.HTTP_URL, "");
        try {
            return new URL(str);
        } catch (MalformedURLException e) {
            throw new ProcedureException("malformed URL: " + str);
        }
    }

    /**
     * Returns the default HTTP headers for the connection.
     *
     * @return the default HTTP headers for the connection, or
     *         an empty string if not set
     */
    public String getHeaders() {
        return this.params.getString(HttpAdapter.HTTP_HEADER, "");
    }
}
