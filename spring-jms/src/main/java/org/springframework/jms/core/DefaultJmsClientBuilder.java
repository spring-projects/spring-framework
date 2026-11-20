/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.core;

import java.util.ArrayList;
import java.util.List;

import jakarta.jms.ConnectionFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.core.CompositeMessagePostProcessor;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link JmsClient.Builder}.
 * @author Brian Clozel
 * @since 7.0
 * @see JmsClient#builder(ConnectionFactory)
 * @see JmsClient#builder(JmsOperations)
 */
class DefaultJmsClientBuilder implements JmsClient.Builder {

	private final DefaultJmsClient jmsClient;

	private @Nullable List<MessageConverter> messageConverters;

	private @Nullable List<MessagePostProcessor> messagePostProcessors;


	DefaultJmsClientBuilder(ConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		this.jmsClient = new DefaultJmsClient(connectionFactory);
	}

	DefaultJmsClientBuilder(JmsOperations jmsTemplate) {
		Assert.notNull(jmsTemplate, "JmsOperations must not be null");
		this.jmsClient = new DefaultJmsClient(jmsTemplate);
	}

	@Override
	public JmsClient.Builder messageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "MessageConverter must not be null");
		if (this.messageConverters == null) {
			this.messageConverters = new ArrayList<>();
		}
		this.messageConverters.add(messageConverter);
		return this;
	}

	@Override
	public JmsClient.Builder messagePostProcessor(MessagePostProcessor messagePostProcessor) {
		Assert.notNull(messagePostProcessor, "MessagePostProcessor must not be null");
		if (this.messagePostProcessors == null) {
			this.messagePostProcessors = new ArrayList<>();
		}
		this.messagePostProcessors.add(messagePostProcessor);
		return this;
	}

	@Override
	public JmsClient build() {
		if (this.messageConverters != null) {
			this.jmsClient.setMessageConverter(new CompositeMessageConverter(this.messageConverters));
		}
		if (this.messagePostProcessors != null) {
			this.jmsClient.setMessagePostProcessor(new CompositeMessagePostProcessor(this.messagePostProcessors));
		}
		return this.jmsClient;
	}

}
