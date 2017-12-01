/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http;

/**
 * constants for {@link com.codahale.metrics.MetricRegistry}
 *
 * @author Xiaoshuang LU
 * @since 4.3.18
 */
public class MetricConstants {
    public static final String REQUESTS_PER_SECOND = "requests-per-second";

    public static final String SUCCEEDED_REQUESTS_PER_SECOND = "succeeded-requests-per-second";

    public static final String FAILED_REQUESTS_PER_SECOND = "failed-requests-per-second";

    // in milliseconds
    public static final String RESPONSE_TIME = "response-time";

    public static final String SLOW_REQUESTS_PER_SECOND = "slow-requests-per-second";
}
