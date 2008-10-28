/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.jms.listener.endpoint;

import javax.jms.Session;

/**
 * Common configuration object for activating a JMS message endpoint.
 * Gets converted into a provider-specific JCA 1.5 ActivationSpec
 * object for activating the endpoint.
 *
 * <p>Typically used in combination with {@link JmsMessageEndpointManager},
 * but not tied to it.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see JmsActivationSpecFactory
 * @see JmsMessageEndpointManager#setActivationSpecConfig
 * @see javax.resource.spi.ResourceAdapter#endpointActivation
 */
public class JmsActivationSpecConfig {

	private String destinationName;

	private boolean pubSubDomain = false;

	private boolean subscriptionDurable = false;

	private String durableSubscriptionName;

	private String clientId;

	private String messageSelector;

	private int acknowledgeMode = Session.AUTO_ACKNOWLEDGE;

	private int maxConcurrency = -1;

	private int prefetchSize = -1;


	public void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
	}

	public String getDestinationName() {
		return this.destinationName;
	}

	public void setPubSubDomain(boolean pubSubDomain) {
		this.pubSubDomain = pubSubDomain;
	}

	public boolean isPubSubDomain() {
		return this.pubSubDomain;
	}

	public void setSubscriptionDurable(boolean subscriptionDurable) {
		this.subscriptionDurable = subscriptionDurable;
	}

	public boolean isSubscriptionDurable() {
		return this.subscriptionDurable;
	}

	public void setDurableSubscriptionName(String durableSubscriptionName) {
		this.durableSubscriptionName = durableSubscriptionName;
	}

	public String getDurableSubscriptionName() {
		return this.durableSubscriptionName;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientId() {
		return this.clientId;
	}

	public void setMessageSelector(String messageSelector) {
		this.messageSelector = messageSelector;
	}

	public String getMessageSelector() {
		return this.messageSelector;
	}

	public void setAcknowledgeMode(int acknowledgeMode) {
		this.acknowledgeMode = acknowledgeMode;
	}

	public int getAcknowledgeMode() {
		return this.acknowledgeMode;
	}

	public void setMaxConcurrency(int maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	public int getMaxConcurrency() {
		return this.maxConcurrency;
	}

	public void setPrefetchSize(int prefetchSize) {
		this.prefetchSize = prefetchSize;
	}

	public int getPrefetchSize() {
		return this.prefetchSize;
	}

}
