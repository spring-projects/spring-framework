/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.jms.listener.endpoint;

import javax.jms.Destination;

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
