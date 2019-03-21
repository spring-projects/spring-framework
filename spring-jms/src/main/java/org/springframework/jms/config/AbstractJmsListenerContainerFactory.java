/*
 * Copyright 2002-2018 the original author or authors.
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

import javax.jms.ConnectionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.ErrorHandler;

/**
 * Base {@link JmsListenerContainerFactory} for Spring's base container implementation.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see AbstractMessageListenerContainer
 */
public abstract class AbstractJmsListenerContainerFactory<C extends AbstractMessageListenerContainer>
		implements JmsListenerContainerFactory<C> {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private ConnectionFactory connectionFactory;

	@Nullable
	private DestinationResolver destinationResolver;

	@Nullable
	private ErrorHandler errorHandler;

	@Nullable
	private MessageConverter messageConverter;

	@Nullable
	private Boolean sessionTransacted;

	@Nullable
	private Integer sessionAcknowledgeMode;

	@Nullable
	private Boolean pubSubDomain;

	@Nullable
	private Boolean replyPubSubDomain;

	@Nullable
	private QosSettings replyQosSettings;

	@Nullable
	private Boolean subscriptionDurable;

	@Nullable
	private Boolean subscriptionShared;

	@Nullable
	private String clientId;

	@Nullable
	private Integer phase;

	@Nullable
	private Boolean autoStartup;


	/**
	 * @see AbstractMessageListenerContainer#setConnectionFactory(ConnectionFactory)
	 */
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * @see AbstractMessageListenerContainer#setDestinationResolver(DestinationResolver)
	 */
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	/**
	 * @see AbstractMessageListenerContainer#setErrorHandler(ErrorHandler)
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * @see AbstractMessageListenerContainer#setMessageConverter(MessageConverter)
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * @see AbstractMessageListenerContainer#setSessionTransacted(boolean)
	 */
	public void setSessionTransacted(Boolean sessionTransacted) {
		this.sessionTransacted = sessionTransacted;
	}

	/**
	 * @see AbstractMessageListenerContainer#setSessionAcknowledgeMode(int)
	 */
	public void setSessionAcknowledgeMode(Integer sessionAcknowledgeMode) {
		this.sessionAcknowledgeMode = sessionAcknowledgeMode;
	}

	/**
	 * @see AbstractMessageListenerContainer#setPubSubDomain(boolean)
	 */
	public void setPubSubDomain(Boolean pubSubDomain) {
		this.pubSubDomain = pubSubDomain;
	}

	/**
	 * @see AbstractMessageListenerContainer#setReplyPubSubDomain(boolean)
	 */
	public void setReplyPubSubDomain(Boolean replyPubSubDomain) {
		this.replyPubSubDomain = replyPubSubDomain;
	}

	/**
	 * @see AbstractMessageListenerContainer#setReplyQosSettings(QosSettings)
	 */
	public void setReplyQosSettings(QosSettings replyQosSettings) {
		this.replyQosSettings = replyQosSettings;
	}

	/**
	 * @see AbstractMessageListenerContainer#setSubscriptionDurable(boolean)
	 */
	public void setSubscriptionDurable(Boolean subscriptionDurable) {
		this.subscriptionDurable = subscriptionDurable;
	}

	/**
	 * @see AbstractMessageListenerContainer#setSubscriptionShared(boolean)
	 */
	public void setSubscriptionShared(Boolean subscriptionShared) {
		this.subscriptionShared = subscriptionShared;
	}

	/**
	 * @see AbstractMessageListenerContainer#setClientId(String)
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * @see AbstractMessageListenerContainer#setPhase(int)
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * @see AbstractMessageListenerContainer#setAutoStartup(boolean)
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public C createListenerContainer(JmsListenerEndpoint endpoint) {
		C instance = createContainerInstance();

		if (this.connectionFactory != null) {
			instance.setConnectionFactory(this.connectionFactory);
		}
		if (this.destinationResolver != null) {
			instance.setDestinationResolver(this.destinationResolver);
		}
		if (this.errorHandler != null) {
			instance.setErrorHandler(this.errorHandler);
		}
		if (this.messageConverter != null) {
			instance.setMessageConverter(this.messageConverter);
		}
		if (this.sessionTransacted != null) {
			instance.setSessionTransacted(this.sessionTransacted);
		}
		if (this.sessionAcknowledgeMode != null) {
			instance.setSessionAcknowledgeMode(this.sessionAcknowledgeMode);
		}
		if (this.pubSubDomain != null) {
			instance.setPubSubDomain(this.pubSubDomain);
		}
		if (this.replyPubSubDomain != null) {
			instance.setReplyPubSubDomain(this.replyPubSubDomain);
		}
		if (this.replyQosSettings != null) {
			instance.setReplyQosSettings(this.replyQosSettings);
		}
		if (this.subscriptionDurable != null) {
			instance.setSubscriptionDurable(this.subscriptionDurable);
		}
		if (this.subscriptionShared != null) {
			instance.setSubscriptionShared(this.subscriptionShared);
		}
		if (this.clientId != null) {
			instance.setClientId(this.clientId);
		}
		if (this.phase != null) {
			instance.setPhase(this.phase);
		}
		if (this.autoStartup != null) {
			instance.setAutoStartup(this.autoStartup);
		}

		initializeContainer(instance);
		endpoint.setupListenerContainer(instance);

		return instance;
	}

	/**
	 * Create an empty container instance.
	 */
	protected abstract C createContainerInstance();

	/**
	 * Further initialize the specified container.
	 * <p>Subclasses can inherit from this method to apply extra
	 * configuration if necessary.
	 */
	protected void initializeContainer(C instance) {
	}

}
