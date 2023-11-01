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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.rapidcontext.core.type.Channel;
import org.rapidcontext.core.type.ConnectionException;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.util.DateUtil;

/**
 * A JDBC communications channel. This class encapsulates a JDBC
 * connection and allows execution of arbitrary SQL queries or
 * statements.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class JdbcChannel extends Channel {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(JdbcChannel.class.getName());

    /**
     * The instance counter, used to identify JDBC connections.
     */
    private static int counter = 0;

    /**
     * The encapsulated JDBC connection.
     */
    protected Connection con;

    /**
     * The JDBC connection id.
     */
    protected String prefix;

    /**
     * The SQL initialization statement.
     */
    protected String sqlInit;

    /**
     * The SQL ping query.
     */
    protected String sqlPing;

    /**
     * The SQL connection and query timeout.
     */
    protected int timeout;

    /**
     * The start time for the current query or statement. If set to
     * zero (0), usage reporting will not include a statement/query.
     */
    private long startTime = 0;

    /**
     * Creates a new JDBC communications channel.
     *
     * @param parent            the parent JDBC connection
     * @param props             the connection properties (user and password)
     *
     * @throws ConnectionException if a connection couldn't be established
     */
    protected JdbcChannel(JdbcConnection parent, Properties props)
    throws ConnectionException {

        super(parent);
        this.prefix = "[JDBC:" + (++counter) + "] ";
        this.sqlInit = parent.sqlInit();
        this.sqlPing = parent.ping();
        this.timeout = parent.timeout();
        try {
            LOG.fine(prefix + "creating connection for " + parent.url());
            DriverManager.registerDriver(parent.driver());
            DriverManager.setLoginTimeout(timeout);
            con = DriverManager.getConnection(parent.url(), props);
            con.setAutoCommit(parent.autoCommit());
            LOG.fine(prefix + "done creating connection for " + parent.url());
        } catch (SQLException e) {
            String msg = "failed to connect to " + parent.url() +
                  " with username '" + props.getProperty("user") + "': " +
                  e.getMessage();
            LOG.warning(prefix + msg);
            throw new ConnectionException(msg);
        }
    }

    /**
     * Checks if this channel can be pooled (i.e. reused). This
     * method should return the same value for all instances of a
     * specific channel subclass.
     *
     * @return true if the channel can be pooled and reused, or
     *         false otherwise
     */
    @Override
    protected boolean isPoolable() {
        return true;
    }

    /**
     * Reserves and activates the channel. This method is called just
     * before a channel is to be used, i.e. when a new channel has
     * been created or fetched from a resource pool.
     *
     * @throws ConnectionException if the channel couldn't be
     *             reserved (channel will be destroyed)
     */
    @Override
    protected void reserve() throws ConnectionException {
        try {
            if (con.isClosed()) {
                String msg = "failed to reserve, connection channel already closed";
                LOG.warning(prefix + msg);
                throw new ConnectionException(msg);
            }
        } catch (SQLException e) {
            String msg = "failed to reserve: " + e.getMessage();
            LOG.warning(prefix + msg);
            throw new ConnectionException(msg);
        }
    }

    /**
     * Releases and passivates the channel. This method is called
     * just after a channel has been used and returned. This should
     * clear or reset the channel, so that it can safely be used
     * again without affecting previous results or operations (if
     * the channel is pooled).
     */
    @Override
    protected void release() {
        // Nothing to do here
    }

    /**
     * Checks if the channel connection is still valid. This method
     * is called before using a channel and regularly when it is idle
     * in the pool. It can be used to trigger a "ping" for a channel.
     * This method can only mark a valid channel as invalid, never
     * the other way around.
     *
     * @see #isValid()
     * @see #invalidate()
     */
    @Override
    public void validate() {
        if (sqlPing != null && sqlPing.trim().length() > 0) {
            reset();
            startTime = 0;
            try {
                executeQuery(sqlPing);
            } catch (Exception e) {
                LOG.log(Level.WARNING, prefix + "validation failure", e);
                invalidate();
            }
        }
    }

    /**
     * Closes the connection. This method is used to free any
     * resources used by the connection.
     */
    protected void close() {
        try {
            LOG.fine(prefix + "closing connection");
            con.close();
            LOG.fine(prefix + "done closing connection");
        } catch (SQLException e) {
            LOG.log(Level.WARNING, prefix + "failed to close connection", e);
        }
    }

    /**
     * Commits any pending changes. This method is called after each
     * successful procedure tree execution that included this
     * connection.
     */
    @Override
    public void commit() {
        try {
            con.commit();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, prefix + "failed to commit connection", e);
        }
    }

    /**
     * Rolls any pending changes back. This method is called after an
     * unsuccessful procedure tree execution that included this
     * connection.
     */
    @Override
    public void rollback() {
        try {
            con.rollback();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, prefix + "failed to rollback connection", e);
        }
    }

    /**
     * Resets this connection before usage. This will execute any SQL
     * initialization statements for the connection before usage.
     */
    public void reset() {
        if (sqlInit != null) {
            startTime = 0;
            try {
                executeStatement(sqlInit);
            } catch (Exception e) {
                LOG.log(Level.WARNING, prefix + "connection SQL init failure", e);
            }
        }
        startTime = System.currentTimeMillis();
    }

    /**
     * Extracts database meta-data from the connection.
     *
     * @return the database meta-data object
     *
     * @throws ConnectionException if the extraction failed
     */
    public Dict metadata() throws ConnectionException {
        try {
            DatabaseMetaData meta = con.getMetaData();
            Dict res = new Dict();
            res.set("dbName", meta.getDatabaseProductName());
            res.set("dbVersion", meta.getDatabaseProductVersion());
            res.set("dbVersionMajor", meta.getDatabaseMajorVersion());
            res.set("dbVersionMinor", meta.getDatabaseMinorVersion());
            res.set("driverName", meta.getDriverName());
            res.set("driverVersion", meta.getDriverVersion());
            res.set("driverVersionMajor", meta.getDriverMajorVersion());
            res.set("driverVersionMinor", meta.getDriverMinorVersion());
            Array schemas = new Array();
            try (ResultSet rs = meta.getCatalogs()) {
                Array arr = (Array) createResults(rs, "no-column-names");
                for (Object o : arr) {
                    Array row = (Array) o;
                    schemas.add(row.get(0));
                }
            }
            try (ResultSet rs = meta.getSchemas()) {
                Array arr = (Array) createResults(rs, "no-column-names");
                for (Object o : arr) {
                    Array row = (Array) o;
                    schemas.add(row.get(0) + "." + row.get(1));
                }
            }
            res.set("schema", schemas);
            report(startTime, true, null);
            return res;
        } catch (SQLException e) {
            LOG.log(Level.WARNING, prefix + "failed to extract meta-data", e);
            report(startTime, false, e.toString());
            throw new ConnectionException("failed to extract meta-data: " +
                                          e.getMessage());
        }
    }

    /**
     * Executes an SQL statement.
     *
     * @param sql            the SQL statement to execute
     *
     * @return the array with generated keys
     *
     * @throws ConnectionException if the execution failed
     */
    public Array executeStatement(String sql) throws ConnectionException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(prefix + "executing statement: " + sql);
        }
        Array res = null;
        try (PreparedStatement stmt = prepare(sql, null)) {
            res = executeStatement(stmt);
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine(prefix + "done executing statement: " + sql);
            }
        } catch (SQLException ignore) {
            // Do nothing
        }
        return res;
    }

    /**
     * Executes an SQL prepared statement. The statement will be
     * closed by this method.
     *
     * @param stmt           the prepared SQL statement to execute
     *
     * @return the array with generated keys
     *
     * @throws ConnectionException if the execution failed
     */
    protected Array executeStatement(PreparedStatement stmt)
    throws ConnectionException {

        try (PreparedStatement local = stmt) {
            Array res = new Array(10);
            local.executeUpdate();
            try (ResultSet set = local.getGeneratedKeys()) {
                while (set != null && set.next()) {
                    res.add(set.getString(1));
                }
            } catch (SQLException ignore) {
                // Ignore errors on generated keys
            }
            report(startTime, true, null);
            return res;
        } catch (SQLException e) {
            String msg = "failed to execute statement: " + e.getMessage();
            LOG.warning(prefix + msg);
            report(startTime, false, msg);
            throw new ConnectionException(msg);
        }
    }

    /**
     * Executes an SQL query. Default processing flags will be used, which
     * means that column meta-data will not be included and column names will
     * be mapped into object properties.
     *
     * @param sql            the SQL query to execute
     *
     * @return the object with the result data
     *
     * @throws ConnectionException if the execution failed
     */
    public Object executeQuery(String sql) throws ConnectionException {
        return executeQuery(sql, "");
    }

    /**
     * Executes an SQL query with the specified processing flags.
     *
     * @param sql            the SQL query to execute
     * @param flags          the processing and mapping flags
     *
     * @return the object with the result data
     *
     * @throws ConnectionException if the execution failed
     */
    public Object executeQuery(String sql, String flags)
    throws ConnectionException {

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(prefix + "executing query: " + sql);
        }
        Object res = null;
        try (PreparedStatement stmt = prepare(sql, null)) {
            res = executeQuery(stmt, flags);
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine(prefix + "done executing query: " + sql);
            }
        } catch (SQLException ignore) {
            // Do nothing
        }
        return res;
    }

    /**
     * Executes a prepared SQL query. The prepared statement will be
     * closed by this method. Default processing flags will be used, which
     * means that column meta-data will not be included and column names will
     * be mapped into object properties.
     *
     * @param stmt           the prepared SQL query to execute
     *
     * @return the object with the result data
     *
     * @throws ConnectionException if the execution failed
     */
    protected Object executeQuery(PreparedStatement stmt)
    throws ConnectionException {

        return executeQuery(stmt, "");
    }

    /**
     * Executes a prepared SQL query. The prepared statement will be
     * closed by this method.
     *
     * @param stmt           the prepared SQL query to execute
     * @param flags          the processing and mapping flags
     *
     * @return the object with the result data
     *
     * @throws ConnectionException if the execution failed
     */
    protected Object executeQuery(PreparedStatement stmt, String flags)
    throws ConnectionException {

        try {
            try (ResultSet set = stmt.executeQuery()) {
                Object res = createResults(set, flags);
                report(startTime, true, null);
                return res;
            }
        } catch (SQLException e) {
            String msg = "failed to execute query: " + e.getMessage();
            LOG.warning(prefix + msg);
            report(startTime, false, msg);
            throw new ConnectionException(msg);
        } finally {
            try {
                stmt.close();
            } catch (SQLException ignore) {
                // Do nothing
            }
        }
    }

    /**
     * Prepares an SQL statement.
     *
     * @param sql            the SQL statement to prepare
     * @param params         the optional list of parameters
     *
     * @return the prepared SQL statement
     *
      @throws ConnectionException if the statement couldn't be prepared
     */
    protected PreparedStatement prepare(String sql, ArrayList<Object> params)
    throws ConnectionException {

        PreparedStatement stmt;
        try {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("preparing SQL:\n" + sql + "\n" + params);
            }
            stmt = con.prepareStatement(sql,
                                        ResultSet.TYPE_FORWARD_ONLY,
                                        ResultSet.CONCUR_READ_ONLY,
                                        ResultSet.HOLD_CURSORS_OVER_COMMIT);
            for (int i = 0; params != null && i < params.size(); i++) {
                Object obj = params.get(i);
                if (obj instanceof String && ((String) obj).length() > 255) {
                    String str = (String) params.get(i);
                    stmt.setCharacterStream(i + 1,
                                            new StringReader(str),
                                            str.length());
                } else {
                    stmt.setObject(i + 1, obj);
                }
            }
            stmt.setQueryTimeout(timeout);
            return stmt;
        } catch (SQLException e) {
            String msg = "failed to prepare SQL: " + e.getMessage();
            LOG.warning(prefix + msg + "\n" + sql + "\n" + params);
            report(startTime, false, msg);
            throw new ConnectionException(msg);
        }
    }

    /**
     * Converts a query result set to a data object.
     *
     * @param rs             the result set to convert
     * @param flags          the processing and mapping flags
     *
     * @return the data object with all the result data
     *
     * @throws ConnectionException if the result data couldn't be read
     */
    protected Object createResults(ResultSet rs, String flags)
    throws ConnectionException {

        ResultSetMetaData meta;
        try {
            meta = rs.getMetaData();
        } catch (SQLException e) {
            String msg = "failed to fetch result meta-data: " + e.getMessage();
            LOG.warning(prefix + msg);
            report(startTime, false, msg);
            throw new ConnectionException(msg);
        }
        if (hasFlag(flags, "metadata", false)) {
            Dict dict = new Dict();
            dict.set("columns", createColumnData(meta, flags));
            dict.set("rows", createRowData(meta, rs, flags));
            return dict;
        } else {
            return createRowData(meta, rs, flags);
        }
    }

    /**
     * Converts the query meta-data into an array of column data objects.
     *
     * @param meta           the result set meta-data to convert
     * @param flags          the processing and mapping flags
     *
     * @return the array of column information
     *
     * @throws ConnectionException if the result data couldn't be read
     */
    protected Array createColumnData(ResultSetMetaData meta, String flags)
    throws ConnectionException {

        Array cols;
        try {
            int colCount = meta.getColumnCount();
            cols = new Array(colCount);
            for (int i = 0; i < colCount; i++) {
                Dict obj = new Dict();
                obj.set("name", meta.getColumnLabel(i + 1).toLowerCase());
                obj.set("catalog", meta.getCatalogName(i + 1));
                obj.set("type", meta.getColumnTypeName(i + 1));
                obj.set("jdbcType", meta.getColumnType(i + 1));
                obj.set("schema", meta.getSchemaName(i + 1));
                obj.set("table", meta.getTableName(i + 1));
                obj.set("column", meta.getColumnName(i + 1));
                cols.add(obj);
            }
        } catch (SQLException e) {
            String msg = "failed to read result meta-data: " + e.getMessage();
            LOG.warning(prefix + msg);
            report(startTime, false, msg);
            throw new ConnectionException(msg);
        }
        return cols;
    }

    /**
     * Converts the query result set into an array or a dictionary
     * (depending on flags).
     *
     * @param meta           the result set meta-data
     * @param rs             the result set to convert
     * @param flags          the processing and mapping flags
     *
     * @return the array of rows or dictionary of a single row
     *
     * @throws ConnectionException if the result data couldn't be read
     */
    protected Object createRowData(ResultSetMetaData meta, ResultSet rs, String flags)
    throws ConnectionException {

        Array rows = new Array(10);
        boolean flagColumnNames = hasFlag(flags, "column-names", true);
        boolean flagNativeTypes = hasFlag(flags, "native-types", true);
        boolean flagBinaryData = hasFlag(flags, "binary-data", false);
        boolean flagSingleColumn = hasFlag(flags, "single-column", false);
        boolean flagSingleRow = hasFlag(flags, "single-row", false);
        try {
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                if (flagSingleColumn) {
                    if (colCount != 1) {
                        String msg = "too many columns in query results; " +
                                     "expected 1, but found " + colCount;
                        throw new ConnectionException(msg);
                    }
                    rows.add(createValue(meta, rs, 1, flagNativeTypes, flagBinaryData));
                } else if (flagColumnNames) {
                    Dict rowDict = new Dict();
                    for (int i = 0; i < colCount; i++) {
                        Object value = createValue(meta, rs, i + 1, flagNativeTypes, flagBinaryData);
                        rowDict.add(meta.getColumnLabel(i + 1).toLowerCase(), value);
                    }
                    rows.add(rowDict);
                } else {
                    Array rowArr = new Array(colCount);
                    for (int i = 0; i < colCount; i++) {
                        Object value = createValue(meta, rs, i + 1, flagNativeTypes, flagBinaryData);
                        rowArr.add(value);
                    }
                    rows.add(rowArr);
                }
            }
        } catch (SQLException e) {
            String msg = "failed to extract query results: " + e.getMessage();
            LOG.warning(prefix + msg);
            report(startTime, false, msg);
            throw new ConnectionException(msg);
        }
        if (flagSingleRow) {
            if (rows.size() < 1) {
                return null;
            } else if (rows.size() == 1) {
                return rows.get(0);
            } else {
                String msg = "too many rows in query results; expected 1, " +
                             "but found " + rows.size();
                LOG.info(prefix + msg);
                throw new ConnectionException(msg);
            }
        }
        return rows;
    }

    /**
     * Converts a specific row column value to a scriptable object. Normally
     * this means returning a simple string containing the value. If the native
     * types flag is set, SQL types will be converted into their native Java
     * object types by the JDBC driver. If the binary data flag is set, any
     * binary data will be returned in a byte[] instead of converted to a
     * string.
     *
     * @param meta           the result set meta-data
     * @param rs             the result set to convert
     * @param column         the column index
     * @param nativeTypes    the native value types flag
     * @param binaryData     the binary data support flag
     *
     * @return the scriptable object with the column value
     *
     * @throws ConnectionException if the result data couldn't be read
     */
    protected Object createValue(ResultSetMetaData meta,
                                 ResultSet rs,
                                 int column,
                                 boolean nativeTypes,
                                 boolean binaryData)
    throws ConnectionException {

        try {
            switch (meta.getColumnType(column)) {
            case Types.DATE:
            case Types.TIMESTAMP:
                try {
                    return DateUtil.formatIsoDateTime(rs.getTimestamp(column));
                } catch (SQLException e) {
                    try {
                        LOG.info(prefix + "discarded invalid date/time: " +
                                 rs.getString(column));
                    } catch (Exception ignore) {
                        // Nothing here
                    }
                    return null;
                }
            case Types.BINARY:
            case Types.BLOB:
            case Types.LONGVARBINARY:
            case Types.VARBINARY:
                if (binaryData) {
                    try (
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        InputStream is = rs.getBinaryStream(column);
                    ) {
                        int count;
                        byte[] buffer = new byte[16384];
                        while ((count = is.read(buffer)) > 0 && os.size() < 1000000) {
                            os.write(buffer, 0, count);
                        }
                        return os.toByteArray();
                    }
                } else {
                    return rs.getString(column);
                }
            default:
                if (nativeTypes) {
                    Object value = rs.getObject(column);
                    return isNativeValue(value) ? value : rs.getString(column);
                } else {
                    return rs.getString(column);
                }
            }
        } catch (Exception e) {
            String msg = "failed to extract query result value for column " +
                         column + ": " + e.getMessage();
            LOG.warning(prefix + msg);
            report(startTime, false, msg);
            throw new ConnectionException(msg);
        }
    }

    /**
     * Checks if a specified flag is either set or unset. I.e. this method both
     * checks for "no-whatever" and "whatever" in the flags string. If none of
     * the two variants is found, the default value is returned.
     *
     * @param flags          the flags string to check
     * @param flag           the flag name
     * @param defaultValue   the default flag value
     *
     * @return true if the flag was set, or
     *         false otherwise
     */
    protected boolean hasFlag(String flags, String flag, boolean defaultValue) {
        if (flags == null || flag == null) {
            return defaultValue;
        } else if (flags.indexOf("no-" + flag) >= 0) {
            return false;
        } else if (flags.indexOf(flag) >= 0) {
            return true;
        } else {
            return defaultValue;
        }
    }

    /**
     * Checks if a specified value is an acceptable native value. If this
     * method returns true, then the value will be returned. Otherwise a string
     * value will be extracted for the column instead. If native types are not
     * used for a query, this step will naturally be omitted.
     *
     * @param value          the value to check
     *
     * @return true if the value is of an acceptable native type, or
     *         false otherwise
     */
    protected boolean isNativeValue(Object value) {
        return value == null ||
               value instanceof Boolean ||
               value instanceof Number ||
               value instanceof String;
    }
}
