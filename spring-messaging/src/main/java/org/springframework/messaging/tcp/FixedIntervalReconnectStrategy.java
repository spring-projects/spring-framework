/*
 * Copyright 2002-2018 the original author or authors.
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
