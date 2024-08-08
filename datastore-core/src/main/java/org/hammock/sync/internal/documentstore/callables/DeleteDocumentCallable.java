/*
 * Copyright © 2016, 2018 IBM Corp. All rights reserved.
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

package org.hammock.sync.internal.documentstore.callables;

import org.hammock.sync.documentstore.Attachment;
import org.hammock.sync.documentstore.ConflictException;
import org.hammock.sync.documentstore.DocumentStoreException;
import org.hammock.sync.internal.common.CouchUtils;
import org.hammock.sync.documentstore.DocumentBodyFactory;
import org.hammock.sync.documentstore.DocumentNotFoundException;
import org.hammock.sync.internal.documentstore.InternalDocumentRevision;
import org.hammock.sync.internal.documentstore.DocumentRevisionBuilder;
import org.hammock.sync.internal.sqlite.Cursor;
import org.hammock.sync.internal.sqlite.SQLCallable;
import org.hammock.sync.internal.sqlite.SQLDatabase;
import org.hammock.sync.internal.util.DatabaseUtils;
import org.hammock.sync.internal.util.JSONUtils;
import org.hammock.sync.internal.util.Misc;

import java.sql.SQLException;
import java.util.Collections;

/**
 * Delete a Document for given Document ID and Revision ID.
 *
 * In the same manner as CouchDB, a new leaf Revision which is marked as deleted is created and made
 * the child Revision of the Revision to be deleted.
 */
public class DeleteDocumentCallable implements SQLCallable<InternalDocumentRevision> {

    String docId;
    String prevRevId;

    /**
     * @param docId     The Document ID of the Revision to be deleted
     * @param prevRevId The Revision ID of the Revision to be deleted
     */
    public DeleteDocumentCallable(String docId, String prevRevId) {
        this.docId = docId;
        this.prevRevId = prevRevId;
    }

    public InternalDocumentRevision call(SQLDatabase db) throws ConflictException,
            DocumentNotFoundException, DocumentStoreException {

        Misc.checkNotNullOrEmpty(docId, "Input document id");
        Misc.checkNotNullOrEmpty(prevRevId, "Input previous revision id");

        CouchUtils.validateRevisionId(prevRevId);

        // get the sequence, numeric document ID, current flag for the given revision - if it's a
        // non-deleted leaf
        Cursor c = null;
        long sequence;
        long docNumericId;
        boolean current;
        try {
            // first check if it exists
            c = db.rawQuery(CallableSQLConstants.GET_METADATA_GIVEN_REVISION, new String[]{docId,
                    prevRevId});
            boolean exists = c.moveToFirst();
            if (!exists) {
                throw new DocumentNotFoundException(docId);
            }
            // now check it's a leaf revision
            String leafQuery = "SELECT " + CallableSQLConstants.METADATA_COLS + " FROM revs, docs WHERE " +
                    "docs.docid=? AND revs.doc_id=docs.doc_id AND revid=? AND revs.sequence NOT " +
                    "IN (SELECT DISTINCT parent FROM revs revs_inner WHERE parent NOT NULL AND revs_inner.doc_id=docs.doc_id) ";
            c = db.rawQuery(leafQuery, new String[]{docId, prevRevId});
            boolean isLeaf = c.moveToFirst();
            if (!isLeaf) {
                throw new ConflictException("Document has newer revisions than the revision " +
                        "passed to delete; get the newest revision of the document and try again.");
            }
            boolean isDeleted = c.getInt(c.getColumnIndex("deleted")) != 0;
            if (isDeleted) {
                throw new DocumentNotFoundException("Previous Revision is already deleted");
            }
            sequence = c.getLong(c.getColumnIndex("sequence"));
            docNumericId = c.getLong(c.getColumnIndex("doc_id"));
            current = c.getInt(c.getColumnIndex("current")) != 0;
        } catch (SQLException sqe) {
            throw new DocumentStoreException(sqe);
        } finally {
            DatabaseUtils.closeCursorQuietly(c);
        }

        new SetCurrentCallable(sequence, false).call(db);
        String newRevisionId = CouchUtils.generateNextRevisionId(prevRevId);
        // Previous revision to be deleted could be winner revision ("current" == true),
        // or a non-winner leaf revision ("current" == false), the new inserted
        // revision must have the same flag as it previous revision.
        // Deletion of non-winner leaf revision is mainly used when resolving
        // conflicts.
        InsertRevisionCallable callable = new InsertRevisionCallable();

        callable.docNumericId = docNumericId;
        callable.revId = newRevisionId;
        callable.parentSequence = sequence;
        callable.deleted = true;
        callable.current = current;
        callable.data = JSONUtils.emptyJSONObjectAsBytes();
        callable.available = false;
        long newSequence = callable.call(db);

        // build up the document to return to the caller - it's quicker than re-querying the
        // database and we know all the values we need
        return new DocumentRevisionBuilder()
                .setInternalId(docNumericId)
                .setDocId(docId)
                .setRevId(newRevisionId)
                .setParent(sequence)
                .setDeleted(true)
                .setCurrent(current)
                .setBody(DocumentBodyFactory.create(JSONUtils.emptyJSONObjectAsBytes()))
                .setSequence(newSequence)
                .setAttachments(Collections.<String, Attachment>emptyMap())
                .build();
    }

}
