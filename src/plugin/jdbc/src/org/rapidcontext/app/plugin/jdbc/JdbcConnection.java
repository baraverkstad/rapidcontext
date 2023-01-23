/*
 * RapidContext JDBC plug-in <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2023 Per Cederberg. All rights reserved.
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

package org.rapidcontext.app.plugin.jdbc;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.StorageException;
import org.rapidcontext.core.type.Channel;
import org.rapidcontext.core.type.Connection;
import org.rapidcontext.core.type.ConnectionException;

import java.sql.Driver;
import java.util.Properties;
import java.util.logging.Logger;

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
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(JdbcConnection.class.getName());

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
     * Normalizes a JDBC connection data object if needed. This method
     * will modify legacy data into the proper keys and values.
     *
     * @param id             the object identifier
     * @param dict           the storage data
     *
     * @return the storage data (possibly modified)
     */
    public static Dict normalize(String id, Dict dict) {
        if (dict.containsKey(JDBC_PASSWORD)) {
            LOG.warning("deprecated: connection " + id + " data: password not hidden");
            String pwd = dict.getString(JDBC_PASSWORD, "");
            dict.remove(JDBC_PASSWORD);
            dict.set(PREFIX_HIDDEN + JDBC_PASSWORD, pwd);
        }
        return dict;
    }

    /**
     * Creates a new JDBC connection from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public JdbcConnection(String id, String type, Dict dict) {
        super(id, type, normalize(id, dict));
    }

    /**
     * Initializes this connection after loading it from a storage.
     *
     * @throws StorageException if the initialization failed
     */
    protected void init() throws StorageException {
        String driver = dict.getString(JDBC_DRIVER, "").trim();
        String url = dict.getString(JDBC_URL, "").trim().toLowerCase();
        String ping = dict.getString(JDBC_PING, "").trim();
        if (driver.length() == 0) {
            // Adjust older MySQL connection URLs (for default driver)
            if (url.startsWith("jdbc:mysql:thin:")) {
                url = StringUtils.replaceOnce(url, "jdbc:mysql:thin:", "jdbc:mariadb:");
                dict.set(PREFIX_COMPUTED + JDBC_URL, url);
            } else if (url.startsWith("jdbc:mysql:")) {
                url = StringUtils.replaceOnce(url, "jdbc:mysql:", "jdbc:mariadb:");
                dict.set(PREFIX_COMPUTED + JDBC_URL, url);
            }
            // Set default driver
            if (url.startsWith("jdbc:odbc")) {
                dict.set(PREFIX_COMPUTED + JDBC_DRIVER, "sun.jdbc.odbc.JdbcOdbcDriver");
            } else if (url.startsWith("jdbc:mariadb:") || url.startsWith("jdbc:mysql:")) {
                dict.set(PREFIX_COMPUTED + JDBC_DRIVER, "org.mariadb.jdbc.Driver");
            } else if (url.startsWith("jdbc:postgresql:")) {
                dict.set(PREFIX_COMPUTED + JDBC_DRIVER, "org.postgresql.Driver");
            } else if (url.startsWith("jdbc:oracle:")) {
                dict.set(PREFIX_COMPUTED + JDBC_DRIVER, "oracle.jdbc.driver.OracleDriver");
            } else if (url.startsWith("jdbc:db2:")) {
                dict.set(PREFIX_COMPUTED + JDBC_DRIVER, "com.ibm.db2.jcc.DB2Driver");
            } else if (url.startsWith("jdbc:microsoft:")) {
                dict.set(PREFIX_COMPUTED + JDBC_DRIVER, "com.microsoft.sqlserver.jdbc.SQLServerDriver");
            }
        } else {
            dict.set(PREFIX_COMPUTED + JDBC_DRIVER, driver);
        }
        if (ping.length() == 0 && url.startsWith("jdbc:oracle:")) {
            dict.set(PREFIX_COMPUTED + JDBC_PING, "SELECT 1 FROM dual");
        } else if (ping.length() == 0) {
            dict.set(PREFIX_COMPUTED + JDBC_PING, "SELECT 1");
        } else {
            dict.set(PREFIX_COMPUTED + JDBC_PING, ping);
        }
        dict.setBoolean(PREFIX_COMPUTED + JDBC_AUTOCOMMIT, autoCommit());
        dict.setInt(PREFIX_COMPUTED + JDBC_TIMEOUT, timeout());
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
     * Returns the JDBC driver class for this connection. The class
     * will be loaded using the application context class loader.
     *
     * @return the JDBC driver class
     *
     * @throws ConnectionException if the class couldn't be found
     *             or wasn't of the correct Java type
     */
    public Driver driver() throws ConnectionException {
        String driverClass;
        if (dict.containsKey(PREFIX_COMPUTED + JDBC_DRIVER)) {
            driverClass = dict.getString(PREFIX_COMPUTED + JDBC_DRIVER, "");
        } else {
            driverClass = dict.getString(JDBC_DRIVER, "");
        }
        String msg;
        try {
            ClassLoader loader = ApplicationContext.getInstance().getClassLoader();
            return (Driver) loader.loadClass(driverClass).getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            msg = "couldn't find or load JDBC driver class " + driverClass +
                  ": class not found";
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
    }

    /**
     * Returns the JDBC connection URL.
     *
     * @return the JDBC connection URL
     */
    public String url() {
        if (dict.containsKey(PREFIX_COMPUTED + JDBC_URL)) {
            return dict.getString(PREFIX_COMPUTED + JDBC_URL, "");
        } else {
            return dict.getString(JDBC_URL, "");
        }
    }

    /**
     * Returns the SQL ping query.
     *
     * @return the SQL ping query, or
     *         null if not configured
     */
    public String ping() {
        if (dict.containsKey(PREFIX_COMPUTED + JDBC_PING)) {
            return dict.getString(PREFIX_COMPUTED + JDBC_PING, null);
        } else {
            return dict.getString(JDBC_PING, null);
        }
    }

    /**
     * Returns the auto-commit (after each SQL) flag.
     *
     * @return the auto-commit flag
     */
    public boolean autoCommit() {
        if (dict.containsKey(PREFIX_COMPUTED + JDBC_AUTOCOMMIT)) {
            return dict.getBoolean(PREFIX_COMPUTED + JDBC_AUTOCOMMIT, false);
        } else {
            return dict.getBoolean(JDBC_AUTOCOMMIT, false);
        }
    }

    /**
     * Returns the connection and query timeout (in seconds).
     *
     * @return the connection and query timeout (in seconds)
     */
    public int timeout() {
        try {
            if (dict.containsKey(PREFIX_COMPUTED + JDBC_TIMEOUT)) {
                return dict.getInt(PREFIX_COMPUTED + JDBC_TIMEOUT, 30);
            } else {
                return dict.getInt(JDBC_TIMEOUT, 30);
            }
        } catch (Exception e) {
            LOG.warning(this + ": failed to parse timeout value: " +
                        dict.get(JDBC_TIMEOUT));
            dict.setInt(PREFIX_COMPUTED + JDBC_TIMEOUT, 30);
            return 30;
        }
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
        Properties props = new Properties();
        for (String key : dict.keys()) {
            if (key.startsWith(PREFIX_HIDDEN)) {
                String name = key.substring(PREFIX_HIDDEN.length());
                props.setProperty(name, dict.getString(key, ""));
            } else if (!key.startsWith(PREFIX_COMPUTED)) {
                props.setProperty(key, dict.getString(key, ""));
            }
        }
        props.remove(KEY_ID);
        props.remove(KEY_TYPE);
        props.remove(KEY_MAX_OPEN);
        props.remove(KEY_MAX_IDLE_SECS);
        return new JdbcChannel(this, props);
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
