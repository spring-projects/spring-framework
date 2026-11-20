/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.broker;

import org.springframework.context.ApplicationEvent;

/**
 * Event raised when a broker's availability changes.
 *
 * @author Andy Wilkinson
 */
public class BrokerAvailabilityEvent extends ApplicationEvent {

	private static final long serialVersionUID = -8156742505179181002L;

	private final boolean brokerAvailable;


	/**
	 * Creates a new {@code BrokerAvailabilityEvent}.
	 *
	 * @param brokerAvailable {@code true} if the broker is available, {@code
	 * false} otherwise
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
