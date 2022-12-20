/*
 * RapidContext JDBC plug-in <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2022 Per Cederberg. All rights reserved.
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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.ConnectionException;
import org.rapidcontext.core.type.Procedure;

/**
 * A base JDBC procedure. This procedure provides common methods for
 * the JDBC procedures.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public abstract class JdbcProcedure extends Procedure {

    /**
     * The binding name for the adapter connection pool.
     */
    public static final String BINDING_DB = "db";

    /**
     * The binding name for the SQL query or statement.
     */
    public static final String BINDING_SQL = "sql";

    /**
     * The binding name for the processing and mapping flags.
     */
    public static final String BINDING_FLAGS = "flags";

    /**
     * Finds or reserved the JDBC connection from the specified bindings.
     *
     * @param cx             the procedure call context
     * @param bindings       the call bindings to use
     *
     * @return the JDBC connection channel reserved
     *
     * @throws ProcedureException if no JDBC connection was found
     */
    protected static JdbcChannel connectionReserve(CallContext cx, Bindings bindings)
    throws ProcedureException {

        Object obj = bindings.getValue(BINDING_DB);
        if (obj instanceof String) {
            obj = cx.connectionReserve((String) obj);
        }
        if (!JdbcChannel.class.isInstance(obj)) {
            String msg = "connection not of JDBC type: " + obj.getClass().getName();
            throw new ProcedureException(msg);
        }
        return (JdbcChannel) obj;
    }

    /**
     * Creates a new procedure from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public JdbcProcedure(String id, String type, Dict dict) {
        super(id, type, dict);
    }

    /**
     * Executes a call of this procedure in the specified context
     * and with the specified call bindings. The semantics of what
     * the procedure actually does, is up to each implementation.
     * Note that the call bindings are normally inherited from the
     * procedure bindings with arguments bound to their call values.
     *
     * @param cx             the procedure call context
     * @param bindings       the call bindings to use
     *
     * @return the result of the call, or
     *         null if the call produced no result
     *
     * @throws ProcedureException if the call execution caused an
     *             error
     */
    public Object call(CallContext cx, Bindings bindings)
    throws ProcedureException {

        JdbcChannel con = connectionReserve(cx, bindings);
        String flags = "";
        if (bindings.hasName(BINDING_FLAGS)) {
            flags = (String) bindings.getValue(BINDING_FLAGS, "");
        }
        Object res = null;
        try (PreparedStatement stmt = prepare(con, cx, bindings)) {
            res = execute(con, stmt, flags.toLowerCase());
        } catch (SQLException ignore) {
            // Do nothing
        }
        return res;
    }

    /**
     * Executes an SQL query or statement on the specified connection.
     *
     * @param con            the JDBC connection to use
     * @param stmt           the SQL prepared statement
     * @param flags          the processing and mapping flags
     *
     * @return the query results, or
     *         null for statements
     *
     * @throws ProcedureException if the SQL couldn't be executed
     *             correctly
     */
    protected abstract Object execute(JdbcChannel con,
                                      PreparedStatement stmt,
                                      String flags)
    throws ProcedureException;

    /**
     * Prepares an SQL query or statement. The SQL code and
     * parameter values will be fetched from the specified bindings.
     *
     * @param con            the JDBC connection to use
     * @param cx             the procedure call context
     * @param bindings       the data bindings to use for lookup
     *
     * @return the prepared SQL statement
     *
     * @throws ProcedureException if the SQL referred to an argument
     *             that wasn't defined in the bindings
     */
    protected PreparedStatement prepare(JdbcChannel con,
                                        CallContext cx,
                                        Bindings bindings)
    throws ProcedureException {

        String sql = (String) bindings.getValue(BINDING_SQL);
        ArrayList<SqlField> fields = new ArrayList<>();
        for (String name : bindings.getNames()) {
            if (bindings.getType(name) == Bindings.ARGUMENT) {
                int pos = 0;
                while ((pos = sql.indexOf(":" + name, pos)) >= 0) {
                    SqlField field = new SqlField(sql, pos, name);
                    fields.add(field);
                    pos = field.endPos;
                }
            }
        }
        Collections.sort(fields);
        ArrayList<Object> params = new ArrayList<>();
        StringBuffer buffer = new StringBuffer();
        int pos = 0;
        for (SqlField field : fields) {
            Object value = bindings.getValue(field.fieldName, null);
            buffer.append(sql.substring(pos, field.startPos));
            buffer.append(field.bind(value, params));
            pos = field.endPos;
        }
        buffer.append(sql.substring(pos));
        try {
            if (cx.isTracing()) {
                cx.log("JDBC " + con.getConnection() + " SQL:");
                cx.log(buffer.toString());
            }
            return con.prepare(buffer.toString(), params);
        } catch (ConnectionException e) {
            throw new ProcedureException(e.getMessage());
        }
    }

    /**
     * An SQL value replacement field. The field is dynamically bound
     * to a value, which may cause minor changes to the SQL syntax
     * around the actual variable.
     *
     * @author   Per Cederberg
     * @version  1.0
     */
    private static class SqlField implements Comparable<SqlField> {

        /**
         * A regular expression to find the SQL WHERE clause.
         */
        private static final Pattern RE_WHERE =
            Pattern.compile("\\s(WHERE|JOIN)\\s", Pattern.CASE_INSENSITIVE);

        /**
         * The start index of the field (in the SQL buffer).
         */
        public int startPos;

        /**
         * The end index of the field (in the SQL buffer).
         */
        public int endPos;

        /**
         * The field name.
         */
        public String fieldName;

        /**
         * The optional SQL operator preceding the field.
         */
        private String operator = null;

        /**
         * The optional SQL column name preceding the operator and
         * field.
         */
        private String column = null;

        /**
         * The null value skip flag.
         */
        public boolean skipNulls = false;

        /**
         * Creates a new SQL field from the specified text, position
         * and field name.
         *
         * @param sql        the SQL buffer text
         * @param pos        the buffer position
         * @param name       the field name
         */
        public SqlField(String sql, int pos, String name) {
            this.startPos = pos;
            this.endPos = pos + name.length() + 1;
            this.fieldName = name;
            pos = findOperator(sql, this.startPos);
            if (pos >= 0) {
                this.operator = sql.substring(pos, this.startPos - 1).trim();
                if (this.operator.startsWith("?")) {
                    this.operator = this.operator.substring(1);
                    this.skipNulls = true;
                }
                this.startPos = pos;
                pos = findColumn(sql, pos);
                if (pos >= 0) {
                    this.column = sql.substring(pos, this.startPos).trim();
                    this.startPos = pos;
                }
            }
        }

        /**
         * Compares this object to another. This method will compare
         * this field to another based on the start position in the
         * SQL buffer.
         *
         * @param obj        the object to compare to
         *
         * @return a negative value if this object is less than,
         *         zero (0) if the objects are equal, or
         *         a positive value if this object is greater than
         *         the other object
         *
         * @throws ClassCastException if the other object isn't an
         *             SQLField instance
         */
        public int compareTo(SqlField obj) throws ClassCastException {
            return this.startPos - obj.startPos;
        }

        /**
         * Finds the optional operator in the SQL buffer text.
         *
         * @param sql        the SQL buffer text
         * @param end        the end position
         *
         * @return the start position of the operator, or
         *         a negative number if not found
         */
        private int findOperator(String sql, int end) {
            Matcher m = RE_WHERE.matcher(sql);
            if (!m.find() || m.end() > end) {
                return -1;
            }
            String opChars = "=!?";
            int pos = end - 1;
            while (pos >= 0) {
                char c = sql.charAt(pos);
                if (!Character.isWhitespace(c) && opChars.indexOf(c) < 0) {
                    pos++;
                    break;
                }
                pos--;
            }
            if (sql.substring(pos, end).trim().length() > 0) {
                return pos;
            } else {
                return -1;
            }
        }

        /**
         * Finds the optional column name in the SQL buffer text.
         *
         * @param sql        the SQL buffer text
         * @param end        the end position
         *
         * @return the start position of the column, or
         *         a negative number if not found
         */
        public int findColumn(String sql, int end) {
            String extraChars = "'`\".";
            int pos = end - 1;
            while (pos >= 0) {
                char c = sql.charAt(pos);
                if (!Character.isJavaIdentifierPart(c) && extraChars.indexOf(c) < 0) {
                    pos++;
                    break;
                }
                pos--;
            }
            if (pos < end) {
                return pos;
            } else {
                return -1;
            }
        }

        /**
         * Binds the specified object value to this field.
         *
         * @param value      the value to bind
         * @param params     the array of SQL parameters
         *
         * @return the new SQL text for the field
         */
        public String bind(Object value, ArrayList<Object> params) {
            if (value == null) {
                return bindNull();
            } else if (value instanceof Array) {
                return bindData((Array) value, params);
            } else if (value instanceof Date) {
                params.add(new java.sql.Timestamp(((Date) value).getTime()));
                return cond(column, operator, "?");
            } else {
                params.add(value);
                return cond(column, operator, "?");
            }
        }

        /**
         * Binds the specified array to this field.
         *
         * @param value      the array to bind
         * @param params     the array of SQL parameters
         *
         * @return the new SQL text for the field
         */
        private String bindData(Array value, ArrayList<Object> params) {
            String op;
            if (operator == null) {
                op = null;
            } else if (operator.equals("=")) {
                op = "IN";
            } else if (operator.equals("!=")) {
                op = "NOT IN";
            } else {
                op = operator;
            }
            if (value.size() <= 0) {
                return bindNull();
            } else {
                return cond(column, op, literal(value));
            }
        }

        /**
         * Binds a null value to this field.
         *
         * @return the new SQL text for the field
         */
        private String bindNull() {
            String op;
            if (operator == null) {
                op = null;
            } else if (operator.equals("=")) {
                op = "IS";
            } else if (operator.equals("!=")) {
                op = "IS NOT";
            } else {
                op = operator;
            }
            if (column != null && skipNulls) {
                return "1 = 1";
            } else {
                return cond(column, op, "NULL");
            }
        }

        /**
         * Creates an SQL condition from the specified values.
         *
         * @param col        the optional column name
         * @param op         the optional operator
         * @param value      the literal SQL value
         *
         * @return the SQL condition text
         */
        private String cond(String col, String op, String value) {
            if (col != null) {
                return col + " " + op + " " + value;
            } else if (op != null) {
                return " " + op + " " + value;
            } else {
                return value;
            }
        }

        /**
         * Returns an SQL literal for a string, number or list value.
         * This function also handles null values by returning "NULL".
         *
         * @param obj            the object to convert
         *
         * @return the corresponding SQL literal
         */
        private String literal(Object obj) {
            if (obj == null) {
                return "NULL";
            } else if (obj instanceof String) {
                return literal((String) obj);
            } else if (obj instanceof Number) {
                return literal((Number) obj);
            } else {
                return literal(obj.toString());
            }
        }

        /**
         * Returns an SQL string literal.
         *
         * @param str            the string to convert
         *
         * @return the corresponding SQL string literal
         */
        private String literal(String str) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("'");
            for (int i = 0; i < str.length(); i++) {
                switch (str.charAt(i)) {
                case '\'':
                    buffer.append("''");
                    break;
                default:
                    buffer.append(str.charAt(i));
                    break;
                }
            }
            buffer.append("'");
            return buffer.toString();
        }

        /**
         * Returns an SQL number literal.
         *
         * @param num            the number to convert
         *
         * @return the corresponding SQL number literal
         */
        private String literal(Number num) {
            int i = num.intValue();
            double d = num.doubleValue();
            if (i == d) {
                return String.valueOf(i);
            } else {
                // TODO: proper number formatting should be used
                return num.toString();
            }
        }

        /**
         * Returns an SQL parenthesized list of literal values.
         *
         * @param list           the array to convert
         *
         * @return the corresponding SQL list of literals
         */
        private String literal(Array list) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("(");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    buffer.append(",");
                }
                buffer.append(literal(list.get(i)));
            }
            buffer.append(")");
            return buffer.toString();
        }
    }
}
