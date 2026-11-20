/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
