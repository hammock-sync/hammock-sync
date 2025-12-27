/*
 * Copyright © 2017 IBM Corp. All rights reserved.
 *
 * Copyright © 2013 Cloudant, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package org.hammock.sync.internal.sqlite.sqlite4java;

import org.hammock.sync.internal.sqlite.Cursor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SQLiteWrapperUtils {

    private static final Logger logger = Logger.getLogger(SQLiteWrapperUtils.class.getCanonicalName());

    public static Long longForQuery(Connection conn, String query)
            throws SQLException {
        return SQLiteWrapperUtils.longForQuery(conn, query, null);
    }

    /**
     * Utility method to run the query on the db and return the value in the
     * first column of the first row.
     */
    public static Long longForQuery(Connection conn, String query, Object[] bindArgs)
            throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(query);
            bindArguments(stmt, bindArgs);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            } else {
                throw new IllegalStateException("query failed to return any result: " + query);
            }
        } finally {
            closeQuietly(rs);
            closeQuietly(stmt);
        }
    }

    /**
     * Utility method to run the query on the db and return the value in the
     * first column of the first row.
     */
    public static int intForQuery(Connection conn, String query, Object[] bindArgs) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(query);
            bindArguments(stmt, bindArgs);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new IllegalStateException("query failed to return any result: " + query);
            }
        } finally {
            closeQuietly(rs);
            closeQuietly(stmt);
        }
    }

    public static void bindArguments(PreparedStatement stmt, Object[] bindArgs)
            throws SQLException {
        if (bindArgs == null) {
            return;
        }
        for (int i = 0; i < bindArgs.length; i++) {
            final Object arg = bindArgs[i];
            int j = i + 1;
            if (arg == null) {
                stmt.setNull(j, Types.NULL);
            } else if (arg instanceof Long) {
                stmt.setLong(j, (Long) arg);
            } else if (arg instanceof Integer) {
                stmt.setInt(j, (Integer) arg);
            } else if (arg instanceof Double) {
                stmt.setDouble(j, (Double) arg);
            } else if (arg instanceof Float) {
                stmt.setFloat(j, (Float) arg);
            } else if (arg instanceof byte[]) {
                stmt.setBytes(j, (byte[]) arg);
            } else if (arg instanceof String) {
                stmt.setString(j, (String) arg);
            } else if (arg instanceof Boolean) {
                stmt.setInt(j, ((Boolean) arg) ? 1 : 0);
            } else {
                stmt.setString(j, arg.toString());
            }
        }
    }

    static void closeQuietly(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    static void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    public static SQLiteCursor buildSQLiteCursor(Connection conn, String sql, Object[] bindArgs)
            throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql);
            bindArguments(stmt, bindArgs);
            rs = stmt.executeQuery();
            List<String> columnNames = getColumnNames(rs);
            int columnCount = columnNames.size();
            List<Tuple> resultSet = new ArrayList<Tuple>();
            while (rs.next()) {
                // Detect types per row, not per column (SQLite has dynamic typing)
                List<Integer> columnTypes = getColumnTypesForRow(rs, columnCount);
                Tuple t = getDataRow(rs, columnTypes);
                logger.finest("Tuple: " + t.toString());
                resultSet.add(t);
            }
            return new SQLiteCursor(columnNames, resultSet);
        } finally {
            closeQuietly(rs);
            closeQuietly(stmt);
        }
    }

    static Tuple getDataRow(ResultSet rs, List<Integer> columnTypes) throws SQLException {
        logger.entering("org.hammock.sync.internal.sqlite.sqlite4java.SQLiteWrapperUtils", "getDataRow", rs);
        Tuple result = new Tuple(columnTypes);
        for (int i = 0; i < columnTypes.size(); i++) {
            int colIdx = i + 1;
            int cursorType = columnTypes.get(i);
            switch (cursorType) {
                case Cursor.FIELD_TYPE_NULL:
                    result.put(i);
                    break;
                case Cursor.FIELD_TYPE_STRING:
                    result.put(i, rs.getString(colIdx));
                    break;
                case Cursor.FIELD_TYPE_INTEGER:
                    result.put(i, rs.getLong(colIdx));
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    result.put(i, rs.getFloat(colIdx));
                    break;
                case Cursor.FIELD_TYPE_BLOB:
                    result.put(i, rs.getBytes(colIdx));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported data type: " + cursorType);
            }
        }
        return result;
    }

    static List<String> getColumnNames(ResultSet rs) throws SQLException {
        List<String> columnNames = new ArrayList<String>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(metaData.getColumnName(i));
        }
        return columnNames;
    }

    static List<Integer> getColumnTypes(ResultSet rs) throws SQLException {
        List<Integer> columnTypes = new ArrayList<Integer>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            columnTypes.add(mapColumnType(metaData.getColumnType(i)));
        }
        return columnTypes;
    }

    /**
     * Get the actual column types for the current row.
     * SQLite has dynamic typing, so we need to check the actual value type for each column.
     */
    static List<Integer> getColumnTypesForRow(ResultSet rs, int columnCount) throws SQLException {
        List<Integer> columnTypes = new ArrayList<Integer>();
        for (int i = 1; i <= columnCount; i++) {
            Object value = rs.getObject(i);
            if (value == null) {
                columnTypes.add(Cursor.FIELD_TYPE_NULL);
            } else if (value instanceof String) {
                columnTypes.add(Cursor.FIELD_TYPE_STRING);
            } else if (value instanceof Long || value instanceof Integer ||
                       value instanceof Short || value instanceof Byte ||
                       value instanceof Boolean) {
                columnTypes.add(Cursor.FIELD_TYPE_INTEGER);
            } else if (value instanceof Double || value instanceof Float) {
                columnTypes.add(Cursor.FIELD_TYPE_FLOAT);
            } else if (value instanceof byte[]) {
                columnTypes.add(Cursor.FIELD_TYPE_BLOB);
            } else {
                // Default to string for unknown types
                columnTypes.add(Cursor.FIELD_TYPE_STRING);
            }
        }
        return columnTypes;
    }

    static int mapColumnType(int columnType) {
        switch (columnType) {
            case Types.NULL:
                return Cursor.FIELD_TYPE_NULL;
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
                return Cursor.FIELD_TYPE_STRING;
            case Types.INTEGER:
            case Types.NUMERIC:
            case Types.BIGINT:
            case Types.SMALLINT:
            case Types.TINYINT:
            case Types.BOOLEAN:
                return Cursor.FIELD_TYPE_INTEGER;
            case Types.REAL:
            case Types.FLOAT:
            case Types.DOUBLE:
                return Cursor.FIELD_TYPE_FLOAT;
            case Types.BLOB:
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                return Cursor.FIELD_TYPE_BLOB;
            default:
                throw new IllegalArgumentException("Unsupported data type from database: " + columnType);
        }
    }
}
