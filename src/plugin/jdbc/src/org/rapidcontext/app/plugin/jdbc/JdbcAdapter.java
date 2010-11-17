/*
 * RapidContext JDBC plug-in <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg. All rights reserved.
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

package org.rapidcontext.app.plugin.jdbc;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Dict;
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
     * The JDBC SQL ping configuration parameter name (optional,
     * defaults to 'SELECT 1').
     */
    protected static final String JDBC_PING = "sqlping";

    /**
     * The JDBC auto-commit configuration parameter name (optional,
     * defaults to false).
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
     * Creates a normalized copy of the JDBC configuration parameters.
     * This will fill in any default values and set the JDBC driver
     * class if only a JDBC url is set.
     *
     * @param params         the config parameters
     *
     * @return the normalized copy of the parameters
     */
    protected static Dict normalize(Dict params) {
        Dict    res = params.copy();
        String  driver;
        String  url;
        String  ping;

        driver = params.getString(JDBC_DRIVER, "").trim();
        url = params.getString(JDBC_URL, "").trim().toLowerCase();
        ping = params.getString(JDBC_PING, "").trim();
        if (driver.isEmpty()) {
            if (url.startsWith("jdbc:odbc")) {
                res.set(JDBC_DRIVER, "sun.jdbc.odbc.JdbcOdbcDriver");
            } else if (url.startsWith("jdbc:mysql:")) {
                res.set(JDBC_DRIVER, "com.mysql.jdbc.Driver");
            } else if (url.startsWith("jdbc:postgresql:")) {
                res.set(JDBC_DRIVER, "org.postgresql.Driver");
            } else if (url.startsWith("jdbc:oracle:")) {
                res.set(JDBC_DRIVER, "oracle.jdbc.driver.OracleDriver");
            } else if (url.startsWith("jdbc:db2:")) {
                res.set(JDBC_DRIVER, "COM.ibm.db2.jdbc.app.DB2Driver");
            } else if (url.startsWith("jdbc:microsoft:")) {
                res.set(JDBC_DRIVER, "com.microsoft.sqlserver.jdbc.SQLServerDriver");
            }
        }
        if (ping.isEmpty() && url.startsWith("jdbc:oracle:")) {
            res.set(JDBC_PING, "SELECT * FROM dual");
        } else if (ping.isEmpty()) {
            res.set(JDBC_PING, "SELECT 1");
        }
        res.setBoolean(JDBC_AUTOCOMMIT, params.getBoolean(JDBC_AUTOCOMMIT, false));
        try {
            res.setInt(JDBC_TIMEOUT, params.getInt(JDBC_TIMEOUT, 30));
        } catch (Exception ignore) {
            // Exception handled when creating connection
        }
        return res;
    }

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
    public AdapterConnection createConnection(Dict params)
        throws AdapterException {

        ClassLoader  loader;
        String       driverClass;
        Driver       driver;
        String       url;
        String       ping;
        boolean      autoCommit;
        int          timeout;
        Properties   props;
        String       msg;

        params = normalize(params);
        driverClass = params.getString(JDBC_DRIVER, "");
        try {
            loader = ApplicationContext.getInstance().getClassLoader();
            driver = (Driver) loader.loadClass(driverClass).newInstance();
        } catch (ClassNotFoundException e) {
            msg = "couldn't find or load JDBC driver class " + driverClass +
                  ": " + e.getMessage();
            throw new AdapterException(msg);
        } catch (ClassCastException e) {
            msg = "couldn't load JDBC driver, must be an instance of " +
                  "java.sql.Driver: " + driverClass;
            throw new AdapterException(msg);
        } catch (Exception e) {
            msg = "couldn't create JDBC driver instance of " + driverClass +
                  ": " + e.getMessage();
            throw new AdapterException(msg);
        }
        url = params.getString(JDBC_URL, "");
        ping = params.getString(JDBC_PING, null);
        autoCommit = params.getBoolean(JDBC_AUTOCOMMIT, false);
        try {
            timeout = params.getInt(JDBC_TIMEOUT, 30);
        } catch (Exception e) {
            throw new AdapterException("failed to parse timeout value: " +
                                       params.getString(JDBC_TIMEOUT, ""));
        }
        props = PropertiesSerializer.toProperties(params);
        return createConnection(driver, url, props, ping, autoCommit, timeout);
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
