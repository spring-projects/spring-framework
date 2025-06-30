/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.jms.config;

import jakarta.jms.MessageListener;
import org.jspecify.annotations.Nullable;

import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.jms.listener.endpoint.JmsActivationSpecConfig;
import org.springframework.jms.listener.endpoint.JmsMessageEndpointManager;
import org.springframework.util.StringUtils;

/**
 * Base model for a JMS listener endpoint.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see MethodJmsListenerEndpoint
 * @see SimpleJmsListenerEndpoint
 */
public abstract class AbstractJmsListenerEndpoint implements JmsListenerEndpoint {

	private String id = "";

	private @Nullable String destination;

	private @Nullable String subscription;

	private @Nullable String selector;

	private @Nullable String concurrency;


	/**
	 * Set a custom id for this endpoint.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Return the id of this endpoint (possibly generated).
	 */
	@Override
	public String getId() {
		return this.id;
	}

	/**
	 * Set the name of the destination for this endpoint.
	 */
	public void setDestination(@Nullable String destination) {
		this.destination = destination;
	}

	/**
	 * Return the name of the destination for this endpoint.
	 */
	public @Nullable String getDestination() {
		return this.destination;
	}

	/**
	 * Set the name for the durable subscription.
	 */
	public void setSubscription(@Nullable String subscription) {
		this.subscription = subscription;
	}

	/**
	 * Return the name for the durable subscription, if any.
	 */
	public @Nullable String getSubscription() {
		return this.subscription;
	}

	/**
	 * Set the JMS message selector expression.
	 * <p>See the JMS specification for a detailed definition of selector expressions.
	 */
	public void setSelector(@Nullable String selector) {
		this.selector = selector;
	}

	/**
	 * Return the JMS message selector expression, if any.
	 */
	public @Nullable String getSelector() {
		return this.selector;
	}

	/**
	 * Set a concurrency for the listener, if any.
	 * <p>The concurrency limits can be a "lower-upper" String, for example, "5-10", or a simple
	 * upper limit String, for example, "10" (the lower limit will be 1 in this case).
	 * <p>The underlying container may or may not support all features. For instance, it
	 * may not be able to scale: in that case only the upper value is used.
	 */
	public void setConcurrency(@Nullable String concurrency) {
		this.concurrency = concurrency;
	}

	/**
	 * Return the concurrency for the listener, if any.
	 */
	public @Nullable String getConcurrency() {
		return this.concurrency;
	}


	@Override
	public void setupListenerContainer(MessageListenerContainer listenerContainer) {
		if (listenerContainer instanceof AbstractMessageListenerContainer abstractContainer) {
			setupJmsListenerContainer(abstractContainer);
		}
		else {
			new JcaEndpointConfigurer().configureEndpoint(listenerContainer);
		}
	}

	private void setupJmsListenerContainer(AbstractMessageListenerContainer listenerContainer) {
		if (StringUtils.hasText(getId())) {
			listenerContainer.setBeanName(getId());
		}
		if (getDestination() != null) {
			listenerContainer.setDestinationName(getDestination());
		}
		if (getSubscription() != null) {
			listenerContainer.setSubscriptionName(getSubscription());
		}
		if (getSelector() != null) {
			listenerContainer.setMessageSelector(getSelector());
		}
		if (getConcurrency() != null) {
			listenerContainer.setConcurrency(getConcurrency());
		}
		setupMessageListener(listenerContainer);
	}

	/**
	 * Create a {@link MessageListener} that is able to serve this endpoint for the
	 * specified container.
	 */
	protected abstract MessageListener createMessageListener(MessageListenerContainer container);

	private void setupMessageListener(MessageListenerContainer container) {
		container.setupMessageListener(createMessageListener(container));
	}

	/**
	 * Return a description for this endpoint.
	 * <p>Available to subclasses, for inclusion in their {@code toString()} result.
	 */
	protected StringBuilder getEndpointDescription() {
		StringBuilder result = new StringBuilder();
		return result.append(getClass().getSimpleName()).append('[').append(this.id).append("] destination=").
				append(this.destination).append("' | subscription='").append(this.subscription).
				append(" | selector='").append(this.selector).append('\'');
	}

	@Override
	public String toString() {
		return getEndpointDescription().toString();
	}


	/**
	 * Inner class to avoid a hard dependency on the JCA API.
	 */
	private class JcaEndpointConfigurer {

		public void configureEndpoint(Object listenerContainer) {
			if (listenerContainer instanceof JmsMessageEndpointManager endpointManager) {
				setupJcaMessageContainer(endpointManager);
			}
			else {
				throw new IllegalArgumentException("Could not configure endpoint with the specified container '" +
						listenerContainer + "' Only JMS (" + AbstractMessageListenerContainer.class.getName() +
						" subclass) or JCA (" + JmsMessageEndpointManager.class.getName() + ") are supported.");
			}
		}

		private void setupJcaMessageContainer(JmsMessageEndpointManager container) {
			JmsActivationSpecConfig activationSpecConfig = container.getActivationSpecConfig();
			if (activationSpecConfig == null) {
				activationSpecConfig = new JmsActivationSpecConfig();
				container.setActivationSpecConfig(activationSpecConfig);
			}
			if (getDestination() != null) {
				activationSpecConfig.setDestinationName(getDestination());
			}
			if (getSubscription() != null) {
				activationSpecConfig.setSubscriptionName(getSubscription());
			}
			if (getSelector() != null) {
				activationSpecConfig.setMessageSelector(getSelector());
			}
			if (getConcurrency() != null) {
				activationSpecConfig.setConcurrency(getConcurrency());
			}
			setupMessageListener(container);
		}
	}

}
