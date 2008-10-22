/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.util;

/**
 * Interface implemented by objects that can provide performance information
 * as well as a record of the number of times they are accessed.
 *
 * <p>Implementing objects must ensure that implementing this interface
 * does <b>not</b> compromise thread safety. However, it may be acceptable
 * for slight innaccuracies in reported statistics to result from the
 * avoidance of synchronization: performance may be well be more important
 * than exact reporting, so long as the errors are not likely to be misleading.
 *
 * @author Rod Johnson
 * @since November 21, 2000
 * @deprecated as of Spring 2.5, to be removed in Spring 3.0
 */
public interface ResponseTimeMonitor {

	/**
	 * Return the number of accesses to this resource.
	 */
	int getAccessCount();

	/**
	 * Return the average response time in milliseconds.
	 */
	int getAverageResponseTimeMillis();

	/**
	 * Return the best (quickest) response time in milliseconds.
	 */
	int getBestResponseTimeMillis();

	/**
	 * Return the worst (slowest) response time in milliseconds.
	 */
	int getWorstResponseTimeMillis();

}
