/*
 * Copyright © 2015, 2019 IBM Corp. All rights reserved.
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

package org.hammock.sync.http.internal.interceptors;

public class HttpConnectionInterceptorException extends RuntimeException {

    public boolean deserialize = false;
    public final String error;
    public final String reason;
    public int statusCode;

    public HttpConnectionInterceptorException(Throwable cause) {
        this.initCause(cause);
        this.error = null;
        this.reason = null;
    }

    HttpConnectionInterceptorException(String msg) {
        super(msg);
        this.error = null;
        this.reason = null;
    }

    HttpConnectionInterceptorException(String error, String reason) {
        super(error + ((reason != null) ? ": " + reason : ""));
        this.error = error;
        this.reason = reason;
    }
}
