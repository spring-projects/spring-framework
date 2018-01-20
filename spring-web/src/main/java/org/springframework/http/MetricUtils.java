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

import com.codahale.metrics.MetricRegistry;

/**
 * utilities for {@link com.codahale.metrics.MetricRegistry}
 *
 * @author Xiaoshuang LU
 * @since 4.3.18
 */
public class MetricUtils {
    /**
     * increase counter
     * @param metricRegistry
     * @param itemName
     * @param value
     */
    public static void inc(MetricRegistry metricRegistry, String itemName, long value) {
        if (metricRegistry != null) {
            try {
                metricRegistry.counter(itemName).inc(value);
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    /**
     * decrease counter
     * @param metricRegistry
     * @param itemName
     * @param value
     */
    public static void dec(MetricRegistry metricRegistry, String itemName, long value) {
        if (metricRegistry != null) {
            try {
                metricRegistry.counter(itemName).dec(value);
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    /**
     * mark meter
     * @param metricRegistry
     * @param itemName
     * @param value
     */
    public static void mark(MetricRegistry metricRegistry, String itemName, long value) {
        if (metricRegistry != null) {
            try {
                metricRegistry.meter(itemName).mark(value);
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    /**
     * update histogram
     * @param metricRegistry
     * @param itemName
     * @param value
     */
    public static void update(MetricRegistry metricRegistry, String itemName, long value) {
        if (metricRegistry != null) {
            try {
                metricRegistry.histogram(itemName).update(value);
            } catch (Exception e) {
                // do nothing
            }
        }
    }
}
