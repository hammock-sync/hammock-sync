/*
 * Copyright © 2015 IBM Corp. All rights reserved.
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

package org.hammock.sync.internal.mazha;

import static org.hammock.common.TestOptions.COUCH_HOST;
import static org.hammock.common.TestOptions.COUCH_PASSWORD;
import static org.hammock.common.TestOptions.COUCH_PORT;
import static org.hammock.common.TestOptions.COUCH_URI;
import static org.hammock.common.TestOptions.COUCH_USERNAME;
import static org.hammock.common.TestOptions.HTTP_PROTOCOL;

import org.hammock.http.HttpConnectionRequestInterceptor;
import org.hammock.http.HttpConnectionResponseInterceptor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * Created by Rhys Short on 30/01/15.
 */
public class SpecifiedCouch {

    private SpecifiedCouch() {
        //empty
     }

    public static CouchConfig defaultConfig(String dbName){
        try {
            String uriString;

            // if full URI specified, then we don't need to build it up from components
            if (COUCH_URI != null) {
                uriString = String.format("%s/%s", COUCH_URI, dbName);
            }
            // otherwise build the URI up
            else {
                uriString = String.format("%s://%s:%s/%s", HTTP_PROTOCOL, COUCH_HOST, COUCH_PORT, dbName);
            }
            CouchConfig config = new CouchConfig(new URI(uriString),
                    new ArrayList<HttpConnectionRequestInterceptor>(),
                    new ArrayList<HttpConnectionResponseInterceptor>(),
                    COUCH_USERNAME,
                    COUCH_PASSWORD);
            return config;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

    }
}
