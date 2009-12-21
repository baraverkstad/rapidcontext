/*
 * RapidContext JDBC plug-in <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2009 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.rapidcontext.app.plugin.jdbc;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.app.plugin.PluginClassLoader;
import org.rapidcontext.core.data.Data;
import org.rapidcontext.core.data.PropertiesSerializer;
import org.rapidcontext.core.env.Adapter;
import org.rapidcontext.core.env.AdapterConnection;
import org.rapidcontext.core.env.AdapterException;

import java.sql.Driver;
import java.util.Properties;

/**
 * A JDBC connectivity adapter. This adapter allows execution of SQL
 * queries and statements to any JDBC data source. Connections may be
 * pooled for maximum resource utilization.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class JdbcAdapter implements Adapter {

    /**
     * The JDBC driver configuration parameter name.
     */
    protected static final String JDBC_DRIVER = "driver";

    /**
     * The JDBC URL configuration parameter name.
     */
    protected static final String JDBC_URL = "url";

    /**
     * The JDBC user configuration parameter name.
     */
    protected static final String JDBC_USER = "user";

    /**
     * The JDBC password configuration parameter name.
     */
    protected static final String JDBC_PASSWORD = "password";

    /**
     * The JDBC SQL ping configuration parameter name.
     */
    protected static final String JDBC_PING = "sqlping";

    /**
     * The JDBC auto-commit configuration parameter name.
     */
    protected static final String JDBC_AUTOCOMMIT = "autocommit";

    /**
     * The JDBC connection and query timeout configuration parameter name.
     */
    protected static final String JDBC_TIMEOUT = "timeout";

    /**
     * The array of all parameters.
     */
    protected static final String[] PARAMS = {
        JDBC_DRIVER,
        JDBC_URL,
        JDBC_USER,
        JDBC_PASSWORD,
        JDBC_PING,
        JDBC_AUTOCOMMIT,
        JDBC_TIMEOUT
    };

    /**
     * Default constructor, required by the Adapter interface.
     */
    public JdbcAdapter() {
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
        if (JDBC_DRIVER.equals(name)) {
            return "The JDBC driver class";
        } else if (JDBC_URL.equals(name)) {
            return "The JDBC connection URL";
        } else if (JDBC_USER.equals(name)) {
            return "The database user name";
        } else if (JDBC_PASSWORD.equals(name)) {
            return "The database user password";
        } else if (JDBC_PING.equals(name)) {
            return "The SQL ping query for checking connections";
        } else if (JDBC_AUTOCOMMIT.equals(name)) {
            return "The auto-commit on each SQL statement flag (defaults to false)";
        } else if (JDBC_TIMEOUT.equals(name)) {
            return "The connection and query timeout (in secs)";
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
    public AdapterConnection createConnection(Data params)
        throws AdapterException {

        PluginClassLoader  loader;
        Class              driverClass;
        Driver             driver;
        Properties         props;
        String             msg;
        boolean            autoCommit;
        int                timeout;

        try {
            loader = ApplicationContext.getInstance().getClassLoader();
            driverClass = loader.loadClass(params.getString(JDBC_DRIVER, ""));
            driver = (Driver) driverClass.newInstance();
        } catch (ClassNotFoundException e) {
            msg = "couldn't find or load JDBC driver class " +
                  params.getString(JDBC_DRIVER, "") + ": " + e.getMessage();
            throw new AdapterException(msg);
        } catch (ClassCastException e) {
            msg = "couldn't load JDBC driver, must be an instance of " +
                  "java.sql.Driver: " + params.getString(JDBC_DRIVER, "");
            throw new AdapterException(msg);
        } catch (Exception e) {
            msg = "couldn't create JDBC driver instance of " +
                  params.getString(JDBC_DRIVER, "") + ": " + e.getMessage();
            throw new AdapterException(msg);
        }
        autoCommit = params.getBoolean(JDBC_AUTOCOMMIT, false);
        try {
            timeout = params.getInt(JDBC_TIMEOUT, 30);
        } catch (Exception e) {
            throw new AdapterException("failed to parse timeout value: " +
                                       params.getString(JDBC_TIMEOUT, ""));
        }
        props = PropertiesSerializer.toProperties(params);
        return createConnection(driver,
                                params.getString(JDBC_URL, ""),
                                props,
                                params.getString(JDBC_PING, ""),
                                autoCommit,
                                timeout);
    }

    /**
     * Creates a new JDBC connection. This method exists to simplify the
     * creation of subclass implementation of the standard JDBC connection.
     * This method is called by the createConnection(Data) method that performs
     * parameter validations.
     *
     * @param driver            the JDBC driver
     * @param url               the connection URL
     * @param props             the connection properties (user and password)
     * @param sqlPing           the SQL ping query
     * @param autoCommit        the auto-commit flag
     * @param timeout           the request timeout (in secs)
     *
     * @return the JDBC connection created
     *
     * @throws AdapterException if a connection couldn't be established
     */
    protected JdbcConnection createConnection(Driver driver,
                                              String url,
                                              Properties props,
                                              String sqlPing,
                                              boolean autoCommit,
                                              int timeout)
        throws AdapterException {

        return new JdbcConnection(driver, url, props, sqlPing, autoCommit, timeout);
    }
}
