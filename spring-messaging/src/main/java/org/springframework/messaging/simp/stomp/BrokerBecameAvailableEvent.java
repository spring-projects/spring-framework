/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.simp.stomp;

/**
 * Event raised when the broker being used by a {@link
 * StompBrokerRelayMessageHandler} becomes available
 *
 * @author Andy Wilkinson
 */
public class BrokerBecameAvailableEvent extends BrokerAvailabilityEvent {

	/**
	 * Creates a new BrokerBecameAvailableEvent
	 *
	 * @param source the {@code StompBrokerRelayMessageHandler} that is acting
	 * as a relay for the broker that has become available. Must not be {@code
	 * null}.
	 */
	public BrokerBecameAvailableEvent(StompBrokerRelayMessageHandler source) {
		super(source);
	}

}
