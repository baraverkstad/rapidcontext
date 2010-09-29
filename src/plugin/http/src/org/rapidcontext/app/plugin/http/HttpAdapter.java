/**
 * RapidContext HTTP plug-in <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg & Dynabyte AB.
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

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.env.Adapter;
import org.rapidcontext.core.env.AdapterConnection;
import org.rapidcontext.core.env.AdapterException;

/**
 * A virtual HTTP connectivity adapter. This adapter allows storing
 * default HTTP connection parameters in the environment instead of
 * in the HTTP procedures. The HTTP connections created only contain
 * default parameter values and should normally not be pooled,
 * unless the number of concurrent connections needs to be
 * controlled.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class HttpAdapter implements Adapter {

    /**
     * The HTTP base URL configuration parameter name.
     */
    public static final String HTTP_URL = "url";

    /**
     * The HTTP header configuration parameter name.
     */
    public static final String HTTP_HEADER = "header";

    /**
     * The array of all parameters.
     */
    private static final String[] PARAMS = {
        HTTP_URL,
        HTTP_HEADER
    };

    /**
     * Default constructor, required by the Adapter interface.
     */
    public HttpAdapter() {
        // Nothing to do here
    }

    /**
     * Initializes this adapter. This method is used to perform any
     * initialization required before creating any connections to
     * external systems. This method will be called exactly once for
     * each adapter.
     */
    public void init() {
        // Nothing to do here
    }

    /**
     * Destroys this adapter. This method is used to free any
     * resources that are common to all adapter connections created.
     * After this method has been called, no further calls will be
     * made to either the adapter or any connections created by it.
     */
    public void destroy() {
        // Nothing to do here
    }

    /**
     * Returns an array with all configuration parameter names. The
     * order of the parameters will control how the they are
     * requested from the user in a GUI or similar.
     *
     * @return an array with configuration parameter names
     */
    public String[] getParameterNames() {
        return PARAMS;
    }

    /**
     * Returns the default value for a configuration parameter.
     *
     * @param name           the configuration parameter name
     *
     * @return the default parameter value, or
     *         null if no default is available
     */
    public String getParameterDefault(String name) {
        return null;
    }

    /**
     * Returns the description for a configuration parameter.
     *
     * @param name           the configuration parameter name
     *
     * @return the parameter description, or
     *         null if no description is available
     */
    public String getParameterDescription(String name) {
        if (HTTP_URL.equals(name)) {
            return "The HTTP base URL";
        } else if (HTTP_HEADER.equals(name)) {
            return "The HTTP headers";
        } else {
            return null;
        }
    }

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
    public AdapterConnection createConnection(Dict params)
        throws AdapterException {

        return new HttpConnection(params);
    }
}
