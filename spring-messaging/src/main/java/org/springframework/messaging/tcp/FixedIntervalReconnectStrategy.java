/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.tcp;

/**
 * A simple strategy for making reconnect attempts at a fixed interval.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class FixedIntervalReconnectStrategy implements ReconnectStrategy {

	private final long interval;


	/**
	 * Create a new {@link FixedIntervalReconnectStrategy} instance.
	 * @param interval the frequency, in millisecond, at which to try to reconnect
	 */
	public FixedIntervalReconnectStrategy(long interval) {
		this.interval = interval;
	}


	@Override
	public Long getTimeToNextAttempt(int attemptCount) {
		return this.interval;
	}

}
