/*
 * Copyright 2002-2014 the original author or authors.
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
 * Indicate the rate at which an operation should be retried.
 *
 * <p>Users of this interface are expected to use it like this:
 *
 * <pre class="code">
 * {@code
 *
 *  long waitInterval = backOff.nextBackOffMillis();
 *  if (waitInterval == BackOff.STOP) {
 *    backOff.reset();
 *    // do not retry operation
 *  }
 *  else {
 *    // sleep, e.g. Thread.sleep(waitInterval)
 *    // retry operation
 *  }
 * }</pre>
 *
 * Once the underlying operation has completed successfully, the instance
 * <b>must</b> be {@link #reset()} before further use. Due to how back off
 * should be used, implementations do not need to be thread-safe.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public interface BackOff {

	/**
	 * Return value of {@link #nextBackOff()} that indicates that the operation
	 * should not be retried.
	 */
	long STOP = -1;

	/**
	 * Return the number of milliseconds to wait before retrying the operation
	 * or {@link #STOP} ({@value #STOP}) to indicate that no further attempt
	 * should be made for the operation.
	 */
	long nextBackOff();

	/**
	 * Reset this instance to its original state.
	 */
	void reset();

}
