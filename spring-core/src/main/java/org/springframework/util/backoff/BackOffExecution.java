/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.backoff;

/**
 * <p>A {@code BackOffExecution} is effectively an executable instance of a given
 * {@link BackOff} strategy.
 *
 * <p>Implementations may be stateful but do not need to be thread-safe.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see BackOff
 */
@FunctionalInterface
public interface BackOffExecution {

	/**
	 * Return value of {@link #nextBackOff()} which indicates that the operation
	 * should not be retried.
	 */
	long STOP = -1;

	/**
	 * Return the number of milliseconds to wait before retrying the operation
	 * or {@link #STOP} ({@value #STOP}) to indicate that no further attempt
	 * should be made for the operation.
	 */
	long nextBackOff();

}
