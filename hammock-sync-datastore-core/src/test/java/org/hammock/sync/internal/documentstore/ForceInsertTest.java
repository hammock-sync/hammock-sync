/*
 * Copyright © 2017 IBM Corp. All rights reserved.
 *
 * Copyright © 2014 Cloudant, Inc. All rights reserved.
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

package org.hammock.sync.internal.documentstore;

import org.hammock.sync.documentstore.Attachment;
import org.hammock.sync.documentstore.DocumentException;
import org.hammock.sync.documentstore.DocumentRevision;
import org.hammock.sync.event.Subscribe;
import org.hammock.sync.event.notifications.DocumentCreated;
import org.hammock.sync.event.notifications.DocumentUpdated;

import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Tests some notification and attachment behaviour using the internal forceInsert methods.
 */
public class ForceInsertTest extends BasicDatastoreTestBase {

    static CountDownLatch documentCreated, documentUpdated;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        datastore.getEventBus().register(this);
    }

    @Test
    public void notification_forceinsert() throws Exception {
        documentUpdated = new CountDownLatch(1);
        documentCreated = new CountDownLatch(1); // 2 because the call to createDocument will
        // also fire
        // create a document and insert the first revision
        DocumentRevision doc1_rev1 = new DocumentRevision();
        doc1_rev1.setBody(bodyOne);
        doc1_rev1 = datastore.create(doc1_rev1);

        ArrayList<String> revisionHistory = new ArrayList<String>();
        revisionHistory.add(doc1_rev1.getRevision());
        revisionHistory.add("2-revision");

        InternalDocumentRevision rev2 = new DocumentRevisionBuilder().setDocId(doc1_rev1.getId())
                .setRevId("2-revision").setBody(bodyOne).build();

        // now do a force insert - we should get an updated event as it's already there
        datastore.forceInsert(rev2, doc1_rev1.getRevision(), "2-revision");
        boolean ok1 = NotificationTestUtils.waitForSignal(documentUpdated);
        Assert.assertTrue("Didn't receive document updated event", ok1);

        // now do a force insert with same rev but with a different ID - we should get a (2nd)
        // created event
        InternalDocumentRevision doc2_rev1 = new DocumentRevisionBuilder().setDocId
                ("new-ID-12345").setRevId(rev2.getRevision()).setBody(bodyOne).build();
        datastore.forceInsert(doc2_rev1, doc1_rev1.getRevision(), "2-revision");
        boolean ok2 = NotificationTestUtils.waitForSignal(documentCreated);
        Assert.assertTrue("Didn't receive document created event", ok2);
    }

    @Test
    public void notification_forceinsertWithAttachments() throws Exception {

        // this test only makes sense if the data is inline base64 (there's no remote server to
        // pull the attachment from)
        boolean pullAttachmentsInline = true;

        // create a document and insert the 1-revision
        DocumentRevision doc1_rev1Mut = new DocumentRevision();
        doc1_rev1Mut.setBody(bodyOne);
        DocumentRevision doc1_rev1 = datastore.create(doc1_rev1Mut);
        Map<String, Object> atts = new HashMap<String, Object>();
        Map<String, Object> att1 = new HashMap<String, Object>();

        // set up attachment dictionary in the form expected by forceInsert
        // (this is the form returned by CouchDB from the _attachments dictionary)
        atts.put("att1", att1);
        att1.put("data", new String(new Base64().encode("this is some data".getBytes())));
        att1.put("content_type", "text/plain");

        ArrayList<String> revisionHistory = new ArrayList<String>();
        revisionHistory.add(doc1_rev1.getRevision());
        revisionHistory.add("2-revision");

        // now create a document and force insert a 2-revision with attachments
        InternalDocumentRevision rev2 = new DocumentRevisionBuilder().setDocId(doc1_rev1.getId())
                .setRevId("2-revision").setBody(bodyOne).build();

        ForceInsertItem fii = new ForceInsertItem(rev2, revisionHistory, atts, null,
                pullAttachmentsInline);
        datastore.forceInsert(Collections.singletonList(fii));

        // check that we can retrieve attachments from 2-rev after force insert
        Attachment storedAtt = datastore.getAttachment(rev2.getId(), rev2.getRevision(), "att1");
        Assert.assertNotNull(storedAtt);

        // check that retrieving a different attachment returns null
        Attachment noSuchAtt = datastore.getAttachment(rev2.getId(), rev2.getRevision(), "att2");
        Assert.assertNull(noSuchAtt);
    }

    @Test
    public void notification_forceinsertWithAttachmentsError() throws Exception {
        Assume.assumeFalse(System.getProperty("os.name").toLowerCase().contains("windows"));
        // this test only makes sense if the data is inline base64 (there's no remote server to
        // pull the attachment from)
        boolean pullAttachmentsInline = true;

        // try and force an IOException when setting the attachment, and check everything is OK:

        // create a read only zero-length file where the extensions dir would go, to cause an IO
        // exception
        File extensions = new File(datastore.datastoreDir + "/extensions");
        extensions.createNewFile();
        extensions.setWritable(false);

        DocumentRevision doc1_rev1Mut = new DocumentRevision();
        doc1_rev1Mut.setBody(bodyOne);
        DocumentRevision doc1_rev1 = datastore.create(doc1_rev1Mut);
        Map<String, Object> atts = new HashMap<String, Object>();
        Map<String, Object> att1 = new HashMap<String, Object>();

        atts.put("att1", att1);
        att1.put("data", new String(new Base64().encode("this is some data".getBytes(StandardCharsets.UTF_8))));
        att1.put("content_type", "text/plain");

        ArrayList<String> revisionHistory = new ArrayList<String>();
        revisionHistory.add(doc1_rev1.getRevision());
        InternalDocumentRevision rev2 = new DocumentRevisionBuilder().setDocId(doc1_rev1.getId())
                .setRevId("2-blah").setBody(bodyOne).build();
        revisionHistory.add("2-blah");
        // now do a force insert
        //catch the exception thrown se we can look into the database
        try {
            ForceInsertItem fii = new ForceInsertItem(rev2, revisionHistory, atts, null,
                    pullAttachmentsInline);
            datastore.forceInsert(Collections.singletonList(fii));
        } catch (DocumentException e) {
            //do nothing.
        }

        // Check that the attachment is not associated with the original rev
        Attachment storedAtt = datastore.getAttachment(doc1_rev1.getId(), doc1_rev1.getRevision()
                , "att1");
        Assert.assertNull(storedAtt);

        // adding the attachment should have failed transactionally, so the rev should not exist
        // as well
        Assert.assertFalse(datastore.contains(rev2.getId(), rev2.getRevision()));

    }

    // some tests don't care about these events so we need to check for null
    @Subscribe
    public void onDocumentCreated(DocumentCreated dc) {
        if (documentCreated != null)
            documentCreated.countDown();
    }

    @Subscribe
    public void onDocumentUpdated(DocumentUpdated du) {
        if (documentUpdated != null)
            documentUpdated.countDown();
    }

}

