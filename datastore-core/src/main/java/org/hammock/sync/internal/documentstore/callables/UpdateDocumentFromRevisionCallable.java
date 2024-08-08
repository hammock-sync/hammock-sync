/*
 * Copyright © 2016 IBM Corp. All rights reserved.
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

import org.hammock.sync.documentstore.DocumentRevision;
import org.hammock.sync.internal.documentstore.AttachmentManager;
import org.hammock.sync.internal.documentstore.AttachmentStreamFactory;
import org.hammock.sync.internal.documentstore.InternalDocumentRevision;
import org.hammock.sync.internal.documentstore.PreparedAttachment;
import org.hammock.sync.internal.documentstore.SavedAttachment;
import org.hammock.sync.internal.sqlite.SQLCallable;
import org.hammock.sync.internal.sqlite.SQLDatabase;
import org.hammock.sync.internal.util.Misc;

import java.util.Map;

/**
 * Update body and attachments of Document Revision by inserting a new child Revision with the JSON contents
 * {@code rev.body} and the new and existing attachments {@code preparedNewAttachments} and {@code existingAttachments}
 */
public class UpdateDocumentFromRevisionCallable implements SQLCallable<InternalDocumentRevision> {

    private DocumentRevision rev;
    private Map<String, PreparedAttachment> preparedNewAttachments;
    private Map<String, SavedAttachment> existingAttachments;

    private String attachmentsDir;
    private AttachmentStreamFactory attachmentStreamFactory;

    public UpdateDocumentFromRevisionCallable(DocumentRevision rev, Map<String, PreparedAttachment>
            preparedNewAttachments, Map<String, SavedAttachment> existingAttachments, String
            attachmentsDir, AttachmentStreamFactory attachmentStreamFactory) {
        this.rev = rev;
        this.preparedNewAttachments = preparedNewAttachments;
        this.existingAttachments = existingAttachments;
        this.attachmentsDir = attachmentsDir;
        this.attachmentStreamFactory = attachmentStreamFactory;
    }

    @Override
    public InternalDocumentRevision call(SQLDatabase db) throws Exception {
        Misc.checkNotNull(rev, "DocumentRevision");

        InternalDocumentRevision updated = new UpdateDocumentBodyCallable(rev.getId(), rev.getRevision(), rev
                .getBody(), attachmentsDir, attachmentStreamFactory).call(db);
        AttachmentManager.addAttachmentsToRevision(db, attachmentsDir, updated,
                preparedNewAttachments);

        AttachmentManager.copyAttachmentsToRevision(db, existingAttachments, updated);

        // now re-fetch the revision with updated attachments
        InternalDocumentRevision updatedWithAttachments = new GetDocumentCallable(updated.getId(),
                updated.getRevision(), this.attachmentsDir, this.attachmentStreamFactory).call(db);
        return updatedWithAttachments;
    }

}
