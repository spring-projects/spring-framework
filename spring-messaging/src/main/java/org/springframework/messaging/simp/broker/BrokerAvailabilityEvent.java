/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.messaging.simp.broker;

import org.springframework.context.ApplicationEvent;

/**
 * Event raised when a broker's availabilty changes
 *
 * @author Andy Wilkinson
 */
public class BrokerAvailabilityEvent extends ApplicationEvent {

	private static final long serialVersionUID = -8156742505179181002L;

	private final boolean brokerAvailable;


	/**
	 * Creates a new {@code BrokerAvailabilityEvent}.
	 *
	 * @param brokerAvailable {@code true} if the broker is available, {@code}
	 * false otherwise
	 * @param source the component that is acting as the broker, or as a relay
	 * for an external broker, that has changed availability. Must not be {@code
	 * null}.
	 */
	public BrokerAvailabilityEvent(boolean brokerAvailable, Object source) {
		super(source);
		this.brokerAvailable = brokerAvailable;
	}

	public boolean isBrokerAvailable() {
		return this.brokerAvailable;
	}

	@Override
	public String toString() {
		return "BrokerAvailabilityEvent[available=" + this.brokerAvailable + ", " + getSource() + "]";
	}

}
