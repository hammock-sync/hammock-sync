/*
 * Copyright © 2017 IBM Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 *
 */

package org.hammock.sync.internal.query.callables;

import org.hammock.sync.internal.query.QueryConstants;
import org.hammock.sync.internal.query.TokenizerHelper;
import org.hammock.sync.internal.sqlite.Cursor;
import org.hammock.sync.internal.sqlite.SQLCallable;
import org.hammock.sync.internal.sqlite.SQLDatabase;
import org.hammock.sync.internal.util.DatabaseUtils;
import org.hammock.sync.query.FieldSort;
import org.hammock.sync.query.Index;
import org.hammock.sync.query.IndexType;
import org.hammock.sync.query.Tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lists the indexes for the database.
 */
public class ListIndexesCallable implements SQLCallable<List<Index>> {

    @Override
    public List<Index> call(SQLDatabase db) throws Exception {
        // Accumulate indexes and definitions into a map
        String sqlIndexNames = String.format("SELECT DISTINCT index_name FROM %s", QueryConstants
                .INDEX_METADATA_TABLE_NAME);
        Cursor cursorIndexNames = null;
        ArrayList<Index> indexes = new ArrayList<Index>();
        try {
            cursorIndexNames = db.rawQuery(sqlIndexNames, new String[]{});
            while (cursorIndexNames.moveToNext()) {
                String indexName = cursorIndexNames.getString(0);
                String sqlIndexes = String.format("SELECT index_type, field_name, index_settings " +
                        "FROM %s WHERE index_name = ?", QueryConstants.INDEX_METADATA_TABLE_NAME);
                Cursor cursorIndexes = null;
                try {
                    cursorIndexes = db.rawQuery(sqlIndexes, new String[]{indexName});
                    IndexType indexType = null;
                    String settings = null;
                    ArrayList<FieldSort> fieldNames = new ArrayList<FieldSort>();
                    boolean first = true;
                    while (cursorIndexes.moveToNext()) {
                        if (first) {
                            // first time round
                            indexType = IndexType.valueOf(cursorIndexes.getString(0).toUpperCase
                                    (Locale.ENGLISH));
                            settings = cursorIndexes.getString(2);
                            first = false;
                        }
                        fieldNames.add(new FieldSort(cursorIndexes.getString(1)));
                    }
                    Tokenizer tokenizer = TokenizerHelper.jsonToTokenizer(settings);
                    if (tokenizer != null) {
                        indexes.add(new Index(fieldNames, indexName, indexType, tokenizer));
                    } else {
                        indexes.add(new Index(fieldNames, indexName, indexType));
                    }
                } finally {
                    DatabaseUtils.closeCursorQuietly(cursorIndexes);
                }
            }
        } finally {
            DatabaseUtils.closeCursorQuietly(cursorIndexNames);
        }

        return indexes;
    }

}
