/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util.backoff;

/**
 * Strategy interface for providing a {@link BackOffExecution} that indicates the
 * rate at which an operation should be retried.
 *
 * <p>Users of this interface are expected to use it like this:
 *
 * <pre class="code">
 * BackOffExecution execution = backOff.start();
 *
 * // In the operation recovery/retry loop:
 * long waitInterval = execution.nextBackOff();
 * if (waitInterval == BackOffExecution.STOP) {
 *     // do not retry operation
 * }
 * else {
 *     // sleep, for example, Thread.sleep(waitInterval)
 *     // retry operation
 * }</pre>
 *
 * <p>Once the underlying operation has completed successfully, the execution
 * instance can be discarded.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see BackOffExecution
 * @see FixedBackOff
 * @see ExponentialBackOff
 */
@FunctionalInterface
public interface BackOff {

	/**
	 * Start a new back off execution.
	 * @return a fresh {@link BackOffExecution} ready to be used
	 */
	BackOffExecution start();

}
