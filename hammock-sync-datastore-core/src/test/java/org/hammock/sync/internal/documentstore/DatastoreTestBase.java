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

package org.hammock.sync.internal.documentstore;

import org.hammock.common.DocumentStoreTestBase;

import org.junit.After;
import org.junit.Before;

/**
 * Test base for any test suite need a <code>DatastoreManager</code> and <code>Datastore</code> instance. It
 * automatically set up and clean up the temp file directly for you.
 */
public abstract class DatastoreTestBase extends DocumentStoreTestBase {

    DatabaseImpl datastore = null;

    @Before
    public void setUpDatabaseImpl() throws Exception {
        this.datastore = (DatabaseImpl) documentStore.database();

    }

    @After
    public void tearDownDatabaseImpl() {
        datastore.close();
    }
}
