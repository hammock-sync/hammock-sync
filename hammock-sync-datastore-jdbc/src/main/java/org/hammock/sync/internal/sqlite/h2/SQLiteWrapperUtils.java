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

package org.hammock.sync.internal.sqlite.h2;

import org.sqlite.SQLiteConnection;

import org.sqlite.SQLiteException;

import org.hammock.sync.internal.sqlite.Cursor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SQLiteWrapperUtils {

    private static final String LOG_TAG = "SQLiteWrapperUtils";
    private static final Logger logger =
            Logger.getLogger(SQLiteWrapperUtils.class.getCanonicalName());

    public static Long longForQuery(SQLiteConnection conn, String query)
            throws SQLException {
        return SQLiteWrapperUtils.longForQuery(conn, query, null);
    }

    /**
     * Utility method to run the query on the db and return the value in the
     * first column of the first row.
     */
    public static Long longForQuery(SQLiteConnection conn, String query, Object[] bindArgs)
            throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            if (bindArgs != null && bindArgs.length > 0) {
                SQLiteWrapperUtils.bindArguments(stmt, bindArgs);
            }
            if (stmt.execute()) {
                try (ResultSet rs = stmt.getResultSet()) {
                    rs.next();
                    return rs.getLong(1);
                }
            } else {
                throw new IllegalStateException("query failed to return any result: " + query);
            }
        }
    }

    /**
     * Utility method to run the query on the db and return the value in the
     * first column of the first row.
     */
    public static int intForQuery(SQLiteConnection conn, String query, Object[] bindArgs) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            bindArguments(stmt, bindArgs);

            if (stmt.execute()) {
                try (ResultSet rs = stmt.getResultSet()) {
                    rs.next();
                    return rs.getInt(1);
                }
            } else {
                throw new IllegalStateException("query failed to return any result: " + query);
            }
        }

    }

    public static PreparedStatement bindArguments(PreparedStatement stmt, Object[] bindArgs)
            throws SQLException {

        if (bindArgs == null) {
            bindArgs = new Object[]{};
        }

        final int count = bindArgs.length;
        if (count != stmt.getParameterMetaData().getParameterCount()) {
            throw new IllegalArgumentException(
                    "Expected " + stmt.getParameterMetaData().getParameterCount() + " bind " +
                            "arguments but "
                            + bindArgs.length + " were provided.");
        }
        if (count == 0) {
            return stmt;
        }

        for (int i = 0; i < count; i++) {
            final Object arg = bindArgs[i];
            switch (DBUtils.getTypeOfObject(arg)) {
                case Cursor.FIELD_TYPE_NULL:
                    stmt.setNull(i + 1, 0 /*TODO: Set column type*/);
                    break;
                case Cursor.FIELD_TYPE_INTEGER:
                    stmt.setLong(i + 1, ((Number) arg).longValue());
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    stmt.setDouble(i + 1, ((Number) arg).doubleValue());
                    break;
                case Cursor.FIELD_TYPE_BLOB:
                    // Blob is not supported by the driver
                    //stmt.setBlob(i + 1, new ByteArrayInputStream((byte[]) arg));
                    stmt.setBytes(i + 1, (byte[]) arg);
                    break;
                case Cursor.FIELD_TYPE_STRING:
                default:
                    if (arg instanceof Boolean) {
                        // Provide compatibility with legacy applications which may pass
                        // Boolean values in bind args.
                        stmt.setInt(i + 1, ((Boolean) arg).booleanValue() ? 1 : 0);
                    } else {
                        stmt.setString(i + 1, arg.toString());
                    }
                    break;
            }
        }

        return stmt;
    }

    static void disposeQuietly(Statement stmt) {
        try {
            if (stmt != null && !stmt.isClosed()) {
                stmt.close();
            }
        } catch (Throwable e) {
        }
    }

    public static SQLiteCursor buildSQLiteCursor(SQLiteConnection conn, String sql,
                                                 Object[] bindArgs)
            throws SQLiteException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindArguments(stmt, bindArgs);
            List<String> columnNames = null;
            List<Tuple> resultSet = new ArrayList<Tuple>();
            if (stmt.execute()) {
                try (ResultSet rs = stmt.getResultSet()) {
                    while (rs.next()) {
                        if (columnNames == null) {
                            columnNames = getColumnNames(rs);
                        }
                        Tuple t = getDataRow(rs);
                        logger.finest("Tuple: " + t.toString());
                        resultSet.add(t);
                    }
                }
            }
            return new SQLiteCursor(columnNames, resultSet);
        } catch (SQLException e) {
            //TODO: Manage exception
            e.printStackTrace();
        }
        return null;
    }

    static Tuple getDataRow(ResultSet rs) throws SQLException {
        logger.entering("com.cloudant.sync.internal.sqlite.sqlite4java.SQLiteWrapperUtils",
                "getDataRow", rs);
        Tuple result = new Tuple(getColumnTypes(rs));
        for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
            Integer type = rs.getMetaData().getColumnType(i + 1);
//            Log.v(LOG_TAG, "i: " + i + ", type: " + mapColumnType(type) + ", expected type: " +
//            result.getType(i));
            switch (type) {
                case SQLiteConstants.SQLITE_NULL:
                    result.put(i);
                    break;
                case SQLiteConstants.SQLITE_TEXT:
                    String val = rs.getString(i + 1);
                    if (rs.wasNull()) {
                        result.putNull(i, Cursor.FIELD_TYPE_STRING);
                    } else {
                        result.put(i, val);
                    }
                    break;
                case SQLiteConstants.SQLITE_INTEGER:
                case SQLiteConstants.SQLITE_NUMERIC:
                case SQLiteConstants.SQLITE_BOOLEAN:
                    long longVal = rs.getLong(i + 1);
                    if (rs.wasNull()) {
                        result.putNull(i, Cursor.FIELD_TYPE_INTEGER);
                    } else {
                        result.put(i, longVal);
                    }
                    break;
                case SQLiteConstants.SQLITE_FLOAT:
                case SQLiteConstants.SQLITE_REAL:
                    double doubleVal = rs.getDouble(i + 1);
                    if (rs.wasNull()) {
                        result.putNull(i, Cursor.FIELD_TYPE_FLOAT);
                    } else {
                        result.put(i, Double.valueOf(doubleVal).floatValue());
                    }
                    break;
                case SQLiteConstants.SQLITE_BLOB:
                    byte[] arr = rs.getBytes(i + 1);
                    if (rs.wasNull()) {
                        result.putNull(i, Cursor.FIELD_TYPE_BLOB);
                    } else {
                        result.put(i, arr);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported data type: " + type);
            }
        }
        return result;
    }

    static List<String> getColumnNames(ResultSet rs) throws SQLException {
//        Log.v(LOG_TAG, "getColumnNames()");

        List<String> columnNames = new ArrayList<String>();
        int columnCount = rs.getMetaData().getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            columnNames.add(i, rs.getMetaData().getColumnName(i + 1));
        }
//        Log.v(LOG_TAG, "columnNames:" + columnNames);
        return columnNames;
    }

    static List<Integer> getColumnTypes(ResultSet rs) throws SQLException {
//        Log.v(LOG_TAG, "getColumnTypes()");
        List<Integer> columnTypes = new ArrayList<Integer>();
        int columnCount = rs.getMetaData().getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            columnTypes.add(i, mapColumnType(rs.getMetaData().getColumnType(i + 1)));
        }
//        Log.v(LOG_TAG, "columnTypes:" + columnTypes);
        return columnTypes;
    }

    static int mapColumnType(int columnType) {
        switch (columnType) {
            case SQLiteConstants.SQLITE_NULL:
                return Cursor.FIELD_TYPE_NULL;
            case SQLiteConstants.SQLITE_TEXT:
                return Cursor.FIELD_TYPE_STRING;
            case SQLiteConstants.SQLITE_INTEGER:
            case SQLiteConstants.SQLITE_NUMERIC:
            case SQLiteConstants.SQLITE_BOOLEAN:
                return Cursor.FIELD_TYPE_INTEGER;
            case SQLiteConstants.SQLITE_FLOAT:
            case SQLiteConstants.SQLITE_REAL:
                return Cursor.FIELD_TYPE_FLOAT;
            case SQLiteConstants.SQLITE_BLOB:
                return Cursor.FIELD_TYPE_BLOB;
            default:
                throw new IllegalArgumentException("Unsupported data type? :" + columnType);
        }
    }
}
