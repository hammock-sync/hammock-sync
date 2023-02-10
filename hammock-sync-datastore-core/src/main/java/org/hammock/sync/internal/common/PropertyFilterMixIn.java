/*
 * Copyright © 2017 IBM Corp. All rights reserved.
 *
 * Copyright © 2015 Cloudant, Inc. All rights reserved.
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

/*
 * Code adapted from:
 *
 * http://stackoverflow.com/questions/8235255/global-property-filter-in-jackson
 */

package org.hammock.sync.internal.common;

import com.fasterxml.jackson.annotation.JsonFilter;

@JsonFilter(PropertyFilterMixIn.SIMPLE_FILTER_NAME)
public class PropertyFilterMixIn {

    public static final String SIMPLE_FILTER_NAME = "couchKeywordsFilter";

}
