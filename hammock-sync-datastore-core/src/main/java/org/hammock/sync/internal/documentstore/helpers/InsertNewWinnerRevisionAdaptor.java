/*
 * Copyright © 2017 IBM Corp. All rights reserved.
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

package org.hammock.sync.internal.documentstore.helpers;

import org.hammock.sync.documentstore.AttachmentException;
import org.hammock.sync.documentstore.DocumentBody;
import org.hammock.sync.documentstore.DocumentStoreException;
import org.hammock.sync.internal.common.CouchUtils;
import org.hammock.sync.internal.documentstore.InternalDocumentRevision;
import org.hammock.sync.internal.documentstore.callables.InsertRevisionCallable;

/**
 * Created by tomblench on 23/08/2017.
 */

public class InsertNewWinnerRevisionAdaptor {

    public static InsertRevisionCallable insert(DocumentBody newWinner,
                                                InternalDocumentRevision oldWinner)
            throws AttachmentException, DocumentStoreException {

        String newRevisionId = CouchUtils.generateNextRevisionId(oldWinner.getRevision());

        InsertRevisionCallable callable = new InsertRevisionCallable();

        callable.docNumericId = oldWinner.getInternalNumericId();
        callable.revId = newRevisionId;
        callable.parentSequence = oldWinner.getSequence();
        callable.deleted = false;
        callable.current = true;
        callable.data = newWinner.asBytes();
        callable.available = true;

        return callable;
    }

}
