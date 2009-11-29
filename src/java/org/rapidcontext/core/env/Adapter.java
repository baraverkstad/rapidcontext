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

package org.rapidcontext.core.env;

import org.rapidcontext.core.data.Data;

/**
 * An external connectivity adapter. An adapter handles the creation
 * of external connections, some of which may be pooled or managed
 * for reutilization.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public interface Adapter {

    /**
     * Initializes this adapter. This method is used to perform any
     * initialization required before creating any connections to
     * external systems. This method will be called exactly once for
     * each adapter.
     *
     * @throws AdapterException if the adapter failed to initialize
     */
    public void init() throws AdapterException;

    /**
     * Destroys this adapter. This method is used to free any
     * resources that are common to all adapter connections created.
     * After this method has been called, no further calls will be
     * made to either the adapter or any connections created by it.
     *
     * @throws AdapterException if the adapter failed to uninitialize
     */
    public void destroy() throws AdapterException;

    /**
     * Returns an array with all configuration parameter names. The
     * order of the parameters will control how the they are
     * requested from the user in a GUI or similar.
     *
     * @return an array with configuration parameter names
     */
    public String[] getParameterNames();

    /**
     * Returns the default value for a configuration parameter.
     *
     * @param name           the configuration parameter name
     *
     * @return the default parameter value, or
     *         null if no default is available
     */
    public String getParameterDefault(String name);

    /**
     * Returns the description for a configuration parameter.
     *
     * @param name           the configuration parameter name
     *
     * @return the parameter description, or
     *         null if no description is available
     */
    public String getParameterDescription(String name);

    /**
     * Creates a new adapter connection. The input parameters contain
     * all the parameter names and values.
     *
     * @param params         the configuration parameters
     *
     * @return the adapter connection created
     *
     * @throws AdapterException if the connection couldn't be created
     *             properly
     */
    public AdapterConnection createConnection(Data params)
        throws AdapterException;
}
