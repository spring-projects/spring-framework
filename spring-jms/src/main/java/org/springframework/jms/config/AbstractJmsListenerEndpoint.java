/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.jms.config;

import javax.jms.MessageListener;

import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.jms.listener.endpoint.JmsActivationSpecConfig;
import org.springframework.jms.listener.endpoint.JmsMessageEndpointManager;
import org.springframework.util.Assert;

/**
 * Base model for a JMS listener endpoint
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see MethodJmsListenerEndpoint
 * @see SimpleJmsListenerEndpoint
 */
public abstract class AbstractJmsListenerEndpoint implements JmsListenerEndpoint {

	private String id;

	private String destination;

	private String subscription;

	private String selector;


	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Return the name of the destination for this endpoint.
	 */
	public String getDestination() {
		return destination;
	}

	/**
	 * Set the name of the destination for this endpoint.
	 */
	public void setDestination(String destination) {
		this.destination = destination;
	}

	/**
	 * Return the name for the durable subscription, if any.
	 */
	public String getSubscription() {
		return subscription;
	}

	/**
	 * Set the name for the durable subscription.
	 */
	public void setSubscription(String subscription) {
		this.subscription = subscription;
	}

	/**
	 * Return the JMS message selector expression, if any.
	 * <p>See the JMS specification for a detailed definition of selector expressions.
	 */
	public String getSelector() {
		return selector;
	}

	/**
	 * Set the JMS message selector expression.
	 */
	public void setSelector(String selector) {
		this.selector = selector;
	}

	@Override
	public void setupMessageContainer(MessageListenerContainer container) {
		if (container instanceof AbstractMessageListenerContainer) { // JMS
			setupJmsMessageContainer((AbstractMessageListenerContainer) container);
		}
		else if (container instanceof JmsMessageEndpointManager) { // JCA
			setupJcaMessageContainer((JmsMessageEndpointManager) container);
		}
		else {
			throw new IllegalArgumentException("Could not configure endpoint with the specified container '"
					+ container + "' Only JMS (" + AbstractMessageListenerContainer.class.getName()
					+ " subclass) or JCA (" + JmsMessageEndpointManager.class.getName() + ") are supported.");
		}
	}

	protected void setupJmsMessageContainer(AbstractMessageListenerContainer container) {
		if (getDestination() != null) {
			container.setDestinationName(getDestination());
		}
		if (getSubscription() != null) {
			container.setDurableSubscriptionName(getSubscription());
		}
		if (getSelector() != null) {
			container.setMessageSelector(getSelector());
		}
		setupMessageListener(container);
	}

	protected void setupJcaMessageContainer(JmsMessageEndpointManager container) {
		JmsActivationSpecConfig activationSpecConfig = container.getActivationSpecConfig();
		if (activationSpecConfig == null) {
			activationSpecConfig = new JmsActivationSpecConfig();
			container.setActivationSpecConfig(activationSpecConfig);
		}

		if (getDestination() != null) {
			activationSpecConfig.setDestinationName(getDestination());
		}
		if (getSubscription() != null) {
			activationSpecConfig.setDurableSubscriptionName(getSubscription());
		}
		if (getSelector() != null) {
			activationSpecConfig.setMessageSelector(getSelector());
		}
		setupMessageListener(container);
	}

	/**
	 * Create a {@link MessageListener} that is able to serve this endpoint for the
	 * specified container.
	 */
	protected abstract MessageListener createMessageListener(MessageListenerContainer container);

	private void setupMessageListener(MessageListenerContainer container) {
		MessageListener messageListener = createMessageListener(container);
		Assert.state(messageListener != null, "Endpoint [" + this + "] must provide a non null message listener");
		container.setupMessageListener(messageListener);
	}

	/**
	 * Return a description for this endpoint.
	 * <p>Available to subclasses, for inclusion in their {@code toString()} result.
	 */
	protected StringBuilder getEndpointDescription() {
		StringBuilder result = new StringBuilder();
		return result.append(getClass().getSimpleName())
				.append("[")
				.append(this.id)
				.append("] destination=")
				.append(this.destination)
				.append("' | subscription='")
				.append(this.subscription)
				.append(" | selector='")
				.append(this.selector)
				.append("'");
	}

	@Override
	public String toString() {
		return getEndpointDescription().toString();
	}

}
