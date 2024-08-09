/*
 * Copyright © 2014, 2016 IBM Corp. All rights reserved.
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

package org.hammock.sync.internal.replication;

import org.hammock.common.RequireRunningCouchDB;
import org.hammock.sync.documentstore.Attachment;
import org.hammock.sync.documentstore.DocumentBodyFactory;
import org.hammock.sync.documentstore.DocumentRevision;
import org.hammock.sync.documentstore.UnsavedFileAttachment;
import org.hammock.sync.internal.mazha.CouchClient;
import org.hammock.sync.replication.PushAttachmentsInline;
import org.hammock.sync.replication.ReplicatorBuilder;
import org.hammock.sync.util.TestUtils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tomblench on 26/03/2014.
 */

@Category(RequireRunningCouchDB.class)
@RunWith(Parameterized.class)
public class AttachmentsPushTest extends ReplicationTestBase {

    String id1;
    String id2;
    String id3;

    // override builder so we can set our push preference
    @Override
    protected ReplicatorBuilder.Push getPushBuilder() {
        return ReplicatorBuilder.push().
                from(this.documentStore).
                to(this.couchConfig.getRootUri()).
                pushAttachmentsInline(pushAttachmentsInline).
                addRequestInterceptors(couchConfig.getRequestInterceptors()).
                addResponseInterceptors(couchConfig.getResponseInterceptors());
    }


    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {PushAttachmentsInline.False}, {PushAttachmentsInline.Small}, {PushAttachmentsInline.True}
        });
    }

    @Parameterized.Parameter
    public PushAttachmentsInline pushAttachmentsInline;

    /**
     * After all documents are created:
     *
     * Doc 1: 1 -> 2 -> 3
     * Doc 2: 1 -> 2
     * Doc 3: 1 -> 2 -> 3
     */
    private void populateSomeDataInLocalDatastore() throws Exception {

        id1 = createDocInDatastore("Doc 1");
        Assert.assertNotNull(id1);
        updateDocInDatastore(id1, "Doc 1");
        updateDocInDatastore(id1, "Doc 1");

        id2 = createDocInDatastore("Doc 2");
        Assert.assertNotNull(id2);
        updateDocInDatastore(id2, "Doc 2");

        id3 = createDocInDatastore("Doc 3");
        Assert.assertNotNull(id3);
        updateDocInDatastore(id3, "Doc 3");
        updateDocInDatastore(id3, "Doc 3");
    }

    public String createDocInDatastore(String d) throws Exception {
        DocumentRevision rev = new DocumentRevision();
        Map<String, String> m = new HashMap<String, String>();
        m.put("data", d);
        rev.setBody(DocumentBodyFactory.create(m));
        return datastore.create(rev).getId();
    }


    public String updateDocInDatastore(String id, String data) throws Exception {
        DocumentRevision rev = datastore.read(id);
        Map<String, String> m = new HashMap<String, String>();
        m.put("data", data);
        rev.setBody(DocumentBodyFactory.create(m));
        return datastore.update(rev).getId();
    }

    @Test
    public void pushAttachmentsTest() throws Exception {
        // simple 1-rev attachment
        String attachmentName = "attachment_1.txt";
        populateSomeDataInLocalDatastore();
        File f = TestUtils.loadFixture("fixture/"+attachmentName);
        Attachment att = new UnsavedFileAttachment(f, "text/plain");
        DocumentRevision oldRevision = datastore.read(id1);
        DocumentRevision newRevision = null;
        // set attachment
        DocumentRevision oldRevision_mut = oldRevision;
        oldRevision_mut.getAttachments().put(attachmentName, att);
        newRevision = datastore.update(oldRevision_mut);


        // push replication
        push();

        // check it's in the DB
        assertAttachmentEquality(id1, newRevision.getRevision(), attachmentName, f);
    }

    @Test
    public void pushBigAttachmentsTest() throws Exception {
        // simple 1-rev attachment
        String attachmentName = "bonsai-boston.jpg";
        populateSomeDataInLocalDatastore();
        File f = TestUtils.loadFixture("fixture/"+ attachmentName);
        Attachment att = new UnsavedFileAttachment(f, "image/jpeg");
        DocumentRevision oldRevision = datastore.read(id1);
        DocumentRevision newRevision = null;
        // set attachment
        DocumentRevision oldRevision_mut = oldRevision;
        oldRevision_mut.getAttachments().put(attachmentName, att);
        newRevision = datastore.update(oldRevision_mut);


        // push replication
        push();

        // check it's in the DB
        assertAttachmentEquality(id1, newRevision.getRevision(), attachmentName, f);
    }


    // check that small and large attachments both get sent as
    // multipart or base64 inline but not a mixture
    // (verification of multipart transport to be done manually eg via
    // wireshark)
    @Test
    public void pushBigAndSmallAttachmentsTest() throws Exception {
        // simple 1-rev attachment
        String attachmentName1 = "bonsai-boston.jpg";
        String attachmentName2 = "attachment_1.txt";
        populateSomeDataInLocalDatastore();
        File f1 = TestUtils.loadFixture("fixture/"+ attachmentName1);
        File f2 = TestUtils.loadFixture("fixture/"+ attachmentName2);
        Attachment att1 = new UnsavedFileAttachment(f1, "image/jpeg");
        Attachment att2 = new UnsavedFileAttachment(f2, "text/plain");
        DocumentRevision oldRevision = datastore.read(id1);
        DocumentRevision newRevision = null;
        // set attachment
        oldRevision.getAttachments().put(attachmentName1, att1);
        oldRevision.getAttachments().put(attachmentName2, att2);
        newRevision = datastore.update(oldRevision);

        // push replication
        push();

        // check it's in the DB
        assertAttachmentEquality(id1, newRevision.getRevision(), attachmentName1, f1);
        assertAttachmentEquality(id1, newRevision.getRevision(), attachmentName2, f2);
    }

    @Test
    public void pushAttachmentsTest2() throws Exception {
        // more complex test with attachments changing between revisions
        String attachmentName1 = "attachment_1.txt";
        String attachmentName2 = "attachment_2.txt";
        populateSomeDataInLocalDatastore();
        File f1 = TestUtils.loadFixture("fixture/"+ attachmentName1);
        File f2 = TestUtils.loadFixture("fixture/"+ attachmentName2);
        Attachment att1 = new UnsavedFileAttachment(f1, "text/plain");
        Attachment att2 = new UnsavedFileAttachment(f2, "text/plain");
        DocumentRevision rev1 = datastore.read(id1);
        DocumentRevision rev2 = null;
        // set attachment
        DocumentRevision rev1_mut = rev1;
        rev1_mut.getAttachments().put(attachmentName1, att1);
        rev2 = datastore.update(rev1_mut);

        // push replication - att1 should be uploaded
        push();

        DocumentRevision rev2_mut = rev2;
        DocumentRevision rev3 = datastore.update(rev2_mut);

        // push replication - no atts should be uploaded
        push();

        DocumentRevision rev4 = null;
        // set attachment
        DocumentRevision rev3_mut = rev3;
        rev3_mut.getAttachments().put(attachmentName2, att2);
        rev4 = datastore.update(rev3_mut);

        // push replication - att2 should be uploaded
        push();

        assertAttachmentEquality(id1, rev2.getRevision(), attachmentName1, f1);
        assertAttachmentEquality(id1, rev3.getRevision(), attachmentName1, f1);
        assertAttachmentEquality(id1, rev4.getRevision(), attachmentName1, f1);
        assertAttachmentEquality(id1, rev4.getRevision(), attachmentName2, f2);
    }

    // regression test for FB 46326 - see
    // https://groups.google.com/forum/#!topic/cloudant-sync/xAgWtuSsrk8 for details
    @Test
    public void pushAttachmentsStubsCorrectlySent() throws Exception {
        // more complex test with attachments changing between revisions
        String attachmentName1 = "attachment_1.txt";
        String attachmentName2 = "attachment_2.txt";
        populateSomeDataInLocalDatastore();
        File f1 = TestUtils.loadFixture("fixture/"+ attachmentName1);
        File f2 = TestUtils.loadFixture("fixture/"+ attachmentName2);
        Attachment att1 = new UnsavedFileAttachment(f1, "text/plain");
        Attachment att2 = new UnsavedFileAttachment(f2, "text/plain");
        DocumentRevision rev1 = datastore.read(id1);
        DocumentRevision rev2 = null;
        // set attachment
        DocumentRevision rev1_mut = rev1;
        rev1_mut.getAttachments().put(attachmentName1, att1);
        rev2 = datastore.update(rev1_mut);

        DocumentRevision rev2_mut = rev2;
        DocumentRevision rev3 = datastore.update(rev2_mut);

        DocumentRevision rev4 = null;
        // set attachment
        DocumentRevision rev3_mut = rev3;
        rev3_mut.getAttachments().put(attachmentName2, att2);
        rev4 = datastore.update(rev3_mut);

        // push replication - att1 & att2 should be uploaded
        push();

        assertAttachmentEquality(id1, rev4.getRevision(), attachmentName1, f1);
        assertAttachmentEquality(id1, rev4.getRevision(), attachmentName2, f2);
    }

    void assertAttachmentEquality(String id, String rev, String attachmentName, File f) throws Exception {
        this.couchClient.processAttachmentStream(id, rev, attachmentName, false, new FileComparingInputStreamProcessor(f));
    }

    private static final class FileComparingInputStreamProcessor implements CouchClient.InputStreamProcessor<Void> {

        private final FileInputStream fileStream;

        FileComparingInputStreamProcessor(File f) throws FileNotFoundException {
            this.fileStream = new FileInputStream(f);
        }

        @Override
        public Void processStream(InputStream stream) throws Exception {
            Assert.assertTrue("Attachment not the same", TestUtils.streamsEqual(stream, fileStream));
            return null;
        }
    }

}
