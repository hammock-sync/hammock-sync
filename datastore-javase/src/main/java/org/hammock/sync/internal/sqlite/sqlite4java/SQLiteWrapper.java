/*
 * Copyright © 2013, 2016 IBM Corp. All rights reserved.
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

import org.hammock.sync.internal.android.ContentValues;
import org.hammock.sync.internal.sqlite.SQLDatabase;
import org.hammock.sync.internal.util.Misc;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Implementation of SQLDatabase backed by sqlite4java.
 */
public class SQLiteWrapper extends SQLDatabase {

    private static final Logger logger = Logger.getLogger(SQLiteWrapper.class.getCanonicalName());

    private static final String[] CONFLICT_VALUES = new String[]
            {"", " OR ROLLBACK ", " OR ABORT ", " OR FAIL ", " OR IGNORE ", " OR REPLACE "};

    private final File databaseFile;

    private Connection localConnection;

    /**
     * Tracks whether the current nested set of transactions has had any
     * failed transactions so far.
     */
    private Boolean transactionNestedSetSuccess = Boolean.FALSE;

    /**
     * Stack to track whether the current transaction is successful.
     * As transactions are started, their status is pushed onto the stack.
     * When complete, the status is popped and used to update
     * {@see SQLiteWrapper#transactionNestedSetSuccess}
     */
    private final Stack<Boolean> transactionStack = new Stack<>();

    public SQLiteWrapper(File databaseFile) {
        this.databaseFile = databaseFile;
    }

    public static SQLiteWrapper open(File databaseFile) {
        SQLiteWrapper db = new SQLiteWrapper(databaseFile);
        db.open();
        return db;
    }

    public Connection getConnection() {
        if (localConnection == null) {
            localConnection = createNewConnection();
        }

        return localConnection;
    }

    public Connection createNewConnection() {
        try {
            Connection conn;
            if (this.databaseFile != null) {
               conn = DriverManager.getConnection("jdbc:sqlite:" + this.databaseFile.getAbsolutePath());
                //conn = new Connection(this.databaseFile);

            } else {
                // conn = new Connection();
                conn = DriverManager.getConnection("jdbc:sqlite:");
            }

            return conn;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to open database.", ex);
        }
    }

    @Override
    public void open() {
    }

    @Override
    public void compactDatabase() {
        try {
            this.execSQL("VACUUM");
        } catch (SQLException e) {
            String error = "Fatal error running 'VACUUM', the database is probably malfunctioning.";
            throw new IllegalStateException(error);
        }
    }

    @Override
    public int getVersion() {
        try {
            return SQLiteWrapperUtils.longForQuery(getConnection(), "PRAGMA user_version;").intValue();
        } catch (SQLException e) {
            throw new IllegalStateException("Can not query for the user_version?", e);
        }
    }

    @Override
    public boolean isOpen() {
        try {
            return getConnection().isValid(500);
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void beginTransaction() {
        Misc.checkState(this.isOpen(), "db must be open");

        // All transaction state variables are thread-local,
        // so we don't have to lock.

        // Start new set of nested transactions
        if(this.transactionStack.isEmpty()) {
            try {
                this.execSQL("BEGIN EXCLUSIVE;");
            } catch (SQLException e) {
                String error = "Fatal error running 'BEGIN', the database is probably malfunctioning.";
                throw new IllegalStateException(error);
            }

            // We assume the set as a whole is successful. If any of the
            // transactions in the set fail, this will be set to false
            // before we commit or rollback.
            transactionNestedSetSuccess = true;
        }

        // This is set to true by setTransactionSuccessful(), if that method
        // is called. If it's still false at the end of this transaction,
        // transactionNestedSetSuccess is set to false.
        transactionStack.push(false);
    }

    @Override
    public void endTransaction() {
        Misc.checkState(this.isOpen(), "db must be open");
        Misc.checkState(!this.transactionStack.isEmpty(),
                "TransactionStatus stack must not be empty");

        // All transaction state variables are thread-local,
        // so we don't have to lock.

        Boolean success = this.transactionStack.pop();
        if (!success) {
            transactionNestedSetSuccess =false;
        }

        if(this.transactionStack.isEmpty()) {
            // We've reached the top of the stack, and need to commit or
            // rollback. At this point transactionNestedSetSuccess will be true
            // iff no transactions in the set failed.
            try {
                if (transactionNestedSetSuccess) {
                    this.execSQL("COMMIT;");
                } else {
                    this.execSQL("ROLLBACK;");
                }
            } catch (java.sql.SQLException e) {
                try {
                    this.execSQL("ROLLBACK;");
                } catch (Exception e2) {
                    String error = "Fatal error running 'ROLLBACK', the database is probably malfunctioning.";
                    throw new IllegalStateException(error);
                }
            }
        }
    }

    @Override
    public void setTransactionSuccessful() {
        Misc.checkState(this.isOpen(), "db must be open");

        // Pop the false value off and replace it with true.
        // As the stack is thread-local, this is thread-safe
        // and we need not lock.
        this.transactionStack.pop();
        this.transactionStack.push(true);
    }

    @Override
    public void close() {
        // it's not possible to call dispose from other threads
        // so the best we can do is call dispose on the connection
        // for the same thread as us
        Connection conn = localConnection;
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            // Silent close
        }
    }

    @Override
    public void execSQL(String sql) throws SQLException {
        Misc.checkNotNullOrEmpty(sql.trim(), "Input SQL");
        try (Statement stmt = this.getConnection().createStatement() ) {
            stmt.execute(sql);
        }
    }

    @Override
    public void execSQL(String sql, Object[] bindArgs) throws SQLException {
        Misc.checkNotNullOrEmpty(sql.trim(), "Input SQL");
        java.sql.PreparedStatement stmt = null;
        try {
            stmt = this.getConnection().prepareStatement(sql);
            SQLiteWrapperUtils.bindArguments(stmt, bindArgs);
            stmt.execute();
        } finally {
            SQLiteWrapperUtils.closeQuietly(stmt);
        }
    }

    @Override
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        if (values == null || values.size() == 0) {
            throw new IllegalArgumentException("Empty values");
        }

        try {
            String updateQuery = QueryBuilder.buildUpdateQuery(table, values, whereClause, whereArgs);
            Object[] bindArgs = QueryBuilder.buildBindArguments(values, whereArgs);
            return this.executeSQLStatementWithCount(updateQuery, bindArgs);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, String.format("Error updating: %s, %s, %s, %s", table,
                    values, whereClause, Arrays.toString(whereArgs)), e);
            return -1;
        }
    }

    @Override
    public SQLiteCursor rawQuery(String sql, String[] bindArgs) throws SQLException {
        return SQLiteWrapperUtils.buildSQLiteCursor(getConnection(), sql, bindArgs);
    }

    @Override
    public int delete(String table, String whereClause, String[] whereArgs) {
        try {
            String sql = "DELETE FROM \"" +
                    table +
                    "\"" +
                    (!Misc.isStringNullOrEmpty(whereClause) ? " WHERE " + whereClause : "");
            return this.executeSQLStatementWithCount(sql, whereArgs);
        } catch (SQLException e) {
            return 0;
        }
    }

    @Override
    public long insertWithOnConflict(String table, ContentValues initialValues, int conflictAlgorithm) {
        int size = (initialValues != null && initialValues.size() > 0)
                ? initialValues.size() : 0;

        if (size == 0) {
            throw new IllegalArgumentException("SQLite does not support to insert an all null row");
        }

        java.sql.PreparedStatement stmt = null;
        java.sql.ResultSet rs = null;
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT ");
            sql.append(CONFLICT_VALUES[conflictAlgorithm]);
            sql.append(" INTO \"");
            sql.append(table);
            sql.append("\"");
            sql.append('(');


            Object[] bindArgs  = new Object[size];
            int i = 0;
            for (String colName : initialValues.keySet()) {
                sql.append((i > 0) ? "," : "");
                sql.append(colName);
                bindArgs[i++] = initialValues.get(colName);
            }
            sql.append(')');
            sql.append(" VALUES (");
            for (i = 0; i < size; i++) {
                sql.append((i > 0) ? ",?" : "?");
            }

            sql.append(')');

            stmt = getConnection().prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS);
            SQLiteWrapperUtils.bindArguments(stmt, bindArgs);
            stmt.executeUpdate();

            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            } else {
                return -1;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, String.format("Error inserting to: %s, %s, %s", table,
                    initialValues, CONFLICT_VALUES[conflictAlgorithm]), e);
            return -1;
        } finally {
            SQLiteWrapperUtils.closeQuietly(rs);
            SQLiteWrapperUtils.closeQuietly(stmt);
        }
    }

    @Override
    public long insert(String table, ContentValues initialValues) {
        return insertWithOnConflict(table, initialValues, CONFLICT_NONE);
    }

    private int executeSQLStatementWithCount(String sql, Object[] values) throws SQLException {
        java.sql.PreparedStatement stmt = null;
        try {
            stmt = getConnection().prepareStatement(sql);
            SQLiteWrapperUtils.bindArguments(stmt, values);
            return stmt.executeUpdate();
        } finally {
            SQLiteWrapperUtils.closeQuietly(stmt);
        }
    }
}
