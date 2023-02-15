package org.hammock.sync.internal.sqlite.h2;

import java.sql.Types;

public interface SQLiteConstants {
    int SQLITE_NULL = Types.NULL;
    int SQLITE_TEXT = Types.VARCHAR;
    int SQLITE_INTEGER = Types.INTEGER;
    int SQLITE_FLOAT = Types.FLOAT;
    int SQLITE_BLOB = Types.BLOB;
    int SQLITE_REAL = Types.REAL;
    int SQLITE_BOOLEAN = Types.BOOLEAN;
    int SQLITE_NUMERIC = Types.NUMERIC;
}
