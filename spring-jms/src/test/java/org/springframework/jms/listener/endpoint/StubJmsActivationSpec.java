/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.listener.endpoint;

import jakarta.jms.Destination;

import org.springframework.jca.StubActivationSpec;

/**
 * StubActivationSpec which implements all required and optional properties (see
 * specification Appendix B.2) except the destination attribute. Because this
 * can be a string but also an {@link Destination} object, which is configured
 * as an administrated object.
 *
 * @author Agim Emruli
 */
public class StubJmsActivationSpec extends StubActivationSpec {

	private String destinationType;

	private String subscriptionDurability;

	private String subscriptionName;

	private String clientId;

	private String messageSelector;

	private String acknowledgeMode;


	public String getDestinationType() {
		return destinationType;
	}

	public void setDestinationType(String destinationType) {
		this.destinationType = destinationType;
	}

	public String getSubscriptionDurability() {
		return subscriptionDurability;
	}

	public void setSubscriptionDurability(String subscriptionDurability) {
		this.subscriptionDurability = subscriptionDurability;
	}

	public String getSubscriptionName() {
		return subscriptionName;
	}

	public void setSubscriptionName(String subscriptionName) {
		this.subscriptionName = subscriptionName;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getMessageSelector() {
		return messageSelector;
	}

	public void setMessageSelector(String messageSelector) {
		this.messageSelector = messageSelector;
	}

	public String getAcknowledgeMode() {
		return acknowledgeMode;
	}

	public void setAcknowledgeMode(String acknowledgeMode) {
		this.acknowledgeMode = acknowledgeMode;
	}

}
