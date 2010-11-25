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
import org.rapidcontext.core.storage.StorageException;
import org.rapidcontext.core.type.Channel;
import org.rapidcontext.core.type.Connection;
import org.rapidcontext.core.type.ConnectionException;

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
public class JdbcConnection extends Connection {

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
     * Creates a new JDBC connection from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public JdbcConnection(String id, String type, Dict dict) {
        super(id, type, dict);
    }

    /**
     * Initializes this connection after loading it from a storage.
     *
     * @throws StorageException if the initialization failed
     */
    protected void init() throws StorageException {
        String  driver;
        String  url;
        String  ping;

        driver = dict.getString(JDBC_DRIVER, "").trim();
        url = dict.getString(JDBC_URL, "").trim().toLowerCase();
        ping = dict.getString(JDBC_PING, "").trim();
        if (driver.isEmpty()) {
            if (url.startsWith("jdbc:odbc")) {
                dict.set(JDBC_DRIVER, "sun.jdbc.odbc.JdbcOdbcDriver");
            } else if (url.startsWith("jdbc:mysql:")) {
                dict.set(JDBC_DRIVER, "com.mysql.jdbc.Driver");
            } else if (url.startsWith("jdbc:postgresql:")) {
                dict.set(JDBC_DRIVER, "org.postgresql.Driver");
            } else if (url.startsWith("jdbc:oracle:")) {
                dict.set(JDBC_DRIVER, "oracle.jdbc.driver.OracleDriver");
            } else if (url.startsWith("jdbc:db2:")) {
                dict.set(JDBC_DRIVER, "COM.ibm.db2.jdbc.app.DB2Driver");
            } else if (url.startsWith("jdbc:microsoft:")) {
                dict.set(JDBC_DRIVER, "com.microsoft.sqlserver.jdbc.SQLServerDriver");
            }
        }
        if (ping.isEmpty() && url.startsWith("jdbc:oracle:")) {
            dict.set(JDBC_PING, "SELECT * FROM dual");
        } else if (ping.isEmpty()) {
            dict.set(JDBC_PING, "SELECT 1");
        }
        dict.setBoolean(JDBC_AUTOCOMMIT, dict.getBoolean(JDBC_AUTOCOMMIT, false));
        try {
            dict.setInt(JDBC_TIMEOUT, dict.getInt(JDBC_TIMEOUT, 30));
        } catch (Exception ignore) {
            // Exception handled when creating connection
        }
        super.init();
    }

    /**
     * Destroys this connection. This method overrides the default to
     * provide package access to it when testing connections.
     */
    protected void destroy() {
        super.destroy();
    }

    /**
     * Creates a new connection channel.
     *
     * @return the channel created
     *
     * @throws ConnectionException if the channel couldn't be created
     *             properly
     */
    protected Channel createChannel() throws ConnectionException {
        ClassLoader  loader;
        String       driverClass;
        Driver       driver;
        String       url;
        String       ping;
        boolean      autoCommit;
        int          timeout;
        Properties   props;
        String       msg;

        driverClass = dict.getString(JDBC_DRIVER, "");
        try {
            loader = ApplicationContext.getInstance().getClassLoader();
            driver = (Driver) loader.loadClass(driverClass).newInstance();
        } catch (ClassNotFoundException e) {
            msg = "couldn't find or load JDBC driver class " + driverClass +
                  ": " + e.getMessage();
            throw new ConnectionException(msg);
        } catch (ClassCastException e) {
            msg = "couldn't load JDBC driver, must be an instance of " +
                  "java.sql.Driver: " + driverClass;
            throw new ConnectionException(msg);
        } catch (Exception e) {
            msg = "couldn't create JDBC driver instance of " + driverClass +
                  ": " + e.getMessage();
            throw new ConnectionException(msg);
        }
        url = dict.getString(JDBC_URL, "");
        ping = dict.getString(JDBC_PING, null);
        autoCommit = dict.getBoolean(JDBC_AUTOCOMMIT, false);
        try {
            timeout = dict.getInt(JDBC_TIMEOUT, 30);
        } catch (Exception e) {
            throw new ConnectionException("failed to parse timeout value: " +
                                          dict.getString(JDBC_TIMEOUT, ""));
        }
        props = PropertiesSerializer.toProperties(dict);
        props.remove(KEY_ID);
        props.remove(KEY_TYPE);
        props.remove(KEY_MAX_ACTIVE);
        props.remove(KEY_MAX_IDLE_SECS);
        return new JdbcChannel(this, driver, url, props, ping, autoCommit, timeout);
    }

    /**
     * Destroys a connection channel, freeing any resources used
     * (such as database connections, networking sockets, etc).
     *
     * @param channel        the channel to destroy
     */
    protected void destroyChannel(Channel channel) {
        ((JdbcChannel) channel).close();
    }
}
